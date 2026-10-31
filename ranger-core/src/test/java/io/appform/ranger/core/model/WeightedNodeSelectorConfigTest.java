package io.appform.ranger.core.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.InvalidTypeIdException;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import org.junit.jupiter.api.Test;

import static io.appform.ranger.core.finder.nodeselector.WeightedRandomServiceNodeSelector.DEFAULT_MIN_NODE_AGE;
import static io.appform.ranger.core.finder.nodeselector.WeightedRandomServiceNodeSelector.DEFAULT_WEIGHTED_SELECTION_MIN_NODES_THRESHOLD;
import static io.appform.ranger.core.model.WeightedNodeSelectorConfig.MIN_NODE_AGE_MS_LOWER_BOUND;
import static io.appform.ranger.core.model.WeightedNodeSelectorConfig.MIN_NODE_AGE_MS_UPPER_BOUND;
import static io.appform.ranger.core.model.WeightedNodeSelectorConfig.WEIGHTED_SELECTION_THRESHOLD_LOWER_BOUND;
import static io.appform.ranger.core.model.WeightedNodeSelectorConfig.WEIGHTED_SELECTION_THRESHOLD_UPPER_BOUND;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

class WeightedNodeSelectorConfigTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void deserializesPolymorphicallyFromWeightedType() throws Exception {
        final var config = mapper.readValue(
                "{\"type\":\"weighted\",\"minNodeAgeMs\":30000,\"weightedSelectionThreshold\":5}",
                NodeSelectorConfig.class);

        final var weighted = assertInstanceOf(WeightedNodeSelectorConfig.class, config);
        assertEquals(30_000L, weighted.getMinNodeAgeMs());
        assertEquals(5, weighted.getWeightedSelectionThreshold());
    }

    @Test
    void appliesDefaultsWhenFieldsAreAbsent() throws Exception {
        final var weighted = assertInstanceOf(
                WeightedNodeSelectorConfig.class,
                mapper.readValue("{\"type\":\"weighted\"}", NodeSelectorConfig.class));

        assertEquals(DEFAULT_MIN_NODE_AGE, weighted.getMinNodeAgeMs());
        assertEquals(DEFAULT_WEIGHTED_SELECTION_MIN_NODES_THRESHOLD, weighted.getWeightedSelectionThreshold());
    }

    @Test
    void ignoresRemovedWeightBoostMultiplier() throws Exception {
        final var config = mapper.readValue(
                "{\"type\":\"weighted\",\"minNodeAgeMs\":30000,\"weightBoostMultiplier\":2.5}",
                NodeSelectorConfig.class);

        assertEquals(30_000L, assertInstanceOf(WeightedNodeSelectorConfig.class, config).getMinNodeAgeMs());
    }

    @Test
    void stillRejectsOtherUnknownProperties() {
        assertThrows(UnrecognizedPropertyException.class, () -> mapper.readValue(
                "{\"type\":\"weighted\",\"minNodeAgMs\":30000}", NodeSelectorConfig.class));
    }

    @Test
    void deserializesConcreteTypeWithoutTypeProperty() throws Exception {
        final var config = mapper.readValue("{\"minNodeAgeMs\":30000}", WeightedNodeSelectorConfig.class);

        assertEquals(30_000L, config.getMinNodeAgeMs());
        assertEquals(DEFAULT_WEIGHTED_SELECTION_MIN_NODES_THRESHOLD, config.getWeightedSelectionThreshold());
    }

    @Test
    void requiresTypePropertyForAbstractConfig() {
        assertThrows(InvalidTypeIdException.class,
                     () -> mapper.readValue("{\"minNodeAgeMs\":30000}", NodeSelectorConfig.class));
    }

    @Test
    void rejectsUnknownType() {
        assertThrows(InvalidTypeIdException.class,
                     () -> mapper.readValue("{\"type\":\"unknown\"}", NodeSelectorConfig.class));
    }

    @Test
    void roundTripsThroughAbstractType() throws Exception {
        final var original = WeightedNodeSelectorConfig.builder()
                .minNodeAgeMs(45_000L)
                .weightedSelectionThreshold(7)
                .build();

        final var json = mapper.writeValueAsString(original);

        assertEquals(original, mapper.readValue(json, NodeSelectorConfig.class));
    }

    @Test
    void validateAcceptsInclusiveBounds() {
        assertDoesNotThrow(() -> config(MIN_NODE_AGE_MS_LOWER_BOUND, WEIGHTED_SELECTION_THRESHOLD_LOWER_BOUND)
                .validate());
        assertDoesNotThrow(() -> config(MIN_NODE_AGE_MS_UPPER_BOUND, WEIGHTED_SELECTION_THRESHOLD_UPPER_BOUND)
                .validate());
    }

    @Test
    void validateRejectsMinNodeAgeOutsideBounds() {
        final var belowLower = config(MIN_NODE_AGE_MS_LOWER_BOUND - 1, WEIGHTED_SELECTION_THRESHOLD_LOWER_BOUND);
        final var aboveUpper = config(MIN_NODE_AGE_MS_UPPER_BOUND + 1, WEIGHTED_SELECTION_THRESHOLD_LOWER_BOUND);

        assertThrows(IllegalArgumentException.class, belowLower::validate);
        assertThrows(IllegalArgumentException.class, aboveUpper::validate);
    }

    @Test
    void validateRejectsThresholdOutsideBounds() {
        final var belowLower = config(MIN_NODE_AGE_MS_LOWER_BOUND, WEIGHTED_SELECTION_THRESHOLD_LOWER_BOUND - 1);
        final var aboveUpper = config(MIN_NODE_AGE_MS_LOWER_BOUND, WEIGHTED_SELECTION_THRESHOLD_UPPER_BOUND + 1);

        assertThrows(IllegalArgumentException.class, belowLower::validate);
        assertThrows(IllegalArgumentException.class, aboveUpper::validate);
    }

    private static WeightedNodeSelectorConfig config(final long minNodeAgeMs, final int threshold) {
        return WeightedNodeSelectorConfig.builder()
                .minNodeAgeMs(minNodeAgeMs)
                .weightedSelectionThreshold(threshold)
                .build();
    }
}
