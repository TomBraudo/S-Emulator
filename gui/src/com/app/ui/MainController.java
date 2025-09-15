package com.app.ui;

import com.app.ui.inputComponent.InputFormController;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.*;

public class MainController {
    @FXML
    private Button newInputBtn;
    @FXML
    private ScrollPane inputScrollPane;
    @FXML
    private AnchorPane inputDisplayPane; // Added this line
    @FXML
    private VBox inputVbox;

    private List<Integer> curInput;

    @FXML
    public void initialize() {
        // The inputDisplayPane is now directly injected by the FXMLLoader
        // We will add the VBox in FXML instead of here.

        newInputBtn.setOnAction(event -> openInputForm());
    }

    private void openInputForm() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("inputComponent/inputForm.fxml"));
            ScrollPane root = loader.load();

            InputFormController inputFormController = loader.getController();

            // **1. Create the list of input variables you want to pass**
            List<String> inputVariables = new ArrayList<>();
            // Add your variables here. For example:
            inputVariables.add("x1");
            inputVariables.add("x2");
            inputVariables.add("x3");
            // Or you could retrieve them from another part of your application.

            // **2. Pass the list of variables to the form controller**
            inputFormController.initData(inputVariables);

            // 3. Pass a callback to receive the map
            inputFormController.setDataCallback(inputMap -> {
                // 4. When finish is called, print each entry as a label
                displayInputData(inputMap);
            });

            Stage formStage = new Stage();
            formStage.setTitle("Input Form");
            formStage.setScene(new Scene(root));

            // Make the main window not interactable while the form is open
            formStage.initModality(Modality.APPLICATION_MODAL);
            formStage.showAndWait();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Inside com.app.ui.MainController

    private void displayInputData(Map<String, Integer> data) {
        // Clear the VBox's children
        inputVbox.getChildren().clear();

        // Create a custom Comparator to sort the keys numerically
        Comparator<String> numericComparator = (key1, key2) -> {
            // Extract the integer part of the string keys (e.g., "x12" -> 12)
            int num1 = Integer.parseInt(key1.substring(1));
            int num2 = Integer.parseInt(key2.substring(1));
            return Integer.compare(num1, num2);
        };

        // Create a TreeMap with the custom comparator to sort the map by index
        TreeMap<String, Integer> sortedData = new TreeMap<>(numericComparator);
        sortedData.putAll(data); // Put all entries from the original map into the new sorted map

        // Add a header
        Label header = new Label("Input Data Received:");
        header.setStyle("-fx-font-weight: bold;");
        inputVbox.getChildren().add(header);

        // Add a new label for each entry, iterating through the sorted map
        for (Map.Entry<String, Integer> entry : sortedData.entrySet()) {
            Label dataLabel = new Label(entry.getKey() + ": " + entry.getValue());
            inputVbox.getChildren().add(dataLabel);
        }

        int maxIndex = 0;
        if (!sortedData.isEmpty()) {
            String lastKey = sortedData.lastKey();
            maxIndex = Integer.parseInt(lastKey.substring(1));
        }

        // Create the List<Integer> with values for each variable from 1 to maxIndex
        curInput = new ArrayList<>();
        for (int i = 1; i <= maxIndex; i++) {
            String variableKey = "x" + i;
            int value = sortedData.getOrDefault(variableKey, 0);
            curInput.add(value);
        }

    }
}
