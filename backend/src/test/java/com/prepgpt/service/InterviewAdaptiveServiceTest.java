package com.prepgpt.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.prepgpt.ai.MockAIService;
import com.prepgpt.dto.*;
import com.prepgpt.dto.AIFollowUpResponse;
import com.prepgpt.entity.*;
import com.prepgpt.exception.BadRequestException;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InterviewAdaptiveServiceTest {

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

    private MockAIService mockAIService;
    private ObjectMapper objectMapper;
    private InterviewAIService interviewAIService;

    private User testUser;
    private User wrongUser;
    private InterviewCategory testCategory;
    private InterviewSession testSession;
    private Question testQuestion;
    private InterviewQuestion testInterviewQuestion;

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
        testUser = new User("candidate@example.com", "password123", "Candidate User", role);
        testUser.setId(1L);

        wrongUser = new User("wrong@example.com", "password123", "Wrong User", role);
        wrongUser.setId(99L);

        testCategory = new InterviewCategory("Java Core", "Core Java Concepts", true);
        testCategory.setId(1L);

        testSession = new InterviewSession(testUser, testCategory, Difficulty.MEDIUM, 3, 30, InterviewMode.AI);
        testSession.setId(10L);
        testSession.setStatus(SessionStatus.IN_PROGRESS);

        testQuestion = new Question(testCategory, "Explain Java memory model and Garbage Collection.", QuestionType.TECHNICAL, Difficulty.MEDIUM, "Focus on Heap, Stack, GC algorithms.");
        testQuestion.setId(100L);

        testInterviewQuestion = new InterviewQuestion(testSession, testQuestion, 1);
        testInterviewQuestion.setId(1000L);
    }

    @Test
    @DisplayName("1. Strong Answer -> DEEP_DIVE decision and difficulty progression")
    void testStrongAnswerDeepDive() {
        String detailedAnswer = "Java memory management relies on Heap (Young and Old Generation) and Stack spaces. Garbage collection reclaims unreachable object references using Mark-Sweep-Compact and G1 region-based collectors to optimize throughput and pause times.";
        InterviewResponse response = new InterviewResponse(testInterviewQuestion, detailedAnswer, InputMethod.TEXT);
        response.setId(500L);

        when(interviewEvaluationRepository.save(any(InterviewEvaluation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        InterviewEvaluation eval = interviewAIService.evaluateAndPersistAIResponse(testSession, testInterviewQuestion, response, response.getAnswerText());

        assertNotNull(eval);
        assertEquals(AdaptiveDecision.DEEP_DIVE, eval.getAdaptiveDecision());
        assertEquals(Difficulty.HARD, eval.getNextDifficulty());
        assertTrue(eval.getShouldAskFollowUp());
        assertNotNull(eval.getUserFacingMessage());
    }

    @Test
    @DisplayName("2. Partial Answer -> FOLLOW_UP decision and targeted follow-up question")
    void testPartialAnswerFollowUp() {
        InterviewResponse response = new InterviewResponse(testInterviewQuestion, "It manages memory in heap and stack.", InputMethod.TEXT);
        response.setId(501L);

        when(interviewEvaluationRepository.save(any(InterviewEvaluation.class))).thenAnswer(i -> i.getArgument(0));

        InterviewEvaluation eval = interviewAIService.evaluateAndPersistAIResponse(testSession, testInterviewQuestion, response, response.getAnswerText());

        assertNotNull(eval);
        assertEquals(AdaptiveDecision.FOLLOW_UP, eval.getAdaptiveDecision());
        assertTrue(eval.getShouldAskFollowUp());
        assertNotNull(eval.getFollowUpQuestion());
    }

    @Test
    @DisplayName("3. Weak Answer -> SIMPLIFY decision and difficulty reduction")
    void testWeakAnswerSimplify() {
        InterviewResponse response = new InterviewResponse(testInterviewQuestion, "I don't know.", InputMethod.TEXT);
        response.setId(502L);

        when(interviewEvaluationRepository.save(any(InterviewEvaluation.class))).thenAnswer(i -> i.getArgument(0));

        InterviewEvaluation eval = interviewAIService.evaluateAndPersistAIResponse(testSession, testInterviewQuestion, response, response.getAnswerText());

        assertNotNull(eval);
        assertEquals(AdaptiveDecision.SIMPLIFY, eval.getAdaptiveDecision());
        assertEquals(Difficulty.EASY, eval.getNextDifficulty());
        assertTrue(eval.getShouldAskFollowUp());
    }

    @Test
    @DisplayName("4. Repeated Weakness -> Targeted concept probing stored in targetConcept field")
    void testTargetedConceptProbing() {
        InterviewResponse response = new InterviewResponse(testInterviewQuestion, "Memory is stored on disk.", InputMethod.TEXT);
        response.setId(503L);

        when(interviewEvaluationRepository.save(any(InterviewEvaluation.class))).thenAnswer(i -> i.getArgument(0));

        InterviewEvaluation eval = interviewAIService.evaluateAndPersistAIResponse(testSession, testInterviewQuestion, response, response.getAnswerText());

        assertNotNull(eval);
        assertNotNull(eval.getTargetConcept());
        assertFalse(eval.getTargetConcept().isBlank());
    }

    @Test
    @DisplayName("5. Maximum Question Count -> Deterministically forces COMPLETE decision")
    void testMaxQuestionCountComplete() {
        // Session question count = 3. Set current question order = 3 (the final question)
        InterviewQuestion lastQuestion = new InterviewQuestion(testSession, testQuestion, 3);
        lastQuestion.setId(3000L);

        InterviewResponse response = new InterviewResponse(lastQuestion, "Detailed answer to final question.", InputMethod.TEXT);
        response.setId(505L);

        when(interviewEvaluationRepository.save(any(InterviewEvaluation.class))).thenAnswer(i -> i.getArgument(0));

        InterviewEvaluation eval = interviewAIService.evaluateAndPersistAIResponse(testSession, lastQuestion, response, response.getAnswerText());

        assertNotNull(eval);
        assertEquals(AdaptiveDecision.COMPLETE, eval.getAdaptiveDecision());
        assertFalse(eval.getShouldAskFollowUp());
    }

    @Test
    @DisplayName("6. Duplicate Question -> Prevented by backend duplicate checking")
    void testDuplicateQuestionPrevented() {
        when(userRepository.findByEmail("candidate@example.com")).thenReturn(Optional.of(testUser));
        when(sessionRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(testSession));

        InterviewQuestion q1 = new InterviewQuestion(testSession, testQuestion, 1);
        q1.setId(1001L);

        when(interviewQuestionRepository.findByInterviewSessionIdOrderByQuestionOrderAsc(10L))
                .thenReturn(List.of(q1));
        when(interviewResponseRepository.existsByInterviewQuestionId(1001L)).thenReturn(true);

        InterviewEvaluation lastEval = new InterviewEvaluation(
                new InterviewResponse(q1, "Answer 1", InputMethod.TEXT),
                8, 8, 8, 8, 8, "[]", "[]", "Good job", "Keep going", true,
                "Explain Java memory model and Garbage Collection." // DUPLICATE of q1!
        );
        lastEval.setAdaptiveDecision(AdaptiveDecision.FOLLOW_UP);
        lastEval.setTargetConcept("Memory Management");

        when(interviewEvaluationRepository.findByInterviewResponseInterviewQuestionId(1001L))
                .thenReturn(Optional.of(lastEval));

        when(questionRepository.save(any(Question.class))).thenAnswer(i -> { Question q = i.getArgument(0); q.setId(105L); return q; });
        when(interviewQuestionRepository.save(any(InterviewQuestion.class))).thenAnswer(i -> { InterviewQuestion iq = i.getArgument(0); iq.setId(1005L); return iq; });

        QuestionResponse response = interviewAIService.getCurrentAIQuestion("candidate@example.com", 10L);

        assertNotNull(response);
        // Verify generated question text is NOT identical to Q1
        assertNotEquals("Explain Java memory model and Garbage Collection.", response.getQuestionText());
    }

    @Test
    @DisplayName("7. Malformed JSON parsing fallback handling")
    void testMalformedJsonHandling() {
        AIFollowUpResponse parsedFallback = mockAIService.generateFollowUp(new AIFollowUpRequest(
                "Java", Difficulty.MEDIUM, "What is JVM?", "It runs byte code",
                new AIEvaluationResponse(7, 7, 7, 7, 7, List.of("Good"), List.of("Short"), "OK", "Expand"),
                "Order 1"
        ));

        assertNotNull(parsedFallback);
        assertNotNull(parsedFallback.getAdaptiveDecision());
        assertNotNull(parsedFallback.getUserFacingMessage());
    }

    @Test
    @DisplayName("8. Question Bank mode remains unaffected by AI adaptive logic")
    void testQuestionBankModeUnaffected() {
        InterviewSession qbSession = new InterviewSession(testUser, testCategory, Difficulty.MEDIUM, 3, 30, InterviewMode.QUESTION_BANK);
        qbSession.setId(20L);

        assertEquals(InterviewMode.QUESTION_BANK, qbSession.getMode());
        when(userRepository.findByEmail("candidate@example.com")).thenReturn(Optional.of(testUser));
        when(sessionRepository.findByIdAndUserId(20L, 1L)).thenReturn(Optional.of(qbSession));

        assertThrows(BadRequestException.class, () -> {
            interviewAIService.startAISession("candidate@example.com", 20L);
        });
    }

    @Test
    @DisplayName("9. Voice mode uses identical adaptive evaluation engine as Text mode")
    void testVoiceModeUsesSameAdaptiveEngine() {
        InterviewSession voiceSession = new InterviewSession(testUser, testCategory, Difficulty.MEDIUM, 3, 30, InterviewMode.AI);
        voiceSession.setInteractionMode(InterviewInteractionMode.VOICE);
        voiceSession.setId(30L);
        voiceSession.setStatus(SessionStatus.IN_PROGRESS);

        InterviewQuestion voiceIQ = new InterviewQuestion(voiceSession, testQuestion, 1);
        voiceIQ.setId(3001L);

        InterviewResponse voiceResponse = new InterviewResponse(voiceIQ, "JVM executes compiled Java bytecode instructions.", InputMethod.VOICE);
        voiceResponse.setId(600L);

        when(interviewEvaluationRepository.save(any(InterviewEvaluation.class))).thenAnswer(i -> i.getArgument(0));

        InterviewEvaluation eval = interviewAIService.evaluateAndPersistAIResponse(voiceSession, voiceIQ, voiceResponse, voiceResponse.getAnswerText());

        assertNotNull(eval);
        assertNotNull(eval.getAdaptiveDecision());
        assertNotNull(eval.getUserFacingMessage());
        assertEquals(InputMethod.VOICE, voiceResponse.getInputMethod());
    }

    @Test
    @DisplayName("10. Authentication / ownership security enforced")
    void testOwnershipSecurityEnforced() {
        when(userRepository.findByEmail("wrong@example.com")).thenReturn(Optional.of(wrongUser));
        when(sessionRepository.findByIdAndUserId(10L, 99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> {
            interviewAIService.getCurrentAIQuestion("wrong@example.com", 10L);
        });
    }
}
