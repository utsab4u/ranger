package io.appform.ranger.core.finder.nodeselector;

import io.appform.ranger.core.finder.nodeselector.weightenricher.RoutingWeightEnricher;
import io.appform.ranger.core.finder.nodeselector.weightenricher.TimeBasedWeightEnricher;
import io.appform.ranger.core.finder.nodeselector.weightenricher.WeightEnricher;
import io.appform.ranger.core.model.ServiceNode;
import io.appform.ranger.core.model.ServiceNodeSelector;
import io.appform.ranger.core.model.WeightedNodeSelectorConfig;

import java.time.Clock;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

public class WeightedRandomServiceNodeSelector<T> implements ServiceNodeSelector<T> {

    public static final long DEFAULT_MIN_NODE_AGE = 60000L;
    public static final int DEFAULT_WEIGHTED_SELECTION_MIN_NODES_THRESHOLD = 10;
    private final ServiceNodeSelector<T> conditionalSelector;

    /**
     * Creates a weighted selector that measures node warm-up against the system UTC clock.
     *
     * @param weightedNodeSelectorConfig the validated selector configuration
     * @throws IllegalArgumentException if the configuration is out of bounds
     */
    public WeightedRandomServiceNodeSelector(final WeightedNodeSelectorConfig weightedNodeSelectorConfig) {
        this(weightedNodeSelectorConfig, Clock.systemUTC());
    }

    /**
     * Creates a weighted selector that measures node warm-up against the given clock.
     *
     * @param weightedNodeSelectorConfig the validated selector configuration
     * @param clock the time source used for warm-up weighting
     * @throws IllegalArgumentException if the configuration is out of bounds
     * @throws NullPointerException if the clock is null
     */
    public WeightedRandomServiceNodeSelector(final WeightedNodeSelectorConfig weightedNodeSelectorConfig,
                                             final Clock clock) {
        weightedNodeSelectorConfig.validate();
        Objects.requireNonNull(clock, "clock must not be null");
        final Predicate<List<ServiceNode<T>>> condition = serviceNodes ->
                serviceNodes.size() <= weightedNodeSelectorConfig.getWeightedSelectionThreshold();
        final List<WeightEnricher<T>> weightEnrichers = List.of(
                new RoutingWeightEnricher<>(),
                new TimeBasedWeightEnricher<>(weightedNodeSelectorConfig.getMinNodeAgeMs(), clock)
                                                               );

        this.conditionalSelector = new ConditionalNodeSelector<>(
                condition,
                new IterativeWeightedSelector<>(weightEnrichers, clock),
                new PairwiseProbabilisticWeightedSelector<>(weightEnrichers)
        );
    }

    @Override
    public ServiceNode<T> select(final List<ServiceNode<T>> serviceNodes) {
        return conditionalSelector.select(serviceNodes);
    }
}
