package com.prepgpt.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class SubmitAnswerRequest {

    @NotBlank(message = "Answer text cannot be empty.")
    @Size(max = 10000, message = "Answer text must not exceed 10,000 characters.")
    private String answerText;

    private com.prepgpt.entity.InputMethod inputMethod = com.prepgpt.entity.InputMethod.TEXT;

    public SubmitAnswerRequest() {
    }

    public SubmitAnswerRequest(String answerText) {
        this(answerText, com.prepgpt.entity.InputMethod.TEXT);
    }

    public SubmitAnswerRequest(String answerText, com.prepgpt.entity.InputMethod inputMethod) {
        this.answerText = answerText;
        this.inputMethod = inputMethod != null ? inputMethod : com.prepgpt.entity.InputMethod.TEXT;
    }

    public String getAnswerText() {
        return answerText;
    }

    public void setAnswerText(String answerText) {
        this.answerText = answerText;
    }

    public com.prepgpt.entity.InputMethod getInputMethod() {
        return inputMethod;
    }

    public void setInputMethod(com.prepgpt.entity.InputMethod inputMethod) {
        this.inputMethod = inputMethod;
    }
}
