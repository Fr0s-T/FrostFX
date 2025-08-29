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

    // Fluent setters
    public PopupConfig setTitle(String title) {
        this.title = title;
        return this;
    }

    public PopupConfig setResizable(boolean resizable) {
        this.resizable = resizable;
        return this;
    }

    public PopupConfig setModal(boolean modal) {
        this.modal = modal;
        return this;
    }

    public PopupConfig setWaitForClose(boolean waitForClose) {
        this.waitForClose = waitForClose;
        return this;
    }

    public PopupConfig setSize(double width, double height) {
        this.width = width;
        this.height = height;
        return this;
    }

    public PopupConfig setIcon(Image icon) {
        this.icon = icon;
        return this;
    }

    /**
     * Gets the controller configurator function
     *
     * @return the controller configurator, or null if not set
     * @since V1.0.0
     */
    public Consumer<Object> getControllerConfigurator() {
        return controllerConfigurator;
    }

    // Getters...
    public String getTitle() { return title; }
    public boolean isResizable() { return resizable; }
    public boolean isModal() { return modal; }
    public boolean isWaitForClose() { return waitForClose; }
    public double getWidth() { return width; }
    public double getHeight() { return height; }
    public Image getIcon() { return icon; }

    /**
     * Sets a configurator function that will be called with the controller instance
     * after the popup is loaded but before it is shown.
     *
     * @param configurator a Consumer that accepts the controller for configuration
     * @return this PopupConfig instance for fluent chaining
     */
    public PopupConfig setControllerConfigurator(Consumer<Object> configurator) {
        this.controllerConfigurator = configurator;
        return this;
    }
}