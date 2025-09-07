package org.frost.internal.loaders;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import org.frost.Utilities.CardControllersResult;
import org.frost.services.CardLoadingService;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

import static org.frost.internal.loaders.SceneManager.runOnFxThread;

/**
 * The main entry point for loading and managing dynamic, reusable UI components ("cards") in the FrostFX framework.
 * <p>
 * This class serves as a facade, providing a clean, type-safe public API for common operations while delegating
 * the complex loading and layout logic to an internal {@link CardLoadingService}.
 * </p>
 * <p>
 * <b>Key Responsibilities:</b>
 * <ul>
 *   <li>Registering cards, panels, and containers with specific stages.</li>
 *   <li>Loading individual cards into panels.</li>
 *   <li>Populating containers with multiple data-bound cards, including spacing.</li>
 *   <li>Managing the lifecycle and state of UI components across the application.</li>
 * </ul>
 * All operations are thread-safe.
 * </p>
 *
 * @see CardLoadingService
 */
public class CardLoader {

    // State management
    private String currentPanelView = "";
    private final Map<String, String> cardPathsMap = new HashMap<>();
    private final Map<Stage, Map<String, AnchorPane>> stagePanels = new HashMap<>();
    private final Map<Stage, Map<String, Pane>> stageContainers = new HashMap<>();
    private Stage primaryStage;

    // The real worker
    private final CardLoadingService loadingService = new CardLoadingService();

    /**
     * Constructs a new CardLoader instance.
     * The loader must be initialized with a primary stage via {@code setPrimaryStage()}
     * before most operations can be performed.
     */
    public CardLoader() { }

    /**
     * Sets the primary stage for this card loader.
     * This method is typically called automatically by the SceneManager during initialization.
     *
     * @param stage the primary JavaFX stage to associate with this loader
     */
    void setPrimaryStage(Stage stage) { this.primaryStage = stage; }

    // ==================== GETTERS & QUERY METHODS ====================//

    /**
     * Returns the name of the currently active panel view.
     *
     * @return the current panel view name, or empty string if no view is active
     */
    public String getCurrentPanelView() { return currentPanelView; }

    /**
     * Returns a set of all panel names registered for the primary stage.
     *
     * @return a set of available panel names
     * @throws java.lang.IllegalStateException if the primary stage has not been set
     */
    public Set<String> getAvailablePanels() {
        ensurePrimaryStageSet();
        return getAvailablePanels(primaryStage);
    }

    /**
     * Returns a set of all panel names registered for the specified stage.
     *
     * @param stage the stage to query for panels
     * @return a set of available panel names for the given stage
     * @throws java.lang.IllegalArgumentException if the stage parameter is null
     */
    public Set<String> getAvailablePanels(Stage stage) {
        if (stage == null) throw new IllegalArgumentException("Stage cannot be null");
        return stagePanels.getOrDefault(stage, Collections.emptyMap()).keySet();
    }

    /**
     * Returns an unmodifiable view of the card registration map.
     * The map contains card names as keys and their corresponding FXML paths as values.
     *
     * @return an unmodifiable map of registered cards
     */
    public Map<String, String> getCardPathsMap() {
        return Collections.unmodifiableMap(cardPathsMap);
    }

    /**
     * Returns a set of all container names registered for the specified stage.
     *
     * @param stage the stage to query for containers
     * @return a set of available container names for the given stage
     * @throws java.lang.IllegalArgumentException if the stage parameter is null
     */
    public Set<String> getAvailableContainers(Stage stage) {
        if (stage == null) throw new IllegalArgumentException("Stage cannot be null");
        return stageContainers.getOrDefault(stage, Collections.emptyMap()).keySet();
    }

    // ==================== REGISTRY METHODS ====================//

    /**
     * Registers a card definition with the loader.
     *
     * @param name the unique name to identify the card
     * @param fxmlPath the classpath path to the FXML file defining the card's UI
     * @throws java.lang.IllegalArgumentException if name or fxmlPath are null or blank
     * @throws java.lang.IllegalStateException if a card with the same name is already registered
     */
    public void registerCard(String name, String fxmlPath) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("Card name must not be null or blank");
        if (fxmlPath == null || fxmlPath.isBlank()) throw new IllegalArgumentException("FXML path must not be null or blank");
        if (cardPathsMap.containsKey(name)) throw new IllegalStateException("Card '" + name + "' is already registered");
        cardPathsMap.put(name, fxmlPath);
    }

    /**
     * Unregisters a card definition from the loader.
     *
     * @param name the name of the card to unregister
     * @return true if the card was found and removed, false otherwise
     */
    public boolean unregisterCard(String name) {
        return cardPathsMap.remove(name) != null;
    }

    /**
     * Registers a dynamic panel with the primary stage.
     *
     * @param name the unique name to identify the panel
     * @param panel the AnchorPane instance to register
     * @throws java.lang.IllegalStateException if the primary stage has not been set
     */
    public void registerDynamicPanel(String name, AnchorPane panel) {
        ensurePrimaryStageSet();
        registerDynamicPanel(name, panel, primaryStage);
    }

    /**
     * Registers a container with the primary stage.
     *
     * @param name the unique name to identify the container
     * @param container the Pane instance to register
     * @throws java.lang.IllegalStateException if the primary stage has not been set
     */
    public void registerContainer(String name, Pane container) {
        ensurePrimaryStageSet();
        registerContainer(name, container, primaryStage);
    }

    /**
     * Registers a dynamic panel with a specific stage.
     *
     * @param name the unique name to identify the panel
     * @param panel the AnchorPane instance to register
     * @param stage the stage to associate with the panel
     * @throws java.lang.IllegalArgumentException if any parameter is null or name is blank
     */
    public void registerDynamicPanel(String name, AnchorPane panel, Stage stage) {
        validateRegistrationParams(name, panel, stage);
        stagePanels.computeIfAbsent(stage, s -> new HashMap<>()).put(name, panel);
    }

    /**
     * Registers a container with a specific stage.
     *
     * @param name the unique name to identify the container
     * @param container the Pane instance to register
     * @param stage the stage to associate with the container
     * @throws java.lang.IllegalArgumentException if any parameter is null or name is blank
     */
    public void registerContainer(String name, Pane container, Stage stage) {
        validateRegistrationParams(name, container, stage);
        stageContainers.computeIfAbsent(stage, s -> new HashMap<>()).put(name, container);
    }

    /**
     * Unregisters a dynamic panel from a specific stage.
     *
     * @param name the name of the panel to unregister
     * @param stage the stage from which to unregister the panel
     * @return true if the panel was found and removed, false otherwise
     */
    public boolean unregisterDynamicPanel(String name, Stage stage) {
        Map<String, AnchorPane> panels = stagePanels.get(stage);
        return panels != null && panels.remove(name) != null;
    }

    /**
     * Unregisters a container from a specific stage.
     *
     * @param name the name of the container to unregister
     * @param stage the stage from which to unregister the container
     * @return true if the container was found and removed, false otherwise
     */
    public boolean unregisterContainer(String name, Stage stage) {
        Map<String, Pane> containers = stageContainers.get(stage);
        return containers != null && containers.remove(name) != null;
    }

    // ==================== SINGLE CARD LOADING METHODS ====================//

    /**
     * Loads a single card into a specified panel within the primary stage.
     *
     * @param <T> the type of the card's controller
     * @param cardName the registered name of the card to load
     * @param anchorPaneName the name of the panel where the card should be placed
     * @return the controller instance of the loaded card
     * @throws java.lang.IllegalStateException if the primary stage has not been set
     * @throws java.lang.IllegalArgumentException if the card is not registered
     */
    public <T> T loadCard(String cardName, String anchorPaneName) {
        ensurePrimaryStageSet();
        return loadCard(cardName, anchorPaneName, primaryStage);
    }

    /**
     * Loads a single card into a specified panel within a specific stage.
     *
     * @param <T> the type of the card's controller
     * @param cardName the registered name of the card to load
     * @param anchorPaneName the name of the panel where the card should be placed
     * @param stage the stage containing the target panel
     * @return the controller instance of the loaded card
     * @throws java.lang.IllegalArgumentException if the card is not registered
     */
    public <T> T loadCard(String cardName, String anchorPaneName, Stage stage) {
        String cardFxmlPath = getCardPath(cardName);
        AnchorPane targetPanel = getPanelFromRegistry(anchorPaneName, stage);
        return loadingService.loadCard(targetPanel, cardFxmlPath, cardName, stage);
    }



    // ==================== MULTIPLE CARDS LOADING METHODS ====================//

    /**
     * Loads multiple cards into a container using the primary stage with no margins.
     * This is a convenience method for simple loading without data injection.
     *
     * @param <T> the type of data items
     * @param containerName the name of the container to populate with cards
     * @param items the list of data items to display (one card per item)
     * @param cardName the registered name of the card to load
     * @throws java.lang.IllegalStateException if the primary stage has not been set
     * @throws java.lang.IllegalArgumentException if the card is not registered
     */
    public <T> void loadCards(String containerName, List<T> items, String cardName) {
        ensurePrimaryStageSet();
        loadCards(containerName, items, cardName, null, primaryStage, 0, 0);
    }

    /**
     * Loads multiple cards into a container using the primary stage with no margins.
     * Includes data injection capability through the provided BiConsumer.
     *<p>
     *<b>Important: Lambda Type Inference</b>
     *<p>
     *Due to Java type erasure, the generic type {@code <C>} (the controller) cannot be
     * automatically inferred in lambdas. You <i>must</i> specify the types explicitly:
     * </p>
     *<pre>{@code
     * (MyController c, MyData d) -> {c.setData(d)}
     * }</pre>
     * <p>
     *Using {@code (c, d) -> ...} will result in a compilation error.
     *</p>
     *
     * @param <T> the type of data items
     * @param containerName the name of the container to populate with cards
     * @param items the list of data items to display (one card per item)
     * @param cardName the registered name of the card to load
     * @param dataSetter a function that binds data items to their card controllers
     * @throws java.lang.IllegalStateException if the primary stage has not been set
     * @throws java.lang.IllegalArgumentException if the card is not registered
     */
    public <T> void loadCards(String containerName, List<T> items, String cardName,
                              BiConsumer<Object, T> dataSetter) {
        ensurePrimaryStageSet();
        loadCards(containerName, items, cardName, dataSetter, primaryStage, 0, 0);
    }

    /**
     * Loads multiple cards into a container using the primary stage with uniform margins.
     * Includes optional data injection.
     *<p>
     *<b>Important: Lambda Type Inference</b>
     *<p>
     *Due to Java type erasure, the generic type {@code <C>} (the controller) cannot be
     * automatically inferred in lambdas. You <i>must</i> specify the types explicitly:
     * </p>
     *<pre>{@code
     * (MyController c, MyData d) -> {c.setData(d)}
     * }</pre>
     * <p>
     *Using {@code (c, d) -> ...} will result in a compilation error.
     *</p>
     *
     * @CSS .frostfx-spacer The CSS class applied to spacing elements between cards.
     * Customize margins using: .frostfx-spacer { -fx-background-color: #e0e0e0; }
     * Target horizontal spacers: .frostfx-spacer:horizontal { /*styles }
     * Target vertical spacers: .frostfx-spacer:vertical { /* styles }
     * @param <T> the type of data items
     * @param containerName the name of the container to populate with cards
     * @param items the list of data items to display (one card per item)
     * @param cardName the registered name of the card to load
     * @param dataSetter a function that binds data items to their card controllers
     * @param margin the uniform margin (applied to both horizontal and vertical spacing)
     * @throws java.lang.IllegalStateException if the primary stage has not been set
     * @throws java.lang.IllegalArgumentException if the card is not registered
     */
    public <T> void loadCards(String containerName, List<T> items, String cardName,
                              BiConsumer<Object, T> dataSetter, double margin) {
        ensurePrimaryStageSet();
        loadCards(containerName, items, cardName, dataSetter, primaryStage, margin, margin);
    }

    /**
     * Loads multiple cards into a container using the primary stage with dual margins.
     * Includes optional data injection.
     *<p>
     *<b>Important: Lambda Type Inference</b>
     *<p>
     *Due to Java type erasure, the generic type {@code <C>} (the controller) cannot be
     * automatically inferred in lambdas. You <i>must</i> specify the types explicitly:
     * </p>
     *<pre>{@code
     * (MyController c, MyData d) -> {c.setData(d)}
     * }</pre>
     * <p>
     *Using {@code (c, d) -> ...} will result in a compilation error.
     *</p>
     *
     * @CSS .frostfx-spacer The CSS class applied to spacing elements between cards.
     * Customize margins using: .frostfx-spacer { -fx-background-color: #e0e0e0; }
     * Target horizontal spacers: .frostfx-spacer:horizontal { /*styles }
     * Target vertical spacers: .frostfx-spacer:vertical { /* styles }
     * @param <T> the type of data items
     * @param containerName the name of the container to populate with cards
     * @param items the list of data items to display (one card per item)
     * @param cardName the registered name of the card to load
     * @param dataSetter a function that binds data items to their card controllers
     * @param horizontalMargin the horizontal spacing between cards
     * @param verticalMargin the vertical spacing between cards
     * @throws java.lang.IllegalStateException if the primary stage has not been set
     * @throws java.lang.IllegalArgumentException if the card is not registered
     */
    public <T> void loadCards(String containerName, List<T> items, String cardName,
                              BiConsumer<Object, T> dataSetter,
                              double horizontalMargin, double verticalMargin) {
        ensurePrimaryStageSet();
        loadCards(containerName, items, cardName, dataSetter, primaryStage,
                horizontalMargin, verticalMargin);
    }

    /**
     * Loads multiple cards into a container using a specific stage.
     * Supports optional data injection and margins.
     *<p>
     *<b>Important: Lambda Type Inference</b>
     *<p>
     *Due to Java type erasure, the generic type {@code <C>} (the controller) cannot be
     * automatically inferred in lambdas. You <i>must</i> specify the types explicitly:
     * </p>
     *<pre>{@code
     * (MyController c, MyData d) -> {c.setData(d)}
     * }</pre>
     * <p>
     *Using {@code (c, d) -> ...} will result in a compilation error.
     *</p>
     *
     * @CSS .frostfx-spacer The CSS class applied to spacing elements between cards.
     * Customize margins using: .frostfx-spacer { -fx-background-color: #e0e0e0; }
     * Target horizontal spacers: .frostfx-spacer:horizontal { /*styles }
     * Target vertical spacers: .frostfx-spacer:vertical { /* styles }
     *
     * @param <T> the type of data items
     * @param containerName the name of the container to populate with cards
     * @param items the list of data items to display (one card per item)
     * @param cardName the registered name of the card to load
     * @param dataSetter a function that binds data items to their card controllers
     * @param stage the stage containing the target container
     * @param horizontalMargin the horizontal spacing between cards
     * @param verticalMargin the vertical spacing between cards
     * @throws java.lang.IllegalArgumentException if the card is not registered
     */
    public <T> void loadCards(String containerName, List<T> items, String cardName,
                              BiConsumer<Object, T> dataSetter, Stage stage,
                              double horizontalMargin, double verticalMargin) {
        String cardFxmlPath = getCardPath(cardName);
        Pane container = getContainerFromRegistry(containerName, stage);
        loadingService.loadCards(container, items, cardFxmlPath, dataSetter,
                horizontalMargin, verticalMargin);
    }


// ==================== MULTIPLE CARDS LOADING WITH CONTROLLER RETURN ====================//

    /**
     * Loads multiple cards and returns their controllers using primary stage with no margins.
     *
     * @param <T> the type of data items
     * @param <C> the type of the card controllers
     * @param containerName the name of the container to populate with cards
     * @param items the list of data items to display (one card per item)
     * @param cardName the registered name of the card to load
     * @return a list of card controller instances
     * @throws java.lang.IllegalStateException if the primary stage has not been set
     * @throws java.lang.IllegalArgumentException if the card is not registered
     */
    public <T, C> List<C> loadCardsWithControllers(String containerName, List<T> items, String cardName) {
        ensurePrimaryStageSet();
        return loadCardsWithControllers(containerName, items, cardName, null,
                primaryStage, 0, 0);
    }

    /**
     * Loads multiple cards and returns their controllers using primary stage with no margins.
     *
     * @param <T> the type of data items
     * @param <C> the type of the card controllers
     * @param containerName the name of the container to populate with cards
     * @param items the list of data items to display (one card per item)
     * @param cardName the registered name of the card to load
     * @param dataSetter a function that binds data items to their card controllers
     * @return a list of card controller instances
     * @throws java.lang.IllegalStateException if the primary stage has not been set
     * @throws java.lang.IllegalArgumentException if the card is not registered
     */
    public <T, C> List<C> loadCardsWithControllers(String containerName, List<T> items, String cardName,
                                                   BiConsumer<C, T> dataSetter) {
        ensurePrimaryStageSet();
        return loadCardsWithControllers(containerName, items, cardName, dataSetter,
                primaryStage, 0, 0);
    }

    /**
     * Loads multiple cards and returns their controllers using primary stage with uniform margins.
     *
     * @param <T> the type of data items
     * @param <C> the type of the card controllers
     * @param containerName the name of the container to populate with cards
     * @param items the list of data items to display (one card per item)
     * @param cardName the registered name of the card to load
     * @param dataSetter a function that binds data items to their card controllers
     * @param margin the uniform margin (applied to both horizontal and vertical spacing)
     * @return a list of card controller instances
     * @throws java.lang.IllegalStateException if the primary stage has not been set
     * @throws java.lang.IllegalArgumentException if the card is not registered
     * @CSS .frostfx-spacer The CSS class applied to spacing elements between cards.
     *       Customize margins using: .frostfx-spacer { -fx-background-color: #e0e0e0; }
     *      Target horizontal spacers: .frostfx-spacer:horizontal { /*styles }
     *      Target vertical spacers: .frostfx-spacer:vertical { /* styles  }
     */
    public <T, C> List<C> loadCardsWithControllers(String containerName, List<T> items, String cardName,
                                                   BiConsumer<C, T> dataSetter, double margin) {
        ensurePrimaryStageSet();
        return loadCardsWithControllers(containerName, items, cardName, dataSetter,
                primaryStage, margin, margin);
    }

    /**
     * Loads multiple cards and returns their controllers using primary stage with dual margins.
     *
     * @param <T> the type of data items
     * @param <C> the type of the card controllers
     * @param containerName the name of the container to populate with cards
     * @param items the list of data items to display (one card per item)
     * @param cardName the registered name of the card to load
     * @param dataSetter a function that binds data items to their card controllers
     * @param horizontalMargin the horizontal spacing between cards
     * @param verticalMargin the vertical spacing between cards
     * @return a list of card controller instances
     * @throws java.lang.IllegalStateException if the primary stage has not been set
     * @throws java.lang.IllegalArgumentException if the card is not registered
     * @CSS .frostfx-spacer The CSS class applied to spacing elements between cards.
     *       Customize margins using: .frostfx-spacer { -fx-background-color: #e0e0e0; }
     *      Target horizontal spacers: .frostfx-spacer:horizontal { /*styles }
     *      Target vertical spacers: .frostfx-spacer:vertical { /* styles  }
     */
    public <T, C> List<C> loadCardsWithControllers(String containerName, List<T> items, String cardName,
                                                   BiConsumer<C, T> dataSetter,
                                                   double horizontalMargin, double verticalMargin) {
        ensurePrimaryStageSet();
        return loadCardsWithControllers(containerName, items, cardName, dataSetter,
                primaryStage, horizontalMargin, verticalMargin);
    }

    /**
     * Loads multiple cards and returns their controllers using a specific stage with no margins.
     *
     * @param <T> the type of data items
     * @param <C> the type of the card controllers
     * @param containerName the name of the container to populate with cards
     * @param items the list of data items to display (one card per item)
     * @param cardName the registered name of the card to load
     * @param dataSetter a function that binds data items to their card controllers
     * @param stage the stage containing the target container
     * @return a list of card controller instances
     * @throws java.lang.IllegalArgumentException if the card is not registered
     * @CSS .frostfx-spacer The CSS class applied to spacing elements between cards.
     *       Customize margins using: .frostfx-spacer { -fx-background-color: #e0e0e0; }
     *      Target horizontal spacers: .frostfx-spacer:horizontal { /*styles }
     *      Target vertical spacers: .frostfx-spacer:vertical { /* styles  }
     */
    public <T, C> List<C> loadCardsWithControllers(String containerName, List<T> items, String cardName,
                                                   BiConsumer<C, T> dataSetter, Stage stage) {
        return loadCardsWithControllers(containerName, items, cardName, dataSetter,
                stage, 0, 0);
    }

    /**
     * Loads multiple cards and returns their controllers using a specific stage with uniform margins.
     *
     * @param <T> the type of data items
     * @param <C> the type of the card controllers
     * @param containerName the name of the container to populate with cards
     * @param items the list of data items to display (one card per item)
     * @param cardName the registered name of the card to load
     * @param dataSetter a function that binds data items to their card controllers
     * @param stage the stage containing the target container
     * @param margin the uniform margin (applied to both horizontal and vertical spacing)
     * @return a list of card controller instances
     * @throws java.lang.IllegalArgumentException if the card is not registered
     * @CSS .frostfx-spacer The CSS class applied to spacing elements between cards.
     *       Customize margins using: .frostfx-spacer { -fx-background-color: #e0e0e0; }
     *      Target horizontal spacers: .frostfx-spacer:horizontal { /*styles }
     *      Target vertical spacers: .frostfx-spacer:vertical { /* styles  }
     */
    public <T, C> List<C> loadCardsWithControllers(String containerName, List<T> items, String cardName,
                                                   BiConsumer<C, T> dataSetter, Stage stage, double margin) {
        return loadCardsWithControllers(containerName, items, cardName, dataSetter,
                stage, margin, margin);
    }

    /**
     * Master method for loading multiple cards and returning their controllers.
     * All other loadCardsWithControllers methods delegate to this implementation.
     *
     * @param <T> the type of data items
     * @param <C> the type of the card controllers
     * @param containerName the name of the container to populate with cards
     * @param items the list of data items to display (one card per item)
     * @param cardName the registered name of the card to load
     * @param dataSetter a function that binds data items to their card controllers
     * @param stage the stage containing the target container
     * @param horizontalMargin the horizontal spacing between cards
     * @param verticalMargin the vertical spacing between cards
     * @return a list of card controller instances
     * @throws java.lang.IllegalArgumentException if the card is not registered
     * @CSS .frostfx-spacer The CSS class applied to spacing elements between cards.
     *       Customize margins using: .frostfx-spacer { -fx-background-color: #e0e0e0; }
     *      Target horizontal spacers: .frostfx-spacer:horizontal { /*styles }
     *      Target vertical spacers: .frostfx-spacer:vertical { /* styles  }
     */
    public <T, C> List<C> loadCardsWithControllers(String containerName, List<T> items, String cardName,
                                                   BiConsumer<C, T> dataSetter, Stage stage,
                                                   double horizontalMargin, double verticalMargin) {
        String cardFxmlPath = getCardPath(cardName);
        Pane container = getContainerFromRegistry(containerName, stage);
        return loadingService.loadCardsWithControllers(container, items, cardFxmlPath, dataSetter,
                horizontalMargin, verticalMargin);
    }

        // ==================== SINGLE CARD ADDERS ====================//

    /**
     * <b>WARNING: State Synchronization</b>
     * <p>
     * This method adds a card visually.
     * </p>
     * <p>
     * <b>You MUST also add the {@code item} to your own data list.</b> Any subsequent call to
     * {@link #loadCards} or {@link #loadCardsWithControllers} will <i>completely wipe</i> the container
     * and rebuild it <i>only from the items in the list you provide to those methods</i>. The controller
     * added by this method will be lost if its data item is not in that list.
     * </p>
     * <p>
     * This method is for advanced, granular UI updates. You are responsible for synchronizing
     * the state of your data model, the UI.
     * </p>
     *
     * @param <T>           the type of data items
     * @param <C>           the type of card controllers
     * @param containerName the name of the target container
     * @param item          the data item for the new card. <b>You must also add this to your data model.</b>
     * @param cardName      the name of the card used in the registry
     * @param dataSetter    callback for injecting data into the card controller
     * @param stage         the stage containing the container
     * @throws java.lang.IllegalArgumentException if the container or card is not registered
     */
    public <T, C> void addCardToContainer(String containerName, T item, String cardName,
                                          BiConsumer<C, T> dataSetter, Stage stage) {
        String cardFxmlPath = getCardPath(cardName);
        Pane container = getContainerFromRegistry(containerName, stage);
        loadingService.addCardToContainer(container, item, cardFxmlPath, dataSetter);
    }



    /**
     * <b>WARNING: State Synchronization</b>
     * <p>
     * This method adds a card visually.
     * </p>
     * <p>
     * <b>You MUST also add the {@code item} to your own data list.</b> Any subsequent call to
     * {@link #loadCards} or {@link #loadCardsWithControllers} will <i>completely wipe</i> the container
     * and rebuild it <i>only from the items in the list you provide to those methods</i>. The controller
     * added by this method will be lost if its data item is not in that list.
     * </p>
     * <p>
     * This method is for advanced, granular UI updates. You are responsible for synchronizing
     * the state of your data model, the UI, and this loader's returned controller.
     * </p>
     *
     * @param <T>           the type of data items
     * @param <C>           the type of card controllers
     * @param containerName the name of the target container
     * @param item          the data item for the new card. <b>You must also add this to your data model.</b>
     * @param cardName      the name of the card used in the registry
     * @param dataSetter    callback for injecting data into the card controller
     * @param stage         the stage containing the container
     * @throws java.lang.IllegalArgumentException if the container or card is not registered
     */
    public <T, C> C addCardToContainerWithController(String containerName, T item, String cardName,
                                                     BiConsumer<C, T> dataSetter, Stage stage) {
        String cardFxmlPath = getCardPath(cardName);
        Pane container = getContainerFromRegistry(containerName, stage);
        return loadingService.addCardToContainer(container, item, cardFxmlPath, dataSetter);
    }

    //====================create a card controller in bulk and re-attach said controller to the scene================//

    /**
     * <h1>ADVANCED USAGE ONLY!</h1>
     *<p>
     * Asynchronously creates card controllers and their corresponding UI nodes,
     * but does not attach them to the scene graph. This allows for preloading
     * components off the main UI thread for later atomic reattachment.
     *</p>
     * <p>WARNING: The returned {@link CardControllersResult} contains live JavaFX
     * controllers and nodes. Incorrect lifecycle handling may cause memory leaks,
     * UI corruption, or application instability.</p>
     *
     * <h2>Usage Pattern:</h2>
     * <pre>{@code
     * cardLoader.createCardControllersAsync("stock-card", stocks, (controller, stock) -> {
     *     controller.setSymbol(stock.getSymbol());
     *     controller.setPrice(stock.getPrice());
     * }).thenCompose(controllersResult -> {
     *     return cardLoader.attachCardControllersAsync("main-container",
     *         stocks, controllersResult, null, primaryStage, 10, 10);
     * }).exceptionally(ex -> {
     *     logger.error("Atomic composition failed", ex);
     *     return null;
     * });
     * }</pre>
     *
     * <h2>Architectural Constraints:</h2>
     * <ul>
     *   <li><b>Thread Safety:</b> Can be called from any thread, but all FX work executes on the JavaFX Application Thread.</li>
     *   <li><b>Lifecycle Management:</b> Callers must maintain strong references to the controllers until disposed.</li>
     *   <li><b>Memory Ownership:</b> Controllers/nodes must be explicitly cleared if no longer used.</li>
     * </ul>
     *
     * <h2>Common Failure Modes:</h2>q
     * <ul>
     *   <li><b>Memory Leaks:</b> Forgetting to clear unused controllers/nodes.</li>
     *   <li><b>UI Corruption:</b> Binding controllers to mismatched data types or using wrong card name.</li>
     *   <li><b>Thread Violations:</b> Calling methods directly on FX components off the UI thread.</li>
     * </ul>
     *
     * @param <T> the type of data items
     * @param <C> the type of card controllers
     * @param cardName registered card name (must exist in registry)
     * @param items data items to bind
     * @param dataSetter callback for initial binding
     * @return future containing ordered {@link CardControllersResult}
     * @throws IllegalStateException if invoked outside FX thread
     * @throws IllegalArgumentException if card name is not registered
     */
    public <T, C> CompletableFuture<CardControllersResult<T, C>> createCardControllersAsync(
            String cardName, List<T> items, BiConsumer<C, T> dataSetter) {

        CompletableFuture<CardControllersResult<T, C>> future = new CompletableFuture<>();

        runOnFxThread(() -> {
            try {
                CardControllersResult<T, C> result = loadingService.createCardControllers(items,getCardPath(cardName),
                        dataSetter);
                future.complete(result);
            } catch (Exception e) {
                future.completeExceptionally(e);
            }
        });

        return future;
    }

    /**
     * <h1>ADVANCED USAGE ONLY!</h1>
     *<p>
     * Atomically attaches pre-created controllers and nodes to a registered container.
     * Intended for instant composition of large UI sets (hundreds of nodes) without blocking.
     *</p>
     * <p>WARNING: The supplied {@link CardControllersResult} must originate from
     * {@link #createCardControllersAsync}. Size mismatches or foreign controllers
     * will cause catastrophic failure.</p>
     *
     * <h2>Usage Pattern:</h2>
     * <pre>{@code
     * cardLoader.attachCardControllersAsync("main-container",
     *     stocks, controllersResult, null, primaryStage, 10, 10)
     *   .thenRun(() -> logger.info("Controllers attached"))
     *   .exceptionally(ex -> {
     *       logger.error("Failed to attach controllers", ex);
     *       return null;
     *   });
     * }</pre>
     *
     * <h2>Architectural Constraints:</h2>
     * <ul>
     *   <li><b>Atomicity Guarantee:</b> All-or-nothing. Partial success is never committed.</li>
     *   <li><b>Thread Safety:</b> Executes only on JavaFX Application Thread.</li>
     *   <li><b>Consistency:</b> Items, controllers, and nodes must be equal-sized and ordered.</li>
     * </ul>
     *
     *<h2>Common Failure Modes:</h2>
     * <ul>
     *   <li><b>UI Corruption:</b> Items size does not match controllers size.</li>
     *   <li><b>Thread Starvation:</b> Long-running callbacks inside dataSetter block the FX thread.</li>
     *   <li><b>State Desync:</b> Mutating items after attachment starts.</li>
     * </ul>
     *
     * @param <T> type of data items
     * @param <C> type of card controllers
     * @param containerName registered container name
     * @param items data items (must match controllersResult size)
     * @param controllersResult pre-created controllers from {@link #createCardControllersAsync}
     * @param dataSetter optional callback for updates during attachment
     * @param stage stage containing target container
     * @param horizontalMargin applied margin between nodes (x-axis)
     * @param verticalMargin applied margin between nodes (y-axis)
     * @return future completing when attachment finishes
     * @throws IllegalStateException if not on FX thread
     * @throws IllegalArgumentException if container not found or sizes mismatch
     *
     * @CSS .frostfx-spacer The CSS class applied to spacing elements between cards.
     *      Customize margins using: .frostfx-spacer { -fx-background-color: #e0e0e0; }
     *      Target horizontal spacers: .frostfx-spacer:horizontal { /*styles }
     *       Target vertical spacers: .frostfx-spacer:vertical { /* styles  }
     */
    public <T, C> CompletableFuture<Void> attachCardControllersAsync(
            String containerName, List<T> items, CardControllersResult<T, C> controllersResult,
            BiConsumer<C, T> dataSetter, Stage stage, double horizontalMargin, double verticalMargin) {

        CompletableFuture<Void> future = new CompletableFuture<>();

        runOnFxThread(() -> {
            try {
                loadingService.attachCardControllers(getContainerFromRegistry(containerName,stage),items
                        ,controllersResult,dataSetter,horizontalMargin,verticalMargin);
                future.complete(null);
            } catch (Exception e) {
                future.completeExceptionally(e);
            }
        });

        return future;
    }


    // ==================== INTERNAL HELPERS ====================//

    /**
     * Validates parameters for registration methods.
     *
     * @param name the name parameter to validate
     * @param component the component parameter to validate
     * @param stage the stage parameter to validate
     * @throws IllegalArgumentException if any parameter is invalid
     */
    private void validateRegistrationParams(String name, Object component, Stage stage) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("Name cannot be null or blank");
        if (component == null) throw new IllegalArgumentException("Component cannot be null");
        if (stage == null) throw new IllegalArgumentException("Stage cannot be null");
    }

    /**
     * Ensures the primary stage has been set before performing operations.
     *
     * @throws IllegalStateException if the primary stage has not been set
     */
    private void ensurePrimaryStageSet() {
        if (primaryStage == null) {
            throw new IllegalStateException("Primary stage has not been set. "
                    + "Call SceneManager.init(primaryStage) first.");
        }
    }

    // ==================== INTERNAL HELPER METHODS ====================//

    /**
     * Gets a container from the registry by name and stage.
     */
    public Pane getContainerFromRegistry(String containerName, Stage stage) {
        Map<String, Pane> containers = stageContainers.get(stage);
        if (containers == null || !containers.containsKey(containerName)) {
            throw new IllegalArgumentException("Container '" + containerName + "' not registered for stage");
        }
        return containers.get(containerName);
    }

    /**
     * Gets a panel from the registry by name and stage.
     */
    private AnchorPane getPanelFromRegistry(String panelName, Stage stage) {
        Map<String, AnchorPane> panels = stagePanels.get(stage);
        if (panels == null || !panels.containsKey(panelName)) {
            throw new IllegalArgumentException("Panel '" + panelName + "' not registered for stage");
        }
        return panels.get(panelName);
    }


    // ==================== INTERNAL HELPERS ====================

    /**
     * Gets the FXML path for a registered card name.
     *
     * @param cardName the registered name of the card
     * @return the FXML path associated with the card name
     * @throws IllegalArgumentException if the card is not registered
     */
    private String getCardPath(String cardName) {
        if (!cardPathsMap.containsKey(cardName)) {
            throw new IllegalArgumentException("Card '" + cardName + "' is not registered. " +
                    "Available cards: " + cardPathsMap.keySet());
        }
        return cardPathsMap.get(cardName);
    }

    // ==================== STATE CHECKS ====================//

    /**
     * Checks if a panel is in the primary stage's scene
     *
     * @param panelName Panel name to check
     * @return true if panel is in scene, false otherwise
     */
    public boolean isPanelInScene(String panelName) {
        ensurePrimaryStageSet();
        return isPanelInScene(panelName, primaryStage);
    }

    /**
     * DEBUG HELPER: Checks if a panel exists and is attached to a scene in the given stage
     * Returns false instead of throwing for null inputs to facilitate debugging
     *
     * @param panelName Name of the panel to check
     * @param stage Stage to search in (can be null for safe debugging)
     * @return true if panel exists and is in a scene, false otherwise (including null inputs)
     */
    public boolean isPanelInScene(String panelName, Stage stage) {
        // Safe debugging - return false instead of throwing
        if (stage == null || panelName == null) {
            System.out.println("[DEBUG] isPanelInScene: Null input - stage: " +
                    (stage == null) + ", panelName: " + (panelName == null));
            return false;
        }

        Map<String, AnchorPane> panels = stagePanels.get(stage);
        if (panels == null) {
            System.out.println("[DEBUG] isPanelInScene: No panels registered for stage: " + stage);
            return false;
        }

        AnchorPane panel = panels.get(panelName);
        boolean result = panel != null && panel.getScene() != null;

        System.out.println("[DEBUG] isPanelInScene: panel='" + panelName +
                "', stage=" + stage + ", result=" + result);
        return result;
    }

    // ==================== DEBUGGING TOOLS ====================

    /**
     * Prints debug information about registered cards and panels
     */
    public void debugPrintRegistry() {
        System.out.println("=== CardLoader Debug Info ===");
        System.out.println("Primary Stage: " +
                (primaryStage != null ? primaryStage.getTitle() : "Not set"));

        System.out.println("\nRegistered Cards:");
        if (cardPathsMap.isEmpty()) {
            System.out.println("  No cards registered");
        } else {
            cardPathsMap.forEach((name, path) ->
                    System.out.println("  " + name + " -> " + path));
        }

        System.out.println("\nStage Panels:");
        if (stagePanels.isEmpty()) {
            System.out.println("  No panels registered for any stage");
        } else {
            stagePanels.forEach((stage, panels) -> {
                System.out.println("  Stage: " + stage.getTitle());
                panels.forEach((name, panel) -> {
                    String inScene = panel.getScene() != null ? " [IN SCENE]" : " [NOT IN SCENE]";
                    System.out.println("    " + name + inScene);
                });
            });
        }
        System.out.println("=============================");
    }

    /**
     * Clears all stage-specific registrations while preserving reusable card definitions
     * Use cases:
     * - Stage reinitialization
     * - Testing cleanup
     * - Application reset scenarios
     *<p>
     * NOTE: This preserves card path registrations since cards are reusable across stages
     *</p>
     *
     * @see #clearAll() for complete cleanup including card definitions
     */
    public void clearAllRegistrations() {
        stagePanels.clear();       // Clear stage-specific panels
        stageContainers.clear();   // Clear stage-specific containers
        currentPanelView = "";     // Reset current view tracking
    }

    /**
     * Completely resets the CardLoader to initial state
     * WARNING: This will remove ALL registrations including reusable card definitions
     * Only use for complete application shutdown or testing scenarios
     */
    public void clearAll() {
        clearAllRegistrations();   // Clear stage-specific stuff
        cardPathsMap.clear();      // Clear cards too (for complete reset)
    }

    /**
     * Clears registrations for a specific stage only
     * Useful for dynamic stage management without affecting other stages
     *
     * @param stage The stage to clear registrations for
     * @return true if stage had registrations that were cleared, false otherwise
     */
    public boolean clearStageRegistrations(Stage stage) {
        boolean hadRegistrations = false;

        if (stagePanels.containsKey(stage)) {
            stagePanels.get(stage).clear();
            hadRegistrations = true;
        }

        if (stageContainers.containsKey(stage)) {
            stageContainers.get(stage).clear();
            hadRegistrations = true;
        }

        return hadRegistrations;
    }

    /**
     * Checks if a card with the given name has been registered.
     *
     * @param cardName the name of the card to check
     * @return true if the card is registered, false otherwise
     */
    public boolean isCardRegistered(String cardName) {
        return cardPathsMap.containsKey(cardName);
    }
}
