package com.prepgpt.ai.prompt;

import com.prepgpt.dto.AIQuestionRequest;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
public class AIQuestionPromptBuilder {

    public String buildQuestionPrompt(AIQuestionRequest request) {
        String previousQs = (request.getPreviousQuestions() != null && !request.getPreviousQuestions().isEmpty())
                ? request.getPreviousQuestions().stream().map(q -> "- " + q).collect(Collectors.joining("\n"))
                : "None";

        String context = request.getConversationContext() != null ? request.getConversationContext() : "None";

        return """
                You are a senior technical interviewer conducting a professional mock interview on the PrepGPT platform.
                
                INTERVIEW PARAMETERS:
                - Category: %s
                - Difficulty: %s
                - Question Type: %s
                - Question Progress: Question %d of %d
                
                PREVIOUS QUESTIONS ASKED IN THIS SESSION:
                %s
                
                CONVERSATION CONTEXT:
                ---BEGIN CONTEXT---
                %s
                ---END CONTEXT---
                
                INSTRUCTIONS:
                1. Generate exactly ONE realistic, high-quality interview question tailored to the specified category and difficulty.
                2. Do NOT repeat or rephrase any of the previous questions asked.
                3. Do NOT provide the answer or hints in the question text.
                4. Maintain a clear, concise, professional tone suitable for campus placements and technical interviews.
                5. Return your response ONLY as a single valid JSON object matching the EXACT schema below. Do not include markdown codeblocks or extra text.
                
                REQUIRED JSON SCHEMA:
                {
                  "question": "Question text here",
                  "questionType": "%s",
                  "difficulty": "%s",
                  "topic": "Specific technical sub-topic",
                  "rationale": "Why this question tests candidate competency"
                }
                """.formatted(
                request.getCategoryName(),
                request.getDifficulty(),
                request.getQuestionType() != null ? request.getQuestionType() : "TECHNICAL",
                request.getQuestionNumber() != null ? request.getQuestionNumber() : 1,
                request.getTotalQuestions() != null ? request.getTotalQuestions() : 5,
                previousQs,
                context,
                request.getQuestionType() != null ? request.getQuestionType() : "TECHNICAL",
                request.getDifficulty()
        );
    }
}
