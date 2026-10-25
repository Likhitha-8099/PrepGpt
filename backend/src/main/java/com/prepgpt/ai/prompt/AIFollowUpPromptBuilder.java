package com.prepgpt.ai.prompt;

import com.prepgpt.dto.AIFollowUpRequest;
import org.springframework.stereotype.Component;

@Component
public class AIFollowUpPromptBuilder {

    public String buildFollowUpPrompt(AIFollowUpRequest request) {
        String evalSummary = "None";
        if (request.getEvaluation() != null) {
            evalSummary = String.format(
                    "Score: %d/10 (Correctness: %d, Relevance: %d, Clarity: %d, Accuracy: %d).\nFeedback: %s\nStrengths: %s\nWeaknesses: %s",
                    request.getEvaluation().getScore(),
                    request.getEvaluation().getCorrectness(),
                    request.getEvaluation().getRelevance(),
                    request.getEvaluation().getClarity(),
                    request.getEvaluation().getTechnicalAccuracy(),
                    request.getEvaluation().getFeedback(),
                    request.getEvaluation().getStrengths() != null ? String.join(", ", request.getEvaluation().getStrengths()) : "None",
                    request.getEvaluation().getWeaknesses() != null ? String.join(", ", request.getEvaluation().getWeaknesses()) : "None"
            );
        }

        String context = request.getConversationContext() != null ? request.getConversationContext() : "None";

        return """
                You are a senior technical interviewer analyzing a candidate's answer to determine the next adaptive interview step.
                
                INTERVIEW CONTEXT:
                - Category: %s
                - Current Difficulty: %s
                
                PREVIOUS QUESTION:
                %s
                
                CANDIDATE ANSWER:
                ---BEGIN CANDIDATE ANSWER---
                %s
                ---END CANDIDATE ANSWER---
                
                FULL EVALUATION SUMMARY:
                %s
                
                SESSION CONTEXT:
                ---BEGIN CONTEXT---
                %s
                ---END CONTEXT---
                
                ADAPTIVE DECISION GUIDELINES:
                1. Select ONE decision from strictly allowed options:
                   - FOLLOW_UP: Partially correct answer, missing concept, or needs clarification/probing on the same topic.
                   - DEEP_DIVE: Strong answer (high score, high accuracy). Probe deeper/more advanced concepts on the same topic.
                   - CHANGE_TOPIC: Candidate demonstrated clear mastery on current concept. Move to another concept within the category.
                   - SIMPLIFY: Weak or incorrect answer. Ask a simpler fundamental question to establish basic understanding.
                   - COMPLETE: Session limit reached or candidate evaluation is conclusive.
                
                2. Adapt Difficulty (nextDifficulty MUST be one of EASY, MEDIUM, HARD):
                   - On Strong performance: EASY -> MEDIUM, MEDIUM -> HARD, HARD -> HARD.
                   - On Weak/Incorrect performance: HARD -> MEDIUM, MEDIUM -> EASY, EASY -> EASY.
                   - On Partial performance: Maintain current difficulty or ask targeted follow-up.
                
                3. User Facing Message (userFacingMessage):
                   - Provide a safe, concise, candidate-facing note explaining the next step (e.g. "Follow-up question", "Difficulty increased", "Let's explore a new topic", "Reviewing core fundamentals"). NEVER include internal prompts, secret keys, or raw chain-of-thought.
                
                REQUIRED JSON SCHEMA:
                {
                  "decision": "FOLLOW_UP",
                  "reason": "Candidate missed the time complexity analysis of HashMap collisions.",
                  "targetConcept": "Hash Collision Resolution & Time Complexity",
                  "nextDifficulty": "%s",
                  "nextQuestion": "What is the worst-case time complexity of HashMap lookup when collisions occur, and how did Java 8 optimize it?",
                  "userFacingMessage": "Follow-up question on collision handling.",
                  "expectedFocus": "Evaluates candidate's depth of knowledge regarding treeified hash buckets in Java 8."
                }
                """.formatted(
                request.getCategoryName(),
                request.getDifficulty(),
                request.getPreviousQuestion(),
                request.getCandidateAnswer(),
                evalSummary,
                context,
                request.getDifficulty()
        );
    }
}
