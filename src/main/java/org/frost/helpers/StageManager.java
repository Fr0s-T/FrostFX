package org.frost.helpers;

import javafx.scene.input.KeyCombination;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.frost.internal.loaders.SceneManager.WindowMode;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * STAGEMANAGER - Centralized stage registry and window state management
 * <p>
 * Manages multiple application stages, including primary and secondary stages,
 * with support for window mode management and stage lifecycle tracking.
 * Provides thread-safe stage registration, retrieval, and window styling services.
 * </p>
 *
 * <p><b>Usage:</b></p>
 * <pre>
 * // Initialize in main application
 * StageManager manager = new StageManager();
 * manager.setPrimaryStage(primaryStage);
 *
 * // Register secondary stages
 * manager.registerSecondaryStage("settings", settingsStage);
 * manager.registerSecondaryStage("preferences", prefsStage);
 *
 * // Apply window modes
 * manager.applyWindowMode(WindowMode.UNDECORATED);
 *
 * // Retrieve stages
 * Stage settings = manager.getRegisteredStage("settings");
 * </pre>
 *
 * @author Frost
 * @version 2.0
 * @since 1.0
 */
public class StageManager {

    private Stage primaryStage;
    private WindowMode currentWindowMode = WindowMode.DECORATED;
    private final Map<String, Stage> secondaryStagesMap = new HashMap<>();
    private final Map<Stage, WindowMode> stageWindowModes = new HashMap<>();
    private final Map<String, String> stageTitles = new HashMap<>();

    public void setStageTitle(String stageName, String title) {
        Stage stage = secondaryStagesMap.get(stageName);
        if (stage != null) {
            stage.setTitle(title);
            stageTitles.put(stageName, title);
        }
    }

    public String getStageTitle(String stageName) {
        return stageTitles.get(stageName);
    }

    /**
     * Checks if the primary stage has been initialized and is available for use.
     * <p>
     * This method provides a safe way to check stage initialization status without
     * triggering IllegalStateException like {@link #getPrimaryStage()} would.
     * </p>
     *
     * <p><b>Usage:</b></p>
     * <pre>
     * // Safe initialization check
     * if (stageManager.isPrimaryStageSet()) {
     *     // Stage is ready for operations
     *     stageManager.getPrimaryStage().setTitle("App Ready");
     * } else {
     *     // Handle uninitialized state
     *     logger.warn("Primary stage not yet initialized");
     * }
     * </pre>
     *
     * <p><b>Common use cases:</b></p>
     * <ul>
     *   <li>Pre-operation validation before accessing the primary stage</li>
     *   <li>Conditional logic based on stage initialization status</li>
     *   <li>Debugging and logging stage lifecycle events</li>
     *   <li>Preventing IllegalStateException in initialization sequences</li>
     * </ul>
     *
     * @return {@code true} if the primary stage has been set via {@link #setPrimaryStage(Stage)},
     *         {@code false} if the stage is null or uninitialized
     *
     * @see #setPrimaryStage(Stage)
     * @see #getPrimaryStage()
     *
     * @apiNote This method is thread-safe for read operations but should be used
     *          in conjunction with proper initialization sequencing
     *
     * @since 2.0
     */
    public boolean isPrimaryStageSet() {
        return primaryStage != null;
    }

    /**
     * Creates a new StageManager instance
     */
    public StageManager() {}

    /**
     * Sets the primary application stage (REQUIRED)
     * Call this once in your Main.start() method
     *
     * @param stage The primary JavaFX stage (cannot be null)
     * @throws IllegalArgumentException if stage is null
     */
    public void setPrimaryStage(Stage stage) {
        if (stage == null) {
            throw new IllegalArgumentException("Primary stage cannot be null");
        }
        primaryStage = stage;
        stageWindowModes.put(stage, currentWindowMode);
    }

    /**
     * Registers a secondary stage with a unique identifier
     *
     * @param stageName Unique name for the stage (cannot be blank)
     * @param newStage The stage to register (cannot be null)
     * @throws IllegalArgumentException if stageName is blank or newStage is null
     * @throws IllegalStateException if stage name is already registered
     */
    public void registerSecondaryStage(String stageName, Stage newStage) {
        if (stageName == null || stageName.isBlank()) {
            throw new IllegalArgumentException("Stage name cannot be null or blank");
        }
        if (newStage == null) {
            throw new IllegalArgumentException("Secondary stage cannot be null");
        }
        if (secondaryStagesMap.containsKey(stageName)) {
            throw new IllegalStateException("Stage name '" + stageName + "' is already registered");
        }

        secondaryStagesMap.put(stageName, newStage);
        stageWindowModes.put(newStage, WindowMode.DECORATED); // Default mode
    }

    /**
     * Unregisters a secondary stage
     *
     * @param stageName Name of the stage to unregister
     * @return true if the stage was found and removed, false otherwise
     */
    public boolean unregisterSecondaryStage(String stageName) {
        Stage stage = secondaryStagesMap.get(stageName);
        if (stage != null) {
            secondaryStagesMap.remove(stageName);
            stageWindowModes.remove(stage);
            return true;
        }
        return false;
    }

    /**
     * Gets the primary application stage
     *
     * @return The primary stage
     * @throws IllegalStateException if primary stage has not been set
     */
    public Stage getPrimaryStage() {
        if (primaryStage == null) {
            throw new IllegalStateException("Primary stage has not been set. Call setPrimaryStage() first.");
        }
        return primaryStage;
    }

    /**
     * Gets a registered secondary stage by name
     *
     * @param stageName Name of the registered stage
     * @return The registered stage, or null if not found
     */
    public Stage getRegisteredStage(String stageName) {
        return secondaryStagesMap.get(stageName);
    }

    /**
     * Gets an unmodifiable view of all registered secondary stages
     *
     * @return Map of stage names to stage instances
     */
    public Map<String, Stage> getSecondaryStagesMap() {
        return Collections.unmodifiableMap(secondaryStagesMap);
    }

    /**
     * Gets all registered secondary stage names
     *
     * @return Set of registered stage names
     */
    public Set<String> getRegisteredStageNames() {
        return Collections.unmodifiableSet(secondaryStagesMap.keySet());
    }

    /**
     * Checks if a stage name is already registered
     *
     * @param stageName Name to check
     * @return true if the name is registered, false otherwise
     */
    public boolean isStageNameRegistered(String stageName) {
        return secondaryStagesMap.containsKey(stageName);
    }

    /**
     * Applies a window mode to a specific stage
     *
     * @param stage The stage to configure
     * @param mode The window mode to apply
     * @throws IllegalArgumentException if stage or mode is null
     */
    public void applyWindowModeToStage(Stage stage, WindowMode mode) {
        if (stage == null) {
            throw new IllegalArgumentException("Stage cannot be null");
        }
        if (mode == null) {
            throw new IllegalArgumentException("WindowMode cannot be null");
        }

        applyWindowModeInternal(stage, mode);

        // Update StageManager state
        stageWindowModes.put(stage, mode);
        if (stage == primaryStage) currentWindowMode = mode;

        // Update internal tracking
        stageWindowModes.put(stage, mode);
        if (stage == primaryStage) currentWindowMode = mode;
    }

    /**
     * Applies a window mode to the primary stage
     *
     * @param mode The window mode to apply
     * @throws IllegalStateException if primary stage has not been set
     */
    public void applyWindowMode(WindowMode mode) {
        if (primaryStage == null) {
            throw new IllegalStateException("Primary stage has not been set");
        }
        applyWindowModeToStage(primaryStage, mode);
    }

    /**
     * Gets the current window mode of the primary stage
     *
     * @return The current window mode
     * @throws IllegalStateException if primary stage has not been set
     */
    public WindowMode getCurrentWindowMode() {
        if (primaryStage == null) {
            throw new IllegalStateException("Primary stage has not been set");
        }
        return currentWindowMode;
    }

    /**
     * Gets the window mode for a specific stage
     *
     * @param stage The stage to check
     * @return The window mode for the stage, or null if not found
     */
    public WindowMode getWindowModeForStage(Stage stage) {
        return stageWindowModes.get(stage);
    }

    /**
     * Clears all registered secondary stages
     * Useful for application cleanup or reset scenarios
     */
    public void clearAllSecondaryStages() {
        secondaryStagesMap.clear();
        // Only remove secondary stages from window modes, keep primary
        stageWindowModes.keySet().removeIf(stage -> stage != primaryStage);
    }

    /**
     * Applies the specified window mode to a stage.
     * Must be called after stage initialization and before stage.show().
     * <p>
     * Notes:
     * - JavaFX requires stage.initStyle() before showing the stage.
     * - Fullscreen with decorations is platform-dependent; some OSes hide the chrome anyway.
     * - Developers should expect a "best-effort" result for FULLSCREEN_DECORATED.
     */
    private void applyWindowModeInternal(Stage stage, WindowMode mode) {

        switch (mode) {
            case DECORATED:
                // Standard window with OS chrome
                stage.initStyle(StageStyle.DECORATED);
                stage.setFullScreen(false);
                stage.setMaximized(false);
                break;

            case UNDECORATED:
                // Window without decorations
                stage.initStyle(StageStyle.UNDECORATED);
                stage.setFullScreen(false);
                stage.setMaximized(false);
                break;

            case MAXIMIZED:
                // Windowed fullscreen with OS bars visible
                stage.initStyle(StageStyle.DECORATED);
                stage.setFullScreen(false);
                stage.setMaximized(true);
                break;

            case FULLSCREEN:
                // True fullscreen (OS bars hidden)
                stage.initStyle(StageStyle.UNDECORATED);
                stage.setMaximized(false); // irrelevant in fullscreen
                stage.setFullScreen(true);
                stage.setFullScreenExitHint(""); // hide exit hint
                stage.setFullScreenExitKeyCombination(KeyCombination.NO_MATCH); // disable exit key
                break;
        }


    }


    // ==================== DEBUGGING TOOLS ====================

    /**
     * Prints all registered stages and their window modes to console
     * Useful for debugging stage management issues
     */
    public void debugPrintStages() {
        System.out.println("=== Stage Manager Debug Info ===");
        System.out.println("Primary Stage: " +
                (primaryStage != null ? primaryStage.getTitle() : "Not set"));
        System.out.println("Primary Window Mode: " + currentWindowMode);

        System.out.println("\nSecondary Stages:");
        if (secondaryStagesMap.isEmpty()) {
            System.out.println("  No secondary stages registered");
        } else {
            secondaryStagesMap.forEach((name, stage) -> {
                WindowMode mode = stageWindowModes.get(stage);
                System.out.println("  " + name + " -> " +
                        stage.getTitle() + " [" + mode + "]");
            });
        }
        System.out.println("=================================");
    }

    /**
     * Validates stage registry consistency
     *
     * @return true if all stages in registry have window mode entries
     */
    public boolean validateRegistryConsistency() {
        boolean consistent = true;

        // Check primary stage
        if (primaryStage != null && !stageWindowModes.containsKey(primaryStage)) {
            System.err.println("Inconsistency: Primary stage missing window mode entry");
            consistent = false;
        }

        // Check secondary stages
        for (Stage stage : secondaryStagesMap.values()) {
            if (!stageWindowModes.containsKey(stage)) {
                System.err.println("Inconsistency: Secondary stage missing window mode entry: " + stage);
                consistent = false;
            }
        }

        return consistent;
    }
}