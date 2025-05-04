package org.example.framefusion_gp;

import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.geometry.Insets;
import javafx.geometry.HPos;
import javafx.geometry.VPos;
import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.util.List;

public class LayoutController {

    @FXML
    private ComboBox<String> layoutSelector;
    @FXML
    private GridPane imageGrid;
    @FXML
    private Button saveBtn;

    private List<String> selectedImagePaths;

    @FXML
    public void initialize() {
        // Disable until images are set
        layoutSelector.setDisable(true);
        saveBtn.setDisable(true);
    }

    public void setSelectedImages(List<String> imagePaths) {
        this.selectedImagePaths = imagePaths;
        int size = imagePaths.size();

        // Restrict selection between 2 and 6
        if (size < 2 || size > 6) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Selection Error");
            alert.setHeaderText("Invalid number of images selected");
            alert.setContentText("Please select between 2 and 6 images.");
            alert.showAndWait();
            layoutSelector.setDisable(true);
            saveBtn.setDisable(true);
            return;
        }

        // Populate layout options based on number of images
        layoutSelector.getItems().clear();
        if (size == 2) {
            layoutSelector.getItems().addAll("Select layout", "2 Horizontal", "2 Vertical");
        } else if (size == 3) {
            layoutSelector.getItems().addAll("Select layout", "3 Horizontal", "3 Vertical");
        } else if (size == 4) {
            layoutSelector.getItems().addAll("Select layout", "2x2 Grid", "4 Horizontal");
        } else if (size == 5) {
            layoutSelector.getItems().addAll("Select layout", "5 Vertical");
        } else {
            layoutSelector.getItems().addAll("Select layout", "3*3 Grid");
        }
        layoutSelector.getSelectionModel().selectFirst();
        layoutSelector.setDisable(false);
        saveBtn.setDisable(false);
    }

    @FXML
    private void onLayoutSelected() {
        String layout = layoutSelector.getValue();
        imageGrid.getChildren().clear();

        switch (layout) {
            case "3*3 Grid":
                buildGrid(3, 3);
                break;
            case "2x2 Grid":
                buildGrid(2, 2);
                break;
            case "4 Horizontal":
                buildGrid(1, 4);
                break;
            case "3 Horizontal":
                buildGrid(1, 3);
                break;
            case "2 Horizontal":
                buildGrid(1, 2);
                break;
            case "5 Vertical":
                buildGrid(5, 1);
                break;
            case "3 Vertical":
                buildGrid(3, 1);
                break;
            case "2 Vertical":
                buildGrid(2, 1);
                break;
            default:
                break;
        }
    }

    private void buildGrid(int rows, int cols) {
        int index = 0;
        for (int row = 0; row < rows && index < selectedImagePaths.size(); row++) {
            for (int col = 0; col < cols && index < selectedImagePaths.size(); col++) {
                imageGrid.add(createImageView(selectedImagePaths.get(index++)), col, row);
            }
        }
    }

    private StackPane createImageView(String path) {
        File file = new File(path);
        if (!file.exists()) {
            return new StackPane();
        }

        Image img = new Image(file.toURI().toString());
        ImageView view = new ImageView(img);

        // No aspect-ratio lock → image can stretch to fill
        view.setPreserveRatio(false);
        view.setSmooth(true);

        // Initial image size
        view.setFitWidth(300);
        view.setFitHeight(300);

        // Wrapper acts as fixed border/frame
        double BORDER_SIZE = 400;
        double PADDING = 10;
        StackPane wrapper = new StackPane(view);
        wrapper.setPrefSize(BORDER_SIZE, BORDER_SIZE);
        wrapper.setMinSize(BORDER_SIZE, BORDER_SIZE);
        wrapper.setMaxSize(BORDER_SIZE, BORDER_SIZE);
        wrapper.setPadding(new Insets(PADDING));
        wrapper.setStyle("-fx-border-color: white; -fx-border-width: 2;");

        GridPane.setHalignment(wrapper, HPos.CENTER);
        GridPane.setValignment(wrapper, VPos.CENTER);

        // Drag to resize image inside fixed frame
        wrapper.addEventFilter(MouseEvent.MOUSE_DRAGGED, e -> {
            double x = e.getX() - PADDING;
            double y = e.getY() - PADDING;
            double maxW = BORDER_SIZE - 2 * PADDING;
            double maxH = BORDER_SIZE - 2 * PADDING;

            double newW = Math.max(50, Math.min(x, maxW));
            double newH = Math.max(50, Math.min(y, maxH));

            view.setFitWidth(newW);
            view.setFitHeight(newH);
        });

        return wrapper;
    }

    @FXML
    private void saveLayout() {
        if (imageGrid.getChildren().isEmpty()) {
            return;
        }

        SnapshotParameters params = new SnapshotParameters();
        params.setFill(Color.TRANSPARENT);
        WritableImage snapshot = imageGrid.snapshot(params, null);

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Composite Layout");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PNG Image", "*.png"));
        File file = fileChooser.showSaveDialog(imageGrid.getScene().getWindow());

        if (file != null) {
            try {
                ImageIO.write(SwingFXUtils.fromFXImage(snapshot, null), "png", file);
                Database.insertImage(file.getAbsolutePath(), ""); // optional DB insert
                ((Stage) imageGrid.getScene().getWindow()).close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
