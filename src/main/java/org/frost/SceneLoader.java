package org.frost;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public final class SceneLoader {

    // ---- State ----
    private static Stage primaryStage;
    private static AnchorPane cachedDynamicPanel;
    private static String currentPanelView = "";
    private static final Map<String, String> cardPathsMap = new HashMap<>();

    private SceneLoader() {}

    // ---- Setup (stage & panel) ----
    public static void setPrimaryStage(Stage stage) {
        primaryStage = stage;
    }

    public static void setDynamicPanel(AnchorPane panel) {
        cachedDynamicPanel = panel;
    }

    public static String getCurrentPanelView() {
        return currentPanelView;
    }

    // ---- Registry (name -> FXML path) ----
    public static void registerCard(String name, String fxmlPath) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("view name must not be blank");
        if (fxmlPath == null || fxmlPath.isBlank()) throw new IllegalArgumentException("fxmlPath must not be blank");
        cardPathsMap.put(name, fxmlPath);
    }

    // ---- Full-scene swap (rare) ----
    public static void loadScene(String fxmlPath, Object controller) {
        runOnFxThread(() -> {
            if (primaryStage == null) {
                throw new IllegalStateException("Primary stage not set. Call SceneLoader.setPrimaryStage(stage) first.");
            }
            try {
                URL url = SceneLoader.class.getResource(fxmlPath);
                if (url == null) throw new IllegalArgumentException("FXML not found on classpath: " + fxmlPath);

                FXMLLoader loader = new FXMLLoader(url);
                if (controller != null) loader.setController(controller);
                Parent root = loader.load();

                Scene scene = new Scene(root);
                primaryStage.setScene(scene);
                primaryStage.centerOnScreen();
                primaryStage.show();
            } catch (IOException e) {
                throw new RuntimeException("Failed to load scene: " + fxmlPath, e);
            }
        });
    }

    // ---- Card mount & refresh (generic) ----
    public static void loadCard(String cardName) {
        ensurePanelCached();
        String path = cardPathsMap.get(cardName);
        if (path == null) throw new IllegalArgumentException("Unknown card: " + cardName);
        try {
            mountIntoPanel(cachedDynamicPanel, path, cardName);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load card '" + cardName + "' from " + path, e);
        }
    }

    public static void refreshPanel() {
        if (currentPanelView != null && !currentPanelView.isEmpty()) {
            loadCard(currentPanelView);
        }
    }

    // ---- Internals ----
    private static void ensurePanelCached() {
        if (cachedDynamicPanel == null) {
            throw new IllegalStateException("No dynamic panel cached. Call setDynamicPanel(panel) first.");
        }
    }

    private static void mountIntoPanel(AnchorPane targetPanel, String resourcePath, String logicalName) throws IOException {
        cachedDynamicPanel = targetPanel;

        URL url = SceneLoader.class.getResource(resourcePath);
        if (url == null) throw new IllegalArgumentException("FXML not found on classpath: " + resourcePath);

        FXMLLoader loader = new FXMLLoader(url);
        Parent card = loader.load();

        runOnFxThread(() -> {
            targetPanel.getChildren().setAll(card);
            AnchorPane.setTopAnchor(card, 0.0);
            AnchorPane.setRightAnchor(card, 0.0);
            AnchorPane.setBottomAnchor(card, 0.0);
            AnchorPane.setLeftAnchor(card, 0.0);
            currentPanelView = logicalName;
        });
    }

    private static void runOnFxThread(Runnable action) {
        if (Platform.isFxApplicationThread()) action.run();
        else Platform.runLater(action);
    }
}
