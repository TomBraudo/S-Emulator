package com.app.ui.dashboard.components.statisticsView;

import com.dto.api.Statistic;
import com.app.ui.dashboard.components.errorComponents.ErrorMessageController;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.stream.Collectors;

public class RunDetailsController {
    @FXML
    private Label indexLabel;
    @FXML
    private Label runTypeLabel;
    @FXML
    private Label nameLabel;
    @FXML
    private Label architectureLabel;
    @FXML
    private Label levelLabel;
    @FXML
    private Label resultLabel;
    @FXML
    private Label cyclesLabel;
    @FXML
    private VBox root;
    @FXML
    private GridPane gridPane;

    private Statistic statistic;
    private java.util.function.Consumer<java.util.List<com.dto.api.ProgramResult.VariableToValue>> onExecuteVariables;

    public void setStatistic(Statistic stat){
        this.statistic = stat;
        indexLabel.setText(String.valueOf(stat.getIndex()));
        runTypeLabel.setText(stat.getRunType().toString());
        nameLabel.setText(stat.getProgramName());
        architectureLabel.setText(stat.getArchitecture());
        levelLabel.setText(String.valueOf(stat.getExpansionLevel()));
        resultLabel.setText(String.valueOf(stat.getResult()));
        cyclesLabel.setText(String.valueOf(stat.getCyclesCount()));

        // Open variables view on click
        if (root != null) {
            root.setOnMouseClicked(e -> openVariablesWindow());
        }
    }

    private void openVariablesWindow(){
        if (statistic == null) return;
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("variablesView.fxml"));
            VariablesViewController controller = new VariablesViewController();
            loader.setController(controller);
            ScrollPane pane = loader.load();
            controller.setStatistic(statistic);
            controller.setOnExecute(() -> {
                if (onExecuteVariables != null){
                    onExecuteVariables.accept(statistic.getVariableToValue());
                }
            });

            Stage stage = new Stage();
            stage.setTitle("Variables - Run #" + statistic.getIndex());
            stage.setScene(new Scene(pane, 420, 520));
            stage.initModality(Modality.NONE);
            stage.show();
        } catch (IOException ex){
            ErrorMessageController.showError("Failed to open variables view\n" + ex.getMessage());
        }
    }

    public void setOnExecuteVariables(java.util.function.Consumer<java.util.List<com.dto.api.ProgramResult.VariableToValue>> handler){
        this.onExecuteVariables = handler;
    }
}
