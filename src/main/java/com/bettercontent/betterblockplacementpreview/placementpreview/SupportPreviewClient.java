package com.bettercontent.betterblockplacementpreview.placementpreview;

import java.util.Objects;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.registries.ForgeRegistries;

public final class SupportPreviewClient {
    private static final PlacementPreviewCache<PlacementKey> CACHE = new PlacementPreviewCache<>();

    private SupportPreviewClient() {
    }

    public static void observe(
            final InteractionHand hand,
            final BlockHitResult hit,
            final BlockState proposedState
    ) {
        final Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) {
            return;
        }
        final ItemStack stack = minecraft.player.getItemInHand(hand);
        final BlockPlaceContext context = new BlockPlaceContext(new UseOnContext(minecraft.player, hand, hit));
        final BlockPos target = context.getClickedPos();
        final int stateId = Block.getId(proposedState);
        final ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(stack.getItem());
        final PlacementKey key = new PlacementKey(
                minecraft.level.dimension().location(),
                hand,
                target.immutable(),
                stateId,
                itemId,
                stack.hasTag() ? Objects.hashCode(stack.getTag()) : 0);
        final long now = monotonicMillis();
        CACHE.observe(key, target, stateId, now, sequence ->
                SupportPreviewNetwork.sendToServer(new SupportPreviewRequest(sequence, hand, hit)));
    }

    public static PreviewTint tint(final boolean vanillaPlacementValid) {
        final long now = monotonicMillis();
        return PreviewTint.resolve(vanillaPlacementValid, CACHE.verdict(now), now);
    }

    static void accept(final SupportPreviewResponse response) {
        CACHE.accept(response, monotonicMillis());
    }

    private static long monotonicMillis() {
        return System.nanoTime() / 1_000_000L;
    }

    private record PlacementKey(
            ResourceLocation dimension,
            InteractionHand hand,
            BlockPos target,
            int stateId,
            ResourceLocation item,
            int itemTagHash
    ) {
    }
}
