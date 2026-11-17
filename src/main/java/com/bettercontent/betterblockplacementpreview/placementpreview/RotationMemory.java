package com.bettercontent.betterblockplacementpreview.placementpreview;

import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.sharpesthead.rotavision.ClientEvents;
import net.sharpesthead.rotavision.RotaVision;
import net.sharpesthead.rotavision.RotationPacket;
import net.minecraftforge.registries.ForgeRegistries;

/** Client-session block-ID rotation memory; RotaVision remains the packet/placement owner. */
public final class RotationMemory {
    private static final int MAX_BLOCKS = 512;
    private static final Map<ResourceLocation, Integer> BY_BLOCK = new LinkedHashMap<>();
    private static ClientLevel world;

    private RotationMemory() {}

    public record Snapshot(ResourceLocation block, boolean switched) {}

    public static Snapshot before(Item previousItem, int nativeIndex) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) {
            clear();
            return null;
        }
        if (world != minecraft.level) {
            clear(); // Includes dimension changes; never persist across worlds or servers.
            world = minecraft.level;
        } else if (rotatable(previousItem)) {
            remember(id(previousItem), nativeIndex);
        }
        Item held = minecraft.player.getMainHandItem().getItem();
        return new Snapshot(id(held), previousItem != held);
    }

    public static int after(Snapshot snapshot, int nativeIndex) {
        if (snapshot == null || snapshot.block() == null) return nativeIndex;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level != world || minecraft.player == null
                || !RotaVision.isEnabledFor(minecraft.player.getUUID())) return nativeIndex;
        if (!snapshot.switched()) {
            if (rotatable(minecraft.player.getMainHandItem().getItem()))
                remember(snapshot.block(), nativeIndex);
            return nativeIndex;
        }
        Integer saved = BY_BLOCK.get(snapshot.block());
        if (saved == null) return nativeIndex;
        Item held = minecraft.player.getMainHandItem().getItem();
        if (!snapshot.block().equals(id(held)) || !rotatable(held)) return nativeIndex;
        // Native tick resets to index 1 before consuming the rotation key. Preserve a
        // click on the same switch tick instead of overwriting it with the saved value.
        int delta = Math.floorMod(nativeIndex - 1, 24);
        int restored = (saved + delta) % 24;
        remember(snapshot.block(), restored);
        if (restored != nativeIndex) RotaVision.PACKET_HANDLER.sendToServer(new RotationPacket(restored));
        return restored;
    }

    private static ResourceLocation id(Item item) {
        return item instanceof BlockItem block ? ForgeRegistries.BLOCKS.getKey(block.getBlock()) : null;
    }

    private static boolean rotatable(Item item) {
        return item instanceof BlockItem block
                && ClientEvents.ClientForgeEvents.isRotatable(block.getBlock().defaultBlockState());
    }

    private static void remember(ResourceLocation block, int index) {
        if (block == null || index < 0 || index >= 24) return;
        if (!BY_BLOCK.containsKey(block) && BY_BLOCK.size() >= MAX_BLOCKS)
            BY_BLOCK.remove(BY_BLOCK.keySet().iterator().next());
        BY_BLOCK.put(block, index);
    }

    public static void clear() {
        BY_BLOCK.clear();
        world = null;
    }
}
