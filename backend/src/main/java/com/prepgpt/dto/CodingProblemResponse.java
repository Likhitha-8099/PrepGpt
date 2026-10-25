package com.prepgpt.dto;

import com.prepgpt.entity.Difficulty;
import java.util.List;

public class CodingProblemResponse {
    private Long id;
    private String title;
    private String description;
    private Difficulty difficulty;
    private String dsaCategory;
    private String constraintsText;
    private String inputFormat;
    private String outputFormat;
    private String starterCodeJava;
    private String starterCodePython;
    private Integer timeLimitMs;
    private Integer memoryLimitMb;
    private List<CodingTestCaseDto> visibleTestCases;

    public CodingProblemResponse() {
    }

    public CodingProblemResponse(Long id, String title, String description, Difficulty difficulty, String dsaCategory,
                                 String constraintsText, String inputFormat, String outputFormat,
                                 String starterCodeJava, String starterCodePython, Integer timeLimitMs,
                                 Integer memoryLimitMb, List<CodingTestCaseDto> visibleTestCases) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.difficulty = difficulty;
        this.dsaCategory = dsaCategory;
        this.constraintsText = constraintsText;
        this.inputFormat = inputFormat;
        this.outputFormat = outputFormat;
        this.starterCodeJava = starterCodeJava;
        this.starterCodePython = starterCodePython;
        this.timeLimitMs = timeLimitMs;
        this.memoryLimitMb = memoryLimitMb;
        this.visibleTestCases = visibleTestCases;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Difficulty getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(Difficulty difficulty) {
        this.difficulty = difficulty;
    }

    public String getDsaCategory() {
        return dsaCategory;
    }

    public void setDsaCategory(String dsaCategory) {
        this.dsaCategory = dsaCategory;
    }

    public String getConstraintsText() {
        return constraintsText;
    }

    public void setConstraintsText(String constraintsText) {
        this.constraintsText = constraintsText;
    }

    public String getInputFormat() {
        return inputFormat;
    }

    public void setInputFormat(String inputFormat) {
        this.inputFormat = inputFormat;
    }

    public String getOutputFormat() {
        return outputFormat;
    }

    public void setOutputFormat(String outputFormat) {
        this.outputFormat = outputFormat;
    }

    public String getStarterCodeJava() {
        return starterCodeJava;
    }

    public void setStarterCodeJava(String starterCodeJava) {
        this.starterCodeJava = starterCodeJava;
    }

    public String getStarterCodePython() {
        return starterCodePython;
    }

    public void setStarterCodePython(String starterCodePython) {
        this.starterCodePython = starterCodePython;
    }

    public Integer getTimeLimitMs() {
        return timeLimitMs;
    }

    public void setTimeLimitMs(Integer timeLimitMs) {
        this.timeLimitMs = timeLimitMs;
    }

    public Integer getMemoryLimitMb() {
        return memoryLimitMb;
    }

    public void setMemoryLimitMb(Integer memoryLimitMb) {
        this.memoryLimitMb = memoryLimitMb;
    }

    public List<CodingTestCaseDto> getVisibleTestCases() {
        return visibleTestCases;
    }

    public void setVisibleTestCases(List<CodingTestCaseDto> visibleTestCases) {
        this.visibleTestCases = visibleTestCases;
    }
}
