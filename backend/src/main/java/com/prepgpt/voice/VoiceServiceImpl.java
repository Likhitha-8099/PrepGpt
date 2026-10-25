package com.prepgpt.voice;

import com.prepgpt.dto.VoiceTranscriptionResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

@Service
@Primary
public class VoiceServiceImpl implements VoiceService {

    private static final Logger log = LoggerFactory.getLogger(VoiceServiceImpl.class);

    private final VoiceService mockVoiceService;
    private final VoiceService restVoiceService;

    @Value("${voice.provider:groq}")
    private String voiceProvider;

    @Value("${groq.api-key:}")
    private String groqApiKey;

    public VoiceServiceImpl(
            MockVoiceService mockVoiceService,
            RestVoiceService restVoiceService) {
        this.mockVoiceService = mockVoiceService;
        this.restVoiceService = restVoiceService;
    }

    private VoiceService getActiveVoiceService() {
        if ("mock".equalsIgnoreCase(voiceProvider) || groqApiKey == null || groqApiKey.isBlank()) {
            log.debug("Using MockVoiceService (provider={}, keyConfigured={})",
                    voiceProvider, groqApiKey != null && !groqApiKey.isBlank());
            return mockVoiceService;
        }
        return restVoiceService;
    }

    @Override
    public VoiceTranscriptionResponse transcribe(byte[] audioBytes, String contentType, String filename) {
        return getActiveVoiceService().transcribe(audioBytes, contentType, filename);
    }

    @Override
    public byte[] synthesize(String text) {
        return getActiveVoiceService().synthesize(text);
    }
}
