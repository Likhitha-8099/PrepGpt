package com.prepgpt.dto;

import com.prepgpt.entity.Difficulty;
import com.prepgpt.entity.QuestionType;

public class AIQuestionResponse {

    private String question;
    private QuestionType questionType;
    private Difficulty difficulty;
    private String topic;
    private String rationale;

    public AIQuestionResponse() {
    }

    public AIQuestionResponse(String question, QuestionType questionType, Difficulty difficulty, String topic, String rationale) {
        this.question = question;
        this.questionType = questionType;
        this.difficulty = difficulty;
        this.topic = topic;
        this.rationale = rationale;
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public QuestionType getQuestionType() {
        return questionType;
    }

    public void setQuestionType(QuestionType questionType) {
        this.questionType = questionType;
    }

    public Difficulty getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(Difficulty difficulty) {
        this.difficulty = difficulty;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public String getRationale() {
        return rationale;
    }

    public void setRationale(String rationale) {
        this.rationale = rationale;
    }
}
