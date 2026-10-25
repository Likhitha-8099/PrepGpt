package com.prepgpt.service;

import com.prepgpt.dto.*;
import com.prepgpt.entity.*;
import com.prepgpt.exception.BadRequestException;
import com.prepgpt.exception.DuplicateResourceException;
import com.prepgpt.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminService Unit Tests")
class AdminServiceTest {

    @Mock UserRepository userRepository;
    @Mock RoleRepository roleRepository;
    @Mock InterviewSessionRepository sessionRepository;
    @Mock InterviewEvaluationRepository evaluationRepository;
    @Mock QuestionRepository questionRepository;
    @Mock CodingProblemRepository codingProblemRepository;
    @Mock JobDescriptionRepository jobDescriptionRepository;
    @Mock ResumeRepository resumeRepository;
    @Mock InterviewCategoryRepository categoryRepository;
    @Mock AdminAuditLogRepository auditLogRepository;

    @InjectMocks AdminService adminService;

    private Role adminRole;
    private Role studentRole;
    private User adminUser;
    private User studentUser;

    @BeforeEach
    void setUp() {
        adminRole = new Role("ROLE_ADMIN");
        studentRole = new Role("ROLE_STUDENT");

        adminUser = new User("admin@test.com", "hash", "Admin User", adminRole);
        adminUser.setId(1L);

        studentUser = new User("student@test.com", "hash", "Student User", studentRole);
        studentUser.setId(2L);
    }

    // ── 1. GET ALL USERS — PAGINATION ────────────────────────────────────

    @Test
    @DisplayName("01: getAllUsers: returns correct paginated slice")
    void getAllUsers_pagination() {
        Page<User> userPage = new PageImpl<>(List.of(studentUser));
        when(userRepository.findAllByOrderByCreatedAtDesc(any(Pageable.class))).thenReturn(userPage);
        when(sessionRepository.countByUserId(2L)).thenReturn(5L);
        when(sessionRepository.countByUserIdAndStatus(2L, SessionStatus.COMPLETED)).thenReturn(3L);

        AdminUserListResponse result = adminService.getAllUsers(0, 20);

        assertThat(result.getUsers()).hasSize(1);
        assertThat(result.getTotalElements()).isEqualTo(1L);
        assertThat(result.getPage()).isEqualTo(0);
        assertThat(result.getSize()).isEqualTo(20);
        verify(userRepository).findAllByOrderByCreatedAtDesc(any(Pageable.class));
    }

    // ── 2. GET ALL USERS — SESSION COUNT ENRICHMENT ───────────────────────

    @Test
    @DisplayName("02: getAllUsers: session count enrichment invokes countByUserId and countByUserIdAndStatus")
    void getAllUsers_sessionCountEnrichment() {
        Page<User> userPage = new PageImpl<>(List.of(studentUser));
        when(userRepository.findAllByOrderByCreatedAtDesc(any(Pageable.class))).thenReturn(userPage);
        when(sessionRepository.countByUserId(2L)).thenReturn(8L);
        when(sessionRepository.countByUserIdAndStatus(2L, SessionStatus.COMPLETED)).thenReturn(4L);

        AdminUserListResponse result = adminService.getAllUsers(0, 10);

        verify(sessionRepository).countByUserId(2L);
        verify(sessionRepository).countByUserIdAndStatus(2L, SessionStatus.COMPLETED);
        verify(sessionRepository, never()).findAll();
        assertThat(result.getUsers().get(0).getTotalSessions()).isEqualTo(8L);
        assertThat(result.getUsers().get(0).getCompletedSessions()).isEqualTo(4L);
    }

    // ── 3. DEACTIVATE USER — NORMAL STUDENT ──────────────────────────────

    @Test
    @DisplayName("03: deactivateUser: normal student → isActive=false, audit written")
    void deactivateUser_normalStudent_success() {
        when(userRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(adminUser));
        when(userRepository.findById(2L)).thenReturn(Optional.of(studentUser));

        adminService.deactivateUser("admin@test.com", 2L);

        assertThat(studentUser.isActive()).isFalse();
        verify(userRepository).save(studentUser);
        verify(auditLogRepository).save(any(AdminAuditLog.class));
    }

    // ── 4. DEACTIVATE USER — SELF DEACTIVATION ────────────────────────────

    @Test
    @DisplayName("04: deactivateUser: self-deactivation → BadRequestException")
    void deactivateUser_selfDeactivation_throws() {
        when(userRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(adminUser));
        when(userRepository.findById(1L)).thenReturn(Optional.of(adminUser));

        assertThatThrownBy(() -> adminService.deactivateUser("admin@test.com", 1L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("cannot deactivate their own account");

        verify(userRepository, never()).save(any());
        verify(auditLogRepository, never()).save(any());
    }

    // ── 5. DEACTIVATE USER — LAST ACTIVE ADMIN ────────────────────────────

    @Test
    @DisplayName("05: deactivateUser: last active admin → BadRequestException with serialized lock")
    void deactivateUser_lastActiveAdmin_throws() {
        User otherAdmin = new User("other@test.com", "hash", "Other Admin", adminRole);
        otherAdmin.setId(3L);

        when(userRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(adminUser));
        when(userRepository.findById(3L)).thenReturn(Optional.of(otherAdmin));
        when(roleRepository.findByNameWithLock("ROLE_ADMIN")).thenReturn(Optional.of(adminRole));
        when(userRepository.findActiveAdminsForUpdate("ROLE_ADMIN")).thenReturn(List.of(otherAdmin));

        assertThatThrownBy(() -> adminService.deactivateUser("admin@test.com", 3L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("last active administrator");

        verify(userRepository, never()).save(any());
        verify(auditLogRepository, never()).save(any());
    }

    // ── 6. DEACTIVATE USER — NON-LAST ADMIN ───────────────────────────────

    @Test
    @DisplayName("06: deactivateUser: non-last admin → succeeds under lock")
    void deactivateUser_nonLastAdmin_success() {
        User otherAdmin = new User("other@test.com", "hash", "Other Admin", adminRole);
        otherAdmin.setId(3L);

        when(userRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(adminUser));
        when(userRepository.findById(3L)).thenReturn(Optional.of(otherAdmin));
        when(roleRepository.findByNameWithLock("ROLE_ADMIN")).thenReturn(Optional.of(adminRole));
        when(userRepository.findActiveAdminsForUpdate("ROLE_ADMIN")).thenReturn(List.of(otherAdmin, adminUser));

        adminService.deactivateUser("admin@test.com", 3L);

        assertThat(otherAdmin.isActive()).isFalse();
        verify(userRepository).save(otherAdmin);
        verify(auditLogRepository).save(any(AdminAuditLog.class));
    }

    // ── 7. REACTIVATE USER ───────────────────────────────────────────────

    @Test
    @DisplayName("07: reactivateUser: inactive user → isActive=true, audit written")
    void reactivateUser_success() {
        studentUser.setActive(false);
        when(userRepository.findById(2L)).thenReturn(Optional.of(studentUser));

        adminService.reactivateUser("admin@test.com", 2L);

        assertThat(studentUser.isActive()).isTrue();
        verify(userRepository).save(studentUser);
        verify(auditLogRepository).save(any(AdminAuditLog.class));
    }

    // ── 8. CHANGE USER ROLE — VALID PROMOTION ─────────────────────────────

    @Test
    @DisplayName("08: changeUserRole: valid promotion to ROLE_ADMIN → audit contains oldRole and newRole")
    void changeUserRole_validPromotion_auditHasOldAndNewRole() {
        when(userRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(adminUser));
        when(userRepository.findById(2L)).thenReturn(Optional.of(studentUser));
        when(roleRepository.findByName("ROLE_ADMIN")).thenReturn(Optional.of(adminRole));
        when(userRepository.save(any())).thenReturn(studentUser);
        when(sessionRepository.countByUserId(anyLong())).thenReturn(0L);
        when(sessionRepository.countByUserIdAndStatus(anyLong(), any())).thenReturn(0L);

        adminService.changeUserRole("admin@test.com", 2L, "ROLE_ADMIN");

        verify(auditLogRepository).save(argThat(log ->
                log.getDetail().contains("ROLE_STUDENT") &&
                log.getDetail().contains("ROLE_ADMIN") &&
                log.getAction().equals("CHANGE_USER_ROLE")
        ));
    }

    // ── 9. CHANGE USER ROLE — INVALID ROLE NAME ──────────────────────────

    @Test
    @DisplayName("09: changeUserRole: invalid role name → BadRequestException")
    void changeUserRole_invalidRole_throws() {
        when(userRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(adminUser));
        when(userRepository.findById(2L)).thenReturn(Optional.of(studentUser));
        when(roleRepository.findByName("ROLE_HACKER")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminService.changeUserRole("admin@test.com", 2L, "ROLE_HACKER"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Invalid role");

        verify(auditLogRepository, never()).save(any());
    }

    // ── 10. CHANGE USER ROLE — OWN ACCOUNT ────────────────────────────────

    @Test
    @DisplayName("10: changeUserRole: self role-change → BadRequestException")
    void changeUserRole_selfChange_throws() {
        when(userRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(adminUser));
        when(userRepository.findById(1L)).thenReturn(Optional.of(adminUser));

        assertThatThrownBy(() -> adminService.changeUserRole("admin@test.com", 1L, "ROLE_STUDENT"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("cannot change their own role");

        verify(auditLogRepository, never()).save(any());
    }

    // ── 11. CHANGE USER ROLE — DEMOTE LAST ADMIN ─────────────────────────

    @Test
    @DisplayName("11: changeUserRole: demote last active admin → BadRequestException with serialized lock")
    void changeUserRole_demoteLastAdmin_throws() {
        User soleAdmin = new User("sole@test.com", "hash", "Sole Admin", adminRole);
        soleAdmin.setId(5L);

        when(userRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(adminUser));
        when(userRepository.findById(5L)).thenReturn(Optional.of(soleAdmin));
        when(roleRepository.findByName("ROLE_STUDENT")).thenReturn(Optional.of(studentRole));
        when(roleRepository.findByNameWithLock("ROLE_ADMIN")).thenReturn(Optional.of(adminRole));
        when(userRepository.findActiveAdminsForUpdate("ROLE_ADMIN")).thenReturn(List.of(soleAdmin));

        assertThatThrownBy(() -> adminService.changeUserRole("admin@test.com", 5L, "ROLE_STUDENT"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("last active administrator");

        verify(auditLogRepository, never()).save(any());
    }

    // ── 12. CHANGE USER ROLE — DEMOTE NON-LAST ADMIN ─────────────────────

    @Test
    @DisplayName("12: changeUserRole: demote non-last admin → succeeds under lock")
    void changeUserRole_demoteNonLastAdmin_success() {
        User otherAdmin = new User("other@test.com", "hash", "Other Admin", adminRole);
        otherAdmin.setId(4L);

        when(userRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(adminUser));
        when(userRepository.findById(4L)).thenReturn(Optional.of(otherAdmin));
        when(roleRepository.findByName("ROLE_STUDENT")).thenReturn(Optional.of(studentRole));
        when(roleRepository.findByNameWithLock("ROLE_ADMIN")).thenReturn(Optional.of(adminRole));
        when(userRepository.findActiveAdminsForUpdate("ROLE_ADMIN")).thenReturn(List.of(otherAdmin, adminUser));
        when(userRepository.save(any())).thenReturn(otherAdmin);
        when(sessionRepository.countByUserId(anyLong())).thenReturn(0L);
        when(sessionRepository.countByUserIdAndStatus(anyLong(), any())).thenReturn(0L);

        adminService.changeUserRole("admin@test.com", 4L, "ROLE_STUDENT");

        verify(userRepository).save(otherAdmin);
        verify(auditLogRepository).save(any(AdminAuditLog.class));
    }

    // ── 13. PLATFORM STATS — AGGREGATE QUERIES ───────────────────────────

    @Test
    @DisplayName("13: getPlatformStats: uses aggregate queries only — no findAll() for sessions/evaluations")
    void getPlatformStats_usesAggregateQueries() {
        when(userRepository.count()).thenReturn(50L);
        when(userRepository.countByIsActiveTrue()).thenReturn(48L);
        when(sessionRepository.count()).thenReturn(200L);
        when(sessionRepository.countByStatus(SessionStatus.COMPLETED)).thenReturn(150L);
        when(sessionRepository.countByStatus(SessionStatus.IN_PROGRESS)).thenReturn(10L);
        when(questionRepository.count()).thenReturn(500L);
        when(questionRepository.countByActiveTrue()).thenReturn(480L);
        when(codingProblemRepository.count()).thenReturn(50L);
        when(jobDescriptionRepository.count()).thenReturn(30L);
        when(resumeRepository.count()).thenReturn(45L);
        when(evaluationRepository.findAveragePlatformScore()).thenReturn(Optional.of(72.5));

        AdminPlatformStatsResponse stats = adminService.getPlatformStats();

        assertThat(stats.getTotalUsers()).isEqualTo(50L);
        assertThat(stats.getActiveUsers()).isEqualTo(48L);
        assertThat(stats.getInactiveUsers()).isEqualTo(2L);
        assertThat(stats.getCompletedSessions()).isEqualTo(150L);
        assertThat(stats.getAveragePlatformScore()).isEqualTo(72.5);

        // Verify no findAll() calls were made for large datasets
        verify(sessionRepository, never()).findAll();
        verify(evaluationRepository, never()).findAll();
        verify(userRepository, never()).findAll();
    }

    // ── 14. CREATE QUESTION — NEW QUESTION ───────────────────────────────

    @Test
    @DisplayName("14: createQuestion: new question → created and audit written")
    void createQuestion_success() {
        InterviewCategory category = new InterviewCategory("Tech", "desc", true);

        AdminQuestionRequest req = new AdminQuestionRequest();
        req.setCategoryId(1L);
        req.setQuestionText("What is polymorphism?");
        req.setQuestionType(QuestionType.TECHNICAL);
        req.setDifficulty(Difficulty.MEDIUM);

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(questionRepository.existsByCategoryIdAndQuestionText(1L, "What is polymorphism?")).thenReturn(false);
        Question saved = new Question(category, "What is polymorphism?", QuestionType.TECHNICAL, Difficulty.MEDIUM, null);
        when(questionRepository.save(any())).thenReturn(saved);

        AdminQuestionResponse response = adminService.createQuestion("admin@test.com", req);

        assertThat(response.getQuestionText()).isEqualTo("What is polymorphism?");
        verify(auditLogRepository).save(any(AdminAuditLog.class));
    }

    // ── 15. CREATE QUESTION — DUPLICATE ──────────────────────────────────

    @Test
    @DisplayName("15: createQuestion: duplicate text+category → DuplicateResourceException")
    void createQuestion_duplicate_throws() {
        InterviewCategory category = new InterviewCategory("Tech", "desc", true);

        AdminQuestionRequest req = new AdminQuestionRequest();
        req.setCategoryId(1L);
        req.setQuestionText("What is polymorphism?");
        req.setQuestionType(QuestionType.TECHNICAL);
        req.setDifficulty(Difficulty.MEDIUM);

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(questionRepository.existsByCategoryIdAndQuestionText(1L, "What is polymorphism?")).thenReturn(true);

        assertThatThrownBy(() -> adminService.createQuestion("admin@test.com", req))
                .isInstanceOf(DuplicateResourceException.class);

        verify(questionRepository, never()).save(any());
        verify(auditLogRepository, never()).save(any());
    }

    // ── 16. DEACTIVATE QUESTION ──────────────────────────────────────────

    @Test
    @DisplayName("16: deactivateQuestion: sets active=false and writes audit log")
    void deactivateQuestion_success() {
        InterviewCategory category = new InterviewCategory("Tech", "desc", true);
        Question question = new Question(category, "What is OOP?", QuestionType.TECHNICAL, Difficulty.EASY, null);
        question.setId(10L);
        question.setActive(true);

        when(questionRepository.findById(10L)).thenReturn(Optional.of(question));

        adminService.deactivateQuestion("admin@test.com", 10L);

        assertThat(question.isActive()).isFalse();
        verify(questionRepository).save(question);
        verify(auditLogRepository).save(argThat(log -> log.getAction().equals("DEACTIVATE_QUESTION")));
    }

    // ── 17. REACTIVATE QUESTION ──────────────────────────────────────────

    @Test
    @DisplayName("17: reactivateQuestion: sets active=true and writes audit log")
    void reactivateQuestion_success() {
        InterviewCategory category = new InterviewCategory("Tech", "desc", true);
        Question question = new Question(category, "What is OOP?", QuestionType.TECHNICAL, Difficulty.EASY, null);
        question.setId(10L);
        question.setActive(false);

        when(questionRepository.findById(10L)).thenReturn(Optional.of(question));

        adminService.reactivateQuestion("admin@test.com", 10L);

        assertThat(question.isActive()).isTrue();
        verify(questionRepository).save(question);
        verify(auditLogRepository).save(argThat(log -> log.getAction().equals("REACTIVATE_QUESTION")));
    }

    // ── 18. GET ALL SESSIONS — NO FILTER ─────────────────────────────────

    @Test
    @DisplayName("18: getAllSessions: paginated correctly with no filter, uses sessionRepository not findAll")
    void getAllSessions_noFilter_paginatedCorrectly() {
        Page<InterviewSession> sessionPage = new PageImpl<>(List.of());
        when(sessionRepository.findAllByOrderByCreatedAtDesc(any(Pageable.class))).thenReturn(sessionPage);

        AdminSessionSummaryListResponse result = adminService.getAllSessions(0, 20, null, null);

        assertThat(result.getSessions()).isEmpty();
        verify(sessionRepository).findAllByOrderByCreatedAtDesc(any(Pageable.class));
        verify(sessionRepository, never()).findAll();
    }

    // ── 19. GET ALL SESSIONS — USERID FILTER ─────────────────────────────

    @Test
    @DisplayName("19: getAllSessions: userId filter returns only that user's sessions")
    void getAllSessions_userIdFilter_returnsOnlyThatUsersSessions() {
        Page<InterviewSession> userSessions = new PageImpl<>(List.of());
        when(sessionRepository.findByUserIdOrderByCreatedAtDesc(eq(2L), any(Pageable.class))).thenReturn(userSessions);

        AdminSessionSummaryListResponse result = adminService.getAllSessions(0, 20, 2L, null);

        assertThat(result.getSessions()).isEmpty();
        verify(sessionRepository).findByUserIdOrderByCreatedAtDesc(eq(2L), any(Pageable.class));
        verify(sessionRepository, never()).findAllByOrderByCreatedAtDesc(any(Pageable.class));
        verify(sessionRepository, never()).findAll();
    }

    // ── 20. GET AUDIT LOG — OVERSIZED PAGE ───────────────────────────────

    @Test
    @DisplayName("20: getAuditLog: page size > 100 → BadRequestException")
    void getAuditLog_oversizedPage_throws() {
        assertThatThrownBy(() -> adminService.getAuditLog(0, 101))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("100");
    }
}
