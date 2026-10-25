package com.prepgpt.dto;

public class CodingTestCaseDto {
    private Long id;
    private String inputData;
    private String expectedOutput;
    private String explanation;

    public CodingTestCaseDto() {
    }

    public CodingTestCaseDto(Long id, String inputData, String expectedOutput, String explanation) {
        this.id = id;
        this.inputData = inputData;
        this.expectedOutput = expectedOutput;
        this.explanation = explanation;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public String getExplanation() {
        return explanation;
    }

    public void setExplanation(String explanation) {
        this.explanation = explanation;
    }
}
