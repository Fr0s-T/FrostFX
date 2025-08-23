package org.frost;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.image.Image;
import javafx.stage.Stage;

/**
 * A comprehensive utility class for displaying standardized, thread-safe alert dialogs.
 * <p>
 * Provides methods for common alert types (information, warning, error, confirmation)
 and ensures they are displayed on the JavaFX Application Thread. Supports both a global
 * default application icon and per-alert custom icons for maximum flexibility.
 *
 * @author Frost
 */
public final class AlertUtilities {

    /** The global default icon to be displayed on alert windows. */
    private static Image globalAppIcon;

    /**
     * Sets the global default icon for all alerts that do not specify a custom icon.
     * This method should typically be called once during application startup.
     *
     * @param icon the {@link Image} to use as the default window icon for alerts,
     *             or {@code null} to use no icon.
     */
    public static void setGlobalAppIcon(Image icon) {
        globalAppIcon = icon;
    }

    /**
     * Displays a warning alert dialog using the global default icon.
     *
     * @param title   the title text for the alert dialog
     * @param message the content message to display
     */
    public static void showWarning(String title, String message) {
        showAlert(Alert.AlertType.WARNING, title, message, globalAppIcon);
    }

    /**
     * Displays a warning alert dialog with a custom icon.
     *
     * @param title   the title text for the alert dialog
     * @param message the content message to display
     * @param icon    the custom {@link Image} to use for this alert's icon.
     *                Overrides the global default. Pass {@code null} to use no icon.
     */
    public static void showWarning(String title, String message, Image icon) {
        showAlert(Alert.AlertType.WARNING, title, message, icon);
    }

    /**
     * Displays a success/information alert dialog with a predefined "Success" title
     * and the global default icon.
     *
     * @param message the content message to display
     */
    public static void showSuccess(String message) {
        showAlert(Alert.AlertType.INFORMATION, "Success", message, globalAppIcon);
    }

    /**
     * Displays a success/information alert dialog with a predefined "Success" title
     * and a custom icon.
     *
     * @param message the content message to display
     * @param icon    the custom {@link Image} to use for this alert's icon.
     *                Overrides the global default. Pass {@code null} to use no icon.
     */
    public static void showSuccess(String message, Image icon) {
        showAlert(Alert.AlertType.INFORMATION, "Success", message, icon);
    }

    /**
     * Displays an error alert dialog using the global default icon.
     *
     * @param title   the title text for the alert dialog
     * @param message the content message to display
     */
    public static void showError(String title, String message) {
        showAlert(Alert.AlertType.ERROR, title, message, globalAppIcon);
    }

    /**
     * Displays an error alert dialog with a custom icon.
     *
     * @param title   the title text for the alert dialog
     * @param message the content message to display
     * @param icon    the custom {@link Image} to use for this alert's icon.
     *                Overrides the global default. Pass {@code null} to use no icon.
     */
    public static void showError(String title, String message, Image icon) {
        showAlert(Alert.AlertType.ERROR, title, message, icon);
    }

    /**
     * Displays a confirmation dialog with "OK" and "Cancel" buttons and the global default icon.
     * This method will block and return the user's choice if called on the FX application thread.
     * If called from a background thread, it shows the dialog asynchronously and returns {@code false}.
     *
     * @param title   the title text for the confirmation dialog
     * @param message the content message to display
     * @return {@code true} if the user clicked OK, {@code false} if they clicked Cancel, closed the dialog,
     *         or if this was called from a background thread.
     */
    public static boolean showConfirmation(String title, String message) {
        return showConfirmationInternal(title, message, globalAppIcon);
    }

    /**
     * Displays a confirmation dialog with "OK" and "Cancel" buttons and a custom icon.
     * This method will block and return the user's choice if called on the FX application thread.
     * If called from a background thread, it shows the dialog asynchronously and returns {@code false}.
     *
     * @param title   the title text for the confirmation dialog
     * @param message the content message to display
     * @param icon    the custom {@link Image} to use for this alert's icon.
     *                Overrides the global default. Pass {@code null} to use no icon.
     * @return {@code true} if the user clicked OK, {@code false} if they clicked Cancel, closed the dialog,
     *         or if this was called from a background thread.
     */
    public static boolean showConfirmation(String title, String message, Image icon) {
        return showConfirmationInternal(title, message, icon);
    }

    // ========== PRIVATE INTERNAL IMPLEMENTATION METHODS ========== //

    /**
     * Internal method to display a confirmation dialog.
     *
     * @param title   the title text for the dialog
     * @param message the content message to display
     * @param icon    the icon to apply, or {@code null}
     * @return the user's confirmation choice, or false if called off the FX thread
     */
    private static boolean showConfirmationInternal(String title, String message, Image icon) {
        final boolean[] confirmed = {false};

        Runnable dialog = () -> {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION, message, ButtonType.OK, ButtonType.CANCEL);
            alert.setTitle(title);
            alert.setHeaderText(null);
            applyIconToAlert(alert, icon);
            alert.showAndWait();
            confirmed[0] = (alert.getResult() == ButtonType.OK);
        };

        if (Platform.isFxApplicationThread()) {
            dialog.run();
        } else {
            try {
                Platform.runLater(dialog);
            } catch (Exception ignored) {
                // Ignore exceptions from platform runLater
            }
            return false;
        }
        return confirmed[0];
    }

    /**
     * Internal method to display a generic alert on the correct thread.
     *
     * @param type    the type of alert to display
     * @param title   the title text for the alert
     * @param message the content message to display
     * @param icon    the icon to apply, or {@code null}
     */
    private static void showAlert(Alert.AlertType type, String title, String message, Image icon) {
        Runnable dialog = () -> {
            Alert alert = new Alert(type);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            applyIconToAlert(alert, icon);
            alert.showAndWait();
        };

        if (Platform.isFxApplicationThread()) {
            dialog.run();
        } else {
            Platform.runLater(dialog);
        }
    }

    /**
     * Applies the specified icon to an alert's window.
     *
     * @param alert the alert to apply the icon to
     * @param icon  the {@link Image} to apply, or {@code null} to apply no icon
     */
    private static void applyIconToAlert(Alert alert, Image icon) {
        if (icon != null) {
            Stage alertStage = (Stage) alert.getDialogPane().getScene().getWindow();
            alertStage.getIcons().add(icon);
        }
    }
}