package io.appform.ranger.core.model;


public interface NodeSelectorConfigVisitor<R> {

    R visit(WeightedNodeSelectorConfig config);

}
