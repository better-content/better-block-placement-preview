package com.bettercontent.betterblockplacementpreview.placementpreview;

import com.bettercontent.betterblockplacementpreview.ModMain;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public final class SupportPreviewNetwork {
    private static final String PROTOCOL = "1";
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            ResourceLocation.fromNamespaceAndPath(ModMain.MOD_ID, "support_preview"),
            () -> PROTOCOL,
            PROTOCOL::equals,
            PROTOCOL::equals);
    private static boolean initialized;

    private SupportPreviewNetwork() {
    }

    public static synchronized void initialize() {
        if (initialized) {
            return;
        }
        initialized = true;
        CHANNEL.messageBuilder(SupportPreviewRequest.class, 0, NetworkDirection.PLAY_TO_SERVER)
                .encoder(SupportPreviewRequest::encode)
                .decoder(SupportPreviewRequest::decode)
                .consumerMainThread(SupportPreviewNetwork::handleRequest)
                .add();
        CHANNEL.messageBuilder(SupportPreviewResponse.class, 1, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SupportPreviewResponse::encode)
                .decoder(SupportPreviewResponse::decode)
                .consumerMainThread(SupportPreviewNetwork::handleResponse)
                .add();
    }

    public static void sendToServer(final SupportPreviewRequest request) {
        CHANNEL.sendToServer(request);
    }

    private static void handleRequest(
            final SupportPreviewRequest request,
            final Supplier<NetworkEvent.Context> contextSupplier
    ) {
        final ServerPlayer sender = contextSupplier.get().getSender();
        if (sender == null) {
            return;
        }
        final SupportPreviewResponse response = SupportPreviewServer.evaluate(sender, request);
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> sender), response);
    }

    private static void handleResponse(
            final SupportPreviewResponse response,
            final Supplier<NetworkEvent.Context> contextSupplier
    ) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                Minecraft.getInstance().execute(() -> SupportPreviewClient.accept(response)));
    }
}
