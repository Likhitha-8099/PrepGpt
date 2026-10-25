package com.prepgpt.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.prepgpt.ai.AIService;
import com.prepgpt.ai.MockAIService;
import com.prepgpt.dto.*;
import com.prepgpt.entity.*;
import com.prepgpt.exception.AIServiceUnavailableException;
import com.prepgpt.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InterviewAIServiceTest {

    @Mock
    private InterviewSessionRepository sessionRepository;

    @Mock
    private QuestionRepository questionRepository;

    @Mock
    private InterviewQuestionRepository interviewQuestionRepository;

    @Mock
    private InterviewResponseRepository interviewResponseRepository;

    @Mock
    private InterviewEvaluationRepository interviewEvaluationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AIService failingAiService;

    private MockAIService mockAIService;
    private ObjectMapper objectMapper;
    private InterviewAIService interviewAIService;

    private User testUser;
    private InterviewCategory testCategory;
    private InterviewSession testSession;

    @BeforeEach
    void setUp() {
        mockAIService = new MockAIService();
        objectMapper = new ObjectMapper();
        interviewAIService = new InterviewAIService(
                sessionRepository,
                questionRepository,
                interviewQuestionRepository,
                interviewResponseRepository,
                interviewEvaluationRepository,
                userRepository,
                mockAIService,
                objectMapper
        );

        Role role = new Role("ROLE_STUDENT");
        testUser = new User("student@example.com", "password", "Student Test", role);
        testUser.setId(1L);

        testCategory = new InterviewCategory("Technical Interview", "Technical description", true);
        testCategory.setId(10L);

        testSession = new InterviewSession(testUser, testCategory, Difficulty.MEDIUM, 3, 15, InterviewMode.AI);
        testSession.setId(100L);
    }

    @Test
    void startAISession_GeneratesAndPersistsQuestion1() {
        when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));
        when(sessionRepository.findByIdAndUserId(testSession.getId(), testUser.getId())).thenReturn(Optional.of(testSession));
        when(interviewQuestionRepository.findByInterviewSessionIdOrderByQuestionOrderAsc(testSession.getId())).thenReturn(Collections.emptyList());

        when(questionRepository.save(any(Question.class))).thenAnswer(inv -> {
            Question q = inv.getArgument(0);
            q.setId(50L);
            return q;
        });

        when(interviewQuestionRepository.save(any(InterviewQuestion.class))).thenAnswer(inv -> {
            InterviewQuestion iq = inv.getArgument(0);
            iq.setId(500L);
            return iq;
        });

        InterviewSessionResponse response = interviewAIService.startAISession(testUser.getEmail(), testSession.getId());

        assertNotNull(response);
        assertEquals(SessionStatus.IN_PROGRESS, response.getStatus());
        assertEquals(InterviewMode.AI, response.getMode());
        verify(questionRepository, times(1)).save(any(Question.class));
        verify(interviewQuestionRepository, times(1)).save(any(InterviewQuestion.class));
    }

    @Test
    void submitAIAnswer_EvaluatesAndPersistsResponseAndEvaluation() {
        testSession.setStatus(SessionStatus.IN_PROGRESS);

        Question q = new Question(testCategory, "AI Question Text", QuestionType.TECHNICAL, Difficulty.MEDIUM, "Guidance");
        q.setId(50L);

        InterviewQuestion iq = new InterviewQuestion(testSession, q, 1);
        iq.setId(500L);

        when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));
        when(sessionRepository.findByIdAndUserId(testSession.getId(), testUser.getId())).thenReturn(Optional.of(testSession));
        when(interviewQuestionRepository.findById(iq.getId())).thenReturn(Optional.of(iq));
        when(interviewResponseRepository.findByInterviewQuestionId(iq.getId())).thenReturn(Optional.empty());

        when(interviewResponseRepository.save(any(InterviewResponse.class))).thenAnswer(inv -> {
            InterviewResponse resp = inv.getArgument(0);
            resp.setId(1000L);
            return resp;
        });

        when(interviewEvaluationRepository.save(any(InterviewEvaluation.class))).thenAnswer(inv -> inv.getArgument(0));

        SubmitAnswerRequest request = new SubmitAnswerRequest("Object-oriented programming is structured around classes and objects incorporating inheritance and encapsulation.");
        AnswerResponse response = interviewAIService.submitAIAnswer(testUser.getEmail(), testSession.getId(), iq.getId(), request);

        assertNotNull(response);
        assertTrue(response.getSuccess());
        assertNotNull(response.getEvaluation());
        assertTrue(response.getEvaluation().getScore() >= 6);
        assertNotNull(response.getEvaluation().getFeedback());

        verify(interviewEvaluationRepository, times(1)).save(any(InterviewEvaluation.class));
    }

    @Test
    void submitAIAnswer_AnswerSurvivesWhenAIFails() {
        InterviewAIService failingService = new InterviewAIService(
                sessionRepository,
                questionRepository,
                interviewQuestionRepository,
                interviewResponseRepository,
                interviewEvaluationRepository,
                userRepository,
                failingAiService,
                objectMapper
        );

        testSession.setStatus(SessionStatus.IN_PROGRESS);

        Question q = new Question(testCategory, "AI Question Text", QuestionType.TECHNICAL, Difficulty.MEDIUM, "Guidance");
        q.setId(50L);

        InterviewQuestion iq = new InterviewQuestion(testSession, q, 1);
        iq.setId(500L);

        when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));
        when(sessionRepository.findByIdAndUserId(testSession.getId(), testUser.getId())).thenReturn(Optional.of(testSession));
        when(interviewQuestionRepository.findById(iq.getId())).thenReturn(Optional.of(iq));
        when(interviewResponseRepository.findByInterviewQuestionId(iq.getId())).thenReturn(Optional.empty());

        when(interviewResponseRepository.save(any(InterviewResponse.class))).thenAnswer(inv -> {
            InterviewResponse resp = inv.getArgument(0);
            resp.setId(1000L);
            return resp;
        });

        when(failingAiService.evaluateAnswer(any())).thenThrow(new AIServiceUnavailableException("AI provider network failure"));

        SubmitAnswerRequest request = new SubmitAnswerRequest("Valid candidate answer text");

        assertThrows(AIServiceUnavailableException.class, () ->
                failingService.submitAIAnswer(testUser.getEmail(), testSession.getId(), iq.getId(), request)
        );

        // Verify that candidate answer was STILL saved to repository!
        verify(interviewResponseRepository, times(1)).save(any(InterviewResponse.class));
    }

    @Test
    void submitAIAnswer_EvaluationIsIdempotent_DoesNotReCallAIIfEvaluationExists() {
        testSession.setStatus(SessionStatus.IN_PROGRESS);

        Question q = new Question(testCategory, "AI Question Text", QuestionType.TECHNICAL, Difficulty.MEDIUM, "Guidance");
        q.setId(50L);

        InterviewQuestion iq = new InterviewQuestion(testSession, q, 1);
        iq.setId(500L);

        InterviewResponse existingResp = new InterviewResponse(iq, "Existing Answer");
        existingResp.setId(1000L);

        InterviewEvaluation existingEval = new InterviewEvaluation(existingResp, 8, 8, 8, 8, 8, "[]", "[]", "Feedback", "Suggestion", false, null);
        existingEval.setId(2000L);

        when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));
        when(sessionRepository.findByIdAndUserId(testSession.getId(), testUser.getId())).thenReturn(Optional.of(testSession));
        when(interviewQuestionRepository.findById(iq.getId())).thenReturn(Optional.of(iq));
        when(interviewResponseRepository.findByInterviewQuestionId(iq.getId())).thenReturn(Optional.of(existingResp));
        when(interviewEvaluationRepository.findByInterviewResponseId(existingResp.getId())).thenReturn(Optional.of(existingEval));

        SubmitAnswerRequest request = new SubmitAnswerRequest("Existing Answer");
        AnswerResponse response = interviewAIService.submitAIAnswer(testUser.getEmail(), testSession.getId(), iq.getId(), request);

        assertNotNull(response);
        assertEquals(8, response.getEvaluation().getScore());

        // Verify that aiService was NOT called because evaluation already existed!
        verify(interviewEvaluationRepository, never()).save(any());
    }

    @Test
    void getCurrentAIQuestion_EnforcesMaxQuestionCountLimit() {
        testSession.setStatus(SessionStatus.IN_PROGRESS);

        Question q1 = new Question(testCategory, "Q1", QuestionType.TECHNICAL, Difficulty.MEDIUM, "G1");
        Question q2 = new Question(testCategory, "Q2", QuestionType.TECHNICAL, Difficulty.MEDIUM, "G2");
        Question q3 = new Question(testCategory, "Q3", QuestionType.TECHNICAL, Difficulty.MEDIUM, "G3");

        InterviewQuestion iq1 = new InterviewQuestion(testSession, q1, 1); iq1.setId(101L);
        InterviewQuestion iq2 = new InterviewQuestion(testSession, q2, 2); iq2.setId(102L);
        InterviewQuestion iq3 = new InterviewQuestion(testSession, q3, 3); iq3.setId(103L);

        List<InterviewQuestion> assigned = List.of(iq1, iq2, iq3);

        when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));
        when(sessionRepository.findByIdAndUserId(testSession.getId(), testUser.getId())).thenReturn(Optional.of(testSession));
        when(interviewQuestionRepository.findByInterviewSessionIdOrderByQuestionOrderAsc(testSession.getId())).thenReturn(assigned);
        when(interviewResponseRepository.existsByInterviewQuestionId(anyLong())).thenReturn(true);

        QuestionResponse response = interviewAIService.getCurrentAIQuestion(testUser.getEmail(), testSession.getId());

        assertNotNull(response);
        assertTrue(response.getAnswered());
        assertEquals(100, response.getProgress());
        assertEquals(SessionStatus.COMPLETED, testSession.getStatus());

        // Verify NO 4th question is generated because max question count is 3
        verify(questionRepository, never()).save(any());
    }

    @Test
    void submitAIAnswer_ProjectSession_EvaluatesWithProjectContextAndPersistsEvaluation() {
        Project project = new Project(testUser, "Distributed LRU Cache", "High performance cache", "Java, Spring Boot, Redis", "Backend Lead");
        project.setId(77L);
        project.setArchitectureDetails("Master-replica with consistent hashing");
        project.setKeyFeatures("Eviction policy, TTL expiration, write-through cache");

        InterviewSession projectSession = new InterviewSession(testUser, testCategory, Difficulty.MEDIUM, 3, 15, InterviewMode.PROJECT);
        projectSession.setId(200L);
        projectSession.setStatus(SessionStatus.IN_PROGRESS);
        projectSession.setProject(project);
        projectSession.setProjectTitleSnapshot("Distributed LRU Cache");

        Question q = new Question(testCategory, "How did you design cache eviction in Distributed LRU Cache?", QuestionType.TECHNICAL, Difficulty.MEDIUM, "Guidance");
        q.setId(51L);

        InterviewQuestion iq = new InterviewQuestion(projectSession, q, 1);
        iq.setId(501L);

        when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));
        when(sessionRepository.findByIdAndUserId(projectSession.getId(), testUser.getId())).thenReturn(Optional.of(projectSession));
        when(interviewQuestionRepository.findById(iq.getId())).thenReturn(Optional.of(iq));
        when(interviewResponseRepository.findByInterviewQuestionId(iq.getId())).thenReturn(Optional.empty());

        when(interviewResponseRepository.save(any(InterviewResponse.class))).thenAnswer(inv -> {
            InterviewResponse resp = inv.getArgument(0);
            resp.setId(1001L);
            return resp;
        });

        when(interviewEvaluationRepository.save(any(InterviewEvaluation.class))).thenAnswer(inv -> {
            InterviewEvaluation eval = inv.getArgument(0);
            eval.setId(3001L);
            return eval;
        });

        SubmitAnswerRequest request = new SubmitAnswerRequest("We implemented a doubly linked list combined with a ConcurrentHashMap to achieve O(1) eviction and lookup.");
        AnswerResponse response = interviewAIService.submitAIAnswer(testUser.getEmail(), projectSession.getId(), iq.getId(), request);

        assertNotNull(response);
        assertTrue(response.getSuccess());
        assertNotNull(response.getEvaluation());
        assertTrue(response.getEvaluation().getScore() >= 6);
        assertNotNull(response.getEvaluation().getTechnicalAccuracy());
        assertNotNull(response.getEvaluation().getCorrectness());
        assertNotNull(response.getEvaluation().getRelevance());
        assertNotNull(response.getEvaluation().getClarity());

        verify(interviewEvaluationRepository, times(1)).save(any(InterviewEvaluation.class));
    }

    @Test
    void submitAIAnswer_ProjectSession_OwnershipSecurityRejection() {
        when(userRepository.findByEmail("wrong@example.com")).thenReturn(Optional.of(testUser));
        when(sessionRepository.findByIdAndUserId(200L, testUser.getId())).thenReturn(Optional.empty());

        SubmitAnswerRequest request = new SubmitAnswerRequest("Answer text");
        assertThrows(com.prepgpt.exception.ResourceNotFoundException.class, () ->
                interviewAIService.submitAIAnswer("wrong@example.com", 200L, 501L, request)
        );

        verify(interviewEvaluationRepository, never()).save(any());
    }
}
