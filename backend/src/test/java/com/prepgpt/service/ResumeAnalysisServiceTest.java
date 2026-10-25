package com.prepgpt.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.prepgpt.ai.GroqAIService;
import com.prepgpt.ai.prompt.ResumeAnalysisPromptBuilder;
import com.prepgpt.dto.ResumeResponse;
import com.prepgpt.entity.Role;
import com.prepgpt.entity.User;
import com.prepgpt.exception.ResourceNotFoundException;
import com.prepgpt.repository.ProjectRepository;
import com.prepgpt.repository.ResumeRepository;
import com.prepgpt.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ResumeAnalysisServiceTest {

    @Mock
    private ResumeRepository resumeRepository;
    @Mock
    private ProjectRepository projectRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ResumeParserService resumeParserService;
    @Mock
    private ResumeAnalysisPromptBuilder resumeAnalysisPromptBuilder;
    @Mock
    private GroqAIService groqAIService;

    private ObjectMapper objectMapper;
    private ResumeAnalysisService resumeAnalysisService;
    private User testUser;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        resumeAnalysisService = new ResumeAnalysisService(
                resumeRepository, projectRepository, userRepository,
                resumeParserService, resumeAnalysisPromptBuilder,
                groqAIService, objectMapper
        );

        Role studentRole = new Role("ROLE_STUDENT");
        testUser = new User("candidate@example.com", "hash", "Test Candidate", studentRole);
        testUser.setId(10L);
    }

    @Test
    void uploadAndAnalyzeResume_Success() {
        MockMultipartFile file = new MockMultipartFile("file", "resume.pdf", "application/pdf", "Dummy PDF content".getBytes());
        ResumeParserService.FileStorageResult result = new ResumeParserService.FileStorageResult(
                "resume.pdf", "uploads/resumes/uuid.pdf", "PDF", 1024L, "Extracted resume content with Java and React skills."
        );

        when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));
        when(resumeParserService.storeAndExtractText(file)).thenReturn(result);
        when(resumeRepository.findByUserIdOrderByCreatedAtDesc(testUser.getId())).thenReturn(Collections.emptyList());
        when(resumeRepository.save(any())).thenAnswer(invocation -> {
            com.prepgpt.entity.Resume r = invocation.getArgument(0);
            r.setId(1L);
            return r;
        });

        String mockAiResponse = """
                {
                  "summary": "Full stack developer",
                  "skills": ["Java", "React"],
                  "experience": [],
                  "education": [],
                  "projects": [
                    {
                      "title": "PrepGPT Project",
                      "description": "Mock interview platform",
                      "technologies": "Java, React",
                      "roleContribution": "Backend"
                    }
                  ],
                  "certifications": []
                }
                """;

        when(resumeAnalysisPromptBuilder.buildResumeAnalysisPrompt(any())).thenReturn("prompt");
        when(groqAIService.callGroqChatApi(any())).thenReturn(mockAiResponse);

        ResumeResponse response = resumeAnalysisService.uploadAndAnalyzeResume(testUser.getEmail(), file);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("resume.pdf", response.getFileName());
        assertTrue(response.getIsPrimary());

        verify(projectRepository, times(1)).save(any());
    }

    @Test
    void getResumeById_UnauthorizedUser_ThrowsException() {
        when(userRepository.findByEmail("other@example.com")).thenReturn(Optional.of(testUser));
        when(resumeRepository.findByIdAndUserId(99L, testUser.getId())).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                resumeAnalysisService.getResumeById("other@example.com", 99L)
        );
    }
}
