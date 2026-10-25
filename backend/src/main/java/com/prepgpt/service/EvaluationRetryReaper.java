package com.prepgpt.service;

import com.prepgpt.ai.AIService;
import com.prepgpt.dto.AIEvaluationRequest;
import com.prepgpt.dto.AIEvaluationResponse;
import com.prepgpt.dto.AIFollowUpRequest;
import com.prepgpt.dto.AIFollowUpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * DB-Backed Evaluation Retry Reaper.
 * Background worker that claims eligible failed evaluations and re-attempts AI evaluation.
 *
 * CRITICAL ARCHITECTURAL GUARANTEE:
 * 1. Claims work in a discrete DB micro-transaction (marks PENDING with optimistic locking).
 * 2. Calls external Groq API strictly OUTSIDE any database transaction.
 * 3. Persists results or failure backoff in a separate discrete DB micro-transaction.
 * 4. Advances to DEAD_LETTER when maximum retry attempts are exhausted.
 */
@Service
public class EvaluationRetryReaper {

    private static final Logger logger = LoggerFactory.getLogger(EvaluationRetryReaper.class);

    private final InterviewPersistenceService persistenceService;
    private final AIService aiService;
    private final int maxAttempts;
    private final int initialDelaySeconds;
    private final double backoffMultiplier;

    @Autowired
    public EvaluationRetryReaper(
            InterviewPersistenceService persistenceService,
            AIService aiService,
            @Value("${prepgpt.evaluation.retry.max-attempts:3}") int maxAttempts,
            @Value("${prepgpt.evaluation.retry.initial-delay-seconds:60}") int initialDelaySeconds,
            @Value("${prepgpt.evaluation.retry.backoff-multiplier:2.0}") double backoffMultiplier) {
        this.persistenceService = persistenceService;
        this.aiService = aiService;
        this.maxAttempts = maxAttempts;
        this.initialDelaySeconds = initialDelaySeconds;
        this.backoffMultiplier = backoffMultiplier;
    }

    @Scheduled(cron = "${prepgpt.evaluation.retry.cron:0 */1 * * * *}")
    public void retryFailedEvaluations() {
        String jobCorrelationId = "reaper-" + java.util.UUID.randomUUID();
        org.slf4j.MDC.put("correlationId", jobCorrelationId);
        try {
            // STEP 1 — DB MICRO-TX: Claim eligible evaluations
            List<Long> evalIds;
            try {
                evalIds = persistenceService.claimEligibleEvaluationIdsForRetry(maxAttempts);
            } catch (Exception e) {
                logger.error("[Reaper] Failed to claim eligible evaluations for retry: {}", e.getMessage(), e);
                return;
            }

            if (evalIds.isEmpty()) {
                return;
            }

            logger.info("[Reaper] Claimed {} eligible evaluations for self-healing retry.", evalIds.size());

            for (Long evalId : evalIds) {
                processSingleEvaluation(evalId);
            }
        } finally {
            org.slf4j.MDC.remove("correlationId");
        }
    }

    public void processSingleEvaluation(Long evalId) {
        // STEP 1.1 — DB MICRO-TX: Read context for claimed evaluation
        InterviewPersistenceService.CandidateAnswerContext ctx;
        try {
            ctx = persistenceService.prepareContextForEvaluationId(evalId);
        } catch (Exception e) {
            logger.warn("[Reaper] Failed to prepare context for evaluation #{}: {}", evalId, e.getMessage());
            return;
        }

        // STEP 2 — EXTERNAL AI CALL (Zero active DB transaction)
        AIEvaluationResponse aiEval;
        AIFollowUpResponse aiFollowUp;
        try {
            AIEvaluationRequest evalReq = new AIEvaluationRequest(
                    ctx.getCategoryName(),
                    ctx.getCurrentDifficulty(),
                    ctx.getQuestionText(),
                    ctx.getAnswerText(),
                    ctx.getExpectedAnswerGuidance(),
                    ctx.getEvalContext()
            );

            aiEval = aiService.evaluateAnswer(evalReq);

            String followUpContext = ctx.getEvalContext() + "\nCandidate Score: " + aiEval.getScore() + "/10. Feedback: " + aiEval.getFeedback();
            AIFollowUpRequest followUpReq = new AIFollowUpRequest(
                    ctx.getCategoryName(),
                    ctx.getCurrentDifficulty(),
                    ctx.getQuestionText(),
                    ctx.getAnswerText(),
                    aiEval,
                    followUpContext
            );

            aiFollowUp = aiService.generateFollowUp(followUpReq);
        } catch (Exception e) {
            logger.warn("[Reaper] Retry attempt failed for evaluation #{} (response #{}): {}", evalId, ctx.getResponseId(), e.getMessage());
            // STEP 3A — DB MICRO-TX: Record reaper retry failure and back off
            try {
                persistenceService.recordReaperFailure(evalId, e.getMessage(), initialDelaySeconds, backoffMultiplier, maxAttempts);
            } catch (Exception pe) {
                logger.error("[Reaper] Failed to record reaper failure for evaluation #{}: {}", evalId, pe.getMessage());
            }
            return;
        }

        // STEP 3B — DB MICRO-TX: Persist successful evaluation and update state
        try {
            persistenceService.persistEvaluationAndAdaptiveState(ctx.getResponseId(), aiEval, aiFollowUp);
            logger.info("[Reaper] Successfully recovered and evaluated evaluation #{} for response #{}", evalId, ctx.getResponseId());
        } catch (Exception e) {
            logger.error("[Reaper] Failed to persist successful evaluation #{} for response #{}: {}", evalId, ctx.getResponseId(), e.getMessage(), e);
        }
    }
}
