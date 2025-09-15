package com.app.ui.inputComponent;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.*;
import java.util.function.Consumer;

public class InputFormController {
    @FXML
    private ScrollPane inputPane;
    @FXML
    private Label inputVariablesLabel;
    @FXML
    private TextField inputVariableTextField;
    @FXML
    private Button addInputVariableBtn;
    @FXML
    private Button finishBtn;

    private final List<SingleInputController> inputControllers = new ArrayList<>();
    private final Set<Integer> usedIndexes = new HashSet<>();

    private VBox inputLinesContainer;

    private Consumer<Map<String, Integer>> dataCallback;

    public void setDataCallback(Consumer<Map<String, Integer>> dataCallback){
        this.dataCallback = dataCallback;
    }

    public void initData(List<String> inputVariables){
        inputVariablesLabel.setText("Input variables in use: " + String.join(", ", inputVariables));
    }

    @FXML
    public void initialize(){
        inputLinesContainer = new VBox();
        inputPane.setContent(inputLinesContainer);
        addInputVariableBtn.setOnAction(event -> addInputVariable());
        finishBtn.setOnAction(event -> finishForm());
    }

    private void addInputVariable(){
        try{
            String variableName = inputVariableTextField.getText();
            if(variableName.length() < 2 || variableName.charAt(0) != 'x'){
                throw new IllegalArgumentException("Variable name must start with 'x' and have a numeric index.");
            }

            int index = Integer.parseInt(variableName.substring(1));

            if(usedIndexes.contains(index)){
                throw new IllegalArgumentException("Index " + index + " is already in use. Please enter a different one.");
            }

            FXMLLoader loader = new FXMLLoader(getClass().getResource("singleInputLine.fxml"));

            SingleInputController controller = new SingleInputController(index);
            loader.setController(controller);

            AnchorPane singleInputLine = loader.load();


            controller.getDeleteButton().setOnAction(event -> {
                inputLinesContainer.getChildren().remove(singleInputLine);
                inputControllers.remove(controller);
                usedIndexes.remove(index);
            });

            inputLinesContainer.getChildren().add(singleInputLine);
            inputControllers.add(controller);
            usedIndexes.add(index);

        } catch (IOException e) {
            e.printStackTrace();
        } catch (NumberFormatException e){
            System.err.println("Invalid input for index. Please enter a valid number after 'x'.");
        } catch (IllegalArgumentException e) {
            System.err.println(e.getMessage());
        }
    }

    private void finishForm(){
        Map<String, Integer> inputMap = new HashMap<>();

        try{
            for (SingleInputController controller : inputControllers){
                int inputValue = Integer.parseInt(controller.getTextFieldValue());
                inputMap.put(controller.getLabelText(), inputValue);
            }

            if(dataCallback != null){
                dataCallback.accept(inputMap);
            }

            Stage stage = (Stage) finishBtn.getScene().getWindow();
            stage.close();
        } catch (NumberFormatException e){
            System.err.println("Invalid input for input variable. Please ensure all values are valid numbers.");
        }
    }
}
