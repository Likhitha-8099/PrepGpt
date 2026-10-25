package com.prepgpt.dto;

public class RunCodeResultItemDto {
    private Long testCaseId;
    private Boolean passed;
    private Boolean isHidden;
    private String inputData;
    private String expectedOutput;
    private String actualOutput;
    private String errorMessage;

    public RunCodeResultItemDto() {
    }

    public RunCodeResultItemDto(Long testCaseId, Boolean passed, Boolean isHidden, String inputData,
                                String expectedOutput, String actualOutput, String errorMessage) {
        this.testCaseId = testCaseId;
        this.passed = passed;
        this.isHidden = isHidden != null ? isHidden : false;
        // Obfuscate if hidden
        if (Boolean.TRUE.equals(this.isHidden)) {
            this.inputData = "[HIDDEN]";
            this.expectedOutput = "[HIDDEN]";
            this.actualOutput = passed ? "[MATCHED]" : "[MISMATCH]";
        } else {
            this.inputData = inputData;
            this.expectedOutput = expectedOutput;
            this.actualOutput = actualOutput;
        }
        this.errorMessage = errorMessage;
    }

    public Long getTestCaseId() {
        return testCaseId;
    }

    public void setTestCaseId(Long testCaseId) {
        this.testCaseId = testCaseId;
    }

    public Boolean getPassed() {
        return passed;
    }

    public void setPassed(Boolean passed) {
        this.passed = passed;
    }

    public Boolean getIsHidden() {
        return isHidden;
    }

    public void setIsHidden(Boolean isHidden) {
        this.isHidden = isHidden;
    }

    public String getInputData() {
        return inputData;
    }

    public void setInputData(String inputData) {
        this.inputData = inputData;
    }

    public String getExpectedOutput() {
        return expectedOutput;
    }

    public void setExpectedOutput(String expectedOutput) {
        this.expectedOutput = expectedOutput;
    }

    public String getActualOutput() {
        return actualOutput;
    }

    public void setActualOutput(String actualOutput) {
        this.actualOutput = actualOutput;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}
