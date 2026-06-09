package com.ecommerce.rag.services.tts;

public class TtsAudioResult {

    private final byte[] audioBytes;
    private final String contentType;
    private final String fileExtension;

    public TtsAudioResult(byte[] audioBytes, String contentType, String fileExtension) {
        this.audioBytes = audioBytes;
        this.contentType = contentType;
        this.fileExtension = fileExtension;
    }

    public byte[] getAudioBytes() {
        return audioBytes;
    }

    public String getContentType() {
        return contentType;
    }

    public String getFileExtension() {
        return fileExtension;
    }
}
