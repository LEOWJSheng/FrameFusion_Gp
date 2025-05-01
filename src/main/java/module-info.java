module org.example.framefusion_gp {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.kordamp.ikonli.javafx;
    requires java.sql;

    opens org.example.framefusion_gp to javafx.fxml;
    exports org.example.framefusion_gp;
}