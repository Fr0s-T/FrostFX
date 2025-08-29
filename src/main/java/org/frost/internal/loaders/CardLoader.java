package org.frost.internal.loaders;

import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import org.frost.services.CardLoadingService;

import java.util.*;
import java.util.function.BiConsumer;

/**
 * CARDLOADER - Dynamic component and card management system
 * Public API facade that delegates to the actual service implementation.
 */
public class CardLoader {

    // State management
    private String currentPanelView = "";
    private final Map<String, String> cardPathsMap = new HashMap<>();
    private final Map<Stage, Map<String, AnchorPane>> stagePanels = new HashMap<>();
    private final Map<Stage, Map<String, Pane>> stageContainers = new HashMap<>();
    private Stage primaryStage;

    // The real worker
    private final CardLoadingService loadingService = new CardLoadingService();

    public CardLoader() { }

    void setPrimaryStage(Stage stage) { this.primaryStage = stage; }

    // ==================== GETTERS & QUERY METHODS ====================

    public String getCurrentPanelView() { return currentPanelView; }

    public Set<String> getAvailablePanels() {
        ensurePrimaryStageSet();
        return getAvailablePanels(primaryStage);
    }

    public Set<String> getAvailablePanels(Stage stage) {
        if (stage == null) throw new IllegalArgumentException("Stage cannot be null");
        return stagePanels.getOrDefault(stage, Collections.emptyMap()).keySet();
    }

    public Map<String, String> getCardPathsMap() {
        return Collections.unmodifiableMap(cardPathsMap);
    }

    public Set<String> getAvailableContainers(Stage stage) {
        if (stage == null) throw new IllegalArgumentException("Stage cannot be null");
        return stageContainers.getOrDefault(stage, Collections.emptyMap()).keySet();
    }

    // ==================== REGISTRY METHODS ====================

    public void registerCard(String name, String fxmlPath) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("Card name must not be null or blank");
        if (fxmlPath == null || fxmlPath.isBlank()) throw new IllegalArgumentException("FXML path must not be null or blank");
        if (cardPathsMap.containsKey(name)) throw new IllegalStateException("Card '" + name + "' is already registered");
        cardPathsMap.put(name, fxmlPath);
    }

    public boolean unregisterCard(String name) {
        return cardPathsMap.remove(name) != null;
    }

    public void registerDynamicPanel(String name, AnchorPane panel) {
        ensurePrimaryStageSet();
        registerDynamicPanel(name, panel, primaryStage);
    }

    public void registerContainer(String name, Pane container) {
        ensurePrimaryStageSet();
        registerContainer(name, container, primaryStage);
    }

    public void registerDynamicPanel(String name, AnchorPane panel, Stage stage) {
        validateRegistrationParams(name, panel, stage);
        stagePanels.computeIfAbsent(stage, s -> new HashMap<>()).put(name, panel);
    }

    public void registerContainer(String name, Pane container, Stage stage) {
        validateRegistrationParams(name, container, stage);
        stageContainers.computeIfAbsent(stage, s -> new HashMap<>()).put(name, container);
    }

    public boolean unregisterDynamicPanel(String name, Stage stage) {
        Map<String, AnchorPane> panels = stagePanels.get(stage);
        return panels != null && panels.remove(name) != null;
    }

    public boolean unregisterContainer(String name, Stage stage) {
        Map<String, Pane> containers = stageContainers.get(stage);
        return containers != null && containers.remove(name) != null;
    }

    // ==================== SINGLE CARD LOADING METHODS ====================

    public <T> T loadCard(String cardName, String anchorPaneName) {
        ensurePrimaryStageSet();
        return loadCard(cardName, anchorPaneName, primaryStage);
    }

    public <T> T loadCard(String cardName, String anchorPaneName, Stage stage) {
        return loadingService.loadCard(cardName, anchorPaneName, stage, cardPathsMap, stagePanels);
    }

// ==================== MULTIPLE CARDS LOADING METHODS ====================

    // Beginner-friendly: No margins, primary stage
    public void loadCards(String containerName, List<?> items, String cardFxmlPath) {
        ensurePrimaryStageSet();
        loadCards(containerName, items, cardFxmlPath, null, primaryStage, 0, 0);
    }

    // Beginner-friendly: With data injection, no margins, primary stage
    public <T> void loadCards(String containerName, List<T> items, String cardFxmlPath,
                              BiConsumer<Object, T> dataSetter) {
        ensurePrimaryStageSet();
        loadCards(containerName, items, cardFxmlPath, dataSetter, primaryStage, 0, 0);
    }

    // Beginner-friendly: Single margin, primary stage
    public <T> void loadCards(String containerName, List<T> items, String cardFxmlPath,
                              BiConsumer<Object, T> dataSetter, double margin) {
        ensurePrimaryStageSet();
        loadCards(containerName, items, cardFxmlPath, dataSetter, primaryStage, margin, margin);
    }

    // Beginner-friendly: Dual margins, primary stage
    public <T> void loadCards(String containerName, List<T> items, String cardFxmlPath,
                              BiConsumer<Object, T> dataSetter,
                              double horizontalMargin, double verticalMargin) {
        ensurePrimaryStageSet();
        loadCards(containerName, items, cardFxmlPath, dataSetter, primaryStage,
                horizontalMargin, verticalMargin);
    }

    // Advanced: Specific stage, no margins
    public <T> void loadCards(String containerName, List<T> items, String cardFxmlPath,
                              BiConsumer<Object, T> dataSetter, Stage stage) {
        loadCards(containerName, items, cardFxmlPath, dataSetter, stage, 0, 0);
    }

    // Advanced: Specific stage, single margin
    public <T> void loadCards(String containerName, List<T> items, String cardFxmlPath,
                              BiConsumer<Object, T> dataSetter, Stage stage, double margin) {
        loadCards(containerName, items, cardFxmlPath, dataSetter, stage, margin, margin);
    }

    // MASTER METHOD: Specific stage, dual margins (all others delegate here)
    public <T> void loadCards(String containerName, List<T> items, String cardFxmlPath,
                              BiConsumer<Object, T> dataSetter, Stage stage,
                              double horizontalMargin, double verticalMargin) {

        loadingService.loadCards(containerName, items, cardFxmlPath, dataSetter, stage,
                horizontalMargin, verticalMargin, stageContainers);
    }

// ==================== MULTIPLE CARDS LOADING WITH CONTROLLER RETURN ====================

    // Returns list of controllers for lifecycle management
    public <T, C> List<C> loadCardsWithControllers(String containerName, List<T> items, String cardFxmlPath,
                                                   BiConsumer<C, T> dataSetter) {
        ensurePrimaryStageSet();
        return loadCardsWithControllers(containerName, items, cardFxmlPath, dataSetter,
                primaryStage, 0, 0);
    }

    // Master method with controller return
    public <T, C> List<C> loadCardsWithControllers(String containerName, List<T> items, String cardFxmlPath,
                                                   BiConsumer<C, T> dataSetter, Stage stage,
                                                   double horizontalMargin, double verticalMargin) {
        return loadingService.loadCardsWithControllers(containerName, items, cardFxmlPath, dataSetter, stage,
                horizontalMargin, verticalMargin, stageContainers);
    }

    // ==================== INTERNAL HELPERS ====================

    private void validateRegistrationParams(String name, Object component, Stage stage) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("Name cannot be null or blank");
        if (component == null) throw new IllegalArgumentException("Component cannot be null");
        if (stage == null) throw new IllegalArgumentException("Stage cannot be null");
    }

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
     * DEBUG HELPER: Checks if a panel exists and is attached to a scene in the given stage
     * Returns false instead of throwing for null inputs to facilitate debugging
     *
     * @param panelName Name of the panel to check
     * @param stage Stage to search in (can be null for safe debugging)
     * @return true if panel exists and is in a scene, false otherwise (including null inputs)
     */
    public boolean isPanelInScene(String panelName, Stage stage) {
        // Safe debugging - return false instead of throwing
        if (stage == null || panelName == null) {
            System.out.println("[DEBUG] isPanelInScene: Null input - stage: " +
                    (stage == null) + ", panelName: " + (panelName == null));
            return false;
        }

        Map<String, AnchorPane> panels = stagePanels.get(stage);
        if (panels == null) {
            System.out.println("[DEBUG] isPanelInScene: No panels registered for stage: " + stage);
            return false;
        }

        AnchorPane panel = panels.get(panelName);
        boolean result = panel != null && panel.getScene() != null;

        System.out.println("[DEBUG] isPanelInScene: panel='" + panelName +
                "', stage=" + stage + ", result=" + result);
        return result;
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
        cardPathsMap.clear();      // Clear cards too (for complete reset)
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