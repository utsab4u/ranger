package io.appform.ranger.core.finder.nodeselector;

import io.appform.ranger.core.finder.nodeselector.weightenricher.WeightEnricher;

import java.util.List;

/**
 * Compatibility alias for {@link PairwiseProbabilisticWeightedSelector}.
 *
 * @param <T> the node data type
 * @deprecated the selector picks probabilistically from a sampled pair rather than comparing
 *             weights; use {@link PairwiseProbabilisticWeightedSelector} instead.
 */
@Deprecated
public class TwoNodeComparisonWeightedSelector<T> extends PairwiseProbabilisticWeightedSelector<T> {

    /**
     * Creates a selector using the supplied weight enrichers.
     *
     * @param weightAdjusters enrichers producing finite, non-negative adjusted weights
     * @deprecated use {@link PairwiseProbabilisticWeightedSelector#PairwiseProbabilisticWeightedSelector(List)}
     */
    @Deprecated
    public TwoNodeComparisonWeightedSelector(final List<WeightEnricher<T>> weightAdjusters) {
        super(weightAdjusters);
    }
}
