package org.example.framefusion_gp;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.stage.Stage;

import java.io.IOException;

public class HomeController {

    @FXML
    private Button galleryBtn;

    @FXML
    private Button videoBtn;

    public void initialize() {
        galleryBtn.setOnAction(e -> goToGallery());
        videoBtn.setOnAction(e -> goToVideo());
    }

    @FXML
    private void goToVideo() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("videoPageController.fxml"));
            Parent homeRoot = loader.load();
            Scene scene = new Scene(homeRoot);
            Stage stage = (Stage) videoBtn.getScene().getWindow();
            stage.setScene(scene);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void goToGallery() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("gallery.fxml"));
            Parent homeRoot = loader.load();
            Scene scene = new Scene(homeRoot);
            Stage stage = (Stage) galleryBtn.getScene().getWindow();
            stage.setScene(scene);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
