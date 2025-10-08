module client {
    requires javafx.controls;
    requires javafx.fxml;
    requires okhttp3;
    requires okio;
    requires kotlin.stdlib;
    requires kotlin.stdlib.common;
    requires org.jetbrains.annotations;
    requires com.google.gson;

    opens com.app.ui to javafx.fxml, javafx.graphics;
    opens com.app.ui.dashboard to javafx.fxml;
    opens com.app.ui.main to javafx.graphics;
    opens com.app.ui.login to javafx.fxml;
}