package io.appform.ranger.core.finder.nodeselector;

import io.appform.ranger.core.model.ServiceNode;

/** Linear warm-up progress shared by warm-up aware selectors and weight enrichers. */
public final class WarmupProgress {

    private WarmupProgress() {
    }

    /**
     * Returns the node's healthy age as a fraction of the warm-up duration, clamped to [0, 1].
     * Future healthy timestamps yield zero; a zero timestamp is interpreted as the Unix epoch.
     *
     * @param node the node whose healthy timestamp determines its progress
     * @param nowMs the current time in epoch milliseconds
     * @param warmupDurationMs the healthy age at full progress; zero disables warm-up
     * @return the warm-up progress, or one when warm-up is disabled
     */
    public static double of(final ServiceNode<?> node, final long nowMs, final long warmupDurationMs) {
        if (warmupDurationMs <= 0) {
            return 1.0;
        }
        // Subtract as doubles to avoid overflow for extreme timestamps.
        final var healthyAgeMs = Math.max(0.0, (double) nowMs - node.getHealthySinceTimeStamp());
        return Math.min(1.0, healthyAgeMs / warmupDurationMs);
    }
}
