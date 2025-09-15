module gui {
    requires javafx.controls;
    requires javafx.fxml;
    requires com.engine; // The native IntelliJ module name for your engine module
    opens com.app.ui to javafx.fxml, javafx.graphics;
    opens customComponents to javafx.graphics;
    opens com.app.ui.inputComponent to javafx.fxml;
    exports customComponents to javafx.fxml;
}
