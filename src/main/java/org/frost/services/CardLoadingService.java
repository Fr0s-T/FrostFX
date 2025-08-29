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
 * This service operates statelessly - all required context is passed as method parameters,
 * ensuring thread safety and clean separation of concerns.
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
     * @param cardName the registered name of the card to load
     * @param anchorPaneName the name of the target anchor panel
     * @param stage the stage containing the panel
     * @param cardPathsMap the registry of card names to FXML paths
     * @param stagePanels the registry of stage panels
     * @return the controller instance of the loaded card
     * @throws IllegalArgumentException if the card or panel is not registered
     * @throws RuntimeException if FXML loading fails
     */
    public <T> T loadCard(String cardName, String anchorPaneName, Stage stage,
                          Map<String, String> cardPathsMap,
                          Map<Stage, Map<String, AnchorPane>> stagePanels) {
        validateStageAndRegistration(cardName, anchorPaneName, stage, cardPathsMap, stagePanels);

        try {
            AnchorPane targetPanel = stagePanels.get(stage).get(anchorPaneName);
            String path = cardPathsMap.get(cardName);
            return mountIntoPanel(targetPanel, path, cardName, stage);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load card '" + cardName + "'", e);
        }
    }

    // ==================== MULTIPLE CARDS LOADING ====================

    /**
     * Loads multiple cards into a container with specified spacing margins.
     *
     * @param <T> the type of data items
     * @param containerName the name of the target container
     * @param items the data items to populate the cards
     * @param cardFxmlPath the FXML path for the card template
     * @param dataSetter callback for injecting data into card controllers
     * @param stage the stage containing the container
     * @param horizontalMargin horizontal spacing between cards
     * @param verticalMargin vertical spacing between cards
     * @param stageContainers the registry of stage containers
     * @throws IllegalArgumentException if the container is not registered
     */
    public <T> void loadCards(String containerName, List<T> items, String cardFxmlPath,
                              BiConsumer<Object, T> dataSetter, Stage stage,
                              double horizontalMargin, double verticalMargin,
                              Map<Stage, Map<String, Pane>> stageContainers) {
        Pane container = validateAndGetContainer(containerName, stage, stageContainers);
        loadCardsInternal(items, container, cardFxmlPath, dataSetter, horizontalMargin, verticalMargin);
    }

    /**
     * Loads multiple cards into a container with uniform spacing margins.
     *
     * @param <T> the type of data items
     * @param containerName the name of the target container
     * @param items the data items to populate the cards
     * @param cardFxmlPath the FXML path for the card template
     * @param dataSetter callback for injecting data into card controllers
     * @param stage the stage containing the container
     * @param margin uniform spacing applied to both axes
     * @param stageContainers the registry of stage containers
     */
    public <T> void loadCards(String containerName, List<T> items, String cardFxmlPath,
                              BiConsumer<Object, T> dataSetter, Stage stage, double margin,
                              Map<Stage, Map<String, Pane>> stageContainers) {
        loadCards(containerName, items, cardFxmlPath, dataSetter, stage, margin, margin, stageContainers);
    }

    /**
     * Loads multiple cards into a container with no spacing margins.
     *
     * @param <T> the type of data items
     * @param containerName the name of the target container
     * @param items the data items to populate the cards
     * @param cardFxmlPath the FXML path for the card template
     * @param dataSetter callback for injecting data into card controllers
     * @param stage the stage containing the container
     * @param stageContainers the registry of stage containers
     */
    public <T> void loadCards(String containerName, List<T> items, String cardFxmlPath,
                              BiConsumer<Object, T> dataSetter, Stage stage,
                              Map<Stage, Map<String, Pane>> stageContainers) {
        loadCards(containerName, items, cardFxmlPath, dataSetter, stage, 0, 0, stageContainers);
    }

    /**
     * Loads multiple cards and returns their controller instances for external management.
     * Enables direct controller manipulation after loading.
     *
     * @param <T> the type of data items
     * @param <C> the type of card controllers
     * @param containerName the name of the target container
     * @param items the data items to populate the cards
     * @param cardFxmlPath the FXML path for the card template
     * @param dataSetter callback for injecting data into card controllers
     * @param stage the stage containing the container
     * @param horizontalMargin horizontal spacing between cards
     * @param verticalMargin vertical spacing between cards
     * @param stageContainers the registry of stage containers
     * @return a list of card controller instances
     */
    public <T, C> List<C> loadCardsWithControllers(String containerName, List<T> items, String cardFxmlPath,
                                                   BiConsumer<C, T> dataSetter, Stage stage,
                                                   double horizontalMargin, double verticalMargin,
                                                   Map<Stage, Map<String, Pane>> stageContainers) {
        Pane container = validateAndGetContainer(containerName, stage, stageContainers);
        return loadCardsInternalWithControllers(items, container, cardFxmlPath, dataSetter,
                horizontalMargin, verticalMargin);
    }

    // ==================== VALIDATION HELPERS ====================

    /**
     * Validates that the specified card and panel are registered for the given stage.
     *
     * @param cardName the card name to validate
     * @param anchorPaneName the panel name to validate
     * @param stage the stage to check registrations against
     * @param cardPathsMap the card registry
     * @param stagePanels the panel registry
     * @throws IllegalArgumentException if validation fails
     */
    private void validateStageAndRegistration(String cardName, String anchorPaneName, Stage stage,
                                              Map<String, String> cardPathsMap,
                                              Map<Stage, Map<String, AnchorPane>> stagePanels) {
        if (stage == null) {
            throw new IllegalArgumentException("Stage cannot be null");
        }

        Map<String, AnchorPane> panels = stagePanels.get(stage);
        if (panels == null || !panels.containsKey(anchorPaneName)) {
            throw new IllegalStateException("No dynamic panel '" + anchorPaneName + "' registered for stage '"
                    + stage.getTitle() + "'. Call registerDynamicPanel('" + anchorPaneName + "', panel, stage) first.");
        }

        if (!cardPathsMap.containsKey(cardName)) {
            throw new IllegalArgumentException("Card '" + cardName + "' is not registered. "
                    + "Call registerCard('" + cardName + "', '/path/to/fxml.fxml') first.");
        }
    }

    /**
     * Validates and retrieves a container from the registry.
     *
     * @param containerName the container name to retrieve
     * @param stage the stage containing the container
     * @param stageContainers the container registry
     * @return the requested Pane container
     * @throws IllegalArgumentException if the container is not found
     */
    private Pane validateAndGetContainer(String containerName, Stage stage,
                                         Map<Stage, Map<String, Pane>> stageContainers) {
        if (stage == null) {
            throw new IllegalArgumentException("Stage cannot be null");
        }

        Map<String, Pane> containers = stageContainers.get(stage);
        if (containers == null || !containers.containsKey(containerName)) {
            throw new IllegalArgumentException("Container '" + containerName + "' is not registered for stage '"
                    + stage.getTitle() + "'. Call registerContainer('" + containerName + "', containerPane, stage) first.");
        }

        return containers.get(containerName);
    }

    // ==================== CORE IMPLEMENTATION METHODS ====================

    /**
     * Core implementation for mounting a single card into an anchor panel.
     * Ensures proper layout constraints and thread safety.
     *
     * @param <T> the type of the card's controller
     * @param targetPanel the anchor panel to receive the card
     * @param resourcePath the FXML resource path
     * @param logicalName logical name for error reporting
     * @param stage the stage for scene validation
     * @return the loaded card's controller
     * @throws IOException if FXML loading fails
     * @throws IllegalStateException if the panel is not in the stage's scene
     */
    private <T> T mountIntoPanel(AnchorPane targetPanel, String resourcePath,
                                 String logicalName, Stage stage) throws IOException {
        if (targetPanel == null) throw new IllegalArgumentException("Target panel cannot be null");
        if (stage == null) throw new IllegalArgumentException("Stage cannot be null");

        URL url = SceneManager.class.getResource(resourcePath);
        if (url == null) {
            throw new IllegalArgumentException("FXML not found: " + resourcePath
                    + ". Check that the file exists in your resources directory.");
        }

        FXMLLoader loader = new FXMLLoader(url);
        Parent card = loader.load();
        Object loadedController = loader.getController();

        runOnFxThread(() -> {
            if (targetPanel.getScene() != stage.getScene()) {
                throw new IllegalStateException(
                        "Target panel '" + logicalName + "' is not in the active scene for stage '"
                                + stage.getTitle() + "'. Ensure the panel belongs to the correct stage's scene."
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
     * Clears the container and populates it with new cards.
     *
     * @param <T> the type of data items
     * @param container the target container
     * @param items the data items for card population
     * @param cardFxmlPath the FXML template path
     * @param dataSetter data injection callback
     * @param horizontalMargin horizontal spacing
     * @param verticalMargin vertical spacing
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
     * Enables external controller management for dynamic updates.
     *
     * @param <T> the type of data items
     * @param <C> the type of card controllers
     * @param items the data items for card population
     * @param container the target container
     * @param cardFxmlPath the FXML template path
     * @param dataSetter data injection callback
     * @param horizontalMargin horizontal spacing
     * @param verticalMargin vertical spacing
     * @return list of card controller instances
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

    /**
     * Adds spacing elements between cards based on margin requirements.
     *
     * @param container the container to add spacers to
     * @param horizontalMargin horizontal spacing amount
     * @param verticalMargin vertical spacing amount
     */
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

    /**
     * Creates a spacer region for card separation.
     *
     * @param size the size of the spacer in pixels
     * @param isHorizontal true for horizontal spacer, false for vertical
     * @return a configured Region for spacing
     */
    private Region createSpacer(double size, boolean isHorizontal) {
        Region spacer = new Region();

        if (isHorizontal) {
            spacer.setMinWidth(size);
            spacer.setPrefWidth(size);
            spacer.setMaxWidth(size);
            spacer.setMinHeight(1);
            spacer.setPrefHeight(Region.USE_COMPUTED_SIZE);
            spacer.setMaxHeight(Double.MAX_VALUE);
        } else {
            spacer.setMinHeight(size);
            spacer.setPrefHeight(size);
            spacer.setMaxHeight(size);
            spacer.setMinWidth(1);
            spacer.setPrefWidth(Region.USE_COMPUTED_SIZE);
            spacer.setMaxWidth(Double.MAX_VALUE);
        }

        spacer.setStyle("-fx-background-color: lightgray; -fx-opacity: 0.3;");

        return spacer;
    }
}