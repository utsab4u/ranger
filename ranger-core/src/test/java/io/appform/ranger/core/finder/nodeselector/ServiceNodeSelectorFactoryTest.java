package io.appform.ranger.core.finder.nodeselector;

import io.appform.ranger.core.model.ServiceNode;
import io.appform.ranger.core.model.WeightedNodeSelectorConfig;
import io.appform.ranger.core.units.TestNodeData;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ServiceNodeSelectorFactoryTest {

    @Test
    void createsWeightedSelectorForWeightedConfig() {
        final var selector = ServiceNodeSelectorFactory.<TestNodeData>create(new WeightedNodeSelectorConfig());

        assertInstanceOf(WeightedRandomServiceNodeSelector.class, selector);
    }

    @Test
    void createsIndependentInstancesOnEachCall() {
        final var config = new WeightedNodeSelectorConfig();

        assertNotSame(ServiceNodeSelectorFactory.<TestNodeData>create(config),
                      ServiceNodeSelectorFactory.<TestNodeData>create(config));
    }

    @Test
    void createdSelectorSelectsFromGivenNodes() {
        final var node = ServiceNode.<TestNodeData>builder()
                .host("localhost")
                .port(8080)
                .routingWeight(1.0)
                .healthySinceTimeStamp(0L)
                .build();
        final var selector = ServiceNodeSelectorFactory.<TestNodeData>create(new WeightedNodeSelectorConfig());

        assertSame(node, selector.select(List.of(node)));
    }

    @Test
    void rejectsNullConfig() {
        assertThrows(NullPointerException.class, () -> ServiceNodeSelectorFactory.create(null));
    }

    @Test
    void rejectsOutOfBoundsConfig() {
        final var config = WeightedNodeSelectorConfig.builder()
                .minNodeAgeMs(WeightedNodeSelectorConfig.MIN_NODE_AGE_MS_UPPER_BOUND + 1)
                .build();

        assertThrows(IllegalArgumentException.class, () -> ServiceNodeSelectorFactory.create(config));
    }

    @Test
    @SuppressWarnings("deprecation")
    void deprecatedTwoNodeComparisonAliasIsPairwiseSelector() {
        final var selector = new TwoNodeComparisonWeightedSelector<TestNodeData>(List.of());

        assertInstanceOf(PairwiseProbabilisticWeightedSelector.class, selector);
    }
}
