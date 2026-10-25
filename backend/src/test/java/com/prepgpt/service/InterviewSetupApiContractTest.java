package com.prepgpt.service;

import com.prepgpt.controller.JobDescriptionController;
import com.prepgpt.controller.ProjectController;
import com.prepgpt.dto.ProjectResponse;
import com.prepgpt.entity.JobDescription;
import com.prepgpt.entity.Role;
import com.prepgpt.entity.User;
import com.prepgpt.exception.GlobalExceptionHandler;
import com.prepgpt.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Interview Setup API Contract & Ownership Regression Tests")
class InterviewSetupApiContractTest {

    @Mock
    private ProjectService projectService;

    @Mock
    private JobDescriptionAnalysisService jdService;

    @Mock
    private UserRepository userRepository;

    private MockMvc projectMockMvc;
    private MockMvc jdMockMvc;

    private User userA;
    private User userB;
    private Authentication authUserA;
    private Authentication authUserB;

    @BeforeEach
    void setUp() {
        ProjectController projectController = new ProjectController(projectService);
        JobDescriptionController jdController = new JobDescriptionController(jdService, userRepository);

        GlobalExceptionHandler exceptionHandler = new GlobalExceptionHandler();

        projectMockMvc = MockMvcBuilders.standaloneSetup(projectController)
                .setControllerAdvice(exceptionHandler)
                .build();

        jdMockMvc = MockMvcBuilders.standaloneSetup(jdController)
                .setControllerAdvice(exceptionHandler)
                .build();

        Role studentRole = new Role("ROLE_STUDENT");

        userA = new User("userA@example.com", "hashA", "User Alpha", studentRole);
        userA.setId(101L);

        userB = new User("userB@example.com", "hashB", "User Beta", studentRole);
        userB.setId(202L);

        authUserA = new UsernamePasswordAuthenticationToken(userA.getEmail(), null, Collections.emptyList());
        authUserB = new UsernamePasswordAuthenticationToken(userB.getEmail(), null, Collections.emptyList());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 1. CANONICAL PROJECT ENDPOINT CONTRACT & OWNERSHIP
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Canonical GET /api/v1/projects: Authenticated User A retrieves only User A projects")
    void getUserProjects_authenticatedUserA_returnsOwnProjects() throws Exception {
        ProjectResponse projectA = new ProjectResponse();
        projectA.setId(1L);
        projectA.setTitle("Alpha Cloud Pipeline");
        projectA.setDescription("Data ingestion pipeline");
        projectA.setTechnologies("Java, AWS, Kafka");
        projectA.setRoleContribution("Lead");

        when(projectService.getUserProjects("userA@example.com")).thenReturn(List.of(projectA));

        projectMockMvc.perform(get("/api/v1/projects")
                        .principal(authUserA)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is(1)))
                .andExpect(jsonPath("$[0].title", is("Alpha Cloud Pipeline")));

        verify(projectService, times(1)).getUserProjects("userA@example.com");
        verify(projectService, never()).getUserProjects("userB@example.com");
    }

    @Test
    @DisplayName("Canonical GET /api/v1/projects: Authenticated User with no projects returns HTTP 200 with []")
    void getUserProjects_authenticatedUserEmpty_returnsEmptyArray() throws Exception {
        when(projectService.getUserProjects("userA@example.com")).thenReturn(Collections.emptyList());

        projectMockMvc.perform(get("/api/v1/projects")
                        .principal(authUserA)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @DisplayName("Project Ownership Isolation: User A cannot retrieve User B's projects")
    void getUserProjects_ownershipIsolation_userCannotAccessOtherUserData() throws Exception {
        ProjectResponse projectB = new ProjectResponse();
        projectB.setId(2L);
        projectB.setTitle("Beta Microservices");
        projectB.setDescription("Auth service");
        projectB.setTechnologies("Go, Docker");
        projectB.setRoleContribution("Developer");

        when(projectService.getUserProjects("userB@example.com")).thenReturn(List.of(projectB));
        when(projectService.getUserProjects("userA@example.com")).thenReturn(Collections.emptyList());

        // User A request returns empty list, never projectB
        projectMockMvc.perform(get("/api/v1/projects")
                        .principal(authUserA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        // User B request returns only projectB
        projectMockMvc.perform(get("/api/v1/projects")
                        .principal(authUserB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title", is("Beta Microservices")));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 2. CANONICAL JOB DESCRIPTION ENDPOINT CONTRACT & OWNERSHIP
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Canonical GET /api/v1/job-descriptions: Authenticated User A retrieves only User A JDs")
    void getUserJobDescriptions_authenticatedUserA_returnsOwnJds() throws Exception {
        JobDescription jdA = new JobDescription();
        jdA.setId(10L);
        jdA.setUser(userA);
        jdA.setTargetRole("Backend Engineer");
        jdA.setCompanyName("Acme Corp");
        jdA.setRawText("We are seeking a Backend Engineer with Java and Spring experience.");
        jdA.setKeyResponsibilitiesJson("[\"Build REST APIs\"]");
        jdA.setExtractedSkillsJson("[\"Java\", \"Spring\"]");

        when(userRepository.findByEmail("userA@example.com")).thenReturn(Optional.of(userA));
        when(jdService.getAllJobDescriptionsForUser(101L)).thenReturn(List.of(jdA));

        jdMockMvc.perform(get("/api/v1/job-descriptions")
                        .principal(authUserA)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is(10)))
                .andExpect(jsonPath("$[0].targetRole", is("Backend Engineer")))
                .andExpect(jsonPath("$[0].companyName", is("Acme Corp")));

        verify(jdService, times(1)).getAllJobDescriptionsForUser(101L);
        verify(jdService, never()).getAllJobDescriptionsForUser(202L);
    }

    @Test
    @DisplayName("Canonical GET /api/v1/job-descriptions: Authenticated User with no JDs returns HTTP 200 with []")
    void getUserJobDescriptions_authenticatedUserEmpty_returnsEmptyArray() throws Exception {
        when(userRepository.findByEmail("userA@example.com")).thenReturn(Optional.of(userA));
        when(jdService.getAllJobDescriptionsForUser(101L)).thenReturn(Collections.emptyList());

        jdMockMvc.perform(get("/api/v1/job-descriptions")
                        .principal(authUserA)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @DisplayName("Job Description Ownership Isolation: User A cannot retrieve User B's JDs")
    void getUserJobDescriptions_ownershipIsolation_userCannotAccessOtherUserData() throws Exception {
        JobDescription jdB = new JobDescription();
        jdB.setId(20L);
        jdB.setUser(userB);
        jdB.setTargetRole("Frontend Engineer");
        jdB.setCompanyName("Beta Tech");
        jdB.setRawText("Frontend developer needed.");

        when(userRepository.findByEmail("userB@example.com")).thenReturn(Optional.of(userB));
        when(jdService.getAllJobDescriptionsForUser(202L)).thenReturn(List.of(jdB));

        when(userRepository.findByEmail("userA@example.com")).thenReturn(Optional.of(userA));
        when(jdService.getAllJobDescriptionsForUser(101L)).thenReturn(Collections.emptyList());

        // User A query returns 0
        jdMockMvc.perform(get("/api/v1/job-descriptions").principal(authUserA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        // User B query returns 1 (User B's JD only)
        jdMockMvc.perform(get("/api/v1/job-descriptions").principal(authUserB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].targetRole", is("Frontend Engineer")));
    }
}
