package com.bettercontent.betterblockplacementpreview.placementpreview;

import java.util.Objects;
import java.util.function.IntConsumer;
import net.minecraft.core.BlockPos;

public final class PlacementPreviewCache<K> {
    public static final long MIN_REQUEST_INTERVAL_MILLIS = 250L;
    public static final long REFRESH_INTERVAL_MILLIS = 500L;
    public static final long RESULT_EXPIRY_MILLIS = 750L;

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
        }
    }

    public boolean accept(final SupportPreviewResponse response, final long nowMillis) {
        if (response.sequence() != currentSequence
                || currentTarget == null
                || !currentTarget.equals(response.targetPos())
                || currentStateId != response.blockStateId()) {
            return false;
        }
        verdict = response.verdict();
        resultReceivedAt = nowMillis;
        return true;
    }

    public SupportPreviewVerdict verdict(final long nowMillis) {
        if (resultReceivedAt == Long.MIN_VALUE || elapsed(nowMillis, resultReceivedAt) > RESULT_EXPIRY_MILLIS) {
            return SupportPreviewVerdict.UNKNOWN;
        }
        return verdict;
    }

    private static long elapsed(final long now, final long before) {
        return before == Long.MIN_VALUE ? Long.MAX_VALUE : Math.max(0L, now - before);
    }
}
