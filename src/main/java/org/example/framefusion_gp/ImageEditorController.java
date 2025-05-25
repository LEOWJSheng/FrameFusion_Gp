package org.example.framefusion_gp;

import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.image.*;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;

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
    private File imageFile;
    private Image originalImage;
    private Image grayscaleImage;
    private Image borderedImage;
    private final ColorAdjust colorAdjust = new ColorAdjust();

    static {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
    }

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

            double maxScaleW = cw / iw;
            double maxScaleH = ch / ih;
            double fillScale = Math.min(maxScaleW, maxScaleH);

            resizeSlider.setMin(0.1);
            resizeSlider.setMax(fillScale);
            resizeSlider.setValue(fillScale * 0.5);
            resizeSlider.setBlockIncrement(fillScale / 10.0);

            double scale = resizeSlider.getValue();
            imageView.setFitWidth(iw * scale);
            imageView.setFitHeight(ih * scale);

            resizeSlider.valueProperty().addListener((obs, oldV, newV) -> {
                double s = newV.doubleValue();
                imageView.setFitWidth(iw * s);
                imageView.setFitHeight(ih * s);
            });
        });

        borderWidthSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (borderOn) applyBorder();
        });

        borderColorPicker.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (borderOn) applyBorder();
        });
    }

    @FXML
    private void onRotate() {
        imageView.setRotate((imageView.getRotate() + 90) % 360);
    }

    @FXML
    private void onGrayscale() {
        if (!grayscaleOn) {
            grayscaleImage = convertToGrayscale(imageView.getImage());
            imageView.setImage(grayscaleImage);
        } else {
            imageView.setImage(originalImage);
        }
        grayscaleOn = !grayscaleOn;
        if (borderOn) applyBorder();
    }

    @FXML
    private void onAddBorder() {
        if (!borderOn) {
            applyBorder();
            borderOn = true;
        } else {
            imageView.setImage(grayscaleOn ? grayscaleImage : originalImage);
            borderOn = false;
        }
    }

    private Image convertToGrayscale(Image inputImage) {
        BufferedImage buffered = SwingFXUtils.fromFXImage(inputImage, null);
        Mat mat = bufferedImageToMat(buffered);

        Imgproc.cvtColor(mat, mat, Imgproc.COLOR_BGR2GRAY);
        Imgproc.cvtColor(mat, mat, Imgproc.COLOR_GRAY2BGR); // keep 3-channel for FX

        return matToImage(mat);
    }

    private void applyBorder() {
        Image input = grayscaleOn ? grayscaleImage : originalImage;
        BufferedImage buffered = SwingFXUtils.fromFXImage(input, null);
        Mat mat = bufferedImageToMat(buffered);

        int borderWidth = Math.max(1, (int) borderWidthSlider.getValue());
        Color fxColor = borderColorPicker.getValue();
        Scalar borderColor = new Scalar(fxColor.getBlue() * 255, fxColor.getGreen() * 255, fxColor.getRed() * 255);

        Core.copyMakeBorder(mat, mat, borderWidth, borderWidth, borderWidth, borderWidth,
                Core.BORDER_CONSTANT, borderColor);

        Image bordered = matToImage(mat);
        imageView.setImage(bordered);
        borderedImage = bordered;
    }

    @FXML
    private void onSave() {
        WritableImage snapshot = imageView.snapshot(null, null);
        BufferedImage bufferedImage = SwingFXUtils.fromFXImage(snapshot, null);

        try {
            File dir = new File("exported");
            if (!dir.exists()) dir.mkdirs();

            String filename = "edited_" + System.currentTimeMillis() + ".png";
            File outFile = new File(dir, filename);

            ImageIO.write(bufferedImage, "png", outFile);
            String imagePath = outFile.getAbsolutePath();
            Database.insertImage(imagePath, "");

            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Saved");
            alert.setHeaderText("Image saved to gallery.");
            alert.showAndWait();

        } catch (IOException e) {
            e.printStackTrace();
            Alert error = new Alert(Alert.AlertType.ERROR);
            error.setTitle("Error");
            error.setHeaderText("Saving failed");
            error.setContentText(e.getMessage());
            error.showAndWait();
        }
    }

   @FXML
private void onBack() {
    try {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("gallery.fxml"));
        Parent root = loader.load();
        Stage stage = (Stage) imageView.getScene().getWindow();
        stage.setScene(new Scene(root));
    } catch (IOException e) {
        e.printStackTrace();
    }
}


    // Utility: Convert BufferedImage to OpenCV Mat
    private Mat bufferedImageToMat(BufferedImage bi) {
        Mat mat = new Mat(bi.getHeight(), bi.getWidth(), CvType.CV_8UC3);
        int[] data = new int[bi.getWidth() * bi.getHeight()];
        bi.getRGB(0, 0, bi.getWidth(), bi.getHeight(), data, 0, bi.getWidth());

        byte[] bytes = new byte[bi.getWidth() * bi.getHeight() * 3];
        for (int i = 0; i < data.length; i++) {
            int argb = data[i];
            bytes[i * 3]     = (byte) (argb & 0xFF);           // Blue
            bytes[i * 3 + 1] = (byte) ((argb >> 8) & 0xFF);    // Green
            bytes[i * 3 + 2] = (byte) ((argb >> 16) & 0xFF);   // Red
        }
        mat.put(0, 0, bytes);
        return mat;
    }

    // Utility: Convert OpenCV Mat to JavaFX Image
    private Image matToImage(Mat mat) {
        MatOfByte buffer = new MatOfByte();
        Imgcodecs.imencode(".png", mat, buffer);
        return new Image(new java.io.ByteArrayInputStream(buffer.toArray()));
    }
}
