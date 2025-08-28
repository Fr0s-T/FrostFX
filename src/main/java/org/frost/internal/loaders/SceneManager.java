package org.frost.internal.loaders;

import javafx.application.Platform;
import javafx.stage.Stage;
import org.frost.Utilities.AlertUtilities;
import org.frost.Utilities.ResetOptions;
import org.frost.helpers.StageManager;



/**
 * SCENEMANAGER - Central coordinator for JavaFX UI management framework
 * <p>
 * Provides thread-safe access to all UI loading components including scene management,
 * card loading, popup dialogs, and alert utilities. Serves as the main entry point
 * for the Frost UI framework.
 * </p>
 *
 * <p><b>Usage:</b></p>
 * <pre>
 * // Initialize in Main.start()
 * SceneManager.init(primaryStage);
 *
 * // Access loaders throughout application
 * SceneManager.SceneLoader().loadScene("/view/main.fxml", controller);
 * SceneManager.CardLoader().loadCard("userCard", "contentPanel");
 * </pre>
 *
 * @author Frost
 * @version 2.0
 * @since 1.0
 */
public final class SceneManager {

    /**
     * Window display mode enum - controls window styling and behavior
     */
    public enum WindowMode {
        /** Standard window with title bar and OS decoration */
        DECORATED,

        /** Borderless window without OS decoration */
        UNDECORATED,

        /** Fullscreen mode with visible control/notification bar */
        FULLSCREEN_DECORATED,

        /** True borderless fullscreen without any OS UI elements */
        FULLSCREEN_UNDECORATED
    }

    private static WindowMode windowMode = WindowMode.DECORATED;
    private static final StageManager stageManager = new StageManager();
    private static final SceneLoader sceneLoader = new SceneLoader();
    private static final CardLoader cardLoader = new CardLoader();
    private static final PopupLoader popupLoader = new PopupLoader();
    private static final AlertUtilities alertUtilities = new AlertUtilities();

    public static void init(Stage primaryStage) {
        if (primaryStage == null) {
            throw new IllegalArgumentException("Primary stage cannot be null");
        }

        if (stageManager.isPrimaryStageSet()) { // 🛡 NEW PROTECTION
            throw new IllegalStateException("Primary stage already initialized. "
                    + "Call SceneManager.reset() first if you need to change stages.");
        }

        stageManager.setPrimaryStage(primaryStage);
        sceneLoader.setPrimaryStage(primaryStage);
        cardLoader.setPrimaryStage(primaryStage);
    }

    // Add validation to setter
    public static void setWindowMode(WindowMode newWindowMode) {
        if (newWindowMode == null) throw new IllegalArgumentException("WindowMode cannot be null");
        windowMode = newWindowMode;
    }


    public static StageManager StageManager(){return stageManager;}
    public static SceneLoader SceneLoader(){return sceneLoader;}
    public static CardLoader CardLoader() {
        return cardLoader;
    }
    public static PopupLoader PopupLoader(){return popupLoader;}
    public static AlertUtilities AlertUtilities(){return alertUtilities;}
    public static WindowMode getWindowMode() {
        return windowMode;
    }


    /**
     * Resets the SceneManager for stage reinitialization with configurable options
     * Use cases: kiosk mode changes, theme switches, post-login transitions
     *
     * @param options Reset options configuration
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

    public static void reinit(Stage newPrimaryStage, ResetOptions options) {
        reset(options);
        init(newPrimaryStage);
    }


    /**
     * Ensures code runs on JavaFX application thread. Use this for:
     * <ul>
     *   <li>UI updates from background threads</li>
     *   <li>Modifying scene graph elements</li>
     *   <li>Updating observable values that are bound to UI</li>
     * </ul>
     *
     * <p><b>Example:</b></p>
     * <pre>
     * // From background thread:
     * SceneManager.runOnFxThread(() -> {
     *     label.setText("Updated safely from background thread");
     * });
     * </pre>
     *
     * @param action The Runnable to execute on JavaFX application thread
     */
    public static void runOnFxThread(Runnable action) {
        if (Platform.isFxApplicationThread()) action.run();
        else Platform.runLater(action);
    }

}
