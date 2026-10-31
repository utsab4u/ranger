package io.appform.ranger.core.finder.nodeselector;

import io.appform.ranger.core.finder.nodeselector.weightenricher.RoutingWeightEnricher;
import io.appform.ranger.core.finder.nodeselector.weightenricher.TimeBasedWeightEnricher;
import io.appform.ranger.core.finder.nodeselector.weightenricher.WeightEnricher;
import io.appform.ranger.core.model.ServiceNode;
import io.appform.ranger.core.model.WeightedNodeSelectorConfig;
import io.appform.ranger.core.units.TestNodeData;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Deterministic tests for the weighted selectors: a fixed clock pins each node's
 * warm-up weight to exactly zero or one, so outcomes do not depend on tolerance bands.
 */
class WeightedSelectorsClockTest {

    private static final long NOW_MS = 1_700_000_000_000L;
    private static final long WARMUP_MS = 60_000L;
    private static final int ITERATIONS = 300;
    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.ofEpochMilli(NOW_MS), ZoneOffset.UTC);

    @Test
    void iterativeSelectorNeverPicksNodeThatJustBecameHealthy() {
        final var coldNode = node("cold", NOW_MS, 1.0);
        final var nodes = List.of(node("a", NOW_MS - WARMUP_MS, 1.0), coldNode, node("b", NOW_MS - WARMUP_MS, 1.0));
        final var selector = new IterativeWeightedSelector<>(warmupEnrichers(), FIXED_CLOCK);

        for (int i = 0; i < ITERATIONS; i++) {
            assertNotSame(coldNode, selector.select(nodes));
        }
    }

    @Test
    void iterativeSelectorFallsBackToUniformWhenAllNodesAreCold() {
        final var nodes = List.of(node("a", NOW_MS, 1.0), node("b", NOW_MS, 1.0), node("c", NOW_MS, 1.0));
        final var selector = new IterativeWeightedSelector<>(warmupEnrichers(), FIXED_CLOCK);

        assertAllNodesReachable(nodes, selector::select);
    }

    @Test
    void iterativeSelectorFallsBackToUniformWhenAllRoutingWeightsAreZero() {
        final var warmSince = NOW_MS - WARMUP_MS;
        final var nodes = List.of(node("a", warmSince, 0.0), node("b", warmSince, 0.0), node("c", warmSince, 0.0));
        final var selector = new IterativeWeightedSelector<>(warmupEnrichers(), FIXED_CLOCK);

        assertAllNodesReachable(nodes, selector::select);
    }

    @Test
    void iterativeSelectorRejectsEmptyListAndNullClock() {
        final var selector = new IterativeWeightedSelector<>(warmupEnrichers(), FIXED_CLOCK);
        final List<ServiceNode<TestNodeData>> empty = List.of();

        assertThrows(IllegalArgumentException.class, () -> selector.select(empty));
        assertThrows(NullPointerException.class, () -> new IterativeWeightedSelector<>(warmupEnrichers(), null));
    }

    @Test
    void pairwiseSelectorNeverPicksColdNodeWhenPairedWithWarmNode() {
        final var coldNode = node("cold", NOW_MS, 1.0);
        final var nodes = new ArrayList<ServiceNode<TestNodeData>>();
        nodes.add(coldNode);
        for (int i = 0; i < 5; i++) {
            nodes.add(node("warm-" + i, NOW_MS - WARMUP_MS, 1.0));
        }
        final var selector = new PairwiseProbabilisticWeightedSelector<>(warmupEnrichers());

        for (int i = 0; i < ITERATIONS; i++) {
            assertNotSame(coldNode, selector.select(nodes));
        }
    }

    @Test
    void weightedRandomSelectorUsesInjectedClockOnBothSelectionPaths() {
        final var config = WeightedNodeSelectorConfig.builder()
                .minNodeAgeMs(WARMUP_MS)
                .weightedSelectionThreshold(3)
                .build();
        final var selector = new WeightedRandomServiceNodeSelector<TestNodeData>(config, FIXED_CLOCK);
        final var coldNode = node("cold", NOW_MS, 1.0);

        final var smallPool = List.of(coldNode, node("a", NOW_MS - WARMUP_MS, 1.0));
        final var largePool = new ArrayList<ServiceNode<TestNodeData>>();
        largePool.add(coldNode);
        for (int i = 0; i < 5; i++) {
            largePool.add(node("warm-" + i, NOW_MS - WARMUP_MS, 1.0));
        }

        for (int i = 0; i < ITERATIONS; i++) {
            assertNotSame(coldNode, selector.select(smallPool));
            assertNotSame(coldNode, selector.select(largePool));
        }
    }

    private static List<WeightEnricher<TestNodeData>> warmupEnrichers() {
        return List.of(new RoutingWeightEnricher<>(), new TimeBasedWeightEnricher<>(WARMUP_MS, FIXED_CLOCK));
    }

    private static void assertAllNodesReachable(
            final List<ServiceNode<TestNodeData>> nodes,
            final Function<List<ServiceNode<TestNodeData>>, ServiceNode<TestNodeData>> select) {
        final var selectedHosts = new HashSet<String>();
        for (int i = 0; i < ITERATIONS; i++) {
            final var selected = select.apply(nodes);
            assertTrue(nodes.contains(selected));
            selectedHosts.add(selected.getHost());
        }
        assertEquals(nodes.size(), selectedHosts.size());
    }

    private static ServiceNode<TestNodeData> node(final String host,
                                                  final long healthySinceMs,
                                                  final double routingWeight) {
        return ServiceNode.<TestNodeData>builder()
                .host(host)
                .port(8080)
                .routingWeight(routingWeight)
                .healthySinceTimeStamp(healthySinceMs)
                .build();
    }
}
