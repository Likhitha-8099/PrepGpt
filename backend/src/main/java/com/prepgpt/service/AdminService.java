package com.prepgpt.service;

import com.prepgpt.dto.*;
import com.prepgpt.entity.*;
import com.prepgpt.exception.BadRequestException;
import com.prepgpt.exception.DuplicateResourceException;
import com.prepgpt.exception.ResourceNotFoundException;
import com.prepgpt.repository.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * AdminService — all write operations include audit log persistence within the same @Transactional scope.
 * Only this service writes to AdminAuditLogRepository.
 * No passwords or secrets are ever logged or included in audit detail.
 */
@Service
public class AdminService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final String ROLE_ADMIN = "ROLE_ADMIN";
    private static final String ROLE_STUDENT = "ROLE_STUDENT";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final InterviewSessionRepository sessionRepository;
    private final InterviewEvaluationRepository evaluationRepository;
    private final QuestionRepository questionRepository;
    private final CodingProblemRepository codingProblemRepository;
    private final JobDescriptionRepository jobDescriptionRepository;
    private final ResumeRepository resumeRepository;
    private final InterviewCategoryRepository categoryRepository;
    private final AdminAuditLogRepository auditLogRepository;

    public AdminService(UserRepository userRepository,
                        RoleRepository roleRepository,
                        InterviewSessionRepository sessionRepository,
                        InterviewEvaluationRepository evaluationRepository,
                        QuestionRepository questionRepository,
                        CodingProblemRepository codingProblemRepository,
                        JobDescriptionRepository jobDescriptionRepository,
                        ResumeRepository resumeRepository,
                        InterviewCategoryRepository categoryRepository,
                        AdminAuditLogRepository auditLogRepository) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.sessionRepository = sessionRepository;
        this.evaluationRepository = evaluationRepository;
        this.questionRepository = questionRepository;
        this.codingProblemRepository = codingProblemRepository;
        this.jobDescriptionRepository = jobDescriptionRepository;
        this.resumeRepository = resumeRepository;
        this.categoryRepository = categoryRepository;
        this.auditLogRepository = auditLogRepository;
    }

    // ──────────────────────────────────────────────────────────────────────
    // PLATFORM STATISTICS — all aggregate queries, no full dataset loading
    // ──────────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public AdminPlatformStatsResponse getPlatformStats() {
        long totalUsers = userRepository.count();
        long activeUsers = userRepository.countByIsActiveTrue();
        long inactiveUsers = totalUsers - activeUsers;

        long totalSessions = sessionRepository.count();
        long completedSessions = sessionRepository.countByStatus(SessionStatus.COMPLETED);
        long inProgressSessions = sessionRepository.countByStatus(SessionStatus.IN_PROGRESS);

        long totalQuestions = questionRepository.count();
        // Count active questions across all categories using a derived query
        long activeQuestions = questionRepository.countByActiveTrue();

        long totalCodingProblems = codingProblemRepository.count();
        long totalJobDescriptions = jobDescriptionRepository.count();
        long totalResumes = resumeRepository.count();

        Double averagePlatformScore = evaluationRepository.findAveragePlatformScore().orElse(null);

        return new AdminPlatformStatsResponse(
                totalUsers, activeUsers, inactiveUsers,
                totalSessions, completedSessions, inProgressSessions,
                totalQuestions, activeQuestions,
                totalCodingProblems, totalJobDescriptions,
                totalResumes, averagePlatformScore
        );
    }

    // ──────────────────────────────────────────────────────────────────────
    // USER MANAGEMENT
    // ──────────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public AdminUserListResponse getAllUsers(int page, int size) {
        validatePagination(page, size);
        Pageable pageable = PageRequest.of(page, size);
        Page<User> userPage = userRepository.findAllByOrderByCreatedAtDesc(pageable);

        List<AdminUserResponse> responses = userPage.getContent().stream()
                .map(this::mapToAdminUserResponse)
                .collect(Collectors.toList());

        return new AdminUserListResponse(
                responses,
                userPage.getTotalElements(),
                userPage.getTotalPages(),
                page, size
        );
    }

    @Transactional(readOnly = true)
    public AdminUserResponse getUserById(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        return mapToAdminUserResponse(user);
    }

    @Transactional
    public void deactivateUser(String adminEmail, Long targetUserId) {
        User adminUser = getAdminByEmail(adminEmail);
        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", targetUserId));

        // Self-protection
        if (adminUser.getId().equals(targetUserId)) {
            throw new BadRequestException("Administrators cannot deactivate their own account.");
        }

        // Last-active-admin invariant: serialize via role write lock + pessimistic row-level locking read
        if (ROLE_ADMIN.equals(targetUser.getRole().getName())) {
            roleRepository.findByNameWithLock(ROLE_ADMIN);
            List<User> activeAdmins = userRepository.findActiveAdminsForUpdate(ROLE_ADMIN);
            if (activeAdmins.size() <= 1) {
                throw new BadRequestException(
                        "Cannot deactivate the last active administrator. At least one active admin must always exist.");
            }
        }

        targetUser.setActive(false);
        userRepository.save(targetUser);

        writeAuditLog(adminEmail, "DEACTIVATE_USER", "USER", targetUserId,
                "{\"targetEmail\":\"" + targetUser.getEmail() + "\",\"role\":\"" + targetUser.getRole().getName() + "\"}");
    }

    @Transactional
    public void reactivateUser(String adminEmail, Long targetUserId) {
        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", targetUserId));

        targetUser.setActive(true);
        userRepository.save(targetUser);

        writeAuditLog(adminEmail, "REACTIVATE_USER", "USER", targetUserId,
                "{\"targetEmail\":\"" + targetUser.getEmail() + "\",\"role\":\"" + targetUser.getRole().getName() + "\"}");
    }

    @Transactional
    public AdminUserResponse changeUserRole(String adminEmail, Long targetUserId, String newRoleName) {
        User adminUser = getAdminByEmail(adminEmail);
        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", targetUserId));

        // Self-protection
        if (adminUser.getId().equals(targetUserId)) {
            throw new BadRequestException("Administrators cannot change their own role.");
        }

        // Validate the requested role exists in DB (rejects ROLE_HACKER, arbitrary strings, etc.)
        Role newRole = roleRepository.findByName(newRoleName)
                .orElseThrow(() -> new BadRequestException(
                        "Invalid role: '" + newRoleName + "'. Valid values: ROLE_STUDENT, ROLE_ADMIN."));

        String oldRoleName = targetUser.getRole().getName();

        // Last-active-admin invariant: block demotion of the last active admin with serialized locking
        if (ROLE_ADMIN.equals(oldRoleName) && ROLE_STUDENT.equals(newRoleName)) {
            roleRepository.findByNameWithLock(ROLE_ADMIN);
            List<User> activeAdmins = userRepository.findActiveAdminsForUpdate(ROLE_ADMIN);
            if (activeAdmins.size() <= 1) {
                throw new BadRequestException(
                        "Cannot demote the last active administrator. At least one active admin must always exist.");
            }
        }

        targetUser.setRole(newRole);
        User savedUser = userRepository.save(targetUser);

        // Audit detail explicitly records old and new role
        String detail = "{\"oldRole\":\"" + oldRoleName + "\",\"newRole\":\"" + newRoleName
                + "\",\"targetEmail\":\"" + targetUser.getEmail() + "\"}";
        writeAuditLog(adminEmail, "CHANGE_USER_ROLE", "USER", targetUserId, detail);

        return mapToAdminUserResponse(savedUser);
    }

    // ──────────────────────────────────────────────────────────────────────
    // QUESTION MANAGEMENT
    // ──────────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public AdminQuestionListResponse getAllQuestions(int page, int size, Long categoryId, String difficulty) {
        validatePagination(page, size);
        Pageable pageable = PageRequest.of(page, size);
        Page<Question> questionPage;

        if (categoryId != null && difficulty != null) {
            Difficulty diff = parseDifficulty(difficulty);
            questionPage = questionRepository.findByCategoryIdAndDifficultyOrderByCreatedAtDesc(categoryId, diff, pageable);
        } else if (categoryId != null) {
            questionPage = questionRepository.findByCategoryIdOrderByCreatedAtDesc(categoryId, pageable);
        } else if (difficulty != null) {
            Difficulty diff = parseDifficulty(difficulty);
            questionPage = questionRepository.findByDifficultyOrderByCreatedAtDesc(diff, pageable);
        } else {
            questionPage = questionRepository.findAllByOrderByCreatedAtDesc(pageable);
        }

        List<AdminQuestionResponse> responses = questionPage.getContent().stream()
                .map(this::mapToAdminQuestionResponse)
                .collect(Collectors.toList());

        return new AdminQuestionListResponse(
                responses, questionPage.getTotalElements(),
                questionPage.getTotalPages(), page, size
        );
    }

    @Transactional
    public AdminQuestionResponse createQuestion(String adminEmail, AdminQuestionRequest request) {
        InterviewCategory category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("InterviewCategory", "id", request.getCategoryId()));

        // Duplicate check: same text + same category → 409
        if (questionRepository.existsByCategoryIdAndQuestionText(request.getCategoryId(), request.getQuestionText())) {
            throw new DuplicateResourceException(
                    "A question with this text already exists in category '" + category.getName() + "'.");
        }

        Question question = new Question(
                category,
                request.getQuestionText(),
                request.getQuestionType(),
                request.getDifficulty(),
                request.getExpectedAnswerGuidance()
        );
        Question saved = questionRepository.save(question);

        writeAuditLog(adminEmail, "CREATE_QUESTION", "QUESTION", saved.getId(),
                "{\"categoryId\":" + category.getId() + ",\"difficulty\":\"" + request.getDifficulty() + "\"}");

        return mapToAdminQuestionResponse(saved);
    }

    @Transactional
    public AdminQuestionResponse updateQuestion(String adminEmail, Long questionId, AdminQuestionRequest request) {
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new ResourceNotFoundException("Question", "id", questionId));

        InterviewCategory category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("InterviewCategory", "id", request.getCategoryId()));

        // Duplicate check: if text or category changed, verify no existing question has that same text
        boolean textChanged = !question.getQuestionText().equals(request.getQuestionText())
                || !question.getCategory().getId().equals(request.getCategoryId());
        if (textChanged && questionRepository.existsByCategoryIdAndQuestionText(
                request.getCategoryId(), request.getQuestionText())) {
            throw new DuplicateResourceException(
                    "A question with this text already exists in the target category.");
        }

        question.setCategory(category);
        question.setQuestionText(request.getQuestionText());
        question.setQuestionType(request.getQuestionType());
        question.setDifficulty(request.getDifficulty());
        question.setExpectedAnswerGuidance(request.getExpectedAnswerGuidance());
        Question saved = questionRepository.save(question);

        writeAuditLog(adminEmail, "UPDATE_QUESTION", "QUESTION", questionId,
                "{\"categoryId\":" + category.getId() + ",\"difficulty\":\"" + request.getDifficulty() + "\"}");

        return mapToAdminQuestionResponse(saved);
    }

    @Transactional
    public void deactivateQuestion(String adminEmail, Long questionId) {
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new ResourceNotFoundException("Question", "id", questionId));
        question.setActive(false);
        questionRepository.save(question);
        writeAuditLog(adminEmail, "DEACTIVATE_QUESTION", "QUESTION", questionId,
                "{\"categoryId\":" + question.getCategory().getId() + "}");
    }

    @Transactional
    public void reactivateQuestion(String adminEmail, Long questionId) {
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new ResourceNotFoundException("Question", "id", questionId));
        question.setActive(true);
        questionRepository.save(question);
        writeAuditLog(adminEmail, "REACTIVATE_QUESTION", "QUESTION", questionId,
                "{\"categoryId\":" + question.getCategory().getId() + "}");
    }

    // ──────────────────────────────────────────────────────────────────────
    // SESSION OVERSIGHT — READ-ONLY
    // ──────────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public AdminSessionSummaryListResponse getAllSessions(int page, int size,
                                                          Long userId, String statusStr) {
        validatePagination(page, size);
        Pageable pageable = PageRequest.of(page, size);
        Page<InterviewSession> sessionPage;
        SessionStatus status = statusStr != null ? parseStatus(statusStr) : null;

        if (userId != null && status != null) {
            sessionPage = sessionRepository.findByUserIdAndStatusOrderByCreatedAtDesc(userId, status, pageable);
        } else if (userId != null) {
            sessionPage = sessionRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        } else if (status != null) {
            sessionPage = sessionRepository.findByStatusOrderByCreatedAtDesc(status, pageable);
        } else {
            sessionPage = sessionRepository.findAllByOrderByCreatedAtDesc(pageable);
        }

        List<AdminSessionSummaryResponse> responses = sessionPage.getContent().stream()
                .map(this::mapToAdminSessionSummary)
                .collect(Collectors.toList());

        return new AdminSessionSummaryListResponse(
                responses, sessionPage.getTotalElements(),
                sessionPage.getTotalPages(), page, size
        );
    }

    // ──────────────────────────────────────────────────────────────────────
    // AUDIT LOG — READ-ONLY VIA API
    // ──────────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public AdminAuditLogListResponse getAuditLog(int page, int size) {
        validatePagination(page, size);
        Pageable pageable = PageRequest.of(page, size);
        Page<com.prepgpt.entity.AdminAuditLog> logPage =
                auditLogRepository.findAllByOrderByPerformedAtDesc(pageable);

        List<AdminAuditLogResponse> responses = logPage.getContent().stream()
                .map(l -> new AdminAuditLogResponse(
                        l.getId(), l.getAdminEmail(), l.getAction(),
                        l.getTargetEntityType(), l.getTargetEntityId(),
                        l.getDetail(), l.getPerformedAt()))
                .collect(Collectors.toList());

        return new AdminAuditLogListResponse(
                responses, logPage.getTotalElements(),
                logPage.getTotalPages(), page, size
        );
    }

    // ──────────────────────────────────────────────────────────────────────
    // PRIVATE HELPERS
    // ──────────────────────────────────────────────────────────────────────

    private void validatePagination(int page, int size) {
        if (page < 0) {
            throw new BadRequestException("Page index cannot be negative. Requested: " + page);
        }
        if (size < 1) {
            throw new BadRequestException("Page size must be at least 1. Requested: " + size);
        }
        if (size > MAX_PAGE_SIZE) {
            throw new BadRequestException("Page size cannot exceed " + MAX_PAGE_SIZE + ". Requested: " + size);
        }
    }

    /**
     * Writes an audit log entry in the current @Transactional scope.
     * If this fails, the enclosing transaction rolls back, ensuring
     * business operations and audit records are always consistent.
     * NEVER include passwords or secrets in the detail string.
     */
    private void writeAuditLog(String adminEmail, String action,
                                String entityType, Long entityId, String detail) {
        AdminAuditLog entry = new AdminAuditLog(adminEmail, action, entityType, entityId, detail);
        auditLogRepository.save(entry);
    }

    private User getAdminByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
    }

    private AdminUserResponse mapToAdminUserResponse(User user) {
        long total = sessionRepository.countByUserId(user.getId());
        long completed = sessionRepository.countByUserIdAndStatus(user.getId(), SessionStatus.COMPLETED);
        return new AdminUserResponse(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getRole().getName(),
                user.isActive(),
                user.getCreatedAt(),
                user.getUpdatedAt(),
                total,
                completed
        );
    }

    private AdminQuestionResponse mapToAdminQuestionResponse(Question q) {
        return new AdminQuestionResponse(
                q.getId(),
                q.getCategory().getId(),
                q.getCategory().getName(),
                q.getQuestionText(),
                q.getQuestionType(),
                q.getDifficulty(),
                q.getExpectedAnswerGuidance(),
                q.isActive(),
                q.getCreatedAt(),
                q.getUpdatedAt()
        );
    }

    private AdminSessionSummaryResponse mapToAdminSessionSummary(InterviewSession session) {
        Double avgScore = evaluationRepository.findAverageScoreBySessionId(session.getId()).orElse(null);
        return new AdminSessionSummaryResponse(
                session.getId(),
                session.getUser().getId(),
                session.getUser().getEmail(),
                session.getUser().getFullName(),
                session.getCategory().getName(),
                session.getMode(),
                session.getDifficulty(),
                session.getStatus(),
                session.getCreatedAt(),
                session.getCompletedAt(),
                session.getQuestionCount(),
                avgScore
        );
    }

    private Difficulty parseDifficulty(String difficulty) {
        try {
            return Difficulty.valueOf(difficulty.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid difficulty value: " + difficulty);
        }
    }

    private SessionStatus parseStatus(String status) {
        try {
            return SessionStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid status value: " + status);
        }
    }
}
