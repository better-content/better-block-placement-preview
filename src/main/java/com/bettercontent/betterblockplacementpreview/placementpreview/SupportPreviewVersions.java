package com.bettercontent.betterblockplacementpreview.placementpreview;

import net.minecraftforge.fml.ModList;

final class SupportPreviewVersions {
    private SupportPreviewVersions() {
    }

    static boolean rotaVisionSupported() {
        return exact("rotavision", "1.0.2");
    }

    static boolean physicsSupported() {
        return rotaVisionSupported()
                && exact("rbp", "1.0.0")
                && exact("realisticphysics", "1.0.1");
    }

    private static boolean exact(final String modId, final String expected) {
        return ModList.get().getModContainerById(modId)
                .map(container -> expected.equals(container.getModInfo().getVersion().toString()))
                .orElse(false);
    }
}
