package com.bettercontent.realisticplacementpreview.placementpreview;

import com.bettercontent.realisticplacementpreview.ModMain;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import xbigellx.rbp.internal.physics.BlockPhysicsHandler;
import xbigellx.realisticphysics.RealisticPhysics;

@PrefixGameTestTemplate(false)
public final class SupportPreviewGameTests {
    private SupportPreviewGameTests() {
    }

    @GameTest(templateNamespace = ModMain.MOD_ID, template = "empty")
    public static void supportedPlacementUsesSnapshotWithoutMutatingWorldOrTasks(final GameTestHelper helper) {
        final BlockPos foundation = helper.absolutePos(BlockPos.ZERO);
        final BlockPos target = foundation.above();
        final BlockState before = helper.getLevel().getBlockState(target);
        final int tasksBefore = queuedTasks(helper);
        final SupportPreviewVerdict verdict = evaluate(helper, target, Blocks.STONE.defaultBlockState());
        final int tasksAfter = queuedTasks(helper);
        if (verdict != SupportPreviewVerdict.SUPPORTED) {
            helper.fail("Expected supported stone placement, got " + verdict);
        } else if (helper.getLevel().getBlockState(target) != before) {
            helper.fail("Support preview mutated the world");
        } else if (tasksAfter != tasksBefore) {
            helper.fail("Support preview mutated the live RBP task queue");
        } else {
            helper.succeed();
        }
    }

    @GameTest(templateNamespace = ModMain.MOD_ID, template = "empty")
    public static void unsupportedUnstableBlockIsPredictedToFall(final GameTestHelper helper) {
        final BlockPos target = helper.absolutePos(new BlockPos(0, 3, 0));
        expect(helper, SupportPreviewVerdict.WILL_FALL,
                evaluate(helper, target, Blocks.SAND.defaultBlockState()), "unsupported sand");
    }

    @GameTest(templateNamespace = ModMain.MOD_ID, template = "empty")
    public static void crushingFailureIsPredictedToFall(final GameTestHelper helper) {
        final BlockPos foundation = helper.absolutePos(BlockPos.ZERO);
        helper.getLevel().setBlockAndUpdate(foundation.above(), Blocks.SPONGE.defaultBlockState());
        for (int y = 2; y <= 4; y++) {
            helper.getLevel().setBlockAndUpdate(foundation.above(y), Blocks.OBSIDIAN.defaultBlockState());
        }
        final BlockPos target = foundation.above(5);
        expect(helper, SupportPreviewVerdict.WILL_FALL,
                evaluate(helper, target, Blocks.OBSIDIAN.defaultBlockState()), "crushing stack");
    }

    @GameTest(templateNamespace = ModMain.MOD_ID, template = "empty")
    public static void rotatedCollisionStateIsUsedByTheOverlay(final GameTestHelper helper) {
        final BlockPos target = helper.absolutePos(BlockPos.ZERO).above();
        final BlockState rotated = Blocks.OAK_STAIRS.defaultBlockState()
                .setValue(StairBlock.FACING, Direction.EAST);
        final PlacementProposal proposal = proposal(target, rotated);
        final SupportPreviewVerdict verdict = RbpSupportPreviewAdapter.evaluate(helper.getLevel(), proposal);
        if (proposal.primaryState().getCollisionShape(helper.getLevel(), target).isEmpty()) {
            helper.fail("Rotated stair collision shape was lost");
        } else {
            expect(helper, SupportPreviewVerdict.SUPPORTED, verdict, "rotated stair");
        }
    }

    @GameTest(templateNamespace = ModMain.MOD_ID, template = "empty")
    public static void unmanagedBlockReturnsUnmanaged(final GameTestHelper helper) {
        final BlockPos target = helper.absolutePos(BlockPos.ZERO).above();
        expect(helper, SupportPreviewVerdict.UNMANAGED,
                evaluate(helper, target, Blocks.DANDELION.defaultBlockState()), "unmanaged flower");
    }

    @GameTest(templateNamespace = ModMain.MOD_ID, template = "empty")
    public static void bedAndDoorCompanionOverlaysAreEvaluated(final GameTestHelper helper) {
        final BlockPos origin = helper.absolutePos(BlockPos.ZERO);
        helper.getLevel().setBlockAndUpdate(origin.east(), Blocks.STONE.defaultBlockState());

        final BlockPos bedFoot = origin.above();
        final BlockState bed = Blocks.RED_BED.defaultBlockState()
                .setValue(BedBlock.PART, BedPart.FOOT)
                .setValue(BedBlock.FACING, Direction.EAST);
        final Map<BlockPos, BlockState> bedStates = SupportPreviewServer.modelStates(
                (net.minecraft.world.item.BlockItem) Blocks.RED_BED.asItem(), bed, bedFoot);
        if (bedStates == null || !bedStates.containsKey(bedFoot.east())) {
            helper.fail("Bed companion state was not modelled");
            return;
        }
        final SupportPreviewVerdict bedVerdict = RbpSupportPreviewAdapter.evaluate(
                helper.getLevel(), new PlacementProposal(bedFoot, bed, bedStates));

        final BlockPos doorLower = origin.above();
        final BlockState door = Blocks.OAK_DOOR.defaultBlockState()
                .setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER);
        final Map<BlockPos, BlockState> doorStates = SupportPreviewServer.modelStates(
                (net.minecraft.world.item.BlockItem) Blocks.OAK_DOOR.asItem(), door, doorLower);
        if (doorStates == null || !doorStates.containsKey(doorLower.above())) {
            helper.fail("Door companion state was not modelled");
            return;
        }
        final SupportPreviewVerdict doorVerdict = RbpSupportPreviewAdapter.evaluate(
                helper.getLevel(), new PlacementProposal(doorLower, door, doorStates));

        final BlockPos plantLower = origin.south().above();
        final BlockState plant = Blocks.SUNFLOWER.defaultBlockState()
                .setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.LOWER);
        final Map<BlockPos, BlockState> plantStates = SupportPreviewServer.modelStates(
                (net.minecraft.world.item.BlockItem) Blocks.SUNFLOWER.asItem(), plant, plantLower);
        if (plantStates == null || !plantStates.containsKey(plantLower.above())) {
            helper.fail("Double-plant companion state was not modelled");
            return;
        }

        if (bedVerdict != SupportPreviewVerdict.SUPPORTED || doorVerdict != SupportPreviewVerdict.SUPPORTED) {
            helper.fail("Expected supported bed/door overlays, got bed=" + bedVerdict + ", door=" + doorVerdict);
        } else {
            helper.succeed();
        }
    }

    private static SupportPreviewVerdict evaluate(
            final GameTestHelper helper,
            final BlockPos target,
            final BlockState state
    ) {
        return RbpSupportPreviewAdapter.evaluate(helper.getLevel(), proposal(target, state));
    }

    private static PlacementProposal proposal(final BlockPos target, final BlockState state) {
        return new PlacementProposal(target, state, Map.of(target, state));
    }

    private static int queuedTasks(final GameTestHelper helper) {
        return RealisticPhysics.physicsManager()
                .findPhysicsHandler(BlockPhysicsHandler.class, helper.getLevel())
                .map(handler -> handler.getLevel().taskManager().totalQueuedTasks())
                .orElse(-1);
    }

    private static void expect(
            final GameTestHelper helper,
            final SupportPreviewVerdict expected,
            final SupportPreviewVerdict actual,
            final String scenario
    ) {
        if (actual == expected) {
            helper.succeed();
        } else {
            helper.fail("Expected " + expected + " for " + scenario + ", got " + actual);
        }
    }
}
