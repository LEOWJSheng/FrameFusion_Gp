module org.example.framefusion_gp {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.swing;
    requires java.desktop;
    requires java.sql;
    requires org.kordamp.ikonli.javafx;

    opens org.example.framefusion_gp to javafx.fxml;
    exports org.example.framefusion_gp;
}
