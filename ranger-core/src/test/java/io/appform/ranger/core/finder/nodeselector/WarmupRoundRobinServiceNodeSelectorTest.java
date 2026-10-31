package io.appform.ranger.core.finder.nodeselector;

import io.appform.ranger.core.model.ServiceNode;
import io.appform.ranger.core.units.TestNodeData;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WarmupRoundRobinServiceNodeSelectorTest {

    private static final long NOW_MS = 1_700_000_000_000L;
    private static final long WARMUP_MS = 60_000L;
    private static final int MAX_ATTEMPTS = 3;
    private static final int ITERATIONS = 300;
    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.ofEpochMilli(NOW_MS), ZoneOffset.UTC);

    @Test
    void fullyWarmNodesAreSelectedInStrictRoundRobin() {
        final var nodes = List.of(warmNode("a"), warmNode("b"), warmNode("c"));
        final var selector = new WarmupRoundRobinServiceNodeSelector<TestNodeData>(WARMUP_MS, MAX_ATTEMPTS,
                                                                                   FIXED_CLOCK);

        final var counts = new HashMap<String, Integer>();
        for (int i = 0; i < ITERATIONS; i++) {
            counts.merge(selector.select(nodes).getHost(), 1, Integer::sum);
        }

        assertEquals(ITERATIONS / nodes.size(), counts.get("a"));
        assertEquals(ITERATIONS / nodes.size(), counts.get("b"));
        assertEquals(ITERATIONS / nodes.size(), counts.get("c"));
    }

    @Test
    void nodeThatJustBecameHealthyIsNeverAcceptedWhileWarmNodesExist() {
        final var coldNode = nodeHealthySince("cold", NOW_MS);
        final var nodes = List.of(warmNode("a"), coldNode, warmNode("b"));
        final var selector = new WarmupRoundRobinServiceNodeSelector<TestNodeData>(WARMUP_MS, MAX_ATTEMPTS,
                                                                                   FIXED_CLOCK);

        for (int i = 0; i < ITERATIONS; i++) {
            assertNotSame(coldNode, selector.select(nodes));
        }
    }

    @Test
    void nodeWithFutureHealthyTimestampIsNeverAcceptedWhileWarmNodesExist() {
        final var futureNode = nodeHealthySince("future", NOW_MS + 5_000L);
        final var nodes = List.of(warmNode("a"), futureNode, warmNode("b"));
        final var selector = new WarmupRoundRobinServiceNodeSelector<TestNodeData>(WARMUP_MS, MAX_ATTEMPTS,
                                                                                   FIXED_CLOCK);

        for (int i = 0; i < ITERATIONS; i++) {
            assertNotSame(futureNode, selector.select(nodes));
        }
    }

    @Test
    void allColdNodesFallBackToUniformSelection() {
        final var nodes = List.of(nodeHealthySince("a", NOW_MS),
                                  nodeHealthySince("b", NOW_MS),
                                  nodeHealthySince("c", NOW_MS));
        final var selector = new WarmupRoundRobinServiceNodeSelector<TestNodeData>(WARMUP_MS, MAX_ATTEMPTS,
                                                                                   FIXED_CLOCK);

        final var selectedHosts = new HashSet<String>();
        for (int i = 0; i < ITERATIONS; i++) {
            final var selected = selector.select(nodes);
            assertTrue(nodes.contains(selected));
            selectedHosts.add(selected.getHost());
        }

        assertEquals(3, selectedHosts.size());
    }

    @Test
    void zeroDurationDisablesWarmupAndDelegatesToRoundRobin() {
        final var nodes = List.of(nodeHealthySince("a", NOW_MS), nodeHealthySince("b", NOW_MS));
        final var selector = new WarmupRoundRobinServiceNodeSelector<TestNodeData>(0L, MAX_ATTEMPTS, FIXED_CLOCK);

        final var first = selector.select(nodes);
        final var second = selector.select(nodes);

        assertNotSame(first, second);
    }

    @Test
    void rejectsInvalidArguments() {
        final List<ServiceNode<TestNodeData>> empty = List.of();
        final var selector = new WarmupRoundRobinServiceNodeSelector<TestNodeData>(WARMUP_MS, MAX_ATTEMPTS,
                                                                                   FIXED_CLOCK);

        assertThrows(IllegalArgumentException.class, () -> selector.select(empty));
        assertThrows(IllegalArgumentException.class,
                     () -> new WarmupRoundRobinServiceNodeSelector<TestNodeData>(-1L, MAX_ATTEMPTS));
        assertThrows(IllegalArgumentException.class,
                     () -> new WarmupRoundRobinServiceNodeSelector<TestNodeData>(WARMUP_MS, 0));
        assertThrows(NullPointerException.class,
                     () -> new WarmupRoundRobinServiceNodeSelector<TestNodeData>(WARMUP_MS, MAX_ATTEMPTS, null));
    }

    private static ServiceNode<TestNodeData> warmNode(final String host) {
        return nodeHealthySince(host, NOW_MS - WARMUP_MS);
    }

    private static ServiceNode<TestNodeData> nodeHealthySince(final String host, final long healthySinceMs) {
        return ServiceNode.<TestNodeData>builder()
                .host(host)
                .port(8080)
                .healthySinceTimeStamp(healthySinceMs)
                .build();
    }
}
