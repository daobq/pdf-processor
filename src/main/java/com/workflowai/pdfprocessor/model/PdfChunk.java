package com.workflowai.pdfprocessor.model;

public class PdfChunk {
    private int chunkId;
    private String text;

    public PdfChunk(int chunkId, String text) {
        this.chunkId = chunkId;
        this.text = text;
    }

    public int getChunkId() {
        return chunkId;
    }

    public void setChunkId(int chunkId) {
        this.chunkId = chunkId;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}