package org.frost.Utilities;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

/**
 * ALERTUTILITIES - Comprehensive, thread-safe alert dialog management system
 * <p>
 * Provides standardized methods for common alert types (information, warning, error, confirmation)
 * with guaranteed thread safety. Supports both global default application icons and per-alert
 * custom icons for maximum flexibility.
 * </p>
 *
 * <p><b>Usage:</b></p>
 * <pre>
 * // Set global icon once
 * SceneManager.AlertUtilities().setGlobalAppIcon(appIcon);
 *
 * // Show alerts from any thread
 * SceneManager.AlertUtilities().showSuccess("Operation completed!");
 * SceneManager.AlertUtilities().showError("Error", "Something went wrong");
 *
 * boolean confirmed = SceneManager.AlertUtilities().showConfirmation("Confirm", "Are you sure?");
 * </pre>
 *
 * @author Frost
 * @version 2.0
 * @since 1.0
 */
public final class AlertUtilities {

    /** The global default icon to be displayed on alert windows. */
    private Image globalAppIcon;

    /**
     * Sets the global default icon for all alerts that do not specify a custom icon.
     * This method should typically be called once during application startup.
     *
     * @param icon the {@link Image} to use as the default window icon for alerts,
     *             or {@code null} to use no icon.
     */
    public void setGlobalAppIcon(Image icon) {
        globalAppIcon = icon;
    }

    /**
     * Gets the current global default icon.
     *
     * @return the current global icon, or {@code null} if not set
     */
    public Image getGlobalAppIcon() {
        return globalAppIcon;
    }

    // ==================== INFORMATION ALERTS ====================

    /**
     * Displays an information alert dialog using the global default icon.
     *
     * @param title   the title text for the alert dialog
     * @param message the content message to display
     */
    public void showInformation(String title, String message) {
        showAlert(Alert.AlertType.INFORMATION, title, message, globalAppIcon, null);
    }

    /**
     * Displays an information alert dialog with a custom icon.
     *
     * @param title   the title text for the alert dialog
     * @param message the content message to display
     * @param icon    the custom {@link Image} to use for this alert's icon.
     *                Overrides the global default. Pass {@code null} to use no icon.
     */
    public void showInformation(String title, String message, Image icon) {
        showAlert(Alert.AlertType.INFORMATION, title, message, icon, null);
    }

    /**
     * Displays an information alert dialog with a custom owner window.
     *
     * @param title   the title text for the alert dialog
     * @param message the content message to display
     * @param owner   the parent window for this alert
     */
    public void showInformation(String title, String message, Window owner) {
        showAlert(Alert.AlertType.INFORMATION, title, message, globalAppIcon, owner);
    }

    // ==================== WARNING ALERTS ====================

    /**
     * Displays a warning alert dialog using the global default icon.
     *
     * @param title   the title text for the alert dialog
     * @param message the content message to display
     */
    public void showWarning(String title, String message) {
        showAlert(Alert.AlertType.WARNING, title, message, globalAppIcon, null);
    }

    /**
     * Displays a warning alert dialog with a custom icon.
     *
     * @param title   the title text for the alert dialog
     * @param message the content message to display
     * @param icon    the custom {@link Image} to use for this alert's icon.
     *                Overrides the global default. Pass {@code null} to use no icon.
     */
    public void showWarning(String title, String message, Image icon) {
        showAlert(Alert.AlertType.WARNING, title, message, icon, null);
    }

    /**
     * Displays a warning alert dialog with a custom owner window.
     *
     * @param title   the title text for the alert dialog
     * @param message the content message to display
     * @param owner   the parent window for this alert
     */
    public void showWarning(String title, String message, Window owner) {
        showAlert(Alert.AlertType.WARNING, title, message, globalAppIcon, owner);
    }

    // ==================== SUCCESS ALERTS ====================

    /**
     * Displays a success/information alert dialog with a predefined "Success" title
     * and the global default icon.
     *
     * @param message the content message to display
     */
    public void showSuccess(String message) {
        showAlert(Alert.AlertType.INFORMATION, "Success", message, globalAppIcon, null);
    }

    /**
     * Displays a success/information alert dialog with a predefined "Success" title
     * and a custom icon.
     *
     * @param message the content message to display
     * @param icon    the custom {@link Image} to use for this alert's icon.
     *                Overrides the global default. Pass {@code null} to use no icon.
     */
    public void showSuccess(String message, Image icon) {
        showAlert(Alert.AlertType.INFORMATION, "Success", message, icon, null);
    }

    /**
     * Displays a success/information alert dialog with a predefined "Success" title
     * and a custom owner window.
     *
     * @param message the content message to display
     * @param owner   the parent window for this alert
     */
    public void showSuccess(String message, Window owner) {
        showAlert(Alert.AlertType.INFORMATION, "Success", message, globalAppIcon, owner);
    }

    // ==================== ERROR ALERTS ====================

    /**
     * Displays an error alert dialog using the global default icon.
     *
     * @param title   the title text for the alert dialog
     * @param message the content message to display
     */
    public void showError(String title, String message) {
        showAlert(Alert.AlertType.ERROR, title, message, globalAppIcon, null);
    }

    /**
     * Displays an error alert dialog with a custom icon.
     *
     * @param title   the title text for the alert dialog
     * @param message the content message to display
     * @param icon    the custom {@link Image} to use for this alert's icon.
     *                Overrides the global default. Pass {@code null} to use no icon.
     */
    public void showError(String title, String message, Image icon) {
        showAlert(Alert.AlertType.ERROR, title, message, icon, null);
    }

    /**
     * Displays an error alert dialog with a custom owner window.
     *
     * @param title   the title text for the alert dialog
     * @param message the content message to display
     * @param owner   the parent window for this alert
     */
    public void showError(String title, String message, Window owner) {
        showAlert(Alert.AlertType.ERROR, title, message, globalAppIcon, owner);
    }

    // ==================== CONFIRMATION DIALOGS ====================

    /**
     * Displays a confirmation dialog with "OK" and "Cancel" buttons and the global default icon.
     *
     * @param title   the title text for the confirmation dialog
     * @param message the content message to display
     * @return {@code true} if the user clicked OK, {@code false} otherwise
     */
    public boolean showConfirmation(String title, String message) {
        return showConfirmationInternal(title, message, globalAppIcon, null);
    }

    /**
     * Displays a confirmation dialog with "OK" and "Cancel" buttons and a custom icon.
     *
     * @param title   the title text for the confirmation dialog
     * @param message the content message to display
     * @param icon    the custom {@link Image} to use for this alert's icon.
     *                Overrides the global default. Pass {@code null} to use no icon.
     * @return {@code true} if the user clicked OK, {@code false} otherwise
     */
    public boolean showConfirmation(String title, String message, Image icon) {
        return showConfirmationInternal(title, message, icon, null);
    }

    /**
     * Displays a confirmation dialog with "OK" and "Cancel" buttons and custom owner window.
     *
     * @param title   the title text for the confirmation dialog
     * @param message the content message to display
     * @param owner   the parent window for this alert
     * @return {@code true} if the user clicked OK, {@code false} otherwise
     */
    public boolean showConfirmation(String title, String message, Window owner) {
        return showConfirmationInternal(title, message, globalAppIcon, owner);
    }

    /**
     * Displays a confirmation dialog asynchronously and returns a CompletableFuture.
     * Safe to call from any thread.
     *
     * @param title   the title text for the confirmation dialog
     * @param message the content message to display
     * @return CompletableFuture that completes with the user's choice
     */
    public CompletableFuture<Boolean> showConfirmationAsync(String title, String message) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();

        Runnable dialog = () -> {
            try {
                boolean result = showConfirmationInternal(title, message, globalAppIcon, null);
                future.complete(result);
            } catch (Exception e) {
                future.completeExceptionally(e);
            }
        };

        if (Platform.isFxApplicationThread()) {
            dialog.run();
        } else {
            Platform.runLater(dialog);
        }

        return future;
    }

    // ==================== PRIVATE IMPLEMENTATION ====================

    /**
     * Internal method to display a generic alert on the correct thread.
     */
    private void showAlert(Alert.AlertType type, String title, String message, Image icon, Window owner) {
        Runnable dialog = () -> {
            Alert alert = new Alert(type);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);

            if (owner != null) {
                alert.initOwner(owner);
            }

            applyIconToAlert(alert, icon);
            alert.showAndWait();
        };

        executeOnFxThread(dialog);
    }

    /**
     * Internal method to display a confirmation dialog.
     */
    private boolean showConfirmationInternal(String title, String message, Image icon, Window owner) {
        AtomicReference<Boolean> result = new AtomicReference<>(false);

        Runnable dialog = () -> {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION, message, ButtonType.OK, ButtonType.CANCEL);
            alert.setTitle(title);
            alert.setHeaderText(null);

            if (owner != null) {
                alert.initOwner(owner);
            }

            applyIconToAlert(alert, icon);

            Optional<ButtonType> buttonType = alert.showAndWait();
            result.set(buttonType.isPresent() && buttonType.get() == ButtonType.OK);
        };

        if (Platform.isFxApplicationThread()) {
            dialog.run();
        } else {
            // For non-FX threads, we need to block and wait
            final Object lock = new Object();
            Platform.runLater(() -> {
                dialog.run();
                synchronized (lock) {
                    lock.notifyAll();
                }
            });

            try {
                synchronized (lock) {
                    lock.wait();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }

        return result.get();
    }

    /**
     * Applies the specified icon to an alert's window.
     */
    private void applyIconToAlert(Alert alert, Image icon) {
        Image iconToUse = icon != null ? icon : globalAppIcon;
        if (iconToUse != null) {
            Stage alertStage = (Stage) alert.getDialogPane().getScene().getWindow();
            alertStage.getIcons().add(iconToUse);
        }
    }

    /**
     * Executes a runnable on the FX application thread.
     */
    private void executeOnFxThread(Runnable runnable) {
        if (Platform.isFxApplicationThread()) {
            runnable.run();
        } else {
            Platform.runLater(runnable);
        }
    }
}