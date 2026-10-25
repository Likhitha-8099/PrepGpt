package com.prepgpt.service;

import com.prepgpt.dto.VoiceTranscriptionResponse;
import com.prepgpt.exception.VoiceServiceUnavailableException;
import com.prepgpt.voice.MockVoiceService;
import com.prepgpt.voice.RestVoiceService;
import com.prepgpt.voice.VoiceServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class VoiceServiceTest {

    private MockVoiceService mockVoiceService;
    private RestVoiceService restVoiceService;
    private VoiceServiceImpl voiceServiceImpl;

    @BeforeEach
    public void setUp() {
        mockVoiceService = new MockVoiceService();
        restVoiceService = new RestVoiceService();
        voiceServiceImpl = new VoiceServiceImpl(mockVoiceService, restVoiceService);
    }

    @Test
    @DisplayName("MockVoiceService returns valid transcription for valid audio bytes")
    public void testMockVoiceTranscribeSuccess() {
        byte[] sampleAudio = new byte[] { 1, 2, 3, 4, 5 };
        VoiceTranscriptionResponse response = mockVoiceService.transcribe(sampleAudio, "audio/webm", "test.webm");

        assertNotNull(response);
        assertNotNull(response.getText());
        assertFalse(response.getText().isBlank());
        assertEquals("en", response.getLanguage());
        assertNotNull(response.getConfidence());
        assertTrue(response.getConfidence() > 0.0f);
    }

    @Test
    @DisplayName("MockVoiceService rejects null or empty audio file")
    public void testMockVoiceTranscribeEmptyAudioRejection() {
        assertThrows(IllegalArgumentException.class, () -> {
            mockVoiceService.transcribe(null, "audio/webm", "test.webm");
        });

        assertThrows(IllegalArgumentException.class, () -> {
            mockVoiceService.transcribe(new byte[0], "audio/webm", "test.webm");
        });
    }

    @Test
    @DisplayName("MockVoiceService synthesizes text into valid WAV audio bytes")
    public void testMockVoiceSynthesizeSuccess() {
        String questionText = "Explain the difference between process and thread in Java.";
        byte[] audioBytes = mockVoiceService.synthesize(questionText);

        assertNotNull(audioBytes);
        assertTrue(audioBytes.length >= 44);
        // Verify WAV header magic bytes
        assertEquals('R', (char) audioBytes[0]);
        assertEquals('I', (char) audioBytes[1]);
        assertEquals('F', (char) audioBytes[2]);
        assertEquals('F', (char) audioBytes[3]);
    }

    @Test
    @DisplayName("MockVoiceService rejects blank or null synthesis text")
    public void testMockVoiceSynthesizeBlankTextRejection() {
        assertThrows(IllegalArgumentException.class, () -> {
            mockVoiceService.synthesize(null);
        });

        assertThrows(IllegalArgumentException.class, () -> {
            mockVoiceService.synthesize("   ");
        });
    }

    @Test
    @DisplayName("RestVoiceService throws 503 VoiceServiceUnavailableException when API keys are missing")
    public void testRestVoiceServiceUnconfiguredFailure() {
        byte[] sampleAudio = new byte[] { 1, 2, 3 };

        assertThrows(VoiceServiceUnavailableException.class, () -> {
            restVoiceService.transcribe(sampleAudio, "audio/webm", "test.webm");
        });

        assertThrows(VoiceServiceUnavailableException.class, () -> {
            restVoiceService.synthesize("Explain polymorphism.");
        });
    }

    @Test
    @DisplayName("VoiceServiceImpl delegates smoothly to MockVoiceService when unconfigured")
    public void testVoiceServiceImplDelegation() {
        byte[] sampleAudio = new byte[] { 10, 20, 30 };
        VoiceTranscriptionResponse transcribeResp = voiceServiceImpl.transcribe(sampleAudio, "audio/webm", "answer.webm");

        assertNotNull(transcribeResp);
        assertNotNull(transcribeResp.getText());

        byte[] audioBytes = voiceServiceImpl.synthesize("What is dependency injection?");
        assertNotNull(audioBytes);
        assertTrue(audioBytes.length > 0);
    }
}
