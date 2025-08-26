package org.frost.helpers;

import javafx.stage.Stage;

/**
 * Contract for controllers that want access to the {@link Stage}
 * of their popup window.
 * <p>
 * When a controller implements this interface, the {@link Stage}
 * is automatically injected by {@code SceneLoader.loadPopupWindow}.
 * </p>
 * @author Frost
 */

public interface PopupController {
    void setDialogStage(Stage stage);
}
