package org.frost;

import javafx.scene.image.Image;

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

    // Getters...
    public String getTitle() { return title; }
    public boolean isResizable() { return resizable; }
    public boolean isModal() { return modal; }
    public boolean isWaitForClose() { return waitForClose; }
    public double getWidth() { return width; }
    public double getHeight() { return height; }
    public Image getIcon() { return icon; }
}