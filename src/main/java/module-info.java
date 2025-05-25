module org.example.framefusion_gp {
    requires javafx.swing;
    requires javafx.controls;
    requires javafx.fxml;
    requires java.desktop;
    requires javafx.media;
    requires opencv;
    
    requires org.kordamp.ikonli.javafx;
    requires java.sql;

    opens org.example.framefusion_gp to javafx.fxml;
    exports org.example.framefusion_gp;
}
