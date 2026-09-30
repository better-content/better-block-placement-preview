package com.bettercontent.betterblockplacementpreview;

import com.llamalad7.mixinextras.MixinExtrasBootstrap;
import com.bettercontent.betterblockplacementpreview.placementpreview.SupportPreviewNetwork;
import com.bettercontent.betterblockplacementpreview.placementpreview.SupportPreviewGameTests;
import net.minecraftforge.event.RegisterGameTestsEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(ModMain.MOD_ID)
public final class ModMain {
    public static final String MOD_ID = "better_block_placement_preview";

    public ModMain() {
        MixinExtrasBootstrap.init();
        SupportPreviewNetwork.initialize();
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::registerTests);
    }
    private void registerTests(RegisterGameTestsEvent event) { event.register(SupportPreviewGameTests.class); }
}
