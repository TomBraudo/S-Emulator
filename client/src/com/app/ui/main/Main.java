package com.app.ui.main;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.util.Objects;

public class Main extends javafx.application.Application {
    @Override
    public void start(Stage stage) throws Exception {
        Parent root = FXMLLoader.load(Objects.requireNonNull(getClass().getResource("/com/app/ui/login/loginPage.fxml")));
        Scene scene = new Scene(root);
        scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/com/app/ui/app.css")).toExternalForm());
        stage.setScene(scene);
        stage.setTitle("S-Emulator - Login");
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}


