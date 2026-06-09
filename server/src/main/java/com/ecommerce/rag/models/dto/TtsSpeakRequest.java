package com.ecommerce.rag.models.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class TtsSpeakRequest {

    @JsonProperty("text")
    private String text;

    @JsonProperty("voice")
    private String voice;

    @JsonProperty("format")
    private String format;

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getVoice() {
        return voice;
    }

    public void setVoice(String voice) {
        this.voice = voice;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }
}
