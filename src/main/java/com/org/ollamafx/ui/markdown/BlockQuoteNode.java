package com.org.ollamafx.ui.markdown;

import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

final class BlockQuoteNode extends HBox {

    BlockQuoteNode(VBox content) {
        getStyleClass().add("md-blockquote");
        setFillHeight(true);

        Region bar = new Region();
        bar.getStyleClass().add("md-blockquote-bar");
        bar.setPrefWidth(3);
        bar.setMinWidth(3);
        bar.setMaxWidth(3);

        content.getStyleClass().add("md-blockquote-content");
        content.setPadding(new Insets(2, 0, 2, 12));
        HBox.setHgrow(content, Priority.ALWAYS);

        getChildren().addAll(bar, content);
    }

    static Node wrap(VBox content) {
        return new BlockQuoteNode(content);
    }
}
