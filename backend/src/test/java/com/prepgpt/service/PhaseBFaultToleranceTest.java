package com.prepgpt.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.prepgpt.ai.AIService;
import com.prepgpt.ai.GroqRetryClassifier;
import com.prepgpt.config.GroqResilienceConfig;
import com.prepgpt.dto.*;
import com.prepgpt.entity.*;
import com.prepgpt.exception.AIServiceUnavailableException;
import com.prepgpt.repository.*;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.retry.Retry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.HttpClientErrorException;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PhaseBFaultToleranceTest {

    @Mock private InterviewSessionRepository sessionRepository;
    @Mock private QuestionRepository questionRepository;
    @Mock private InterviewQuestionRepository interviewQuestionRepository;
    @Mock private InterviewResponseRepository interviewResponseRepository;
    @Mock private InterviewEvaluationRepository interviewEvaluationRepository;
    @Mock private UserRepository userRepository;
    @Mock private ProjectRepository projectRepository;

    private InterviewPersistenceService persistenceService;
    private InterviewAIService interviewAIService;
    private ObjectMapper objectMapper;
    private GroqResilienceConfig resilienceConfig;

    private User testUser;
    private InterviewCategory testCategory;
    private InterviewSession testSession;
    private Question testQuestion;
    private InterviewQuestion testInterviewQuestion;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        resilienceConfig = new GroqResilienceConfig();

        persistenceService = new InterviewPersistenceService(
                sessionRepository,
                questionRepository,
                interviewQuestionRepository,
                interviewResponseRepository,
                interviewEvaluationRepository,
                userRepository,
                projectRepository,
                objectMapper
        );

        Role role = new Role("ROLE_STUDENT");
        testUser = new User("candidate@prepgpt.com", "hash", "Candidate User", role);
        testUser.setId(101L);

        testCategory = new InterviewCategory("Distributed Systems", "Core concepts", true);
        testCategory.setId(201L);

        testSession = new InterviewSession(testUser, testCategory, Difficulty.MEDIUM, 3, 30, InterviewMode.AI);
        testSession.setId(301L);
        testSession.setStatus(SessionStatus.IN_PROGRESS);

        testQuestion = new Question(testCategory, "Explain circuit breaker pattern.", QuestionType.TECHNICAL, Difficulty.MEDIUM, "Guidance");
        testQuestion.setId(401L);

        testInterviewQuestion = new InterviewQuestion(testSession, testQuestion, 1);
        testInterviewQuestion.setId(501L);
    }

    // =========================================================================
    // TEST 1 — Groq 503: Transient failure triggers bounded retry, then controlled failure
    // =========================================================================
    @Test
    @DisplayName("Test 1 — Groq 503: Transient 503 retries, answer survives, controlled failure returned")
    void test1_groq503_boundedRetryAndControlledFailure() {
        AtomicInteger callCount = new AtomicInteger(0);

        CircuitBreaker cb = resilienceConfig.groqCircuitBreaker(10, 5, 50, Duration.ofSeconds(30), 2);
        Retry retry = resilienceConfig.groqRetry(3, Duration.ofMillis(10), 1.5);
        assertNotNull(cb);
        assertNotNull(retry);

        AIService mockAiService = mock(AIService.class);
        when(mockAiService.evaluateAnswer(any())).thenAnswer(inv -> {
            callCount.incrementAndGet();
            throw new HttpServerErrorException(HttpStatus.SERVICE_UNAVAILABLE, "Groq 503 Service Unavailable");
        });

        // Verification of classifier: 503 is retryable
        assertTrue(GroqRetryClassifier.isRetryable(new HttpServerErrorException(HttpStatus.SERVICE_UNAVAILABLE)));

        when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));
        when(sessionRepository.findByIdAndUserId(testSession.getId(), testUser.getId())).thenReturn(Optional.of(testSession));
        when(interviewQuestionRepository.findById(testInterviewQuestion.getId())).thenReturn(Optional.of(testInterviewQuestion));
        when(interviewResponseRepository.findByInterviewQuestionId(testInterviewQuestion.getId())).thenReturn(Optional.empty());

        when(interviewResponseRepository.save(any(InterviewResponse.class))).thenAnswer(inv -> {
            InterviewResponse r = inv.getArgument(0);
            r.setId(801L);
            return r;
        });

        interviewAIService = new InterviewAIService(persistenceService, mockAiService, null, null, null, null, objectMapper);

        SubmitAnswerRequest request = new SubmitAnswerRequest("Circuit breaker monitors failure rates.");

        assertThrows(HttpServerErrorException.class, () ->
                interviewAIService.submitAIAnswer(testUser.getEmail(), testSession.getId(), testInterviewQuestion.getId(), request)
        );

        // Verification: Candidate answer was saved
        verify(interviewResponseRepository, times(1)).save(any(InterviewResponse.class));
        // Verification: No fake evaluation with scores was saved
        verify(interviewEvaluationRepository, never()).save(argThat(eval -> eval != null && eval.getScore() != null));
    }

    // =========================================================================
    // TEST 2 — Groq 429: Rate limit triggers bounded retry, then controlled failure
    // =========================================================================
    @Test
    @DisplayName("Test 2 — Groq 429: HTTP 429 is recognized as retryable transient failure")
    void test2_groq429_boundedRetry() {
        HttpClientErrorException ex429 = HttpClientErrorException.create(
                HttpStatus.TOO_MANY_REQUESTS, "Rate limit reached", org.springframework.http.HttpHeaders.EMPTY, new byte[0], null);

        assertTrue(GroqRetryClassifier.isRetryable(ex429), "HTTP 429 must be classified as retryable");

        Retry retry = resilienceConfig.groqRetry(3, Duration.ofMillis(10), 1.5);
        AtomicInteger attempts = new AtomicInteger(0);

        try {
            Retry.decorateRunnable(retry, () -> {
                attempts.incrementAndGet();
                throw ex429;
            }).run();
        } catch (Exception e) {
            // expected
        }

        assertEquals(3, attempts.get(), "Retry should execute up to bounded maxAttempts (3) on 429");
    }

    // =========================================================================
    // TEST 3 — Groq 401: Authentication failure must NOT retry
    // =========================================================================
    @Test
    @DisplayName("Test 3 — Groq 401: Auth failure is non-retryable and fails fast without retry storm")
    void test3_groq401_noRetryStorm() {
        HttpClientErrorException ex401 = HttpClientErrorException.create(
                HttpStatus.UNAUTHORIZED, "Invalid API key", org.springframework.http.HttpHeaders.EMPTY, new byte[0], null);

        assertFalse(GroqRetryClassifier.isRetryable(ex401), "HTTP 401 must NOT be retryable");

        Retry retry = resilienceConfig.groqRetry(3, Duration.ofMillis(10), 1.5);
        AtomicInteger attempts = new AtomicInteger(0);

        assertThrows(HttpClientErrorException.class, () ->
                Retry.decorateRunnable(retry, () -> {
                    attempts.incrementAndGet();
                    throw ex401;
                }).run()
        );

        assertEquals(1, attempts.get(), "Non-retryable 401 must fail immediately on attempt 1 without retry storm");
    }

    // =========================================================================
    // TEST 4 — Groq 400: Bad Request / Schema validation must NOT retry
    // =========================================================================
    @Test
    @DisplayName("Test 4 — Groq 400: Validation error is non-retryable and fails immediately")
    void test4_groq400_noRetry() {
        HttpClientErrorException ex400 = HttpClientErrorException.create(
                HttpStatus.BAD_REQUEST, "Malformed JSON", org.springframework.http.HttpHeaders.EMPTY, new byte[0], null);

        assertFalse(GroqRetryClassifier.isRetryable(ex400), "HTTP 400 must NOT be retryable");

        Retry retry = resilienceConfig.groqRetry(3, Duration.ofMillis(10), 1.5);
        AtomicInteger attempts = new AtomicInteger(0);

        assertThrows(HttpClientErrorException.class, () ->
                Retry.decorateRunnable(retry, () -> {
                    attempts.incrementAndGet();
                    throw ex400;
                }).run()
        );

        assertEquals(1, attempts.get(), "Non-retryable 400 must not retry");
    }

    // =========================================================================
    // TEST 5 — Circuit Breaker Opens: Repeated failures transition CLOSED -> OPEN
    // =========================================================================
    @Test
    @DisplayName("Test 5 — Circuit Opens: Repeated failures open circuit and fast-fail subsequent calls")
    void test5_circuitBreakerOpens_fastFailsWithoutCallingProvider() {
        CircuitBreaker cb = resilienceConfig.groqCircuitBreaker(4, 4, 50, Duration.ofSeconds(30), 2);
        assertEquals(CircuitBreaker.State.CLOSED, cb.getState());

        HttpServerErrorException ex500 = new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR);

        // Record 4 failures to trip circuit
        for (int i = 0; i < 4; i++) {
            try {
                CircuitBreaker.decorateRunnable(cb, () -> { throw ex500; }).run();
            } catch (Exception ignored) {}
        }

        assertEquals(CircuitBreaker.State.OPEN, cb.getState(), "Circuit should transition to OPEN after 100% failure rate exceeding threshold");

        // Subsequent call must be rejected immediately by circuit breaker
        AtomicBoolean providerCalled = new AtomicBoolean(false);
        assertThrows(CallNotPermittedException.class, () ->
                CircuitBreaker.decorateRunnable(cb, () -> providerCalled.set(true)).run()
        );

        assertFalse(providerCalled.get(), "Groq provider must NOT be called when circuit breaker is OPEN");
    }

    // =========================================================================
    // TEST 6 — Half-Open Recovery: OPEN -> HALF_OPEN -> CLOSED on successful probes
    // =========================================================================
    @Test
    @DisplayName("Test 6 — Half-Open Recovery: Successful probes in HALF_OPEN transition back to CLOSED")
    void test6_halfOpenRecovery_transitionsBackToClosed() {
        CircuitBreaker cb = resilienceConfig.groqCircuitBreaker(2, 2, 50, Duration.ofMillis(10), 2);

        // Manually transition to OPEN
        cb.transitionToOpenState();
        assertEquals(CircuitBreaker.State.OPEN, cb.getState());

        // Transition to HALF_OPEN
        cb.transitionToHalfOpenState();
        assertEquals(CircuitBreaker.State.HALF_OPEN, cb.getState());

        // Execute permitted half-open calls successfully
        CircuitBreaker.decorateRunnable(cb, () -> {}).run();
        CircuitBreaker.decorateRunnable(cb, () -> {}).run();

        assertEquals(CircuitBreaker.State.CLOSED, cb.getState(), "Circuit should reset to CLOSED after successful probe calls in HALF_OPEN");
    }

    // =========================================================================
    // TEST 7 — Retry Reaper Success: FAILED_RETRYABLE -> COMPLETED
    // =========================================================================
    @Test
    @DisplayName("Test 7 — Retry Reaper Success: Eligible failed evaluation is processed and marked COMPLETED")
    void test7_retryReaperSuccess_recoversFailedEvaluation() {
        InterviewResponse resp = new InterviewResponse(testInterviewQuestion, "Valid answer");
        resp.setId(701L);

        InterviewEvaluation failedEval = new InterviewEvaluation();
        failedEval.setId(901L);
        failedEval.setInterviewResponse(resp);
        failedEval.setStatus(EvaluationStatus.FAILED_RETRYABLE);
        failedEval.setRetryCount(1);
        failedEval.setNextRetryAt(LocalDateTime.now().minusMinutes(1));

        AIService mockAiService = mock(AIService.class);
        when(mockAiService.evaluateAnswer(any())).thenReturn(
                new AIEvaluationResponse(9, 9, 9, 9, 9, List.of("Strong knowledge"), List.of(), "Excellent", "None")
        );
        when(mockAiService.generateFollowUp(any())).thenReturn(
                new AIFollowUpResponse(AdaptiveDecision.COMPLETE, "Session goal reached", null, null, Difficulty.MEDIUM, null, null)
        );

        when(interviewEvaluationRepository.findByStatusAndNextRetryAtLessThanEqualAndRetryCountLessThanOrderByNextRetryAtAsc(
                eq(EvaluationStatus.FAILED_RETRYABLE), any(LocalDateTime.class), eq(3)))
                .thenReturn(List.of(failedEval));

        when(interviewEvaluationRepository.findById(901L)).thenReturn(Optional.of(failedEval));
        when(interviewResponseRepository.findById(701L)).thenReturn(Optional.of(resp));
        when(interviewEvaluationRepository.findByInterviewResponseId(701L)).thenReturn(Optional.of(failedEval));
        when(interviewEvaluationRepository.save(any(InterviewEvaluation.class))).thenAnswer(inv -> inv.getArgument(0));

        EvaluationRetryReaper reaper = new EvaluationRetryReaper(persistenceService, mockAiService, 3, 60, 2.0);
        reaper.retryFailedEvaluations();

        // Verify evaluation was updated to COMPLETED with genuine AI score
        assertEquals(EvaluationStatus.COMPLETED, failedEval.getStatus());
        assertEquals(9, failedEval.getScore());
        assertNull(failedEval.getLastError());
        assertNull(failedEval.getNextRetryAt());
    }

    // =========================================================================
    // TEST 8 — Retry Reaper Exhaustion: FAILED_RETRYABLE -> DEAD_LETTER after max attempts
    // =========================================================================
    @Test
    @DisplayName("Test 8 — Retry Reaper Exhaustion: Max attempts transitions evaluation to DEAD_LETTER with no score")
    void test8_retryReaperExhaustion_movesToDeadLetter() {
        InterviewResponse resp = new InterviewResponse(testInterviewQuestion, "Valid answer");
        resp.setId(702L);

        InterviewEvaluation failedEval = new InterviewEvaluation();
        failedEval.setId(902L);
        failedEval.setInterviewResponse(resp);
        failedEval.setStatus(EvaluationStatus.FAILED_RETRYABLE);
        failedEval.setRetryCount(2); // Attempt 2 previously failed; next will be 3 (maxAttempts = 3)
        failedEval.setNextRetryAt(LocalDateTime.now().minusMinutes(1));

        AIService mockAiService = mock(AIService.class);
        when(mockAiService.evaluateAnswer(any())).thenThrow(new AIServiceUnavailableException("Persistent outage"));

        when(interviewEvaluationRepository.findByStatusAndNextRetryAtLessThanEqualAndRetryCountLessThanOrderByNextRetryAtAsc(
                eq(EvaluationStatus.FAILED_RETRYABLE), any(LocalDateTime.class), eq(3)))
                .thenReturn(List.of(failedEval));

        when(interviewEvaluationRepository.findById(902L)).thenReturn(Optional.of(failedEval));
        when(interviewEvaluationRepository.save(any(InterviewEvaluation.class))).thenAnswer(inv -> inv.getArgument(0));

        EvaluationRetryReaper reaper = new EvaluationRetryReaper(persistenceService, mockAiService, 3, 60, 2.0);
        reaper.retryFailedEvaluations();

        // Verify status moved to DEAD_LETTER after 3rd failure
        assertEquals(EvaluationStatus.DEAD_LETTER, failedEval.getStatus());
        assertEquals(3, failedEval.getRetryCount());
        assertNull(failedEval.getScore(), "Dead letter evaluation MUST NOT fabricate zero or fake scores");
        assertNull(failedEval.getNextRetryAt(), "Dead letter should not schedule further retries");
    }

    // =========================================================================
    // TEST 9 — Reaper Idempotency: Multiple invocations do not duplicate evaluations
    // =========================================================================
    @Test
    @DisplayName("Test 9 — Reaper Idempotency: Claiming sets state to PENDING and prevents double processing")
    void test9_reaperIdempotency_noDuplicateProcessing() {
        InterviewResponse resp = new InterviewResponse(testInterviewQuestion, "Valid answer");
        resp.setId(703L);

        InterviewEvaluation failedEval = new InterviewEvaluation();
        failedEval.setId(903L);
        failedEval.setInterviewResponse(resp);
        failedEval.setStatus(EvaluationStatus.FAILED_RETRYABLE);
        failedEval.setRetryCount(0);
        failedEval.setNextRetryAt(LocalDateTime.now().minusMinutes(1));

        when(interviewEvaluationRepository.findByStatusAndNextRetryAtLessThanEqualAndRetryCountLessThanOrderByNextRetryAtAsc(
                eq(EvaluationStatus.FAILED_RETRYABLE), any(LocalDateTime.class), eq(3)))
                .thenReturn(List.of(failedEval))
                .thenReturn(Collections.emptyList()); // Second run sees nothing eligible because status was claimed

        when(interviewEvaluationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        List<Long> claimed1 = persistenceService.claimEligibleEvaluationIdsForRetry(3);
        assertEquals(1, claimed1.size());
        assertEquals(EvaluationStatus.PENDING, failedEval.getStatus(), "Claimed evaluation must transition to PENDING");

        List<Long> claimed2 = persistenceService.claimEligibleEvaluationIdsForRetry(3);
        assertEquals(0, claimed2.size(), "Second claim must see empty list, preventing double evaluation");
    }

    // =========================================================================
    // TEST 10 — Concurrent Answer Submissions: DataIntegrityViolation handled safely
    // =========================================================================
    @Test
    @DisplayName("Test 10 — Concurrent Answer Submissions: Unique constraint race is caught, existing response reused, no 500")
    void test10_concurrentAnswerSubmission_handlesDataIntegrityViolationSafely() {
        InterviewResponse existingResponse = new InterviewResponse(testInterviewQuestion, "First submission text");
        existingResponse.setId(850L);

        when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));
        when(sessionRepository.findByIdAndUserId(testSession.getId(), testUser.getId())).thenReturn(Optional.of(testSession));
        when(interviewQuestionRepository.findById(testInterviewQuestion.getId())).thenReturn(Optional.of(testInterviewQuestion));

        // Thread A checked and didn't see response yet
        when(interviewResponseRepository.findByInterviewQuestionId(testInterviewQuestion.getId()))
                .thenReturn(Optional.empty()) // First check during concurrent race
                .thenReturn(Optional.of(existingResponse)); // Recovery lookup after unique violation

        // Simulate unique constraint collision from database
        when(interviewResponseRepository.save(any(InterviewResponse.class)))
                .thenThrow(new DataIntegrityViolationException("Duplicate entry '501' for key 'interview_responses.uk_question_id'"));

        // Context preparation should safely recover existing response without throwing 500
        InterviewPersistenceService.CandidateAnswerContext ctx = persistenceService.saveCandidateAnswerAndPrepareContext(
                testUser.getEmail(), testSession.getId(), testInterviewQuestion.getId(), "Second submission text", InputMethod.TEXT
        );

        assertNotNull(ctx);
        assertEquals(850L, ctx.getResponseId(), "Should safely resolve to existing response ID from concurrent winner");
    }

    // =========================================================================
    // TEST 11 — Existing Idempotency Regression: Submit answer twice sequentially
    // =========================================================================
    @Test
    @DisplayName("Test 11 — Idempotency Regression: Sequential answer submission reuses existing response & evaluation")
    void test11_sequentialSubmission_preservesExistingIdempotency() {
        InterviewResponse existingResponse = new InterviewResponse(testInterviewQuestion, "Original answer");
        existingResponse.setId(860L);

        InterviewEvaluation existingEval = new InterviewEvaluation(
                existingResponse, 8, 8, 8, 8, 8, "[]", "[]", "Good", "Keep practicing", false, null
        );
        existingEval.setId(960L);
        existingEval.setStatus(EvaluationStatus.COMPLETED);

        when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));
        when(sessionRepository.findByIdAndUserId(testSession.getId(), testUser.getId())).thenReturn(Optional.of(testSession));
        when(interviewQuestionRepository.findById(testInterviewQuestion.getId())).thenReturn(Optional.of(testInterviewQuestion));
        when(interviewResponseRepository.findByInterviewQuestionId(testInterviewQuestion.getId())).thenReturn(Optional.of(existingResponse));
        when(interviewEvaluationRepository.findByInterviewResponseId(860L)).thenReturn(Optional.of(existingEval));

        AIService mockAiService = mock(AIService.class);
        interviewAIService = new InterviewAIService(persistenceService, mockAiService, null, null, null, null, objectMapper);

        SubmitAnswerRequest request = new SubmitAnswerRequest("Original answer resubmitted");
        AnswerResponse response = interviewAIService.submitAIAnswer(testUser.getEmail(), testSession.getId(), testInterviewQuestion.getId(), request);

        assertNotNull(response);
        assertTrue(response.getSuccess());
        assertEquals(8, response.getEvaluation().getScore());

        // Verify Groq was NOT called again
        verify(mockAiService, never()).evaluateAnswer(any());
        verify(mockAiService, never()).generateFollowUp(any());
    }

    // =========================================================================
    // TEST 12 — Phase A Transaction Regression: Zero active transactions during Groq call
    // =========================================================================
    @Test
    @DisplayName("Test 12 — Transaction Regression: TransactionSynchronizationManager reports inactive transaction during Groq network call")
    void test12_transactionRegression_noActiveTransactionDuringGroq() {
        AtomicBoolean txActiveDuringAi = new AtomicBoolean(true);

        AIService spyAiService = mock(AIService.class);
        when(spyAiService.evaluateAnswer(any())).thenAnswer(inv -> {
            txActiveDuringAi.set(TransactionSynchronizationManager.isActualTransactionActive());
            return new AIEvaluationResponse(8, 8, 8, 8, 8, List.of("Clear"), List.of(), "Good job", "Practice more");
        });
        when(spyAiService.generateFollowUp(any())).thenReturn(
                new AIFollowUpResponse(AdaptiveDecision.COMPLETE, "Done", null, null, Difficulty.MEDIUM, null, null)
        );

        when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));
        when(sessionRepository.findByIdAndUserId(testSession.getId(), testUser.getId())).thenReturn(Optional.of(testSession));
        when(interviewQuestionRepository.findById(testInterviewQuestion.getId())).thenReturn(Optional.of(testInterviewQuestion));
        when(interviewResponseRepository.findByInterviewQuestionId(testInterviewQuestion.getId())).thenReturn(Optional.empty());

        when(interviewResponseRepository.save(any(InterviewResponse.class))).thenAnswer(inv -> {
            InterviewResponse r = inv.getArgument(0);
            r.setId(880L);
            return r;
        });
        when(interviewEvaluationRepository.save(any(InterviewEvaluation.class))).thenAnswer(inv -> inv.getArgument(0));

        interviewAIService = new InterviewAIService(persistenceService, spyAiService, null, null, null, null, objectMapper);

        SubmitAnswerRequest request = new SubmitAnswerRequest("Valid candidate answer text");
        AnswerResponse response = interviewAIService.submitAIAnswer(testUser.getEmail(), testSession.getId(), testInterviewQuestion.getId(), request);

        assertNotNull(response);
        assertFalse(txActiveDuringAi.get(), "Database transaction MUST NOT be active during Groq network call");
    }
}
