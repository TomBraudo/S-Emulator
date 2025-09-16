package com.app.ui.statisticsView;

import com.api.Statistic;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

import java.util.stream.Collectors;

public class RunDetailsController {
    @FXML
    private Label indexLabel;
    @FXML
    private Label expansionLevelLabel;
    @FXML
    private Label inputLabel;
    @FXML
    private Label resultLabel;
    @FXML
    private Label cyclesLabel;

    public void setStatistic(Statistic stat){
        indexLabel.setText(String.valueOf(stat.getIndex()));
        expansionLevelLabel.setText(String.valueOf(stat.getExpansionLevel()));
        String input = stat.getInput().stream().map(String::valueOf).collect(Collectors.joining(", "));
        inputLabel.setText(input);
        resultLabel.setText(String.valueOf(stat.getResult()));
        cyclesLabel.setText(String.valueOf(stat.getCyclesCount()));
    }
}


