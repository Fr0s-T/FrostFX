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
 * Internal service handling the actual loading logic.
 * All methods receive all necessary data as parameters.
 */
public class CardLoadingService {

    public CardLoadingService() {}

    // ==================== SINGLE CARD LOADING ====================

    /**
     * The REAL implementation of loading a single card
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
     * Load multiple cards with dual margins
     */
    public <T> void loadCards(String containerName, List<T> items, String cardFxmlPath,
                              BiConsumer<Object, T> dataSetter, Stage stage,
                              double horizontalMargin, double verticalMargin,
                              Map<Stage, Map<String, Pane>> stageContainers) {
        Pane container = validateAndGetContainer(containerName, stage, stageContainers);
        loadCardsInternal(items, container, cardFxmlPath, dataSetter, horizontalMargin, verticalMargin);
    }

    /**
     * Load multiple cards with single margin
     */
    public <T> void loadCards(String containerName, List<T> items, String cardFxmlPath,
                              BiConsumer<Object, T> dataSetter, Stage stage, double margin,
                              Map<Stage, Map<String, Pane>> stageContainers) {
        loadCards(containerName, items, cardFxmlPath, dataSetter, stage, margin, margin, stageContainers);
    }

    /**
     * Load multiple cards with no margins
     */
    public <T> void loadCards(String containerName, List<T> items, String cardFxmlPath,
                              BiConsumer<Object, T> dataSetter, Stage stage,
                              Map<Stage, Map<String, Pane>> stageContainers) {
        loadCards(containerName, items, cardFxmlPath, dataSetter, stage, 0, 0, stageContainers);
    }

    /**
     * NEW: Load multiple cards and return their controllers
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

    // ==================== PURE LOGIC METHODS ====================

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

                    // Add margin regions between cards
                    if (i < items.size() - 1) {
                        addMarginSpacers(container, horizontalMargin, verticalMargin);
                    }

                } catch (IOException e) {
                    throw new RuntimeException("Failed to load card from: " + cardFxmlPath, e);
                }
            }
        });
    }

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

                    // Add margin regions between cards
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

        // Remove or keep the debug style based on needs
        spacer.setStyle("-fx-background-color: lightgray; -fx-opacity: 0.3;");

        return spacer;
    }
}