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

/**
 * SCENELOADER - Professional JavaFX Scene and Component Management
 * <p>
 * A lightweight, thread-safe utility for managing JavaFX scene navigation,
 * dynamic card loading, and UI composition. Handles FXML loading, threading,
 * and lifecycle management automatically.
 * <p>
 * USAGE:
 * 1. In Main.java: SceneLoader.setPrimaryStage(primaryStage);
 * 2. Register components: registerCard(), registerDynamicPanel(), registerContainer()
 * 3. Load content: loadScene(), loadCard(), loadCards()
 *
 * @author Frost
 * @version 1.0
 */
public final class SceneLoader {

    // ==================== STATE MANAGEMENT ====================

    /** Primary application stage - set once at startup */
    private static Stage primaryStage;

    /** Tracks currently displayed panel view (for refresh functionality) */
    private static String currentPanelView = "";

    /** Registry of card names to FXML paths */
    private static final Map<String, String> cardPathsMap = new HashMap<>();

    /** Cache of registered dynamic panels (AnchorPanes for card mounting) */
    private static final Map<String, AnchorPane> cachedDynamicPanels = new HashMap<>();

    /** List of scene lifecycle listeners for hooks */
    private static final List<SceneLoaderListener> sceneListeners = new ArrayList<>();

    // ==================== CONTAINER REGISTRY ====================

    /** Flexible registry for any Pane type (FlowPane, VBox, HBox, etc.) */
    private static final Map<String, Pane> containerRegistry = new HashMap<>();

    /** Private constructor - static utility class only */
    private SceneLoader() {}

    // ==================== CORE SETUP METHODS ====================

    /**
     * Sets the primary application stage (REQUIRED)
     * Call this once in your Main.start() method
     *
     * @param stage The primary JavaFX stage
     */
    public static void setPrimaryStage(Stage stage) {
        primaryStage = stage;
    }

    /**
     * Gets the name of the currently displayed panel view
     * Useful for refresh operations
     */
    public static String getCurrentPanelView() {
        return currentPanelView;
    }

    // ==================== LIFECYCLE HOOKS SYSTEM ====================

    /**
     * Interface for scene loading lifecycle events
     * Implement this to receive callbacks before/after scene loads
     */
    public interface SceneLoaderListener {
        /** Called immediately before a scene begins loading */
        void onBeforeSceneLoad(String fxmlPath);

        /** Called immediately after a scene finishes loading */
        void onAfterSceneLoad(String fxmlPath, Object controller);
    }

    /**
     * Registers a lifecycle listener for scene loading events
     * Perfect for analytics, permissions, preloading, etc.
     *
     * @param listener Implementation of SceneLoaderListener
     */
    public static void addSceneLoaderListener(SceneLoaderListener listener) {
        sceneListeners.add(listener);
    }

    /**
     * Removes a previously registered lifecycle listener
     */
    public static void removeSceneLoaderListener(SceneLoaderListener listener) {
        sceneListeners.remove(listener);
    }

    // ==================== COMPONENT REGISTRATION ====================

    /**
     * Registers a card template for later use
     *
     * @param name Logical name for the card (e.g., "user-profile")
     * @param fxmlPath Classpath path to FXML file (e.g., "/cards/user.fxml")
     * @throws IllegalArgumentException if name or path is null/blank
     */
    public static void registerCard(String name, String fxmlPath) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("view name must not be blank");
        if (fxmlPath == null || fxmlPath.isBlank()) throw new IllegalArgumentException("fxmlPath must not be blank");
        cardPathsMap.put(name, fxmlPath);
    }

    /**
     * Registers a dynamic panel for card mounting
     * Typically AnchorPanes that serve as card containers
     *
     * @param name Logical name for the panel (e.g., "main-content")
     * @param dynamicPane The AnchorPane to register
     * @throws IllegalArgumentException if name or pane is null/blank
     */
    public static void registerDynamicPanel(String name, AnchorPane dynamicPane) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("Panel name must not be blank");
        if (dynamicPane == null) throw new IllegalArgumentException("Panel must not be null");
        cachedDynamicPanels.put(name, dynamicPane);
    }

    /**
     * Registers any Pane type as a container for bulk card loading
     * Supports FlowPane, VBox, HBox, TilePane, etc.
     *
     * @param name Logical name for the container (e.g., "product-grid")
     * @param container The Pane to register
     * @throws IllegalArgumentException if name or container is null/blank
     */
    public static void registerContainer(String name, Pane container) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("Container name must not be blank");
        if (container == null) throw new IllegalArgumentException("Container must not be null");
        containerRegistry.put(name, container);
    }

    // ==================== SCENE MANAGEMENT ====================

    /**
     * Loads a complete scene into the primary stage
     * Use for major navigation events (e.g., login → dashboard)
     *
     * @param fxmlPath Classpath path to the scene FXML
     * @param controller Optional custom controller (null for FXML-defined)
     * @throws IllegalStateException if primary stage not set
     * @throws IllegalArgumentException if FXML not found
     * @throws RuntimeException if loading fails
     */
    public static void loadScene(String fxmlPath, Object controller) {
        runOnFxThread(() -> {
            if (primaryStage == null) {
                throw new IllegalStateException("Primary stage not set. Call SceneLoader.setPrimaryStage(stage) first.");
            }

            try {
                // Trigger before-load hooks
                fireBeforeSceneLoad(fxmlPath);

                URL url = SceneLoader.class.getResource(fxmlPath);
                if (url == null) throw new IllegalArgumentException("FXML not found on classpath: " + fxmlPath);

                FXMLLoader loader = new FXMLLoader(url);
                if (controller != null) loader.setController(controller);
                Parent root = loader.load();

                Object loadedController = loader.getController();

                Scene scene = new Scene(root);
                primaryStage.setScene(scene);
                primaryStage.centerOnScreen();
                primaryStage.show();

                // Trigger after-load hooks
                fireAfterSceneLoad(fxmlPath, loadedController);

            } catch (IOException e) {
                throw new RuntimeException("Failed to load scene: " + fxmlPath, e);
            }
        });
    }

    // ==================== CARD LOADING SYSTEM ====================

    /**
     * Loads a single card into a registered dynamic panel
     * Simple API for basic card mounting
     *
     * @param cardName Registered card name
     * @param anchorPaneName Registered panel name
     * @throws IllegalArgumentException if card or panel not registered
     * @throws RuntimeException if loading fails
     */
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

    /**
     * Loads multiple cards into a container (simple version)
     * No data injection - controllers must be self-contained
     *
     * @param items List of data items (one card per item)
     * @param container The Pane to populate with cards
     * @param cardFxmlPath Path to card FXML template
     */
    public static <T> void loadCards(List<T> items, Pane container, String cardFxmlPath) {
        loadCards(items, container, cardFxmlPath, null);
    }

    /**
     * Loads multiple cards with data injection (advanced version)
     * Perfect for dynamic data displays (products, users, etc.)
     *
     * @param items List of data items
     * @param container The Pane to populate
     * @param cardFxmlPath Path to card FXML template
     * @param dataSetter BiConsumer that injects data into each card's controller
     * @param <T> Type of data items
     */
    public static <T> void loadCards(List<T> items, Pane container, String cardFxmlPath,
                                     BiConsumer<Object, T> dataSetter) {
        runOnFxThread(() -> {
            container.getChildren().clear();

            for (T item : items) {
                try {
                    FXMLLoader loader = new FXMLLoader(SceneLoader.class.getResource(cardFxmlPath));
                    Node card = loader.load();

                    // Inject data if provider specified
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

    /**
     * Loads cards into a registered container by name
     * Convenience method for registered containers
     *
     * @param containerName Registered container name
     * @param items List of data items
     * @param cardFxmlPath Path to card FXML template
     * @param dataSetter BiConsumer for data injection
     * @param <T> Type of data items
     * @throws IllegalArgumentException if container not registered
     */
    public static <T> void loadCardsInto(String containerName, List<T> items,
                                         String cardFxmlPath, BiConsumer<Object, T> dataSetter) {
        Pane container = containerRegistry.get(containerName);
        if (container == null) throw new IllegalArgumentException("Container not registered: " + containerName);
        loadCards(items, container, cardFxmlPath, dataSetter);
    }

    // ==================== INTERNAL METHODS ====================

    /** Validates that a panel is registered and available */
    private static void ensurePanelCached(String anchorPaneName) {
        if (cachedDynamicPanels.get(anchorPaneName) == null) {
            throw new IllegalStateException("No dynamic panel registered. Call registerDynamicPanel() first.");
        }
    }

    /** Internal method for mounting cards into panels with validation */
    private static void mountIntoPanel(AnchorPane targetPanel, String resourcePath, String logicalName) throws IOException {
        URL url = SceneLoader.class.getResource(resourcePath);
        if (url == null) throw new IllegalArgumentException("FXML not found on classpath: " + resourcePath);

        // Safety check: prevent cross-scene modification
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

    /** Triggers all before-load lifecycle hooks */
    private static void fireBeforeSceneLoad(String fxmlPath) {
        for (SceneLoaderListener listener : sceneListeners) {
            listener.onBeforeSceneLoad(fxmlPath);
        }
    }

    /** Triggers all after-load lifecycle hooks */
    private static void fireAfterSceneLoad(String fxmlPath, Object controller) {
        for (SceneLoaderListener listener : sceneListeners) {
            listener.onAfterSceneLoad(fxmlPath, controller);
        }
    }

    // ==================== DEBUGGING & UTILITY METHODS ====================

    /** Returns all registered panel names */
    public static Set<String> getAvailablePanels() {
        return cachedDynamicPanels.keySet();
    }

    /** Returns all registered container names */
    public static Set<String> getAvailableContainers() {
        return containerRegistry.keySet();
    }

    /** Checks if a panel is in the currently active scene */
    public static boolean isPanelInCurrentScene(String panelName) {
        AnchorPane panel = cachedDynamicPanels.get(panelName);
        if (panel == null || panel.getScene() == null || primaryStage == null) {
            return false;
        }
        return panel.getScene() == primaryStage.getScene();
    }

    /** Checks if a panel is attached to any scene */
    public static boolean isPanelInScene(String panelName) {
        AnchorPane panel = cachedDynamicPanels.get(panelName);
        return panel != null && panel.getScene() != null;
    }

    /** Returns the card registry map for inspection */
    public static Map<String, String> getCardPathsMap() {
        return cardPathsMap;
    }

    // ==================== THREAD SAFETY ====================

    /** Ensures code runs on JavaFX application thread */
    private static void runOnFxThread(Runnable action) {
        if (Platform.isFxApplicationThread()) action.run();
        else Platform.runLater(action);
    }
}