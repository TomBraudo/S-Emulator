package com.app.ui.commandRow;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Circle;

import java.util.function.IntConsumer;

public class CommandRowController {
    @FXML
    private StackPane gutter;
    @FXML
    private Circle bpCircle;
    @FXML
    private Label commandLabel;

    private int commandIndex;
    private boolean active;
    private IntConsumer onToggle;
    private String baseLabelStyle;
    private boolean debugHighlighted;
    private boolean searchHighlighted;

    @FXML
    private void initialize() {
        // capture the base style from FXML (set in FXML file)
        baseLabelStyle = commandLabel.getStyle();
        gutter.addEventHandler(MouseEvent.MOUSE_CLICKED, e -> {
            if (onToggle != null) {
                onToggle.accept(commandIndex);
            }
        });
    }

    public void init(int index, String text, boolean isActive, IntConsumer onToggle) {
        this.commandIndex = index;
        this.onToggle = onToggle;
        this.commandLabel.setText(text);
        setActive(isActive);
    }

    public void setActive(boolean active) {
        this.active = active;
        bpCircle.setVisible(active);
        if (active) {
            bpCircle.setStyle("-fx-fill: red;");
        } else {
            bpCircle.setStyle("");
        }
    }

    public int getCommandIndex() {
        return commandIndex;
    }

    public void setHighlighted(boolean highlighted){
        this.debugHighlighted = highlighted;
        applyCurrentStyle();
    }

    public void setSearchHighlighted(boolean highlighted){
        this.searchHighlighted = highlighted;
        applyCurrentStyle();
    }

    private void applyCurrentStyle(){
        if (debugHighlighted){
            commandLabel.setStyle(baseLabelStyle.replace("-fx-background-color: white;", "-fx-background-color: #ffebeb;"));
        } else if (searchHighlighted){
            commandLabel.setStyle(baseLabelStyle.replace("-fx-background-color: white;", "-fx-background-color: #b7f7b0;"));
        } else {
            commandLabel.setStyle(baseLabelStyle);
        }
    }

    public String getCommandText(){
        return commandLabel.getText();
    }
}


