package customComponents;

import javafx.application.Application;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.ScrollPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;

public class TestShrinkWrapPane extends Application {

    @Override
    public void start(Stage stage) {
        // Create master scroll pane
        ScrollPane masterScrollPane = new ScrollPane();
        masterScrollPane.setFitToWidth(true);
        masterScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        masterScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);

        // Master ShrinkWrapPane - override computePref size with logging for debug
        ShrinkWrapPane masterShrinkWrap = new ShrinkWrapPane() {
            @Override
            protected double computePrefWidth(double height) {
                double prefWidth = super.computePrefWidth(height);
                return prefWidth;
            }

            @Override
            protected double computePrefHeight(double width) {
                double totalHeight = getInsets().getTop() + getInsets().getBottom();
                int numChildren = getChildren().size();
                if (numChildren == 0) {
                    return totalHeight;
                }

                for (int i = 0; i < numChildren; i++) {
                    javafx.scene.Node child = getChildren().get(i);
                    // Let child compute its preferred height for given width
                    double childPrefHeight = child.prefHeight(width);
                    totalHeight += childPrefHeight;

                    // Add vertical gap except after last child
                    if (i < numChildren - 1) {
                        totalHeight += getVgap();
                    }
                }
                return totalHeight;
            }


        };
        masterShrinkWrap.setHgap(10);
        masterShrinkWrap.setVgap(10);

        // Add several "horizontal line" ShrinkWrapPane children to the master
        for (int i = 0; i < 10; i++) {
            ShrinkWrapPane linePane = new ShrinkWrapPane();
            linePane.setHgap(5);
            linePane.setVgap(5);

            // Add some sample rectangles simulating controls
            for (int j = 0; j < 5 + i; j++) {
                Rectangle rect = new Rectangle(50 + j * 10, 30, Color.color(Math.random(), Math.random(), Math.random()));
                linePane.getChildren().add(rect);
            }
            masterShrinkWrap.getChildren().add(linePane);
        }

        // Set masterShrinkWrap as content of ScrollPane
        masterScrollPane.setContent(masterShrinkWrap);

        // Set up scene and show
        Scene scene = new Scene(masterScrollPane, 600, 400);
        stage.setScene(scene);
        stage.setTitle("Nested ScrollPane + ShrinkWrapPane Test");
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}
