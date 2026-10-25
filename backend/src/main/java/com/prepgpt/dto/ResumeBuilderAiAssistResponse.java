package com.prepgpt.dto;

import java.util.ArrayList;
import java.util.List;

public class ResumeBuilderAiAssistResponse {

    private String action;
    private String improvedText;
    private List<String> suggestions = new ArrayList<>();
    private List<String> matchedKeywords = new ArrayList<>();
    private List<String> missingKeywords = new ArrayList<>();
    private List<String> auditTips = new ArrayList<>();
    private Integer atsReadinessScore;

    public ResumeBuilderAiAssistResponse() {
    }

    public ResumeBuilderAiAssistResponse(String action, String improvedText) {
        this.action = action;
        this.improvedText = improvedText;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getImprovedText() {
        return improvedText;
    }

    public void setImprovedText(String improvedText) {
        this.improvedText = improvedText;
    }

    public List<String> getSuggestions() {
        return suggestions;
    }

    public void setSuggestions(List<String> suggestions) {
        this.suggestions = suggestions;
    }

    public List<String> getMatchedKeywords() {
        return matchedKeywords;
    }

    public void setMatchedKeywords(List<String> matchedKeywords) {
        this.matchedKeywords = matchedKeywords;
    }

    public List<String> getMissingKeywords() {
        return missingKeywords;
    }

    public void setMissingKeywords(List<String> missingKeywords) {
        this.missingKeywords = missingKeywords;
    }

    public List<String> getAuditTips() {
        return auditTips;
    }

    public void setAuditTips(List<String> auditTips) {
        this.auditTips = auditTips;
    }

    public Integer getAtsReadinessScore() {
        return atsReadinessScore;
    }

    public void setAtsReadinessScore(Integer atsReadinessScore) {
        this.atsReadinessScore = atsReadinessScore;
    }
}
