package io.appform.ranger.core.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.OptBoolean;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;

import static io.appform.ranger.core.finder.nodeselector.WeightedRandomServiceNodeSelector.DEFAULT_MIN_NODE_AGE;
import static io.appform.ranger.core.finder.nodeselector.WeightedRandomServiceNodeSelector.DEFAULT_WEIGHTED_SELECTION_MIN_NODES_THRESHOLD;

/** Configuration for weighted selection using routing weights and linear healthy-age warm-up. */
@Data
@EqualsAndHashCode(callSuper = false)
@ToString(callSuper = false)
@NoArgsConstructor
@AllArgsConstructor
@Builder
// weightBoostMultiplier was removed; ignore it so existing configs that still set it keep deserializing.
@JsonIgnoreProperties({"weightBoostMultiplier"})
@JsonTypeName("weighted")
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type", requireTypeIdForSubtypes = OptBoolean.FALSE)
public class WeightedNodeSelectorConfig extends NodeSelectorConfig {
    /** Lowest supported {@code minNodeAgeMs}; zero disables warm-up. */
    public static final long MIN_NODE_AGE_MS_LOWER_BOUND = 0L;
    /** Highest supported {@code minNodeAgeMs} (10 minutes). */
    public static final long MIN_NODE_AGE_MS_UPPER_BOUND = 600_000L;
    /** Lowest supported {@code weightedSelectionThreshold}. */
    public static final int WEIGHTED_SELECTION_THRESHOLD_LOWER_BOUND = 1;
    /** Highest supported {@code weightedSelectionThreshold}. */
    public static final int WEIGHTED_SELECTION_THRESHOLD_UPPER_BOUND = 50;

    /** Healthy age at full linear warm-up weight; zero disables warm-up. */
    @Min(MIN_NODE_AGE_MS_LOWER_BOUND)
    @Max(MIN_NODE_AGE_MS_UPPER_BOUND)
    @Builder.Default
    private long minNodeAgeMs = DEFAULT_MIN_NODE_AGE;

    @Min(WEIGHTED_SELECTION_THRESHOLD_LOWER_BOUND)
    @Max(WEIGHTED_SELECTION_THRESHOLD_UPPER_BOUND)
    @Builder.Default
    private int weightedSelectionThreshold = DEFAULT_WEIGHTED_SELECTION_MIN_NODES_THRESHOLD;

    /**
     * Validates the warm-up duration and the threshold for exact pool-wide selection.
     *
     * @throws IllegalArgumentException if either setting is outside its supported range
     */
    public void validate() {
        if (this.getMinNodeAgeMs() < MIN_NODE_AGE_MS_LOWER_BOUND
                || this.getMinNodeAgeMs() > MIN_NODE_AGE_MS_UPPER_BOUND) {
            throw new IllegalArgumentException("minNodeAgeMs must be between " + MIN_NODE_AGE_MS_LOWER_BOUND
                                                       + " and " + MIN_NODE_AGE_MS_UPPER_BOUND);
        }
        if (this.getWeightedSelectionThreshold() < WEIGHTED_SELECTION_THRESHOLD_LOWER_BOUND
                || this.getWeightedSelectionThreshold() > WEIGHTED_SELECTION_THRESHOLD_UPPER_BOUND) {
            throw new IllegalArgumentException("weightedSelectionThreshold must be between "
                                                       + WEIGHTED_SELECTION_THRESHOLD_LOWER_BOUND
                                                       + " and " + WEIGHTED_SELECTION_THRESHOLD_UPPER_BOUND);
        }
    }

    /** {@inheritDoc} */
    @Override
    public <R> R accept(final NodeSelectorConfigVisitor<R> visitor) {
        return visitor.visit(this);
    }
}
