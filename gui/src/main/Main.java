package main;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.stage.Stage;

public class Main extends Application {
    @Override
    public void start(Stage primaryStage){
        primaryStage.setTitle("JavaFX Native IntelliJ Test");
        primaryStage.setScene(new Scene(new Label("It works!"), 400, 200));
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
