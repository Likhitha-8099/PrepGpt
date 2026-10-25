package com.prepgpt.service;

import com.prepgpt.ai.GroqAIService;
import com.prepgpt.ai.prompt.CodingEvaluationPromptBuilder;
import com.prepgpt.dto.*;
import com.prepgpt.entity.*;
import com.prepgpt.exception.ResourceNotFoundException;
import com.prepgpt.exception.UnauthorizedException;
import com.prepgpt.repository.*;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CodingProblemService {

    private static final Logger logger = LoggerFactory.getLogger(CodingProblemService.class);

    private final CodingProblemRepository problemRepository;
    private final CodingTestCaseRepository testCaseRepository;
    private final CodeSubmissionRepository submissionRepository;
    private final InterviewSessionRepository sessionRepository;
    private final InterviewQuestionRepository questionRepository;
    private final InterviewResponseRepository responseRepository;
    private final InterviewEvaluationRepository evaluationRepository;
    private final CodeExecutionService codeExecutionService;
    private final GroqAIService groqAIService;
    private final CodingEvaluationPromptBuilder evaluationPromptBuilder;

    public CodingProblemService(CodingProblemRepository problemRepository,
                                CodingTestCaseRepository testCaseRepository,
                                CodeSubmissionRepository submissionRepository,
                                InterviewSessionRepository sessionRepository,
                                InterviewQuestionRepository questionRepository,
                                InterviewResponseRepository responseRepository,
                                InterviewEvaluationRepository evaluationRepository,
                                CodeExecutionService codeExecutionService,
                                GroqAIService groqAIService,
                                CodingEvaluationPromptBuilder evaluationPromptBuilder) {
        this.problemRepository = problemRepository;
        this.testCaseRepository = testCaseRepository;
        this.submissionRepository = submissionRepository;
        this.sessionRepository = sessionRepository;
        this.questionRepository = questionRepository;
        this.responseRepository = responseRepository;
        this.evaluationRepository = evaluationRepository;
        this.codeExecutionService = codeExecutionService;
        this.groqAIService = groqAIService;
        this.evaluationPromptBuilder = evaluationPromptBuilder;
    }

    @PostConstruct
    public void seedInitialProblems() {
        if (problemRepository.count() > 0) {
            return;
        }

        logger.info("Seeding initial Coding & DSA problem bank...");

        // Problem 1: Two Sum
        CodingProblem p1 = new CodingProblem(
                "Two Sum",
                "Given an array of integers `nums` and an integer `target`, return indices of the two numbers such that they add up to `target`.\nOutput format: space-separated indices, e.g. `0 1`.",
                Difficulty.EASY,
                "Arrays",
                "2 <= nums.length <= 10^4\n-10^9 <= nums[i] <= 10^9",
                "Line 1: space-separated integers for nums\nLine 2: target integer",
                "Space-separated indices",
                """
                import java.util.Scanner;
                import java.util.HashMap;

                public class Solution {
                    public static void main(String[] args) {
                        Scanner scanner = new Scanner(System.in);
                        if (!scanner.hasNextLine()) return;
                        String[] parts = scanner.nextLine().trim().split("\\s+");
                        int target = scanner.nextInt();
                        int[] nums = new int[parts.length];
                        for (int i = 0; i < parts.length; i++) {
                            nums[i] = Integer.parseInt(parts[i]);
                        }
                        HashMap<Integer, Integer> map = new HashMap<>();
                        for (int i = 0; i < nums.length; i++) {
                            int diff = target - nums[i];
                            if (map.containsKey(diff)) {
                                System.out.println(map.get(diff) + " " + i);
                                return;
                            }
                            map.put(nums[i], i);
                        }
                    }
                }
                """,
                """
                import sys

                def solve():
                    lines = sys.stdin.read().splitlines()
                    if not lines:
                        return
                    nums = list(map(int, lines[0].split()))
                    target = int(lines[1])
                    seen = {}
                    for i, num in enumerate(nums):
                        diff = target - num
                        if diff in seen:
                            print(f"{seen[diff]} {i}")
                            return
                        seen[num] = i

                if __name__ == '__main__':
                    solve()
                """,
                3000, 256
        );
        p1 = problemRepository.save(p1);

        testCaseRepository.save(new CodingTestCase(p1, "2 7 11 15\n9", "0 1", false, "2 + 7 = 9", 1));
        testCaseRepository.save(new CodingTestCase(p1, "3 2 4\n6", "1 2", false, "2 + 4 = 6", 2));
        testCaseRepository.save(new CodingTestCase(p1, "3 3\n6", "0 1", true, "3 + 3 = 6", 3));

        // Problem 2: Valid Palindrome
        CodingProblem p2 = new CodingProblem(
                "Valid Palindrome",
                "A phrase is a palindrome if, after converting all uppercase letters into lowercase letters and removing all non-alphanumeric characters, it reads the same forward and backward.\nOutput `true` or `false`.",
                Difficulty.EASY,
                "Strings",
                "1 <= s.length <= 2 * 10^5",
                "A single string line s",
                "`true` or `false`",
                """
                import java.util.Scanner;

                public class Solution {
                    public static void main(String[] args) {
                        Scanner scanner = new Scanner(System.in);
                        String s = scanner.hasNextLine() ? scanner.nextLine() : "";
                        String clean = s.replaceAll("[^a-zA-Z0-9]", "").toLowerCase();
                        String rev = new StringBuilder(clean).reverse().toString();
                        System.out.println(clean.equals(rev) ? "true" : "false");
                    }
                }
                """,
                """
                import sys

                def solve():
                    s = sys.stdin.read().strip()
                    clean = ''.join(c.lower() for c in s if c.isalnum())
                    print("true" if clean == clean[::-1] else "false")

                if __name__ == '__main__':
                    solve()
                """,
                3000, 256
        );
        p2 = problemRepository.save(p2);

        testCaseRepository.save(new CodingTestCase(p2, "A man, a plan, a canal: Panama", "true", false, "amanaplanacanalpanama is a palindrome", 1));
        testCaseRepository.save(new CodingTestCase(p2, "race a car", "false", false, "raceacar is not a palindrome", 2));
        testCaseRepository.save(new CodingTestCase(p2, " ", "true", true, "empty clean string is palindrome", 3));

        logger.info("Successfully seeded Coding problem bank.");
    }

    public List<CodingProblemResponse> getAllProblems() {
        return problemRepository.findAll().stream()
                .map(this::toProblemResponse)
                .collect(Collectors.toList());
    }

    public CodingProblemResponse getProblemById(Long problemId) {
        CodingProblem problem = problemRepository.findById(problemId)
                .orElseThrow(() -> new ResourceNotFoundException("Coding problem not found with id: " + problemId));
        return toProblemResponse(problem);
    }

    private CodingProblemResponse toProblemResponse(CodingProblem problem) {
        List<CodingTestCase> visibleCases = testCaseRepository.findByCodingProblemIdAndIsHiddenFalseOrderByDisplayOrderAsc(problem.getId());
        List<CodingTestCaseDto> visibleDtos = visibleCases.stream()
                .map(tc -> new CodingTestCaseDto(tc.getId(), tc.getInputData(), tc.getExpectedOutput(), tc.getExplanation()))
                .collect(Collectors.toList());

        return new CodingProblemResponse(
                problem.getId(),
                problem.getTitle(),
                problem.getDescription(),
                problem.getDifficulty(),
                problem.getDsaCategory(),
                problem.getConstraintsText(),
                problem.getInputFormat(),
                problem.getOutputFormat(),
                problem.getStarterCodeJava(),
                problem.getStarterCodePython(),
                problem.getTimeLimitMs(),
                problem.getMemoryLimitMb(),
                visibleDtos
        );
    }

    @Transactional
    public RunCodeResponse runCode(Long sessionId, Long questionId, Long userId, RunCodeRequest request) {
        InterviewSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Interview session not found with id: " + sessionId));

        if (!session.getUser().getId().equals(userId)) {
            throw new UnauthorizedException("You are not authorized to run code for this session");
        }

        InterviewQuestion question = questionRepository.findById(questionId)
                .orElseThrow(() -> new ResourceNotFoundException("Interview question not found with id: " + questionId));

        CodingProblem problem = problemRepository.findByTitle(question.getQuestion().getQuestionText())
                .orElseGet(() -> problemRepository.findAll().get(0));

        // Fetch ONLY visible test cases for RUN
        List<CodingTestCase> visibleTestCases = testCaseRepository.findByCodingProblemIdAndIsHiddenFalseOrderByDisplayOrderAsc(problem.getId());

        CodeExecutionService.ExecutionBatchResult batchResult = codeExecutionService.executeCode(
                request.getLanguage(),
                request.getCode(),
                visibleTestCases,
                problem.getTimeLimitMs()
        );

        // Record CodeSubmission log for RUN attempt
        CodeSubmission submission = new CodeSubmission(
                session, question, session.getUser(),
                "RUN", request.getLanguage(), request.getCode(),
                batchResult.status, batchResult.passedCount, batchResult.totalCount,
                batchResult.executionTimeMs, batchResult.compileOutput
        );
        submissionRepository.save(submission);

        return new RunCodeResponse(
                batchResult.status,
                batchResult.passedCount,
                batchResult.totalCount,
                batchResult.executionTimeMs,
                batchResult.compileOutput,
                batchResult.testResults
        );
    }

    @Transactional
    public SubmitCodeResponse submitCode(Long sessionId, Long questionId, Long userId, RunCodeRequest request) {
        InterviewSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Interview session not found with id: " + sessionId));

        if (!session.getUser().getId().equals(userId)) {
            throw new UnauthorizedException("You are not authorized to submit code for this session");
        }

        InterviewQuestion question = questionRepository.findById(questionId)
                .orElseThrow(() -> new ResourceNotFoundException("Interview question not found with id: " + questionId));

        CodingProblem problem = problemRepository.findByTitle(question.getQuestion().getQuestionText())
                .orElseGet(() -> problemRepository.findAll().get(0));

        // Fetch ALL test cases (visible + hidden) for SUBMIT
        List<CodingTestCase> allTestCases = testCaseRepository.findByCodingProblemIdOrderByDisplayOrderAsc(problem.getId());

        CodeExecutionService.ExecutionBatchResult batchResult = codeExecutionService.executeCode(
                request.getLanguage(),
                request.getCode(),
                allTestCases,
                problem.getTimeLimitMs()
        );

        // Record CodeSubmission log for SUBMIT attempt
        CodeSubmission submission = new CodeSubmission(
                session, question, session.getUser(),
                "SUBMIT", request.getLanguage(), request.getCode(),
                batchResult.status, batchResult.passedCount, batchResult.totalCount,
                batchResult.executionTimeMs, batchResult.compileOutput
        );
        submissionRepository.save(submission);

        // Save/Update InterviewResponse (Final candidate code)
        InterviewResponse response = responseRepository.findByInterviewQuestionId(question.getId())
                .orElse(new InterviewResponse(question, request.getCode(), InputMethod.CODE));
        response.setAnswerText(request.getCode());
        response.setInputMethod(InputMethod.CODE);
        response = responseRepository.save(response);

        // Format execution summary (No raw hidden test inputs/outputs sent to AI)
        String execSummary = "Passed %d/%d test cases. Execution Status: %s. Execution Time: %dms."
                .formatted(batchResult.passedCount, batchResult.totalCount, batchResult.status.name(), batchResult.executionTimeMs);

        // Perform qualitative Groq AI evaluation via CodingEvaluationPromptBuilder
        String prompt = evaluationPromptBuilder.buildPrompt(
                problem.getTitle(),
                problem.getDescription(),
                request.getLanguage(),
                request.getCode(),
                execSummary
        );

        String rawAiResponse = groqAIService.callGroqChatApi(prompt);
        CodingEvaluationDto evaluationDto = evaluationPromptBuilder.parseResponse(rawAiResponse);

        // Map score based on authoritative execution
        int overallScore = (batchResult.passedCount * 10) / Math.max(1, batchResult.totalCount);

        // Save/Update InterviewEvaluation
        InterviewEvaluation evaluation = evaluationRepository.findByInterviewResponseId(response.getId())
                .orElse(new InterviewEvaluation());
        evaluation.setInterviewResponse(response);
        evaluation.setScore(overallScore);
        evaluation.setCorrectness(evaluationDto.getCorrectnessScore());
        evaluation.setRelevance(8);
        evaluation.setClarity(evaluationDto.getCodeQualityScore());
        evaluation.setTechnicalAccuracy(overallScore);
        evaluation.setStrengths(String.join("; ", evaluationDto.getStrengths()));
        evaluation.setWeaknesses(String.join("; ", evaluationDto.getWeaknesses()));
        evaluation.setFeedback(evaluationDto.getOverallFeedback());
        evaluation.setImprovementSuggestion(String.join("; ", evaluationDto.getOptimizationSuggestions()));

        // Phase 7 Adaptive decision logic
        boolean isHardLimitReached = question.getQuestionOrder() >= session.getQuestionCount();
        AdaptiveDecision decision = isHardLimitReached ? AdaptiveDecision.COMPLETE :
                (overallScore >= 8 ? AdaptiveDecision.DEEP_DIVE :
                (overallScore >= 5 ? AdaptiveDecision.FOLLOW_UP : AdaptiveDecision.SIMPLIFY));

        evaluation.setAdaptiveDecision(decision);
        evaluation.setAdaptiveReason("Authoritative score: " + overallScore + "/10. Tests passed: " + batchResult.passedCount + "/" + batchResult.totalCount);
        evaluation.setTargetConcept(problem.getDsaCategory());
        evaluation.setUserFacingMessage("Code evaluation complete. Status: " + batchResult.status);
        evaluation.setNextDifficulty(session.getDifficulty());
        evaluationRepository.save(evaluation);

        // Check if session completed
        boolean isCompleted = isHardLimitReached || decision == AdaptiveDecision.COMPLETE;
        if (isCompleted) {
            session.setStatus(SessionStatus.COMPLETED);
            session.setCompletedAt(LocalDateTime.now());
            sessionRepository.save(session);
        }

        return new SubmitCodeResponse(
                batchResult.status,
                batchResult.passedCount,
                batchResult.totalCount,
                batchResult.executionTimeMs,
                batchResult.compileOutput,
                batchResult.testResults,
                evaluationDto,
                decision,
                isCompleted
        );
    }
}
