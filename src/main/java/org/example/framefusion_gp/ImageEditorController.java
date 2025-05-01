package org.example.framefusion_gp;

import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.image.*;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class ImageEditorController {

    @FXML private ImageView imageView;
    @FXML private StackPane imageContainer;
    @FXML private Slider brightnessSlider;
    @FXML private Slider contrastSlider;
    @FXML private Slider resizeSlider;
    @FXML private Slider borderWidthSlider;
    @FXML private ColorPicker borderColorPicker;

    private File imageFile;
    private Image originalImage;
    private final ColorAdjust colorAdjust = new ColorAdjust();

    public void setImage(File file) {
        this.imageFile = file;
        this.originalImage = new Image(file.toURI().toString());
        imageView.setImage(originalImage);
        imageView.setPreserveRatio(true);
        setupBindings();
    }

    private void setupBindings() {
        imageView.setEffect(colorAdjust);

        brightnessSlider.valueProperty().addListener((obs, oldVal, newVal) ->
                colorAdjust.setBrightness(newVal.doubleValue()));

        contrastSlider.valueProperty().addListener((obs, oldVal, newVal) ->
                colorAdjust.setContrast(newVal.doubleValue()));

        resizeSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            double scale = newVal.doubleValue();
            imageView.setFitWidth(originalImage.getWidth() * scale);
            imageView.setFitHeight(originalImage.getHeight() * scale);
        });

        borderWidthSlider.valueProperty().addListener((obs, oldVal, newVal) -> updateBorder());
        borderColorPicker.valueProperty().addListener((obs, oldVal, newVal) -> updateBorder());
    }

    private void updateBorder() {
        if (imageContainer == null) return;

        double width = borderWidthSlider.getValue();
        Color color = borderColorPicker.getValue();

        String style = String.format(
                "-fx-border-color: rgba(%d,%d,%d,1); -fx-border-width: %.1f; -fx-border-style: solid;",
                (int) (color.getRed() * 255),
                (int) (color.getGreen() * 255),
                (int) (color.getBlue() * 255),
                width
        );

        imageContainer.setStyle(style);
    }

    @FXML
    private void onRotate() {
        imageView.setRotate((imageView.getRotate() + 90) % 360);
    }

    @FXML
    private void onGrayscale() {
        Image image = imageView.getImage();
        int width = (int) image.getWidth();
        int height = (int) image.getHeight();
        WritableImage grayImage = new WritableImage(width, height);
        PixelReader reader = image.getPixelReader();
        PixelWriter writer = grayImage.getPixelWriter();

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                Color color = reader.getColor(x, y);
                double gray = (color.getRed() + color.getGreen() + color.getBlue()) / 3.0;
                writer.setColor(x, y, new Color(gray, gray, gray, color.getOpacity()));
            }
        }

        imageView.setImage(grayImage);
    }

    @FXML
    private void onAddBorder() {
        updateBorder();
    }

    @FXML
    private void onSave() {
        WritableImage snapshot = imageView.snapshot(null, null);
        BufferedImage bufferedImage = SwingFXUtils.fromFXImage(snapshot, null);

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Image");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("PNG Files", "*.png")
        );
        File saveFile = fileChooser.showSaveDialog(imageView.getScene().getWindow());

        if (saveFile != null) {
            try {
                ImageIO.write(bufferedImage, "png", saveFile);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @FXML
    private void onBack() {
        Stage stage = (Stage) imageView.getScene().getWindow();
        stage.close();
    }
}
