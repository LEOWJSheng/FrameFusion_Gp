package org.example.framefusion_gp;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Screen;
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
    @FXML
    private Button composeLayoutBtn;
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

            // Access the controller
            VideoController videoController = loader.getController();

            // Extract selected image files
            List<File> selectedFiles = new ArrayList<>();
            for (StackPane pane : selectedImages) {
                ImageView imageView = (ImageView) pane.getChildren().get(0);
                String url = imageView.getImage().getUrl().replace("file:", "");
                selectedFiles.add(new File(url));
            }

            // Pass selected files to the video controller
            videoController.importImages(selectedFiles);

            // Show the video editor
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

            MenuButton menuButton = new MenuButton("⋮");
            menuButton.setStyle("-fx-font-size: 14px; -fx-background-color: transparent;");
            MenuItem annotateItem = new MenuItem("Edit Annotation");
            MenuItem editItem = new MenuItem("Open Editor");

            annotateItem.setOnAction(e -> {
                try {
                    openAnnotationEditor(data);
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            });

            editItem.setOnAction(e -> openImageEditor(data.getPath()));
            menuButton.getItems().addAll(annotateItem, editItem);
            StackPane.setAlignment(menuButton, Pos.TOP_LEFT);
            stack.getChildren().add(menuButton);

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
                    showImagePreview(data.getPath());
                }
            });
            // Add to galleryFlowPane
            galleryFlowPane.getChildren().add(stack);
        }
    }

    private void showImagePreview(String imagePath) {
        try {
            // Load a simple FXML or just build the UI in code
            Stage preview = new Stage();
            preview.setTitle("Preview");

            ImageView iv = new ImageView(new Image("file:" + imagePath));
            iv.setPreserveRatio(true);
            iv.setSmooth(true);

            // make it fill up to, say, 80% of the screen
            Rectangle2D bounds = Screen.getPrimary().getVisualBounds();
            iv.setFitWidth(bounds.getWidth() * 0.6);
            iv.setFitHeight(bounds.getHeight() * 0.6);

            StackPane root = new StackPane(iv);
            root.setStyle("-fx-background-color: black");
            root.setPadding(new Insets(10));

            Scene scene = new Scene(root);
            preview.setScene(scene);
            preview.show();

        } catch (Exception ex) {
            ex.printStackTrace();
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

    private void openImageEditor(String imagePath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("image_editor.fxml"));
            Parent root = loader.load();

            ImageEditorController controller = loader.getController();
            controller.setImage(new File(imagePath));

            Stage stage = new Stage();
            stage.setTitle("Image Editor");
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void openLayoutCompose() {
        if (selectedImages.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION, "No images selected.");
            alert.showAndWait();
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("layout_page.fxml"));
            Parent root = loader.load();
            LayoutController controller = loader.getController();
            List<String> paths = selectedImages.stream()
                    .map(stack -> {
                        ImageView iv = (ImageView) stack.getChildren().get(0);
                        String url = iv.getImage().getUrl();
                        return url.replace("file:", "");
                    })
                    .toList();
            controller.setSelectedImages(paths);

            Stage stage = new Stage();
            stage.setTitle("Composite Image");
            stage.setScene(new Scene(root));
            stage.setOnHidden(e -> loadImages());
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
