package com.app.ui.execute.components.commandRow;

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
    private IntConsumer onCommandClick;
    private String baseLabelStyle;
    private boolean debugHighlighted;
    private boolean architectureCompatible;
    private Integer commandArchitecture;
    private Integer selectedArchitecture;

    @FXML
    private void initialize() {
        // capture the base style from FXML (set in FXML file)
        baseLabelStyle = commandLabel.getStyle();
        gutter.addEventHandler(MouseEvent.MOUSE_CLICKED, e -> {
            if (onToggle != null) {
                onToggle.accept(commandIndex);
            }
        });
        commandLabel.addEventHandler(MouseEvent.MOUSE_CLICKED, e -> {
            if (onCommandClick != null) {
                onCommandClick.accept(commandIndex);
            }
        });
    }

    public void init(int index, String text, boolean isActive, IntConsumer onToggle) {
        this.commandIndex = index;
        this.onToggle = onToggle;
        this.commandLabel.setText(text);
        setActive(isActive);
    }

    public void setOnCommandClicked(IntConsumer onClick){
        this.onCommandClick = onClick;
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

    public void setArchitecture(int commandArch, int selectedArch) {
        this.commandArchitecture = commandArch;
        this.selectedArchitecture = selectedArch;
        this.architectureCompatible = commandArch <= selectedArch;
        applyCurrentStyle();
    }

    private void applyCurrentStyle(){
        // Remove any existing highlight classes
        commandLabel.getStyleClass().removeAll("row-debug", "row-arch-compatible", "row-arch-incompatible");
        
        if (debugHighlighted){
            commandLabel.getStyleClass().add("row-debug");
        } else if (commandArchitecture != null && selectedArchitecture != null) {
            // Apply architecture highlighting
            if (architectureCompatible) {
                commandLabel.getStyleClass().add("row-arch-compatible");
            } else {
                commandLabel.getStyleClass().add("row-arch-incompatible");
            }
        }
    }

    public String getCommandText(){
        return commandLabel.getText();
    }
}

