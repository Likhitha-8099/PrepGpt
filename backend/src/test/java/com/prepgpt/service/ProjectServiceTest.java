package com.prepgpt.service;

import com.prepgpt.ai.GroqAIService;
import com.prepgpt.ai.prompt.ProjectAnalysisPromptBuilder;
import com.prepgpt.dto.CreateProjectRequest;
import com.prepgpt.dto.ProjectResponse;
import com.prepgpt.entity.Difficulty;
import com.prepgpt.entity.Project;
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

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProjectServiceTest {

    @Mock
    private ProjectRepository projectRepository;
    @Mock
    private ResumeRepository resumeRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ProjectAnalysisPromptBuilder projectAnalysisPromptBuilder;
    @Mock
    private GroqAIService groqAIService;

    private ProjectService projectService;
    private User testUser;

    @BeforeEach
    void setUp() {
        projectService = new ProjectService(
                projectRepository, resumeRepository, userRepository,
                projectAnalysisPromptBuilder, groqAIService
        );

        Role studentRole = new Role("ROLE_STUDENT");
        testUser = new User("candidate@example.com", "hash", "Test Candidate", studentRole);
        testUser.setId(10L);
    }

    @Test
    void createProject_Success() {
        CreateProjectRequest request = new CreateProjectRequest(
                "Distributed Cache System", "High performance key-value store", "Java, Netty, Redis", "Backend Lead"
        );

        when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));
        when(projectRepository.save(any())).thenAnswer(invocation -> {
            Project p = invocation.getArgument(0);
            p.setId(5L);
            return p;
        });

        ProjectResponse response = projectService.createProject(testUser.getEmail(), request);

        assertNotNull(response);
        assertEquals(5L, response.getId());
        assertEquals("Distributed Cache System", response.getTitle());
        assertEquals(Difficulty.MEDIUM, response.getDifficulty());
    }

    @Test
    void getProjectById_UnauthorizedAccess_ThrowsException() {
        when(userRepository.findByEmail("other@example.com")).thenReturn(Optional.of(testUser));
        when(projectRepository.findByIdAndUserId(99L, testUser.getId())).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                projectService.getProjectById("other@example.com", 99L)
        );
    }
}
