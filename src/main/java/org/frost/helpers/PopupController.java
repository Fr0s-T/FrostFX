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
 * @since V1.0.0
 */
public interface PopupController {
    /**
     * override this method and use it as you would use <code>initialize()</code>
     * @param stage a stage
     */
    void setDialogStage(Stage stage);
}
