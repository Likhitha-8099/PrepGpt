package com.prepgpt.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.prepgpt.dto.*;
import com.prepgpt.entity.*;
import com.prepgpt.exception.ResourceNotFoundException;
import com.prepgpt.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReportAnalyticsServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private InterviewSessionRepository sessionRepository;

    @Mock
    private InterviewQuestionRepository interviewQuestionRepository;

    @Mock
    private InterviewResponseRepository interviewResponseRepository;

    @Mock
    private InterviewEvaluationRepository interviewEvaluationRepository;

    private ObjectMapper objectMapper;
    private ReportAnalyticsService reportAnalyticsService;

    private User testUser;
    private User wrongUser;
    private InterviewCategory testCategory;
    private InterviewSession testSession;
    private Question testQuestion;
    private InterviewQuestion testIQ;
    private InterviewResponse testResponse;
    private InterviewEvaluation testEvaluation;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        reportAnalyticsService = new ReportAnalyticsService(
                userRepository,
                sessionRepository,
                interviewQuestionRepository,
                interviewResponseRepository,
                interviewEvaluationRepository,
                objectMapper
        );

        Role role = new Role("ROLE_STUDENT");
        testUser = new User("candidate@example.com", "password123", "Candidate User", role);
        testUser.setId(1L);

        wrongUser = new User("wrong@example.com", "password123", "Wrong User", role);
        wrongUser.setId(99L);

        testCategory = new InterviewCategory("Java Core", "Core Java Concepts", true);
        testCategory.setId(1L);

        testSession = new InterviewSession(testUser, testCategory, Difficulty.MEDIUM, 3, 30, InterviewMode.AI);
        testSession.setId(10L);
        testSession.setStatus(SessionStatus.COMPLETED);

        testQuestion = new Question(testCategory, "Explain Java Memory Model.", QuestionType.TECHNICAL, Difficulty.MEDIUM, "Heap vs Stack");
        testQuestion.setId(100L);

        testIQ = new InterviewQuestion(testSession, testQuestion, 1);
        testIQ.setId(1000L);

        testResponse = new InterviewResponse(testIQ, "Java memory consists of Heap and Stack.", InputMethod.TEXT);
        testResponse.setId(500L);

        testEvaluation = new InterviewEvaluation(
                testResponse, 8, 8, 9, 8, 8,
                "[\"Clear memory explanation\"]", "[\"Brief on GC pause times\"]",
                "Good answer", "Elaborate G1 GC", true, "Next question"
        );
        testEvaluation.setAdaptiveDecision(AdaptiveDecision.DEEP_DIVE);
        testEvaluation.setTargetConcept("Memory Management");
        testEvaluation.setUserFacingMessage("Great job!");
        testEvaluation.setNextDifficulty(Difficulty.HARD);
    }

    @Test
    @DisplayName("1. Get Detailed Completed Session Report")
    void testGetInterviewReport() {
        when(userRepository.findByEmail("candidate@example.com")).thenReturn(Optional.of(testUser));
        when(sessionRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(testSession));
        when(interviewQuestionRepository.findByInterviewSessionIdOrderByQuestionOrderAsc(10L)).thenReturn(List.of(testIQ));
        when(interviewResponseRepository.findByInterviewQuestionId(1000L)).thenReturn(Optional.of(testResponse));
        when(interviewEvaluationRepository.findByInterviewResponseId(500L)).thenReturn(Optional.of(testEvaluation));

        InterviewReportResponse report = reportAnalyticsService.getInterviewReport("candidate@example.com", 10L);

        assertNotNull(report);
        assertEquals(10L, report.getSessionId());
        assertEquals("Java Core", report.getCategoryName());
        assertEquals(8.0, report.getOverallScore());
        assertEquals("STRONG", report.getPerformanceLevel());
        assertEquals(1, report.getQuestions().size());
        assertEquals(AdaptiveDecision.DEEP_DIVE, report.getQuestions().get(0).getAdaptiveDecision());
        assertTrue(report.getKeyStrengths().contains("Clear memory explanation"));
    }

    @Test
    @DisplayName("2. Session ownership security rejection")
    void testGetInterviewReportOwnershipDenied() {
        when(userRepository.findByEmail("wrong@example.com")).thenReturn(Optional.of(wrongUser));
        when(sessionRepository.findByIdAndUserId(10L, 99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> {
            reportAnalyticsService.getInterviewReport("wrong@example.com", 10L);
        });
    }

    @Test
    @DisplayName("3. Get Analytics Overview across multiple sessions")
    void testGetAnalyticsOverview() {
        when(userRepository.findByEmail("candidate@example.com")).thenReturn(Optional.of(testUser));
        when(sessionRepository.findByUserIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(testSession));
        when(interviewQuestionRepository.findByInterviewSessionIdOrderByQuestionOrderAsc(10L)).thenReturn(List.of(testIQ));
        when(interviewResponseRepository.findByInterviewQuestionId(1000L)).thenReturn(Optional.of(testResponse));
        when(interviewEvaluationRepository.findByInterviewResponseId(500L)).thenReturn(Optional.of(testEvaluation));

        AnalyticsOverviewResponse analytics = reportAnalyticsService.getAnalyticsOverview("candidate@example.com");

        assertNotNull(analytics);
        assertEquals(1L, analytics.getTotalInterviews());
        assertEquals(1L, analytics.getCompletedInterviews());
        assertEquals(8.0, analytics.getAverageOverallScore());
        assertEquals(1, analytics.getScoreTrend().size());
        assertTrue(analytics.getStrongestCategories().contains("Java Core"));
    }

    @Test
    @DisplayName("4. Get Category Analytics Breakdown")
    void testGetCategoryAnalytics() {
        when(userRepository.findByEmail("candidate@example.com")).thenReturn(Optional.of(testUser));
        when(sessionRepository.findByUserIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(testSession));
        when(interviewQuestionRepository.findByInterviewSessionIdOrderByQuestionOrderAsc(10L)).thenReturn(List.of(testIQ));
        when(interviewResponseRepository.findByInterviewQuestionId(1000L)).thenReturn(Optional.of(testResponse));
        when(interviewEvaluationRepository.findByInterviewResponseId(500L)).thenReturn(Optional.of(testEvaluation));

        List<CategoryAnalyticsResponse> categoryAnalytics = reportAnalyticsService.getCategoryAnalytics("candidate@example.com");

        assertNotNull(categoryAnalytics);
        assertEquals(1, categoryAnalytics.size());
        assertEquals("Java Core", categoryAnalytics.get(0).getCategoryName());
        assertEquals(8.0, categoryAnalytics.get(0).getAverageScore());
        assertEquals("STRONG", categoryAnalytics.get(0).getPerformanceLevel());
    }

    @Test
    @DisplayName("5. Get Detailed Completed Project Session Report with Project Metadata")
    void testGetInterviewReport_ProjectSession() {
        Project testProject = new Project(testUser, "Distributed LRU Cache", "In-memory caching system", "Java, Spring Boot, Redis", "Lead Developer");
        testProject.setId(42L);

        InterviewSession projectSession = new InterviewSession(testUser, testCategory, Difficulty.MEDIUM, 3, 30, InterviewMode.PROJECT);
        projectSession.setId(20L);
        projectSession.setStatus(SessionStatus.COMPLETED);
        projectSession.setProject(testProject);
        projectSession.setProjectTitleSnapshot("Distributed LRU Cache");

        InterviewQuestion projectIQ = new InterviewQuestion(projectSession, testQuestion, 1);
        projectIQ.setId(2000L);

        InterviewResponse projectResponse = new InterviewResponse(projectIQ, "Redis cache eviction implemented using LRU algorithm.", InputMethod.TEXT);
        projectResponse.setId(600L);

        InterviewEvaluation projectEvaluation = new InterviewEvaluation(
                projectResponse, 9, 9, 8, 9, 9,
                "[\"Excellent architecture knowledge\", \"Accurate Redis LRU details\"]",
                "[\"Could mention cache stampede mitigation\"]",
                "Great project technical answer", "Explore write-through caching", true, "Next"
        );

        when(userRepository.findByEmail("candidate@example.com")).thenReturn(Optional.of(testUser));
        when(sessionRepository.findByIdAndUserId(20L, 1L)).thenReturn(Optional.of(projectSession));
        when(interviewQuestionRepository.findByInterviewSessionIdOrderByQuestionOrderAsc(20L)).thenReturn(List.of(projectIQ));
        when(interviewResponseRepository.findByInterviewQuestionId(2000L)).thenReturn(Optional.of(projectResponse));
        when(interviewEvaluationRepository.findByInterviewResponseId(600L)).thenReturn(Optional.of(projectEvaluation));

        InterviewReportResponse report = reportAnalyticsService.getInterviewReport("candidate@example.com", 20L);

        assertNotNull(report);
        assertEquals(20L, report.getSessionId());
        assertEquals(InterviewMode.PROJECT, report.getMode());
        assertEquals(42L, report.getProjectId());
        assertEquals("Distributed LRU Cache", report.getProjectTitle());
        assertEquals(9.0, report.getOverallScore());
        assertEquals(9.0, report.getTechnicalAccuracyScore());
        assertEquals("EXCELLENT", report.getPerformanceLevel());
        assertEquals(1, report.getQuestions().size());
        assertTrue(report.getKeyStrengths().contains("Excellent architecture knowledge"));
    }

    @Test
    @DisplayName("6. Project Session ownership security rejection - User A cannot access User B's project report")
    void testGetInterviewReport_ProjectSession_OwnershipRejected() {
        when(userRepository.findByEmail("wrong@example.com")).thenReturn(Optional.of(wrongUser));
        when(sessionRepository.findByIdAndUserId(20L, 99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> {
            reportAnalyticsService.getInterviewReport("wrong@example.com", 20L);
        });
    }
}
