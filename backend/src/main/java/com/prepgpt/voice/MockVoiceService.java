package com.prepgpt.voice;

import com.prepgpt.dto.VoiceTranscriptionResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service("mockVoiceService")
public class MockVoiceService implements VoiceService {

    private static final Logger log = LoggerFactory.getLogger(MockVoiceService.class);

    @Override
    public VoiceTranscriptionResponse transcribe(byte[] audioBytes, String contentType, String filename) {
        log.info("MockVoiceService: Transcribing audio file '{}' ({} bytes, content-type '{}')",
                filename, audioBytes != null ? audioBytes.length : 0, contentType);

        if (audioBytes == null || audioBytes.length == 0) {
            throw new IllegalArgumentException("Audio file is empty.");
        }

        // Return deterministic mock speech transcription
        return new VoiceTranscriptionResponse(
                "I believe the core tradeoff involves balancing memory overhead with time complexity.",
                "en",
                0.96f
        );
    }

    @Override
    public byte[] synthesize(String text) {
        log.info("MockVoiceService: Synthesizing speech audio for text length {}", text != null ? text.length() : 0);

        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("Synthesis text cannot be empty.");
        }

        // Generate a valid 44-byte WAV audio header with silent PCM payload for seamless browser audio player playback
        byte[] wavHeader = new byte[] {
                'R', 'I', 'F', 'F',
                36, 0, 0, 0,        // ChunkSize: 36 + SubChunk2Size
                'W', 'A', 'V', 'E',
                'f', 'm', 't', ' ',
                16, 0, 0, 0,        // Subchunk1Size (16 for PCM)
                1, 0,               // AudioFormat (1 for PCM)
                1, 0,               // NumChannels (1 channel)
                64, 31, 0, 0,       // SampleRate (8000 Hz)
                64, 31, 0, 0,       // ByteRate
                1, 0,               // BlockAlign
                8, 0,               // BitsPerSample (8 bits)
                'd', 'a', 't', 'a',
                0, 0, 0, 0          // Subchunk2Size (0 bytes data)
        };
        return wavHeader;
    }
}
