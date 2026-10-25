package com.prepgpt.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "interview_sessions", indexes = {
        @Index(name = "idx_session_user_created", columnList = "user_id, created_at")
})
public class InterviewSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    private Long version;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private InterviewCategory category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Difficulty difficulty;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SessionStatus status = SessionStatus.CONFIGURED;

    @Enumerated(EnumType.STRING)
    @Column(name = "mode", nullable = false, length = 20)
    private InterviewMode mode = InterviewMode.QUESTION_BANK;

    @Enumerated(EnumType.STRING)
    @Column(name = "interaction_mode", nullable = false, length = 20)
    private InterviewInteractionMode interactionMode = InterviewInteractionMode.TEXT;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resume_id")
    private Resume resume;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_description_id")
    private JobDescription jobDescription;

    @Column(name = "jd_target_role")
    private String jdTargetRole;

    @Column(name = "jd_company_name")
    private String jdCompanyName;

    @Column(name = "jd_extracted_skills_json", columnDefinition = "TEXT")
    private String jdExtractedSkillsJson;

    @Column(name = "jd_key_responsibilities_json", columnDefinition = "TEXT")
    private String jdKeyResponsibilitiesJson;

    @Column(name = "jd_ai_analysis_summary", columnDefinition = "TEXT")
    private String jdAiAnalysisSummary;

    @Column(name = "resume_skills_snapshot_json", columnDefinition = "TEXT")
    private String resumeSkillsSnapshotJson;

    @Column(name = "resume_experience_snapshot_json", columnDefinition = "TEXT")
    private String resumeExperienceSnapshotJson;

    @Column(name = "project_title_snapshot")
    private String projectTitleSnapshot;

    @Column(name = "project_tech_snapshot", columnDefinition = "TEXT")
    private String projectTechSnapshot;

    @Column(name = "question_count", nullable = false)
    private Integer questionCount;

    @Column(name = "duration_minutes", nullable = false)
    private Integer durationMinutes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    public InterviewSession() {
    }

    public InterviewSession(User user, InterviewCategory category, Difficulty difficulty,
                            Integer questionCount, Integer durationMinutes) {
        this(user, category, difficulty, questionCount, durationMinutes, InterviewMode.QUESTION_BANK, InterviewInteractionMode.TEXT);
    }

    public InterviewSession(User user, InterviewCategory category, Difficulty difficulty,
                            Integer questionCount, Integer durationMinutes, InterviewMode mode) {
        this(user, category, difficulty, questionCount, durationMinutes, mode, InterviewInteractionMode.TEXT);
    }

    public InterviewSession(User user, InterviewCategory category, Difficulty difficulty,
                            Integer questionCount, Integer durationMinutes, InterviewMode mode,
                            InterviewInteractionMode interactionMode) {
        this.user = user;
        this.category = category;
        this.difficulty = difficulty;
        this.questionCount = questionCount;
        this.durationMinutes = durationMinutes;
        this.mode = mode != null ? mode : InterviewMode.QUESTION_BANK;
        this.interactionMode = interactionMode != null ? interactionMode : InterviewInteractionMode.TEXT;
        this.status = SessionStatus.CONFIGURED;
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

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public InterviewCategory getCategory() {
        return category;
    }

    public void setCategory(InterviewCategory category) {
        this.category = category;
    }

    public Difficulty getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(Difficulty difficulty) {
        this.difficulty = difficulty;
    }

    public SessionStatus getStatus() {
        return status;
    }

    public void setStatus(SessionStatus status) {
        this.status = status;
    }

    public InterviewMode getMode() {
        return mode;
    }

    public void setMode(InterviewMode mode) {
        this.mode = mode;
    }

    public InterviewInteractionMode getInteractionMode() {
        return interactionMode;
    }

    public void setInteractionMode(InterviewInteractionMode interactionMode) {
        this.interactionMode = interactionMode;
    }

    public Integer getQuestionCount() {
        return questionCount;
    }

    public void setQuestionCount(Integer questionCount) {
        this.questionCount = questionCount;
    }

    public Integer getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(Integer durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public Resume getResume() {
        return resume;
    }

    public void setResume(Resume resume) {
        this.resume = resume;
    }

    public Project getProject() {
        return project;
    }

    public void setProject(Project project) {
        this.project = project;
    }

    public JobDescription getJobDescription() {
        return jobDescription;
    }

    public void setJobDescription(JobDescription jobDescription) {
        this.jobDescription = jobDescription;
    }

    public String getJdTargetRole() {
        return jdTargetRole;
    }

    public void setJdTargetRole(String jdTargetRole) {
        this.jdTargetRole = jdTargetRole;
    }

    public String getJdCompanyName() {
        return jdCompanyName;
    }

    public void setJdCompanyName(String jdCompanyName) {
        this.jdCompanyName = jdCompanyName;
    }

    public String getJdExtractedSkillsJson() {
        return jdExtractedSkillsJson;
    }

    public void setJdExtractedSkillsJson(String jdExtractedSkillsJson) {
        this.jdExtractedSkillsJson = jdExtractedSkillsJson;
    }

    public String getJdKeyResponsibilitiesJson() {
        return jdKeyResponsibilitiesJson;
    }

    public void setJdKeyResponsibilitiesJson(String jdKeyResponsibilitiesJson) {
        this.jdKeyResponsibilitiesJson = jdKeyResponsibilitiesJson;
    }

    public String getJdAiAnalysisSummary() {
        return jdAiAnalysisSummary;
    }

    public void setJdAiAnalysisSummary(String jdAiAnalysisSummary) {
        this.jdAiAnalysisSummary = jdAiAnalysisSummary;
    }

    public String getResumeSkillsSnapshotJson() {
        return resumeSkillsSnapshotJson;
    }

    public void setResumeSkillsSnapshotJson(String resumeSkillsSnapshotJson) {
        this.resumeSkillsSnapshotJson = resumeSkillsSnapshotJson;
    }

    public String getResumeExperienceSnapshotJson() {
        return resumeExperienceSnapshotJson;
    }

    public void setResumeExperienceSnapshotJson(String resumeExperienceSnapshotJson) {
        this.resumeExperienceSnapshotJson = resumeExperienceSnapshotJson;
    }

    public String getProjectTitleSnapshot() {
        return projectTitleSnapshot;
    }

    public void setProjectTitleSnapshot(String projectTitleSnapshot) {
        this.projectTitleSnapshot = projectTitleSnapshot;
    }

    public String getProjectTechSnapshot() {
        return projectTechSnapshot;
    }

    public void setProjectTechSnapshot(String projectTechSnapshot) {
        this.projectTechSnapshot = projectTechSnapshot;
    }
}
