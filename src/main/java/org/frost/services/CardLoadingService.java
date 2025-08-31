package org.frost.services;

import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.stage.Stage;
import org.frost.internal.loaders.SceneManager;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

import static org.frost.internal.loaders.SceneManager.runOnFxThread;

/**
 * Core service implementation for dynamic card loading and management.
 * Handles the actual FXML loading, controller instantiation, and layout composition.
 * <p>
 * This service operates with direct container references for optimal performance.
 * All registry management and validation is handled by the CardLoader facade.
 *
 * @see org.frost.internal.loaders.CardLoader The public API facade that delegates to this service
 */
public class CardLoadingService {

    /**
     * Constructs a new CardLoadingService instance.
     */
    public CardLoadingService() {}

    // ==================== SINGLE CARD LOADING ====================

    /**
     * Loads and mounts a single card into a specified anchor panel.
     *
     * @param <T> the type of the card's controller
     * @param targetPanel the anchor panel to receive the card (must not be null)
     * @param cardFxmlPath the FXML path for the card template (must not be null or empty)
     * @param logicalName logical name for error reporting and logging
     * @param stage the stage for scene validation (must not be null)
     * @return the controller instance of the loaded card
     * @throws java.lang.IllegalArgumentException if any parameter is invalid
     * @throws java.lang.RuntimeException if FXML loading fails
     */
    public <T> T loadCard(AnchorPane targetPanel, String cardFxmlPath,
                          String logicalName, Stage stage) {
        if (targetPanel == null) {
            throw new IllegalArgumentException("Target panel cannot be null");
        }
        if (cardFxmlPath == null || cardFxmlPath.isBlank()) {
            throw new IllegalArgumentException("Card FXML path cannot be null or empty");
        }
        if (stage == null) {
            throw new IllegalArgumentException("Stage cannot be null");
        }

        try {
            return mountIntoPanel(targetPanel, cardFxmlPath, logicalName, stage);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load card '" + logicalName + "' from: " + cardFxmlPath, e);
        }
    }

    // ==================== MULTIPLE CARDS LOADING ====================

    /**
     * Loads multiple cards into a container with specified spacing margins.
     * Clears the container before adding new cards.
     *
     * @param <T> the type of data items
     * @param container the target container to populate with cards (must not be null)
     * @param items the data items to populate the cards (must not be null)
     * @param cardFxmlPath the FXML path for the card template (must not be null or empty)
     * @param dataSetter callback for injecting data into card controllers (can be null)
     * @param horizontalMargin horizontal spacing between cards in pixels
     * @param verticalMargin vertical spacing between cards in pixels
     * @throws java.lang.IllegalArgumentException if container, items, or cardFxmlPath are invalid
     * @throws java.lang.RuntimeException if FXML loading fails
     */
    public <T> void loadCards(Pane container, List<T> items, String cardFxmlPath,
                              BiConsumer<Object, T> dataSetter,
                              double horizontalMargin, double verticalMargin) {
        validateContainerAndPath(container, cardFxmlPath);
        if (items == null) {
            throw new IllegalArgumentException("Items list cannot be null");
        }

        loadCardsInternal(items, container, cardFxmlPath, dataSetter, horizontalMargin, verticalMargin);
    }

    /**
     * Loads multiple cards into a container with uniform spacing margins.
     *
     * @param <T> the type of data items
     * @param container the target container to populate with cards
     * @param items the data items to populate the cards
     * @param cardFxmlPath the FXML path for the card template
     * @param dataSetter callback for injecting data into card controllers
     * @param margin uniform spacing applied to both horizontal and vertical axes
     */
    public <T> void loadCards(Pane container, List<T> items, String cardFxmlPath,
                              BiConsumer<Object, T> dataSetter, double margin) {
        loadCards(container, items, cardFxmlPath, dataSetter, margin, margin);
    }

    /**
     * Loads multiple cards into a container with no spacing margins.
     *
     * @param <T> the type of data items
     * @param container the target container to populate with cards
     * @param items the data items to populate the cards
     * @param cardFxmlPath the FXML path for the card template
     * @param dataSetter callback for injecting data into card controllers
     */
    public <T> void loadCards(Pane container, List<T> items, String cardFxmlPath,
                              BiConsumer<Object, T> dataSetter) {
        loadCards(container, items, cardFxmlPath, dataSetter, 0, 0);
    }

    // ==================== MULTIPLE CARDS WITH CONTROLLER RETURN ====================

    /**
     * Loads multiple cards and returns their controller instances for external management.
     * Clears the container before adding new cards.
     *
     * @param <T> the type of data items
     * @param <C> the type of card controllers
     * @param container the target container to populate with cards (must not be null)
     * @param items the data items to populate the cards (must not be null)
     * @param cardFxmlPath the FXML path for the card template (must not be null or empty)
     * @param dataSetter callback for injecting data into card controllers (can be null)
     * @param horizontalMargin horizontal spacing between cards in pixels
     * @param verticalMargin vertical spacing between cards in pixels
     * @return a list of card controller instances (never null)
     * @throws java.lang.IllegalArgumentException if container, items, or cardFxmlPath are invalid
     * @throws java.lang.RuntimeException if FXML loading fails
     */
    public <T, C> List<C> loadCardsWithControllers(Pane container, List<T> items, String cardFxmlPath,
                                                   BiConsumer<C, T> dataSetter,
                                                   double horizontalMargin, double verticalMargin) {
        validateContainerAndPath(container, cardFxmlPath);
        if (items == null) {
            throw new IllegalArgumentException("Items list cannot be null");
        }

        return loadCardsInternalWithControllers(items, container, cardFxmlPath, dataSetter,
                horizontalMargin, verticalMargin);
    }

    /**
     * Loads multiple cards and returns their controllers with uniform spacing.
     *
     * @param <T> the type of data items
     * @param <C> the type of card controllers
     * @param container the target container to populate with cards
     * @param items the data items to populate the cards
     * @param cardFxmlPath the FXML path for the card template
     * @param dataSetter callback for injecting data into card controllers
     * @param margin uniform spacing applied to both axes
     * @return a list of card controller instances
     */
    public <T, C> List<C> loadCardsWithControllers(Pane container, List<T> items, String cardFxmlPath,
                                                   BiConsumer<C, T> dataSetter, double margin) {
        return loadCardsWithControllers(container, items, cardFxmlPath, dataSetter, margin, margin);
    }

    /**
     * Loads multiple cards and returns their controllers with no spacing.
     *
     * @param <T> the type of data items
     * @param <C> the type of card controllers
     * @param container the target container to populate with cards
     * @param items the data items to populate the cards
     * @param cardFxmlPath the FXML path for the card template
     * @param dataSetter callback for injecting data into card controllers
     * @return a list of card controller instances
     */
    public <T, C> List<C> loadCardsWithControllers(Pane container, List<T> items, String cardFxmlPath,
                                                   BiConsumer<C, T> dataSetter) {
        return loadCardsWithControllers(container, items, cardFxmlPath, dataSetter, 0, 0);
    }

    // ==================== SINGLE CARD ADDITION ====================

    /**
     * Adds a single card to an existing container and returns its controller.
     * Does not clear the container - appends to existing content.
     *
     * @param <T> the type of data items
     * @param <C> the type of card controllers
     * @param container the target container to add the card to (must not be null)
     * @param item the data item for the new card (must not be null)
     * @param cardFxmlPath the FXML path for the card template (must not be null or empty)
     * @param dataSetter callback for injecting data into the card controller (can be null)
     * @return the controller instance of the newly added card
     * @throws java.lang.IllegalArgumentException if container, item, or cardFxmlPath are invalid
     * @throws java.lang.RuntimeException if FXML loading fails
     */
    public <T, C> C addCardToContainer(Pane container, T item, String cardFxmlPath,
                                       BiConsumer<C, T> dataSetter) {
        validateContainerAndPath(container, cardFxmlPath);
        if (item == null) {
            throw new IllegalArgumentException("Item cannot be null");
        }

        try {
            FXMLLoader loader = new FXMLLoader(SceneManager.class.getResource(cardFxmlPath));
            Node card = loader.load();
            @SuppressWarnings("unchecked")
            C controller = (C) loader.getController();

            if (dataSetter != null) {
                dataSetter.accept(controller, item);
            }

            runOnFxThread(() -> container.getChildren().add(card));
            return controller;

        } catch (IOException e) {
            throw new RuntimeException("Failed to load card from: " + cardFxmlPath, e);
        }
    }

    // ==================== VALIDATION HELPERS ====================

    /**
     * Validates that container and FXML path are not null or empty.
     *
     * @param container the container to validate
     * @param cardFxmlPath the FXML path to validate
     * @throws IllegalArgumentException if validation fails
     */
    private void validateContainerAndPath(Pane container, String cardFxmlPath) {
        if (container == null) {
            throw new IllegalArgumentException("Container cannot be null");
        }
        if (cardFxmlPath == null || cardFxmlPath.isBlank()) {
            throw new IllegalArgumentException("Card FXML path cannot be null or empty");
        }
    }

    // ==================== CORE IMPLEMENTATION METHODS ====================

    /**
     * Core implementation for mounting a single card into an anchor panel.
     */
    private <T> T mountIntoPanel(AnchorPane targetPanel, String resourcePath,
                                 String logicalName, Stage stage) throws IOException {
        URL url = SceneManager.class.getResource(resourcePath);
        if (url == null) {
            throw new IllegalArgumentException("FXML not found: " + resourcePath);
        }

        FXMLLoader loader = new FXMLLoader(url);
        Parent card = loader.load();
        Object loadedController = loader.getController();

        runOnFxThread(() -> {
            if (targetPanel.getScene() != stage.getScene()) {
                throw new IllegalStateException(
                        "Target panel '" + logicalName + "' is not in the active scene for stage '"
                                + stage.getTitle() + "'"
                );
            }
            targetPanel.getChildren().setAll(card);
            AnchorPane.setTopAnchor(card, 0.0);
            AnchorPane.setRightAnchor(card, 0.0);
            AnchorPane.setBottomAnchor(card, 0.0);
            AnchorPane.setLeftAnchor(card, 0.0);
        });

        return (T) loadedController;
    }

    /**
     * Core implementation for loading multiple cards without controller return.
     */
    private <T> void loadCardsInternal(List<T> items, Pane container, String cardFxmlPath,
                                       BiConsumer<Object, T> dataSetter,
                                       double horizontalMargin, double verticalMargin) {
        runOnFxThread(() -> {
            container.getChildren().clear();

            for (int i = 0; i < items.size(); i++) {
                try {
                    FXMLLoader loader = new FXMLLoader(SceneManager.class.getResource(cardFxmlPath));
                    Node card = loader.load();

                    if (dataSetter != null) {
                        dataSetter.accept(loader.getController(), items.get(i));
                    }

                    container.getChildren().add(card);

                    if (i < items.size() - 1) {
                        addMarginSpacers(container, horizontalMargin, verticalMargin);
                    }

                } catch (IOException e) {
                    throw new RuntimeException("Failed to load card from: " + cardFxmlPath, e);
                }
            }
        });
    }

    /**
     * Core implementation for loading multiple cards with controller return.
     */
    private <T, C> List<C> loadCardsInternalWithControllers(List<T> items, Pane container, String cardFxmlPath,
                                                            BiConsumer<C, T> dataSetter,
                                                            double horizontalMargin, double verticalMargin) {
        List<C> controllers = new ArrayList<>();

        runOnFxThread(() -> {
            container.getChildren().clear();

            for (int i = 0; i < items.size(); i++) {
                try {
                    FXMLLoader loader = new FXMLLoader(SceneManager.class.getResource(cardFxmlPath));
                    Node card = loader.load();

                    @SuppressWarnings("unchecked")
                    C controller = (C) loader.getController();
                    controllers.add(controller);

                    if (dataSetter != null) {
                        dataSetter.accept(controller, items.get(i));
                    }

                    container.getChildren().add(card);

                    if (i < items.size() - 1) {
                        addMarginSpacers(container, horizontalMargin, verticalMargin);
                    }

                } catch (IOException e) {
                    throw new RuntimeException("Failed to load card from: " + cardFxmlPath, e);
                }
            }
        });

        return controllers;
    }

    // ==================== UTILITY METHODS ====================

    private void addMarginSpacers(Pane container, double horizontalMargin, double verticalMargin) {
        if (horizontalMargin > 0) {
            Region hSpacer = createSpacer(horizontalMargin, true);
            container.getChildren().add(hSpacer);
        }

        if (verticalMargin > 0) {
            Region vSpacer = createSpacer(verticalMargin, false);
            container.getChildren().add(vSpacer);
        }
    }

    private Region createSpacer(double size, boolean isHorizontal) {
        Region spacer = new Region();

        if (isHorizontal) {
            spacer.setMinWidth(size);
            spacer.setPrefWidth(size);
            spacer.setMaxWidth(size);
        } else {
            spacer.setMinHeight(size);
            spacer.setPrefHeight(size);
            spacer.setMaxHeight(size);
        }

        spacer.getStyleClass().add("frostfx-spacer");
        return spacer;
    }
}
