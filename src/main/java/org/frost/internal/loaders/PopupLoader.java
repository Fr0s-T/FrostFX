package org.frost.internal.loaders;

import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.frost.helpers.PopupConfig;
import org.frost.helpers.PopupController;

import java.io.IOException;

public class PopupLoader {

    public PopupLoader(){}

    /**
     * Loads and displays a popup window from the given FXML file.
     * <p>
     * This method creates a new {@link Stage}, applies the configuration
     * provided in {@link PopupConfig}, and shows the popup either with
     * {@link Stage#show()} or {@link Stage#showAndWait()} depending on the
     * configuration.
     * </p>
     *
     * <p>
     * If the controller of the FXML implements {@link PopupController}, the
     * popup {@link Stage} is automatically injected via
     * {@link PopupController#setDialogStage(Stage)}.
     * </p>
     *
     * @param fxmlPath path to the FXML resource (must not be null or blank)
     * @param config   popup configuration object
     * @param <T>      type of the controller associated with the FXML
     * @throws IllegalArgumentException if {@code fxmlPath} is null or blank
     * @throws RuntimeException         if the FXML could not be loaded
     */

    public <T> void loadPopupWindow(String fxmlPath, PopupConfig config) {
        if (fxmlPath == null || fxmlPath.isBlank())
            throw new IllegalArgumentException("FXML file path can’t be empty or blank");

        try {
            FXMLLoader loader = new FXMLLoader(SceneManager.class.getResource(fxmlPath));
            AnchorPane page = loader.load();

            Stage dialogStage = new Stage();
            dialogStage.setTitle(config.getTitle());
            dialogStage.setResizable(config.isResizable());
            if (config.isModal()) {
                dialogStage.initModality(Modality.APPLICATION_MODAL);
            }

            if (config.getIcon() != null) {
                dialogStage.getIcons().add(config.getIcon());
            }

            Scene scene = new Scene(page, config.getWidth(), config.getHeight());
            dialogStage.setScene(scene);

            // Controller
            T controller = loader.getController();
            if (controller instanceof PopupController) {
                ((PopupController) controller).setDialogStage(dialogStage);
            }

            if (config.isWaitForClose()) {
                dialogStage.showAndWait();
            } else {
                dialogStage.show();
            }

        } catch (IOException e) {
            throw new RuntimeException("Failed to render popup window from " + fxmlPath, e);
        }
    }
}
