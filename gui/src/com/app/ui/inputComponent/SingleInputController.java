package com.app.ui.inputComponent;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

public class SingleInputController {
    @FXML
    private Label xiLabel;

    @FXML
    private TextField xiTextField;

    @FXML
    private Button xiDeleteBtn;

    private final int index;

    public SingleInputController(int index){
        this.index = index;
    }

    @FXML
    private void initialize(){
        String baseId = "x" + index;
        xiLabel.setId(baseId + "Label");
        xiTextField.setId(baseId + "TextField");
        xiDeleteBtn.setId(baseId + "DeleteBtn");
        xiLabel.setText("x" + index);
    }

    public String getLabelText(){
        return xiLabel.getText();
    }

    public void setLabelText(String text){
        xiLabel.setText(text);
    }

    public String getTextFieldValue(){
        return xiTextField.getText();
    }

    public Button getDeleteButton(){
        return xiDeleteBtn;
    }

    public int getIndex(){
        return index;
    }

}
