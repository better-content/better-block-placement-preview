package com.bettercontent.betterblockplacementpreview.placementpreview;

import com.bettercontent.betterblockplacementpreview.ModMain;
import com.bettercontent.betterblockplacementpreview.diagnostics.PlaytestLog;
import java.util.Objects;
import java.util.function.IntConsumer;
import net.minecraft.core.BlockPos;

public final class PlacementPreviewCache<K> {
    public static final long MIN_REQUEST_INTERVAL_MILLIS = 250L;
    public static final long REFRESH_INTERVAL_MILLIS = 500L;
    public static final long RESULT_EXPIRY_MILLIS = 750L;

    private final PlaytestLog diagnostics;
    public PlacementPreviewCache() { this(ModMain.PLAYTEST); }
    PlacementPreviewCache(PlaytestLog diagnostics) { this.diagnostics = diagnostics; }

    private K currentKey;
    private BlockPos currentTarget;
    private int currentStateId = -1;
    private int currentSequence = -1;
    private int nextSequence;
    private long lastRequestAt = Long.MIN_VALUE;
    private long resultReceivedAt = Long.MIN_VALUE;
    private SupportPreviewVerdict verdict = SupportPreviewVerdict.UNKNOWN;

    public void observe(
            final K key,
            final BlockPos target,
            final int stateId,
            final long nowMillis,
            final IntConsumer requestSender
    ) {
        if (!Objects.equals(currentKey, key)) {
            currentKey = key;
            currentTarget = target.immutable();
            currentStateId = stateId;
            currentSequence = -1;
            resultReceivedAt = Long.MIN_VALUE;
            verdict = SupportPreviewVerdict.UNKNOWN;
        }
        final boolean neverRequested = currentSequence < 0;
        final boolean refreshDue = elapsed(nowMillis, lastRequestAt) >= REFRESH_INTERVAL_MILLIS;
        final boolean rateLimitAllows = elapsed(nowMillis, lastRequestAt) >= MIN_REQUEST_INTERVAL_MILLIS;
        if ((neverRequested || refreshDue) && rateLimitAllows) {
            currentSequence = nextSequence++;
            lastRequestAt = nowMillis;
            requestSender.accept(currentSequence);
            if (diagnostics.enabled("request")) {
                diagnostics.event("request", "preview.requested", "client", "submitted", "sender_returned",
                        () -> PlaytestLog.fields("sequence", currentSequence));
            }
        }
    }

    public boolean accept(final SupportPreviewResponse response, final long nowMillis) {
        if (response.sequence() != currentSequence
                || currentTarget == null
                || !currentTarget.equals(response.targetPos())
                || currentStateId != response.blockStateId()) {
            responseOutcome(response, "rejected", "stale_or_mismatched");
            return false;
        }
        verdict = response.verdict();
        resultReceivedAt = nowMillis;
        responseOutcome(response, "accepted", "cache_updated");
        return true;
    }

    public SupportPreviewVerdict verdict(final long nowMillis) {
        if (resultReceivedAt == Long.MIN_VALUE || elapsed(nowMillis, resultReceivedAt) > RESULT_EXPIRY_MILLIS) {
            return SupportPreviewVerdict.UNKNOWN;
        }
        return verdict;
    }

    /** A disconnected client must not retain an advisory result or pending target. Sequence never rewinds. */
    public void clear() {
        currentKey = null;
        currentTarget = null;
        currentStateId = -1;
        currentSequence = -1;
        lastRequestAt = resultReceivedAt = Long.MIN_VALUE;
        verdict = SupportPreviewVerdict.UNKNOWN;
    }

    private void responseOutcome(SupportPreviewResponse response, String result, String reason) {
        if (diagnostics.enabled("response")) {
            diagnostics.event("response", "preview.response", "client", result, reason,
                    () -> PlaytestLog.fields("sequence", response.sequence(), "verdict", response.verdict()));
        }
    }

    private static long elapsed(final long now, final long before) {
        return before == Long.MIN_VALUE ? Long.MAX_VALUE : Math.max(0L, now - before);
    }
}
