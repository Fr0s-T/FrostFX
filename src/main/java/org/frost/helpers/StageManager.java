package org.frost.helpers;

import javafx.stage.Stage;

import java.util.HashMap;
import java.util.Map;

public class StageManager {


    private Stage primaryStage;
    private final Map<String,Stage> secondaryStagesMap = new HashMap<>();

    public StageManager(){}

    /**
     * Sets the primary application stage (REQUIRED)
     * Call this once in your Main.start() method
     *
     * @param stage The primary JavaFX stage
     */
    public void setPrimaryStage(Stage stage) {
        primaryStage = stage;
    }

    public void registerSecondaryStage(String stageName, Stage newStage){
        if (stageName.isEmpty()) throw new IllegalArgumentException("A Stage Name Cannot be Blank");
        if (newStage == null) throw new IllegalArgumentException("A new secondary Stage Cannot be null");
        secondaryStagesMap.put(stageName,newStage);
    }

    public Stage getPrimaryStage(){
        return primaryStage;
    }

    public Stage getARegisterdedStage(String stageName){
        return secondaryStagesMap.get(stageName);
    }

    public Map<String,Stage> getSecondaryStagesMap(){
        return secondaryStagesMap;
    }

}
