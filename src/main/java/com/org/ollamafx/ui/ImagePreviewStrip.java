package com.org.ollamafx.ui;

import com.org.ollamafx.App;
import com.org.ollamafx.util.ImageUtils;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * Horizontal strip of image thumbnails shown in the input area when images
 * are attached for multimodal chat. Each thumbnail has a remove button.
 */
public class ImagePreviewStrip extends HBox {

    private static final int THUMB_SIZE = 60;

    private final List<File> imageFiles = new ArrayList<>();
    private final List<String> base64Cache = new ArrayList<>();
    private final BooleanProperty empty = new SimpleBooleanProperty(true);
    private final Label countLabel;

    public ImagePreviewStrip() {
        this.getStyleClass().add("image-preview-strip");
        this.setSpacing(8);
        this.setPadding(new Insets(6, 0, 6, 0));
        this.setAlignment(Pos.CENTER_LEFT);

        countLabel = new Label();
        countLabel.getStyleClass().add("image-count-label");
        countLabel.setVisible(false);
        countLabel.setManaged(false);

        // Hidden by default
        this.setVisible(false);
        this.setManaged(false);
    }

    /**
     * Adds an image file to the strip. Validates format and size.
     *
     * @return null on success, or an i18n error key on failure
     */
    public String addImage(File file) {
        if (!ImageUtils.isSupportedFormat(file)) {
            return "chat.image.invalidFormat";
        }
        if (ImageUtils.isFileTooLarge(file)) {
            return "chat.image.tooLarge";
        }

        try {
            String base64 = ImageUtils.toBase64(file);
            Image thumb = ImageUtils.createThumbnail(file, THUMB_SIZE);

            imageFiles.add(file);
            base64Cache.add(base64);

            // Build thumbnail pill
            StackPane pill = createThumbnailPill(thumb, imageFiles.size() - 1);
            this.getChildren().add(pill);

            updateVisibility();
            return null; // Success
        } catch (IOException e) {
            e.printStackTrace();
            return "chat.image.invalidFormat";
        }
    }

    private StackPane createThumbnailPill(Image thumb, int index) {
        ImageView iv = new ImageView(thumb);
        iv.setFitWidth(THUMB_SIZE);
        iv.setFitHeight(THUMB_SIZE);
        iv.setPreserveRatio(true);
        iv.setSmooth(true);

        // Rounded corners
        Rectangle clip = new Rectangle(THUMB_SIZE, THUMB_SIZE);
        clip.setArcWidth(12);
        clip.setArcHeight(12);
        iv.setClip(clip);

        // Remove button
        Button removeBtn = new Button("✕");
        removeBtn.getStyleClass().add("image-remove-btn");
        removeBtn.setOnAction(e -> removeImage(index));

        StackPane pill = new StackPane(iv, removeBtn);
        pill.getStyleClass().add("image-thumbnail");
        StackPane.setAlignment(removeBtn, Pos.TOP_RIGHT);
        StackPane.setMargin(removeBtn, new Insets(-4, -4, 0, 0));

        Tooltip.install(pill,
                new Tooltip(imageFiles.get(index).getName()));

        // Subtle hover scale
        pill.setOnMouseEntered(e -> pill.setScaleX(1.05));
        pill.setOnMouseExited(e -> pill.setScaleX(1.0));
        pill.setOnMouseEntered(e -> {
            pill.setScaleX(1.05);
            pill.setScaleY(1.05);
        });
        pill.setOnMouseExited(e -> {
            pill.setScaleX(1.0);
            pill.setScaleY(1.0);
        });

        return pill;
    }

    private void removeImage(int index) {
        if (index >= 0 && index < imageFiles.size()) {
            imageFiles.remove(index);
            base64Cache.remove(index);
            rebuildThumbnails();
            updateVisibility();
        }
    }

    private void rebuildThumbnails() {
        this.getChildren().clear();
        for (int i = 0; i < imageFiles.size(); i++) {
            Image thumb = ImageUtils.createThumbnail(imageFiles.get(i), THUMB_SIZE);
            this.getChildren().add(createThumbnailPill(thumb, i));
        }
    }

    private void updateVisibility() {
        boolean hasImages = !imageFiles.isEmpty();
        this.setVisible(hasImages);
        this.setManaged(hasImages);
        empty.set(!hasImages);

        if (hasImages) {
            String template = App.getBundle().getString("chat.image.attached");
            countLabel.setText(MessageFormat.format(template, imageFiles.size()));
            countLabel.setVisible(true);
            countLabel.setManaged(true);
        } else {
            countLabel.setVisible(false);
            countLabel.setManaged(false);
        }
    }

    /**
     * Clears all attached images and resets the strip.
     */
    public void clearImages() {
        imageFiles.clear();
        base64Cache.clear();
        this.getChildren().clear();
        updateVisibility();
    }

    /**
     * Returns the base64-encoded image data for sending to Ollama.
     */
    public List<String> getBase64Images() {
        return new ArrayList<>(base64Cache);
    }

    /**
     * Returns true if there are images attached.
     */
    public boolean hasImages() {
        return !imageFiles.isEmpty();
    }

    /**
     * Observable property for binding visibility of related components.
     */
    public BooleanProperty emptyProperty() {
        return empty;
    }

    /**
     * Returns the image files for rendering thumbnails in the user bubble.
     */
    public List<File> getImageFiles() {
        return new ArrayList<>(imageFiles);
    }

    /**
     * Returns the count label (can be added to parent layout externally).
     */
    public Label getCountLabel() {
        return countLabel;
    }
}
