package com.prepgpt.dto;

import com.prepgpt.entity.ExecutionStatus;
import java.util.List;

public class RunCodeResponse {
    private ExecutionStatus status;
    private Integer passedCount;
    private Integer totalCount;
    private Long executionTimeMs;
    private String compileOutput;
    private List<RunCodeResultItemDto> testResults;

    public RunCodeResponse() {
    }

    public RunCodeResponse(ExecutionStatus status, Integer passedCount, Integer totalCount, Long executionTimeMs,
                           String compileOutput, List<RunCodeResultItemDto> testResults) {
        this.status = status;
        this.passedCount = passedCount;
        this.totalCount = totalCount;
        this.executionTimeMs = executionTimeMs;
        this.compileOutput = compileOutput;
        this.testResults = testResults;
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
}
