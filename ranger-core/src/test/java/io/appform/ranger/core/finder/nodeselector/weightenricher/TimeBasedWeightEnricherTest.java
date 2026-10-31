package io.appform.ranger.core.finder.nodeselector.weightenricher;

import io.appform.ranger.core.model.ServiceNode;
import io.appform.ranger.core.units.TestNodeData;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TimeBasedWeightEnricherTest {

    private static final long NOW_MS = 1_700_000_000_000L;
    private static final long WARMUP_MS = 60_000L;
    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.ofEpochMilli(NOW_MS), ZoneOffset.UTC);

    @Test
    void weightGrowsLinearlyWithHealthyAgeMeasuredByInjectedClock() {
        final var enricher = new TimeBasedWeightEnricher<TestNodeData>(WARMUP_MS, FIXED_CLOCK);

        assertEquals(0.0, enricher.enrichWeight(nodeHealthySince(NOW_MS)));
        assertEquals(0.25, enricher.enrichWeight(nodeHealthySince(NOW_MS - 15_000L)));
        assertEquals(0.5, enricher.enrichWeight(nodeHealthySince(NOW_MS - 30_000L)));
        assertEquals(1.0, enricher.enrichWeight(nodeHealthySince(NOW_MS - WARMUP_MS)));
        assertEquals(1.0, enricher.enrichWeight(nodeHealthySince(NOW_MS - 10 * WARMUP_MS)));
    }

    @Test
    void futureHealthyTimestampGetsZeroWeight() {
        final var enricher = new TimeBasedWeightEnricher<TestNodeData>(WARMUP_MS, FIXED_CLOCK);

        assertEquals(0.0, enricher.enrichWeight(nodeHealthySince(NOW_MS + 5_000L)));
    }

    @Test
    void zeroDurationDisablesWarmup() {
        final var enricher = new TimeBasedWeightEnricher<TestNodeData>(0L, FIXED_CLOCK);

        assertEquals(1.0, enricher.enrichWeight(nodeHealthySince(NOW_MS + 5_000L)));
    }

    @Test
    void rejectsInvalidArguments() {
        assertThrows(IllegalArgumentException.class, () -> new TimeBasedWeightEnricher<TestNodeData>(-1L));
        assertThrows(NullPointerException.class, () -> new TimeBasedWeightEnricher<TestNodeData>(WARMUP_MS, null));
    }

    private static ServiceNode<TestNodeData> nodeHealthySince(final long healthySinceMs) {
        return ServiceNode.<TestNodeData>builder()
                .host("host")
                .port(8080)
                .healthySinceTimeStamp(healthySinceMs)
                .build();
    }
}
