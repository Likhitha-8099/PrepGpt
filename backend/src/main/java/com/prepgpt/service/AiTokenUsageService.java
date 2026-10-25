package com.prepgpt.service;

import com.prepgpt.entity.AiOperationType;
import com.prepgpt.entity.AiTokenUsage;
import com.prepgpt.repository.AiTokenUsageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Service for securely recording AI token usage telemetry.
 *
 * Operational Safety & Fault-Isolation:
 * Uses REQUIRES_NEW propagation and internal try-catch handling.
 * A database failure or timeout during token telemetry recording MUST NEVER fail
 * an otherwise successful candidate interview operation.
 */
@Service
public class AiTokenUsageService {

    private static final Logger logger = LoggerFactory.getLogger(AiTokenUsageService.class);

    private final AiTokenUsageRepository tokenUsageRepository;

    @Autowired
    public AiTokenUsageService(AiTokenUsageRepository tokenUsageRepository) {
        this.tokenUsageRepository = tokenUsageRepository;
    }

    /**
     * Record token telemetry in an isolated transaction.
     * Guaranteed never to throw exceptions to the caller.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordUsage(AiOperationType operationType, String provider, String model,
                            Integer promptTokens, Integer completionTokens, Integer totalTokens) {
        recordUsage(operationType, provider, model, promptTokens, completionTokens, totalTokens, null, null);
    }

    /**
     * Record token telemetry with optional session and response correlation.
     * Guaranteed never to throw exceptions to the caller.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordUsage(AiOperationType operationType, String provider, String model,
                            Integer promptTokens, Integer completionTokens, Integer totalTokens,
                            Long sessionId, Long responseId) {
        try {
            AiTokenUsage usage = new AiTokenUsage();
            usage.setOperationType(operationType != null ? operationType : AiOperationType.GENERAL_AI);
            usage.setProvider(provider != null ? provider : "GROQ");
            usage.setModel(model);
            usage.setPromptTokens(promptTokens);
            usage.setCompletionTokens(completionTokens);
            usage.setTotalTokens(totalTokens);
            usage.setSessionId(sessionId);
            usage.setResponseId(responseId);
            usage.setCreatedAt(LocalDateTime.now());

            tokenUsageRepository.save(usage);
            logger.debug("[TokenTelemetry] Persisted token usage: operation={}, provider={}, model={}, promptTokens={}, completionTokens={}, totalTokens={}",
                    operationType, provider, model, promptTokens, completionTokens, totalTokens);
        } catch (Exception e) {
            // Fault isolation: Never bubble telemetry exceptions up to disrupt user business transactions
            logger.warn("[TokenTelemetry] Failed to persist AI token usage telemetry (isolated): {}", e.getMessage());
        }
    }
}
