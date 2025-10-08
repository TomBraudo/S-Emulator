package com.app.ui.dashboard.components.function;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;

public class FunctionLineController {
    @FXML private Label nameLabel;
    @FXML private Label sourceProgramLabel;
    @FXML private Label usernameLabel;
    @FXML private Label commandsCountLabel;
    @FXML private Label maxExpansionLevelLabel;
    @FXML private VBox root;
    
    private Runnable onPressAction;
    private boolean isSelected = false;

    public void init(String name,
                     String sourceProgram,
                     String username,
                     String commandsCount,
                     String maxExpansionLevel) {
        nameLabel.setText(name);
        sourceProgramLabel.setText(sourceProgram);
        usernameLabel.setText(username);
        commandsCountLabel.setText(commandsCount);
        maxExpansionLevelLabel.setText(maxExpansionLevel);
    }

    @FXML
    private void initialize() {
        // Set click handler on the root VBox
        if (root != null) {
            root.setOnMouseClicked(this::handleClick);
        }
    }
    
    private void handleClick(MouseEvent event) {
        if (onPressAction != null) {
            onPressAction.run();
        }
    }

    public void setOnPressAction(Runnable action) {
        this.onPressAction = action;
    }
    
    public void setSelected(boolean selected) {
        this.isSelected = selected;
        if (selected) {
            root.getStyleClass().add("selected");
        } else {
            root.getStyleClass().remove("selected");
        }
    }
    
    public boolean isSelected() {
        return isSelected;
    }
}
