package org.frost.internal.loaders;

import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.frost.helpers.PopupConfig;
import org.frost.helpers.PopupController;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.frost.internal.loaders.SceneManager.runOnFxThread;

/**
 * POPUPLOADER - Professional popup and dialog management system
 * <p>
 * Handles creation, configuration, and display of modal and non-modal popup windows.
 * Supports both blocking (showAndWait) and non-blocking (show) popup operations.
 * </p>
 *
 * <p><b>Usage:</b></p>
 * <pre>
 * PopupConfig config = new PopupConfig()
 *     .title("User Settings")
 *     .size(400, 300)
 *     .modal(true)
 *     .waitForClose(true);
 *
 * SceneManager.PopupLoader().loadPopupWindow("/popups/settings.fxml", config);
 * </pre>
 *
 * @author Frost
 * @version 2.0
 * @since 1.0
 */
public class PopupLoader {

    private final Map<String, Stage> activePopups = new HashMap<>();

    public PopupLoader() {}

    /**
     * Loads and displays a popup window from the given FXML file.
     *
     * @param fxmlPath path to the FXML resource (must not be null or blank)
     * @param config   popup configuration object
     * @param <T>      type of the controller associated with the FXML
     * @throws IllegalArgumentException if {@code fxmlPath} is null or blank
     * @throws RuntimeException         if the FXML could not be loaded
     */
    public <T> void loadPopupWindow(String fxmlPath, PopupConfig config) {
        loadPopupWindow(fxmlPath, config, null);
    }

    /**
     * Loads and displays a popup window with custom controller
     *
     * @param fxmlPath   path to the FXML resource
     * @param config     popup configuration object
     * @param controller custom controller instance (null for FXML-defined)
     * @param <T>        type of the controller
     */
    public <T> void loadPopupWindow(String fxmlPath, PopupConfig config, T controller) {
        loadPopupWindow(fxmlPath, config, controller, null);
    }

    /**
     * Loads and displays a popup window with parent window ownership
     *
     * @param fxmlPath   path to the FXML resource
     * @param config     popup configuration object
     * @param controller custom controller instance
     * @param owner      parent window for modal relationship
     * @param <T>        type of the controller
     */
    public <T> void loadPopupWindow(String fxmlPath, PopupConfig config, T controller, Window owner) {
        if (fxmlPath == null || fxmlPath.isBlank()) {
            throw new IllegalArgumentException("FXML file path cannot be null or blank");
        }
        if (config == null) {
            throw new IllegalArgumentException("PopupConfig cannot be null");
        }

        runOnFxThread(() -> {
            try {
                URL url = SceneManager.class.getResource(fxmlPath);
                if (url == null) {
                    throw new IllegalArgumentException("FXML not found: " + fxmlPath);
                }

                FXMLLoader loader = new FXMLLoader(url);
                if (controller != null) {
                    loader.setController(controller);
                }

                Pane root = loader.load();
                Stage dialogStage = createDialogStage(config, root, owner);

                T loadedController = loader.getController();
                setupController(loadedController, dialogStage, config);

                showDialog(dialogStage, config, fxmlPath);

            } catch (IOException e) {
                throw new RuntimeException("Failed to load popup window: " + fxmlPath, e);
            }
        });
    }

    /**
     * Loads a popup window asynchronously and returns a CompletableFuture for the controller
     *
     * @param fxmlPath path to the FXML resource
     * @param config   popup configuration object
     * @param <T>      type of the controller
     * @return CompletableFuture that completes with the popup controller
     */
    public <T> CompletableFuture<T> loadPopupWindowAsync(String fxmlPath, PopupConfig config) {
        CompletableFuture<T> future = new CompletableFuture<>();

        runOnFxThread(() -> {
            try {
                loadPopupWindow(fxmlPath, config, null);
                // Note: For async, we might need to return the controller differently
                // This is a simplified version
                future.complete(null); // Would need to capture controller
            } catch (Exception e) {
                future.completeExceptionally(e);
            }
        });

        return future;
    }

    /**
     * Closes a previously opened popup by its configuration ID or title
     *
     * @param popupId identifier used when opening the popup
     * @return true if popup was found and closed, false otherwise
     */
    public boolean closePopup(String popupId) {
        Stage popup = activePopups.get(popupId);
        if (popup != null) {
            runOnFxThread(popup::close);
            activePopups.remove(popupId);
            return true;
        }
        return false;
    }

    /**
     * Closes all active popups
     */
    public void closeAllPopups() {
        runOnFxThread(() -> {
            activePopups.values().forEach(Stage::close);
            activePopups.clear();
        });
    }

    // ==================== PRIVATE HELPERS ====================//

    private Stage createDialogStage(PopupConfig config, Pane root, Window owner) {
        Stage dialogStage = new Stage();

        if (owner != null) {
            dialogStage.initOwner(owner);
        }

        dialogStage.setTitle(config.getTitle());
        dialogStage.setResizable(config.isResizable());

        if (config.isModal() && owner != null) {
            dialogStage.initModality(Modality.WINDOW_MODAL);
        } else if (config.isModal()) {
            dialogStage.initModality(Modality.APPLICATION_MODAL);
        }

        if (config.getIcon() != null) {
            dialogStage.getIcons().add(config.getIcon());
        }

        Scene scene = new Scene(root, config.getWidth(), config.getHeight());
        dialogStage.setScene(scene);

        return dialogStage;
    }

    private <T> void setupController(T controller, Stage dialogStage, PopupConfig config) {
        if (controller instanceof PopupController) {
            ((PopupController) controller).setDialogStage(dialogStage);
        }

        // Additional controller setup could go here
        if (config.getControllerConfigurator() != null) {
            config.getControllerConfigurator().accept(controller);
        }
    }

    private void showDialog(Stage dialogStage, PopupConfig config, String fxmlPath) {
        if (config.isWaitForClose()) {
            activePopups.put(fxmlPath, dialogStage);
            dialogStage.showAndWait();
            activePopups.remove(fxmlPath);
        } else {
            activePopups.put(fxmlPath, dialogStage);
            dialogStage.show();

            // Auto-remove from active popups when closed
            dialogStage.setOnHidden(e -> activePopups.remove(fxmlPath));
        }
    }

    /**
     * Gets all currently active popups
     *
     * @return map of popup identifiers to stages
     */
    public Map<String, Stage> getActivePopups() {
        return new HashMap<>(activePopups);
    }

    /**
     * Checks if a specific popup is currently open
     *
     * @param popupId identifier used when opening the popup
     * @return true if popup is open, false otherwise
     */
    public boolean isPopupOpen(String popupId) {
        Stage popup = activePopups.get(popupId);
        return popup != null && popup.isShowing();
    }
}