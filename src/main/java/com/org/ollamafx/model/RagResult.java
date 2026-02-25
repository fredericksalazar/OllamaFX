package com.org.ollamafx.model;

/**
 * Represents a single result from a RAG (vector similarity) query.
 */
public class RagResult {
    private String content;
    private String fileName;
    private int pageNumber;
    private double score;

    public RagResult(String content, String fileName, int pageNumber, double score) {
        this.content = content;
        this.fileName = fileName;
        this.pageNumber = pageNumber;
        this.score = score;
    }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public int getPageNumber() { return pageNumber; }
    public void setPageNumber(int pageNumber) { this.pageNumber = pageNumber; }

    public double getScore() { return score; }
    public void setScore(double score) { this.score = score; }
}
