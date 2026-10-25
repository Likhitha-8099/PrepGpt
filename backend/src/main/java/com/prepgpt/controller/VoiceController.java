package com.prepgpt.controller;

import com.prepgpt.dto.VoiceSynthesisRequest;
import com.prepgpt.dto.VoiceTranscriptionResponse;
import com.prepgpt.entity.InterviewSession;
import com.prepgpt.entity.SessionStatus;
import com.prepgpt.entity.User;
import com.prepgpt.exception.BadRequestException;
import com.prepgpt.exception.ResourceNotFoundException;

import com.prepgpt.repository.InterviewSessionRepository;
import com.prepgpt.repository.UserRepository;
import com.prepgpt.voice.VoiceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/interview-sessions")
@Tag(name = "Voice Interview API", description = "Endpoints for Speech-to-Text transcription and Text-to-Speech synthesis")
public class VoiceController {

    private static final long MAX_AUDIO_SIZE_BYTES = 10 * 1024 * 1024; // 10 MB limit

    private final VoiceService voiceService;
    private final InterviewSessionRepository sessionRepository;
    private final UserRepository userRepository;

    public VoiceController(
            VoiceService voiceService,
            InterviewSessionRepository sessionRepository,
            UserRepository userRepository) {
        this.voiceService = voiceService;
        this.sessionRepository = sessionRepository;
        this.userRepository = userRepository;
    }

    @PostMapping("/{id}/voice/transcribe")
    @Operation(summary = "Transcribe candidate audio answer", description = "Converts multipart audio file to structured transcript text via STT adapter")
    public ResponseEntity<VoiceTranscriptionResponse> transcribeAudio(
            Authentication authentication,
            @PathVariable("id") Long sessionId,
            @RequestParam("file") MultipartFile file) {

        User user = getUser(authentication);
        InterviewSession session = getAuthorizedSession(sessionId, user);

        if (session.getStatus() != SessionStatus.IN_PROGRESS) {
            throw new BadRequestException("Speech transcription is only allowed for active IN_PROGRESS interview sessions.");
        }

        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Audio file is required and cannot be empty.");
        }

        if (file.getSize() > MAX_AUDIO_SIZE_BYTES) {
            throw new BadRequestException("Audio recording is too large (" + (file.getSize() / (1024 * 1024)) + " MB). Maximum allowed size is 10 MB.");
        }

        String contentType = file.getContentType();
        if (contentType != null && !contentType.toLowerCase().startsWith("audio/")
                && !contentType.toLowerCase().startsWith("video/")
                && !contentType.equalsIgnoreCase("application/octet-stream")) {
            throw new BadRequestException("Unsupported audio format: " + contentType);
        }

        byte[] audioBytes;
        try {
            audioBytes = file.getBytes();
        } catch (java.io.IOException e) {
            throw new BadRequestException("Failed to read uploaded audio file: " + e.getMessage());
        }

        try {
            VoiceTranscriptionResponse response = voiceService.transcribe(
                    audioBytes,
                    contentType != null ? contentType : "audio/webm",
                    file.getOriginalFilename()
            );
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            if (e instanceof com.prepgpt.exception.VoiceServiceUnavailableException
                    || e instanceof BadRequestException) {
                throw e;
            }
            throw new BadRequestException("Failed to process audio file: " + e.getMessage());
        }
    }

    @PostMapping("/{id}/voice/synthesize")
    @Operation(summary = "Synthesize AI interviewer question into speech audio", description = "Converts AI question text into MP3 audio via TTS adapter")
    public ResponseEntity<byte[]> synthesizeText(
            Authentication authentication,
            @PathVariable("id") Long sessionId,
            @Valid @RequestBody VoiceSynthesisRequest request) {

        User user = getUser(authentication);
        InterviewSession session = getAuthorizedSession(sessionId, user);

        if (session.getStatus() == SessionStatus.CONFIGURED || session.getStatus() == SessionStatus.ABANDONED) {
            throw new BadRequestException("Text-to-Speech synthesis is not allowed for " + session.getStatus() + " sessions.");
        }

        byte[] audioBytes = voiceService.synthesize(request.getText());

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("audio/mpeg"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"speech.mp3\"")
                .body(audioBytes);
    }

    private User getUser(Authentication authentication) {
        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", authentication.getName()));
    }

    private InterviewSession getAuthorizedSession(Long sessionId, User user) {
        return sessionRepository.findByIdAndUserId(sessionId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("InterviewSession", "id", sessionId));
    }
}
