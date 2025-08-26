package org.frost.internal.loaders;

import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.function.BiConsumer;

import static org.frost.internal.loaders.SceneManager.runOnFxThread;

public class CardLoader {

    /** Tracks currently displayed panel view (for refresh functionality) */
    private String currentPanelView = "";

    /** Registry of card names to FXML paths */
    private final Map<String, String> cardPathsMap = new HashMap<>();

    /** Cache of registered dynamic panels per stage */
    private final Map<Stage, Map<String, AnchorPane>> stagePanels = new HashMap<>();

    /** Flexible registry for any Pane type per stage */
    private final Map<Stage, Map<String, Pane>> stageContainers = new HashMap<>();

    /** Default stage for simpler usage */
    private Stage primaryStage;

    public CardLoader() { }

    void setPrimaryStage(Stage stage) { this.primaryStage = stage; }

    // ==================== GETTERS ====================

    public String getCurrentPanelView() { return currentPanelView; }

    public Set<String> getAvailablePanels(){
        return getAvailablePanels(primaryStage);
    }

    public Set<String> getAvailablePanels(Stage stage) {
        return stagePanels.getOrDefault(stage, Collections.emptyMap()).keySet();
    }

    public Map<String, String> getCardPathsMap() { return cardPathsMap; }

    public Set<String> getAvailableContainers(Stage stage) {
        return stageContainers.getOrDefault(stage, Collections.emptyMap()).keySet();
    }

    // ==================== REGISTRY ====================

    public void registerCard(String name, String fxmlPath) {
        if (name == null || name.isBlank())
            throw new IllegalArgumentException("view name must not be blank");
        if (fxmlPath == null || fxmlPath.isBlank())
            throw new IllegalArgumentException("fxmlPath must not be blank");
        cardPathsMap.put(name, fxmlPath);
    }

    public void registerDynamicPanel(String name, AnchorPane panel){
        registerDynamicPanel(name,panel,primaryStage);
    }

    public void registerContainer(String name, Pane container){
        registerContainer(name,container,primaryStage);
    }

    public void registerDynamicPanel(String name, AnchorPane panel, Stage stage) {
        stagePanels.computeIfAbsent(stage, s -> new HashMap<>()).put(name, panel);
    }

    public void registerContainer(String name, Pane container, Stage stage) {
        stageContainers.computeIfAbsent(stage, s -> new HashMap<>()).put(name, container);
    }

    // ==================== CARD LOADING ====================

    public <T> T loadCard(String cardName, String anchorPaneName) {
        return loadCard(cardName, anchorPaneName, primaryStage);
    }

    public <T> T loadCard(String cardName, String anchorPaneName, Stage stage) {
        ensurePanelCached(stage, anchorPaneName);
        String path = cardPathsMap.get(cardName);
        if (path == null) throw new IllegalArgumentException("Unknown card: " + cardName);
        try {
            return mountIntoPanel(stagePanels.get(stage).get(anchorPaneName), path, cardName, stage);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load card '" + cardName + "' from " + path, e);
        }
    }

    public <T> void loadCards(List<T> items, Pane container, String cardFxmlPath) {
        loadCards(items, container, cardFxmlPath, null);
    }

    public <T> void loadCards(List<T> items, Pane container, String cardFxmlPath,
                              BiConsumer<Object, T> dataSetter) {
        runOnFxThread(() -> {
            container.getChildren().clear();
            for (T item : items) {
                try {
                    FXMLLoader loader = new FXMLLoader(SceneManager.class.getResource(cardFxmlPath));
                    Node card = loader.load();
                    if (dataSetter != null) dataSetter.accept(loader.getController(), item);
                    container.getChildren().add(card);
                } catch (IOException e) {
                    throw new RuntimeException("Failed to load card: " + cardFxmlPath, e);
                }
            }
        });
    }

    public <T> void loadCardsInto(String containerName, List<T> items, String cardFxmlPath,
                                  BiConsumer<Object, T> dataSetter) {
        loadCardsInto(containerName, items, cardFxmlPath, dataSetter, primaryStage);
    }

    public <T> void loadCardsInto(String containerName, List<T> items, String cardFxmlPath,
                                  BiConsumer<Object, T> dataSetter, Stage stage) {
        Pane container = stageContainers.getOrDefault(stage, Collections.emptyMap()).get(containerName);
        if (container == null) throw new IllegalArgumentException("Container not registered: " + containerName);
        loadCards(items, container, cardFxmlPath, dataSetter);
    }

    // ==================== INTERNAL HELPERS ====================

    private void ensurePanelCached(Stage stage, String anchorPaneName) {
        Map<String, AnchorPane> panels = stagePanels.get(stage);
        if (panels == null || !panels.containsKey(anchorPaneName)) {
            throw new IllegalStateException("No dynamic panel registered for this stage. Call registerDynamicPanel() first.");
        }
    }

    private <T> T mountIntoPanel(AnchorPane targetPanel, String resourcePath,
                                 String logicalName, Stage stage) throws IOException {
        URL url = SceneManager.class.getResource(resourcePath);
        if (url == null) throw new IllegalArgumentException("FXML not found: " + resourcePath);

        FXMLLoader loader = new FXMLLoader(url);
        Parent card = loader.load();

        Object loadedController = loader.getController();

        runOnFxThread(() -> {
            if (targetPanel.getScene() != stage.getScene()) {
                throw new IllegalStateException(
                        "Target panel '" + logicalName + "' is not in the active scene for this stage."
                );
            }
            targetPanel.getChildren().setAll(card);
            AnchorPane.setTopAnchor(card, 0.0);
            AnchorPane.setRightAnchor(card, 0.0);
            AnchorPane.setBottomAnchor(card, 0.0);
            AnchorPane.setLeftAnchor(card, 0.0);
            currentPanelView = logicalName;
        });

        return (T) loadedController;
    }

    // ==================== STATE CHECKS ====================
    public boolean isPanelInScene(String panelName){
        return isPanelInScene(panelName,primaryStage);
    }
    public boolean isPanelInScene(String panelName, Stage stage) {
        Map<String, AnchorPane> panels = stagePanels.get(stage);
        if (panels == null) return false;
        AnchorPane panel = panels.get(panelName);
        return panel != null && panel.getScene() != null;
    }

}
