package com.prepgpt.ai;

import com.prepgpt.dto.*;
import com.prepgpt.entity.Difficulty;
import com.prepgpt.entity.QuestionType;
import org.springframework.stereotype.Service;

import java.util.List;

@Service("mockAIService")
public class MockAIService implements AIService {

    @Override
    public AIQuestionResponse generateQuestion(AIQuestionRequest request) {
        int qNum = request.getQuestionNumber() != null ? request.getQuestionNumber() : 1;
        String category = request.getCategoryName() != null ? request.getCategoryName() : "Technical";
        Difficulty diff = request.getDifficulty() != null ? request.getDifficulty() : Difficulty.MEDIUM;
        QuestionType type = request.getQuestionType() != null ? request.getQuestionType() : QuestionType.TECHNICAL;

        String qText = String.format("AI Mock Question #%d for %s (%s): Explain key principles and trade-offs.", qNum, category, diff);
        String topic = "Core Concepts";
        String rationale = "Evaluates structured technical thinking.";

        return new AIQuestionResponse(qText, type, diff, topic, rationale);
    }

    @Override
    public AIEvaluationResponse evaluateAnswer(AIEvaluationRequest request) {
        String answer = request.getCandidateAnswer() != null ? request.getCandidateAnswer().toLowerCase() : "";
        int score = answer.length() > 50 ? 8 : (answer.length() > 15 ? 6 : 4);

        List<String> strengths = List.of(
                "Addressed the primary question directly.",
                "Used clear structural organization."
        );

        List<String> weaknesses = List.of(
                "Could provide a more detailed code or practical architectural example."
        );

        String feedback = "Good explanation of the fundamental concepts. Expanding on real-world edge cases will strengthen your response.";
        String suggestion = "Include a concrete scenario or trade-off analysis when explaining this topic.";

        return new AIEvaluationResponse(score, score, score + 1 > 10 ? 10 : score + 1, score, score, strengths, weaknesses, feedback, suggestion);
    }

    @Override
    public AIFollowUpResponse generateFollowUp(AIFollowUpRequest request) {
        String answer = request.getCandidateAnswer() != null ? request.getCandidateAnswer() : "";
        Difficulty currentDiff = request.getDifficulty() != null ? request.getDifficulty() : Difficulty.MEDIUM;

        if (answer.length() < 20) {
            Difficulty simplifiedDiff = currentDiff == Difficulty.HARD ? Difficulty.MEDIUM : Difficulty.EASY;
            return new AIFollowUpResponse(
                    com.prepgpt.entity.AdaptiveDecision.SIMPLIFY,
                    "Answer was very brief; simplifying to core fundamentals.",
                    "Core Fundamentals",
                    "Let's review the core fundamentals.",
                    simplifiedDiff,
                    "Can you explain the basic definition and primary use case?",
                    "Evaluates fundamental concepts."
            );
        } else if (answer.length() < 60) {
            return new AIFollowUpResponse(
                    com.prepgpt.entity.AdaptiveDecision.FOLLOW_UP,
                    "Candidate answer was concise; probing for deeper understanding.",
                    "Edge Cases & Elaborations",
                    "Follow-up question.",
                    currentDiff,
                    "Can you elaborate with a concrete example or explain how you would handle edge cases?",
                    "Evaluates edge-case handling."
            );
        } else if (answer.length() > 150) {
            Difficulty deeperDiff = currentDiff == Difficulty.EASY ? Difficulty.MEDIUM : Difficulty.HARD;
            return new AIFollowUpResponse(
                    com.prepgpt.entity.AdaptiveDecision.DEEP_DIVE,
                    "Strong comprehensive answer; advancing difficulty to deeper concept.",
                    "Advanced System Architecture",
                    "Great explanation! Difficulty increased.",
                    deeperDiff,
                    "How would you optimize this design under high concurrency and distributed state replication?",
                    "Evaluates advanced architectural scaling."
            );
        }

        return new AIFollowUpResponse(
                com.prepgpt.entity.AdaptiveDecision.CHANGE_TOPIC,
                "Answer was sufficiently comprehensive.",
                "Next Track Topic",
                "Exploring new concept.",
                currentDiff,
                null,
                "Evaluates next core topic."
        );
    }
}
