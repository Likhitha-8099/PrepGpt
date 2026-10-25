package com.prepgpt.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.prepgpt.ai.GroqAIService;
import com.prepgpt.ai.prompt.ResumeBuilderPromptBuilder;
import com.prepgpt.dto.*;
import com.prepgpt.entity.Resume;
import com.prepgpt.entity.Role;
import com.prepgpt.entity.StudentProfile;
import com.prepgpt.entity.User;
import com.prepgpt.exception.BadRequestException;
import com.prepgpt.repository.ProjectRepository;
import com.prepgpt.repository.ResumeRepository;
import com.prepgpt.repository.StudentProfileRepository;
import com.prepgpt.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ResumeBuilderServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private StudentProfileRepository profileRepository;
    @Mock
    private ResumeRepository resumeRepository;
    @Mock
    private ProjectRepository projectRepository;
    @Mock
    private GroqAIService groqAIService;

    private ResumeBuilderPromptBuilder promptBuilder;
    private ObjectMapper objectMapper;
    private ResumeBuilderService resumeBuilderService;
    private User testUser;

    @BeforeEach
    void setUp() {
        promptBuilder = new ResumeBuilderPromptBuilder();
        objectMapper = new ObjectMapper();
        resumeBuilderService = new ResumeBuilderService(
                userRepository, profileRepository, resumeRepository, projectRepository,
                groqAIService, promptBuilder, objectMapper, "target/test-uploads"
        );

        Role role = new Role("ROLE_STUDENT");
        testUser = new User("candidate@prepgpt.ai", "secretHash", "Jane Doe", role);
        testUser.setId(42L);
    }

    @Test
    void getInitialData_Success() {
        when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));

        StudentProfile profile = new StudentProfile(testUser);
        profile.setTargetRole("Backend Engineer");
        profile.setPhone("555-0199");
        profile.setBio("Passionate developer");
        when(profileRepository.findByUserId(testUser.getId())).thenReturn(Optional.of(profile));

        when(resumeRepository.findByUserIdOrderByCreatedAtDesc(testUser.getId())).thenReturn(Collections.emptyList());
        when(projectRepository.findByUserIdOrderByCreatedAtDesc(testUser.getId())).thenReturn(Collections.emptyList());

        ResumeBuilderInitialDataResponse response = resumeBuilderService.getInitialData(testUser.getEmail());

        assertNotNull(response);
        assertEquals("Jane Doe", response.getFullName());
        assertEquals("candidate@prepgpt.ai", response.getEmail());
        assertEquals("Backend Engineer", response.getTargetRole());
        assertEquals("555-0199", response.getPhone());
    }

    @Test
    void processAiAssist_ImproveSummary_GroqSuccess() {
        when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));

        String mockJsonResponse = """
                {
                  "improvedText": "Results-oriented Backend Engineer with expertise in Java and Spring Boot.",
                  "suggestions": ["Alternative variation 1", "Alternative variation 2"]
                }
                """;
        when(groqAIService.callGroqChatApi(any())).thenReturn(mockJsonResponse);

        ResumeBuilderAiAssistRequest request = new ResumeBuilderAiAssistRequest();
        request.setAction("IMPROVE_SUMMARY");
        request.setSummary("I am a backend engineer doing java.");
        request.setTargetRole("Backend Engineer");

        ResumeBuilderAiAssistResponse response = resumeBuilderService.processAiAssist(testUser.getEmail(), request);

        assertNotNull(response);
        assertEquals("IMPROVE_SUMMARY", response.getAction());
        assertEquals("Results-oriented Backend Engineer with expertise in Java and Spring Boot.", response.getImprovedText());
        assertEquals(2, response.getSuggestions().size());
    }

    @Test
    void processAiAssist_FallbackOnGroqError() {
        when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));
        when(groqAIService.callGroqChatApi(any())).thenThrow(new RuntimeException("Groq API rate limit"));

        ResumeBuilderAiAssistRequest request = new ResumeBuilderAiAssistRequest();
        request.setAction("IMPROVE_BULLET");
        request.setBullet("Created database tables.");

        ResumeBuilderAiAssistResponse response = resumeBuilderService.processAiAssist(testUser.getEmail(), request);

        assertNotNull(response);
        assertNotNull(response.getImprovedText());
        assertFalse(response.getSuggestions().isEmpty());
    }

    @Test
    void saveResumeFromBuilder_Success() {
        when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));
        when(resumeRepository.findByUserIdOrderByCreatedAtDesc(testUser.getId())).thenReturn(Collections.emptyList());
        when(resumeRepository.save(any(Resume.class))).thenAnswer(invocation -> {
            Resume r = invocation.getArgument(0);
            r.setId(101L);
            return r;
        });

        ResumeBuilderSaveRequest saveRequest = new ResumeBuilderSaveRequest();
        saveRequest.setFileName("Jane_Doe_ATS_Resume.txt");
        saveRequest.setTemplateId("classic-professional");
        saveRequest.setPlainTextContent("Jane Doe\\nBackend Engineer\\nSummary: Experienced developer.");
        saveRequest.setSkillsJson("[\"Java\", \"Spring Boot\"]");
        saveRequest.setExperienceJson("[]");

        ResumeResponse response = resumeBuilderService.saveResumeFromBuilder(testUser.getEmail(), saveRequest);

        assertNotNull(response);
        assertEquals(101L, response.getId());
        assertEquals("Jane_Doe_ATS_Resume.txt", response.getFileName());
        assertTrue(response.getIsPrimary());
    }

    @Test
    void saveResumeFromBuilder_BlankContent_ThrowsBadRequest() {
        when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));

        ResumeBuilderSaveRequest saveRequest = new ResumeBuilderSaveRequest();
        saveRequest.setPlainTextContent("");

        assertThrows(BadRequestException.class, () ->
                resumeBuilderService.saveResumeFromBuilder(testUser.getEmail(), saveRequest)
        );
    }
}
