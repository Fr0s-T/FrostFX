package org.frost.Utilities;

import javafx.scene.Node;

import java.util.List;

/**
 * <p>Internal data transfer object for card controller creation results.</p>
 * <p>Maintains strict order preservation between controllers, nodes, and initial data items.
 * This is critical for proper reattachment sequence.</p>
 *
 * @param <T> the type of data items
 * @param <C> the type of card controllers
 */
public class CardControllersResult<T, C> {
    private final List<C> controllers;
    private final List<Node> nodes;
    private final List<T> initialData;

    /**
     * Constructs a new result with ordered components.
     *
     * @param controllers the list of controllers in creation order
     * @param nodes the list of root nodes in corresponding order
     * @param initialData the list of data items in corresponding order
     * @throws IllegalArgumentException if lists have different sizes
     */
    public CardControllersResult(List<C> controllers, List<Node> nodes, List<T> initialData) {
        if (controllers.size() != nodes.size() || controllers.size() != initialData.size()) {
            throw new IllegalArgumentException("All lists must have the same size");
        }
        this.controllers = List.copyOf(controllers);
        this.nodes = List.copyOf(nodes);
        this.initialData = List.copyOf(initialData);
    }

    public List<C> getControllers() { return controllers; }
    public List<Node> getNodes() { return nodes; }
    public List<T> getInitialData() { return initialData; }
    public int size() { return controllers.size(); }
}