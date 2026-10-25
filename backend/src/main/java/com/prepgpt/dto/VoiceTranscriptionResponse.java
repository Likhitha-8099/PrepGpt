package com.prepgpt.dto;

public class VoiceTranscriptionResponse {

    private String text;
    private String language;
    private Float confidence;

    public VoiceTranscriptionResponse() {
    }

    public VoiceTranscriptionResponse(String text, String language, Float confidence) {
        this.text = text;
        this.language = language != null ? language : "en";
        this.confidence = confidence;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public Float getConfidence() {
        return confidence;
    }

    public void setConfidence(Float confidence) {
        this.confidence = confidence;
    }
}
