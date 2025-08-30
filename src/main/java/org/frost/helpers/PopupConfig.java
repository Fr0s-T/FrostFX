package org.frost.helpers;

import javafx.scene.image.Image;
import java.util.function.Consumer;

/**
 * Configuration object for popup windows.
 * <p>
 * Provides a fluent API for customizing properties of the popup,
 * such as title, modality, size, icon, and whether it blocks
 * until closed.
 * </p>
 * @author Frost
 */
public class PopupConfig {
    private String title = "Popup";
    private boolean resizable = false;
    private boolean modal = true;
    private boolean waitForClose = false;
    private double width = 400;
    private double height = 300;
    private Image icon;
    private Consumer<Object> controllerConfigurator;

    /**
     * Sets the popup title.
     * @param title the title text for the popup window
     * @return the PopupConfig instance
     */
    public PopupConfig setTitle(String title) {
        this.title = title;
        return this;
    }

    /**
     * Sets whether popup is resizable.
     * @param resizable true to allow resizing, false to disable
     * @return the PopupConfig instance
     */
    public PopupConfig setResizable(boolean resizable) {
        this.resizable = resizable;
        return this;
    }

    /**
     * Sets whether popup is modal.
     * @param modal true for modal (blocks parent window), false for modeless
     * @return the PopupConfig instance
     */
    public PopupConfig setModal(boolean modal) {
        this.modal = modal;
        return this;
    }

    /**
     * Sets whether to wait for popup to close.
     * @param waitForClose true to wait for close, false to return immediately
     * @return the PopupConfig instance
     */
    public PopupConfig setWaitForClose(boolean waitForClose) {
        this.waitForClose = waitForClose;
        return this;
    }

    /**
     * Sets the popup size.
     * @param width the width of the popup window
     * @param height the height of the popup window
     * @return the PopupConfig instance
     */
    public PopupConfig setSize(double width, double height) {
        this.width = width;
        this.height = height;
        return this;
    }

    /**
     * Sets the popup icon.
     * @param icon the Image to use as window icon
     * @return the PopupConfig instance
     */
    public PopupConfig setIcon(Image icon) {
        this.icon = icon;
        return this;
    }

    /**
     * Returns the controller configurator.
     */
    public Consumer<Object> getControllerConfigurator() {
        return controllerConfigurator;
    }

    /**
     * Returns the popup title.
     */
    public String getTitle() { return title; }

    /**
     * Returns whether popup is resizable.
     */
    public boolean isResizable() { return resizable; }

    /**
     * Returns whether popup is modal.
     */
    public boolean isModal() { return modal; }

    /**
     * Returns whether to wait for popup close.
     */
    public boolean isWaitForClose() { return waitForClose; }

    /**
     * Returns the popup width.
     */
    public double getWidth() { return width; }

    /**
     * Returns the popup height.
     */
    public double getHeight() { return height; }

    /**
     * Returns the popup icon.
     */
    public Image getIcon() { return icon; }

    /**
     * Sets the controller configurator function.
     */
    public PopupConfig setControllerConfigurator(Consumer<Object> configurator) {
        this.controllerConfigurator = configurator;
        return this;
    }
}