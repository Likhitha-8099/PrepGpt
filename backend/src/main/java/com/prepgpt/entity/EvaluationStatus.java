package com.prepgpt.entity;

/**
 * Lifecycle status of an interview evaluation.
 * Supports asynchronous self-healing and failure recovery via the Evaluation Retry Reaper.
 */
public enum EvaluationStatus {
    /**
     * Answer received, evaluation is currently being processed by the AI provider.
     */
    PENDING,

    /**
     * AI evaluation completed successfully with final scores, feedback, and adaptive follow-up.
     */
    COMPLETED,

    /**
     * Transient failure occurred during AI evaluation (e.g., 503, 429, network timeout).
     * Eligible for background recovery by the Evaluation Retry Reaper.
     */
    FAILED_RETRYABLE,

    /**
     * Maximum retry attempts exhausted without successful evaluation.
     * Candidate answer remains safely preserved; no fabricated scores.
     */
    DEAD_LETTER
}
