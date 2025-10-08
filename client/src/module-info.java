module client {
    requires javafx.controls;
    requires javafx.fxml;

    opens com.app.ui to javafx.fxml, javafx.graphics;
    opens com.app.ui.dashboard to javafx.fxml;
    opens com.app.ui.main to javafx.graphics;
}