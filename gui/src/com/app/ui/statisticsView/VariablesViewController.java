package com.app.ui.statisticsView;

import com.api.ProgramResult;
import com.api.Statistic;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import java.util.List;

public class VariablesViewController {
    @FXML
    private VBox container;
    @FXML
    private Label header;
    @FXML
    private Button executeBtn;

    private Runnable onExecute;

    public void setStatistic(Statistic stat){
        if (stat == null){
            renderVariables(List.of());
            return;
        }
        renderVariables(stat.getVariableToValue());
    }

    public void setVariables(List<ProgramResult.VariableToValue> variables){
        renderVariables(variables);
    }

    private void renderVariables(List<ProgramResult.VariableToValue> variables){
        // Remove any previous rows except the header
        container.getChildren().removeIf(node -> node != header && node != executeBtn);

        if (variables == null || variables.isEmpty()){
            Label empty = new Label("No variables recorded");
            empty.getStyleClass().add("label-meta");
            container.getChildren().add(empty);
            return;
        }

        for (ProgramResult.VariableToValue v : variables){
            Label row = new Label(v.variable() + ": " + v.value());
            row.getStyleClass().add("label-strong");
            row.setWrapText(true);
            container.getChildren().add(row);
        }
    }

    @FXML
    private void initialize(){
        if (executeBtn != null){
            executeBtn.setOnAction(e -> {
                if (onExecute != null){
                    onExecute.run();
                }
                // close window
                if (executeBtn.getScene() != null && executeBtn.getScene().getWindow() != null){
                    executeBtn.getScene().getWindow().hide();
                }
            });
        }
    }

    public void setOnExecute(Runnable onExecute){
        this.onExecute = onExecute;
    }
}

