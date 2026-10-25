package com.prepgpt.dto;

import com.prepgpt.entity.AdaptiveDecision;
import com.prepgpt.entity.Difficulty;

import java.util.List;
import java.util.Map;

public class AdaptiveSummaryDto {

    private List<Difficulty> difficultyTimeline;
    private Map<AdaptiveDecision, Integer> decisionCounts;
    private List<String> focusedConcepts;

    public AdaptiveSummaryDto() {
    }

    public AdaptiveSummaryDto(List<Difficulty> difficultyTimeline,
                              Map<AdaptiveDecision, Integer> decisionCounts,
                              List<String> focusedConcepts) {
        this.difficultyTimeline = difficultyTimeline;
        this.decisionCounts = decisionCounts;
        this.focusedConcepts = focusedConcepts;
    }

    public List<Difficulty> getDifficultyTimeline() {
        return difficultyTimeline;
    }

    public void setDifficultyTimeline(List<Difficulty> difficultyTimeline) {
        this.difficultyTimeline = difficultyTimeline;
    }

    public Map<AdaptiveDecision, Integer> getDecisionCounts() {
        return decisionCounts;
    }

    public void setDecisionCounts(Map<AdaptiveDecision, Integer> decisionCounts) {
        this.decisionCounts = decisionCounts;
    }

    public List<String> getFocusedConcepts() {
        return focusedConcepts;
    }

    public void setFocusedConcepts(List<String> focusedConcepts) {
        this.focusedConcepts = focusedConcepts;
    }
}
