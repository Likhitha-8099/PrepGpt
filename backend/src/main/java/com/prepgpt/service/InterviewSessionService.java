package com.prepgpt.service;

import com.prepgpt.dto.CreateInterviewSessionRequest;
import com.prepgpt.dto.InterviewSessionResponse;
import com.prepgpt.entity.InterviewCategory;
import com.prepgpt.entity.InterviewSession;
import com.prepgpt.entity.SessionStatus;
import com.prepgpt.entity.User;
import com.prepgpt.exception.BadRequestException;
import com.prepgpt.exception.ResourceNotFoundException;
import com.prepgpt.repository.InterviewCategoryRepository;
import com.prepgpt.repository.InterviewSessionRepository;
import com.prepgpt.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class InterviewSessionService {

    private final InterviewSessionRepository sessionRepository;
    private final InterviewCategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final com.prepgpt.repository.ResumeRepository resumeRepository;
    private final com.prepgpt.repository.ProjectRepository projectRepository;
    private final com.prepgpt.repository.JobDescriptionRepository jobDescriptionRepository;

    public InterviewSessionService(InterviewSessionRepository sessionRepository,
                                  InterviewCategoryRepository categoryRepository,
                                  UserRepository userRepository,
                                  com.prepgpt.repository.ResumeRepository resumeRepository,
                                  com.prepgpt.repository.ProjectRepository projectRepository,
                                  com.prepgpt.repository.JobDescriptionRepository jobDescriptionRepository) {
        this.sessionRepository = sessionRepository;
        this.categoryRepository = categoryRepository;
        this.userRepository = userRepository;
        this.resumeRepository = resumeRepository;
        this.projectRepository = projectRepository;
        this.jobDescriptionRepository = jobDescriptionRepository;
    }

    @Transactional
    public InterviewSessionResponse createSession(String userEmail, CreateInterviewSessionRequest request) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", userEmail));

        InterviewCategory category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("InterviewCategory", "id", request.getCategoryId()));

        if (!category.isActive()) {
            throw new BadRequestException("Selected interview category is not active");
        }

        InterviewSession session = new InterviewSession(
                user,
                category,
                request.getDifficulty(),
                request.getQuestionCount(),
                request.getDurationMinutes(),
                request.getMode(),
                request.getInteractionMode()
        );

        if (request.getResumeId() != null) {
            com.prepgpt.entity.Resume resume = resumeRepository.findByIdAndUserId(request.getResumeId(), user.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Resume not found with id " + request.getResumeId() + " for current user"));
            session.setResume(resume);
            session.setResumeSkillsSnapshotJson(resume.getSkillsJson());
            session.setResumeExperienceSnapshotJson(resume.getExperienceJson());
        }

        if (request.getProjectId() != null) {
            com.prepgpt.entity.Project project = projectRepository.findByIdAndUserId(request.getProjectId(), user.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Project not found with id " + request.getProjectId() + " for current user"));
            session.setProject(project);
            session.setProjectTitleSnapshot(project.getTitle());
            session.setProjectTechSnapshot(project.getTechnologies());
        }

        if (request.getJobDescriptionId() != null) {
            com.prepgpt.entity.JobDescription jd = jobDescriptionRepository.findByIdAndUserId(request.getJobDescriptionId(), user.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Job description not found with id " + request.getJobDescriptionId() + " for current user"));
            session.setJobDescription(jd);
            session.setJdTargetRole(jd.getTargetRole());
            session.setJdCompanyName(jd.getCompanyName());
            session.setJdExtractedSkillsJson(jd.getExtractedSkillsJson());
            session.setJdKeyResponsibilitiesJson(jd.getKeyResponsibilitiesJson());
            session.setJdAiAnalysisSummary(jd.getAiAnalysisSummary());
        }

        InterviewSession savedSession = sessionRepository.save(session);
        return mapToResponse(savedSession);
    }

    @Transactional(readOnly = true)
    public List<InterviewSessionResponse> getUserSessions(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", userEmail));

        return sessionRepository.findByUserIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public InterviewSessionResponse getUserSessionById(String userEmail, Long sessionId) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", userEmail));

        InterviewSession session = sessionRepository.findByIdAndUserId(sessionId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("InterviewSession not found with id " + sessionId + " for current user"));

        return mapToResponse(session);
    }

    @Transactional
    public InterviewSessionResponse startSession(String userEmail, Long sessionId) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", userEmail));

        InterviewSession session = sessionRepository.findByIdAndUserId(sessionId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("InterviewSession not found with id " + sessionId + " for current user"));

        if (session.getStatus() == SessionStatus.COMPLETED) {
            throw new BadRequestException("Completed interview sessions cannot be restarted");
        }
        if (session.getStatus() == SessionStatus.ABANDONED) {
            throw new BadRequestException("Abandoned interview sessions cannot be restarted");
        }

        session.setStatus(SessionStatus.IN_PROGRESS);
        if (session.getStartedAt() == null) {
            session.setStartedAt(LocalDateTime.now());
        }

        InterviewSession updatedSession = sessionRepository.save(session);
        return mapToResponse(updatedSession);
    }

    @Transactional
    public InterviewSessionResponse abandonSession(String userEmail, Long sessionId) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", userEmail));

        InterviewSession session = sessionRepository.findByIdAndUserId(sessionId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("InterviewSession not found with id " + sessionId + " for current user"));

        if (session.getStatus() == SessionStatus.COMPLETED) {
            throw new BadRequestException("Cannot abandon a completed session");
        }

        session.setStatus(SessionStatus.ABANDONED);
        InterviewSession updatedSession = sessionRepository.save(session);
        return mapToResponse(updatedSession);
    }

    public InterviewSessionResponse mapToResponse(InterviewSession session) {
        InterviewSessionResponse resp = new InterviewSessionResponse(
                session.getId(),
                session.getCategory().getId(),
                session.getCategory().getName(),
                session.getDifficulty(),
                session.getStatus(),
                session.getMode(),
                session.getInteractionMode(),
                session.getQuestionCount(),
                session.getDurationMinutes(),
                session.getCreatedAt(),
                session.getStartedAt(),
                session.getCompletedAt()
        );

        if (session.getResume() != null) {
            resp.setResumeId(session.getResume().getId());
            resp.setResumeFileName(session.getResume().getFileName());
        }

        if (session.getProject() != null) {
            resp.setProjectId(session.getProject().getId());
            resp.setProjectTitle(session.getProject().getTitle());
        }

        if (session.getJobDescription() != null) {
            resp.setJobDescriptionId(session.getJobDescription().getId());
        }
        resp.setJdTargetRole(session.getJdTargetRole());
        resp.setJdCompanyName(session.getJdCompanyName());

        return resp;
    }
}
