package com.prepgpt.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.prepgpt.dto.*;
import com.prepgpt.entity.*;
import com.prepgpt.exception.ResourceNotFoundException;
import com.prepgpt.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ReportAnalyticsService {

    private final UserRepository userRepository;
    private final InterviewSessionRepository sessionRepository;
    private final InterviewQuestionRepository interviewQuestionRepository;
    private final InterviewResponseRepository interviewResponseRepository;
    private final InterviewEvaluationRepository interviewEvaluationRepository;
    private final ObjectMapper objectMapper;

    public ReportAnalyticsService(UserRepository userRepository,
                                 InterviewSessionRepository sessionRepository,
                                 InterviewQuestionRepository interviewQuestionRepository,
                                 InterviewResponseRepository interviewResponseRepository,
                                 InterviewEvaluationRepository interviewEvaluationRepository,
                                 ObjectMapper objectMapper) {
        this.userRepository = userRepository;
        this.sessionRepository = sessionRepository;
        this.interviewQuestionRepository = interviewQuestionRepository;
        this.interviewResponseRepository = interviewResponseRepository;
        this.interviewEvaluationRepository = interviewEvaluationRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public InterviewReportResponse getInterviewReport(String userEmail, Long sessionId) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", userEmail));

        InterviewSession session = sessionRepository.findByIdAndUserId(sessionId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("InterviewSession not found with id " + sessionId + " for current user"));

        List<InterviewQuestion> assignedQuestions = interviewQuestionRepository
                .findByInterviewSessionIdOrderByQuestionOrderAsc(sessionId);

        List<QuestionEvaluationReportDto> questionReports = new ArrayList<>();
        List<String> keyStrengths = new ArrayList<>();
        List<String> keyWeaknesses = new ArrayList<>();
        List<Difficulty> difficultyTimeline = new ArrayList<>();
        Map<AdaptiveDecision, Integer> decisionCounts = new LinkedHashMap<>();
        List<String> focusedConcepts = new ArrayList<>();

        double totalScoreSum = 0;
        double correctnessSum = 0;
        double relevanceSum = 0;
        double claritySum = 0;
        double techAccuracySum = 0;
        int evaluatedCount = 0;
        int answeredCount = 0;

        for (InterviewQuestion iq : assignedQuestions) {
            difficultyTimeline.add(iq.getQuestion().getDifficulty() != null ? iq.getQuestion().getDifficulty() : session.getDifficulty());

            Optional<InterviewResponse> respOpt = interviewResponseRepository.findByInterviewQuestionId(iq.getId());
            if (respOpt.isPresent()) {
                answeredCount++;
                InterviewResponse resp = respOpt.get();
                Optional<InterviewEvaluation> evalOpt = interviewEvaluationRepository.findByInterviewResponseId(resp.getId());

                if (evalOpt.isPresent()) {
                    evaluatedCount++;
                    InterviewEvaluation eval = evalOpt.get();

                    totalScoreSum += eval.getScore() != null ? eval.getScore() : 0;
                    correctnessSum += eval.getCorrectness() != null ? eval.getCorrectness() : 0;
                    relevanceSum += eval.getRelevance() != null ? eval.getRelevance() : 0;
                    claritySum += eval.getClarity() != null ? eval.getClarity() : 0;
                    techAccuracySum += eval.getTechnicalAccuracy() != null ? eval.getTechnicalAccuracy() : 0;

                    List<String> strengths = fromJsonString(eval.getStrengths());
                    List<String> weaknesses = fromJsonString(eval.getWeaknesses());

                    keyStrengths.addAll(strengths);
                    keyWeaknesses.addAll(weaknesses);

                    if (eval.getAdaptiveDecision() != null) {
                        decisionCounts.put(eval.getAdaptiveDecision(), decisionCounts.getOrDefault(eval.getAdaptiveDecision(), 0) + 1);
                    }
                    if (eval.getTargetConcept() != null && !eval.getTargetConcept().isBlank()) {
                        focusedConcepts.add(eval.getTargetConcept());
                    }

                    questionReports.add(new QuestionEvaluationReportDto(
                            iq.getQuestionOrder(),
                            iq.getQuestion().getQuestionText(),
                            iq.getQuestion().getQuestionType(),
                            iq.getQuestion().getDifficulty(),
                            resp.getAnswerText(),
                            resp.getInputMethod(),
                            eval.getScore(),
                            eval.getCorrectness(),
                            eval.getRelevance(),
                            eval.getClarity(),
                            eval.getTechnicalAccuracy(),
                            strengths,
                            weaknesses,
                            eval.getFeedback(),
                            eval.getImprovementSuggestion(),
                            eval.getAdaptiveDecision(),
                            eval.getUserFacingMessage(),
                            eval.getTargetConcept(),
                            eval.getNextDifficulty()
                    ));
                } else {
                    questionReports.add(new QuestionEvaluationReportDto(
                            iq.getQuestionOrder(),
                            iq.getQuestion().getQuestionText(),
                            iq.getQuestion().getQuestionType(),
                            iq.getQuestion().getDifficulty(),
                            resp.getAnswerText(),
                            resp.getInputMethod(),
                            null, null, null, null, null,
                            Collections.emptyList(), Collections.emptyList(),
                            "Evaluation pending", null, null, null, null, null
                    ));
                }
            } else {
                questionReports.add(new QuestionEvaluationReportDto(
                        iq.getQuestionOrder(),
                        iq.getQuestion().getQuestionText(),
                        iq.getQuestion().getQuestionType(),
                        iq.getQuestion().getDifficulty(),
                        null, null, null, null, null, null, null,
                        Collections.emptyList(), Collections.emptyList(),
                        "Unanswered", null, null, null, null, null
                ));
            }
        }

        int totalQuestions = session.getQuestionCount() != null ? session.getQuestionCount() : assignedQuestions.size();
        double completionRate = totalQuestions > 0 ? roundDouble(((double) answeredCount / totalQuestions) * 100) : 0.0;

        double avgOverall = evaluatedCount > 0 ? roundDouble(totalScoreSum / evaluatedCount) : 0.0;
        double avgCorrectness = evaluatedCount > 0 ? roundDouble(correctnessSum / evaluatedCount) : 0.0;
        double avgRelevance = evaluatedCount > 0 ? roundDouble(relevanceSum / evaluatedCount) : 0.0;
        double avgClarity = evaluatedCount > 0 ? roundDouble(claritySum / evaluatedCount) : 0.0;
        double avgTechAccuracy = evaluatedCount > 0 ? roundDouble(techAccuracySum / evaluatedCount) : 0.0;

        String performanceLevel = classifyPerformanceLevel(avgOverall);
        List<String> distinctStrengths = keyStrengths.stream().distinct().collect(Collectors.toList());
        List<String> distinctWeaknesses = keyWeaknesses.stream().distinct().collect(Collectors.toList());
        List<String> distinctConcepts = focusedConcepts.stream().distinct().collect(Collectors.toList());

        AdaptiveSummaryDto adaptiveSummary = new AdaptiveSummaryDto(difficultyTimeline, decisionCounts, distinctConcepts);

        InterviewReportResponse reportResponse = new InterviewReportResponse(
                session.getId(),
                session.getCategory().getId(),
                session.getCategory().getName(),
                session.getDifficulty(),
                session.getMode(),
                session.getInteractionMode(),
                session.getStatus(),
                session.getStartedAt(),
                session.getCompletedAt(),
                session.getDurationMinutes(),
                totalQuestions,
                answeredCount,
                completionRate,
                avgOverall,
                avgCorrectness,
                avgRelevance,
                avgClarity,
                avgTechAccuracy,
                performanceLevel,
                distinctStrengths,
                distinctWeaknesses,
                adaptiveSummary,
                questionReports
        );

        reportResponse.setJdTargetRole(session.getJdTargetRole());
        reportResponse.setJdCompanyName(session.getJdCompanyName());

        if (session.getProjectTitleSnapshot() != null && !session.getProjectTitleSnapshot().isBlank()) {
            reportResponse.setProjectTitle(session.getProjectTitleSnapshot());
        } else if (session.getProject() != null) {
            reportResponse.setProjectTitle(session.getProject().getTitle());
        }
        if (session.getProject() != null) {
            reportResponse.setProjectId(session.getProject().getId());
        }

        return reportResponse;
    }

    @Transactional(readOnly = true)
    public AnalyticsOverviewResponse getAnalyticsOverview(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", userEmail));

        List<InterviewSession> allSessions = sessionRepository.findByUserIdOrderByCreatedAtDesc(user.getId());

        long totalInterviews = allSessions.size();
        long completedCount = allSessions.stream().filter(s -> s.getStatus() == SessionStatus.COMPLETED).count();
        long inProgressCount = allSessions.stream().filter(s -> s.getStatus() == SessionStatus.IN_PROGRESS).count();
        double completionRate = totalInterviews > 0 ? roundDouble(((double) completedCount / totalInterviews) * 100) : 0.0;

        List<ScoreTrendDto> scoreTrend = new ArrayList<>();
        Map<String, List<Double>> categoryScoresMap = new HashMap<>();

        double overallSum = 0;
        double maxScore = 0.0;
        Double latestScore = null;
        long totalQuestionsAnswered = 0;

        for (InterviewSession session : allSessions) {
            List<InterviewQuestion> iqs = interviewQuestionRepository.findByInterviewSessionIdOrderByQuestionOrderAsc(session.getId());

            double sessionScoreSum = 0;
            int sessionEvalCount = 0;

            for (InterviewQuestion iq : iqs) {
                Optional<InterviewResponse> respOpt = interviewResponseRepository.findByInterviewQuestionId(iq.getId());
                if (respOpt.isPresent()) {
                    totalQuestionsAnswered++;
                    Optional<InterviewEvaluation> evalOpt = interviewEvaluationRepository.findByInterviewResponseId(respOpt.get().getId());
                    if (evalOpt.isPresent() && evalOpt.get().getScore() != null) {
                        sessionScoreSum += evalOpt.get().getScore();
                        sessionEvalCount++;
                    }
                }
            }

            if (sessionEvalCount > 0) {
                double sessionAvgScore = roundDouble(sessionScoreSum / sessionEvalCount);
                overallSum += sessionAvgScore;

                if (sessionAvgScore > maxScore) {
                    maxScore = sessionAvgScore;
                }
                if (latestScore == null) {
                    latestScore = sessionAvgScore;
                }

                scoreTrend.add(new ScoreTrendDto(
                        session.getId(),
                        session.getCreatedAt(),
                        session.getCategory().getName(),
                        session.getDifficulty(),
                        session.getMode(),
                        sessionAvgScore
                ));

                categoryScoresMap.computeIfAbsent(session.getCategory().getName(), k -> new ArrayList<>()).add(sessionAvgScore);
            }
        }

        double avgOverall = scoreTrend.size() > 0 ? roundDouble(overallSum / scoreTrend.size()) : 0.0;
        double bestOverall = maxScore;
        double currentLatest = latestScore != null ? latestScore : 0.0;

        List<Map.Entry<String, Double>> categoryAvgList = categoryScoresMap.entrySet().stream()
                .map(e -> Map.entry(e.getKey(), roundDouble(e.getValue().stream().mapToDouble(Double::doubleValue).average().orElse(0.0))))
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .collect(Collectors.toList());

        List<String> strongestCategories = categoryAvgList.stream().limit(3).map(Map.Entry::getKey).collect(Collectors.toList());
        List<String> weakestCategories = categoryAvgList.stream()
                .sorted(Map.Entry.comparingByValue())
                .limit(3)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        Collections.reverse(scoreTrend); // Ascending chronological order for chart display

        return new AnalyticsOverviewResponse(
                totalInterviews,
                completedCount,
                inProgressCount,
                completionRate,
                avgOverall,
                bestOverall,
                currentLatest,
                totalQuestionsAnswered,
                scoreTrend,
                strongestCategories,
                weakestCategories
        );
    }

    @Transactional(readOnly = true)
    public List<CategoryAnalyticsResponse> getCategoryAnalytics(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", userEmail));

        List<InterviewSession> allSessions = sessionRepository.findByUserIdOrderByCreatedAtDesc(user.getId());

        Map<InterviewCategory, List<InterviewSession>> categorySessionMap = allSessions.stream()
                .collect(Collectors.groupingBy(InterviewSession::getCategory));

        List<CategoryAnalyticsResponse> result = new ArrayList<>();

        for (Map.Entry<InterviewCategory, List<InterviewSession>> entry : categorySessionMap.entrySet()) {
            InterviewCategory category = entry.getKey();
            List<InterviewSession> catSessions = entry.getValue();

            long totalInterviews = catSessions.size();
            long completedInterviews = catSessions.stream().filter(s -> s.getStatus() == SessionStatus.COMPLETED).count();

            double totalScoreSum = 0;
            double correctnessSum = 0;
            double relevanceSum = 0;
            double claritySum = 0;
            double techAccuracySum = 0;
            double maxScore = 0.0;
            int totalEvals = 0;

            for (InterviewSession s : catSessions) {
                List<InterviewQuestion> iqs = interviewQuestionRepository.findByInterviewSessionIdOrderByQuestionOrderAsc(s.getId());
                for (InterviewQuestion iq : iqs) {
                    Optional<InterviewResponse> respOpt = interviewResponseRepository.findByInterviewQuestionId(iq.getId());
                    if (respOpt.isPresent()) {
                        Optional<InterviewEvaluation> evalOpt = interviewEvaluationRepository.findByInterviewResponseId(respOpt.get().getId());
                        if (evalOpt.isPresent() && evalOpt.get().getScore() != null) {
                            InterviewEvaluation eval = evalOpt.get();
                            totalEvals++;
                            double score = eval.getScore();
                            totalScoreSum += score;
                            correctnessSum += eval.getCorrectness() != null ? eval.getCorrectness() : 0;
                            relevanceSum += eval.getRelevance() != null ? eval.getRelevance() : 0;
                            claritySum += eval.getClarity() != null ? eval.getClarity() : 0;
                            techAccuracySum += eval.getTechnicalAccuracy() != null ? eval.getTechnicalAccuracy() : 0;

                            if (score > maxScore) {
                                maxScore = score;
                            }
                        }
                    }
                }
            }

            double avgScore = totalEvals > 0 ? roundDouble(totalScoreSum / totalEvals) : 0.0;
            double avgCorrectness = totalEvals > 0 ? roundDouble(correctnessSum / totalEvals) : 0.0;
            double avgRelevance = totalEvals > 0 ? roundDouble(relevanceSum / totalEvals) : 0.0;
            double avgClarity = totalEvals > 0 ? roundDouble(claritySum / totalEvals) : 0.0;
            double avgTechAccuracy = totalEvals > 0 ? roundDouble(techAccuracySum / totalEvals) : 0.0;

            result.add(new CategoryAnalyticsResponse(
                    category.getId(),
                    category.getName(),
                    totalInterviews,
                    completedInterviews,
                    avgScore,
                    roundDouble(maxScore),
                    avgCorrectness,
                    avgRelevance,
                    avgClarity,
                    avgTechAccuracy,
                    classifyPerformanceLevel(avgScore)
            ));
        }

        result.sort(Comparator.comparing(CategoryAnalyticsResponse::getAverageScore).reversed());
        return result;
    }

    private String classifyPerformanceLevel(double score) {
        if (score >= 8.5) return "EXCELLENT";
        if (score >= 7.0) return "STRONG";
        if (score >= 5.5) return "GOOD";
        return "NEEDS_IMPROVEMENT";
    }

    private double roundDouble(double val) {
        return BigDecimal.valueOf(val).setScale(1, RoundingMode.HALF_UP).doubleValue();
    }

    private List<String> fromJsonString(String json) {
        if (json == null || json.isBlank()) return Collections.emptyList();
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }
}
