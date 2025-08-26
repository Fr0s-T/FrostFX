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

/**
 * CARDLOADER - Dynamic component and card management system
 * <p>
 * Handles registration and loading of reusable UI components (cards) into
 * designated container panels. Supports both single card loading and
 * bulk data-driven card population with per-stage isolation.
 * </p>
 *
 * <p><b>Lifecycle:</b></p>
 * <ol>
 *   <li>Register cards: {@code registerCard("cardName", "/path/to/card.fxml")}</li>
 *   <li>Register panels: {@code registerDynamicPanel("panelName", anchorPane, stage)}</li>
 *   <li>Load cards: {@code loadCard("cardName", "panelName", stage)}</li>
 * </ol>
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>
 * // Single card loading
 * UserController ctrl = SceneManager.CardLoader()
 *     .loadCard("userCard", "contentPanel");
 * ctrl.setUserData(user);
 *
 * // Bulk card loading
 * SceneManager.CardLoader().loadCardsInto("userList", users,
 *     "/cards/user.fxml", (controller, user) -> {
 *         ((UserController)controller).setUser(user);
 *     });
 * </pre>
 *
 * @author Frost
 * @version 2.0
 * @since 1.0
 * @throws IllegalStateException if panels are not properly registered
 * @throws IllegalArgumentException if cards are not found or registered incorrectly
 */
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

    /**
     * Creates a new CardLoader instance
     */
    public CardLoader() { }

    /**
     * Sets the primary stage for default operations
     *
     * @param stage The primary stage to use for default operations
     */
    void setPrimaryStage(Stage stage) { this.primaryStage = stage; }

    // ==================== GETTERS & QUERY METHODS ====================

    /**
     * Gets the name of the currently displayed panel view
     *
     * @return The current panel view name, or empty string if none
     */
    public String getCurrentPanelView() { return currentPanelView; }

    /**
     * Gets all available panel names for the primary stage
     *
     * @return Set of available panel names
     * @throws IllegalStateException if primary stage is not set
     */
    public Set<String> getAvailablePanels() {
        ensurePrimaryStageSet();
        return getAvailablePanels(primaryStage);
    }

    /**
     * Gets all available panel names for a specific stage
     *
     * @param stage The stage to query
     * @return Set of available panel names for the stage
     */
    public Set<String> getAvailablePanels(Stage stage) {
        if (stage == null) {
            throw new IllegalArgumentException("Stage cannot be null");
        }
        return stagePanels.getOrDefault(stage, Collections.emptyMap()).keySet();
    }

    /**
     * Gets the card registry map for inspection
     *
     * @return Unmodifiable view of the card paths map
     */
    public Map<String, String> getCardPathsMap() {
        return Collections.unmodifiableMap(cardPathsMap);
    }

    /**
     * Gets all available container names for a specific stage
     *
     * @param stage The stage to query
     * @return Set of available container names for the stage
     */
    public Set<String> getAvailableContainers(Stage stage) {
        if (stage == null) {
            throw new IllegalArgumentException("Stage cannot be null");
        }
        return stageContainers.getOrDefault(stage, Collections.emptyMap()).keySet();
    }

    // ==================== REGISTRY METHODS ====================

    /**
     * Registers a card template for later use
     *
     * @param name Logical name for the card (e.g., "user-profile")
     * @param fxmlPath Classpath path to FXML file (e.g., "/cards/user.fxml")
     * @throws IllegalArgumentException if name or path is null/blank
     * @throws IllegalStateException if card name is already registered
     */
    public void registerCard(String name, String fxmlPath) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Card name must not be null or blank");
        }
        if (fxmlPath == null || fxmlPath.isBlank()) {
            throw new IllegalArgumentException("FXML path must not be null or blank");
        }
        if (cardPathsMap.containsKey(name)) {
            throw new IllegalStateException("Card '" + name + "' is already registered");
        }

        cardPathsMap.put(name, fxmlPath);
    }

    /**
     * Unregisters a card template
     *
     * @param name Logical name of the card to unregister
     * @return true if the card was found and removed, false otherwise
     */
    public boolean unregisterCard(String name) {
        return cardPathsMap.remove(name) != null;
    }

    /**
     * Registers a dynamic panel for the primary stage
     *
     * @param name Logical name for the panel (e.g., "main-content")
     * @param panel The AnchorPane to register
     */
    public void registerDynamicPanel(String name, AnchorPane panel) {
        ensurePrimaryStageSet();
        registerDynamicPanel(name, panel, primaryStage);
    }

    /**
     * Registers a container for the primary stage
     *
     * @param name Logical name for the container (e.g., "product-grid")
     * @param container The Pane to register
     */
    public void registerContainer(String name, Pane container) {
        ensurePrimaryStageSet();
        registerContainer(name, container, primaryStage);
    }

    /**
     * Registers a dynamic panel for a specific stage
     *
     * @param name Logical name for the panel
     * @param panel The AnchorPane to register
     * @param stage The target stage for this panel
     * @throws IllegalArgumentException if any parameter is null or name is blank
     */
    public void registerDynamicPanel(String name, AnchorPane panel, Stage stage) {
        validateRegistrationParams(name, panel, stage);
        stagePanels.computeIfAbsent(stage, s -> new HashMap<>()).put(name, panel);
    }

    /**
     * Registers a container for a specific stage
     *
     * @param name Logical name for the container
     * @param container The Pane to register
     * @param stage The target stage for this container
     * @throws IllegalArgumentException if any parameter is null or name is blank
     */
    public void registerContainer(String name, Pane container, Stage stage) {
        validateRegistrationParams(name, container, stage);
        stageContainers.computeIfAbsent(stage, s -> new HashMap<>()).put(name, container);
    }

    /**
     * Unregisters a dynamic panel from a stage
     *
     * @param name Panel name to unregister
     * @param stage Stage from which to unregister
     * @return true if panel was found and removed, false otherwise
     */
    public boolean unregisterDynamicPanel(String name, Stage stage) {
        Map<String, AnchorPane> panels = stagePanels.get(stage);
        return panels != null && panels.remove(name) != null;
    }

    /**
     * Unregisters a container from a stage
     *
     * @param name Container name to unregister
     * @param stage Stage from which to unregister
     * @return true if container was found and removed, false otherwise
     */
    public boolean unregisterContainer(String name, Stage stage) {
        Map<String, Pane> containers = stageContainers.get(stage);
        return containers != null && containers.remove(name) != null;
    }

    // ==================== CARD LOADING METHODS ====================

    /**
     * Loads a single card into a registered dynamic panel on the primary stage
     *
     * @param cardName Registered card name
     * @param anchorPaneName Registered panel name
     * @param <T> Type of the controller to return
     * @return The loaded card's controller
     * @throws IllegalStateException if primary stage not set or panel not registered
     * @throws IllegalArgumentException if card not registered
     */
    public <T> T loadCard(String cardName, String anchorPaneName) {
        ensurePrimaryStageSet();
        return loadCard(cardName, anchorPaneName, primaryStage);
    }

    /**
     * Loads a single card into a registered dynamic panel on a specific stage
     *
     * @param cardName Registered card name
     * @param anchorPaneName Registered panel name
     * @param stage The target stage
     * @param <T> Type of the controller to return
     * @return The loaded card's controller
     * @throws IllegalStateException if panel not registered for stage
     * @throws IllegalArgumentException if card not registered or stage is null
     */
    public <T> T loadCard(String cardName, String anchorPaneName, Stage stage) {
        if (stage == null) {
            throw new IllegalArgumentException("Stage cannot be null");
        }

        ensurePanelCached(stage, anchorPaneName);
        String path = cardPathsMap.get(cardName);
        if (path == null) {
            throw new IllegalArgumentException("Card '" + cardName + "' is not registered. "
                    + "Call registerCard('" + cardName + "', '/path/to/fxml.fxml') first.");
        }

        try {
            return mountIntoPanel(stagePanels.get(stage).get(anchorPaneName), path, cardName, stage);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load card '" + cardName + "' from " + path, e);
        }
    }

    /**
     * Loads multiple cards into a container (simple version)
     *
     * @param items List of data items (one card per item)
     * @param container The Pane to populate with cards
     * @param cardFxmlPath Path to card FXML template
     * @param <T> Type of data items
     */
    public <T> void loadCards(List<T> items, Pane container, String cardFxmlPath) {
        loadCards(items, container, cardFxmlPath, null);
    }

    /**
     * Loads multiple cards with data injection (advanced version)
     *
     * @param items List of data items
     * @param container The Pane to populate
     * @param cardFxmlPath Path to card FXML template
     * @param dataSetter BiConsumer that injects data into each card's controller
     * @param <T> Type of data items
     * @throws IllegalArgumentException if FXML path is invalid
     */
    public <T> void loadCards(List<T> items, Pane container, String cardFxmlPath,
                              BiConsumer<Object, T> dataSetter) {
        if (container == null) {
            throw new IllegalArgumentException("Container cannot be null");
        }
        if (cardFxmlPath == null || cardFxmlPath.isBlank()) {
            throw new IllegalArgumentException("Card FXML path cannot be null or blank");
        }

        runOnFxThread(() -> {
            container.getChildren().clear();
            for (T item : items) {
                try {
                    FXMLLoader loader = new FXMLLoader(SceneManager.class.getResource(cardFxmlPath));
                    Node card = loader.load();
                    if (dataSetter != null) {
                        dataSetter.accept(loader.getController(), item);
                    }
                    container.getChildren().add(card);
                } catch (IOException e) {
                    throw new RuntimeException("Failed to load card from: " + cardFxmlPath, e);
                }
            }
        });
    }

    /**
     * Loads cards into a registered container on the primary stage
     *
     * @param containerName Registered container name
     * @param items List of data items
     * @param cardFxmlPath Path to card FXML template
     * @param dataSetter BiConsumer for data injection
     * @param <T> Type of data items
     */
    public <T> void loadCardsInto(String containerName, List<T> items, String cardFxmlPath,
                                  BiConsumer<Object, T> dataSetter) {
        ensurePrimaryStageSet();
        loadCardsInto(containerName, items, cardFxmlPath, dataSetter, primaryStage);
    }

    /**
     * Loads cards into a registered container on a specific stage
     *
     * @param containerName Registered container name
     * @param items List of data items
     * @param cardFxmlPath Path to card FXML template
     * @param dataSetter BiConsumer for data injection
     * @param stage The target stage
     * @param <T> Type of data items
     * @throws IllegalArgumentException if container not registered
     */
    public <T> void loadCardsInto(String containerName, List<T> items, String cardFxmlPath,
                                  BiConsumer<Object, T> dataSetter, Stage stage) {
        if (stage == null) {
            throw new IllegalArgumentException("Stage cannot be null");
        }

        Pane container = stageContainers.getOrDefault(stage, Collections.emptyMap()).get(containerName);
        if (container == null) {
            throw new IllegalArgumentException("Container '" + containerName + "' is not registered for this stage. "
                    + "Call registerContainer('" + containerName + "', containerPane, stage) first.");
        }
        loadCards(items, container, cardFxmlPath, dataSetter);
    }

    // ==================== INTERNAL HELPERS ====================

    /**
     * Validates that a panel is registered and available for a stage
     */
    private void ensurePanelCached(Stage stage, String anchorPaneName) {
        Map<String, AnchorPane> panels = stagePanels.get(stage);
        if (panels == null || !panels.containsKey(anchorPaneName)) {
            throw new IllegalStateException("No dynamic panel '" + anchorPaneName + "' registered for stage '"
                    + stage.getTitle() + "'. Call registerDynamicPanel('" + anchorPaneName + "', panel, stage) first.");
        }
    }

    /**
     * Internal method for mounting cards into panels with validation
     */
    private <T> T mountIntoPanel(AnchorPane targetPanel, String resourcePath,
                                 String logicalName, Stage stage) throws IOException {
        if (targetPanel == null) {
            throw new IllegalArgumentException("Target panel cannot be null");
        }
        if (stage == null) {
            throw new IllegalArgumentException("Stage cannot be null");
        }
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
            currentPanelView = logicalName;
        });

        return (T) loadedController;
    }

    /**
     * Validates registration parameters
     */
    private void validateRegistrationParams(String name, Object component, Stage stage) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Name cannot be null or blank");
        }
        if (component == null) {
            throw new IllegalArgumentException("Component cannot be null");
        }
        if (stage == null) {
            throw new IllegalArgumentException("Stage cannot be null");
        }
    }

    /**
     * Ensures primary stage is set
     */
    private void ensurePrimaryStageSet() {
        if (primaryStage == null) {
            throw new IllegalStateException("Primary stage has not been set. "
                    + "Call SceneManager.init(primaryStage) first.");
        }
    }

    // ==================== STATE CHECKS ====================

    /**
     * Checks if a panel is in the primary stage's scene
     *
     * @param panelName Panel name to check
     * @return true if panel is in scene, false otherwise
     */
    public boolean isPanelInScene(String panelName) {
        ensurePrimaryStageSet();
        return isPanelInScene(panelName, primaryStage);
    }

    /**
     * Checks if a panel is in a specific stage's scene
     *
     * @param panelName Panel name to check
     * @param stage Stage to check
     * @return true if panel is in scene, false otherwise
     */
    public boolean isPanelInScene(String panelName, Stage stage) {
        if (stage == null) {
            throw new IllegalArgumentException("Stage cannot be null");
        }

        Map<String, AnchorPane> panels = stagePanels.get(stage);
        if (panels == null) return false;

        AnchorPane panel = panels.get(panelName);
        return panel != null && panel.getScene() != null;
    }

    // ==================== DEBUGGING TOOLS ====================

    /**
     * Prints debug information about registered cards and panels
     */
    public void debugPrintRegistry() {
        System.out.println("=== CardLoader Debug Info ===");
        System.out.println("Primary Stage: " +
                (primaryStage != null ? primaryStage.getTitle() : "Not set"));

        System.out.println("\nRegistered Cards:");
        if (cardPathsMap.isEmpty()) {
            System.out.println("  No cards registered");
        } else {
            cardPathsMap.forEach((name, path) ->
                    System.out.println("  " + name + " -> " + path));
        }

        System.out.println("\nStage Panels:");
        if (stagePanels.isEmpty()) {
            System.out.println("  No panels registered for any stage");
        } else {
            stagePanels.forEach((stage, panels) -> {
                System.out.println("  Stage: " + stage.getTitle());
                panels.forEach((name, panel) -> {
                    String inScene = panel.getScene() != null ? " [IN SCENE]" : " [NOT IN SCENE]";
                    System.out.println("    " + name + inScene);
                });
            });
        }
        System.out.println("=============================");
    }

    /**
     * Clears all stage-specific registrations while preserving reusable card definitions
     * Use cases:
     * - Stage reinitialization
     * - Testing cleanup
     * - Application reset scenarios
     * <p>
     * NOTE: This preserves card path registrations since cards are reusable across stages
     *
     * @see #clearAll() for complete cleanup including card definitions
     */
    public void clearAllRegistrations() {
        stagePanels.clear();       // Clear stage-specific panels
        stageContainers.clear();   // Clear stage-specific containers
        currentPanelView = "";     // Reset current view tracking
    }

    /**
     * Completely resets the CardLoader to initial state
     * WARNING: This will remove ALL registrations including reusable card definitions
     * Only use for complete application shutdown or testing scenarios
     */
    public void clearAll() {
        clearAllRegistrations();   // Clear stage-specific stuff
        cardPathsMap.clear();      // ✅ Now clear cards too (for complete reset)
    }

    /**
     * Clears registrations for a specific stage only
     * Useful for dynamic stage management without affecting other stages
     *
     * @param stage The stage to clear registrations for
     * @return true if stage had registrations that were cleared, false otherwise
     */
    public boolean clearStageRegistrations(Stage stage) {
        boolean hadRegistrations = false;

        if (stagePanels.containsKey(stage)) {
            stagePanels.get(stage).clear();
            hadRegistrations = true;
        }

        if (stageContainers.containsKey(stage)) {
            stageContainers.get(stage).clear();
            hadRegistrations = true;
        }

        return hadRegistrations;
    }
}