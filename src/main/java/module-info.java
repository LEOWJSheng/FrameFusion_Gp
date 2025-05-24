module org.example.framefusion_gp {
    requires javafx.swing;
    requires javafx.controls;
    requires javafx.fxml;
    requires java.desktop;
    requires javafx.media;


    requires org.kordamp.ikonli.javafx;
    requires java.sql;
    requires opencv;

    opens org.example.framefusion_gp to javafx.fxml;
    exports org.example.framefusion_gp;
}
