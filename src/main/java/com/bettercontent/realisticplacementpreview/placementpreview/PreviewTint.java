package com.bettercontent.realisticplacementpreview.placementpreview;

public record PreviewTint(int red, int green, int blue, int alpha) {
    public static final int VALID_ALPHA = 128;
    public static final int INVALID_ALPHA = 160;

    public static PreviewTint resolve(
            final boolean vanillaPlacementValid,
            final SupportPreviewVerdict verdict,
            final long nowMillis
    ) {
        if (!vanillaPlacementValid) {
            return new PreviewTint(0xFF, 0x28, 0x28, INVALID_ALPHA);
        }
        return switch (verdict) {
            case SUPPORTED -> new PreviewTint(0x40, 0xDC, 0xBE, VALID_ALPHA);
            case WILL_FALL -> new PreviewTint(0xFF, 0x96, 0x20, pulseAlpha(nowMillis));
            case UNMANAGED, UNKNOWN -> new PreviewTint(0xFF, 0xFF, 0xFF, VALID_ALPHA);
        };
    }

    static int pulseAlpha(final long nowMillis) {
        final double phase = (Math.floorMod(nowMillis, 2_400L) / 2_400.0) * Math.PI * 2.0;
        return 112 + (int) Math.round(Math.sin(phase) * 24.0);
    }
}
