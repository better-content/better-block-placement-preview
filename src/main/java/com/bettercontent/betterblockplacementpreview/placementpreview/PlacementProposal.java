package com.bettercontent.betterblockplacementpreview.placementpreview;

import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

record PlacementProposal(BlockPos targetPos, BlockState primaryState, Map<BlockPos, BlockState> states) {
}
