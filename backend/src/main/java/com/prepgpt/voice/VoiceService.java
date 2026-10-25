package com.prepgpt.voice;

import com.prepgpt.dto.VoiceTranscriptionResponse;

public interface VoiceService {

    VoiceTranscriptionResponse transcribe(byte[] audioBytes, String contentType, String filename);

    byte[] synthesize(String text);
}
