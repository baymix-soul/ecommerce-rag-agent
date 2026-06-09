package com.ecommerce.rag.models.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class TtsSpeakResponse {

    @JsonProperty("audio_url")
    private String audioUrl;

    @JsonProperty("audio_base64")
    private String audioBase64;

    @JsonProperty("content_type")
    private String contentType;

    public TtsSpeakResponse() {
    }

    public TtsSpeakResponse(String audioUrl, String contentType) {
        this.audioUrl = audioUrl;
        this.contentType = contentType;
    }

    public String getAudioUrl() {
        return audioUrl;
    }

    public void setAudioUrl(String audioUrl) {
        this.audioUrl = audioUrl;
    }

    public String getAudioBase64() {
        return audioBase64;
    }

    public void setAudioBase64(String audioBase64) {
        this.audioBase64 = audioBase64;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }
}
