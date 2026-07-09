module com.soulknight {
    requires javafx.controls;
    requires javafx.media;
    requires javafx.graphics;
    requires javafx.fxml;
    opens com.soulknight.ui to javafx.fxml;
    opens com.soulknight to javafx.graphics, javafx.fxml;
    exports com.soulknight;
}