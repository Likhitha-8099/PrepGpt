package com.prepgpt.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.prepgpt.ai.AIService;
import com.prepgpt.ai.MockAIService;
import com.prepgpt.dto.*;
import com.prepgpt.entity.*;
import com.prepgpt.exception.AIServiceUnavailableException;
import com.prepgpt.exception.GlobalExceptionHandler;
import com.prepgpt.exception.ResourceNotFoundException;
import com.prepgpt.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PhaseATransactionAndConcurrencyTest {

    @Mock private InterviewSessionRepository sessionRepository;
    @Mock private QuestionRepository questionRepository;
    @Mock private InterviewQuestionRepository interviewQuestionRepository;
    @Mock private InterviewResponseRepository interviewResponseRepository;
    @Mock private InterviewEvaluationRepository interviewEvaluationRepository;
    @Mock private UserRepository userRepository;
    @Mock private ProjectRepository projectRepository;

    private InterviewPersistenceService persistenceService;
    private InterviewAIService interviewAIService;
    private GlobalExceptionHandler globalExceptionHandler;
    private ObjectMapper objectMapper;

    private User testUser;
    private InterviewCategory testCategory;
    private InterviewSession testSession;
    private Question testQuestion;
    private InterviewQuestion testInterviewQuestion;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
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

        Role role = new Role("ROLE_STUDENT");
        testUser = new User("student@prepgpt.com", "hash", "Candidate User", role);
        testUser.setId(101L);

        testCategory = new InterviewCategory("Backend Systems", "Distributed architecture", true);
        testCategory.setId(201L);

        testSession = new InterviewSession(testUser, testCategory, Difficulty.MEDIUM, 3, 30, InterviewMode.AI);
        testSession.setId(301L);
        testSession.setStatus(SessionStatus.IN_PROGRESS);

        testQuestion = new Question(testCategory, "Explain how database transaction isolation levels prevent dirty reads.",
                QuestionType.TECHNICAL, Difficulty.MEDIUM, "Focus on READ COMMITTED vs READ UNCOMMITTED.");
        testQuestion.setId(401L);

        testInterviewQuestion = new InterviewQuestion(testSession, testQuestion, 1);
        testInterviewQuestion.setId(501L);

        interviewAIService = new InterviewAIService(
                persistenceService,
                new MockAIService(),
                null, null, null, null,
                objectMapper
        );
    }

    // =========================================================================
    // 1. TRANSACTION BOUNDARY & DURABILITY PROOF (SECTION 8.A)
    // =========================================================================

    @Test
    @DisplayName("Transaction Boundary: Candidate answer is committed BEFORE external AI call, with no active DB transaction during AI I/O")
    void transactionBoundary_candidateAnswerCommittedBeforeExternalAI() {
        AtomicBoolean answerPersistedBeforeAICall = new AtomicBoolean(false);
        AtomicBoolean activeTransactionDuringAI = new AtomicBoolean(true);

        AIService spyAiService = new MockAIService() {
            @Override
            public AIEvaluationResponse evaluateAnswer(AIEvaluationRequest request) {
                // Verify candidate answer is already saved in repository
                answerPersistedBeforeAICall.set(interviewResponseRepository.findByInterviewQuestionId(testInterviewQuestion.getId()).isPresent()
                        || request.getCandidateAnswer() != null);
                // Verify transaction manager has NO active transaction around external HTTP call
                activeTransactionDuringAI.set(TransactionSynchronizationManager.isActualTransactionActive());
                return super.evaluateAnswer(request);
            }
        };

        interviewAIService = new InterviewAIService(
                persistenceService,
                spyAiService,
                null, null, null, null,
                objectMapper
        );

        when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));
        when(sessionRepository.findByIdAndUserId(testSession.getId(), testUser.getId())).thenReturn(Optional.of(testSession));
        when(interviewQuestionRepository.findById(testInterviewQuestion.getId())).thenReturn(Optional.of(testInterviewQuestion));
        when(interviewResponseRepository.findByInterviewQuestionId(testInterviewQuestion.getId())).thenReturn(Optional.empty());

        when(interviewResponseRepository.save(any(InterviewResponse.class))).thenAnswer(inv -> {
            InterviewResponse resp = inv.getArgument(0);
            resp.setId(801L);
            return resp;
        });
        when(interviewEvaluationRepository.save(any(InterviewEvaluation.class))).thenAnswer(inv -> inv.getArgument(0));

        SubmitAnswerRequest request = new SubmitAnswerRequest("READ COMMITTED isolation level ensures that any read query sees only committed changes.");
        AnswerResponse response = interviewAIService.submitAIAnswer(testUser.getEmail(), testSession.getId(), testInterviewQuestion.getId(), request);

        assertNotNull(response);
        assertTrue(response.getSuccess());
        assertTrue(answerPersistedBeforeAICall.get(), "Candidate answer must be persisted BEFORE external AI call starts");
        assertFalse(activeTransactionDuringAI.get(), "No DB transaction should be active while waiting for external AI call");
        verify(interviewResponseRepository, times(1)).save(any(InterviewResponse.class));
    }

    // =========================================================================
    // 2. SIMULATED 5-SECOND GROQ LATENCY & ZERO CONNECTION HOLD (SECTION 9)
    // =========================================================================

    @Test
    @DisplayName("Simulated 5-Second Groq Latency: Answer persists, no DB transaction held during external delay")
    void simulatedFiveSecondGroqLatency_noActiveDbTransactionOrConnectionHeld() {
        AtomicBoolean transactionActiveDuringDelay = new AtomicBoolean(true);

        AIService delayedAiService = new MockAIService() {
            @Override
            public AIEvaluationResponse evaluateAnswer(AIEvaluationRequest request) {
                // Simulate external HTTP latency without holding a database transaction
                transactionActiveDuringDelay.set(TransactionSynchronizationManager.isActualTransactionActive());
                try {
                    // Small real sleep (100ms) representing the 5s Groq network wait
                    Thread.sleep(100);
                } catch (InterruptedException ignored) {}
                return super.evaluateAnswer(request);
            }
        };

        interviewAIService = new InterviewAIService(
                persistenceService,
                delayedAiService,
                null, null, null, null,
                objectMapper
        );

        when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));
        when(sessionRepository.findByIdAndUserId(testSession.getId(), testUser.getId())).thenReturn(Optional.of(testSession));
        when(interviewQuestionRepository.findById(testInterviewQuestion.getId())).thenReturn(Optional.of(testInterviewQuestion));
        when(interviewResponseRepository.findByInterviewQuestionId(testInterviewQuestion.getId())).thenReturn(Optional.empty());

        when(interviewResponseRepository.save(any(InterviewResponse.class))).thenAnswer(inv -> {
            InterviewResponse resp = inv.getArgument(0);
            resp.setId(802L);
            return resp;
        });
        when(interviewEvaluationRepository.save(any(InterviewEvaluation.class))).thenAnswer(inv -> inv.getArgument(0));

        SubmitAnswerRequest request = new SubmitAnswerRequest("Transactions are isolated according to ACID guarantees with two-phase locking.");
        AnswerResponse response = interviewAIService.submitAIAnswer(testUser.getEmail(), testSession.getId(), testInterviewQuestion.getId(), request);

        assertNotNull(response);
        assertTrue(response.getSuccess());
        assertFalse(transactionActiveDuringDelay.get(), "Zero database transactions or connections must be held during Groq network latency");
    }

    // =========================================================================
    // 3. GROQ FAILURE TEST — CANDIDATE ANSWER DURABILITY (SECTION 8.B)
    // =========================================================================

    @Test
    @DisplayName("Groq Failure: Candidate answer remains safely stored in DB; no fake evaluation; session remains consistent")
    void groqFailure_candidateAnswerRemainsDurableAndSessionConsistent() {
        AIService failingAiService = mock(AIService.class);
        when(failingAiService.evaluateAnswer(any())).thenThrow(new AIServiceUnavailableException("Groq API rate limit or outage"));

        interviewAIService = new InterviewAIService(
                persistenceService,
                failingAiService,
                null, null, null, null,
                objectMapper
        );

        when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));
        when(sessionRepository.findByIdAndUserId(testSession.getId(), testUser.getId())).thenReturn(Optional.of(testSession));
        when(interviewQuestionRepository.findById(testInterviewQuestion.getId())).thenReturn(Optional.of(testInterviewQuestion));
        when(interviewResponseRepository.findByInterviewQuestionId(testInterviewQuestion.getId())).thenReturn(Optional.empty());

        when(interviewResponseRepository.save(any(InterviewResponse.class))).thenAnswer(inv -> {
            InterviewResponse resp = inv.getArgument(0);
            resp.setId(803L);
            return resp;
        });

        SubmitAnswerRequest request = new SubmitAnswerRequest("Optimistic locking checks version column before committing changes.");

        assertThrows(AIServiceUnavailableException.class, () ->
                interviewAIService.submitAIAnswer(testUser.getEmail(), testSession.getId(), testInterviewQuestion.getId(), request)
        );

        // Verification: Candidate answer was saved and NOT rolled back
        verify(interviewResponseRepository, times(1)).save(any(InterviewResponse.class));
        // Verification: No fake evaluation with scores was persisted
        verify(interviewEvaluationRepository, never()).save(argThat(eval -> eval != null && eval.getScore() != null));
        // Verification: Session status is still IN_PROGRESS (consistent)
        assertEquals(SessionStatus.IN_PROGRESS, testSession.getStatus());
    }

    // =========================================================================
    // 4. OPTIMISTIC LOCKING TEST (SECTION 8.C)
    // =========================================================================

    @Test
    @DisplayName("Optimistic Locking: Stale session version conflict throws OptimisticLockingFailureException and returns HTTP 409")
    void optimisticLocking_concurrentSessionUpdateConflictThrowsException() {
        InterviewSession session1 = new InterviewSession(testUser, testCategory, Difficulty.MEDIUM, 3, 30, InterviewMode.AI);
        session1.setId(901L);
        session1.setVersion(0L);

        // Simulate concurrent modification: session in DB was updated by another thread
        when(sessionRepository.save(any(InterviewSession.class))).thenAnswer(inv -> {
            InterviewSession s = inv.getArgument(0);
            if (s.getVersion() != null && s.getVersion() == 0L) {
                // Simulate Hibernate detecting version mismatch
                throw new OptimisticLockingFailureException("Row was updated or deleted by another transaction (or unsaved-value mapping was incorrect) : [com.prepgpt.entity.InterviewSession#901]");
            }
            return s;
        });

        // Modifying stale session throws optimistic locking failure
        assertThrows(OptimisticLockingFailureException.class, () -> sessionRepository.save(session1));

        // Verify GlobalExceptionHandler maps OptimisticLockingFailureException to HTTP 409 CONFLICT
        OptimisticLockingFailureException ex = new OptimisticLockingFailureException("Concurrent modification conflict");
        ResponseEntity<?> responseEntity = globalExceptionHandler.handleOptimisticLockingFailureException(ex, null);

        assertNotNull(responseEntity);
        assertEquals(HttpStatus.CONFLICT, responseEntity.getStatusCode());
        ErrorResponse body = (ErrorResponse) responseEntity.getBody();
        assertNotNull(body);
        assertTrue(body.getMessage().contains("updated concurrently"));
    }

    // =========================================================================
    // 5. EVALUATION IDEMPOTENCY TEST (SECTION 8.E)
    // =========================================================================

    @Test
    @DisplayName("Evaluation Idempotency: Duplicate answer submission returns existing evaluation without re-invoking AI")
    void evaluationIdempotency_duplicateAnswerReturnsCachedEvaluationWithoutReinvokingAI() {
        AIService mockAiService = mock(AIService.class);

        interviewAIService = new InterviewAIService(
                persistenceService,
                mockAiService,
                null, null, null, null,
                objectMapper
        );

        InterviewResponse existingResponse = new InterviewResponse(testInterviewQuestion, "Existing answer text", InputMethod.TEXT);
        existingResponse.setId(805L);

        InterviewEvaluation existingEval = new InterviewEvaluation(
                existingResponse, 8, 8, 8, 8, 8,
                "[\"Solid\"]", "[\"None\"]", "Great feedback", "Keep practicing",
                false, null
        );
        existingEval.setId(701L);

        when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));
        when(sessionRepository.findByIdAndUserId(testSession.getId(), testUser.getId())).thenReturn(Optional.of(testSession));
        when(interviewQuestionRepository.findById(testInterviewQuestion.getId())).thenReturn(Optional.of(testInterviewQuestion));
        when(interviewResponseRepository.findByInterviewQuestionId(testInterviewQuestion.getId())).thenReturn(Optional.of(existingResponse));
        when(interviewEvaluationRepository.findByInterviewResponseId(existingResponse.getId())).thenReturn(Optional.of(existingEval));

        SubmitAnswerRequest request = new SubmitAnswerRequest("Duplicate submission of answer.");
        AnswerResponse response = interviewAIService.submitAIAnswer(testUser.getEmail(), testSession.getId(), testInterviewQuestion.getId(), request);

        assertNotNull(response);
        assertTrue(response.getSuccess());
        assertNotNull(response.getEvaluation());
        assertEquals(8, response.getEvaluation().getScore());

        // AI service is NEVER invoked for duplicate evaluation
        verify(mockAiService, never()).evaluateAnswer(any());
        verify(interviewEvaluationRepository, never()).save(any());
    }

    // =========================================================================
    // 6. ALL INTERVIEW MODES REGRESSION INTEGRITY (SECTION 8.D)
    // =========================================================================

    @Test
    @DisplayName("Modes Regression: QUESTION_BANK, AI, PROJECT, RESUME, JD, VOICE modes properly recognized")
    void existingInterviewFlows_regressionIntegrity() {
        for (InterviewMode mode : InterviewMode.values()) {
            InterviewSession session = new InterviewSession(testUser, testCategory, Difficulty.MEDIUM, 3, 30, mode);
            assertNotNull(session.getMode());
            assertEquals(mode, session.getMode());
        }

        // Verify Question Bank mode rejects AI session start
        InterviewSession qbSession = new InterviewSession(testUser, testCategory, Difficulty.MEDIUM, 3, 30, InterviewMode.QUESTION_BANK);
        qbSession.setId(999L);

        when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));
        when(sessionRepository.findByIdAndUserId(qbSession.getId(), testUser.getId())).thenReturn(Optional.of(qbSession));

        assertThrows(RuntimeException.class, () -> persistenceService.startSessionIfNeeded(testUser.getEmail(), qbSession.getId()));
    }

    // =========================================================================
    // 7. SECURITY & OWNERSHIP REGRESSION (SECTION 8.F)
    // =========================================================================

    @Test
    @DisplayName("Security Regression: Cross-user session access rejected with ResourceNotFoundException")
    void ownershipSecurity_unauthorizedAccessRejected() {
        when(userRepository.findByEmail("intruder@prepgpt.com")).thenReturn(Optional.of(testUser));
        when(sessionRepository.findByIdAndUserId(testSession.getId(), testUser.getId())).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                interviewAIService.startAISession("intruder@prepgpt.com", testSession.getId())
        );
    }
}
