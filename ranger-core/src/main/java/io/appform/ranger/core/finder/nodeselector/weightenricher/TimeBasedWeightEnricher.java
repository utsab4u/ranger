package io.appform.ranger.core.finder.nodeselector.weightenricher;

import io.appform.ranger.core.finder.nodeselector.WarmupProgress;
import io.appform.ranger.core.model.ServiceNode;

import java.time.Clock;
import java.util.Objects;

public class TimeBasedWeightEnricher<T> implements WeightEnricher<T> {

    private final long minNodeAgeMs;
    private final Clock clock;

    /**
     * Creates a linear warm-up enricher that measures node age against the system UTC clock.
     *
     * @param minNodeAgeMs healthy age at full weight; zero disables warm-up
     * @throws IllegalArgumentException if the duration is negative
     */
    public TimeBasedWeightEnricher(final long minNodeAgeMs) {
        this(minNodeAgeMs, Clock.systemUTC());
    }

    /**
     * Creates a linear warm-up enricher that measures node age against the given clock.
     *
     * @param minNodeAgeMs healthy age at full weight; zero disables warm-up
     * @param clock the time source used to compute healthy age
     * @throws IllegalArgumentException if the duration is negative
     * @throws NullPointerException if the clock is null
     */
    public TimeBasedWeightEnricher(final long minNodeAgeMs, final Clock clock) {
        if (minNodeAgeMs < 0) {
            throw new IllegalArgumentException("minNodeAgeMs must be non-negative");
        }
        this.minNodeAgeMs = minNodeAgeMs;
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    /**
     * Returns the healthy age fraction, clamped to [0, 1].
     * Future timestamps receive zero weight during warm-up. A zero timestamp is
     * interpreted as the Unix epoch, preserving the existing timestamp convention.
     *
     * @param node the node whose healthy timestamp determines its warm-up progress
     * @return the linear time-based weight, or one when warm-up is disabled
     */
    @Override
    public double enrichWeight(final ServiceNode<T> node) {
        if (minNodeAgeMs == 0) {
            return 1.0;
        }
        return WarmupProgress.of(node, clock.millis(), minNodeAgeMs);
    }
}
