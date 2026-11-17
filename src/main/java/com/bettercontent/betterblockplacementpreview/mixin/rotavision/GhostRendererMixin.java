package com.bettercontent.betterblockplacementpreview.mixin.rotavision;

import com.bettercontent.betterblockplacementpreview.placementpreview.SupportPreviewClient;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.sharpesthead.rotavision.ClientEvents;
import net.sharpesthead.rotavision.RotationUtils;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.sharpesthead.rotavision.GhostRenderer", remap = false)
public abstract class GhostRendererMixin {
    private static final int ZERO_ROTATION_EQUIVALENT = 120;

    @ModifyExpressionValue(
            method = "onRenderLevel(Lnet/minecraftforge/client/event/RenderLevelStageEvent;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/sharpesthead/rotavision/ClientEvents$ClientForgeEvents;isRotatable(Lnet/minecraft/world/level/block/state/BlockState;)Z",
                    remap = false),
            require = 1,
            remap = false)
    private static boolean betterContentFixes$previewEveryBlockItem(final boolean upstream) {
        return true;
    }

    @ModifyExpressionValue(
            method = "onRenderLevel(Lnet/minecraftforge/client/event/RenderLevelStageEvent;)V",
            at = @At(
                    value = "FIELD",
                    target = "Lnet/sharpesthead/rotavision/ClientEvents$ClientForgeEvents;rotationIndex:I",
                    opcode = Opcodes.GETSTATIC,
                    remap = false),
            require = 1,
            remap = false)
    private static int betterContentFixes$renderContextOrientationAtZero(final int rotationIndex) {
        return rotationIndex == 0 ? ZERO_ROTATION_EQUIVALENT : rotationIndex;
    }

    @Redirect(
            method = "onRenderLevel(Lnet/minecraftforge/client/event/RenderLevelStageEvent;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/sharpesthead/rotavision/RotationUtils;applyRotation(Lnet/minecraft/world/level/block/state/BlockState;I)Lnet/minecraft/world/level/block/state/BlockState;",
                    remap = false),
            require = 1,
            remap = false)
    private static BlockState betterContentFixes$rotateAndObserve(
            final BlockState placementState,
            final int rotationIndex
    ) {
        final BlockState proposedState = ClientEvents.ClientForgeEvents.isRotatable(placementState)
                ? RotationUtils.applyRotation(placementState, rotationIndex)
                : placementState;
        final Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.hitResult instanceof BlockHitResult hit) {
            SupportPreviewClient.observe(InteractionHand.MAIN_HAND, hit, proposedState);
        }
        return proposedState;
    }

    @Inject(method = "onRenderLevel(Lnet/minecraftforge/client/event/RenderLevelStageEvent;)V",
            at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;pushPose()V",
                    ordinal = 0, remap = true), cancellable = true, require = 1, remap = false)
    private static void betterContentFixes$hideKnownProhibitedGhost(
            net.minecraftforge.client.event.RenderLevelStageEvent event, CallbackInfo callback) {
        if (SupportPreviewClient.suppressGhost()) callback.cancel();
    }
}
