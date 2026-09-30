package com.bettercontent.betterblockplacementpreview.placementpreview;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;

public record SupportPreviewResponse(
        int sequence,
        BlockPos targetPos,
        int blockStateId,
        SupportPreviewVerdict verdict
) {
    static SupportPreviewResponse decode(final FriendlyByteBuf buffer) {
        return new SupportPreviewResponse(
                buffer.readVarInt(),
                buffer.readBlockPos(),
                buffer.readVarInt(),
                buffer.readEnum(SupportPreviewVerdict.class));
    }

    void encode(final FriendlyByteBuf buffer) {
        buffer.writeVarInt(sequence);
        buffer.writeBlockPos(targetPos);
        buffer.writeVarInt(blockStateId);
        buffer.writeEnum(verdict);
    }
}
