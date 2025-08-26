package org.frost.Utilities;

/**
 * Configuration options for SceneManager reset operation
 * Uses builder pattern for fluent configuration
 */
public class ResetOptions {
    // Instance fields - each ResetOptions has its own state!
    private boolean clearPrimaryStage = true;
    private boolean clearSecondaryStages = true;
    private boolean clearCardRegistrations = true;
    private boolean clearAllCards = false;
    private boolean clearListeners = false;
    private boolean resetWindowMode = true;

    // Builder-style methods (return THIS instance for chaining)
    public ResetOptions clearPrimaryStage(boolean clear) {
        this.clearPrimaryStage = clear;
        return this;
    }

    public ResetOptions clearSecondaryStages(boolean clear) {
        this.clearSecondaryStages = clear;
        return this;
    }

    public ResetOptions clearCardRegistrations(boolean clear) {
        this.clearCardRegistrations = clear;
        return this;
    }

    public ResetOptions clearAllCards(boolean clear) {
        this.clearAllCards = clear;
        return this;
    }

    public ResetOptions clearListeners(boolean clear) {
        this.clearListeners = clear;
        return this;
    }

    public ResetOptions resetWindowMode(boolean reset) {
        this.resetWindowMode = reset;
        return this;
    }

    // Getter methods (required for the reset() method to work)
    public boolean shouldClearPrimaryStage() {
        return clearPrimaryStage;
    }

    public boolean shouldClearSecondaryStages() {
        return clearSecondaryStages;
    }

    public boolean shouldClearCardRegistrations() {
        return clearCardRegistrations;
    }

    public boolean shouldClearAllCards() {
        return clearAllCards;
    }

    public boolean shouldClearListeners() {
        return clearListeners;
    }

    public boolean shouldResetWindowMode() {
        return resetWindowMode;
    }

    /** Predefined common reset profiles */
    public static ResetOptions softReset() {
        return new ResetOptions()
                .clearPrimaryStage(true)
                .clearSecondaryStages(false)
                .clearCardRegistrations(true)
                .clearAllCards(false)
                .clearListeners(false)
                .resetWindowMode(true);
    }

    public static ResetOptions hardReset() {
        return new ResetOptions()
                .clearPrimaryStage(true)
                .clearSecondaryStages(true)
                .clearCardRegistrations(true)
                .clearAllCards(true)
                .clearListeners(true)
                .resetWindowMode(true);
    }

    public static ResetOptions stageOnlyReset() {
        return new ResetOptions()
                .clearPrimaryStage(true)
                .clearSecondaryStages(true)
                .clearCardRegistrations(false) // Keep card registrations
                .clearAllCards(false)
                .clearListeners(false)
                .resetWindowMode(false);
    }
}