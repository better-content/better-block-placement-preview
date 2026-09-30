package com.bettercontent.betterblockplacementpreview.placementpreview;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.BlockHitResult;

public record SupportPreviewRequest(int sequence, InteractionHand hand, BlockHitResult blockHitResult) {
    static SupportPreviewRequest decode(final FriendlyByteBuf buffer) {
        return new SupportPreviewRequest(
                buffer.readVarInt(),
                buffer.readEnum(InteractionHand.class),
                buffer.readBlockHitResult());
    }

    void encode(final FriendlyByteBuf buffer) {
        buffer.writeVarInt(sequence);
        buffer.writeEnum(hand);
        buffer.writeBlockHitResult(blockHitResult);
    }
}
