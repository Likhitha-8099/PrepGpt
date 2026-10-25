package com.prepgpt.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.prepgpt.ai.prompt.AIEvaluationPromptBuilder;
import com.prepgpt.ai.prompt.AIFollowUpPromptBuilder;
import com.prepgpt.ai.prompt.AIQuestionPromptBuilder;
import com.prepgpt.dto.*;
import com.prepgpt.entity.AdaptiveDecision;
import com.prepgpt.entity.Difficulty;
import com.prepgpt.entity.QuestionType;
import com.prepgpt.exception.AIServiceUnavailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
@Primary
public class GroqAIService implements AIService {

    private static final Logger logger = LoggerFactory.getLogger(GroqAIService.class);

    private final String apiKey;
    private final String model;
    private final String baseUrl;

    private final AIQuestionPromptBuilder questionPromptBuilder;
    private final AIEvaluationPromptBuilder evaluationPromptBuilder;
    private final AIFollowUpPromptBuilder followUpPromptBuilder;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;
    private final io.github.resilience4j.circuitbreaker.CircuitBreaker circuitBreaker;
    private final io.github.resilience4j.retry.Retry retry;
    private final com.prepgpt.service.AiTokenUsageService tokenUsageService;

    @org.springframework.beans.factory.annotation.Autowired
    public GroqAIService(
            @Value("${groq.api-key:}") String apiKey,
            @Value("${groq.ai.model:openai/gpt-oss-120b}") String model,
            @Value("${groq.ai.base-url:https://api.groq.com/openai/v1/chat/completions}") String baseUrl,
            AIQuestionPromptBuilder questionPromptBuilder,
            AIEvaluationPromptBuilder evaluationPromptBuilder,
            AIFollowUpPromptBuilder followUpPromptBuilder,
            ObjectMapper objectMapper,
            @org.springframework.beans.factory.annotation.Autowired(required = false)
            io.github.resilience4j.circuitbreaker.CircuitBreaker circuitBreaker,
            @org.springframework.beans.factory.annotation.Autowired(required = false)
            io.github.resilience4j.retry.Retry retry,
            @org.springframework.beans.factory.annotation.Autowired(required = false)
            com.prepgpt.service.AiTokenUsageService tokenUsageService) {
        this.apiKey = apiKey != null ? apiKey.trim() : "";
        this.model = model != null && !model.isBlank() ? model.trim() : "openai/gpt-oss-120b";
        this.baseUrl = baseUrl != null && !baseUrl.isBlank() ? baseUrl.trim() : "https://api.groq.com/openai/v1/chat/completions";
        this.questionPromptBuilder = questionPromptBuilder;
        this.evaluationPromptBuilder = evaluationPromptBuilder;
        this.followUpPromptBuilder = followUpPromptBuilder;
        this.objectMapper = objectMapper;
        this.circuitBreaker = circuitBreaker;
        this.retry = retry;
        this.tokenUsageService = tokenUsageService;

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10000);
        factory.setReadTimeout(15000);
        this.restTemplate = new RestTemplate(factory);
    }

    public GroqAIService(
            String apiKey,
            String model,
            String baseUrl,
            AIQuestionPromptBuilder questionPromptBuilder,
            AIEvaluationPromptBuilder evaluationPromptBuilder,
            AIFollowUpPromptBuilder followUpPromptBuilder,
            ObjectMapper objectMapper,
            io.github.resilience4j.circuitbreaker.CircuitBreaker circuitBreaker,
            io.github.resilience4j.retry.Retry retry) {
        this(apiKey, model, baseUrl, questionPromptBuilder, evaluationPromptBuilder, followUpPromptBuilder, objectMapper, circuitBreaker, retry, null);
    }

    public GroqAIService(
            String apiKey,
            String model,
            String baseUrl,
            AIQuestionPromptBuilder questionPromptBuilder,
            AIEvaluationPromptBuilder evaluationPromptBuilder,
            AIFollowUpPromptBuilder followUpPromptBuilder,
            ObjectMapper objectMapper) {
        this(apiKey, model, baseUrl, questionPromptBuilder, evaluationPromptBuilder, followUpPromptBuilder, objectMapper, null, null, null);
    }

    @Override
    public AIQuestionResponse generateQuestion(AIQuestionRequest request) {
        verifyConfigured();
        String prompt = questionPromptBuilder.buildQuestionPrompt(request);
        String rawJson = callGroqChatApi(prompt, com.prepgpt.entity.AiOperationType.QUESTION_GENERATION);

        try {
            JsonNode root = objectMapper.readTree(rawJson);
            String qText = root.path("question").asText();
            if (qText == null || qText.isBlank()) {
                throw new AIServiceUnavailableException("Groq AI service returned empty question text.");
            }

            QuestionType qType = parseEnumOrDefault(root.path("questionType").asText(), QuestionType.class, request.getQuestionType() != null ? request.getQuestionType() : QuestionType.TECHNICAL);
            Difficulty diff = parseEnumOrDefault(root.path("difficulty").asText(), Difficulty.class, request.getDifficulty());
            String topic = root.path("topic").asText("Technical Knowledge");
            String rationale = root.path("rationale").asText("Evaluates key concepts.");

            return new AIQuestionResponse(qText, qType, diff, topic, rationale);
        } catch (Exception e) {
            logger.error("Failed to parse Groq AI question response JSON: {}", rawJson, e);
            throw new AIServiceUnavailableException("Failed to parse structured AI question response.", e);
        }
    }

    @Override
    public AIEvaluationResponse evaluateAnswer(AIEvaluationRequest request) {
        verifyConfigured();
        String prompt = evaluationPromptBuilder.buildEvaluationPrompt(request);
        String rawJson = callGroqChatApi(prompt, com.prepgpt.entity.AiOperationType.ANSWER_EVALUATION);

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
            logger.error("Failed to parse Groq AI evaluation response JSON: {}", rawJson, e);
            throw new AIServiceUnavailableException("Failed to parse structured AI evaluation response.", e);
        }
    }

    @Override
    public AIFollowUpResponse generateFollowUp(AIFollowUpRequest request) {
        verifyConfigured();
        String prompt = followUpPromptBuilder.buildFollowUpPrompt(request);
        String rawJson = callGroqChatApi(prompt, com.prepgpt.entity.AiOperationType.FOLLOW_UP_GENERATION);

        try {
            JsonNode root = objectMapper.readTree(rawJson);

            AdaptiveDecision decision = parseEnumOrDefault(root.path("decision").asText(), AdaptiveDecision.class, null);
            if (decision == null) {
                boolean legacyShouldAsk = root.path("shouldAskFollowUp").asBoolean(false);
                decision = legacyShouldAsk ? AdaptiveDecision.FOLLOW_UP : AdaptiveDecision.CHANGE_TOPIC;
            }

            String reason = root.path("reason").asText("Adaptive decision evaluated.");
            String targetConcept = root.path("targetConcept").asText(null);
            String userFacingMessage = root.path("userFacingMessage").asText(null);
            String nextQuestion = root.path("nextQuestion").asText(root.path("question").asText(null));
            String expectedFocus = root.path("expectedFocus").asText(null);

            Difficulty nextDifficulty = parseEnumOrDefault(root.path("nextDifficulty").asText(root.path("difficulty").asText()), Difficulty.class, request.getDifficulty());

            return new AIFollowUpResponse(decision, reason, targetConcept, userFacingMessage, nextDifficulty, nextQuestion, expectedFocus);
        } catch (Exception e) {
            logger.error("Failed to parse Groq AI follow-up response JSON: {}", rawJson, e);
            return new AIFollowUpResponse(AdaptiveDecision.CHANGE_TOPIC, "Parsing error fallback", null, null, request.getDifficulty(), null, null);
        }
    }

    private void verifyConfigured() {
        if (apiKey.isBlank()) {
            logger.warn("GROQ_API_KEY is missing or blank. Service is unavailable.");
            throw new AIServiceUnavailableException("AI interviewer is temporarily unavailable (GROQ_API_KEY is not configured in environment).");
        }
    }

    public String callGroqChatApi(String prompt) {
        return callGroqChatApi(prompt, com.prepgpt.entity.AiOperationType.GENERAL_AI);
    }

    public String callGroqChatApi(String prompt, com.prepgpt.entity.AiOperationType operationType) {
        java.util.function.Supplier<String> supplier = () -> executeHttpRequest(prompt, operationType);

        if (retry != null) {
            supplier = io.github.resilience4j.retry.Retry.decorateSupplier(retry, supplier);
        }
        if (circuitBreaker != null) {
            supplier = io.github.resilience4j.circuitbreaker.CircuitBreaker.decorateSupplier(circuitBreaker, supplier);
        }

        try {
            return supplier.get();
        } catch (io.github.resilience4j.circuitbreaker.CallNotPermittedException cne) {
            logger.warn("Groq AI circuit breaker is OPEN. Fast-failing outbound request without calling provider: {}", cne.getMessage());
            throw new AIServiceUnavailableException("AI interviewer is currently temporarily unavailable (circuit breaker open). Please try again shortly.", cne);
        } catch (AIServiceUnavailableException aue) {
            throw aue;
        } catch (Exception e) {
            logger.error("Error communicating with Groq AI API provider: {}", e.getMessage(), e);
            throw new AIServiceUnavailableException("Failed to communicate with Groq AI interviewer provider: " + e.getMessage(), e);
        }
    }

    public static class GroqParsedResult {
        private final String text;
        private final Integer promptTokens;
        private final Integer completionTokens;
        private final Integer totalTokens;

        public GroqParsedResult(String text, Integer promptTokens, Integer completionTokens, Integer totalTokens) {
            this.text = text;
            this.promptTokens = promptTokens;
            this.completionTokens = completionTokens;
            this.totalTokens = totalTokens;
        }

        public String getText() {
            return text;
        }

        public Integer getPromptTokens() {
            return promptTokens;
        }

        public Integer getCompletionTokens() {
            return completionTokens;
        }

        public Integer getTotalTokens() {
            return totalTokens;
        }
    }

    private String executeHttpRequest(String prompt, com.prepgpt.entity.AiOperationType operationType) {
        Map<String, Object> systemMessage = Map.of(
                "role", "system",
                "content", "You are a professional AI technical interviewer. You MUST respond with valid JSON matching the requested schema."
        );
        Map<String, Object> userMessage = Map.of(
                "role", "user",
                "content", prompt
        );

        Map<String, Object> responseFormat = Map.of("type", "json_object");

        Map<String, Object> requestBody = Map.of(
                "model", model,
                "messages", List.of(systemMessage, userMessage),
                "response_format", responseFormat,
                "temperature", 0.7
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);
        headers.set(HttpHeaders.USER_AGENT, "PrepGPT-Backend/1.0");

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        ResponseEntity<String> response = restTemplate.exchange(baseUrl, HttpMethod.POST, entity, String.class);
        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            try {
                GroqParsedResult parsed = parseGroqResponse(response.getBody());
                if (tokenUsageService != null) {
                    tokenUsageService.recordUsage(operationType, "GROQ", model,
                            parsed.getPromptTokens(), parsed.getCompletionTokens(), parsed.getTotalTokens());
                }
                return parsed.getText();
            } catch (JsonProcessingException jpe) {
                throw new AIServiceUnavailableException("Failed to parse Groq response JSON", jpe);
            }
        }
        throw new AIServiceUnavailableException("Groq AI provider returned unexpected HTTP status: " + response.getStatusCode());
    }

    public GroqParsedResult parseGroqResponse(String responseJson) throws JsonProcessingException {
        JsonNode root = objectMapper.readTree(responseJson);
        JsonNode choices = root.path("choices");
        if (!choices.isArray() || choices.size() == 0) {
            throw new AIServiceUnavailableException("Invalid choices format in Groq AI provider response.");
        }
        String text = choices.get(0).path("message").path("content").asText();
        if (text == null || text.isBlank()) {
            throw new AIServiceUnavailableException("Empty response content from Groq AI provider.");
        }
        // Strip markdown code fences if present
        if (text.startsWith("```json")) {
            text = text.substring(7);
        } else if (text.startsWith("```")) {
            text = text.substring(3);
        }
        if (text.endsWith("```")) {
            text = text.substring(0, text.length() - 3);
        }
        text = text.trim();

        // Extract usage if returned by Groq accurately without fabricating 0s
        Integer promptTokens = null;
        Integer completionTokens = null;
        Integer totalTokens = null;

        JsonNode usageNode = root.get("usage");
        if (usageNode != null && !usageNode.isNull() && !usageNode.isMissingNode()) {
            if (usageNode.has("prompt_tokens") && !usageNode.get("prompt_tokens").isNull()) {
                promptTokens = usageNode.get("prompt_tokens").asInt();
            }
            if (usageNode.has("completion_tokens") && !usageNode.get("completion_tokens").isNull()) {
                completionTokens = usageNode.get("completion_tokens").asInt();
            }
            if (usageNode.has("total_tokens") && !usageNode.get("total_tokens").isNull()) {
                totalTokens = usageNode.get("total_tokens").asInt();
            }
        }

        return new GroqParsedResult(text, promptTokens, completionTokens, totalTokens);
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
