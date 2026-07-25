module com.soulknight {
    requires javafx.controls;
    requires javafx.media;
    requires javafx.graphics;
    requires javafx.fxml;
    requires com.fasterxml.jackson.annotation;
    requires com.fasterxml.jackson.databind;
    requires javafx.base;
    opens com.soulknight.ui to javafx.fxml;
    opens com.soulknight to javafx.graphics, javafx.fxml;
    opens com.soulknight.map.json to com.fasterxml.jackson.databind;
    opens com.soulknight.model to com.fasterxml.jackson.databind;
    exports com.soulknight.map.json to com.fasterxml.jackson.databind;
    exports com.soulknight.model;
    exports com.soulknight;
}