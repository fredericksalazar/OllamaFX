package com.org.ollamafx.controller;

import com.vladsch.flexmark.ext.gfm.tasklist.TaskListExtension;
import com.vladsch.flexmark.ext.tables.TablesExtension;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.data.MutableDataSet;
import com.vladsch.flexmark.util.misc.Extension;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.web.WebView;
import javafx.stage.Stage;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class MarkdownViewerController {

    @FXML
    private Label titleLabel;

    @FXML
    private WebView webView;

    private File currentFile;

    @FXML
    public void initialize() {
        // Initialization if needed
    }

    /**
     * Loads a markdown string directly into the viewer and renders it.
     * 
     * @param markdown   The raw markdown content
     * @param title      The title to display
     * @param sourceFile Optional source file for opening externally
     */
    public void loadMarkdown(String markdown, String title, File sourceFile) {
        this.currentFile = sourceFile;
        titleLabel.setText(title != null ? title : "Markdown Viewer");

        // Flexmark setup with extensions (Tables, TaskLists)
        List<Extension> extensions = Arrays.asList(
                TablesExtension.create(),
                TaskListExtension.create());

        MutableDataSet options = new MutableDataSet();
        options.set(Parser.EXTENSIONS, extensions);
        options.set(HtmlRenderer.SOFT_BREAK, "<br />\n");
        options.set(HtmlRenderer.GENERATE_HEADER_ID, true);

        Parser parser = Parser.builder(options).build();
        HtmlRenderer renderer = HtmlRenderer.builder(options).build();

        Node document = parser.parse(markdown);
        String htmlBody = renderer.render(document);

        // Wrap in a styled HTML skeleton
        String fullHtml = buildHtmlFrame(htmlBody);

        webView.getEngine().loadContent(fullHtml);
    }

    private String buildHtmlFrame(String body) {
        // Base styling for the WebView to match OllamaFX
        String css = """
                    body {
                        font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
                        line-height: 1.6;
                        padding: 2rem;
                        color: #e0e0e0;
                        background-color: #1e1e1e;
                    }
                    h1, h2, h3 { color: #ffffff; margin-top: 1.5em; }
                    h1 { border-bottom: 1px solid #333; padding-bottom: 0.3em; }
                    a { color: #58a6ff; text-decoration: none; }
                    a:hover { text-decoration: underline; }
                    pre {
                        background-color: #0d1117;
                        padding: 1rem;
                        border-radius: 6px;
                        overflow-x: auto;
                        border: 1px solid #30363d;
                    }
                    code {
                        font-family: "SFMono-Regular", Consolas, "Liberation Mono", Menlo, monospace;
                        background-color: rgba(110,118,129,0.4);
                        padding: 0.2em 0.4em;
                        border-radius: 3px;
                        font-size: 85%;
                    }
                    pre code { background-color: transparent; padding: 0; border: none; }
                    blockquote {
                        border-left: 4px solid #3b3b3b;
                        color: #8b949e;
                        padding-left: 1em;
                        margin-left: 0;
                    }
                    table { border-collapse: collapse; width: 100%; margin: 15px 0; border: 1px solid #3b3b3b; }
                    th, td { border: 1px solid #3b3b3b; padding: 8px 12px; text-align: left; }
                    th { background-color: #2d2d2d; font-weight: bold; }
                    tr:nth-child(even) { background-color: #252525; }
                    hr { border: 0; border-top: 1px solid #3b3b3b; margin: 2em 0; }
                    .task-list-item { list-style-type: none; }
                    .task-list-item input { margin-right: 0.5em; }
                """;

        String prismJs = "https://cdnjs.cloudflare.com/ajax/libs/prism/1.29.0/prism.min.js";
        String prismCss = "https://cdnjs.cloudflare.com/ajax/libs/prism/1.29.0/themes/prism-tomorrow.min.css";
        String prismAutoloader = "https://cdnjs.cloudflare.com/ajax/libs/prism/1.29.0/plugins/autoloader/prism-autoloader.min.js";

        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset='utf-8'>
                    <link href="%s" rel="stylesheet" />
                    <style>
                        %s
                        /* Override Prism background to match our theme */
                        pre[class*="language-"] {
                            background: #0d1117 !important;
                            margin: 1.5em 0;
                            padding: 1em;
                            border-radius: 6px;
                            border: 1px solid #30363d;
                        }
                    </style>
                </head>
                <body>
                    %s
                    <script src="%s"></script>
                    <script src="%s"></script>
                    <script>
                        Prism.plugins.autoloader.languages_path = 'https://cdnjs.cloudflare.com/ajax/libs/prism/1.29.0/components/';
                    </script>
                </body>
                </html>
                """
                .formatted(prismCss, css, body, prismJs, prismAutoloader);
    }

    @FXML
    private void onCloseClicked() {
        Stage stage = (Stage) webView.getScene().getWindow();
        stage.close();
    }

    @FXML
    private void onOpenExternalClicked() {
        if (currentFile != null && Desktop.isDesktopSupported()) {
            try {
                Desktop.getDesktop().open(currentFile);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
