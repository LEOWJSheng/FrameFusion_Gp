package org.example.framefusion_gp;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.geometry.Side;
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
import java.net.URL;
import java.sql.SQLException;
import java.util.*;

public class GalleryController {

    @FXML private Button HomeBtn;
    @FXML private Button addImageBtn;
    @FXML private FlowPane galleryFlowPane;
    @FXML private ScrollPane scrollPane;
    @FXML private Label emptyImgLabel;
    @FXML private Button makeVideoBtn;
    @FXML private Button composeLayoutBtn;

    private static final int IMAGE_SIZE = 120;
    private final Set<StackPane> selectedImages = new HashSet<>();

    public void initialize() {
        galleryFlowPane.setPadding(new Insets(20));
        addImageBtn.setOnAction(e -> uploadImage());
        loadImages();
        HomeBtn.setOnAction(e -> goToHome());
        composeLayoutBtn.setDisable(true);
        composeLayoutBtn.setOnAction(e -> openLayoutCompose());
        makeVideoBtn.setDisable(true);
        makeVideoBtn.setOnAction(e -> makeVideo());
    }

    public void loadImages() {
        galleryFlowPane.getChildren().clear();
        List<ImageData> images = Database.getImages();

        if (images.isEmpty()) {
            galleryFlowPane.getChildren().add(emptyImgLabel);
            return;
        }

        for (ImageData data : images) {
            File imgFile = new File(data.getPath());
            if (!imgFile.exists()) continue;

            StackPane stack = new StackPane();
            stack.setPrefSize(130, 130);
            stack.getStyleClass().add("image-cell");

            ImageView view = new ImageView(new Image("file:" + data.getPath()));
            view.setPreserveRatio(true);
            view.setFitWidth(IMAGE_SIZE);
            view.setFitHeight(IMAGE_SIZE);
            stack.getChildren().add(view);
            StackPane.setAlignment(view, Pos.CENTER);

            if (data.getAnnotation() != null && !data.getAnnotation().isEmpty()) {
                Label heart = new Label("♥");
                heart.getStyleClass().add("heart-label");
                StackPane.setAlignment(heart, Pos.TOP_RIGHT);
                stack.getChildren().add(heart);
            }

            Button optionsButton = new Button();
            URL iconUrl = getClass().getResource("/icons/menu.png");
            if (iconUrl != null) {
                ImageView iconView = new ImageView(new Image(iconUrl.toExternalForm()));
                iconView.setFitWidth(16);
                iconView.setFitHeight(16);
                optionsButton.setGraphic(iconView);
            } else {
                optionsButton.setText("⋮");
                System.out.println("Hamburger icon not found.");
            }

            optionsButton.setStyle("-fx-background-color: transparent;");
            StackPane.setAlignment(optionsButton, Pos.TOP_LEFT);
            ContextMenu contextMenu = new ContextMenu();
            MenuItem annotateItem = new MenuItem("Edit Annotation");
            MenuItem editItem = new MenuItem("Open Editor");
            MenuItem deleteItem = new MenuItem("Delete");

            deleteItem.setOnAction(e -> {
                Database.deleteImage(data.getPath());
                loadImages();
            });

            annotateItem.setOnAction(e -> {
                try {
                    openAnnotationEditor(data);
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            });

            editItem.setOnAction(e -> openImageEditor(data.getPath()));
            contextMenu.getItems().addAll(annotateItem, editItem, deleteItem);
            optionsButton.setOnAction(e -> {
                if (!contextMenu.isShowing()) {
                    contextMenu.show(optionsButton, Side.BOTTOM, 0, 0);
                } else {
                    contextMenu.hide();
                }
            });

            stack.getChildren().add(optionsButton);

            stack.setOnMouseClicked(e -> {
                if (e.getTarget() instanceof ImageView) {
                    if (e.isControlDown()) {
                        if (selectedImages.contains(stack)) {
                            stack.setStyle("");
                            selectedImages.remove(stack);
                        } else {
                            stack.setStyle("-fx-border-color: red; -fx-border-width: 2;");
                            selectedImages.add(stack);
                        }
                        makeVideoBtn.setDisable(selectedImages.size() <= 1);
                        composeLayoutBtn.setDisable(selectedImages.size() <= 1);
                    } else {
                        showImagePreview(data);
                    }
                }
            });

            galleryFlowPane.getChildren().add(stack);
        }
    }

    private void showImagePreview(ImageData imageData) {
        try {
            Stage preview = new Stage();
            preview.setTitle("Preview");

            ImageView iv = new ImageView(new Image("file:" + imageData.getPath()));
            iv.setPreserveRatio(true);
            iv.setSmooth(true);

            Rectangle2D bounds = Screen.getPrimary().getVisualBounds();
            iv.setFitWidth(bounds.getWidth() * 0.8);
            iv.setFitHeight(bounds.getHeight() * 0.8);

            VBox contentBox = new VBox(10);
            contentBox.setAlignment(Pos.CENTER);
            contentBox.setPadding(new Insets(20));
            contentBox.setStyle("-fx-background-color: black");

            contentBox.getChildren().add(iv);

            if (imageData.getAnnotation() != null && !imageData.getAnnotation().isEmpty()) {
                Label annotationLabel = new Label(imageData.getAnnotation());
                annotationLabel.setStyle("-fx-text-fill: white; -fx-font-size: 16px;");
                annotationLabel.setWrapText(true);
                annotationLabel.setMaxWidth(bounds.getWidth() * 0.7);
                contentBox.getChildren().add(annotationLabel);
            }

            Scene scene = new Scene(contentBox);
            preview.setScene(scene);
            preview.getIcons().add(new Image(getClass().getResourceAsStream("frameFusion_icon.png")));
            preview.show();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void openAnnotationEditor(ImageData data) throws SQLException {
        TextInputDialog dialog = new TextInputDialog(data.getAnnotation());
        dialog.setHeaderText("Edit annotation");
        dialog.setContentText("Annotation:");
        Optional<String> result = dialog.showAndWait();
        if (result.isPresent()) {
            Database.updateAnnotation(data.getId(), result.get());
            loadImages();
        }
    }

private void openImageEditor(String imagePath) {
    try {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("image_editor.fxml"));
        Parent root = loader.load();
        ImageEditorController controller = loader.getController();
        controller.setImage(new File(imagePath));

        Stage stage = (Stage) galleryFlowPane.getScene().getWindow(); // Get current window
        stage.setTitle("Image Editor");
        stage.getIcons().add(new Image(getClass().getResourceAsStream("frameFusion_icon.png")));
        stage.setScene(new Scene(root));
        stage.setOnHidden(e -> loadImages()); // Reload gallery when editor closes (optional if you return)

    } catch (IOException e) {
        e.printStackTrace();
    }
}


    public void uploadImage() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Upload Image");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg"));
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

    public void openLayoutCompose() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("layout_page.fxml"));
            Parent root = loader.load();
            LayoutController controller = loader.getController();
            List<String> paths = selectedImages.stream()
                    .map(stack -> ((ImageView) stack.getChildren().get(0)).getImage().getUrl().replace("file:", ""))
                    .toList();
            controller.setSelectedImages(paths);
            Stage stage = (Stage) composeLayoutBtn.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setOnHidden(e -> {
                selectedImages.clear();
                composeLayoutBtn.setDisable(true);
                makeVideoBtn.setDisable(true);
                loadImages();
            });
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void makeVideo() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("videoController.fxml"));
            Parent videoRoot = loader.load();
            VideoController videoController = loader.getController();

            List<File> selectedFiles = new ArrayList<>();
            for (StackPane pane : selectedImages) {
                ImageView imageView = (ImageView) pane.getChildren().get(0);
                String url = imageView.getImage().getUrl().replace("file:", "");
                selectedFiles.add(new File(url));
            }

            videoController.importImages(selectedFiles);
            Stage stage = (Stage) makeVideoBtn.getScene().getWindow();
            stage.setScene(new Scene(videoRoot));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void goToHome() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("home_page.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) HomeBtn.getScene().getWindow();
            stage.setScene(new Scene(root));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
