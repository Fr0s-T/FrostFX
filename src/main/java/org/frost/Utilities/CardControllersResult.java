package org.frost.Utilities;

import javafx.scene.Node;

import java.util.List;
import java.util.Objects;

/**
 * Internal data transfer object for card controller creation results.
 *
 * <p>This class maintains strict order between controllers, nodes, and initial data items.
 * Correct ordering is critical for proper reattachment and lifecycle management in the framework.</p>
 *
 * <p>Usage Notes:</p>
 * <ul>
 *   <li>All input lists must be non-null and have equal sizes.</li>
 *   <li>The lists are copied internally to prevent external modification.</li>
 *   <li>Optional debug summary can be printed manually for inspection.</li>
 * </ul>
 *
 * @param <T> type of data items
 * @param <C> type of card controllers
 */
public class CardControllersResult<T, C> {

    private final List<C> controllers;
    private final List<Node> nodes;
    private final List<T> initialData;

    /**
     * Constructs a new CardControllersResult with ordered components.
     *
     * <p>Validates that all lists are non-null and of the same size. Fails fast if the
     * contract is violated to prevent inconsistent state during UI reattachment.</p>
     *
     * @param controllers list of controllers in creation order (cannot be null)
     * @param nodes list of root nodes corresponding to controllers (cannot be null)
     * @param initialData list of data items corresponding to controllers (cannot be null)
     * @throws NullPointerException if any input list is null
     * @throws IllegalArgumentException if the lists are of different sizes
     */
    public CardControllersResult(List<C> controllers, List<Node> nodes, List<T> initialData) {
        Objects.requireNonNull(controllers, "controllers list cannot be null");
        Objects.requireNonNull(nodes, "nodes list cannot be null");
        Objects.requireNonNull(initialData, "initialData list cannot be null");

        if (controllers.size() != nodes.size() || controllers.size() != initialData.size()) {
            throw new IllegalArgumentException(
                    String.format("Size mismatch: controllers=%d, nodes=%d, initialData=%d",
                            controllers.size(), nodes.size(), initialData.size())
            );
        }

        this.controllers = List.copyOf(controllers);
        this.nodes = List.copyOf(nodes);
        this.initialData = List.copyOf(initialData);
    }

    /**
     * Returns the unmodifiable list of controllers.
     *
     * <p>The returned list preserves creation order and is immutable. Callers
     * should not attempt to modify it.</p>
     *
     * @return unmodifiable list of controllers
     */
    public List<C> getControllers() {
        return controllers;
    }

    /**
     * Returns the unmodifiable list of root nodes.
     *
     * <p>The returned list corresponds one-to-one with the controllers and initial data.</p>
     *
     * @return unmodifiable list of nodes
     */
    public List<Node> getNodes() {
        return nodes;
    }

    /**
     * Returns the unmodifiable list of initial data items.
     *
     * <p>The returned list corresponds one-to-one with the controllers and nodes.</p>
     *
     * @return unmodifiable list of initial data items
     */
    public List<T> getInitialData() {
        return initialData;
    }

    /**
     * Returns the number of controllers/nodes/data items in this result.
     *
     * @return the size of the result
     */
    public int size() {
        return controllers.size();
    }

    /**
     * Prints a detailed debug summary of all controllers, their nodes, and corresponding data.
     *
     * <p>This method is intended for manual debugging. Call only when you need
     * to inspect the state of the controllers result. The method will print to the console.</p>
     */
    public void logDebugSummary() {
        System.out.println("CardControllersResult Debug Summary: total items = " + size());
        for (int i = 0; i < controllers.size(); i++) {
            C controller = controllers.get(i);
            Node node = nodes.get(i);
            T data = initialData.get(i);
            System.out.printf("Index %d: Controller=%s, Node=%s, Data=%s%n",
                    i,
                    controller != null ? controller.getClass().getSimpleName() : "null",
                    node != null ? node.getClass().getSimpleName() : "null",
                    data);
        }
        System.out.println("End of Debug Summary");
    }
}
