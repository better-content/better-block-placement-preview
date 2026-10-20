package com.bettercontent.betterblockplacementpreview;

import com.llamalad7.mixinextras.MixinExtrasBootstrap;
import com.bettercontent.betterblockplacementpreview.diagnostics.PlaytestLog;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.fml.ModList;
import com.bettercontent.betterblockplacementpreview.placementpreview.SupportPreviewNetwork;
import com.bettercontent.betterblockplacementpreview.placementpreview.SupportPreviewGameTests;
import net.minecraftforge.event.RegisterGameTestsEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(ModMain.MOD_ID)
public final class ModMain {
    public static final String MOD_ID = "better_block_placement_preview";
    public static final PlaytestLog PLAYTEST = new PlaytestLog(MOD_ID);

    public ModMain() {
        MixinExtrasBootstrap.init();
        SupportPreviewNetwork.initialize();
        PLAYTEST.status("common", ModList.get().getModContainerById(MOD_ID)
                .map(mod -> mod.getModInfo().getVersion().toString()).orElse("UNKNOWN"), "UNKNOWN",
                () -> PlaytestLog.fields("enabled", true, "integration", "version_gated",
                        "rotavision_version", version("rotavision"), "rbp_version", version("rbp"),
                        "realisticphysics_version", version("realisticphysics"),
                        "request_interval_ms", 250, "expiry_ms", 750));
        MinecraftForge.EVENT_BUS.addListener(ModMain::serverTick);
        MinecraftForge.EVENT_BUS.addListener(ModMain::serverStopped);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::registerTests);
    }
    private static String version(String modId) {
        return ModList.get().getModContainerById(modId)
                .map(mod -> mod.getModInfo().getVersion().toString()).orElse("absent");
    }
    private static void serverTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && PLAYTEST.enabled("summary")) PLAYTEST.summary("server");
    }
    private static void serverStopped(ServerStoppedEvent event) { PLAYTEST.reset("server"); }
    private void registerTests(RegisterGameTestsEvent event) { event.register(SupportPreviewGameTests.class); }
}
