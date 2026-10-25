package com.prepgpt.ai.prompt;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.prepgpt.dto.CodingEvaluationDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class CodingEvaluationPromptBuilder {

    private static final Logger logger = LoggerFactory.getLogger(CodingEvaluationPromptBuilder.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    public String buildPrompt(String problemTitle, String problemDescription, String language,
                              String candidateCode, String executionSummary) {
        return """
                You are a senior technical interviewer and algorithm expert evaluating a candidate's code submission.

                PROBLEM TITLE: %s
                PROBLEM DESCRIPTION:
                %s

                CANDIDATE LANGUAGE: %s
                CANDIDATE CODE:
                ```
                %s
                ```

                AUTHORITATIVE EXECUTION SUMMARY:
                %s

                IMPORTANT:
                1. The execution summary above is authoritative regarding correctness and test case execution.
                2. Evaluate the candidate's code quality, time complexity, space complexity, edge-case handling, and readability.

                Provide your response strictly in the following JSON format:
                {
                  "correctnessScore": <integer 0-10>,
                  "timeComplexity": "<e.g., O(N) or O(N log N)>",
                  "spaceComplexity": "<e.g., O(1) or O(N)>",
                  "codeQualityScore": <integer 0-10>,
                  "strengths": ["<strength 1>", "<strength 2>"],
                  "weaknesses": ["<weakness 1>", "<weakness 2>"],
                  "optimizationSuggestions": ["<suggestion 1>", "<suggestion 2>"],
                  "overallFeedback": "<detailed constructive feedback>"
                }
                """.formatted(
                problemTitle,
                problemDescription,
                language,
                candidateCode,
                executionSummary
        );
    }

    public CodingEvaluationDto parseResponse(String rawAiResponse) {
        try {
            String jsonContent = extractJson(rawAiResponse);
            JsonNode root = objectMapper.readTree(jsonContent);

            int correctnessScore = root.path("correctnessScore").asInt(5);
            String timeComplexity = root.path("timeComplexity").asText("O(N)");
            String spaceComplexity = root.path("spaceComplexity").asText("O(1)");
            int codeQualityScore = root.path("codeQualityScore").asInt(5);

            List<String> strengths = extractStringList(root.path("strengths"));
            List<String> weaknesses = extractStringList(root.path("weaknesses"));
            List<String> suggestions = extractStringList(root.path("optimizationSuggestions"));
            String feedback = root.path("overallFeedback").asText("Code evaluated successfully.");

            return new CodingEvaluationDto(
                    correctnessScore,
                    timeComplexity,
                    spaceComplexity,
                    codeQualityScore,
                    strengths,
                    weaknesses,
                    suggestions,
                    feedback
            );
        } catch (Exception e) {
            logger.error("Failed to parse Groq AI coding evaluation response: {}", e.getMessage());
            return new CodingEvaluationDto(
                    5, "O(N)", "O(1)", 5,
                    List.of("Submitted code executed"),
                    List.of("Consider refining complexity analysis"),
                    List.of("Review edge cases"),
                    "AI evaluation parsed with fallback."
            );
        }
    }

    private String extractJson(String text) {
        if (text == null) return "{}";
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start != -1 && end != -1 && end > start) {
            return text.substring(start, end + 1);
        }
        return text;
    }

    private List<String> extractStringList(JsonNode node) {
        List<String> list = new ArrayList<>();
        if (node != null && node.isArray()) {
            for (JsonNode item : node) {
                if (item.isTextual() && !item.asText().isBlank()) {
                    list.add(item.asText());
                }
            }
        }
        return list;
    }
}
