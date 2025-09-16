package com.app.ui.statisticsView;

import com.api.ProgramResult;
import com.api.Statistic;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import java.util.List;

public class VariablesViewController {
    @FXML
    private VBox container;
    @FXML
    private Label header;

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
        container.getChildren().removeIf(node -> node != header);

        if (variables == null || variables.isEmpty()){
            Label empty = new Label("No variables recorded");
            empty.setStyle("-fx-text-fill: #666; -fx-font-style: italic;");
            container.getChildren().add(empty);
            return;
        }

        for (ProgramResult.VariableToValue v : variables){
            Label row = new Label(v.variable() + ": " + v.value());
            row.setStyle("-fx-font-weight: bold;");
            row.setWrapText(true);
            container.getChildren().add(row);
        }
    }
}

