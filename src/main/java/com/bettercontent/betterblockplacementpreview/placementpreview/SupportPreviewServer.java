package com.bettercontent.betterblockplacementpreview.placementpreview;

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
        final PlacementProposal proposal = reconstruct(player, request);
        if (proposal == null) {
            return unknown(request.sequence(), request.blockHitResult().getBlockPos());
        }
        final SupportPreviewVerdict verdict = SupportPreviewVersions.physicsSupported()
                ? RbpSupportPreviewAdapter.evaluate(player.serverLevel(), proposal)
                : SupportPreviewVerdict.UNKNOWN;
        return new SupportPreviewResponse(
                request.sequence(),
                proposal.targetPos(),
                net.minecraft.world.level.block.Block.getId(proposal.primaryState()),
                verdict);
    }

    static PlacementProposal reconstruct(
            final ServerPlayer player,
            final SupportPreviewRequest request
    ) {
        if (!SupportPreviewVersions.rotaVisionSupported() || !RotaVision.isEnabledFor(player.getUUID())) {
            return null;
        }
        final Vec3 eye = player.getEyePosition();
        if (eye.distanceToSqr(request.blockHitResult().getLocation()) > MAX_DISTANCE_SQUARED) {
            return null;
        }
        final ItemStack stack = player.getItemInHand(request.hand());
        if (!(stack.getItem() instanceof BlockItem blockItem)) {
            return null;
        }
        final BlockPlaceContext context = new BlockPlaceContext(
                player,
                request.hand(),
                stack,
                request.blockHitResult());
        final BlockPos target = context.getClickedPos();
        if (!player.serverLevel().hasChunkAt(target)) {
            return null;
        }
        BlockState state = blockItem.getBlock().getStateForPlacement(context);
        if (state == null) {
            return null;
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
            return null;
        }
        return new PlacementProposal(target.immutable(), state, states);
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
