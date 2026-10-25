package com.prepgpt.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.prepgpt.ai.AIService;
import com.prepgpt.dto.*;
import com.prepgpt.entity.*;
import com.prepgpt.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * AI Interview Orchestrator.
 * Coordinates candidate answer evaluation, follow-up decisions, and adaptive question generation.
 *
 * CRITICAL ARCHITECTURAL GUARANTEE:
 * All external AI HTTP network calls (Groq API chat completions) execute strictly OUTSIDE
 * database transactions. Database reads and writes are encapsulated in discrete, rapid
 * micro-transactions via InterviewPersistenceService to eliminate HikariCP connection starvation.
 */
@Service
public class InterviewAIService {

    private static final Logger logger = LoggerFactory.getLogger(InterviewAIService.class);

    private final InterviewPersistenceService persistenceService;
    private final AIService aiService;
    private final com.prepgpt.ai.prompt.ResumeQuestionPromptBuilder resumeQuestionPromptBuilder;
    private final com.prepgpt.ai.prompt.ProjectQuestionPromptBuilder projectQuestionPromptBuilder;
    private final com.prepgpt.ai.prompt.JdQuestionPromptBuilder jdQuestionPromptBuilder;
    private final com.prepgpt.ai.GroqAIService groqAIService;
    private final ObjectMapper objectMapper;
    private final QuestionQualityValidator questionQualityValidator;

    @Autowired
    public InterviewAIService(InterviewPersistenceService persistenceService,
                             AIService aiService,
                             com.prepgpt.ai.prompt.ResumeQuestionPromptBuilder resumeQuestionPromptBuilder,
                             com.prepgpt.ai.prompt.ProjectQuestionPromptBuilder projectQuestionPromptBuilder,
                             com.prepgpt.ai.prompt.JdQuestionPromptBuilder jdQuestionPromptBuilder,
                             com.prepgpt.ai.GroqAIService groqAIService,
                             ObjectMapper objectMapper,
                             @Autowired(required = false) QuestionQualityValidator questionQualityValidator) {
        this.persistenceService = persistenceService;
        this.aiService = aiService;
        this.resumeQuestionPromptBuilder = resumeQuestionPromptBuilder != null ? resumeQuestionPromptBuilder : new com.prepgpt.ai.prompt.ResumeQuestionPromptBuilder();
        this.projectQuestionPromptBuilder = projectQuestionPromptBuilder != null ? projectQuestionPromptBuilder : new com.prepgpt.ai.prompt.ProjectQuestionPromptBuilder();
        this.jdQuestionPromptBuilder = jdQuestionPromptBuilder != null ? jdQuestionPromptBuilder : new com.prepgpt.ai.prompt.JdQuestionPromptBuilder();
        this.groqAIService = groqAIService != null ? groqAIService : (aiService instanceof com.prepgpt.ai.GroqAIService ? (com.prepgpt.ai.GroqAIService) aiService : null);
        this.objectMapper = objectMapper != null ? objectMapper : new ObjectMapper();
        this.questionQualityValidator = questionQualityValidator != null ? questionQualityValidator : new QuestionQualityValidator();
    }

    public InterviewAIService(InterviewPersistenceService persistenceService,
                             AIService aiService,
                             com.prepgpt.ai.prompt.ResumeQuestionPromptBuilder resumeQuestionPromptBuilder,
                             com.prepgpt.ai.prompt.ProjectQuestionPromptBuilder projectQuestionPromptBuilder,
                             com.prepgpt.ai.prompt.JdQuestionPromptBuilder jdQuestionPromptBuilder,
                             com.prepgpt.ai.GroqAIService groqAIService,
                             ObjectMapper objectMapper) {
        this(persistenceService, aiService, resumeQuestionPromptBuilder, projectQuestionPromptBuilder, jdQuestionPromptBuilder, groqAIService, objectMapper, null);
    }

    public InterviewAIService(InterviewSessionRepository sessionRepository,
                             QuestionRepository questionRepository,
                             InterviewQuestionRepository interviewQuestionRepository,
                             InterviewResponseRepository interviewResponseRepository,
                             InterviewEvaluationRepository interviewEvaluationRepository,
                             UserRepository userRepository,
                             AIService aiService,
                             ObjectMapper objectMapper) {
        this(new InterviewPersistenceService(sessionRepository, questionRepository, interviewQuestionRepository,
                interviewResponseRepository, interviewEvaluationRepository, userRepository, objectMapper),
             aiService,
             new com.prepgpt.ai.prompt.ResumeQuestionPromptBuilder(),
             new com.prepgpt.ai.prompt.ProjectQuestionPromptBuilder(),
             new com.prepgpt.ai.prompt.JdQuestionPromptBuilder(),
             aiService instanceof com.prepgpt.ai.GroqAIService ? (com.prepgpt.ai.GroqAIService) aiService : null,
             objectMapper);
    }

    public InterviewAIService(InterviewSessionRepository sessionRepository,
                             QuestionRepository questionRepository,
                             InterviewQuestionRepository interviewQuestionRepository,
                             InterviewResponseRepository interviewResponseRepository,
                             InterviewEvaluationRepository interviewEvaluationRepository,
                             UserRepository userRepository,
                             ProjectRepository projectRepository,
                             AIService aiService,
                             com.prepgpt.ai.prompt.ResumeQuestionPromptBuilder resumeQuestionPromptBuilder,
                             com.prepgpt.ai.prompt.ProjectQuestionPromptBuilder projectQuestionPromptBuilder,
                             com.prepgpt.ai.prompt.JdQuestionPromptBuilder jdQuestionPromptBuilder,
                             com.prepgpt.ai.GroqAIService groqAIService,
                             ObjectMapper objectMapper) {
        this(new InterviewPersistenceService(sessionRepository, questionRepository, interviewQuestionRepository,
                interviewResponseRepository, interviewEvaluationRepository, userRepository, projectRepository, objectMapper),
             aiService,
             resumeQuestionPromptBuilder,
             projectQuestionPromptBuilder,
             jdQuestionPromptBuilder,
             groqAIService,
             objectMapper);
    }

    // =========================================================================
    // START AI INTERVIEW SESSION
    // =========================================================================

    public InterviewSessionResponse startAISession(String userEmail, Long sessionId) {
        // STEP 1 — DATABASE MICRO-TRANSACTION
        InterviewPersistenceService.StartSessionState state = persistenceService.startSessionIfNeeded(userEmail, sessionId);

        if (!state.isNeedsQuestion1()) {
            return state.getSessionResponse();
        }

        // STEP 2 — EXTERNAL AI CALL (Zero active DB transaction)
        GeneratedQuestionDetails generated = generateQuestionDetails(state.getQuestion1Params(), Collections.emptyList(), null);

        // STEP 3 — DATABASE MICRO-TRANSACTION
        persistenceService.persistGeneratedQuestion(
                userEmail,
                sessionId,
                1,
                generated.text,
                generated.type,
                generated.difficulty,
                generated.guidance,
                generated.groundedSkill,
                generated.groundedResponsibility,
                generated.groundingCategory
        );

        return state.getSessionResponse();
    }

    // =========================================================================
    // GET CURRENT / ADAPTIVE QUESTION
    // =========================================================================

    public QuestionResponse getCurrentAIQuestion(String userEmail, Long sessionId) {
        // STEP 1 — DATABASE MICRO-TRANSACTION
        InterviewPersistenceService.CurrentQuestionState state = persistenceService.getCurrentQuestionState(userEmail, sessionId);

        if (!state.isNeedsGeneration()) {
            return state.getExistingQuestionResponse();
        }

        // STEP 2 — EXTERNAL AI CALL (Zero active DB transaction)
        GeneratedQuestionDetails generated = generateAdaptiveQuestionWithDuplicateProtection(state.getGenerationParams());

        // STEP 3 — DATABASE MICRO-TRANSACTION
        return persistenceService.persistGeneratedQuestion(
                userEmail,
                sessionId,
                state.getGenerationParams().getNextOrder(),
                generated.text,
                generated.type,
                generated.difficulty,
                generated.guidance,
                generated.groundedSkill,
                generated.groundedResponsibility,
                generated.groundingCategory
        );
    }

    // =========================================================================
    // SUBMIT ANSWER AND EVALUATE (CORE TRANSACTION BOUNDARY FIX)
    // =========================================================================

    public AnswerResponse submitAIAnswer(String userEmail, Long sessionId, Long questionId, SubmitAnswerRequest request) {
        // STEP 1 — DATABASE MICRO-TRANSACTION
        // Candidate answer is validated, stored, and committed to DB BEFORE any external AI call.
        InterviewPersistenceService.CandidateAnswerContext ctx = persistenceService.saveCandidateAnswerAndPrepareContext(
                userEmail, sessionId, questionId, request.getAnswerText(), request.getInputMethod()
        );

        // Evaluation Idempotency: Return cached evaluation immediately if already present
        if (ctx.hasExistingEvaluation()) {
            return new AnswerResponse(true, questionId, true, ctx.isAlreadyCompleted(), ctx.getExistingEvaluation());
        }

        // STEP 2 — EXTERNAL AI CALL (OUTSIDE ANY DATABASE TRANSACTION)
        // No HikariCP database connection is held while waiting for Groq network I/O.
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
            // Answer Durability: Candidate answer safely survived in MySQL even though AI failed.
            // Record failure metadata so the DB-backed Evaluation Retry Reaper can recover it.
            logger.error("AI evaluation failed for response #{}, but candidate answer remains safely persisted: {}",
                    ctx.getResponseId(), e.getMessage());
            try {
                persistenceService.recordEvaluationFailure(ctx.getResponseId(), e.getMessage(), 60, 3);
            } catch (Exception pe) {
                logger.warn("Could not record evaluation failure status for response #{}: {}", ctx.getResponseId(), pe.getMessage());
            }
            throw e;
        }

        // STEP 3 — DATABASE MICRO-TRANSACTION
        // Persist evaluation and adaptive state updates in a rapid, isolated transaction.
        return persistenceService.persistEvaluationAndAdaptiveState(ctx.getResponseId(), aiEval, aiFollowUp);
    }

    // =========================================================================
    // SESSION DETAILS
    // =========================================================================

    public InterviewSessionResponse getAISessionDetails(String userEmail, Long sessionId) {
        return persistenceService.getAISessionDetails(userEmail, sessionId);
    }

    // =========================================================================
    // BACKWARD COMPATIBILITY HELPERS FOR EXISTING UNIT TESTS
    // =========================================================================

    public InterviewResponse saveCandidateAnswer(String userEmail, Long sessionId, Long questionId, String answerTextRaw, InputMethod inputMethod) {
        InterviewPersistenceService.CandidateAnswerContext ctx = persistenceService.saveCandidateAnswerAndPrepareContext(
                userEmail, sessionId, questionId, answerTextRaw, inputMethod
        );
        InterviewResponse resp = new InterviewResponse();
        resp.setId(ctx.getResponseId());
        resp.setAnswerText(ctx.getAnswerText());
        return resp;
    }

    public InterviewEvaluation evaluateAndPersistAIResponse(InterviewSession session, InterviewQuestion interviewQuestion, InterviewResponse savedResponse, String answerText) {
        Difficulty currentQuestionDiff = interviewQuestion.getQuestion().getDifficulty() != null ? interviewQuestion.getQuestion().getDifficulty() : session.getDifficulty();

        String evalContext = "Session total questions count: " + session.getQuestionCount();
        if (session.getMode() == InterviewMode.PROJECT) {
            String projCtx = persistenceService.buildProjectEvaluationContext(session);
            if (projCtx != null) {
                evalContext = projCtx + "\n\n" + evalContext;
            }
        }

        AIEvaluationRequest evalReq = new AIEvaluationRequest(
                session.getCategory().getName(),
                currentQuestionDiff,
                interviewQuestion.getQuestion().getQuestionText(),
                answerText,
                interviewQuestion.getQuestion().getExpectedAnswerGuidance(),
                evalContext
        );

        AIEvaluationResponse aiEval = aiService.evaluateAnswer(evalReq);

        String followUpContext = evalContext + "\nCandidate Score: " + aiEval.getScore() + "/10. Feedback: " + aiEval.getFeedback();
        AIFollowUpRequest followUpReq = new AIFollowUpRequest(
                session.getCategory().getName(),
                currentQuestionDiff,
                interviewQuestion.getQuestion().getQuestionText(),
                answerText,
                aiEval,
                followUpContext
        );

        AIFollowUpResponse aiFollowUp = aiService.generateFollowUp(followUpReq);

        return persistenceService.saveEvaluationDirect(session, interviewQuestion, savedResponse, aiEval, aiFollowUp);
    }

    // =========================================================================
    // EXTERNAL AI QUESTION GENERATION (ZERO DB TRANSACTION HELD)
    // =========================================================================

    private GeneratedQuestionDetails generateAdaptiveQuestionWithDuplicateProtection(InterviewPersistenceService.AdaptiveQuestionParams params) {
        List<String> previousQuestions = params.getPreviousQuestions();
        String recommendedNextQ = params.getRecommendedNextQ();
        String targetConcept = params.getTargetConcept();
        Difficulty targetDifficulty = params.getTargetDifficulty();
        AdaptiveDecision decision = params.getLastDecision();

        // Step A: If recommended next question is provided by AI, validate quality THEN uniqueness
        if (recommendedNextQ != null && !recommendedNextQ.isBlank()) {
            QuestionQualityValidator.ValidationResult qualityResult = questionQualityValidator.validate(recommendedNextQ);
            if (qualityResult.isValid()) {
                if (!isDuplicateQuestion(recommendedNextQ, previousQuestions)) {
                    return new GeneratedQuestionDetails(
                            recommendedNextQ.trim(),
                            QuestionType.TECHNICAL,
                            targetDifficulty,
                            "Adaptive question targeting: " + (targetConcept != null ? targetConcept : "concept mastery"),
                            null, null, null
                    );
                }
                logger.warn("Recommended follow-up question is duplicate of existing session question. Regenerating...");
            } else {
                logger.warn("Recommended follow-up question failed quality validation ({}). Regenerating...", qualityResult.getMessage());
            }
        }

        // Step B: Bounded retry loop (max 3 retries)
        for (int attempt = 1; attempt <= 3; attempt++) {
            String context = buildAdaptiveQuestionContext(decision, targetConcept, attempt);

            if (params.getMode() == InterviewMode.PROJECT && params.getProject() != null && groqAIService != null) {
                try {
                    String prompt = projectQuestionPromptBuilder.buildProjectQuestionPrompt(
                            params.getProject(), targetDifficulty,
                            params.getNextOrder(), params.getTotalQuestions(), previousQuestions, context
                    );
                    String rawJson = groqAIService.callGroqChatApi(prompt);
                    JsonNode root = objectMapper.readTree(rawJson);
                    String qText = root.path("question").asText(null);
                    if (qText != null && !qText.isBlank()) {
                        QuestionQualityValidator.ValidationResult qvResult = questionQualityValidator.validate(qText);
                        if (!qvResult.isValid()) {
                            logger.warn("Attempt #{}: Project adaptive question failed quality validation ({}). Retrying...", attempt, qvResult.getMessage());
                            continue;
                        }
                        if (isDuplicateQuestion(qText, previousQuestions)) {
                            logger.warn("Attempt #{}: Project adaptive question is duplicate. Retrying...", attempt);
                            continue;
                        }
                        String rationale = root.path("rationale").asText("Project-grounded adaptive question.");
                        return new GeneratedQuestionDetails(qText.trim(), QuestionType.TECHNICAL, targetDifficulty, rationale, null, null, null);
                    }
                } catch (Exception e) {
                    logger.warn("Adaptive project question retry #{} failed, continuing retry loop: {}", attempt, e.getMessage());
                }
            } else {
                AIQuestionRequest req = new AIQuestionRequest(
                        params.getCategoryName(),
                        targetDifficulty,
                        QuestionType.TECHNICAL,
                        previousQuestions,
                        context,
                        params.getNextOrder(),
                        params.getTotalQuestions()
                );

                try {
                    AIQuestionResponse aiResp = aiService.generateQuestion(req);
                    String candidateText = aiResp.getQuestion();

                    if (candidateText != null && !candidateText.isBlank()) {
                        QuestionQualityValidator.ValidationResult qvResult = questionQualityValidator.validate(candidateText);
                        if (!qvResult.isValid()) {
                            logger.warn("Attempt #{}: AI candidate question failed quality validation ({}). Retrying...", attempt, qvResult.getMessage());
                            continue;
                        }
                        if (isDuplicateQuestion(candidateText, previousQuestions)) {
                            logger.warn("Attempt #{}: Generated candidate question is duplicate. Retrying...", attempt);
                            continue;
                        }
                        return new GeneratedQuestionDetails(
                                candidateText.trim(),
                                aiResp.getQuestionType(),
                                targetDifficulty != null ? targetDifficulty : aiResp.getDifficulty(),
                                aiResp.getRationale(),
                                null, null, null
                        );
                    }
                } catch (Exception e) {
                    logger.warn("Adaptive AI question retry #{} failed, continuing retry loop: {}", attempt, e.getMessage());
                }
            }
        }

        // Step C: Fallback to unique variant if retries exhausted
        String fallbackTopic = targetConcept != null && !targetConcept.isBlank() ? targetConcept :
                (params.getMode() == InterviewMode.PROJECT && params.getProjectTitleSnapshot() != null ? params.getProjectTitleSnapshot() : params.getCategoryName());
        String fallbackText = "Could you explain another key aspect of " + fallbackTopic + " in detail?";
        if (isDuplicateQuestion(fallbackText, previousQuestions)) {
            fallbackText = "What are the core technical trade-offs when implementing " + fallbackTopic + " in production systems?";
        }

        return new GeneratedQuestionDetails(fallbackText, QuestionType.TECHNICAL, targetDifficulty, "Adaptive fallback question.", null, null, null);
    }

    private GeneratedQuestionDetails generateQuestionDetails(InterviewPersistenceService.AdaptiveQuestionParams params,
                                                             List<String> previousQuestions, String context) {
        for (int attempt = 1; attempt <= 3; attempt++) {
            if (params.getMode() == InterviewMode.JOB_DESCRIPTION || params.getJdTargetRole() != null) {
                try {
                    String prompt = jdQuestionPromptBuilder.buildQuestionPrompt(
                            params.getJdTargetRole(),
                            params.getJdExtractedSkillsJson(),
                            params.getJdKeyResponsibilitiesJson(),
                            params.getResumeSkillsSnapshotJson(),
                            params.getProjectTitleSnapshot(),
                            params.getProjectTechSnapshot(),
                            params.getTargetDifficulty(),
                            params.getNextOrder()
                    );
                    String rawJson = groqAIService.callGroqChatApi(prompt);
                    String jsonBlock = extractJsonBlock(rawJson);
                    JsonNode root = objectMapper.readTree(jsonBlock);

                    String qText = root.path("questionText").asText(null);
                    if (qText == null || qText.isBlank()) {
                        qText = root.path("question").asText(null);
                    }

                    if (qText != null && !qText.isBlank()) {
                        QuestionQualityValidator.ValidationResult qvResult = questionQualityValidator.validate(qText);
                        if (!qvResult.isValid()) {
                            logger.warn("Attempt #{}: JD question failed quality validation ({}). Retrying...", attempt, qvResult.getMessage());
                            continue;
                        }
                        if (isDuplicateQuestion(qText, previousQuestions)) {
                            logger.warn("Attempt #{}: JD question is duplicate. Retrying...", attempt);
                            continue;
                        }

                        String groundedSkill = root.path("groundedSkill").asText("N/A");
                        String groundedResp = root.path("groundedResponsibility").asText("N/A");
                        String groundingCategory = root.path("groundingCategory").asText("TECHNICAL");
                        String guidance = root.path("expectedAnswerGuidance").asText("JD grounded question.");

                        String validatedSkill = validateGroundedSkillAgainstSnapshot(groundedSkill, params.getJdExtractedSkillsJson());
                        String validatedResp = validateGroundedResponsibilityAgainstSnapshot(groundedResp, params.getJdKeyResponsibilitiesJson());

                        return new GeneratedQuestionDetails(
                                qText.trim(), QuestionType.JOB_DESCRIPTION, params.getTargetDifficulty(), guidance,
                                validatedSkill, validatedResp, groundingCategory
                        );
                    }
                } catch (Exception e) {
                    logger.warn("Attempt #{}: JD question prompt generation failed: {}", attempt, e.getMessage());
                }
            } else if (params.getMode() == InterviewMode.RESUME && params.getResume() != null) {
                try {
                    String prompt = resumeQuestionPromptBuilder.buildResumeQuestionPrompt(
                            params.getResume(), params.getTargetDifficulty(), params.getNextOrder(), params.getTotalQuestions(), previousQuestions, context
                    );
                    String rawJson = groqAIService.callGroqChatApi(prompt);
                    JsonNode root = objectMapper.readTree(rawJson);
                    String qText = root.path("question").asText(null);
                    if (qText != null && !qText.isBlank()) {
                        QuestionQualityValidator.ValidationResult qvResult = questionQualityValidator.validate(qText);
                        if (!qvResult.isValid()) {
                            logger.warn("Attempt #{}: Resume question failed quality validation ({}). Retrying...", attempt, qvResult.getMessage());
                            continue;
                        }
                        if (isDuplicateQuestion(qText, previousQuestions)) {
                            logger.warn("Attempt #{}: Resume question is duplicate. Retrying...", attempt);
                            continue;
                        }
                        String rationale = root.path("rationale").asText("Resume-grounded question.");
                        return new GeneratedQuestionDetails(qText.trim(), QuestionType.TECHNICAL, params.getTargetDifficulty(), rationale, null, null, null);
                    }
                } catch (Exception e) {
                    logger.warn("Attempt #{}: Resume question prompt generation failed: {}", attempt, e.getMessage());
                }
            } else if (params.getMode() == InterviewMode.PROJECT && params.getProject() != null) {
                try {
                    String prompt = projectQuestionPromptBuilder.buildProjectQuestionPrompt(
                            params.getProject(), params.getTargetDifficulty(), params.getNextOrder(), params.getTotalQuestions(), previousQuestions, context
                    );
                    String rawJson = groqAIService.callGroqChatApi(prompt);
                    JsonNode root = objectMapper.readTree(rawJson);
                    String qText = root.path("question").asText(null);
                    if (qText != null && !qText.isBlank()) {
                        QuestionQualityValidator.ValidationResult qvResult = questionQualityValidator.validate(qText);
                        if (!qvResult.isValid()) {
                            logger.warn("Attempt #{}: Project question failed quality validation ({}). Retrying...", attempt, qvResult.getMessage());
                            continue;
                        }
                        if (isDuplicateQuestion(qText, previousQuestions)) {
                            logger.warn("Attempt #{}: Project question is duplicate. Retrying...", attempt);
                            continue;
                        }
                        String rationale = root.path("rationale").asText("Project-grounded question.");
                        return new GeneratedQuestionDetails(qText.trim(), QuestionType.TECHNICAL, params.getTargetDifficulty(), rationale, null, null, null);
                    }
                } catch (Exception e) {
                    logger.warn("Attempt #{}: Project question prompt generation failed: {}", attempt, e.getMessage());
                }
            } else {
                AIQuestionRequest req = new AIQuestionRequest(
                        params.getCategoryName(),
                        params.getTargetDifficulty(),
                        QuestionType.TECHNICAL,
                        previousQuestions,
                        context,
                        params.getNextOrder(),
                        params.getTotalQuestions()
                );
                try {
                    AIQuestionResponse aiResp = aiService.generateQuestion(req);
                    String candidateText = aiResp.getQuestion();
                    if (candidateText != null && !candidateText.isBlank()) {
                        QuestionQualityValidator.ValidationResult qvResult = questionQualityValidator.validate(candidateText);
                        if (!qvResult.isValid()) {
                            logger.warn("Attempt #{}: Standard AI question failed quality validation ({}). Retrying...", attempt, qvResult.getMessage());
                            continue;
                        }
                        if (isDuplicateQuestion(candidateText, previousQuestions)) {
                            logger.warn("Attempt #{}: Standard AI question is duplicate. Retrying...", attempt);
                            continue;
                        }
                        return new GeneratedQuestionDetails(
                                candidateText.trim(), aiResp.getQuestionType(), aiResp.getDifficulty(), aiResp.getRationale(),
                                null, null, null
                        );
                    }
                } catch (Exception e) {
                    logger.warn("Attempt #{}: Standard AI question generation failed: {}", attempt, e.getMessage());
                }
            }
        }

        // Bounded fallback if all generation attempts fail quality or uniqueness
        String fallbackCategory = params.getCategoryName() != null ? params.getCategoryName() : "Software Engineering";
        String fallbackText = "Could you explain the core concepts and fundamental architecture of " + fallbackCategory + "?";
        return new GeneratedQuestionDetails(
                fallbackText,
                QuestionType.TECHNICAL,
                params.getTargetDifficulty(),
                "Safe initial question fallback.",
                null, null, null
        );
    }

    private String buildAdaptiveQuestionContext(AdaptiveDecision decision, String targetConcept, int attempt) {
        StringBuilder sb = new StringBuilder();
        if (decision != null) {
            sb.append("Adaptive Strategy: ").append(decision.name()).append(". ");
        }
        if (targetConcept != null && !targetConcept.isBlank()) {
            sb.append("Target Concept: ").append(targetConcept).append(". ");
        }
        if (attempt > 1) {
            sb.append("Note: Previous generation produced a duplicate. Please ask a fresh, distinct question.");
        }
        return sb.toString();
    }

    private boolean isDuplicateQuestion(String candidateText, List<String> previousQuestions) {
        if (candidateText == null || candidateText.isBlank()) return true;
        String candidateNorm = normalizeQuestionText(candidateText);

        for (String prev : previousQuestions) {
            String prevNorm = normalizeQuestionText(prev);
            if (candidateNorm.equals(prevNorm)) {
                return true;
            }
            if (candidateNorm.length() > 20 && prevNorm.length() > 20) {
                if (candidateNorm.contains(prevNorm) || prevNorm.contains(candidateNorm)) {
                    return true;
                }
            }
        }
        return false;
    }

    private String normalizeQuestionText(String text) {
        if (text == null) return "";
        return text.trim().toLowerCase().replaceAll("[^a-z0-9]", "");
    }

    private String validateGroundedSkillAgainstSnapshot(String suggestedSkill, String skillsJson) {
        if (skillsJson == null || skillsJson.isBlank() || skillsJson.equals("[]")) {
            return "General Tech";
        }
        try {
            List<String> skills = objectMapper.readValue(skillsJson, new TypeReference<List<String>>() {});
            if (skills.isEmpty()) return "General Tech";

            if (suggestedSkill != null && !suggestedSkill.isBlank() && !suggestedSkill.equalsIgnoreCase("N/A")) {
                for (String s : skills) {
                    if (s.equalsIgnoreCase(suggestedSkill.trim())) {
                        return s;
                    }
                }
            }
            return skills.get(0);
        } catch (Exception e) {
            return "General Tech";
        }
    }

    private String validateGroundedResponsibilityAgainstSnapshot(String suggestedResp, String respJson) {
        if (respJson == null || respJson.isBlank() || respJson.equals("[]")) {
            return "Core Position Duties";
        }
        try {
            List<String> resps = objectMapper.readValue(respJson, new TypeReference<List<String>>() {});
            if (resps.isEmpty()) return "Core Position Duties";

            if (suggestedResp != null && !suggestedResp.isBlank() && !suggestedResp.equalsIgnoreCase("N/A")) {
                for (String r : resps) {
                    if (r.equalsIgnoreCase(suggestedResp.trim())) {
                        return r;
                    }
                }
            }
            return resps.get(0);
        } catch (Exception e) {
            return "Core Position Duties";
        }
    }

    private String extractJsonBlock(String text) {
        if (text == null) return "{}";
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start != -1 && end != -1 && end > start) {
            return text.substring(start, end + 1);
        }
        return text.trim();
    }

    private static class GeneratedQuestionDetails {
        final String text;
        final QuestionType type;
        final Difficulty difficulty;
        final String guidance;
        final String groundedSkill;
        final String groundedResponsibility;
        final String groundingCategory;

        GeneratedQuestionDetails(String text, QuestionType type, Difficulty difficulty, String guidance,
                                 String groundedSkill, String groundedResponsibility, String groundingCategory) {
            this.text = text;
            this.type = type != null ? type : QuestionType.TECHNICAL;
            this.difficulty = difficulty;
            this.guidance = guidance;
            this.groundedSkill = groundedSkill;
            this.groundedResponsibility = groundedResponsibility;
            this.groundingCategory = groundingCategory;
        }
    }
}
