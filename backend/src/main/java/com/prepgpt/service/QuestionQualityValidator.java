package com.prepgpt.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Validates the quality, structural integrity, and prompt safety of AI-generated interview questions.
 *
 * Operational & Quality Constraints:
 * 1. Non-empty and reasonable length: 25 to 400 characters inclusive.
 * 2. Interrogative form: Must end with a question mark (?).
 * 3. Delimiter & Prompt-Boundary Protection: Must NOT leak internal prompt delimiters,
 *    control tokens, or prompt-injection artifacts (<system>, <user>, ---BEGIN RESUME TEXT---, etc.).
 */
@Component
public class QuestionQualityValidator {

    private static final Logger logger = LoggerFactory.getLogger(QuestionQualityValidator.class);

    public static final int MIN_LENGTH = 25;
    public static final int MAX_LENGTH = 400;

    public enum QuestionQualityRejectionReason {
        EMPTY,
        TOO_SHORT,
        TOO_LONG,
        NOT_INTERROGATIVE,
        PROMPT_BOUNDARY_LEAK,
        INVALID_CONTENT
    }

    public static class ValidationResult {
        private final boolean valid;
        private final QuestionQualityRejectionReason reason;
        private final String message;

        private ValidationResult(boolean valid, QuestionQualityRejectionReason reason, String message) {
            this.valid = valid;
            this.reason = reason;
            this.message = message;
        }

        public static ValidationResult valid() {
            return new ValidationResult(true, null, "Question meets quality standards.");
        }

        public static ValidationResult invalid(QuestionQualityRejectionReason reason, String message) {
            return new ValidationResult(false, reason, message);
        }

        public boolean isValid() {
            return valid;
        }

        public QuestionQualityRejectionReason getReason() {
            return reason;
        }

        public String getMessage() {
            return message;
        }
    }

    private static final List<String> PROMPT_DELIMITERS = List.of(
            "<system>", "</system>",
            "<user>", "</user>",
            "<assistant>", "</assistant>",
            "<job_description_content>", "</job_description_content>",
            "<resume_content>", "</resume_content>",
            "---BEGIN RESUME TEXT---", "---END RESUME TEXT---",
            "---BEGIN CONTEXT---", "---END CONTEXT---",
            "CANDIDATE RESUME SUMMARY",
            "CANDIDATE PROJECT SPECIFICATION",
            "Structured Job Description Requirements",
            "Candidate Context for Cross-Grounding",
            "REQUIRED JSON SCHEMA",
            "Ignore previous instructions",
            "You are a senior technical interviewer",
            "You are an expert technical interviewer",
            "You are a professional AI technical interviewer",
            "You are a senior software architect",
            "[INST]", "[/INST]",
            "<|im_start|>", "<|im_end|>"
    );

    /**
     * Validates a candidate question string.
     *
     * @param questionText Candidate question text to validate
     * @return ValidationResult indicating whether the question is valid or the rejection reason
     */
    public ValidationResult validate(String questionText) {
        if (questionText == null || questionText.trim().isEmpty()) {
            return ValidationResult.invalid(QuestionQualityRejectionReason.EMPTY, "Question text is empty or blank.");
        }

        String trimmed = questionText.trim();

        // 1. Length validation (25–400 characters)
        if (trimmed.length() < MIN_LENGTH) {
            return ValidationResult.invalid(QuestionQualityRejectionReason.TOO_SHORT,
                    "Question is too short (" + trimmed.length() + " chars, minimum required is " + MIN_LENGTH + ").");
        }
        if (trimmed.length() > MAX_LENGTH) {
            return ValidationResult.invalid(QuestionQualityRejectionReason.TOO_LONG,
                    "Question is too long (" + trimmed.length() + " chars, maximum allowed is " + MAX_LENGTH + ").");
        }

        // 2. Format validation: Interrogative form (must end with '?')
        if (!trimmed.endsWith("?")) {
            return ValidationResult.invalid(QuestionQualityRejectionReason.NOT_INTERROGATIVE,
                    "Question must end with an interrogative question mark (?).");
        }

        // 3. Prompt-Boundary & Control Delimiter Leakage check
        String lower = trimmed.toLowerCase();
        for (String delimiter : PROMPT_DELIMITERS) {
            if (lower.contains(delimiter.toLowerCase())) {
                logger.warn("[QuestionQuality] Question rejected due to prompt boundary/control delimiter leakage: {}", delimiter);
                return ValidationResult.invalid(QuestionQualityRejectionReason.PROMPT_BOUNDARY_LEAK,
                        "Question contains internal prompt boundary delimiter or instruction leakage.");
            }
        }

        // 4. Raw JSON schema or syntax leaks
        if (trimmed.contains("{\"") || trimmed.contains("\"question\":") || trimmed.contains("\"questionText\":")) {
            logger.warn("[QuestionQuality] Question rejected due to raw JSON schema leakage.");
            return ValidationResult.invalid(QuestionQualityRejectionReason.PROMPT_BOUNDARY_LEAK,
                    "Question contains raw JSON schema delimiters.");
        }

        return ValidationResult.valid();
    }
}
