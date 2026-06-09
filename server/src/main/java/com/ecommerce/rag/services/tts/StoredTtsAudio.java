package com.ecommerce.rag.services.tts;

public class StoredTtsAudio {

    private final String fileName;
    private final String contentType;

    public StoredTtsAudio(String fileName, String contentType) {
        this.fileName = fileName;
        this.contentType = contentType;
    }

    public String getFileName() {
        return fileName;
    }

    public String getContentType() {
        return contentType;
    }
}
