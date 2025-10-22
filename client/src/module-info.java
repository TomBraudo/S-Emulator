module client {
    requires javafx.controls;
    requires javafx.fxml;
    requires okhttp3;
    requires okio;
    requires kotlin.stdlib;
    requires com.google.gson;
    requires transitive dto;

    opens com.app.ui to javafx.fxml, javafx.graphics;
    opens com.app.ui.dashboard to javafx.fxml;
    opens com.app.ui.dashboard.components to javafx.fxml;
    opens com.app.ui.dashboard.components.user to javafx.fxml;
    opens com.app.ui.dashboard.components.program to javafx.fxml;
    opens com.app.ui.dashboard.components.function to javafx.fxml;
    opens com.app.ui.dashboard.components.chargeCredits to javafx.fxml;
    opens com.app.ui.dashboard.components.statisticsView to javafx.fxml;
    opens com.app.ui.dashboard.components.errorComponents to javafx.fxml;
    opens com.app.ui.execute to javafx.fxml;
    opens com.app.ui.execute.components.commandRow to javafx.fxml;
    opens com.app.ui.execute.components.executionComponents to javafx.fxml;
    opens com.app.ui.execute.components.historyView to javafx.fxml;
    opens com.app.ui.execute.components.inputComponent to javafx.fxml;
    opens com.app.ui.main to javafx.graphics;
    opens com.app.ui.login to javafx.fxml;
    opens com.app.ui.utils to com.google.gson;
    opens com.app.ui.chat to javafx.fxml;
    opens com.app.ui.chat.components to javafx.fxml;
}