package net.magnesiumbackend.core.registry;

import net.magnesiumbackend.core.route.RouteDefinition;
import org.jetbrains.annotations.ApiStatus;

import java.util.HashMap;
import java.util.Map;

@ApiStatus.Internal
public class RouteNode<T> {
    public final Map<String, RouteNode<T>> staticChildren = new HashMap<>();
    public RouteNode<T> variableChild;

    public String variableName;
    public T value;

    @Override
    public String toString() {
        return "RouteNode{" +
            "staticChildren=" + staticChildren +
            ", variableChild=" + variableChild +
            ", variableName='" + variableName + '\'' +
            ", value=" + value +
            '}';
    }
}