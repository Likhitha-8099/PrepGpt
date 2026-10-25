package com.prepgpt.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.prepgpt.controller.ResumeBuilderController;
import com.prepgpt.dto.*;
import com.prepgpt.exception.GlobalExceptionHandler;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ATS Resume Builder Controller API Contract Tests")
class ResumeBuilderControllerTest {

    @Mock
    private ResumeBuilderService resumeBuilderService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private Authentication authUser;

    @BeforeEach
    void setUp() {
        ResumeBuilderController controller = new ResumeBuilderController(resumeBuilderService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        objectMapper = new ObjectMapper();
        authUser = new UsernamePasswordAuthenticationToken("candidate@prepgpt.ai", null, Collections.emptyList());
    }

    @Test
    void getInitialData_ReturnsCandidateData() throws Exception {
        ResumeBuilderInitialDataResponse response = new ResumeBuilderInitialDataResponse();
        response.setFullName("John Doe");
        response.setEmail("candidate@prepgpt.ai");
        response.setTargetRole("Full Stack Engineer");

        when(resumeBuilderService.getInitialData("candidate@prepgpt.ai")).thenReturn(response);

        mockMvc.perform(get("/api/v1/resume-builder/initial-data")
                        .principal(authUser))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("John Doe"))
                .andExpect(jsonPath("$.email").value("candidate@prepgpt.ai"))
                .andExpect(jsonPath("$.targetRole").value("Full Stack Engineer"));
    }

    @Test
    void processAiAssist_ValidRequest_ReturnsImprovedContent() throws Exception {
        ResumeBuilderAiAssistRequest request = new ResumeBuilderAiAssistRequest();
        request.setAction("IMPROVE_SUMMARY");
        request.setSummary("Backend dev with java.");

        ResumeBuilderAiAssistResponse response = new ResumeBuilderAiAssistResponse();
        response.setAction("IMPROVE_SUMMARY");
        response.setImprovedText("High impact backend engineer specializing in distributed systems.");
        response.setSuggestions(List.of("Option A", "Option B"));

        when(resumeBuilderService.processAiAssist(eq("candidate@prepgpt.ai"), any(ResumeBuilderAiAssistRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/resume-builder/ai-assist")
                        .principal(authUser)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.action").value("IMPROVE_SUMMARY"))
                .andExpect(jsonPath("$.improvedText").value("High impact backend engineer specializing in distributed systems."))
                .andExpect(jsonPath("$.suggestions.length()").value(2));
    }

    @Test
    void saveResumeFromBuilder_ValidPayload_ReturnsCreatedResume() throws Exception {
        ResumeBuilderSaveRequest request = new ResumeBuilderSaveRequest();
        request.setFileName("Test_Resume.txt");
        request.setPlainTextContent("John Doe\nSummary: Software Engineer");

        ResumeResponse resumeResponse = new ResumeResponse();
        resumeResponse.setId(205L);
        resumeResponse.setFileName("Test_Resume.txt");
        resumeResponse.setIsPrimary(true);

        when(resumeBuilderService.saveResumeFromBuilder(eq("candidate@prepgpt.ai"), any(ResumeBuilderSaveRequest.class)))
                .thenReturn(resumeResponse);

        mockMvc.perform(post("/api/v1/resume-builder/save-to-resumes")
                        .principal(authUser)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(205))
                .andExpect(jsonPath("$.fileName").value("Test_Resume.txt"));
    }
}
