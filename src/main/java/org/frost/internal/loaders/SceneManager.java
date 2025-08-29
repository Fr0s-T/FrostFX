package org.frost.internal.loaders;

import javafx.application.Platform;
import javafx.stage.Stage;
import org.frost.Utilities.AlertUtilities;
import org.frost.Utilities.ResetOptions;
import org.frost.helpers.StageManager;

/**
 * The central entry point and orchestrator for the FrostFX UI management framework.
 * Provides unified, thread-safe access to all framework components and services.
 * <p>
 * This class follows the facade pattern, presenting a simple interface to the
 * complex underlying framework architecture. All component access is thread-safe
 * and designed for seamless integration throughout the application lifecycle.
 *
 * <h3>Initialization</h3>
 * <pre>
 * // In Application.start()
 * {@literal @}Override
 * public void start(Stage primaryStage) {
 *     SceneManager.init(primaryStage);
 *     // Framework is now ready
 * }
 * </pre>
 *
 * <h3>Typical Usage</h3>
 * <pre>
 * // Load scenes (major navigation)
 * SceneManager.SceneLoader().loadScene("/views/main.fxml", controller);
 *
 * // Load dynamic content (within scenes)
 * SceneManager.CardLoader().loadCards("contentPane", items, "/cards/item.fxml");
 *
 * // Show dialogs and alerts
 * SceneManager.AlertUtilities().showInformation("Title", "Message");
 * </pre>
 *
 * @see StageManager
 * @see SceneLoader
 * @see CardLoader
 * @see PopupLoader
 * @see AlertUtilities
 */
public final class SceneManager {

    /**
     * Window display modes controlling window styling and behavior.
     */
    public enum WindowMode {
        /** Standard window with title bar and borders */
        DECORATED,
        /** Window without OS decorations (custom chrome) */
        UNDECORATED,
        /** Window maximized to screen dimensions (taskbar visible) */
        MAXIMIZED,
        /** Exclusive fullscreen mode (OS bars hidden) */
        FULLSCREEN
    }

    private static WindowMode windowMode = WindowMode.DECORATED;
    private static final StageManager stageManager = new StageManager();
    private static final SceneLoader sceneLoader = new SceneLoader();
    private static final CardLoader cardLoader = new CardLoader();
    private static final PopupLoader popupLoader = new PopupLoader();
    private static final AlertUtilities alertUtilities = new AlertUtilities();

    private SceneManager() {
        // Private constructor to prevent instantiation
    }

    /**
     * Initializes the framework with the primary application stage.
     * Must be called before any other framework operations.
     *
     * @param primaryStage the primary JavaFX stage from Application.start()
     * @throws IllegalArgumentException if primaryStage is null
     * @throws IllegalStateException if already initialized (use reset() first)
     */
    public static void init(Stage primaryStage) {
        if (primaryStage == null) {
            throw new IllegalArgumentException("Primary stage cannot be null");
        }

        if (stageManager.isPrimaryStageSet()) {
            throw new IllegalStateException("Primary stage already initialized. "
                    + "Call SceneManager.reset() first if you need to change stages.");
        }

        stageManager.setPrimaryStage(primaryStage);
        sceneLoader.setPrimaryStage(primaryStage);
        cardLoader.setPrimaryStage(primaryStage);
    }

    /**
     * Sets the global window display mode for new stages.
     *
     * @param newWindowMode the window mode to apply
     * @throws IllegalArgumentException if newWindowMode is null
     */
    public static void setWindowMode(WindowMode newWindowMode) {
        if (newWindowMode == null) throw new IllegalArgumentException("WindowMode cannot be null");
        windowMode = newWindowMode;
    }

    /**
     * Returns the stage management component for multi-window applications.
     *
     * @return the StageManager instance
     */
    public static StageManager StageManager() { return stageManager; }

    /**
     * Returns the scene loading component for major navigation.
     *
     * @return the SceneLoader instance
     */
    public static SceneLoader SceneLoader() { return sceneLoader; }

    /**
     * Returns the card loading component for dynamic content management.
     *
     * @return the CardLoader instance
     */
    public static CardLoader CardLoader() { return cardLoader; }

    /**
     * Returns the popup window management component.
     *
     * @return the PopupLoader instance
     */
    public static PopupLoader PopupLoader() { return popupLoader; }

    /**
     * Returns the alert and dialog utilities component.
     *
     * @return the AlertUtilities instance
     */
    public static AlertUtilities AlertUtilities() { return alertUtilities; }

    /**
     * Returns the current global window display mode.
     *
     * @return the active WindowMode
     */
    public static WindowMode getWindowMode() { return windowMode; }

    /**
     * Resets framework state with configurable options.
     * Useful for application mode changes, theme switches, or reinitialization.
     *
     * @param options configuration specifying what components to reset
     * @throws IllegalArgumentException if options is null
     *
     * @see ResetOptions
     */
    public static void reset(ResetOptions options) {
        if (options == null) {
            throw new IllegalArgumentException("ResetOptions cannot be null");
        }

        if (options.shouldClearPrimaryStage()) {
            stageManager.setPrimaryStage(new Stage());
            sceneLoader.clearStageReferences();
        }

        if (options.shouldClearSecondaryStages()) {
            stageManager.clearAllSecondaryStages();
        }

        if (options.shouldClearCardRegistrations()) {
            if (options.shouldClearAllCards()) {
                cardLoader.clearAll();
            } else {
                cardLoader.clearAllRegistrations();
            }
        }

        if (options.shouldClearListeners()) {
            sceneLoader.clearAllListeners();
        }

        if (options.shouldResetWindowMode()) {
            windowMode = WindowMode.DECORATED;
        }
    }

    /**
     * Reinitialized the framework with a new primary stage.
     * Combines reset() and init() for convenience.
     *
     * @param newPrimaryStage the new primary stage
     * @param options reset configuration options
     */
    public static void reinit(Stage newPrimaryStage, ResetOptions options) {
        reset(options);
        init(newPrimaryStage);
    }

    /**
     * Ensures code execution on the JavaFX application thread.
     * Use for all UI modifications from background threads.
     *
     * <p>Examples:</p>
     * <pre>
     * // Safe UI update from background thread
     * SceneManager.runOnFxThread(() -> label.setText("Updated"));
     *
     * // Safe scene graph modification
     * SceneManager.runOnFxThread(() -> container.getChildren().add(newNode));
     * </pre>
     *
     * @param action the operation to execute on the JavaFX thread
     */
    public static void runOnFxThread(Runnable action) {
        if (Platform.isFxApplicationThread()) {
            action.run();
        } else {
            Platform.runLater(action);
        }
    }
}