package com.prepgpt.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "coding_problems")
public class CodingProblem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Difficulty difficulty;

    @Column(name = "dsa_category", nullable = false, length = 50)
    private String dsaCategory;

    @Column(name = "constraints_text", columnDefinition = "TEXT")
    private String constraintsText;

    @Column(name = "input_format", columnDefinition = "TEXT")
    private String inputFormat;

    @Column(name = "output_format", columnDefinition = "TEXT")
    private String outputFormat;

    @Column(name = "starter_code_java", columnDefinition = "TEXT")
    private String starterCodeJava;

    @Column(name = "starter_code_python", columnDefinition = "TEXT")
    private String starterCodePython;

    @Column(name = "time_limit_ms", nullable = false)
    private Integer timeLimitMs = 3000;

    @Column(name = "memory_limit_mb", nullable = false)
    private Integer memoryLimitMb = 256;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public CodingProblem() {
    }

    public CodingProblem(String title, String description, Difficulty difficulty, String dsaCategory,
                         String constraintsText, String inputFormat, String outputFormat,
                         String starterCodeJava, String starterCodePython, Integer timeLimitMs, Integer memoryLimitMb) {
        this.title = title;
        this.description = description;
        this.difficulty = difficulty;
        this.dsaCategory = dsaCategory;
        this.constraintsText = constraintsText;
        this.inputFormat = inputFormat;
        this.outputFormat = outputFormat;
        this.starterCodeJava = starterCodeJava;
        this.starterCodePython = starterCodePython;
        this.timeLimitMs = timeLimitMs != null ? Math.min(timeLimitMs, 5000) : 3000;
        this.memoryLimitMb = memoryLimitMb != null ? memoryLimitMb : 256;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
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
        this.timeLimitMs = timeLimitMs != null ? Math.min(timeLimitMs, 5000) : 3000;
    }

    public Integer getMemoryLimitMb() {
        return memoryLimitMb;
    }

    public void setMemoryLimitMb(Integer memoryLimitMb) {
        this.memoryLimitMb = memoryLimitMb;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
