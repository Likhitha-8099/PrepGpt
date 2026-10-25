package com.prepgpt.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.prepgpt.ai.prompt.AIEvaluationPromptBuilder;
import com.prepgpt.ai.prompt.AIFollowUpPromptBuilder;
import com.prepgpt.ai.prompt.AIQuestionPromptBuilder;
import com.prepgpt.dto.*;
import com.prepgpt.entity.Difficulty;
import com.prepgpt.entity.QuestionType;
import com.prepgpt.exception.AIServiceUnavailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
public class GeminiRestAIService implements AIService {

    private static final Logger logger = LoggerFactory.getLogger(GeminiRestAIService.class);

    private final String apiKey;
    private final String model;
    private final String baseUrl;

    private final AIQuestionPromptBuilder questionPromptBuilder;
    private final AIEvaluationPromptBuilder evaluationPromptBuilder;
    private final AIFollowUpPromptBuilder followUpPromptBuilder;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;

    public GeminiRestAIService(
            @Value("${app.ai.provider:gemini}") String provider,
            @Value("${app.ai.api-key:}") String apiKey,
            @Value("${app.ai.model:gemini-1.5-flash}") String model,
            @Value("${app.ai.base-url:https://generativelanguage.googleapis.com/v1beta/models}") String baseUrl,
            AIQuestionPromptBuilder questionPromptBuilder,
            AIEvaluationPromptBuilder evaluationPromptBuilder,
            AIFollowUpPromptBuilder followUpPromptBuilder,
            ObjectMapper objectMapper) {
        this.apiKey = apiKey != null ? apiKey.trim() : "";
        this.model = model;
        this.baseUrl = baseUrl;
        this.questionPromptBuilder = questionPromptBuilder;
        this.evaluationPromptBuilder = evaluationPromptBuilder;
        this.followUpPromptBuilder = followUpPromptBuilder;
        this.objectMapper = objectMapper;

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10000);
        factory.setReadTimeout(15000);
        this.restTemplate = new RestTemplate(factory);
    }

    @Override
    public AIQuestionResponse generateQuestion(AIQuestionRequest request) {
        verifyConfigured();
        String prompt = questionPromptBuilder.buildQuestionPrompt(request);
        String rawJson = callLlmApi(prompt);

        try {
            JsonNode root = objectMapper.readTree(rawJson);
            String qText = root.path("question").asText();
            if (qText == null || qText.isBlank()) {
                throw new AIServiceUnavailableException("AI service returned empty question text.");
            }

            QuestionType qType = parseEnumOrDefault(root.path("questionType").asText(), QuestionType.class, request.getQuestionType() != null ? request.getQuestionType() : QuestionType.TECHNICAL);
            Difficulty diff = parseEnumOrDefault(root.path("difficulty").asText(), Difficulty.class, request.getDifficulty());
            String topic = root.path("topic").asText("Technical Knowledge");
            String rationale = root.path("rationale").asText("Evaluates key concepts.");

            return new AIQuestionResponse(qText, qType, diff, topic, rationale);
        } catch (Exception e) {
            logger.error("Failed to parse AI question response JSON: {}", rawJson, e);
            throw new AIServiceUnavailableException("Failed to parse structured AI question response.", e);
        }
    }

    @Override
    public AIEvaluationResponse evaluateAnswer(AIEvaluationRequest request) {
        verifyConfigured();
        String prompt = evaluationPromptBuilder.buildEvaluationPrompt(request);
        String rawJson = callLlmApi(prompt);

        try {
            JsonNode root = objectMapper.readTree(rawJson);

            int score = clampScore(root.path("score").asInt(7));
            int correctness = clampScore(root.path("correctness").asInt(7));
            int relevance = clampScore(root.path("relevance").asInt(8));
            int clarity = clampScore(root.path("clarity").asInt(7));
            int technicalAccuracy = clampScore(root.path("technicalAccuracy").asInt(7));

            List<String> strengths = parseStringList(root.path("strengths"));
            List<String> weaknesses = parseStringList(root.path("weaknesses"));
            String feedback = root.path("feedback").asText("Good response provided.");
            String suggestion = root.path("improvementSuggestion").asText("Provide more concrete examples.");

            return new AIEvaluationResponse(score, correctness, relevance, clarity, technicalAccuracy, strengths, weaknesses, feedback, suggestion);
        } catch (Exception e) {
            logger.error("Failed to parse AI evaluation response JSON: {}", rawJson, e);
            throw new AIServiceUnavailableException("Failed to parse structured AI evaluation response.", e);
        }
    }

    @Override
    public AIFollowUpResponse generateFollowUp(AIFollowUpRequest request) {
        verifyConfigured();
        String prompt = followUpPromptBuilder.buildFollowUpPrompt(request);
        String rawJson = callLlmApi(prompt);

        try {
            JsonNode root = objectMapper.readTree(rawJson);
            boolean shouldAsk = root.path("shouldAskFollowUp").asBoolean(false);
            String question = root.path("question").asText(null);
            String reason = root.path("reason").asText(null);
            Difficulty diff = parseEnumOrDefault(root.path("difficulty").asText(), Difficulty.class, request.getDifficulty());

            return new AIFollowUpResponse(shouldAsk, question, reason, diff);
        } catch (Exception e) {
            logger.error("Failed to parse AI follow-up response JSON: {}", rawJson, e);
            return new AIFollowUpResponse(false, null, "Parsing error fallback", request.getDifficulty());
        }
    }

    private void verifyConfigured() {
        if (apiKey.isBlank()) {
            logger.warn("AI API key is missing or blank. Service is unavailable.");
            throw new AIServiceUnavailableException("AI interviewer is temporarily unavailable (API key is not configured in environment).");
        }
    }

    private String callLlmApi(String prompt) {
        String url = String.format("%s/%s:generateContent?key=%s", baseUrl, model, apiKey);

        Map<String, Object> textPart = Map.of("text", prompt);
        Map<String, Object> contentsPart = Map.of("parts", List.of(textPart));
        Map<String, Object> generationConfig = Map.of("response_mime_type", "application/json");

        Map<String, Object> requestBody = Map.of(
                "contents", List.of(contentsPart),
                "generationConfig", generationConfig
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return extractTextFromGeminiResponse(response.getBody());
            }
            throw new AIServiceUnavailableException("AI API provider returned unexpected HTTP status: " + response.getStatusCode());
        } catch (Exception e) {
            logger.error("Error communicating with AI API provider: {}", e.getMessage(), e);
            throw new AIServiceUnavailableException("Failed to communicate with AI interviewer provider.", e);
        }
    }

    private String extractTextFromGeminiResponse(String responseJson) throws JsonProcessingException {
        JsonNode root = objectMapper.readTree(responseJson);
        JsonNode candidates = root.path("candidates");
        if (candidates.isArray() && candidates.size() > 0) {
            JsonNode parts = candidates.get(0).path("content").path("parts");
            if (parts.isArray() && parts.size() > 0) {
                String text = parts.get(0).path("text").asText();
                // Clean markdown code blocks if present
                if (text.startsWith("```json")) {
                    text = text.substring(7);
                } else if (text.startsWith("```")) {
                    text = text.substring(3);
                }
                if (text.endsWith("```")) {
                    text = text.substring(0, text.length() - 3);
                }
                return text.trim();
            }
        }
        throw new AIServiceUnavailableException("Invalid candidate format in AI provider response.");
    }

    private int clampScore(int score) {
        return Math.max(0, Math.min(10, score));
    }

    private List<String> parseStringList(JsonNode node) {
        List<String> list = new ArrayList<>();
        if (node.isArray()) {
            for (JsonNode item : node) {
                if (item.isTextual() && !item.asText().isBlank()) {
                    list.add(item.asText());
                }
            }
        }
        return list;
    }

    private <E extends Enum<E>> E parseEnumOrDefault(String text, Class<E> enumClass, E defaultValue) {
        if (text == null || text.isBlank()) return defaultValue;
        try {
            return Enum.valueOf(enumClass, text.trim().toUpperCase());
        } catch (Exception e) {
            return defaultValue;
        }
    }
}
