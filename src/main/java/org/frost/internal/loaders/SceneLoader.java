package org.frost.internal.loaders;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.input.KeyCombination;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.frost.internal.loaders.SceneManager.WindowMode;
import static org.frost.internal.loaders.SceneManager.runOnFxThread;

public class SceneLoader {

    private Stage primaryStage;

    /** List of scene lifecycle listeners for hooks */
    private static final List<FrameLoaderListener> sceneListeners = new ArrayList<>();

    public interface FrameLoaderListener {
        /** Called immediately before a scene begins loading */
        void onBeforeSceneLoad(String fxmlPath);

        /** Called immediately after a scene finishes loading */
        void onAfterSceneLoad(String fxmlPath, Object controller);
    }

    public SceneLoader() { }

    void setPrimaryStage(Stage stage) { this.primaryStage = stage; }

    /**
     * Registers a lifecycle listener for scene loading events
     * Perfect for analytics, permissions, preloading, etc.
     *
     * @param listener Implementation of FrameLoaderListener
     */
    public void addSceneLoaderListener(FrameLoaderListener listener) {
        sceneListeners.add(listener);
    }

    /**
     * Removes a previously registered lifecycle listener
     */
    public void removeSceneLoaderListener(FrameLoaderListener listener) {
        sceneListeners.remove(listener);
    }


    public void loadScene(String fxmlPath, Object controller){
        loadScene(fxmlPath, controller, primaryStage);
    }

    /**
     * Loads a complete scene into a stage (void version).
     * Use for major navigation events (e.g., login → dashboard).
     * This method is thread-safe.
     *
     * @param fxmlPath   Classpath path to the scene FXML
     * @param controller Optional custom controller (null for FXML-defined)
     * @param stage      The stage to load the scene into
     */
    public void loadScene(String fxmlPath, Object controller, Stage stage) {
        runOnFxThread(() -> {
            if (stage == null) {
                throw new IllegalStateException("Stage cannot be null.");
            }

            try {
                fireBeforeSceneLoad(fxmlPath);

                URL url = getClass().getResource(fxmlPath);
                if (url == null) throw new IllegalArgumentException("FXML not found: " + fxmlPath);

                FXMLLoader loader = new FXMLLoader(url);
                if (controller != null) loader.setController(controller);
                Parent root = loader.load();

                Object loadedController = loader.getController();

                Scene scene = new Scene(root);
                stage.setScene(scene);
                stage.centerOnScreen();
                stage.show();

                fireAfterSceneLoad(fxmlPath, loadedController);

            } catch (IOException e) {
                throw new RuntimeException("Failed to load scene: " + fxmlPath, e);
            }
        });
    }

    /**
     * Loads a complete scene into a stage and returns a Future for its controller.
     * Use when you need a reference to the controller after loading.
     * The future completes on the JavaFX Application Thread.
     *
     * @param fxmlPath   Classpath path to the scene FXML
     * @param controller Optional custom controller (null for FXML-defined)
     * @param stage      The stage to load the scene into
     * @param <T>        The type of the controller
     * @return a CompletableFuture that will hold the controller once loading is complete
     */
    public <T> CompletableFuture<T> loadSceneAsync(String fxmlPath, Object controller, Stage stage) {
        CompletableFuture<T> future = new CompletableFuture<>();

        runOnFxThread(() -> {
            try {
                fireBeforeSceneLoad(fxmlPath);

                URL url = getClass().getResource(fxmlPath);
                if (url == null) {
                    future.completeExceptionally(new IllegalArgumentException("FXML not found: " + fxmlPath));
                    return;
                }

                FXMLLoader loader = new FXMLLoader(url);
                if (controller != null) loader.setController(controller);
                Parent root = loader.load();

                T loadedController = (T) loader.getController();

                Scene scene = new Scene(root);
                stage.setScene(scene);
                stage.centerOnScreen();
                stage.show();

                fireAfterSceneLoad(fxmlPath, loadedController);
                future.complete(loadedController);

            } catch (Exception e) {
                future.completeExceptionally(e);
            }
        });

        return future;
    }

    // =============== NEW WINDOWMODE OVERLOADS ===============

    /**
     * Loads a scene with specific window mode
     * @param fxmlPath Classpath path to the scene FXML
     * @param controller Optional custom controller
     * @param windowMode The window mode to apply
     */
    public void loadScene(String fxmlPath, Object controller, WindowMode windowMode) {
        loadScene(fxmlPath, controller, primaryStage, windowMode);
    }

    /**
     * Loads a scene with specific window mode and stage
     * @param fxmlPath Classpath path to the scene FXML
     * @param controller Optional custom controller
     * @param stage The stage to load into
     * @param windowMode The window mode to apply
     */
    public void loadScene(String fxmlPath, Object controller, Stage stage, WindowMode windowMode) {
        runOnFxThread(() -> {
            // Apply window mode first
            applyWindowModeToStage(stage, windowMode);

            // Then load the scene using existing logic
            loadScene(fxmlPath, controller, stage);
        });
    }

    /**
     * Loads a scene asynchronously with specific window mode
     * @param fxmlPath Classpath path to the scene FXML
     * @param controller Optional custom controller
     * @param windowMode The window mode to apply
     * @return Future that will hold the controller
     */
    public <T> CompletableFuture<T> loadSceneAsync(String fxmlPath, Object controller, WindowMode windowMode) {
        return loadSceneAsync(fxmlPath, controller, primaryStage, windowMode);
    }

    /**
     * Loads a scene asynchronously with specific window mode and stage
     * @param fxmlPath Classpath path to the scene FXML
     * @param controller Optional custom controller
     * @param stage The stage to load into
     * @param windowMode The window mode to apply
     * @return Future that will hold the controller
     */
    public <T> CompletableFuture<T> loadSceneAsync(String fxmlPath, Object controller, Stage stage, WindowMode windowMode) {
        CompletableFuture<T> future = new CompletableFuture<>();

        runOnFxThread(() -> {
            try {
                // Apply window mode
                applyWindowModeToStage(stage, windowMode);

                // Then proceed with async loading
                fireBeforeSceneLoad(fxmlPath);

                URL url = getClass().getResource(fxmlPath);
                if (url == null) {
                    future.completeExceptionally(new IllegalArgumentException("FXML not found: " + fxmlPath));
                    return;
                }

                FXMLLoader loader = new FXMLLoader(url);
                if (controller != null) loader.setController(controller);
                Parent root = loader.load();

                T loadedController = (T) loader.getController();

                Scene scene = new Scene(root);
                stage.setScene(scene);
                stage.centerOnScreen();
                stage.show();

                fireAfterSceneLoad(fxmlPath, loadedController);
                future.complete(loadedController);

            } catch (Exception e) {
                future.completeExceptionally(e);
            }
        });

        return future;
    }

    // =============== HELPER METHODS ===============

    // Private helper method to apply window mode to a stage
    private void applyWindowModeToStage(Stage stage, WindowMode windowMode) {
        switch (windowMode) {
            case DECORATED:
                stage.setFullScreen(false);
                stage.initStyle(StageStyle.DECORATED);
                break;
            case UNDECORATED:
                stage.setFullScreen(false);
                stage.initStyle(StageStyle.UNDECORATED);
                break;
            case FULLSCREEN_DECORATED:
                stage.setFullScreen(true);
                stage.setFullScreenExitHint("");
                stage.setFullScreenExitKeyCombination(KeyCombination.NO_MATCH);
                break;
            case FULLSCREEN_UNDECORATED:
                stage.setFullScreen(true);
                stage.initStyle(StageStyle.UNDECORATED);
                stage.setFullScreenExitHint("");
                stage.setFullScreenExitKeyCombination(KeyCombination.NO_MATCH);
                break;
        }

        /*// Update StageManager's current window mode if this is the primary stage
        if (stage == primaryStage) {
            SceneManager.StageManager().applyWindowMode(windowMode);
        }*/
    }

    /** Triggers all before-load lifecycle hooks */
    private void fireBeforeSceneLoad(String fxmlPath) {
        for (FrameLoaderListener listener : sceneListeners) {
            listener.onBeforeSceneLoad(fxmlPath);
        }
    }

    /** Triggers all after-load lifecycle hooks */
    private void fireAfterSceneLoad(String fxmlPath, Object controller) {
        for (FrameLoaderListener listener : sceneListeners) {
            listener.onAfterSceneLoad(fxmlPath, controller);
        }
    }
}