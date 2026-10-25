package com.prepgpt.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class VoiceSynthesisRequest {

    @NotBlank(message = "Text to synthesize cannot be blank")
    @Size(max = 2000, message = "Synthesis text cannot exceed 2,000 characters")
    private String text;

    public VoiceSynthesisRequest() {
    }

    public VoiceSynthesisRequest(String text) {
        this.text = text;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}
