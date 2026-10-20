package com.bettercontent.betterblockplacementpreview.placementpreview;

import com.bettercontent.betterblockplacementpreview.ModMain;
import com.bettercontent.betterblockplacementpreview.diagnostics.PlaytestLog;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.BedItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DoubleHighBlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.Vec3;
import net.sharpesthead.rotavision.PlacementHandler;
import net.sharpesthead.rotavision.RotaVision;
import net.sharpesthead.rotavision.RotationUtils;

final class SupportPreviewServer {
    private static final double MAX_DISTANCE_SQUARED = 8.0 * 8.0;

    private SupportPreviewServer() {
    }

    static SupportPreviewResponse evaluate(
            final ServerPlayer player,
            final SupportPreviewRequest request
    ) {
        final Reconstruction reconstruction = reconstructResult(player, request);
        final PlacementProposal proposal = reconstruction.proposal();
        if (proposal == null) {
            outcome(player, request, "unknown", reconstruction.reason());
            return unknown(request.sequence(), request.blockHitResult().getBlockPos());
        }
        final SupportPreviewVerdict verdict = SupportPreviewVersions.physicsSupported()
                ? RbpSupportPreviewAdapter.evaluate(player.serverLevel(), proposal)
                : SupportPreviewVerdict.UNKNOWN;
        outcome(player, request, verdict.name(), verdict == SupportPreviewVerdict.UNKNOWN
                ? "physics_unsupported_or_unmodeled" : "advisory_evaluated");
        return new SupportPreviewResponse(
                request.sequence(),
                proposal.targetPos(),
                net.minecraft.world.level.block.Block.getId(proposal.primaryState()),
                verdict);
    }

    private static void outcome(ServerPlayer player, SupportPreviewRequest request, String result, String reason) {
        if (ModMain.PLAYTEST.enabled("support")) {
            ModMain.PLAYTEST.event("support", "preview.evaluated", "server", result, reason,
                    () -> PlaytestLog.fields("sequence", request.sequence()));
        }
        if (ModMain.PLAYTEST.traceEnabled("support")) {
            var pos = request.blockHitResult().getBlockPos();
            ModMain.PLAYTEST.trace("support", "preview.focused", new PlaytestLog.Context("server",
                    player.serverLevel().dimension().location().toString(), player.serverLevel().getGameTime(),
                    player.getUUID().toString(), pos.getX() + "," + pos.getY() + "," + pos.getZ()), result, reason,
                    () -> PlaytestLog.fields("sequence", request.sequence()));
        }
    }

    static PlacementProposal reconstruct(
            final ServerPlayer player,
            final SupportPreviewRequest request
    ) {
        return reconstructResult(player, request).proposal();
    }

    /** Reasons describe existing advisory decisions, not placement admission or world mutation. */
    private record Reconstruction(PlacementProposal proposal, String reason) {}

    private static Reconstruction reconstructResult(
            final ServerPlayer player,
            final SupportPreviewRequest request
    ) {
        if (!SupportPreviewVersions.rotaVisionSupported() || !RotaVision.isEnabledFor(player.getUUID())) {
            return new Reconstruction(null, "rotavision_unavailable_or_disabled");
        }
        final Vec3 eye = player.getEyePosition();
        if (eye.distanceToSqr(request.blockHitResult().getLocation()) > MAX_DISTANCE_SQUARED) {
            return new Reconstruction(null, "distance");
        }
        final ItemStack stack = player.getItemInHand(request.hand());
        if (!(stack.getItem() instanceof BlockItem blockItem)) {
            return new Reconstruction(null, "not_block_item");
        }
        final BlockPlaceContext context = new BlockPlaceContext(
                player,
                request.hand(),
                stack,
                request.blockHitResult());
        final BlockPos target = context.getClickedPos();
        if (!player.serverLevel().hasChunkAt(target)) {
            return new Reconstruction(null, "target_unloaded");
        }
        BlockState state = blockItem.getBlock().getStateForPlacement(context);
        if (state == null) {
            return new Reconstruction(null, "no_placement_state");
        }
        if (state.hasProperty(ChestBlock.TYPE)) {
            state = state.setValue(ChestBlock.TYPE, ChestType.SINGLE);
        }
        final int rotation = RotaVision.PLAYER_ROTATIONS.getOrDefault(player.getUUID(), 1);
        if (PlacementHandler.isRotatable(state)) {
            state = RotationUtils.applyRotation(state, rotation);
        }

        final Map<BlockPos, BlockState> states = modelStates(blockItem, state, target);
        if (states == null || states.keySet().stream().anyMatch(pos -> !player.serverLevel().hasChunkAt(pos))) {
            return new Reconstruction(null, "unmodeled_or_unloaded_multiblock");
        }
        return new Reconstruction(new PlacementProposal(target.immutable(), state, states), "reconstructed");
    }

    static Map<BlockPos, BlockState> modelStates(
            final BlockItem blockItem,
            final BlockState state,
            final BlockPos target
    ) {
        if (blockItem.getClass() != BlockItem.class
                && !(blockItem instanceof BedItem)
                && !(blockItem instanceof DoubleHighBlockItem)) {
            return null;
        }
        final Map<BlockPos, BlockState> states = new LinkedHashMap<>();
        states.put(target.immutable(), state);
        if (state.getBlock() instanceof BedBlock || blockItem instanceof BedItem) {
            if (!state.hasProperty(BlockStateProperties.BED_PART)
                    || !state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
                return null;
            }
            final BlockPos headPos = target.relative(state.getValue(BlockStateProperties.HORIZONTAL_FACING));
            states.put(headPos, state.setValue(BlockStateProperties.BED_PART, BedPart.HEAD));
        } else if (state.getBlock() instanceof DoorBlock) {
            states.put(target.above(), state.setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.UPPER));
        } else if (blockItem instanceof DoubleHighBlockItem
                || state.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)) {
            if (!state.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)) {
                return null;
            }
            states.put(target.above(), state.setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.UPPER));
        }
        return Map.copyOf(states);
    }

    private static SupportPreviewResponse unknown(final int sequence, final BlockPos target) {
        return new SupportPreviewResponse(sequence, target.immutable(), 0, SupportPreviewVerdict.UNKNOWN);
    }
}
