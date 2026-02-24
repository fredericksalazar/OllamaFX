package com.org.ollamafx.service;

import com.org.ollamafx.model.ChatMessage;
import com.org.ollamafx.model.ChatSession;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class MarkdownService {

    /**
     * Exports a ChatSession to a standard Markdown file.
     *
     * @param session The chat session to export
     * @param file    The destination file
     * @throws IOException If writing to the file fails
     */
    public static void exportChatToMarkdown(ChatSession session, File file) throws IOException {
        try (PrintWriter writer = new PrintWriter(new FileWriter(file))) {
            // Frontmatter / Header
            writer.println("# " + session.getName());
            writer.println();
            writer.println("**Model:** `" + session.getModelName() + "`");

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            writer.println("**Exported:** " + sdf.format(new Date()));
            writer.println();
            writer.println("---");
            writer.println();

            // Messages
            List<ChatMessage> messages = session.getMessages();
            for (ChatMessage msg : messages) {
                if ("system".equalsIgnoreCase(msg.getRole())) {
                    continue; // Optional: skip system prompts, or format them differently
                }

                String roleHeader = "user".equalsIgnoreCase(msg.getRole()) ? "### 👤 User" : "### 🤖 Ollama";
                writer.println(roleHeader);
                writer.println();

                // Add images tag if present
                if (msg.getImages() != null && !msg.getImages().isEmpty()) {
                    writer.println("*(Attached " + msg.getImages().size() + " image(s))*");
                    writer.println();
                }

                writer.println(msg.getContent());
                writer.println();
                writer.println("---");
                writer.println();
            }
        }
    }
}
