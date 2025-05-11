package org.example.framefusion_gp;

import javafx.application.Platform;
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
    private boolean grayscaleOn = false;
    private boolean borderOn = false;
    private WritableImage grayImage;
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

        Platform.runLater(() -> {
            double cw = imageContainer.getWidth();
            double ch = imageContainer.getHeight();
            double iw = originalImage.getWidth();
            double ih = originalImage.getHeight();

            // compute the "fill" scale
            double maxScaleW = cw / iw;
            double maxScaleH = ch / ih;
            double fillScale = Math.min(maxScaleW, maxScaleH);

            // configure slider: 0.1× up to fillScale, start at fillScale
            resizeSlider.setMin(0.1);
            resizeSlider.setMax(fillScale);
            resizeSlider.setValue(fillScale);
            resizeSlider.setBlockIncrement(fillScale / 10.0);

            // now listen for user‐driven changes
            resizeSlider.valueProperty().addListener((obs, oldV, newV) -> {
                double s = newV.doubleValue();
                imageView.setFitWidth(iw * s);
                imageView.setFitHeight(ih * s);
            });
            double startScale = fillScale * 0.5;
            resizeSlider.setValue(startScale);
            imageView.setFitWidth(iw * startScale);
            imageView.setFitHeight(ih * startScale);
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
        if (!grayscaleOn) {
            Image img = imageView.getImage();
            int w = (int) img.getWidth(), h = (int) img.getHeight();
            grayImage = new WritableImage(w, h);
            PixelReader reader = img.getPixelReader();
            PixelWriter writer = grayImage.getPixelWriter();
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    Color c = reader.getColor(x, y);
                    double g = (c.getRed() + c.getGreen() + c.getBlue()) / 3.0;
                    writer.setColor(x, y, new Color(g, g, g, c.getOpacity()));
                }
            }
            imageView.setImage(grayImage);
        } else {
            imageView.setImage(originalImage);
        }
        grayscaleOn = !grayscaleOn;
    }

    @FXML
    private void onAddBorder() {
        if (!borderOn) {
            updateBorder();
        } else {
            imageContainer.setStyle("");
        }
        borderOn = !borderOn;
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