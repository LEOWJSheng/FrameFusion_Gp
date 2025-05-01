package org.example.framefusion_gp;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.*;

public class GalleryController {

    @FXML
    private Button HomeBtn;

    @FXML
    private Button addImageBtn;

    @FXML
    private FlowPane galleryFlowPane;

    @FXML
    private ScrollPane scrollPane;

    @FXML
    private Label emptyImgLabel;

    @FXML
    private Button makeVideoBtn;

    private static final int IMAGE_SIZE = 120;
    private final Set<StackPane> selectedImages = new HashSet<>();

    public void initialize() {
        galleryFlowPane.setPadding(new Insets(20));
        addImageBtn.setOnAction(e -> uploadImage());
        loadImages();  // Load images on app start
        HomeBtn.setOnAction(e -> goToHome());
        makeVideoBtn.setDisable(true);
        makeVideoBtn.setOnAction(e -> makeVideo());
    }

    private void makeVideo() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("videoController.fxml"));
            Parent videoRoot = loader.load();
            Scene scene = new Scene(videoRoot);
            Stage stage = (Stage) makeVideoBtn.getScene().getWindow();
            stage.setScene(scene);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void loadImages() {
        galleryFlowPane.getChildren().clear();
        List<ImageData> images = Database.getImages();

        if (images.isEmpty()) {
            galleryFlowPane.getChildren().add(emptyImgLabel);
            return;
        }

        for (ImageData data : images) {
            // Container for image + optional heart icon
            StackPane stack = new StackPane();
            stack.setPrefSize(130, 130);
            stack.setMinSize(130, 130);
            stack.setMaxSize(130, 130);

            // Style container for border, shadow, etc.
            stack.getStyleClass().add("image-cell");

            // Image view setup
            ImageView view = new ImageView(new Image("file:" + data.getPath()));
            //make sure every photo in gallery have same size and fit ratio
            view.setPreserveRatio(true);
            view.setFitWidth(IMAGE_SIZE);
            view.setFitHeight(IMAGE_SIZE);

            stack.getChildren().add(view);
            StackPane.setAlignment(view, Pos.CENTER);

            // Add heart icon if annotated
            if (data.getAnnotation() != null && !data.getAnnotation().isEmpty()) {
                Label heart = new Label("♥");
                heart.getStyleClass().add("heart-label");
                StackPane.setAlignment(heart, Pos.TOP_RIGHT);
                stack.getChildren().add(heart);
            }

            // Click: Ctrl = multi-select; else open editor
            stack.setOnMouseClicked(e -> {
                if (e.isControlDown()) {
                    if (selectedImages.contains(stack)) {
                        stack.setStyle(""); // Unhighlight
                        selectedImages.remove(stack);
                    } else {
                        stack.setStyle("-fx-border-color: red; -fx-border-width: 2;");
                        selectedImages.add(stack);
                    }
                    makeVideoBtn.setDisable(selectedImages.size() <= 1);
                } else {
                    try {
                        openAnnotationEditor(data);
                    } catch (SQLException ex) {
                        ex.printStackTrace();
                    }
                }
            });

            // Add to galleryFlowPane
            galleryFlowPane.getChildren().add(stack);
        }
    }

    @FXML
    private void goToHome() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("home_page.fxml"));
            Parent homeRoot = loader.load();
            Scene scene = new Scene(homeRoot);
            Stage stage = (Stage) HomeBtn.getScene().getWindow();
            stage.setScene(scene);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void uploadImage() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Upload Image");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg")
        );
        File file = fileChooser.showOpenDialog(galleryFlowPane.getScene().getWindow());

        if (file != null) {
            TextInputDialog dialog = new TextInputDialog();
            dialog.setHeaderText("Add annotation (optional)");
            dialog.setContentText("Annotation:");

            Optional<String> result = dialog.showAndWait();
            String annotation = result.orElse("");

            Database.insertImage(file.getAbsolutePath(), annotation);
            loadImages();
        }
    }

    private void openAnnotationEditor(ImageData data) throws SQLException {
        TextInputDialog dialog = new TextInputDialog(data.getAnnotation());
        dialog.setHeaderText("Edit annotation");
        dialog.setContentText("Annotation:");

        Optional<String> result = dialog.showAndWait();
        if (result.isPresent()) {
            String newAnnotation = result.get();
            Database.updateAnnotation(data.getId(), newAnnotation);
            loadImages();
        }
    }
}
