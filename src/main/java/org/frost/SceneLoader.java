package org.frost;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.function.BiConsumer;

public final class SceneLoader {

    // ---- State ----
    private static Stage primaryStage;
    private static String currentPanelView = "";
    private static final Map<String, String> cardPathsMap = new HashMap<>();
    private static final Map<String, AnchorPane> cachedDynamicPanels = new HashMap<>();
    private static final List<SceneLoaderListener> sceneListeners = new ArrayList<>();

    // ---- Container Registry ----
    private static final Map<String, Pane> containerRegistry = new HashMap<>();

    private SceneLoader() {}

    // ---- Setup (stage & panel) ----
    public static void setPrimaryStage(Stage stage) {
        primaryStage = stage;
    }

    public static String getCurrentPanelView() {
        return currentPanelView;
    }

    // ---- Lifecycle Hooks ----
    public interface SceneLoaderListener {
        void onBeforeSceneLoad(String fxmlPath);
        void onAfterSceneLoad(String fxmlPath, Object controller);
    }


    // Add registration methods:
    public static void addSceneLoaderListener(SceneLoaderListener listener) {
        sceneListeners.add(listener);
    }

    public static void removeSceneLoaderListener(SceneLoaderListener listener) {
        sceneListeners.remove(listener);
    }

    // ---- Registry (name -> FXML path) ----
    public static void registerCard(String name, String fxmlPath) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("view name must not be blank");
        if (fxmlPath == null || fxmlPath.isBlank()) throw new IllegalArgumentException("fxmlPath must not be blank");
        cardPathsMap.put(name, fxmlPath);
    }

    public static void registerDynamicPanel(String name, AnchorPane dynamicPane) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("Panel name must not be blank");
        if (dynamicPane == null) throw new IllegalArgumentException("Panel must not be null");
        cachedDynamicPanels.put(name, dynamicPane);
    }

    public static void registerContainer(String name, Pane container) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("Container name must not be blank");
        if (container == null) throw new IllegalArgumentException("Container must not be null");
        containerRegistry.put(name, container);
    }

    // ---- Full-scene swap (rare) ----
    public static void loadScene(String fxmlPath, Object controller) {
        runOnFxThread(() -> {
            if (primaryStage == null) {
                throw new IllegalStateException("Primary stage not set. Call SceneLoader.setPrimaryStage(stage) first.");
            }

            try {
                // BEFORE HOOK: Notify listeners scene is about to load
                fireBeforeSceneLoad(fxmlPath);

                URL url = SceneLoader.class.getResource(fxmlPath);
                if (url == null) throw new IllegalArgumentException("FXML not found on classpath: " + fxmlPath);

                FXMLLoader loader = new FXMLLoader(url);
                if (controller != null) loader.setController(controller);
                Parent root = loader.load();

                Object loadedController = loader.getController(); // Get the actual controller

                Scene scene = new Scene(root);
                primaryStage.setScene(scene);
                primaryStage.centerOnScreen();
                primaryStage.show();

                // AFTER HOOK: Notify listeners scene finished loading
                fireAfterSceneLoad(fxmlPath, loadedController);

            } catch (IOException e) {
                throw new RuntimeException("Failed to load scene: " + fxmlPath, e);
            }
        });
    }

    // ---- Basic Card Loading (AnchorPane) ----
    public static void loadCard(String cardName, String anchorPaneName) {
        ensurePanelCached(anchorPaneName);
        String path = cardPathsMap.get(cardName);
        if (path == null) throw new IllegalArgumentException("Unknown card: " + cardName);
        try {
            mountIntoPanel(cachedDynamicPanels.get(anchorPaneName), path, cardName);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load card '" + cardName + "' from " + path, e);
        }
    }

    // ---- Advanced Card Loading (Generic) ----
    public static <T> void loadCards(List<T> items, Pane container, String cardFxmlPath) {
        loadCards(items, container, cardFxmlPath, null);
    }

    public static <T> void loadCards(List<T> items, Pane container, String cardFxmlPath,
                                     BiConsumer<Object, T> dataSetter) {
        runOnFxThread(() -> {
            container.getChildren().clear();

            for (T item : items) {
                try {
                    FXMLLoader loader = new FXMLLoader(SceneLoader.class.getResource(cardFxmlPath));
                    Node card = loader.load();

                    // Only call dataSetter if provided
                    if (dataSetter != null) {
                        dataSetter.accept(loader.getController(), item);
                    }

                    container.getChildren().add(card);
                } catch (IOException e) {
                    throw new RuntimeException("Failed to load card: " + cardFxmlPath, e);
                }
            }
        });
    }

    public static <T> void loadCardsInto(String containerName, List<T> items,
                                         String cardFxmlPath, BiConsumer<Object, T> dataSetter) {
        Pane container = containerRegistry.get(containerName);
        if (container == null) throw new IllegalArgumentException("Container not registered: " + containerName);
        loadCards(items, container, cardFxmlPath, dataSetter);
    }

    // ---- Internals ----
    private static void ensurePanelCached(String anchorPaneName) {
        if (cachedDynamicPanels.get(anchorPaneName) == null) {
            throw new IllegalStateException("No dynamic panel registered. Call registerDynamicPanel() first.");
        }
    }

    private static void mountIntoPanel(AnchorPane targetPanel, String resourcePath, String logicalName) throws IOException {
        URL url = SceneLoader.class.getResource(resourcePath);
        if (url == null) throw new IllegalArgumentException("FXML not found on classpath: " + resourcePath);

        // Validation: Prevent cross-scene modification attempts
        if (targetPanel.getScene() != primaryStage.getScene()) {
            throw new IllegalStateException(
                    "Target panel '" + logicalName + "' is not in the active scene. " +
                            "You can only modify panels that belong to the currently displayed window."
            );
        }

        FXMLLoader loader = new FXMLLoader(url);
        Parent card = loader.load();

        runOnFxThread(() -> {
            targetPanel.getChildren().setAll(card);
            AnchorPane.setTopAnchor(card, 0.0);
            AnchorPane.setRightAnchor(card, 0.0);
            AnchorPane.setBottomAnchor(card, 0.0);
            AnchorPane.setLeftAnchor(card, 0.0);
            currentPanelView = logicalName;
        });
    }

    private static void fireBeforeSceneLoad(String fxmlPath) {
        for (SceneLoaderListener listener : sceneListeners) {
            listener.onBeforeSceneLoad(fxmlPath);
        }
    }

    private static void fireAfterSceneLoad(String fxmlPath, Object controller) {
        for (SceneLoaderListener listener : sceneListeners) {
            listener.onAfterSceneLoad(fxmlPath, controller);
        }
    }

    // ---- Helper methods for debugging ----
    public static Set<String> getAvailablePanels() {
        return cachedDynamicPanels.keySet();
    }

    public static Set<String> getAvailableContainers() {
        return containerRegistry.keySet();
    }

    public static boolean isPanelInCurrentScene(String panelName) {
        AnchorPane panel = cachedDynamicPanels.get(panelName);
        if (panel == null || panel.getScene() == null || primaryStage == null) {
            return false;
        }
        return panel.getScene() == primaryStage.getScene();
    }

    public static boolean isPanelInScene(String panelName) {
        AnchorPane panel = cachedDynamicPanels.get(panelName);
        return panel != null && panel.getScene() != null;
    }

    public static Map<String, String> getCardPathsMap() {
        return cardPathsMap;
    }

    // ---- Thread safety ----
    private static void runOnFxThread(Runnable action) {
        if (Platform.isFxApplicationThread()) action.run();
        else Platform.runLater(action);
    }
}