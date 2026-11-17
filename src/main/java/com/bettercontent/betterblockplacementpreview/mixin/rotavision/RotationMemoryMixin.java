package com.bettercontent.betterblockplacementpreview.mixin.rotavision;

import com.bettercontent.betterblockplacementpreview.placementpreview.RotationMemory;
import net.minecraft.world.item.Item;
import net.minecraftforge.event.TickEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Capture the native index before its item-change reset, restore through its own packet. */
@Mixin(targets = "net.sharpesthead.rotavision.ClientEvents$ClientForgeEvents", remap = false)
public abstract class RotationMemoryMixin {
    @Shadow(remap = false) private static Item lastItem;
    @Shadow(remap = false) public static int rotationIndex;
    private static RotationMemory.Snapshot better_block_placement_preview$pending;

    @Inject(method = "onClientTick(Lnet/minecraftforge/event/TickEvent$ClientTickEvent;)V",
            at = @At("HEAD"), require = 1, remap = false)
    private static void better_block_placement_preview$beforeRotationTick(TickEvent.ClientTickEvent event, CallbackInfo callback) {
        if (event.phase == TickEvent.Phase.END)
            better_block_placement_preview$pending = RotationMemory.before(lastItem, rotationIndex);
    }

    @Inject(method = "onClientTick(Lnet/minecraftforge/event/TickEvent$ClientTickEvent;)V",
            at = @At("TAIL"), require = 1, remap = false)
    private static void better_block_placement_preview$afterRotationTick(TickEvent.ClientTickEvent event, CallbackInfo callback) {
        if (event.phase != TickEvent.Phase.END) return;
        rotationIndex = RotationMemory.after(better_block_placement_preview$pending, rotationIndex);
        better_block_placement_preview$pending = null;
    }
}
