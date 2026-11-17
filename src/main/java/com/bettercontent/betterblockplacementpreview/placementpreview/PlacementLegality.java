package com.bettercontent.betterblockplacementpreview.placementpreview;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/** Conservative, read-only rejection of placement that vanilla already knows is illegal.
 *  A false result is NOT a server placement permit; modded callbacks still own admission.
 */
public final class PlacementLegality {
    private PlacementLegality() {}

    public static boolean knownProhibited(Level level, Player player, ItemStack stack,
            BlockPlaceContext context, BlockState proposed) {
        if (proposed == null || level == null || player == null || stack.isEmpty()) return false;
        BlockPos target = context.getClickedPos();
        if (!level.hasChunkAt(target)) return false; // Unknown must not load chunks.
        try {
            return !context.canPlace()
                || !level.mayInteract(player, target)
                || !player.mayUseItemAt(target, context.getClickedFace(), stack)
                || !proposed.canSurvive(level, target);
        } catch (RuntimeException | LinkageError unknown) {
            // A modded predicate may be dynamic; fail open rather than making up a ban.
            return false;
        }
    }
}
