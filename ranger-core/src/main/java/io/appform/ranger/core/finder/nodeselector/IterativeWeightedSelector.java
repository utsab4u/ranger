package io.appform.ranger.core.finder.nodeselector;

import io.appform.ranger.core.finder.nodeselector.weightenricher.WeightEnricher;
import io.appform.ranger.core.model.ServiceNode;
import lombok.extern.slf4j.Slf4j;

import java.time.Clock;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
public class IterativeWeightedSelector<T> extends AbstractWeightedSelector<T> {

    private static final long ZERO_WEIGHT_WARN_INTERVAL_MS = 60_000L;

    private final AtomicLong lastZeroWeightWarnMs = new AtomicLong(0L);
    private final Clock clock;

    /**
     * Creates a selector that rate-limits zero-weight warnings using the system UTC clock.
     *
     * @param weightAdjusters the enrichers whose product determines each node's weight
     */
    public IterativeWeightedSelector(final List<WeightEnricher<T>> weightAdjusters) {
        this(weightAdjusters, Clock.systemUTC());
    }

    /**
     * Creates a selector that rate-limits zero-weight warnings using the given clock.
     *
     * @param weightAdjusters the enrichers whose product determines each node's weight
     * @param clock the time source used to rate-limit zero-weight warnings
     * @throws NullPointerException if the clock is null
     */
    public IterativeWeightedSelector(final List<WeightEnricher<T>> weightAdjusters, final Clock clock) {
        super(weightAdjusters);
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    /**
     * Selects proportionally to adjusted weights across the pool. If every weight is
     * zero, selects uniformly so a newly healthy pool can still serve requests.
     *
     * @param serviceNodes a non-empty list of eligible nodes
     * @return the selected node
     * @throws IllegalArgumentException if the list is empty
     */
    @Override
    @SuppressWarnings("java:S2245")
    public ServiceNode<T> select(final List<ServiceNode<T>> serviceNodes) {
        final var nodeCount = serviceNodes.size();
        if (nodeCount == 0) {
            throw new IllegalArgumentException("serviceNodes must not be empty");
        }
        final var cumulativeWeights = new double[nodeCount];
        double totalWeight = 0.0;

        for (int i = 0; i < nodeCount; i++) {
            final var node = serviceNodes.get(i);
            final var adjustedWeight = adjustWeight(node);
            totalWeight += adjustedWeight;
            cumulativeWeights[i] = totalWeight;
        }

        final var random = ThreadLocalRandom.current();
        if (totalWeight == 0.0) {
            warnAllZeroWeights(nodeCount);
            return serviceNodes.get(random.nextInt(nodeCount));
        }
        final var randomValue = random.nextDouble(totalWeight);
        // Find the first strictly greater cumulative weight, skipping zero-weight intervals.
        int low = 0;
        int high = nodeCount - 1;
        while (low < high) {
            final var middle = low + (high - low) / 2;
            if (cumulativeWeights[middle] <= randomValue) {
                low = middle + 1;
            } else {
                high = middle;
            }
        }

        return serviceNodes.get(low);
    }

    private void warnAllZeroWeights(final int nodeCount) {
        final var now = clock.millis();
        final var last = lastZeroWeightWarnMs.get();
        if (now - last >= ZERO_WEIGHT_WARN_INTERVAL_MS && lastZeroWeightWarnMs.compareAndSet(last, now)) {
            log.warn("All {} eligible nodes have zero adjusted weight; falling back to uniform random selection",
                     nodeCount);
        }
    }
}
