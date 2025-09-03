package org.frost.internal.loaders;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;

import java.util.function.BiConsumer;

import static org.frost.internal.loaders.SceneManager.runOnFxThread;

/**
 * <p>SceneLoader class.</p>
 *
 */
public class SceneLoader {

    private Stage primaryStage;

    /** List of scene lifecycle listeners for hooks */
    private static final List<SceneLoaderListener> sceneListeners = new CopyOnWriteArrayList<>();


    public interface SceneLoaderListener {
        /** Called immediately before a scene begins loading
         *
         *
         * @param fxmlPath the path to seen you want to attach this to
         * */
        void onBeforeSceneLoad(String fxmlPath);

        /** Called immediately after a scene finishes loading
         *
         * @param fxmlPath the path to seen you want to attach this to
         * */
        void onAfterSceneLoad(String fxmlPath, Object controller);
    }

    /**
     * <p>Constructor for SceneLoader.</p>
     */
    public SceneLoader() { }

    void setPrimaryStage(Stage stage) { this.primaryStage = stage; }

    /**
     * Registers a lifecycle listener for scene loading events
     * Perfect for analytics, permissions, preloading, etc.
     *
     * @param listener Implementation of SceneLoaderListener
     */
    public void addSceneLoaderListener(SceneLoaderListener listener) {
        sceneListeners.add(listener);
    }

    /**
     * Removes a previously registered lifecycle listener
     *
     * @param listener the listener you want to remove
     */
    public void removeSceneLoaderListener(SceneLoaderListener listener) {
        sceneListeners.remove(listener);
    }

    /**
     * Loads a complete scene into a stage (void version).
     * Use for major navigation events (e.g., login → dashboard).
     * This method is thread-safe.
     *
     * @param fxmlPath   Classpath path to the scene FXML
     * @param controller Optional custom controller (null for FXML-defined)
     * @throws java.lang.RuntimeException throws RTE if the fxml class fails to load
     */
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
     * @throws java.lang.RuntimeException throws RTE if the fxml class fails to load
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
     * @throws java.lang.RuntimeException throws RTE if the fxml class fails to load
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

    // ===============Create a controller and use it to load a scene===================//

    public <T, C> CompletableFuture<T> createControllerAsync(String fxmlPath, C context,
                                                             BiConsumer<T, C> preInit) {
        CompletableFuture<T> future = new CompletableFuture<>();

        CompletableFuture.runAsync(() -> {
            try {
                URL url = getClass().getResource(fxmlPath);
                if (url == null) {
                    future.completeExceptionally(new IllegalArgumentException("FXML not found: " + fxmlPath));
                    return;
                }

                FXMLLoader loader = new FXMLLoader(url);
                Parent root = loader.load();

                @SuppressWarnings("unchecked")
                T controller = (T) loader.getController();

                // Validate controller isn't null
                if (controller == null) {
                    future.completeExceptionally(new IllegalStateException(
                            "Controller is null. Check FXML fx:controller declaration or custom controller setup"));
                    return;
                }

                // Pre-initialization with null-safe checks
                if (preInit != null) {
                    try {
                        preInit.accept(controller, context);
                    } catch (Exception e) {
                        future.completeExceptionally(new RuntimeException("Pre-initialization failed", e));
                        return;
                    }
                }

                future.complete(controller);

            } catch (Exception e) {
                future.completeExceptionally(e);
            }
        });

        return future;
    }

    /**
     * Attaches an existing controller to a scene/stage without recreating the controller.
     * Perfect for when you want to reuse a pre-initialized controller.
     */
    public <T> void attachControllerToScene(String fxmlPath, T existingController, Stage stage) {
        runOnFxThread(() -> {
            try {
                URL url = getClass().getResource(fxmlPath);
                if (url == null) throw new IllegalArgumentException("FXML not found: " + fxmlPath);

                FXMLLoader loader = getFxmlLoader(existingController, url);

                Parent root = loader.load();
                Scene scene = new Scene(root);
                stage.setScene(scene);
                stage.centerOnScreen();
                stage.show();

            } catch (Exception e) {
                throw new RuntimeException("Failed to attach controller to scene", e);
            }
        });
    }

    private <T> FXMLLoader getFxmlLoader(T existingController, URL url) {
        FXMLLoader loader = new FXMLLoader(url);

        // Enhanced controller factory
        loader.setControllerFactory(param -> {
            // Return existing controller if types match
            if (param.isInstance(existingController)) {
                return existingController;
            }

            // For other controllers (if any), create new instances
            try {
                return param.getDeclaredConstructor().newInstance();
            } catch (Exception e) {
                throw new RuntimeException("Failed to create controller: " + param.getName(), e);
            }
        });
        return loader;
    }
    // =============== HELPER METHODS ===============//

    /** Triggers all before-load lifecycle hooks
     *
     * @param fxmlPath  the path to seen you want to attach this to
     * */
    private void fireBeforeSceneLoad(String fxmlPath) {
        for (SceneLoaderListener listener : sceneListeners) {
            listener.onBeforeSceneLoad(fxmlPath);
        }
    }

    /** Triggers all after-load lifecycle hooks
     *
     * @param fxmlPath the path to seen you want to attach this to
     * */
    private void fireAfterSceneLoad(String fxmlPath, Object controller) {
        for (SceneLoaderListener listener : sceneListeners) {
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
