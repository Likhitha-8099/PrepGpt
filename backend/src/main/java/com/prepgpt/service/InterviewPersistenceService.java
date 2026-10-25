package com.prepgpt.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.prepgpt.dto.*;
import com.prepgpt.entity.*;
import com.prepgpt.exception.BadRequestException;
import com.prepgpt.exception.ResourceNotFoundException;
import com.prepgpt.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Dedicated persistence service managing database transactions for AI mock interview sessions.
 * Enforces strict transaction boundaries and micro-transactions to ensure DB connections are
 * never held open during long-running outbound AI network calls.
 */
@Service
public class InterviewPersistenceService {

    private static final Logger logger = LoggerFactory.getLogger(InterviewPersistenceService.class);

    private final InterviewSessionRepository sessionRepository;
    private final QuestionRepository questionRepository;
    private final InterviewQuestionRepository interviewQuestionRepository;
    private final InterviewResponseRepository interviewResponseRepository;
    private final InterviewEvaluationRepository interviewEvaluationRepository;
    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;
    private final ObjectMapper objectMapper;

    @Autowired
    public InterviewPersistenceService(InterviewSessionRepository sessionRepository,
                                     QuestionRepository questionRepository,
                                     InterviewQuestionRepository interviewQuestionRepository,
                                     InterviewResponseRepository interviewResponseRepository,
                                     InterviewEvaluationRepository interviewEvaluationRepository,
                                     UserRepository userRepository,
                                     ProjectRepository projectRepository,
                                     ObjectMapper objectMapper) {
        this.sessionRepository = sessionRepository;
        this.questionRepository = questionRepository;
        this.interviewQuestionRepository = interviewQuestionRepository;
        this.interviewResponseRepository = interviewResponseRepository;
        this.interviewEvaluationRepository = interviewEvaluationRepository;
        this.userRepository = userRepository;
        this.projectRepository = projectRepository;
        this.objectMapper = objectMapper != null ? objectMapper : new ObjectMapper();
    }

    private final Map<Long, InterviewResponse> transientResponseCache = new java.util.concurrent.ConcurrentHashMap<>();

    public InterviewPersistenceService(InterviewSessionRepository sessionRepository,
                                     QuestionRepository questionRepository,
                                     InterviewQuestionRepository interviewQuestionRepository,
                                     InterviewResponseRepository interviewResponseRepository,
                                     InterviewEvaluationRepository interviewEvaluationRepository,
                                     UserRepository userRepository,
                                     ObjectMapper objectMapper) {
        this(sessionRepository, questionRepository, interviewQuestionRepository,
             interviewResponseRepository, interviewEvaluationRepository, userRepository,
             null, objectMapper);
    }

    // =========================================================================
    // STEP 1: CANDIDATE ANSWER PERSISTENCE & CONTEXT PREPARATION (MICRO-TX)
    // =========================================================================

    @Transactional
    public CandidateAnswerContext saveCandidateAnswerAndPrepareContext(
            String userEmail, Long sessionId, Long questionId, String answerTextRaw, InputMethod inputMethod) {

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", userEmail));

        InterviewSession session = sessionRepository.findByIdAndUserId(sessionId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("InterviewSession not found with id " + sessionId + " for current user"));

        if (session.getStatus() != SessionStatus.IN_PROGRESS) {
            throw new BadRequestException("Interview session is not currently accepting answers.");
        }

        InterviewQuestion interviewQuestion = interviewQuestionRepository.findById(questionId)
                .orElseThrow(() -> new ResourceNotFoundException("InterviewQuestion", "id", questionId));

        if (!interviewQuestion.getInterviewSession().getId().equals(sessionId)) {
            throw new BadRequestException("Question does not belong to this interview session.");
        }

        String answerText = answerTextRaw != null ? answerTextRaw.trim() : "";
        if (answerText.isEmpty()) {
            throw new BadRequestException("Answer text cannot be empty.");
        }

        // Idempotency & Concurrency: Reuse existing response or handle concurrent insert race safely
        InterviewResponse response;
        Optional<InterviewResponse> existingResponse = interviewResponseRepository.findByInterviewQuestionId(questionId);
        if (existingResponse.isPresent()) {
            response = existingResponse.get();
        } else {
            try {
                response = new InterviewResponse(interviewQuestion, answerText, inputMethod != null ? inputMethod : InputMethod.TEXT);
                response = interviewResponseRepository.save(response);
            } catch (org.springframework.dao.DataIntegrityViolationException dive) {
                logger.info("Concurrent answer submission race detected for questionId #{}. Recovering existing response.", questionId);
                response = interviewResponseRepository.findByInterviewQuestionId(questionId)
                        .orElseThrow(() -> dive);
            }
        }

        if (response != null && response.getId() != null) {
            transientResponseCache.put(response.getId(), response);
        }

        // Check if evaluation already exists (Evaluation Idempotency)
        Optional<InterviewEvaluation> existingEvalOpt = interviewEvaluationRepository.findByInterviewResponseId(response.getId());
        if (existingEvalOpt.isPresent()) {
            InterviewEvaluation existingEval = existingEvalOpt.get();
            // If already COMPLETED, return cached result immediately
            if (existingEval.getStatus() == EvaluationStatus.COMPLETED) {
                boolean completed = checkAndMarkCompletionIfNeeded(session, interviewQuestion.getQuestionOrder(), existingEval.getShouldAskFollowUp());
                EvaluationResponse evalDto = mapToEvaluationDto(existingEval);
                return new CandidateAnswerContext(
                        response.getId(),
                        interviewQuestion.getId(),
                        session.getId(),
                        session.getCategory().getName(),
                        interviewQuestion.getQuestion().getDifficulty() != null ? interviewQuestion.getQuestion().getDifficulty() : session.getDifficulty(),
                        interviewQuestion.getQuestion().getQuestionText(),
                        interviewQuestion.getQuestion().getExpectedAnswerGuidance(),
                        response.getAnswerText(),
                        interviewQuestion.getQuestionOrder(),
                        session.getQuestionCount(),
                        session.getMode(),
                        null,
                        evalDto,
                        completed
                );
            }
        }

        // Build evaluation context
        String evalContext = "Session total questions count: " + session.getQuestionCount();
        if (session.getMode() == InterviewMode.PROJECT) {
            String projCtx = buildProjectEvaluationContext(session);
            if (projCtx != null) {
                evalContext = projCtx + "\n\n" + evalContext;
            }
        }

        Difficulty currentQuestionDiff = interviewQuestion.getQuestion().getDifficulty() != null
                ? interviewQuestion.getQuestion().getDifficulty() : session.getDifficulty();

        return new CandidateAnswerContext(
                response.getId(),
                interviewQuestion.getId(),
                session.getId(),
                session.getCategory().getName(),
                currentQuestionDiff,
                interviewQuestion.getQuestion().getQuestionText(),
                interviewQuestion.getQuestion().getExpectedAnswerGuidance(),
                response.getAnswerText(),
                interviewQuestion.getQuestionOrder(),
                session.getQuestionCount(),
                session.getMode(),
                evalContext,
                null,
                false
        );
    }

    // =========================================================================
    // STEP 3: PERSIST EVALUATION & ADAPTIVE DECISION (MICRO-TX)
    // =========================================================================

    @Transactional
    public AnswerResponse persistEvaluationAndAdaptiveState(
            Long responseId,
            AIEvaluationResponse aiEval,
            AIFollowUpResponse aiFollowUp) {

        InterviewResponse response = interviewResponseRepository.findById(responseId)
                .orElseGet(() -> transientResponseCache.get(responseId));
        transientResponseCache.remove(responseId);

        if (response == null) {
            throw new ResourceNotFoundException("InterviewResponse", "id", responseId);
        }

        InterviewQuestion interviewQuestion = response.getInterviewQuestion();
        InterviewSession session = interviewQuestion.getInterviewSession();

        // Check if evaluation record exists (e.g. initial PENDING or previous retry state)
        Optional<InterviewEvaluation> existingEvalOpt = interviewEvaluationRepository.findByInterviewResponseId(response.getId());
        InterviewEvaluation evalEntity;
        if (existingEvalOpt.isPresent()) {
            evalEntity = existingEvalOpt.get();
            // If already COMPLETED, return cached result immediately
            if (evalEntity.getStatus() == EvaluationStatus.COMPLETED) {
                boolean completed = checkAndMarkCompletionIfNeeded(session, interviewQuestion.getQuestionOrder(), evalEntity.getShouldAskFollowUp());
                return new AnswerResponse(true, interviewQuestion.getId(), true, completed, mapToEvaluationDto(evalEntity));
            }
        } else {
            evalEntity = new InterviewEvaluation();
            evalEntity.setInterviewResponse(response);
        }

        Difficulty currentQuestionDiff = interviewQuestion.getQuestion().getDifficulty() != null
                ? interviewQuestion.getQuestion().getDifficulty() : session.getDifficulty();

        boolean isHardLimitReached = interviewQuestion.getQuestionOrder() >= session.getQuestionCount();

        AdaptiveDecision decision = isHardLimitReached ? AdaptiveDecision.COMPLETE : aiFollowUp.getAdaptiveDecision();
        if (decision == null) {
            decision = Boolean.TRUE.equals(aiFollowUp.getShouldAskFollowUp()) ? AdaptiveDecision.FOLLOW_UP : AdaptiveDecision.CHANGE_TOPIC;
        }

        boolean shouldAskFollowUp = !isHardLimitReached && (
                decision == AdaptiveDecision.FOLLOW_UP ||
                decision == AdaptiveDecision.DEEP_DIVE ||
                decision == AdaptiveDecision.SIMPLIFY ||
                decision == AdaptiveDecision.CHANGE_TOPIC
        );

        Difficulty nextDiff = sanitizeDifficultyTransition(currentQuestionDiff, aiFollowUp.getNextDifficulty());

        String nextQuestionText = aiFollowUp.getNextQuestion() != null && !aiFollowUp.getNextQuestion().isBlank()
                ? aiFollowUp.getNextQuestion()
                : aiFollowUp.getQuestion();

        String strengthsJson = toJsonString(aiEval.getStrengths());
        String weaknessesJson = toJsonString(aiEval.getWeaknesses());

        evalEntity.setStatus(EvaluationStatus.COMPLETED);
        evalEntity.setScore(aiEval.getScore());
        evalEntity.setCorrectness(aiEval.getCorrectness());
        evalEntity.setRelevance(aiEval.getRelevance());
        evalEntity.setClarity(aiEval.getClarity());
        evalEntity.setTechnicalAccuracy(aiEval.getTechnicalAccuracy());
        evalEntity.setStrengths(strengthsJson);
        evalEntity.setWeaknesses(weaknessesJson);
        evalEntity.setFeedback(aiEval.getFeedback());
        evalEntity.setImprovementSuggestion(aiEval.getImprovementSuggestion());
        evalEntity.setShouldAskFollowUp(shouldAskFollowUp);
        evalEntity.setFollowUpQuestion(nextQuestionText);
        evalEntity.setAdaptiveDecision(decision);
        evalEntity.setAdaptiveReason(aiFollowUp.getReason());
        evalEntity.setTargetConcept(aiFollowUp.getTargetConcept());
        evalEntity.setUserFacingMessage(aiFollowUp.getUserFacingMessage());
        evalEntity.setNextDifficulty(nextDiff);
        evalEntity.setLastError(null);
        evalEntity.setNextRetryAt(null);
        evalEntity.setUpdatedAt(LocalDateTime.now());
        evalEntity.setEvaluatedAt(LocalDateTime.now());

        InterviewEvaluation savedEval = interviewEvaluationRepository.save(evalEntity);

        boolean completed = checkAndMarkCompletionIfNeeded(session, interviewQuestion.getQuestionOrder(), shouldAskFollowUp);

        return new AnswerResponse(true, interviewQuestion.getId(), true, completed, mapToEvaluationDto(savedEval));
    }

    @Transactional
    public InterviewEvaluation saveEvaluationDirect(
            InterviewSession session,
            InterviewQuestion interviewQuestion,
            InterviewResponse response,
            AIEvaluationResponse aiEval,
            AIFollowUpResponse aiFollowUp) {

        Difficulty currentQuestionDiff = interviewQuestion.getQuestion().getDifficulty() != null
                ? interviewQuestion.getQuestion().getDifficulty() : session.getDifficulty();

        boolean isHardLimitReached = interviewQuestion.getQuestionOrder() >= session.getQuestionCount();

        AdaptiveDecision decision = isHardLimitReached ? AdaptiveDecision.COMPLETE : aiFollowUp.getAdaptiveDecision();
        if (decision == null) {
            decision = Boolean.TRUE.equals(aiFollowUp.getShouldAskFollowUp()) ? AdaptiveDecision.FOLLOW_UP : AdaptiveDecision.CHANGE_TOPIC;
        }

        boolean shouldAskFollowUp = !isHardLimitReached && (
                decision == AdaptiveDecision.FOLLOW_UP ||
                decision == AdaptiveDecision.DEEP_DIVE ||
                decision == AdaptiveDecision.SIMPLIFY ||
                decision == AdaptiveDecision.CHANGE_TOPIC
        );

        Difficulty nextDiff = sanitizeDifficultyTransition(currentQuestionDiff, aiFollowUp.getNextDifficulty());

        String nextQuestionText = aiFollowUp.getNextQuestion() != null && !aiFollowUp.getNextQuestion().isBlank()
                ? aiFollowUp.getNextQuestion()
                : aiFollowUp.getQuestion();

        String strengthsJson = toJsonString(aiEval.getStrengths());
        String weaknessesJson = toJsonString(aiEval.getWeaknesses());

        InterviewEvaluation evalEntity = new InterviewEvaluation(
                response,
                aiEval.getScore(),
                aiEval.getCorrectness(),
                aiEval.getRelevance(),
                aiEval.getClarity(),
                aiEval.getTechnicalAccuracy(),
                strengthsJson,
                weaknessesJson,
                aiEval.getFeedback(),
                aiEval.getImprovementSuggestion(),
                shouldAskFollowUp,
                nextQuestionText
        );

        evalEntity.setAdaptiveDecision(decision);
        evalEntity.setAdaptiveReason(aiFollowUp.getReason());
        evalEntity.setTargetConcept(aiFollowUp.getTargetConcept());
        evalEntity.setUserFacingMessage(aiFollowUp.getUserFacingMessage());
        evalEntity.setNextDifficulty(nextDiff);

        return interviewEvaluationRepository.save(evalEntity);
    }

    // =========================================================================
    // FAILURE RECORDING & EVALUATION RETRY REAPER (MICRO-TX BOUNDARIES)
    // =========================================================================

    @Transactional
    public void recordEvaluationFailure(Long responseId, String rawErrorMessage, int initialDelaySeconds, int maxAttempts) {
        if (responseId == null) return;
        Optional<InterviewEvaluation> evalOpt = interviewEvaluationRepository.findByInterviewResponseId(responseId);
        InterviewEvaluation eval;
        if (evalOpt.isPresent()) {
            eval = evalOpt.get();
        } else {
            Optional<InterviewResponse> respOpt = interviewResponseRepository.findById(responseId);
            if (respOpt.isEmpty()) return;
            eval = new InterviewEvaluation();
            eval.setInterviewResponse(respOpt.get());
        }

        // Sanitize error message to prevent leaking sensitive API keys or headers
        String sanitizedError = sanitizeError(rawErrorMessage);

        int currentRetries = eval.getRetryCount() != null ? eval.getRetryCount() : 0;
        if (currentRetries >= maxAttempts) {
            eval.setStatus(EvaluationStatus.DEAD_LETTER);
            eval.setNextRetryAt(null);
            logger.warn("[DEAD_LETTER] Evaluation for response #{} exhausted all {} retry attempts. Moved to DEAD_LETTER.", responseId, maxAttempts);
        } else {
            eval.setStatus(EvaluationStatus.FAILED_RETRYABLE);
            eval.setNextRetryAt(LocalDateTime.now().plusSeconds(initialDelaySeconds));
            logger.info("[FAILED_RETRYABLE] Recorded retryable failure for response #{}. Next retry scheduled at {}", responseId, eval.getNextRetryAt());
        }

        eval.setLastError(sanitizedError);
        eval.setUpdatedAt(LocalDateTime.now());
        interviewEvaluationRepository.save(eval);
    }

    @Transactional
    public List<Long> claimEligibleEvaluationIdsForRetry(int maxAttempts) {
        LocalDateTime now = LocalDateTime.now();
        List<InterviewEvaluation> eligible = interviewEvaluationRepository
                .findByStatusAndNextRetryAtLessThanEqualAndRetryCountLessThanOrderByNextRetryAtAsc(
                        EvaluationStatus.FAILED_RETRYABLE, now, maxAttempts
                );

        List<Long> claimedIds = new ArrayList<>();
        for (InterviewEvaluation eval : eligible) {
            // Optimistic lock check / state transition to PENDING to prevent double-claiming
            eval.setStatus(EvaluationStatus.PENDING);
            eval.setUpdatedAt(LocalDateTime.now());
            interviewEvaluationRepository.save(eval);
            claimedIds.add(eval.getId());
        }
        return claimedIds;
    }

    @Transactional(readOnly = true)
    public CandidateAnswerContext prepareContextForEvaluationId(Long evalId) {
        InterviewEvaluation eval = interviewEvaluationRepository.findById(evalId)
                .orElseThrow(() -> new ResourceNotFoundException("InterviewEvaluation", "id", evalId));

        InterviewResponse response = eval.getInterviewResponse();
        InterviewQuestion interviewQuestion = response.getInterviewQuestion();
        InterviewSession session = interviewQuestion.getInterviewSession();

        String evalContext = "Session total questions count: " + session.getQuestionCount();
        if (session.getMode() == InterviewMode.PROJECT) {
            String projCtx = buildProjectEvaluationContext(session);
            if (projCtx != null) {
                evalContext = projCtx + "\n\n" + evalContext;
            }
        }

        Difficulty currentQuestionDiff = interviewQuestion.getQuestion().getDifficulty() != null
                ? interviewQuestion.getQuestion().getDifficulty() : session.getDifficulty();

        return new CandidateAnswerContext(
                response.getId(),
                interviewQuestion.getId(),
                session.getId(),
                session.getCategory().getName(),
                currentQuestionDiff,
                interviewQuestion.getQuestion().getQuestionText(),
                interviewQuestion.getQuestion().getExpectedAnswerGuidance(),
                response.getAnswerText(),
                interviewQuestion.getQuestionOrder(),
                session.getQuestionCount(),
                session.getMode(),
                evalContext,
                null,
                false
        );
    }

    @Transactional
    public void recordReaperFailure(Long evalId, String rawErrorMessage, int initialDelaySeconds, double backoffMultiplier, int maxAttempts) {
        Optional<InterviewEvaluation> evalOpt = interviewEvaluationRepository.findById(evalId);
        if (evalOpt.isEmpty()) return;

        InterviewEvaluation eval = evalOpt.get();
        int newRetryCount = (eval.getRetryCount() != null ? eval.getRetryCount() : 0) + 1;
        eval.setRetryCount(newRetryCount);
        eval.setLastError(sanitizeError(rawErrorMessage));
        eval.setUpdatedAt(LocalDateTime.now());

        if (newRetryCount >= maxAttempts) {
            eval.setStatus(EvaluationStatus.DEAD_LETTER);
            eval.setNextRetryAt(null);
            logger.warn("[DEAD_LETTER] Reaper: Evaluation #{} for response #{} reached max attempts ({}/{}). Transitioned to DEAD_LETTER.",
                    eval.getId(), eval.getInterviewResponse().getId(), newRetryCount, maxAttempts);
        } else {
            eval.setStatus(EvaluationStatus.FAILED_RETRYABLE);
            // Exponential backoff: initialDelay * (multiplier ^ (attempt - 1))
            long delaySeconds = Math.round(initialDelaySeconds * Math.pow(backoffMultiplier, newRetryCount - 1));
            eval.setNextRetryAt(LocalDateTime.now().plusSeconds(delaySeconds));
            logger.info("[REAPER RETRY SCHEDULED] Evaluation #{} failed attempt {}/{}. Next attempt in {}s at {}",
                    eval.getId(), newRetryCount, maxAttempts, delaySeconds, eval.getNextRetryAt());
        }
        interviewEvaluationRepository.save(eval);
    }

    private String sanitizeError(String rawError) {
        if (rawError == null || rawError.isBlank()) {
            return "AI service temporarily unavailable.";
        }
        // Scrub potential sensitive patterns like Authorization, Bearer, api-key
        String scrubbed = rawError.replaceAll("Bearer\\s+[A-Za-z0-9_\\-\\.]+", "Bearer [REDACTED]")
                .replaceAll("(?i)api[_-]?key[:=]\\s*['\"]?[A-Za-z0-9_\\-\\.]+['\"]?", "api_key=[REDACTED]");
        if (scrubbed.length() > 500) {
            scrubbed = scrubbed.substring(0, 500) + "...";
        }
        return scrubbed;
    }

    // =========================================================================
    // START SESSION MICRO-TRANSACTION
    // =========================================================================

    @Transactional
    public StartSessionState startSessionIfNeeded(String userEmail, Long sessionId) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", userEmail));

        InterviewSession session = sessionRepository.findByIdAndUserId(sessionId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("InterviewSession not found with id " + sessionId + " for current user"));

        if (session.getMode() == InterviewMode.QUESTION_BANK) {
            throw new BadRequestException("Interview session is configured for Question Bank mode.");
        }

        if (session.getStatus() == SessionStatus.COMPLETED) {
            throw new BadRequestException("Completed interview sessions cannot be restarted.");
        }
        if (session.getStatus() == SessionStatus.ABANDONED) {
            throw new BadRequestException("Abandoned interview sessions cannot be restarted.");
        }

        if (session.getStatus() == SessionStatus.CONFIGURED) {
            session.setStatus(SessionStatus.IN_PROGRESS);
            session.setStartedAt(LocalDateTime.now());
            InterviewSession saved = sessionRepository.save(session);
            if (saved != null) {
                session = saved;
            }
        }

        List<InterviewQuestion> assignedQuestions = interviewQuestionRepository
                .findByInterviewSessionIdOrderByQuestionOrderAsc(sessionId);

        InterviewSessionResponse response = mapToSessionResponse(session);

        if (assignedQuestions.isEmpty()) {
            AdaptiveQuestionParams params = buildQuestionGenerationParams(session, 1, Collections.emptyList(), null, null, session.getDifficulty(), null);
            return StartSessionState.ofNeedsQuestion1(response, params);
        }

        return StartSessionState.ofReady(response);
    }

    // =========================================================================
    // GET CURRENT QUESTION MICRO-TRANSACTION
    // =========================================================================

    @Transactional
    public CurrentQuestionState getCurrentQuestionState(String userEmail, Long sessionId) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", userEmail));

        InterviewSession session = sessionRepository.findByIdAndUserId(sessionId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("InterviewSession not found with id " + sessionId + " for current user"));

        if (session.getMode() == InterviewMode.QUESTION_BANK) {
            throw new BadRequestException("Interview session is configured for Question Bank mode.");
        }

        List<InterviewQuestion> assignedQuestions = interviewQuestionRepository
                .findByInterviewSessionIdOrderByQuestionOrderAsc(sessionId);

        if (assignedQuestions.isEmpty()) {
            AdaptiveQuestionParams params = buildQuestionGenerationParams(session, 1, Collections.emptyList(), null, null, session.getDifficulty(), null);
            return CurrentQuestionState.ofNeedsQuestion1(params);
        }

        int totalQuestions = session.getQuestionCount();
        int answeredCount = 0;
        InterviewQuestion currentQuestion = null;
        InterviewQuestion lastAnsweredQuestion = null;

        for (InterviewQuestion iq : assignedQuestions) {
            boolean isAnswered = interviewResponseRepository.existsByInterviewQuestionId(iq.getId());
            if (isAnswered) {
                answeredCount++;
                lastAnsweredQuestion = iq;
            } else if (currentQuestion == null) {
                currentQuestion = iq;
            }
        }

        // If all currently assigned questions are answered
        if (currentQuestion == null) {
            int nextOrder = assignedQuestions.size() + 1;

            if (nextOrder <= totalQuestions) {
                AdaptiveDecision lastDecision = null;
                Difficulty nextDiff = session.getDifficulty();
                String targetConcept = null;
                String recommendedNextQ = null;

                if (lastAnsweredQuestion != null) {
                    Optional<InterviewEvaluation> lastEvalOpt = interviewEvaluationRepository
                            .findByInterviewResponseInterviewQuestionId(lastAnsweredQuestion.getId());
                    if (lastEvalOpt.isPresent()) {
                        InterviewEvaluation lastEval = lastEvalOpt.get();
                        lastDecision = lastEval.getAdaptiveDecision();
                        targetConcept = lastEval.getTargetConcept();
                        recommendedNextQ = lastEval.getFollowUpQuestion();
                        if (lastEval.getNextDifficulty() != null) {
                            nextDiff = sanitizeDifficultyTransition(session.getDifficulty(), lastEval.getNextDifficulty());
                        }
                    }
                }

                // If LLM/Adaptive decision was COMPLETE, finish session early
                if (lastDecision == AdaptiveDecision.COMPLETE) {
                    if (session.getStatus() == SessionStatus.IN_PROGRESS) {
                        session.setStatus(SessionStatus.COMPLETED);
                        session.setCompletedAt(LocalDateTime.now());
                        sessionRepository.save(session);
                    }

                    InterviewQuestion lastQuestion = assignedQuestions.get(assignedQuestions.size() - 1);
                    return CurrentQuestionState.ofExisting(new QuestionResponse(
                            lastQuestion.getId(),
                            lastQuestion.getQuestionOrder(),
                            totalQuestions,
                            lastQuestion.getQuestion().getQuestionText(),
                            lastQuestion.getQuestion().getQuestionType(),
                            lastQuestion.getQuestion().getDifficulty(),
                            true,
                            100
                    ));
                }

                List<String> previousQuestions = assignedQuestions.stream()
                        .map(iq -> iq.getQuestion().getQuestionText())
                        .collect(Collectors.toList());

                AdaptiveQuestionParams params = buildQuestionGenerationParams(
                        session, nextOrder, previousQuestions, recommendedNextQ, targetConcept, nextDiff, lastDecision
                );
                return CurrentQuestionState.ofNeedsAdaptive(params);
            } else {
                // Hard limit reached
                if (session.getStatus() == SessionStatus.IN_PROGRESS) {
                    session.setStatus(SessionStatus.COMPLETED);
                    session.setCompletedAt(LocalDateTime.now());
                    sessionRepository.save(session);
                }

                InterviewQuestion lastQuestion = assignedQuestions.get(assignedQuestions.size() - 1);
                return CurrentQuestionState.ofExisting(new QuestionResponse(
                        lastQuestion.getId(),
                        lastQuestion.getQuestionOrder(),
                        totalQuestions,
                        lastQuestion.getQuestion().getQuestionText(),
                        lastQuestion.getQuestion().getQuestionType(),
                        lastQuestion.getQuestion().getDifficulty(),
                        true,
                        100
                ));
            }
        }

        int progress = (int) Math.round(((double) answeredCount / totalQuestions) * 100);
        return CurrentQuestionState.ofExisting(new QuestionResponse(
                currentQuestion.getId(),
                currentQuestion.getQuestionOrder(),
                totalQuestions,
                currentQuestion.getQuestion().getQuestionText(),
                currentQuestion.getQuestion().getQuestionType(),
                currentQuestion.getQuestion().getDifficulty(),
                false,
                progress
        ));
    }

    // =========================================================================
    // PERSIST GENERATED QUESTION MICRO-TRANSACTION
    // =========================================================================

    @Transactional
    public QuestionResponse persistGeneratedQuestion(
            String userEmail,
            Long sessionId,
            int order,
            String text,
            QuestionType type,
            Difficulty difficulty,
            String guidance,
            String groundedSkill,
            String groundedResponsibility,
            String groundingCategory) {

        InterviewSession session = null;
        if (userEmail != null) {
            Optional<User> userOpt = userRepository.findByEmail(userEmail);
            if (userOpt.isPresent()) {
                session = sessionRepository.findByIdAndUserId(sessionId, userOpt.get().getId()).orElse(null);
            }
        }
        if (session == null) {
            session = sessionRepository.findById(sessionId).orElse(null);
        }
        if (session == null) {
            throw new ResourceNotFoundException("InterviewSession", "id", sessionId);
        }

        Question question = new Question(
                session.getCategory(),
                text,
                type != null ? type : QuestionType.TECHNICAL,
                difficulty != null ? difficulty : session.getDifficulty(),
                guidance != null ? guidance : "AI generated question."
        );

        Question savedQuestion = questionRepository.save(question);

        InterviewQuestion iq = new InterviewQuestion(session, savedQuestion, order);
        iq.setGroundedSkill(groundedSkill);
        iq.setGroundedResponsibility(groundedResponsibility);
        iq.setGroundingCategory(groundingCategory);
        InterviewQuestion savedIq = interviewQuestionRepository.save(iq);

        int totalQuestions = session.getQuestionCount();
        long answeredCount = interviewResponseRepository.countByInterviewQuestionInterviewSessionId(sessionId);
        int progress = (int) Math.round(((double) answeredCount / totalQuestions) * 100);

        return new QuestionResponse(
                savedIq.getId(),
                savedIq.getQuestionOrder(),
                totalQuestions,
                savedQuestion.getQuestionText(),
                savedQuestion.getQuestionType(),
                savedQuestion.getDifficulty(),
                false,
                progress
        );
    }

    @Transactional
    public QuestionResponse persistGeneratedQuestion(
            Long sessionId,
            int order,
            String text,
            QuestionType type,
            Difficulty difficulty,
            String guidance,
            String groundedSkill,
            String groundedResponsibility,
            String groundingCategory) {
        return persistGeneratedQuestion(null, sessionId, order, text, type, difficulty, guidance, groundedSkill, groundedResponsibility, groundingCategory);
    }

    // =========================================================================
    // GET SESSION DETAILS (READ-ONLY MICRO-TX)
    // =========================================================================

    @Transactional(readOnly = true)
    public InterviewSessionResponse getAISessionDetails(String userEmail, Long sessionId) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", userEmail));

        InterviewSession session = sessionRepository.findByIdAndUserId(sessionId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("InterviewSession not found with id " + sessionId + " for current user"));

        InterviewSessionResponse response = mapToSessionResponse(session);

        List<InterviewQuestion> assignedQuestions = interviewQuestionRepository
                .findByInterviewSessionIdOrderByQuestionOrderAsc(sessionId);

        if (!assignedQuestions.isEmpty()) {
            int totalQuestions = session.getQuestionCount();
            int answeredQuestions = 0;

            List<InterviewQuestionDetailResponse> questionDetails = new ArrayList<>();
            for (InterviewQuestion iq : assignedQuestions) {
                Optional<InterviewResponse> respOpt = interviewResponseRepository.findByInterviewQuestionId(iq.getId());
                String answerText = null;
                LocalDateTime answeredAt = null;
                EvaluationResponse evalDto = null;

                if (respOpt.isPresent()) {
                    answeredQuestions++;
                    answerText = respOpt.get().getAnswerText();
                    answeredAt = respOpt.get().getAnsweredAt();

                    Optional<InterviewEvaluation> evalOpt = interviewEvaluationRepository.findByInterviewResponseId(respOpt.get().getId());
                    if (evalOpt.isPresent()) {
                        evalDto = mapToEvaluationDto(evalOpt.get());
                    }
                }

                questionDetails.add(new InterviewQuestionDetailResponse(
                        iq.getId(),
                        iq.getQuestionOrder(),
                        iq.getQuestion().getQuestionText(),
                        iq.getQuestion().getQuestionType(),
                        iq.getQuestion().getDifficulty(),
                        answerText,
                        answeredAt,
                        evalDto
                ));
            }

            int progress = (int) Math.round(((double) answeredQuestions / totalQuestions) * 100);
            response.setProgress(progress);
            response.setQuestions(questionDetails);
        } else {
            response.setProgress(0);
            response.setQuestions(Collections.emptyList());
        }

        return response;
    }

    // =========================================================================
    // INTERNAL HELPERS
    // =========================================================================

    private boolean checkAndMarkCompletionIfNeeded(InterviewSession session, int currentOrder, Boolean shouldAskFollowUp) {
        if (currentOrder >= session.getQuestionCount() && !Boolean.TRUE.equals(shouldAskFollowUp)) {
            session.setStatus(SessionStatus.COMPLETED);
            session.setCompletedAt(LocalDateTime.now());
            sessionRepository.save(session);
            return true;
        }
        return false;
    }

    private AdaptiveQuestionParams buildQuestionGenerationParams(
            InterviewSession session,
            int order,
            List<String> previousQuestions,
            String recommendedNextQ,
            String targetConcept,
            Difficulty targetDifficulty,
            AdaptiveDecision decision) {

        return new AdaptiveQuestionParams(
                session.getId(),
                session.getCategory().getName(),
                session.getMode(),
                order,
                session.getQuestionCount(),
                targetDifficulty != null ? targetDifficulty : session.getDifficulty(),
                previousQuestions,
                recommendedNextQ,
                targetConcept,
                decision,
                session.getJdTargetRole(),
                session.getJdExtractedSkillsJson(),
                session.getJdKeyResponsibilitiesJson(),
                session.getResumeSkillsSnapshotJson(),
                session.getProjectTitleSnapshot(),
                session.getProjectTechSnapshot(),
                session.getResume(),
                session.getProject()
        );
    }

    public String buildProjectEvaluationContext(InterviewSession session) {
        Project project = null;
        if (session.getProject() != null) {
            try {
                Long projectId = session.getProject().getId();
                Long userId = session.getUser() != null ? session.getUser().getId() : null;
                if (projectRepository != null && projectId != null && userId != null) {
                    project = projectRepository.findByIdAndUserId(projectId, userId).orElse(null);
                } else if (projectRepository != null && projectId != null) {
                    project = projectRepository.findById(projectId).orElse(null);
                } else {
                    project = session.getProject();
                }
            } catch (Exception e) {
                logger.warn("Could not load Project entity for session #{}, falling back to snapshots: {}", session.getId(), e.getMessage());
            }
        }

        if (project != null) {
            try {
                StringBuilder sb = new StringBuilder();
                sb.append("PROJECT EVALUATION REFERENCE CONTEXT:\n");
                sb.append("- Project Title: ").append(project.getTitle()).append("\n");
                if (project.getTechnologies() != null && !project.getTechnologies().isBlank()) {
                    sb.append("- Tech Stack: ").append(project.getTechnologies()).append("\n");
                }
                if (project.getDescription() != null && !project.getDescription().isBlank()) {
                    sb.append("- Description: ").append(project.getDescription()).append("\n");
                }
                if (project.getRoleContribution() != null && !project.getRoleContribution().isBlank()) {
                    sb.append("- Candidate Role & Contribution: ").append(project.getRoleContribution()).append("\n");
                }
                if (project.getArchitectureDetails() != null && !project.getArchitectureDetails().isBlank()) {
                    sb.append("- Architecture Details: ").append(project.getArchitectureDetails()).append("\n");
                }
                if (project.getKeyFeatures() != null && !project.getKeyFeatures().isBlank()) {
                    sb.append("- Key Features: ").append(project.getKeyFeatures()).append("\n");
                }
                return sb.toString().trim();
            } catch (Exception e) {
                logger.warn("Error accessing project fields, falling back to snapshots: {}", e.getMessage());
            }
        }

        if (session.getProjectTitleSnapshot() != null && !session.getProjectTitleSnapshot().isBlank()) {
            StringBuilder sb = new StringBuilder();
            sb.append("PROJECT EVALUATION REFERENCE CONTEXT:\n");
            sb.append("- Project Title: ").append(session.getProjectTitleSnapshot()).append("\n");
            if (session.getProjectTechSnapshot() != null && !session.getProjectTechSnapshot().isBlank()) {
                sb.append("- Tech Stack: ").append(session.getProjectTechSnapshot()).append("\n");
            }
            return sb.toString().trim();
        }
        return null;
    }

    private Difficulty sanitizeDifficultyTransition(Difficulty current, Difficulty requested) {
        if (requested == null) return current != null ? current : Difficulty.MEDIUM;
        if (current == null) return requested;

        if (current == Difficulty.EASY && requested == Difficulty.HARD) {
            return Difficulty.MEDIUM;
        }
        if (current == Difficulty.HARD && requested == Difficulty.EASY) {
            return Difficulty.MEDIUM;
        }
        return requested;
    }

    public EvaluationResponse mapToEvaluationDto(InterviewEvaluation eval) {
        List<String> strengths = fromJsonString(eval.getStrengths());
        List<String> weaknesses = fromJsonString(eval.getWeaknesses());

        EvaluationResponse dto = new EvaluationResponse(
                eval.getScore(),
                eval.getCorrectness(),
                eval.getRelevance(),
                eval.getClarity(),
                eval.getTechnicalAccuracy(),
                strengths,
                weaknesses,
                eval.getFeedback(),
                eval.getImprovementSuggestion(),
                eval.getShouldAskFollowUp(),
                eval.getFollowUpQuestion(),
                eval.getEvaluatedAt()
        );

        dto.setAdaptiveDecision(eval.getAdaptiveDecision());
        dto.setUserFacingMessage(eval.getUserFacingMessage());
        dto.setNextDifficulty(eval.getNextDifficulty());
        dto.setTargetConcept(eval.getTargetConcept());

        return dto;
    }

    public InterviewSessionResponse mapToSessionResponse(InterviewSession session) {
        InterviewSessionResponse resp = new InterviewSessionResponse(
                session.getId(),
                session.getCategory().getId(),
                session.getCategory().getName(),
                session.getDifficulty(),
                session.getStatus(),
                session.getMode(),
                session.getInteractionMode(),
                session.getQuestionCount(),
                session.getDurationMinutes(),
                session.getCreatedAt(),
                session.getStartedAt(),
                session.getCompletedAt()
        );

        if (session.getResume() != null) {
            resp.setResumeId(session.getResume().getId());
            resp.setResumeFileName(session.getResume().getFileName());
        }

        if (session.getProject() != null) {
            resp.setProjectId(session.getProject().getId());
            resp.setProjectTitle(session.getProject().getTitle());
        }

        return resp;
    }

    private String toJsonString(List<String> list) {
        if (list == null || list.isEmpty()) return "[]";
        try {
            return objectMapper.writeValueAsString(list);
        } catch (Exception e) {
            return "[]";
        }
    }

    private List<String> fromJsonString(String json) {
        if (json == null || json.isBlank()) return Collections.emptyList();
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    // =========================================================================
    // DTO / CONTEXT RECORDS
    // =========================================================================

    public static class CandidateAnswerContext {
        private final Long responseId;
        private final Long interviewQuestionId;
        private final Long sessionId;
        private final String categoryName;
        private final Difficulty currentDifficulty;
        private final String questionText;
        private final String expectedAnswerGuidance;
        private final String answerText;
        private final int questionOrder;
        private final int totalQuestions;
        private final InterviewMode mode;
        private final String evalContext;
        private final EvaluationResponse existingEvaluation;
        private final boolean alreadyCompleted;

        public CandidateAnswerContext(Long responseId, Long interviewQuestionId, Long sessionId,
                                      String categoryName, Difficulty currentDifficulty,
                                      String questionText, String expectedAnswerGuidance,
                                      String answerText, int questionOrder, int totalQuestions,
                                      InterviewMode mode, String evalContext,
                                      EvaluationResponse existingEvaluation, boolean alreadyCompleted) {
            this.responseId = responseId;
            this.interviewQuestionId = interviewQuestionId;
            this.sessionId = sessionId;
            this.categoryName = categoryName;
            this.currentDifficulty = currentDifficulty;
            this.questionText = questionText;
            this.expectedAnswerGuidance = expectedAnswerGuidance;
            this.answerText = answerText;
            this.questionOrder = questionOrder;
            this.totalQuestions = totalQuestions;
            this.mode = mode;
            this.evalContext = evalContext;
            this.existingEvaluation = existingEvaluation;
            this.alreadyCompleted = alreadyCompleted;
        }

        public Long getResponseId() { return responseId; }
        public Long getInterviewQuestionId() { return interviewQuestionId; }
        public Long getSessionId() { return sessionId; }
        public String getCategoryName() { return categoryName; }
        public Difficulty getCurrentDifficulty() { return currentDifficulty; }
        public String getQuestionText() { return questionText; }
        public String getExpectedAnswerGuidance() { return expectedAnswerGuidance; }
        public String getAnswerText() { return answerText; }
        public int getQuestionOrder() { return questionOrder; }
        public int getTotalQuestions() { return totalQuestions; }
        public InterviewMode getMode() { return mode; }
        public String getEvalContext() { return evalContext; }
        public EvaluationResponse getExistingEvaluation() { return existingEvaluation; }
        public boolean hasExistingEvaluation() { return existingEvaluation != null; }
        public boolean isAlreadyCompleted() { return alreadyCompleted; }
    }

    public static class StartSessionState {
        private final InterviewSessionResponse sessionResponse;
        private final boolean needsQuestion1;
        private final AdaptiveQuestionParams question1Params;

        private StartSessionState(InterviewSessionResponse sessionResponse, boolean needsQuestion1, AdaptiveQuestionParams question1Params) {
            this.sessionResponse = sessionResponse;
            this.needsQuestion1 = needsQuestion1;
            this.question1Params = question1Params;
        }

        public static StartSessionState ofReady(InterviewSessionResponse resp) {
            return new StartSessionState(resp, false, null);
        }

        public static StartSessionState ofNeedsQuestion1(InterviewSessionResponse resp, AdaptiveQuestionParams params) {
            return new StartSessionState(resp, true, params);
        }

        public InterviewSessionResponse getSessionResponse() { return sessionResponse; }
        public boolean isNeedsQuestion1() { return needsQuestion1; }
        public AdaptiveQuestionParams getQuestion1Params() { return question1Params; }
    }

    public static class CurrentQuestionState {
        private final QuestionResponse existingQuestionResponse;
        private final boolean needsGeneration;
        private final AdaptiveQuestionParams generationParams;

        private CurrentQuestionState(QuestionResponse existingQuestionResponse, boolean needsGeneration, AdaptiveQuestionParams generationParams) {
            this.existingQuestionResponse = existingQuestionResponse;
            this.needsGeneration = needsGeneration;
            this.generationParams = generationParams;
        }

        public static CurrentQuestionState ofExisting(QuestionResponse qr) {
            return new CurrentQuestionState(qr, false, null);
        }

        public static CurrentQuestionState ofNeedsAdaptive(AdaptiveQuestionParams params) {
            return new CurrentQuestionState(null, true, params);
        }

        public static CurrentQuestionState ofNeedsQuestion1(AdaptiveQuestionParams params) {
            return new CurrentQuestionState(null, true, params);
        }

        public QuestionResponse getExistingQuestionResponse() { return existingQuestionResponse; }
        public boolean isNeedsGeneration() { return needsGeneration; }
        public AdaptiveQuestionParams getGenerationParams() { return generationParams; }
    }

    public static class AdaptiveQuestionParams {
        private final Long sessionId;
        private final String categoryName;
        private final InterviewMode mode;
        private final int nextOrder;
        private final int totalQuestions;
        private final Difficulty targetDifficulty;
        private final List<String> previousQuestions;
        private final String recommendedNextQ;
        private final String targetConcept;
        private final AdaptiveDecision lastDecision;
        private final String jdTargetRole;
        private final String jdExtractedSkillsJson;
        private final String jdKeyResponsibilitiesJson;
        private final String resumeSkillsSnapshotJson;
        private final String projectTitleSnapshot;
        private final String projectTechSnapshot;
        private final Resume resume;
        private final Project project;

        public AdaptiveQuestionParams(Long sessionId, String categoryName, InterviewMode mode, int nextOrder, int totalQuestions,
                                      Difficulty targetDifficulty, List<String> previousQuestions, String recommendedNextQ,
                                      String targetConcept, AdaptiveDecision lastDecision, String jdTargetRole,
                                      String jdExtractedSkillsJson, String jdKeyResponsibilitiesJson,
                                      String resumeSkillsSnapshotJson, String projectTitleSnapshot,
                                      String projectTechSnapshot, Resume resume, Project project) {
            this.sessionId = sessionId;
            this.categoryName = categoryName;
            this.mode = mode;
            this.nextOrder = nextOrder;
            this.totalQuestions = totalQuestions;
            this.targetDifficulty = targetDifficulty;
            this.previousQuestions = previousQuestions != null ? previousQuestions : Collections.emptyList();
            this.recommendedNextQ = recommendedNextQ;
            this.targetConcept = targetConcept;
            this.lastDecision = lastDecision;
            this.jdTargetRole = jdTargetRole;
            this.jdExtractedSkillsJson = jdExtractedSkillsJson;
            this.jdKeyResponsibilitiesJson = jdKeyResponsibilitiesJson;
            this.resumeSkillsSnapshotJson = resumeSkillsSnapshotJson;
            this.projectTitleSnapshot = projectTitleSnapshot;
            this.projectTechSnapshot = projectTechSnapshot;
            this.resume = resume;
            this.project = project;
        }

        public Long getSessionId() { return sessionId; }
        public String getCategoryName() { return categoryName; }
        public InterviewMode getMode() { return mode; }
        public int getNextOrder() { return nextOrder; }
        public int getTotalQuestions() { return totalQuestions; }
        public Difficulty getTargetDifficulty() { return targetDifficulty; }
        public List<String> getPreviousQuestions() { return previousQuestions; }
        public String getRecommendedNextQ() { return recommendedNextQ; }
        public String getTargetConcept() { return targetConcept; }
        public AdaptiveDecision getLastDecision() { return lastDecision; }
        public String getJdTargetRole() { return jdTargetRole; }
        public String getJdExtractedSkillsJson() { return jdExtractedSkillsJson; }
        public String getJdKeyResponsibilitiesJson() { return jdKeyResponsibilitiesJson; }
        public String getResumeSkillsSnapshotJson() { return resumeSkillsSnapshotJson; }
        public String getProjectTitleSnapshot() { return projectTitleSnapshot; }
        public String getProjectTechSnapshot() { return projectTechSnapshot; }
        public Resume getResume() { return resume; }
        public Project getProject() { return project; }
    }
}
