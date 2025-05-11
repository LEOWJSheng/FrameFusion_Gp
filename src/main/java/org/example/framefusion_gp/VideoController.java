package org.example.framefusion_gp;

import javafx.application.Platform;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.image.*;
import javafx.scene.shape.Rectangle;
import javafx.scene.input.*;
import javafx.scene.text.*;
import javafx.scene.*;
import javafx.fxml.*;
import javafx.stage.*;
import javafx.geometry.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.WritableImage;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.util.concurrent.CountDownLatch;


public class VideoController {
    @FXML private ImageView bgImageView;
    @FXML private ToggleButton importBtn;
    @FXML private ToggleButton textBtn;
    @FXML private ToggleButton graphicsBtn;
    @FXML private ScrollPane scrollPanel;
    @FXML public TilePane tilePanel;
    @FXML private StackPane mediaPlayerPane;
    @FXML private AnchorPane overlayPane;
    @FXML public HBox videoTrack;
    @FXML public HBox textTrack;
    @FXML private Rectangle timelineTrack;
    @FXML private Rectangle playhead;
    @FXML public Button discardBtn;
    @FXML public Button exportVideoBtn;
    @FXML public TextField videoName;

    private static final int PHOTO_DURATION_SECONDS = 5;
    private List<StackPane> importedImageTiles = new ArrayList<>();
    private List<StackPane> textTiles = new ArrayList<>();
    private List<StackPane> graphicsTiles = new ArrayList<>();

    // for scrubbing through frames
    private static class MediaSegment {
        Image image;
        double startTime;
        MediaSegment(Image img, double t) { image = img; startTime = t; }
    }
    private List<MediaSegment> segments = new ArrayList<>();
    private double totalDuration = 0.0;

    public void initialize() {
        setDiscardBtn();
        importBtn.setSelected(true);
        showImportedImages();

        importBtn.setOnAction(e -> {
            importBtn.setSelected(true); textBtn.setSelected(false); graphicsBtn.setSelected(false);
            showImportedImages();
        });

        textBtn.setOnAction(e -> {
            importBtn.setSelected(false); textBtn.setSelected(true); graphicsBtn.setSelected(false);
            loadTextOptions();
        });

        graphicsBtn.setOnAction(e -> {
            importBtn.setSelected(false); textBtn.setSelected(false); graphicsBtn.setSelected(true);
            loadGraphicsOptions();
        });

        bgImageView.imageProperty().addListener((o,old,newImg) -> centerBgImage());
        mediaPlayerPane.widthProperty().addListener((o,old,newW) -> centerBgImage());
        mediaPlayerPane.heightProperty().addListener((o,old,newH) -> centerBgImage());

        setupDragAndDropOnVideoTrack();
        setupOverlayDropTargets();
        setupPlayheadDragging();
        mediaPlayerPane.setPickOnBounds(true);

        exportVideoBtn.setDisable(true);
        videoName.textProperty().addListener((obs, oldText, newText) ->
                exportVideoBtn.setDisable(newText.trim().isEmpty())
        );
        exportVideoBtn.setOnAction(e -> onExport());
    }

    public void importImages(List<File> imageFiles) {
        importedImageTiles.clear();
        for (File file : imageFiles) {
            Image img = new Image(file.toURI().toString());
            ImageView iv = new ImageView(img);
            iv.setFitWidth(120); iv.setPreserveRatio(true);
            StackPane cell = new StackPane(iv);
            cell.setPadding(new Insets(5));
            cell.setOnDragDetected(ev -> {
                Dragboard db = cell.startDragAndDrop(TransferMode.COPY);
                ClipboardContent cc = new ClipboardContent(); cc.putString("photo"); db.setContent(cc);
                ev.consume();
            });
            importedImageTiles.add(cell);
        }
        showImportedImages();
    }

    private void showImportedImages() {
        tilePanel.getChildren().setAll(importedImageTiles);
    }

    public void loadTextOptions() {
        if (textTiles.isEmpty()) {
            // Show one “Sample” label for each font
            String[] fonts = { "Arial", "Verdana", "Courier New", "Comic Sans MS" };
            for (String fontName : fonts) {
                Label sample = new Label("Sample");
                sample.setFont(Font.font(fontName, 20));
                sample.setStyle("-fx-background-color: white; -fx-border-color: black; -fx-padding: 5;");
                StackPane cell = new StackPane(sample);
                cell.setPadding(new Insets(5));

                // On click: ask the user for their text, then add an overlay using that font
                cell.setOnDragDetected(ev -> {
                    Dragboard db = cell.startDragAndDrop(TransferMode.COPY);
                    ClipboardContent cc = new ClipboardContent();
                    cc.putString(fontName + "|" + sample.getText());
                    db.setContent(cc);
                    ev.consume();
                });
                textTiles.add(cell);
            }
        }
        tilePanel.getChildren().setAll(textTiles);
    }

    public void loadGraphicsOptions() {
        if (graphicsTiles.isEmpty()) {
            String[] files = { "star.png", "arrow.png", "circle.png" };
            for (String fn : files) {
                Image img = new Image(getClass().getResourceAsStream(fn));
                ImageView preview = new ImageView(img);
                preview.setFitWidth(60);
                preview.setPreserveRatio(true);
                StackPane cell = new StackPane(preview);
                cell.setPadding(new Insets(5));

                cell.setOnMouseClicked(ev -> {
                    // Center the graphic in the mediaPlayerPane initially
                    double centerX = mediaPlayerPane.getWidth() / 2;
                    double centerY = mediaPlayerPane.getHeight() / 2;
                    addGraphicOverlay(img, centerX, centerY);
                });
                graphicsTiles.add(cell);
            }
        }
        tilePanel.getChildren().setAll(graphicsTiles);
    }

    private void setupDragAndDropOnVideoTrack() {
        videoTrack.setOnDragOver(ev -> {
            if ("photo".equals(ev.getDragboard().getString())) {
                ev.acceptTransferModes(TransferMode.COPY);
            }
            ev.consume();
        });

        videoTrack.setOnDragDropped(ev -> {
            String payload = ev.getDragboard().getString();
            if (!"photo".equals(payload)) {
                ev.setDropCompleted(false);
                ev.consume();
                return;
            }

            // 1) Add thumbnail
            StackPane src = (StackPane) ev.getGestureSource();
            Image img = ((ImageView) src.getChildren().get(0)).getImage();
            ImageView thumb = new ImageView(img);
            thumb.setFitHeight(60);
            thumb.setPreserveRatio(true);
            HBox.setMargin(thumb, new Insets(2));
            videoTrack.getChildren().add(thumb);
            int idx = videoTrack.getChildren().size() - 1;

            // 2) Record segment
            segments.add(new MediaSegment(img, totalDuration));
            totalDuration += PHOTO_DURATION_SECONDS;

            // 3) Swap in the new background image…
            bgImageView.setImage(img);
            // 4) …and re-center it in the pane:
            centerBgImage();

            // 5) Double-click removal
            bgImageView.setOnMouseClicked(me -> {
                if (me.getClickCount() == 2) {
                    mediaPlayerPane.getChildren().remove(bgImageView);
                    if (idx < videoTrack.getChildren().size()) {
                        videoTrack.getChildren().remove(idx);
                        segments.remove(idx);
                    }
                    playhead.setTranslateX(0);
                    updatePreviewAtTime(0);
                }
            });

            // 6) Reset scrubber & preview
            playhead.setTranslateX(0);
            updatePreviewAtTime(0);

            ev.setDropCompleted(true);
            ev.consume();
        });
    }

    private void centerBgImage() {
        Image img = bgImageView.getImage();
        if (img == null) return;
        bgImageView.applyCss();
        mediaPlayerPane.applyCss();
        mediaPlayerPane.layout();
        Bounds ivBounds = bgImageView.getBoundsInParent();
        double iw = ivBounds.getWidth();
        double ih = ivBounds.getHeight();
        double pw = mediaPlayerPane.getWidth();
        double ph = mediaPlayerPane.getHeight();
        bgImageView.setLayoutX((pw - iw) / 2);
        bgImageView.setLayoutY((ph - ih) / 2);
    }


    private void setupOverlayDropTargets() {
        textTrack.setOnDragOver(ev -> {
            String payload = ev.getDragboard().getString();
            if (payload != null && payload.contains("|")) {
                ev.acceptTransferModes(TransferMode.COPY);
            }
            ev.consume();
        });
        textTrack.setOnDragDropped(ev -> {
            String payload = ev.getDragboard().getString();
            String[] parts = payload != null ? payload.split("\\|", 2) : new String[0];
            if (parts.length < 2) {
                ev.setDropCompleted(false);
                ev.consume();
                return;
            }
            String fontName    = parts[0];
            String initialText = parts[1];

            TextInputDialog dlg = new TextInputDialog(initialText);
            dlg.setHeaderText("Enter caption text:");
            dlg.showAndWait().ifPresent(finalText -> addTextOverlay(finalText, fontName));

            ev.setDropCompleted(true);
            ev.consume();
        });
    }

    private void addTextOverlay(String text, String fontName) {
        Label lbl = new Label(text);
        lbl.setStyle("-fx-background-color: rgba(156,156,156,0.8); -fx-padding:5;");
        lbl.setFont(Font.font(fontName, 14));
        makeNodeDraggable(lbl);

        // 1) build the context menu
        ContextMenu menu = new ContextMenu();
        MenuItem editItem   = new MenuItem("Edit");
        MenuItem deleteItem = new MenuItem("Delete");

        editItem.setOnAction(evt -> {
            TextInputDialog dlg = new TextInputDialog(lbl.getText());
            dlg.setHeaderText("Edit Caption");
            dlg.showAndWait()
                    .filter(Objects::nonNull)
                    .ifPresent(lbl::setText);
        });

        deleteItem.setOnAction(evt -> {
            mediaPlayerPane.getChildren().remove(lbl);
        });

        menu.getItems().addAll(editItem, deleteItem);

        // 2) show it on right-click
        lbl.setOnContextMenuRequested(evt -> {
            menu.show(lbl, evt.getScreenX(), evt.getScreenY());
            evt.consume();
        });

        lbl.setOnMousePressed(evt -> {
            if (evt.getButton() == MouseButton.SECONDARY) {
                evt.consume();
            }
        });

        overlayPane.getChildren().add(lbl);

        // center at bottom after layout
        Platform.runLater(() -> {
            double w = lbl.getBoundsInParent().getWidth();
            double h = lbl.getBoundsInParent().getHeight();
            double x = (mediaPlayerPane.getWidth() - w) / 2;
            double y = mediaPlayerPane.getHeight() - h - 20;
            lbl.setLayoutX(x);
            lbl.setLayoutY(y);
        });
    }

    private void addGraphicOverlay(Image img, double x, double y) {
        ImageView iv = new ImageView(img);
        iv.setFitWidth(80);
        iv.setPreserveRatio(true);
        iv.applyCss();

        // center the image on the drop point
        double w = iv.getBoundsInParent().getWidth();
        double h = iv.getBoundsInParent().getHeight();
        iv.setLayoutX(x - w/2);
        iv.setLayoutY(y - h/2);
        makeNodeDraggable(iv);
        iv.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                mediaPlayerPane.getChildren().remove(iv);
            }
        });
        overlayPane.getChildren().add(iv);
    }

    private void setupPlayheadDragging() {
        playhead.setOnMouseDragged(e -> {
            double trackX = timelineTrack.getLayoutX(), trackW = timelineTrack.getWidth(), half = playhead.getWidth()/2;
            double newX = e.getSceneX() - trackX - half;
            newX = Math.max(0, Math.min(newX, trackW - playhead.getWidth()));
            playhead.setTranslateX(newX);
            double pct = newX / (trackW - playhead.getWidth());
            updatePreviewAtTime(pct * totalDuration);
            e.consume();
        });
    }

    private void updatePreviewAtTime(double time) {
        for (MediaSegment seg : segments) {
            if (time >= seg.startTime && time < seg.startTime + PHOTO_DURATION_SECONDS) {
                if (bgImageView != null) {
                    bgImageView.setImage(seg.image);
                }
                break;
            }
        }
    }

    private void onExport() {
        String name = videoName.getText().trim();
        FileChooser fc = new FileChooser();
        fc.setTitle("Save “"+name+"”");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("MP4","*.mp4"));
        fc.setInitialFileName(name+".mp4");
        File out = fc.showSaveDialog(exportVideoBtn.getScene().getWindow());
        if (out==null) return;

        new Thread(() -> {
            try {
                double fps = 30.0;
                int frames = (int)(totalDuration*fps);
                Path tmp = Files.createTempDirectory("ffmpeg-frames");
                for (int i=0;i<frames;i++) {
                    double t = i/fps;
                    WritableImage wi = new WritableImage((int)mediaPlayerPane.getWidth(), (int)mediaPlayerPane.getHeight());
                    CountDownLatch latch = new CountDownLatch(1);
                    Platform.runLater(() -> { updatePreviewAtTime(t); mediaPlayerPane.snapshot(new SnapshotParameters(), wi); latch.countDown(); });
                    latch.await();
                    BufferedImage bi = SwingFXUtils.fromFXImage(wi, null);
                    File f = tmp.resolve(String.format("frame_%04d.png", i)).toFile();
                    ImageIO.write(bi, "png", f);
                }
                new ProcessBuilder(
                        "ffmpeg","-y","-framerate",String.valueOf((int)fps),
                        "-i", tmp.resolve("frame_%04d.png").toString(),
                        "-vf","pad=ceil(iw/2)*2:ceil(ih/2)*2",
                        "-c:v","libx264","-pix_fmt","yuv420p",
                        out.getAbsolutePath()
                ).inheritIO().start().waitFor();
                Files.list(tmp).forEach(p->p.toFile().delete());
                tmp.toFile().delete();

                // ← **Insert into DB here** before switching:
                Database.insertVideo(out.getAbsolutePath());

                Platform.runLater(this::switchToGallery);
            } catch(Exception ex) {
                ex.printStackTrace();
            }
        }).start();
    }

    private void switchToGallery() {
        try {
            FXMLLoader l=new FXMLLoader(getClass().getResource("video_page.fxml"));
            Parent r=l.load();
            VideoPageController vpc=l.getController(); vpc.refresh();
            Stage s=(Stage)exportVideoBtn.getScene().getWindow(); s.setScene(new Scene(r));
        } catch(IOException e){e.printStackTrace();}
    }

    private void setDiscardBtn() { discardBtn.setOnAction(e->discard()); }
    private void discard() {
        try { Parent r=FXMLLoader.load(getClass().getResource("gallery.fxml"));
            Stage s=(Stage)discardBtn.getScene().getWindow(); s.setScene(new Scene(r));
        } catch(Exception ex){ex.printStackTrace();}
    }

    private void makeNodeDraggable(Node node) {
        final Delta dragDelta = new Delta();
        node.setOnMousePressed(e -> {
            Point2D localPoint = mediaPlayerPane.sceneToLocal(e.getSceneX(), e.getSceneY());
            dragDelta.x = localPoint.getX() - node.getLayoutX();
            dragDelta.y = localPoint.getY() - node.getLayoutY();
            e.consume();
        });
        node.setOnMouseDragged(e -> {
            Point2D localPoint = mediaPlayerPane.sceneToLocal(e.getSceneX(), e.getSceneY());
            double newX = localPoint.getX() - dragDelta.x;
            double newY = localPoint.getY() - dragDelta.y;
            newX = Math.max(0, Math.min(newX, mediaPlayerPane.getWidth() - node.getBoundsInParent().getWidth()));
            newY = Math.max(0, Math.min(newY, mediaPlayerPane.getHeight() - node.getBoundsInParent().getHeight()));
            node.setLayoutX(newX);
            node.setLayoutY(newY);
            e.consume();
        });
    }

    private static class Delta{ double x,y; }
}