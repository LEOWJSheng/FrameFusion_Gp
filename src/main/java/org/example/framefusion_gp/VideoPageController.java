package org.example.framefusion_gp;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class VideoPageController {
    private static final int IMAGE_SIZE = 130;
    @FXML private FlowPane videoFlowPane;
    @FXML private Label emptyVidLabel;
    @FXML public Button homeBtn;

    @FXML
    public void initialize() {
        videoFlowPane.setPadding(new Insets(20));
        videoFlowPane.setHgap(10);
        videoFlowPane.setVgap(10);
        homeBtn.setOnAction(e -> goHome());
        refresh();
    }

    public void goHome() {
        try {
            Parent homeRoot = FXMLLoader.load(getClass().getResource("home_page.fxml"));
            Stage stage = (Stage) homeBtn.getScene().getWindow();
            stage.setScene(new Scene(homeRoot));
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    /** Populate the gallery from the DB */
    public void refresh() {
        videoFlowPane.getChildren().clear();
        List<Database.VideoData> videos = Database.getVideos();

        if (videos.isEmpty()) {
            videoFlowPane.getChildren().add(emptyVidLabel);
            return;
        }

        for (Database.VideoData vd : videos) {
            File file = new File(vd.path);
            if (!file.exists()) continue;

            // 1) Container exactly like your image cells:
            StackPane stack = new StackPane();
            stack.setPrefSize(IMAGE_SIZE, IMAGE_SIZE);
            stack.setMinSize(IMAGE_SIZE, IMAGE_SIZE);
            stack.setMaxSize(IMAGE_SIZE, IMAGE_SIZE);
            stack.getStyleClass().add("image-cell");


            // 2) Create a MediaView & size it like an ImageView
            Media media = new Media(file.toURI().toString());
            MediaPlayer player = new MediaPlayer(media);
            MediaView thumb = new MediaView(player);
            thumb.setPreserveRatio(true);
            thumb.setFitWidth(IMAGE_SIZE);
            thumb.setFitHeight(IMAGE_SIZE);

            // pause on first frame so it shows immediately as a thumbnail
            player.setOnReady(player::pause);

            stack.getChildren().add(thumb);
            StackPane.setAlignment(thumb, Pos.CENTER);

            // 3) Click the cell to open your full‐size viewer
            stack.setOnMouseClicked(e -> viewVideo(file));
            FlowPane.setMargin(stack, new Insets(10));
            videoFlowPane.getChildren().add(stack);
        }
    }

    /**
     * Opens a new window with a larger MediaView and basic controls
     */
    public void viewVideo(File videoFile) {
        // 1) create Media and MediaPlayer
        Media media = new Media(videoFile.toURI().toString());
        MediaPlayer player = new MediaPlayer(media);
        player.setAutoPlay(false);

        // 2) create a MediaView for the video
        MediaView viewer = new MediaView(player);
        viewer.setPreserveRatio(true);
        viewer.setFitWidth(640);
        viewer.setFitHeight(360);

        // 3) create the time slider
        Slider timeSlider = new Slider(0, 100, 0);
        timeSlider.setMinWidth(600);
        // once media is ready, set slider max to its duration
        player.setOnReady(() -> {
            timeSlider.setMax(media.getDuration().toSeconds());
            player.pause();
        });

        // update slider as video plays
        player.currentTimeProperty().addListener((obs, oldTime, newTime) -> {
            if (!timeSlider.isValueChanging()) {
                timeSlider.setValue(newTime.toSeconds());
            }
        });

        // when user finishes dragging, seek to that time
        timeSlider.valueChangingProperty().addListener((obs, wasChanging, isChanging) -> {
            if (!isChanging) {
                player.seek(Duration.seconds(timeSlider.getValue()));
            }
        });

        // also allow scrubbing mid-drag
        timeSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (timeSlider.isValueChanging()) {
                player.seek(Duration.seconds(newVal.doubleValue()));
            }
        });

        // 4) pause button under the slider
        Button pauseButton = new Button("Pause");
        pauseButton.setOnAction(e -> player.pause());
        HBox pauseBox = new HBox(pauseButton);
        pauseBox.setAlignment(Pos.CENTER);

        // 5) play and stop buttons
        Button playButton = new Button("Play");
        playButton.setOnAction(e -> player.play());
        Button stopButton = new Button("Stop");
        stopButton.setOnAction(e -> {
            player.stop();
            player.seek(Duration.ZERO);
        });
        HBox controlBar = new HBox(10, playButton, stopButton);
        controlBar.setAlignment(Pos.CENTER);
        controlBar.setPadding(new Insets(5, 0, 0, 0));

        // 6) assemble everything in a VBox
        VBox root = new VBox(10,
                viewer,
                timeSlider,
                pauseBox,
                controlBar
        );
        root.setPadding(new Insets(15));

        // 7) show in a new Stage
        Stage stage = new Stage();
        stage.setTitle("Preview: " + videoFile.getName());
        stage.setScene(new Scene(root));
        stage.show();
    }

}
