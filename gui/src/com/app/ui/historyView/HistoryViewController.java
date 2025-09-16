package com.app.ui.historyView;

import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.List;

public class HistoryViewController {
    @FXML
    private VBox container;

    public void setHistory(List<String> history){
        container.getChildren().clear();
        if (history == null || history.isEmpty()){
            Label empty = new Label("No history available");
            empty.setStyle("-fx-text-fill: #666; -fx-font-style: italic;");
            container.getChildren().add(empty);
            return;
        }
        for (int i = 0; i < history.size(); i++){
            String line = history.get(i);
            Label lineLabel = new Label(line);
            lineLabel.setMaxWidth(Double.MAX_VALUE);
            lineLabel.setWrapText(true);
            lineLabel.setStyle(
                    "-fx-font-family: 'Consolas', 'Monospaced';" +
                    "-fx-font-weight: bold;" +
                    "-fx-padding: 6 8;" +
                    "-fx-background-color: white;" +
                    "-fx-border-color: #ccc;" +
                    "-fx-border-radius: 6;" +
                    "-fx-background-radius: 6;"
            );

            container.getChildren().add(lineLabel);

            if (i < history.size() - 1){
                Label arrow = new Label("\u2191"); // up arrow
                arrow.setStyle("-fx-text-fill: #888; -fx-font-size: 14px;");
                HBox arrowRow = new HBox(arrow);
                arrowRow.setAlignment(Pos.CENTER);
                HBox.setMargin(arrow, new Insets(4, 0, 4, 0));
                container.getChildren().add(arrowRow);
            }
        }
    }
}


