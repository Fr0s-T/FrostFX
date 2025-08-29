package org.frost.internal.loaders;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.lang.annotation.Documented;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

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
     * @throws RuntimeException throws RTE if the fxml class fails to load
     */
    public void loadScene(String fxmlPath, Object controller, Stage stage) {
        runOnFxThread(() -> {
            if (stage == null) throw new IllegalStateException("Stage cannot be null");
            if (fxmlPath == null || fxmlPath.isBlank()) throw new IllegalArgumentException("Invalid FXML path");

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
                throw new RuntimeException("FXML Parsing Error: Failed to load '" + fxmlPath + "'. " +
                        "This is typically caused by:\n" +
                        "1. Invalid FXML syntax\n" +
                        "2. A missing or invalid controller class\n" +
                        "3. An invalid resource path inside the FXML file",
                        e);
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
     * @param <T>        The type of the controller
     * @return a CompletableFuture that will hold the controller once loading is complete
     * @throws RuntimeException throws RTE if the fxml class fails to load
     */
    public <T> CompletableFuture<T> loadSceneAsync(String fxmlPath, Object controller) {
        ensurePrimaryStageSet();
        return loadSceneAsync(fxmlPath, controller, primaryStage);
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
     * @return a CompletableFuture that, if completed exceptionally, will contain an Exception
     * *         indicating the reason for failure (e.g., FXML not found, parsing error, etc.)
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

                @SuppressWarnings("unchecked")
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

    // =============== HELPER METHODS ===============]]

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

    /**
     * Clears stage references from the SceneLoader
     * Currently only clears the primary stage reference
     * Method exists for future expansion and API consistency
     */
    public void clearStageReferences() {
        primaryStage = null;
    }

    /**
     * Clears references for a specific stage
     *
     * @param stage The stage to clear references for
     */
    public void clearReferencesForStage(Stage stage) {
        if (stage != null && primaryStage == stage) {
            primaryStage = null;
        }
    }
    /**
     * Clears all registered scene lifecycle listeners
     */
    public void clearAllListeners(){
        sceneListeners.clear();
    }

    private void ensurePrimaryStageSet() {
        if (primaryStage == null) {
            throw new IllegalStateException("Primary stage has not been set. "
                    + "Call SceneManager.init(primaryStage) first.");
        }
    }
}