module no.dependent {
    requires transitive javafx.base;
    requires transitive javafx.graphics;
    requires transitive javafx.swing;
    requires transitive javafx.controls;
    requires transitive javafx.fxml;
    requires transitive jdk.unsupported;
    requires transitive java.sql;
    exports no.dependent;
}

