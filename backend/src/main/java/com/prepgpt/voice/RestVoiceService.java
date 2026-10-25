package com.prepgpt.voice;

import com.prepgpt.dto.VoiceTranscriptionResponse;
import com.prepgpt.exception.VoiceServiceUnavailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service("restVoiceService")
public class RestVoiceService implements VoiceService {

    private static final Logger log = LoggerFactory.getLogger(RestVoiceService.class);

    private final RestTemplate restTemplate;

    @Value("${groq.api-key:}")
    private String groqApiKey;

    @Value("${groq.stt.model:whisper-large-v3-turbo}")
    private String sttModel;

    @Value("${groq.tts.model:canopylabs/orpheus-v1-english}")
    private String ttsModel;

    @Value("${groq.tts.voice:daniel}")
    private String ttsVoice;

    @Value("${groq.stt.base-url:https://api.groq.com/openai/v1/audio/transcriptions}")
    private String sttBaseUrl;

    @Value("${groq.tts.base-url:https://api.groq.com/openai/v1/audio/speech}")
    private String ttsBaseUrl;

    public RestVoiceService() {
        org.springframework.http.client.SimpleClientHttpRequestFactory factory = new org.springframework.http.client.SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10000);
        factory.setReadTimeout(15000);
        this.restTemplate = new RestTemplate(factory);
    }

    public RestTemplate getRestTemplateForTesting() {
        return this.restTemplate;
    }

    @Override
    public VoiceTranscriptionResponse transcribe(byte[] audioBytes, String contentType, String filename) {
        if (groqApiKey == null || groqApiKey.isBlank()) {
            log.warn("GROQ_API_KEY is missing or blank for STT.");
            throw new VoiceServiceUnavailableException("The Speech-to-Text service is currently unconfigured. Set GROQ_API_KEY in environment configuration.");
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            headers.setBearerAuth(groqApiKey);
            headers.set(HttpHeaders.USER_AGENT, "PrepGPT-Backend/1.0");

            ByteArrayResourceWithFilename audioResource = new ByteArrayResourceWithFilename(
                    audioBytes,
                    filename != null ? filename : "speech.webm"
            );

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", audioResource);
            body.add("model", sttModel);

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    sttBaseUrl,
                    HttpMethod.POST,
                    requestEntity,
                    new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {}
            );
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                String text = (String) response.getBody().get("text");
                return new VoiceTranscriptionResponse(text != null ? text : "", "en", 0.95f);
            } else {
                throw new VoiceServiceUnavailableException("Groq STT provider returned unsuccessful status code: " + response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("Failed to execute Groq STT provider request: {}", e.getMessage());
            throw new VoiceServiceUnavailableException("Speech-to-Text service provider unavailable: " + e.getMessage(), e);
        }
    }

    @Override
    public byte[] synthesize(String text) {
        if (groqApiKey == null || groqApiKey.isBlank()) {
            log.warn("GROQ_API_KEY is missing or blank for TTS.");
            throw new VoiceServiceUnavailableException("The Text-to-Speech service is currently unconfigured. Set GROQ_API_KEY in environment configuration.");
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(groqApiKey);
            headers.set(HttpHeaders.USER_AGENT, "PrepGPT-Backend/1.0");

            Map<String, Object> payload = Map.of(
                    "model", ttsModel,
                    "input", text,
                    "voice", ttsVoice,
                    "response_format", "wav"
            );

            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(payload, headers);
            ResponseEntity<byte[]> response = restTemplate.postForEntity(ttsBaseUrl, requestEntity, byte[].class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody();
            } else {
                throw new VoiceServiceUnavailableException("Groq TTS provider returned status code: " + response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("Failed to execute Groq TTS provider request: {}", e.getMessage());
            throw new VoiceServiceUnavailableException("Text-to-Speech service provider unavailable: " + e.getMessage(), e);
        }
    }

    private static class ByteArrayResourceWithFilename extends org.springframework.core.io.ByteArrayResource {
        private final String filename;

        public ByteArrayResourceWithFilename(byte[] byteArray, String filename) {
            super(byteArray);
            this.filename = filename;
        }

        @Override
        public String getFilename() {
            return filename;
        }
    }
}
