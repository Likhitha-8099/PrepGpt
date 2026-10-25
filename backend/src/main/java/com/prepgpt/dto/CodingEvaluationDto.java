package com.prepgpt.dto;

import java.util.List;

public class CodingEvaluationDto {
    private Integer correctnessScore; // 0-10
    private String timeComplexity;     // e.g. "O(N)"
    private String spaceComplexity;    // e.g. "O(1)"
    private Integer codeQualityScore;  // 0-10
    private List<String> strengths;
    private List<String> weaknesses;
    private List<String> optimizationSuggestions;
    private String overallFeedback;

    public CodingEvaluationDto() {
    }

    public CodingEvaluationDto(Integer correctnessScore, String timeComplexity, String spaceComplexity,
                               Integer codeQualityScore, List<String> strengths, List<String> weaknesses,
                               List<String> optimizationSuggestions, String overallFeedback) {
        this.correctnessScore = correctnessScore;
        this.timeComplexity = timeComplexity;
        this.spaceComplexity = spaceComplexity;
        this.codeQualityScore = codeQualityScore;
        this.strengths = strengths;
        this.weaknesses = weaknesses;
        this.optimizationSuggestions = optimizationSuggestions;
        this.overallFeedback = overallFeedback;
    }

    public Integer getCorrectnessScore() {
        return correctnessScore;
    }

    public void setCorrectnessScore(Integer correctnessScore) {
        this.correctnessScore = correctnessScore;
    }

    public String getTimeComplexity() {
        return timeComplexity;
    }

    public void setTimeComplexity(String timeComplexity) {
        this.timeComplexity = timeComplexity;
    }

    public String getSpaceComplexity() {
        return spaceComplexity;
    }

    public void setSpaceComplexity(String spaceComplexity) {
        this.spaceComplexity = spaceComplexity;
    }

    public Integer getCodeQualityScore() {
        return codeQualityScore;
    }

    public void setCodeQualityScore(Integer codeQualityScore) {
        this.codeQualityScore = codeQualityScore;
    }

    public List<String> getStrengths() {
        return strengths;
    }

    public void setStrengths(List<String> strengths) {
        this.strengths = strengths;
    }

    public List<String> getWeaknesses() {
        return weaknesses;
    }

    public void setWeaknesses(List<String> weaknesses) {
        this.weaknesses = weaknesses;
    }

    public List<String> getOptimizationSuggestions() {
        return optimizationSuggestions;
    }

    public void setOptimizationSuggestions(List<String> optimizationSuggestions) {
        this.optimizationSuggestions = optimizationSuggestions;
    }

    public String getOverallFeedback() {
        return overallFeedback;
    }

    public void setOverallFeedback(String overallFeedback) {
        this.overallFeedback = overallFeedback;
    }
}
