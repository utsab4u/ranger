package io.appform.ranger.core.model;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * Configuration for Ranger node selection, selected by a named JSON/YAML type.
 * Currently supports weighted selection; other selectors remain available programmatically.
 * Applications own the containing configuration and any fallback when this config is absent.
 *
 * <p>Selection is generic over node data and does not depend on a discovery transport
 * or application framework. Programmatic custom selectors can still be supplied directly.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = WeightedNodeSelectorConfig.class, name = "weighted")
})
public abstract class NodeSelectorConfig {

    /**
     * Dispatches to the visitor method for this configuration type.
     * Selector construction lives in the factory, not in the configuration.
     *
     * @param visitor the visitor to dispatch to
     * @param <R> the result type
     * @return the visitor result
     */
    public abstract <R> R accept(NodeSelectorConfigVisitor<R> visitor);
}
