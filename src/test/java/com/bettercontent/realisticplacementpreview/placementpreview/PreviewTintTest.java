package com.bettercontent.realisticplacementpreview.placementpreview;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Test;

final class PreviewTintTest {
    @Test
    void invalidVanillaPlacementRemainsRedForEveryStructuralVerdict() {
        for (final SupportPreviewVerdict verdict : SupportPreviewVerdict.values()) {
            assertEquals(new PreviewTint(0xFF, 0x28, 0x28, 160), PreviewTint.resolve(false, verdict, 0));
        }
    }

    @Test
    void supportedAndFallbackTintsUseTheSpecifiedColours() {
        assertEquals(new PreviewTint(0x40, 0xDC, 0xBE, 128),
                PreviewTint.resolve(true, SupportPreviewVerdict.SUPPORTED, 0));
        assertEquals(new PreviewTint(0xFF, 0xFF, 0xFF, 128),
                PreviewTint.resolve(true, SupportPreviewVerdict.UNMANAGED, 0));
        assertEquals(new PreviewTint(0xFF, 0xFF, 0xFF, 128),
                PreviewTint.resolve(true, SupportPreviewVerdict.UNKNOWN, 0));
    }

    @Test
    void fallingTintPulsesSlowlyWithoutChangingAmber() {
        final PreviewTint low = PreviewTint.resolve(true, SupportPreviewVerdict.WILL_FALL, 1_800);
        final PreviewTint high = PreviewTint.resolve(true, SupportPreviewVerdict.WILL_FALL, 600);
        assertEquals(0xFF, low.red());
        assertEquals(0x96, low.green());
        assertEquals(0x20, low.blue());
        assertNotEquals(low.alpha(), high.alpha());
        assertEquals(88, low.alpha());
        assertEquals(136, high.alpha());
    }
}
