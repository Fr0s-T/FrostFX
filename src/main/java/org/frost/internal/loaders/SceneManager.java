package org.frost.internal.loaders;

import javafx.application.Platform;
import javafx.stage.Stage;
import org.frost.Utilities.AlertUtilities;
import org.frost.helpers.StageManager;

/**
 * SCENELOADER - Professional JavaFX Scene and Component Management
 * <p>
 * A lightweight, thread-safe utility for managing JavaFX scene navigation,
 * dynamic card loading, and UI composition. Handles FXML loading, threading,
 * and lifecycle management automatically.
 * <p>
 * USAGE:
 * <p>
 * 1. In Main.java: SceneLoader.setPrimaryStage(primaryStage);
 * <p>
 * 2. Register components: registerCard(), registerDynamicPanel(), registerContainer()
 * <p>
 * 3. Load content: loadScene(), loadCard(), loadCards()
 *
 * @author Frost
 * @version 1.0
 */

public final class SceneManager {

    public enum WindowMode {
        DECORATED,          // normal window with title bar
        UNDECORATED,        // borderless but not fullscreen
        FULLSCREEN_DECORATED,   // fullscreen with bar (toggle)
        FULLSCREEN_UNDECORATED  // true fullscreen borderless (requires stage trick)
    }

    private static WindowMode windowMode = null;
    private static final StageManager stateManager = new StageManager();
    private static final SceneLoader frameLoader = new SceneLoader();
    private static final CardLoader cardLoader = new CardLoader();
    private static final PopupLoader popupLoader = new PopupLoader();
    private static final AlertUtilities alertUtilities = new AlertUtilities();

    public static void innit(Stage primaryStage){
        stateManager.setPrimaryStage(primaryStage);
        frameLoader.setPrimaryStage(primaryStage);
        cardLoader.setPrimaryStage(primaryStage);
    }

    public static void setWindowMode(WindowMode newWindowMode){windowMode = newWindowMode;}


    public static StageManager StageManager(){return stateManager;}
    public static SceneLoader FrameLoader(){return frameLoader;}
    public static CardLoader CardLoader() {
        return cardLoader;
    }
    public static PopupLoader PopupLoader(){return popupLoader;}
    public static AlertUtilities AlertUtilities(){return alertUtilities;}
    public static WindowMode getWindowMode() {
        return windowMode;
    }


    /** Ensures code runs on JavaFX application thread */
    public static void runOnFxThread(Runnable action) {
        if (Platform.isFxApplicationThread()) action.run();
        else Platform.runLater(action);
    }

}
