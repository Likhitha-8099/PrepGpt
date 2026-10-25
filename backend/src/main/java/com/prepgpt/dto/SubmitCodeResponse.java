package com.prepgpt.dto;

import com.prepgpt.entity.AdaptiveDecision;
import com.prepgpt.entity.ExecutionStatus;
import java.util.List;

public class SubmitCodeResponse {
    private ExecutionStatus status;
    private Integer passedCount;
    private Integer totalCount;
    private Long executionTimeMs;
    private String compileOutput;
    private List<RunCodeResultItemDto> testResults;
    private CodingEvaluationDto evaluation;
    private AdaptiveDecision adaptiveDecision;
    private Boolean isCompleted;

    public SubmitCodeResponse() {
    }

    public SubmitCodeResponse(ExecutionStatus status, Integer passedCount, Integer totalCount, Long executionTimeMs,
                              String compileOutput, List<RunCodeResultItemDto> testResults, CodingEvaluationDto evaluation,
                              AdaptiveDecision adaptiveDecision, Boolean isCompleted) {
        this.status = status;
        this.passedCount = passedCount;
        this.totalCount = totalCount;
        this.executionTimeMs = executionTimeMs;
        this.compileOutput = compileOutput;
        this.testResults = testResults;
        this.evaluation = evaluation;
        this.adaptiveDecision = adaptiveDecision;
        this.isCompleted = isCompleted;
    }

    public ExecutionStatus getStatus() {
        return status;
    }

    public void setStatus(ExecutionStatus status) {
        this.status = status;
    }

    public Integer getPassedCount() {
        return passedCount;
    }

    public void setPassedCount(Integer passedCount) {
        this.passedCount = passedCount;
    }

    public Integer getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(Integer totalCount) {
        this.totalCount = totalCount;
    }

    public Long getExecutionTimeMs() {
        return executionTimeMs;
    }

    public void setExecutionTimeMs(Long executionTimeMs) {
        this.executionTimeMs = executionTimeMs;
    }

    public String getCompileOutput() {
        return compileOutput;
    }

    public void setCompileOutput(String compileOutput) {
        this.compileOutput = compileOutput;
    }

    public List<RunCodeResultItemDto> getTestResults() {
        return testResults;
    }

    public void setTestResults(List<RunCodeResultItemDto> testResults) {
        this.testResults = testResults;
    }

    public CodingEvaluationDto getEvaluation() {
        return evaluation;
    }

    public void setEvaluation(CodingEvaluationDto evaluation) {
        this.evaluation = evaluation;
    }

    public AdaptiveDecision getAdaptiveDecision() {
        return adaptiveDecision;
    }

    public void setAdaptiveDecision(AdaptiveDecision adaptiveDecision) {
        this.adaptiveDecision = adaptiveDecision;
    }

    public Boolean getIsCompleted() {
        return isCompleted;
    }

    public void setIsCompleted(Boolean isCompleted) {
        this.isCompleted = isCompleted;
    }
}
