package com.bettercontent.betterblockplacementpreview.placementpreview;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.bettercontent.betterblockplacementpreview.diagnostics.PlaytestLog;
import java.util.Properties;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

final class PlacementPreviewCacheTest {
    @Test
    void coalescesUnchangedKeysAndRefreshesAtFiveHundredMilliseconds() {
        final PlacementPreviewCache<String> cache = new PlacementPreviewCache<>();
        final List<Integer> sent = new ArrayList<>();
        cache.observe("same", BlockPos.ZERO, 4, 0, sent::add);
        cache.observe("same", BlockPos.ZERO, 4, 249, sent::add);
        cache.observe("same", BlockPos.ZERO, 4, 499, sent::add);
        assertEquals(List.of(0), sent);
        cache.observe("same", BlockPos.ZERO, 4, 500, sent::add);
        assertEquals(List.of(0, 1), sent);
    }

    @Test
    void changedKeysRespectFourRequestsPerSecond() {
        final PlacementPreviewCache<String> cache = new PlacementPreviewCache<>();
        final List<Integer> sent = new ArrayList<>();
        cache.observe("a", BlockPos.ZERO, 1, 0, sent::add);
        cache.observe("b", BlockPos.ZERO, 2, 100, sent::add);
        cache.observe("b", BlockPos.ZERO, 2, 249, sent::add);
        assertEquals(List.of(0), sent);
        cache.observe("b", BlockPos.ZERO, 2, 250, sent::add);
        assertEquals(List.of(0, 1), sent);
    }

    @Test
    void expiresResultsAndRejectsStaleOutOfOrderAndMismatchedResponses() {
        final PlacementPreviewCache<String> cache = new PlacementPreviewCache<>();
        final List<Integer> sent = new ArrayList<>();
        final BlockPos target = new BlockPos(2, 3, 4);
        cache.observe("a", target, 7, 0, sent::add);
        cache.observe("a", target, 7, 500, sent::add);

        assertFalse(cache.accept(new SupportPreviewResponse(0, target, 7, SupportPreviewVerdict.WILL_FALL), 510));
        assertFalse(cache.accept(new SupportPreviewResponse(1, target.above(), 7, SupportPreviewVerdict.WILL_FALL), 510));
        assertFalse(cache.accept(new SupportPreviewResponse(1, target, 8, SupportPreviewVerdict.WILL_FALL), 510));
        assertTrue(cache.accept(new SupportPreviewResponse(1, target, 7, SupportPreviewVerdict.SUPPORTED), 510));
        assertEquals(SupportPreviewVerdict.SUPPORTED, cache.verdict(1_260));
        assertEquals(SupportPreviewVerdict.UNKNOWN, cache.verdict(1_261));
    }

    @Test
    void diagnosticOutcomesAreCausalAndDisconnectDoesNotReuseSequence() {
        final List<String> records = new ArrayList<>();
        PlaytestLog.Sink sink = new PlaytestLog.Sink() {
            public boolean enabled(String category, String level) { return true; }
            public void write(String category, String level, String message) { records.add(message); }
        };
        final PlacementPreviewCache<String> cache = new PlacementPreviewCache<>(
                new PlaytestLog("better_block_placement_preview", sink, () -> 0, new Properties()));
        final List<Integer> sent = new ArrayList<>();
        cache.observe("a", BlockPos.ZERO, 7, 0, sent::add);
        for (int i = 1; i < 100; i++) cache.observe("a", BlockPos.ZERO, 7, i, sent::add);
        assertEquals(1, records.size());
        assertFalse(cache.accept(new SupportPreviewResponse(99, BlockPos.ZERO, 7, SupportPreviewVerdict.SUPPORTED), 100));
        assertTrue(cache.accept(new SupportPreviewResponse(0, BlockPos.ZERO, 7, SupportPreviewVerdict.SUPPORTED), 101));
        assertTrue(records.get(1).contains("reason=stale_or_mismatched"));
        assertTrue(records.get(2).contains("reason=cache_updated"));
        cache.clear();
        assertEquals(SupportPreviewVerdict.UNKNOWN, cache.verdict(102));
        cache.observe("a", BlockPos.ZERO, 7, 102, sent::add);
        assertEquals(List.of(0, 1), sent);
        assertFalse(cache.accept(new SupportPreviewResponse(0, BlockPos.ZERO, 7, SupportPreviewVerdict.SUPPORTED), 103));
        records.clear();
        cache.clear();
        assertThrows(IllegalStateException.class, () -> cache.observe("failed", BlockPos.ZERO, 7, 104,
                sequence -> { throw new IllegalStateException("transport failure"); }));
        assertTrue(records.isEmpty(), "a throwing sender must not be reported as submitted");
    }

    @Test
    void keyChangeInvalidatesAnOtherwiseMatchingLateResponse() {
        final PlacementPreviewCache<String> cache = new PlacementPreviewCache<>();
        final List<Integer> sent = new ArrayList<>();
        cache.observe("item-a", BlockPos.ZERO, 9, 0, sent::add);
        cache.observe("item-b", BlockPos.ZERO, 9, 100, sent::add);
        assertFalse(cache.accept(new SupportPreviewResponse(0, BlockPos.ZERO, 9, SupportPreviewVerdict.SUPPORTED), 110));
        assertEquals(SupportPreviewVerdict.UNKNOWN, cache.verdict(110));
    }
}
