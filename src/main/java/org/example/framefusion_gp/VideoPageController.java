package org.example.framefusion_gp;

import javafx.animation.PauseTransition;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.*;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.File;
import java.io.IOException;
import java.util.List;
import javafx.scene.image.Image;

public class VideoPageController {
    private static final int IMAGE_SIZE = 130;
    @FXML private FlowPane videoFlowPane;
    @FXML private Label emptyVidLabel;
    @FXML public Button HomeBtn;

    @FXML
    public void initialize() {
        videoFlowPane.setPadding(new Insets(20));
        videoFlowPane.setHgap(10);
        videoFlowPane.setVgap(10);
        HomeBtn.setOnAction(e -> goHome());
        PauseTransition delay = new PauseTransition(Duration.millis(300));
        delay.setOnFinished(evt -> refresh());
        delay.play();
    }

    private void goHome() {
        try {
            Parent homeRoot = FXMLLoader.load(getClass().getResource("home_page.fxml"));
            Stage stage = (Stage) HomeBtn.getScene().getWindow();
            stage.setScene(new Scene(homeRoot));
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

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
            videoFlowPane.getChildren().add(createThumbnailCell(file));
        }
    }

    private StackPane createThumbnailCell(File file) {
        StackPane stack = new StackPane();
        stack.setPrefSize(IMAGE_SIZE, IMAGE_SIZE);
        stack.getStyleClass().add("image-cell");

        MediaPlayer thumbPlayer = new MediaPlayer(new Media(file.toURI().toString()));
        MediaView thumbView = new MediaView(thumbPlayer);
        thumbView.setPreserveRatio(true);
        thumbView.setFitWidth(IMAGE_SIZE);
        //thumbView.setFitHeight(IMAGE_SIZE);
        thumbPlayer.setOnReady(thumbPlayer::pause);

        stack.getChildren().add(thumbView);
        StackPane.setAlignment(thumbView, Pos.CENTER);
        FlowPane.setMargin(stack, new Insets(10));

        stack.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.PRIMARY) {
                showPopup(file);
            }
        });

        MenuButton overflow = new MenuButton("⋮");
        overflow.setStyle("-fx-background-color: transparent; -fx-font-size: 14px;");
        StackPane.setAlignment(overflow, Pos.TOP_RIGHT);
        MenuItem deleteItem = new MenuItem("Delete");
        deleteItem.setOnAction(e -> {
            Database.deleteVideo(file.getAbsolutePath());
            refresh();
        });
        overflow.getItems().add(deleteItem);
        stack.getChildren().add(overflow);
        return stack;
    }

    private void showPopup(File videoFile) {
        Media media = new Media(videoFile.toURI().toString());
        MediaPlayer player = new MediaPlayer(media);
        player.setAutoPlay(false);

        MediaView viewer = new MediaView(player);
        viewer.setPreserveRatio(true);
        viewer.setFitWidth(640);
        viewer.setFitHeight(360);

        StackPane videoContainer = new StackPane(viewer);
        videoContainer.getStyleClass().add("placeholder");
        videoContainer.setPrefSize(640, 360);
        videoContainer.setMaxSize(640, 360);
        videoContainer.setPadding(new Insets(8));

        Slider slider = new Slider();
        slider.setMin(0);
        slider.setMinWidth(600);
        player.setOnReady(() -> {
            slider.setMax(player.getTotalDuration().toSeconds());
            player.pause();
        });
        player.currentTimeProperty().addListener((obs, old, now) -> {
            if (!slider.isValueChanging()) slider.setValue(now.toSeconds());
        });
        slider.valueChangingProperty().addListener((obs, was, changing) -> {
            if (!changing) player.seek(Duration.seconds(slider.getValue()));
        });

        Button play = new Button("Play");
        play.setOnAction(e -> player.play());
        Button pause = new Button("Pause");
        pause.setOnAction(e -> player.pause());
        Button stop = new Button("Stop");
        stop.setOnAction(e -> {
            player.stop();
            player.seek(Duration.ZERO);
        });
        HBox controls = new HBox(10, play, pause, stop);
        controls.setAlignment(Pos.CENTER);
        controls.setPadding(new Insets(5, 0, 0, 0));

        VBox content = new VBox(10, videoContainer, slider, controls);
        content.setPadding(new Insets(15));

        // Use a StackPane as the popup root so we can style the entire background
        StackPane popupRoot = new StackPane(content);
        popupRoot.getStyleClass().add("blackBg");

        Scene popupScene = new Scene(popupRoot);
        popupScene.getStylesheets().add(
                getClass().getResource("gallery.css").toExternalForm()
        );

        Stage popup = new Stage();
        popup.setTitle("Preview: " + videoFile.getName());
        popup.getIcons().add(new Image(getClass().getResourceAsStream("frameFusion_icon.png")));
        popup.setScene(popupScene);
        popup.show();
    }
}
