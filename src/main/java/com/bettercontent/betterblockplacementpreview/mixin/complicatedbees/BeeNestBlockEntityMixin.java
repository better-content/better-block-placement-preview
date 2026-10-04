package com.bettercontent.betterblockplacementpreview.mixin.complicatedbees;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "com.accbdd.complicated_bees.block.entity.BeeNestBlockEntity", remap = false)
public abstract class BeeNestBlockEntityMixin {
    @Inject(method = "getNestColor", at = @At("HEAD"), cancellable = true, require = 1, remap = false)
    private static void betterBlockPlacementPreview$colorWithoutLevel(
            final BlockState state,
            final BlockAndTintGetter level,
            final BlockPos pos,
            final int tintIndex,
            final CallbackInfoReturnable<Integer> callback
    ) {
        if (level == null) {
            callback.setReturnValue(0xFFFFFF);
        }
    }
}
