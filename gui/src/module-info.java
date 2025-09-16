module gui {
    requires javafx.controls;
    requires javafx.fxml;
    requires com.engine;
    requires org.eclipse.angus.activation;
    requires jakarta.xml.bind; // The native IntelliJ module name for your engine module
    opens com.app.ui to javafx.fxml, javafx.graphics;
    opens com.app.ui.commandRow to javafx.fxml;
    opens com.app.ui.historyView to javafx.fxml;
    opens customComponents to javafx.graphics;
    opens com.app.ui.inputComponent to javafx.fxml;
    exports customComponents to javafx.fxml;
    opens com.app.ui.errorComponents to javafx.fxml, javafx.graphics;
}
