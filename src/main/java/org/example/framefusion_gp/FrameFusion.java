package org.example.framefusion_gp;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import java.io.IOException;

public class FrameFusion extends Application {

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(FrameFusion.class.getResource("home_page.fxml"));
        Parent root = fxmlLoader.load();

        Scene scene = new Scene(root);
        scene.getStylesheets().add(FrameFusion.class.getResource("gallery.css").toExternalForm());

        stage.setTitle("Frame Fusion");
        stage.getIcons().add(new Image(getClass().getResourceAsStream("frameFusion_icon.png")));
        stage.setScene(scene);
        stage.show();

    }

    public static void main(String[] args) {
        Database.initializeDatabase();
        launch();
    }
}