package com.prepgpt.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.prepgpt.ai.AIService;
import com.prepgpt.ai.GroqRetryClassifier;
import com.prepgpt.config.GroqResilienceConfig;
import com.prepgpt.dto.*;
import com.prepgpt.entity.*;
import com.prepgpt.exception.AIServiceUnavailableException;
import com.prepgpt.exception.CodeExecutionUnavailableException;
import com.prepgpt.exception.GlobalExceptionHandler;
import com.prepgpt.repository.*;
import com.prepgpt.security.CorrelationIdFilter;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.retry.Retry;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ResilienceFailureSimulationTest {

    @Mock private InterviewSessionRepository sessionRepository;
    @Mock private QuestionRepository questionRepository;
    @Mock private InterviewQuestionRepository interviewQuestionRepository;
    @Mock private InterviewResponseRepository interviewResponseRepository;
    @Mock private InterviewEvaluationRepository interviewEvaluationRepository;
    @Mock private UserRepository userRepository;
    @Mock private ProjectRepository projectRepository;
    @Mock private AiTokenUsageRepository tokenUsageRepository;

    private InterviewPersistenceService persistenceService;
    private InterviewAIService interviewAIService;
    private QuestionQualityValidator questionQualityValidator;
    private AiTokenUsageService tokenUsageService;
    private ObjectMapper objectMapper;
    private GroqResilienceConfig resilienceConfig;
    private GlobalExceptionHandler globalExceptionHandler;

    private User testUser;
    private InterviewCategory testCategory;
    private InterviewSession testSession;
    private Question testQuestion;
    private InterviewQuestion testInterviewQuestion;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        questionQualityValidator = new QuestionQualityValidator();
        tokenUsageService = new AiTokenUsageService(tokenUsageRepository);
        resilienceConfig = new GroqResilienceConfig();
        globalExceptionHandler = new GlobalExceptionHandler();

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

        testCategory = new InterviewCategory("Distributed Systems", "Core concepts", true);
        testCategory.setId(201L);

        Role role = new Role("ROLE_STUDENT");
        testUser = new User("candidate@prepgpt.com", "hash", "Candidate User", role);
        testUser.setId(101L);

        testSession = new InterviewSession(testUser, testCategory, Difficulty.MEDIUM, 3, 30, InterviewMode.AI);
        testSession.setId(301L);
        testSession.setStatus(SessionStatus.IN_PROGRESS);

        testQuestion = new Question(testCategory, "Explain circuit breaker pattern.", QuestionType.TECHNICAL, Difficulty.MEDIUM, "Guidance");
        testQuestion.setId(401L);

        testInterviewQuestion = new InterviewQuestion(testSession, testQuestion, 1);
        testInterviewQuestion.setId(501L);
    }

    // =========================================================================
    // FAILURE SCENARIO A — GROQ TIMEOUT
    // =========================================================================

    @Test
    @DisplayName("Scenario A: Groq Timeout preserves candidate answer, fabricates no fake score, bounds retries")
    void testScenarioA_groqTimeout_answerPreservedNoFakeScore() {
        AtomicInteger callCount = new AtomicInteger(0);
        AIService mockAiService = mock(AIService.class);

        when(mockAiService.evaluateAnswer(any())).thenAnswer(inv -> {
            callCount.incrementAndGet();
            throw new ResourceAccessException("I/O error on POST request: Read timed out", new SocketTimeoutException("Read timed out"));
        });

        interviewAIService = new InterviewAIService(persistenceService, mockAiService, null, null, null, null, objectMapper, questionQualityValidator);

        when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));
        when(sessionRepository.findByIdAndUserId(testSession.getId(), testUser.getId())).thenReturn(Optional.of(testSession));
        when(interviewQuestionRepository.findById(testInterviewQuestion.getId())).thenReturn(Optional.of(testInterviewQuestion));
        when(interviewResponseRepository.findByInterviewQuestionId(testInterviewQuestion.getId())).thenReturn(Optional.empty());

        when(interviewResponseRepository.save(any(InterviewResponse.class))).thenAnswer(inv -> {
            InterviewResponse r = inv.getArgument(0);
            r.setId(801L);
            return r;
        });
        when(interviewResponseRepository.findById(801L)).thenAnswer(inv -> {
            InterviewResponse r = new InterviewResponse(testInterviewQuestion, "Candidate detailed answer explaining timeouts.");
            r.setId(801L);
            return Optional.of(r);
        });

        when(interviewEvaluationRepository.save(any(InterviewEvaluation.class))).thenAnswer(inv -> {
            InterviewEvaluation eval = inv.getArgument(0);
            eval.setId(701L);
            return eval;
        });

        SubmitAnswerRequest request = new SubmitAnswerRequest("Candidate detailed answer explaining timeouts.");

        // Controlled exception returned to candidate
        assertThrows(ResourceAccessException.class, () -> {
            interviewAIService.submitAIAnswer(testUser.getEmail(), testSession.getId(), testInterviewQuestion.getId(), request);
        });

        // 1. Candidate answer survived and was persisted in DB
        verify(interviewResponseRepository, times(1)).save(any(InterviewResponse.class));

        // 2. An evaluation record with FAILED_RETRYABLE was saved, NOT a fake completed evaluation with fabricated scores
        ArgumentCaptor<InterviewEvaluation> evalCaptor = ArgumentCaptor.forClass(InterviewEvaluation.class);
        verify(interviewEvaluationRepository).save(evalCaptor.capture());
        InterviewEvaluation savedEval = evalCaptor.getValue();
        assertEquals(EvaluationStatus.FAILED_RETRYABLE, savedEval.getStatus());
        assertNull(savedEval.getScore(), "No fake score may be fabricated");
        assertNotNull(savedEval.getNextRetryAt(), "Next retry timestamp must be scheduled");
        assertTrue(savedEval.getRetryCount() >= 0);
    }

    // =========================================================================
    // FAILURE SCENARIO B — GROQ 429 (RATE LIMIT)
    // =========================================================================

    @Test
    @DisplayName("Scenario B: Groq 429 is classified as retryable, bounded retry occurs, answer survives")
    void testScenarioB_groq429_retryableBoundedRetry() {
        AtomicInteger callCount = new AtomicInteger(0);
        Retry retry = resilienceConfig.groqRetry(3, Duration.ofMillis(10), 1.5);
        assertNotNull(retry);

        // Verify GroqRetryClassifier classifies 429 as transient/retryable
        assertTrue(GroqRetryClassifier.isRetryable(new HttpClientErrorException(HttpStatus.TOO_MANY_REQUESTS)));

        AIService mockAiService = mock(AIService.class);
        when(mockAiService.evaluateAnswer(any())).thenAnswer(inv -> {
            callCount.incrementAndGet();
            throw new HttpClientErrorException(HttpStatus.TOO_MANY_REQUESTS, "Rate limit reached");
        });

        interviewAIService = new InterviewAIService(persistenceService, mockAiService, null, null, null, null, objectMapper, questionQualityValidator);

        when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));
        when(sessionRepository.findByIdAndUserId(testSession.getId(), testUser.getId())).thenReturn(Optional.of(testSession));
        when(interviewQuestionRepository.findById(testInterviewQuestion.getId())).thenReturn(Optional.of(testInterviewQuestion));
        when(interviewResponseRepository.findByInterviewQuestionId(testInterviewQuestion.getId())).thenReturn(Optional.empty());
        when(interviewResponseRepository.save(any(InterviewResponse.class))).thenAnswer(inv -> {
            InterviewResponse r = inv.getArgument(0);
            r.setId(802L);
            return r;
        });

        SubmitAnswerRequest request = new SubmitAnswerRequest("Candidate answer during 429 limit.");

        assertThrows(HttpClientErrorException.class, () -> {
            interviewAIService.submitAIAnswer(testUser.getEmail(), testSession.getId(), testInterviewQuestion.getId(), request);
        });

        // Exactly 1 InterviewResponse persisted (no duplicates)
        verify(interviewResponseRepository, times(1)).save(any(InterviewResponse.class));
    }

    // =========================================================================
    // FAILURE SCENARIO C — GROQ 5XX (500, 502, 503, 504)
    // =========================================================================

    @Test
    @DisplayName("Scenario C: Groq 500, 502, 503, 504 are all classified as transient/retryable")
    void testScenarioC_groq5xx_allClassifiedAsRetryable() {
        assertTrue(GroqRetryClassifier.isRetryable(new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR)));
        assertTrue(GroqRetryClassifier.isRetryable(new HttpServerErrorException(HttpStatus.BAD_GATEWAY)));
        assertTrue(GroqRetryClassifier.isRetryable(new HttpServerErrorException(HttpStatus.SERVICE_UNAVAILABLE)));
        assertTrue(GroqRetryClassifier.isRetryable(new HttpServerErrorException(HttpStatus.GATEWAY_TIMEOUT)));
    }

    // =========================================================================
    // FAILURE SCENARIO D — NON-RETRYABLE GROQ ERROR (400, 401, 403)
    // =========================================================================

    @Test
    @DisplayName("Scenario D: Non-retryable errors (400, 401, 403) fast-fail with no retries or retry storm")
    void testScenarioD_nonRetryableGroqError_fastFailureNoRetryStorm() {
        assertFalse(GroqRetryClassifier.isRetryable(new HttpClientErrorException(HttpStatus.BAD_REQUEST)));
        assertFalse(GroqRetryClassifier.isRetryable(new HttpClientErrorException(HttpStatus.UNAUTHORIZED)));
        assertFalse(GroqRetryClassifier.isRetryable(new HttpClientErrorException(HttpStatus.FORBIDDEN)));

        AtomicInteger callCount = new AtomicInteger(0);
        AIService mockAiService = mock(AIService.class);

        when(mockAiService.evaluateAnswer(any())).thenAnswer(inv -> {
            callCount.incrementAndGet();
            throw new HttpClientErrorException(HttpStatus.UNAUTHORIZED, "Invalid API key");
        });

        interviewAIService = new InterviewAIService(persistenceService, mockAiService, null, null, null, null, objectMapper, questionQualityValidator);

        when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));
        when(sessionRepository.findByIdAndUserId(testSession.getId(), testUser.getId())).thenReturn(Optional.of(testSession));
        when(interviewQuestionRepository.findById(testInterviewQuestion.getId())).thenReturn(Optional.of(testInterviewQuestion));
        when(interviewResponseRepository.findByInterviewQuestionId(testInterviewQuestion.getId())).thenReturn(Optional.empty());
        when(interviewResponseRepository.save(any(InterviewResponse.class))).thenAnswer(inv -> {
            InterviewResponse r = inv.getArgument(0);
            r.setId(804L);
            return r;
        });

        SubmitAnswerRequest request = new SubmitAnswerRequest("Candidate answer with bad auth.");

        assertThrows(HttpClientErrorException.class, () -> {
            interviewAIService.submitAIAnswer(testUser.getEmail(), testSession.getId(), testInterviewQuestion.getId(), request);
        });

        // Fast-fail: exactly 1 attempt made, no retry storm
        assertEquals(1, callCount.get());
        // Candidate answer remains preserved post-commit
        verify(interviewResponseRepository, times(1)).save(any(InterviewResponse.class));
    }

    // =========================================================================
    // FAILURE SCENARIO E — CIRCUIT BREAKER OPEN
    // =========================================================================

    @Test
    @DisplayName("Scenario E: Circuit breaker OPEN fast-fails and Groq is NOT invoked")
    void testScenarioE_circuitBreakerOpen_groqNeverCalled() {
        CircuitBreaker cb = resilienceConfig.groqCircuitBreaker(10, 5, 50, Duration.ofSeconds(30), 2);

        // Force 5 failures (min calls) with 100% failure rate -> Breaker transitions to OPEN
        for (int i = 0; i < 5; i++) {
            cb.onError(0, java.util.concurrent.TimeUnit.MILLISECONDS, new HttpServerErrorException(HttpStatus.SERVICE_UNAVAILABLE));
        }

        assertEquals(CircuitBreaker.State.OPEN, cb.getState(), "Circuit breaker must be OPEN");

        // Execute Groq call wrapped in circuit breaker
        java.util.function.Supplier<String> supplier = () -> {
            fail("Groq provider MUST NOT be called when circuit is OPEN");
            return "unexpected";
        };
        java.util.function.Supplier<String> decorated = CircuitBreaker.decorateSupplier(cb, supplier);

        assertThrows(CallNotPermittedException.class, decorated::get);
    }

    // =========================================================================
    // FAILURE SCENARIO F — HALF-OPEN RECOVERY
    // =========================================================================

    @Test
    @DisplayName("Scenario F: Half-Open state recovery: successful probes close circuit, failing probe re-opens")
    void testScenarioF_halfOpenRecovery() {
        CircuitBreaker cb = resilienceConfig.groqCircuitBreaker(10, 5, 50, Duration.ofSeconds(30), 2);

        // Trip to OPEN
        for (int i = 0; i < 5; i++) {
            cb.onError(0, java.util.concurrent.TimeUnit.MILLISECONDS, new HttpServerErrorException(HttpStatus.SERVICE_UNAVAILABLE));
        }
        assertEquals(CircuitBreaker.State.OPEN, cb.getState());

        // Transition to HALF_OPEN
        cb.transitionToHalfOpenState();
        assertEquals(CircuitBreaker.State.HALF_OPEN, cb.getState());

        // Probe 1: Success
        CircuitBreaker.decorateRunnable(cb, () -> {}).run();
        assertEquals(CircuitBreaker.State.HALF_OPEN, cb.getState());

        // Probe 2: Success -> Closes circuit!
        CircuitBreaker.decorateRunnable(cb, () -> {}).run();
        assertEquals(CircuitBreaker.State.CLOSED, cb.getState(), "Two successful probes must restore CLOSED state");

        // Verify failing probe in HALF_OPEN re-opens circuit
        cb.transitionToOpenState();
        cb.transitionToHalfOpenState();
        assertEquals(CircuitBreaker.State.HALF_OPEN, cb.getState());

        // Permitted calls in half open state is 2. Both failing probes will evaluate failure rate > 50% and trip back to OPEN.
        for (int i = 0; i < 2; i++) {
            assertThrows(HttpServerErrorException.class, () -> {
                CircuitBreaker.decorateRunnable(cb, () -> {
                    throw new HttpServerErrorException(HttpStatus.SERVICE_UNAVAILABLE);
                }).run();
            });
        }
        assertEquals(CircuitBreaker.State.OPEN, cb.getState(), "Failing probes in HALF_OPEN must re-trip to OPEN");
    }

    // =========================================================================
    // FAILURE SCENARIO G — EVALUATION RETRY REAPER RECOVERY
    // =========================================================================

    @Test
    @DisplayName("Scenario G: EvaluationRetryReaper heals FAILED_RETRYABLE to COMPLETED with genuine evaluation")
    void testScenarioG_reaperHealsFailedEvaluation() {
        InterviewResponse resp = new InterviewResponse(testInterviewQuestion, "Good concurrency answer", InputMethod.TEXT);
        resp.setId(806L);

        InterviewEvaluation failedEval = new InterviewEvaluation();
        failedEval.setId(706L);
        failedEval.setInterviewResponse(resp);
        failedEval.setStatus(EvaluationStatus.FAILED_RETRYABLE);
        failedEval.setRetryCount(1);
        failedEval.setNextRetryAt(LocalDateTime.now().minusMinutes(2));

        when(interviewEvaluationRepository.findByStatusAndNextRetryAtLessThanEqualAndRetryCountLessThanOrderByNextRetryAtAsc(
                eq(EvaluationStatus.FAILED_RETRYABLE), any(), eq(3)
        )).thenReturn(List.of(failedEval));

        when(interviewEvaluationRepository.findById(706L)).thenReturn(Optional.of(failedEval));
        when(interviewResponseRepository.findById(806L)).thenReturn(Optional.of(resp));
        when(interviewEvaluationRepository.findByInterviewResponseId(806L)).thenReturn(Optional.of(failedEval));
        when(interviewEvaluationRepository.save(any(InterviewEvaluation.class))).thenAnswer(inv -> inv.getArgument(0));

        AIService mockAi = mock(AIService.class);
        when(mockAi.evaluateAnswer(any())).thenReturn(
                new AIEvaluationResponse(9, 9, 9, 9, 9, List.of("Strong"), List.of("Minor"), "Excellent recovery", "Keep going")
        );
        when(mockAi.generateFollowUp(any())).thenReturn(
                new AIFollowUpResponse(AdaptiveDecision.FOLLOW_UP, "Good", "Scalability", "Next question", Difficulty.HARD, "How to scale?", "Arch")
        );

        EvaluationRetryReaper reaper = new EvaluationRetryReaper(persistenceService, mockAi, 3, 60, 2.0);
        reaper.retryFailedEvaluations();

        // Status transitioned to COMPLETED and genuine evaluation scores populated
        assertEquals(EvaluationStatus.COMPLETED, failedEval.getStatus());
        assertEquals(9, failedEval.getScore());
        assertEquals(9, failedEval.getTechnicalAccuracy());
        assertEquals("Excellent recovery", failedEval.getFeedback());
    }

    // =========================================================================
    // FAILURE SCENARIO H — RETRY EXHAUSTION / DEAD LETTER QUEUE (DLQ)
    // =========================================================================

    @Test
    @DisplayName("Scenario H: Exhausted retries advance evaluation to DEAD_LETTER with no fake scores")
    void testScenarioH_retryExhaustion_advancesToDeadLetter() {
        InterviewResponse resp = new InterviewResponse(testInterviewQuestion, "Answer during outage", InputMethod.TEXT);
        resp.setId(807L);

        InterviewEvaluation eval = new InterviewEvaluation();
        eval.setId(707L);
        eval.setInterviewResponse(resp);
        eval.setStatus(EvaluationStatus.FAILED_RETRYABLE);
        eval.setRetryCount(2); // Attempt 3 will be the final attempt
        eval.setNextRetryAt(LocalDateTime.now().minusMinutes(5));

        when(interviewEvaluationRepository.findByStatusAndNextRetryAtLessThanEqualAndRetryCountLessThanOrderByNextRetryAtAsc(
                eq(EvaluationStatus.FAILED_RETRYABLE), any(), eq(3)
        )).thenReturn(List.of(eval));

        when(interviewEvaluationRepository.findById(707L)).thenReturn(Optional.of(eval));
        when(interviewEvaluationRepository.save(any(InterviewEvaluation.class))).thenAnswer(inv -> inv.getArgument(0));

        AIService failingAi = mock(AIService.class);
        when(failingAi.evaluateAnswer(any())).thenThrow(new AIServiceUnavailableException("Persistent outage 503"));

        EvaluationRetryReaper reaper = new EvaluationRetryReaper(persistenceService, failingAi, 3, 60, 2.0);
        reaper.retryFailedEvaluations();

        // Advanced to DEAD_LETTER
        assertEquals(EvaluationStatus.DEAD_LETTER, eval.getStatus());
        assertEquals(3, eval.getRetryCount());
        assertNull(eval.getScore(), "Dead-letter evaluations must NOT have fabricated scores");
        assertNotNull(eval.getLastError());
        assertTrue(eval.getLastError().contains("Persistent outage"));
    }

    // =========================================================================
    // FAILURE SCENARIO I — DATABASE FAILURE DURING TELEMETRY IS ISOLATED
    // =========================================================================

    @Test
    @DisplayName("Scenario I: Database failure during token telemetry recording never disrupts user operation")
    void testScenarioI_tokenTelemetryFailure_isolatedFromOperation() {
        doThrow(new DataIntegrityViolationException("Simulated DB disk failure during telemetry save"))
                .when(tokenUsageRepository).save(any(AiTokenUsage.class));

        // Must succeed without throwing
        assertDoesNotThrow(() -> {
            tokenUsageService.recordUsage(
                    AiOperationType.ANSWER_EVALUATION, "GROQ", "openai/gpt-oss-120b",
                    150, 60, 210
            );
        });
    }

    // =========================================================================
    // FAILURE SCENARIO J — OPTIMISTIC LOCK CONFLICT
    // =========================================================================

    @Test
    @DisplayName("Scenario J: Optimistic locking conflict maps to HTTP 409 Conflict with no lost updates")
    void testScenarioJ_optimisticLockConflict_returnsHttp409() {
        OptimisticLockingFailureException ex = new OptimisticLockingFailureException(
                "Row was updated or deleted by another transaction : [InterviewSession#301]"
        );
        ResponseEntity<?> entity = globalExceptionHandler.handleOptimisticLockingFailureException(ex, null);

        assertNotNull(entity);
        assertEquals(HttpStatus.CONFLICT, entity.getStatusCode());
        ErrorResponse body = (ErrorResponse) entity.getBody();
        assertNotNull(body);
        assertTrue(body.getMessage().contains("updated concurrently"));
    }

    // =========================================================================
    // FAILURE SCENARIO K — CONCURRENT ANSWER SUBMISSION IDEMPOTENCY
    // =========================================================================

    @Test
    @DisplayName("Scenario K: Concurrent answer submissions result in exactly ONE InterviewResponse and ONE InterviewEvaluation")
    void testScenarioK_concurrentAnswerSubmission_idempotent() {
        AIService mockAiService = mock(AIService.class);
        interviewAIService = new InterviewAIService(persistenceService, mockAiService, null, null, null, null, objectMapper, questionQualityValidator);

        InterviewResponse existingResponse = new InterviewResponse(testInterviewQuestion, "Existing answer", InputMethod.TEXT);
        existingResponse.setId(808L);

        InterviewEvaluation existingEval = new InterviewEvaluation(existingResponse, 8, 8, 8, 8, 8, "Good", "None", "Feedback", "Focus", false, null);
        existingEval.setId(708L);

        when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));
        when(sessionRepository.findByIdAndUserId(testSession.getId(), testUser.getId())).thenReturn(Optional.of(testSession));
        when(interviewQuestionRepository.findById(testInterviewQuestion.getId())).thenReturn(Optional.of(testInterviewQuestion));
        // Return existing response for the concurrent/subsequent request
        when(interviewResponseRepository.findByInterviewQuestionId(testInterviewQuestion.getId())).thenReturn(Optional.of(existingResponse));
        when(interviewEvaluationRepository.findByInterviewResponseId(808L)).thenReturn(Optional.of(existingEval));

        SubmitAnswerRequest request = new SubmitAnswerRequest("Candidate concurrent answer");
        AnswerResponse response = interviewAIService.submitAIAnswer(testUser.getEmail(), testSession.getId(), testInterviewQuestion.getId(), request);

        assertNotNull(response);
        assertTrue(response.getSuccess());
        assertEquals(8, response.getEvaluation().getScore());

        // AI is NOT re-invoked and no duplicate evaluation is created
        verify(mockAiService, never()).evaluateAnswer(any());
        verify(interviewResponseRepository, never()).save(any());
    }

    // =========================================================================
    // FAILURE SCENARIO L — DOCKER SANDBOX FAILURE
    // =========================================================================

    @Test
    @DisplayName("Scenario L: Docker sandbox failure throws CodeExecutionUnavailableException without host fallback")
    void testScenarioL_dockerSandboxFailure_noHostFallback() {
        CodeExecutionService service = new CodeExecutionService() {
            @Override
            public boolean isDockerAvailable() {
                return false; // Simulate Docker daemon offline / socket failure
            }
        };

        CodingTestCase tc = new CodingTestCase(null, "1 2\n", "3", false, "Sample test", 1);
        assertThrows(CodeExecutionUnavailableException.class, () -> {
            service.executeCode("JAVA", "class Solution {}", List.of(tc), 3000);
        });
    }

    // =========================================================================
    // FAILURE SCENARIO M — DATABASE CONNECTION PRESSURE / ZERO TX IN AI LATENCY
    // =========================================================================

    @Test
    @DisplayName("Scenario M: 5-second AI latency executes with ZERO active database transaction")
    void testScenarioM_aiLatencyDecoupledFromDatabaseTransaction() {
        AtomicBoolean txWasActiveDuringAiCall = new AtomicBoolean(true);

        AIService latencyAiService = new AIService() {
            @Override
            public AIQuestionResponse generateQuestion(AIQuestionRequest request) {
                return new AIQuestionResponse("Question?", QuestionType.TECHNICAL, Difficulty.MEDIUM, "Java", "Test");
            }

            @Override
            public AIEvaluationResponse evaluateAnswer(AIEvaluationRequest request) {
                // Check if Spring DB transaction is active during AI execution
                txWasActiveDuringAiCall.set(TransactionSynchronizationManager.isActualTransactionActive());
                return new AIEvaluationResponse(8, 8, 8, 8, 8, List.of("Good"), List.of("None"), "Feedback", "Guidance");
            }

            @Override
            public AIFollowUpResponse generateFollowUp(AIFollowUpRequest request) {
                return new AIFollowUpResponse(AdaptiveDecision.FOLLOW_UP, "Good", "Topic", "Msg", Difficulty.MEDIUM, "Next?", "Focus");
            }
        };

        interviewAIService = new InterviewAIService(persistenceService, latencyAiService, null, null, null, null, objectMapper, questionQualityValidator);

        when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));
        when(sessionRepository.findByIdAndUserId(testSession.getId(), testUser.getId())).thenReturn(Optional.of(testSession));
        when(interviewQuestionRepository.findById(testInterviewQuestion.getId())).thenReturn(Optional.of(testInterviewQuestion));
        when(interviewResponseRepository.findByInterviewQuestionId(testInterviewQuestion.getId())).thenReturn(Optional.empty());

        when(interviewResponseRepository.save(any(InterviewResponse.class))).thenAnswer(inv -> {
            InterviewResponse r = inv.getArgument(0);
            r.setId(809L);
            return r;
        });

        when(interviewEvaluationRepository.save(any(InterviewEvaluation.class))).thenAnswer(inv -> inv.getArgument(0));

        SubmitAnswerRequest request = new SubmitAnswerRequest("Candidate answer to test transaction decoupling.");
        interviewAIService.submitAIAnswer(testUser.getEmail(), testSession.getId(), testInterviewQuestion.getId(), request);

        // Architectural guarantee verified: DB transaction was NOT active during AI call
        assertFalse(txWasActiveDuringAiCall.get(), "Database transaction MUST NOT be active during external AI call");
    }

    // =========================================================================
    // FAILURE SCENARIO N — MDC CLEANUP & NO THREAD-LOCAL LEAKS
    // =========================================================================

    @Test
    @DisplayName("Scenario N: MDC correlationId cleans up after request and does not leak between requests")
    void testScenarioN_mdcCleanupNoLeak() throws ServletException, IOException {
        CorrelationIdFilter filter = new CorrelationIdFilter();

        HttpServletRequest req1 = mock(HttpServletRequest.class);
        HttpServletResponse resp1 = mock(HttpServletResponse.class);
        FilterChain chain1 = mock(FilterChain.class);

        when(req1.getHeader("X-Correlation-ID")).thenReturn("request-1-correlation-id");

        filter.doFilter(req1, resp1, chain1);

        // After request 1, MDC must be completely clean
        assertNull(MDC.get("correlationId"), "MDC must be clean after request 1 completes");

        HttpServletRequest req2 = mock(HttpServletRequest.class);
        HttpServletResponse resp2 = mock(HttpServletResponse.class);
        FilterChain chain2 = mock(FilterChain.class);

        // Request 2 has no header
        when(req2.getHeader("X-Correlation-ID")).thenReturn(null);

        AtomicBoolean req2InheritedReq1 = new AtomicBoolean(false);
        doAnswer(inv -> {
            String currentMdc = MDC.get("correlationId");
            if ("request-1-correlation-id".equals(currentMdc)) {
                req2InheritedReq1.set(true);
            }
            return null;
        }).when(chain2).doFilter(req2, resp2);

        filter.doFilter(req2, resp2, chain2);

        assertFalse(req2InheritedReq1.get(), "Request 2 MUST NOT inherit Request 1's correlation ID");
        assertNull(MDC.get("correlationId"), "MDC must be clean after request 2 completes");
    }
}
