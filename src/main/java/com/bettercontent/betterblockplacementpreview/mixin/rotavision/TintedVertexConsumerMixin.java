package com.bettercontent.betterblockplacementpreview.mixin.rotavision;

import com.bettercontent.betterblockplacementpreview.placementpreview.PreviewTint;
import com.bettercontent.betterblockplacementpreview.placementpreview.SupportPreviewClient;
import com.mojang.blaze3d.vertex.VertexConsumer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Tint at the consumer boundary, independent of either upstream render call site. */
@Mixin(targets = "net.sharpesthead.rotavision.GhostRenderer$TintedVertexConsumer", remap = false)
public abstract class TintedVertexConsumerMixin {
    @Shadow(remap = false) @Final @Mutable private int r;
    @Shadow(remap = false) @Final @Mutable private int g;
    @Shadow(remap = false) @Final @Mutable private int b;
    @Shadow(remap = false) @Final @Mutable private int a;

    @Inject(method = "<init>(Lcom/mojang/blaze3d/vertex/VertexConsumer;IIII)V", at = @At("TAIL"),
            require = 1, allow = 1, remap = false)
    private void betterContentFixes$applyStructuralTint(
            final VertexConsumer parent, final int red, final int green, final int blue, final int alpha,
            final CallbackInfo callback
    ) {
        final PreviewTint tint = SupportPreviewClient.tint(green == 0xFF && blue == 0xFF);
        this.r = tint.red();
        this.g = tint.green();
        this.b = tint.blue();
        this.a = tint.alpha();
    }
}
