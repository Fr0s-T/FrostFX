package org.frost.Utilities;

/**
 * Configuration options for SceneManager reset operation.
 * Uses builder pattern for fluent configuration to allow chaining of configuration methods.
 *
 * <p>This class provides fine-grained control over what gets reset during a SceneManager
 * reset operation, allowing for different reset profiles based on application needs.</p>
 *
 * @see org.frost.internal.loaders.SceneManager
 */
public class ResetOptions {
    // Instance fields - each ResetOptions has its own state!
    private boolean clearPrimaryStage = true;
    private boolean clearSecondaryStages = true;
    private boolean clearCardRegistrations = true;
    private boolean clearAllCards = false;
    private boolean clearListeners = false;
    private boolean resetWindowMode = true;

    /**
     * Constructs a new ResetOptions instance with default settings.
     * Default configuration clears primary stage, secondary stages, and card registrations,
     * but preserves cards and listeners.
     */
    public ResetOptions() {
        // Default constructor
    }

    /**
     * Configures whether to clear the primary stage during reset.
     *
     * @param clear true to clear the primary stage, false to preserve it
     * @return this ResetOptions instance for method chaining
     */
    public ResetOptions clearPrimaryStage(boolean clear) {
        this.clearPrimaryStage = clear;
        return this;
    }

    /**
     * Configures whether to clear all secondary stages during reset.
     *
     * @param clear true to clear secondary stages, false to preserve them
     * @return this ResetOptions instance for method chaining
     */
    public ResetOptions clearSecondaryStages(boolean clear) {
        this.clearSecondaryStages = clear;
        return this;
    }

    /**
     * Configures whether to clear card registrations during reset.
     * This removes the mapping between card names and FXML paths.
     *
     * @param clear true to clear card registrations, false to preserve them
     * @return this ResetOptions instance for method chaining
     */
    public ResetOptions clearCardRegistrations(boolean clear) {
        this.clearCardRegistrations = clear;
        return this;
    }

    /**
     * Configures whether to clear all loaded cards from containers during reset.
     * This removes visual cards but preserves the card registration mappings.
     *
     * @param clear true to clear all cards, false to preserve them
     * @return this ResetOptions instance for method chaining
     */
    public ResetOptions clearAllCards(boolean clear) {
        this.clearAllCards = clear;
        return this;
    }

    /**
     * Configures whether to clear all registered listeners during reset.
     * This includes scene load listeners and other event handlers.
     *
     * @param clear true to clear listeners, false to preserve them
     * @return this ResetOptions instance for method chaining
     */
    public ResetOptions clearListeners(boolean clear) {
        this.clearListeners = clear;
        return this;
    }

    /**
     * Configures whether to reset the window mode to default during reset.
     * This typically resets the application window state and positioning.
     *
     * @param reset true to reset window mode, false to preserve current window state
     * @return this ResetOptions instance for method chaining
     */
    public ResetOptions resetWindowMode(boolean reset) {
        this.resetWindowMode = reset;
        return this;
    }

    /**
     * Returns whether the primary stage should be cleared.
     *
     * @return true if primary stage should be cleared, false otherwise
     */
    public boolean shouldClearPrimaryStage() {
        return clearPrimaryStage;
    }

    /**
     * Returns whether secondary stages should be cleared.
     *
     * @return true if secondary stages should be cleared, false otherwise
     */
    public boolean shouldClearSecondaryStages() {
        return clearSecondaryStages;
    }

    /**
     * Returns whether card registrations should be cleared.
     *
     * @return true if card registrations should be cleared, false otherwise
     */
    public boolean shouldClearCardRegistrations() {
        return clearCardRegistrations;
    }

    /**
     * Returns whether all loaded cards should be cleared from containers.
     *
     * @return true if all cards should be cleared, false otherwise
     */
    public boolean shouldClearAllCards() {
        return clearAllCards;
    }

    /**
     * Returns whether listeners should be cleared.
     *
     * @return true if listeners should be cleared, false otherwise
     */
    public boolean shouldClearListeners() {
        return clearListeners;
    }

    /**
     * Returns whether window mode should be reset to default.
     *
     * @return true if window mode should be reset, false otherwise
     */
    public boolean shouldResetWindowMode() {
        return resetWindowMode;
    }

    /**
     * Predefined reset profile: Soft reset.
     * Clears primary stage and card registrations, but preserves secondary stages,
     * loaded cards, and listeners. Resets window mode.
     *
     * <p>Use this for partial resets that maintain most application state while
     * cleaning up core components.</p>
     *
     * @return a ResetOptions instance configured for a soft reset
     */
    public static ResetOptions softReset() {
        return new ResetOptions()
                .clearPrimaryStage(true)
                .clearSecondaryStages(false)
                .clearCardRegistrations(true)
                .clearAllCards(false)
                .clearListeners(false)
                .resetWindowMode(true);
    }

    /**
     * Predefined reset profile: Hard reset.
     * Clears everything - primary stage, secondary stages, card registrations,
     * all loaded cards, listeners, and resets window mode.
     *
     * <p>Use this for complete application cleanup, such as during application
     * shutdown or when a completely fresh state is required.</p>
     *
     * @return a ResetOptions instance configured for a hard reset
     */
    public static ResetOptions hardReset() {
        return new ResetOptions()
                .clearPrimaryStage(true)
                .clearSecondaryStages(true)
                .clearCardRegistrations(true)
                .clearAllCards(true)
                .clearListeners(true)
                .resetWindowMode(true);
    }

    /**
     * Predefined reset profile: Stage-only reset.
     * Clears only the primary and secondary stages, preserving card registrations,
     * loaded cards, listeners, and window mode.
     *
     * <p>Use this when you want to reset the UI stages but keep all the application
     * logic and data intact.</p>
     *
     * @return a ResetOptions instance configured for a stage-only reset
     */
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