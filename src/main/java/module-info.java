module org.example.framefusion_gp {
    requires javafx.controls;
    requires javafx.fxml;
<<<<<<< HEAD
    requires javafx.swing;
    requires java.desktop;
    requires java.sql;
    requires org.kordamp.ikonli.javafx;

    opens org.example.framefusion_gp to javafx.fxml;
    exports org.example.framefusion_gp;
}
=======

    requires org.kordamp.ikonli.javafx;
    requires java.sql;

    opens org.example.framefusion_gp to javafx.fxml;
    exports org.example.framefusion_gp;
}
>>>>>>> b228602903204b8465b21a355254ea3b7cc347d4
