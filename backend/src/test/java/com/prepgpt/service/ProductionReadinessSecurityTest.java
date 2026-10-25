package com.prepgpt.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.prepgpt.config.CorsConfig;
import com.prepgpt.controller.HealthController;
import com.prepgpt.dto.ErrorResponse;
import com.prepgpt.dto.HealthCheckResponse;
import com.prepgpt.exception.BadRequestException;
import com.prepgpt.exception.GlobalExceptionHandler;
import com.prepgpt.repository.*;
import com.prepgpt.security.RateLimitingFilter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.servlet.config.annotation.CorsRegistration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Phase 13: Production Readiness & Security Hardening Unit Tests")
class ProductionReadinessSecurityTest {

    @Mock private DataSource dataSource;
    @Mock private Connection connection;

    @Mock private UserRepository userRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private InterviewSessionRepository sessionRepository;
    @Mock private InterviewEvaluationRepository evaluationRepository;
    @Mock private QuestionRepository questionRepository;
    @Mock private CodingProblemRepository codingProblemRepository;
    @Mock private JobDescriptionRepository jobDescriptionRepository;
    @Mock private ResumeRepository resumeRepository;
    @Mock private InterviewCategoryRepository categoryRepository;
    @Mock private AdminAuditLogRepository auditLogRepository;

    private AdminService adminService;
    private HealthController healthController;
    private GlobalExceptionHandler globalExceptionHandler;
    private RateLimitingFilter rateLimitingFilter;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        adminService = new AdminService(
                userRepository, roleRepository, sessionRepository, evaluationRepository,
                questionRepository, codingProblemRepository, jobDescriptionRepository,
                resumeRepository, categoryRepository, auditLogRepository
        );
        healthController = new HealthController(dataSource);
        globalExceptionHandler = new GlobalExceptionHandler();
        objectMapper = new ObjectMapper().registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
        rateLimitingFilter = new RateLimitingFilter(objectMapper);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 1. CORS CONFIGURATION: PATCH METHOD & ORIGINS
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("CORS: PATCH method and origins properly registered in CorsConfig")
    @SuppressWarnings("unchecked")
    void corsConfig_allowsPatchAndCustomOrigins() {
        CorsConfig corsConfig = new CorsConfig();
        ReflectionTestUtils.setField(corsConfig, "allowedOrigins", "http://localhost:5173,http://127.0.0.1:5173");

        WebMvcConfigurer configurer = corsConfig.corsConfigurer();
        assertThat(configurer).isNotNull();

        CorsRegistry registry = new CorsRegistry();
        configurer.addCorsMappings(registry);

        List<CorsRegistration> registrations = (List<CorsRegistration>) ReflectionTestUtils.getField(registry, "registrations");
        assertThat(registrations).isNotNull().isNotEmpty();

        CorsRegistration registration = registrations.get(0);
        org.springframework.web.cors.CorsConfiguration corsConfiguration =
                (org.springframework.web.cors.CorsConfiguration) ReflectionTestUtils.getField(registration, "config");
        assertThat(corsConfiguration).isNotNull();

        assertThat(corsConfiguration.getAllowedMethods()).contains("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS");
        assertThat(corsConfiguration.getAllowedOrigins()).containsExactlyInAnyOrder("http://localhost:5173", "http://127.0.0.1:5173");
        assertThat(corsConfiguration.getAllowCredentials()).isTrue();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 2. ERROR MESSAGE SANITIZATION (500 INTERNAL SERVER ERROR)
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("500 Error Sanitization: Raw SQL and stack details are hidden from response")
    void handleGenericException_sanitizesErrorMessage() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/test");
        Exception ex = new SQLException("Table 'prepgpt.users_credentials' doesn't exist at syntax near SELECT *");

        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleGenericException(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo("An unexpected internal error occurred. Please try again later.");
        assertThat(response.getBody().getMessage()).doesNotContain("prepgpt.users_credentials");
        assertThat(response.getBody().getMessage()).doesNotContain("syntax");
        assertThat(response.getBody().getMessage()).doesNotContain("SQLException");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 3. HEALTH CHECK DATABASE PROBE
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Health Check: Valid DB connection returns 200 UP")
    void healthCheck_validConnection_returns200Up() throws Exception {
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.isValid(2)).thenReturn(true);

        ResponseEntity<HealthCheckResponse> response = healthController.getHealth();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo("UP");
        assertThat(response.getBody().getApplication()).isEqualTo("PrepGPT");
    }

    @Test
    @DisplayName("Health Check: Invalid DB connection returns 503 DOWN")
    void healthCheck_invalidConnection_returns503Down() throws Exception {
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.isValid(2)).thenReturn(false);

        ResponseEntity<HealthCheckResponse> response = healthController.getHealth();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo("DOWN");
    }

    @Test
    @DisplayName("Health Check: Unreachable DB exception returns 503 DOWN")
    void healthCheck_connectionException_returns503Down() throws Exception {
        when(dataSource.getConnection()).thenThrow(new SQLException("Connection refused"));

        ResponseEntity<HealthCheckResponse> response = healthController.getHealth();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo("DOWN");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 4. RATE LIMITING FILTER & TEST ISOLATION
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Rate Limiting: Auth endpoint enforces 10 requests/min and returns 429 with Retry-After")
    void rateLimitingFilter_authEndpoint_blocksAfterLimit() throws Exception {
        String testIp = "192.168.10.1";

        // First 10 requests should pass
        for (int i = 0; i < 10; i++) {
            MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/login");
            request.setRemoteAddr(testIp);
            MockHttpServletResponse response = new MockHttpServletResponse();
            MockFilterChain chain = new MockFilterChain();

            rateLimitingFilter.doFilter(request, response, chain);
            assertThat(response.getStatus()).isEqualTo(HttpStatus.OK.value());
        }

        // 11th request must be blocked with HTTP 429
        MockHttpServletRequest blockedRequest = new MockHttpServletRequest("POST", "/api/v1/auth/login");
        blockedRequest.setRemoteAddr(testIp);
        MockHttpServletResponse blockedResponse = new MockHttpServletResponse();
        MockFilterChain blockedChain = new MockFilterChain();

        rateLimitingFilter.doFilter(blockedRequest, blockedResponse, blockedChain);

        assertThat(blockedResponse.getStatus()).isEqualTo(429);
        assertThat(blockedResponse.getHeader("Retry-After")).isNotNull();
        assertThat(blockedResponse.getContentAsString()).contains("Rate limit exceeded");
    }

    @Test
    @DisplayName("Rate Limiting Isolation: Client B is unaffected by Client A's exhausted quota")
    void rateLimitingFilter_clientIsolation() throws Exception {
        String clientA_ip = "192.168.10.2";
        String clientB_ip = "192.168.10.3";

        // Exhaust Client A's quota (10 requests)
        for (int i = 0; i < 10; i++) {
            MockHttpServletRequest reqA = new MockHttpServletRequest("POST", "/api/v1/auth/login");
            reqA.setRemoteAddr(clientA_ip);
            rateLimitingFilter.doFilter(reqA, new MockHttpServletResponse(), new MockFilterChain());
        }

        // Verify Client A is blocked
        MockHttpServletRequest reqABlocked = new MockHttpServletRequest("POST", "/api/v1/auth/login");
        reqABlocked.setRemoteAddr(clientA_ip);
        MockHttpServletResponse resABlocked = new MockHttpServletResponse();
        rateLimitingFilter.doFilter(reqABlocked, resABlocked, new MockFilterChain());
        assertThat(resABlocked.getStatus()).isEqualTo(429);

        // Client B from different IP must successfully pass
        MockHttpServletRequest reqB = new MockHttpServletRequest("POST", "/api/v1/auth/login");
        reqB.setRemoteAddr(clientB_ip);
        MockHttpServletResponse resB = new MockHttpServletResponse();
        rateLimitingFilter.doFilter(reqB, resB, new MockFilterChain());
        assertThat(resB.getStatus()).isEqualTo(HttpStatus.OK.value());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 5. STRICT PAGINATION BOUNDARY VALIDATION
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Admin Pagination: getAllUsers rejects page < 0, size < 1, and size > 100")
    void getAllUsers_paginationValidation() {
        assertThatThrownBy(() -> adminService.getAllUsers(-1, 10))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Page index cannot be negative");

        assertThatThrownBy(() -> adminService.getAllUsers(0, 0))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Page size must be at least 1");

        assertThatThrownBy(() -> adminService.getAllUsers(0, 101))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Page size cannot exceed 100");
    }

    @Test
    @DisplayName("Admin Pagination: getAllQuestions rejects invalid page/size parameters")
    void getAllQuestions_paginationValidation() {
        assertThatThrownBy(() -> adminService.getAllQuestions(-1, 20, null, null))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Page index cannot be negative");

        assertThatThrownBy(() -> adminService.getAllQuestions(0, 0, null, null))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Page size must be at least 1");

        assertThatThrownBy(() -> adminService.getAllQuestions(0, 105, null, null))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Page size cannot exceed 100");
    }

    @Test
    @DisplayName("Admin Pagination: getAllSessions rejects invalid page/size parameters")
    void getAllSessions_paginationValidation() {
        assertThatThrownBy(() -> adminService.getAllSessions(-2, 20, null, null))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Page index cannot be negative");

        assertThatThrownBy(() -> adminService.getAllSessions(0, 0, null, null))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Page size must be at least 1");

        assertThatThrownBy(() -> adminService.getAllSessions(0, 200, null, null))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Page size cannot exceed 100");
    }

    @Test
    @DisplayName("Admin Pagination: getAuditLog rejects invalid page/size parameters")
    void getAuditLog_paginationValidation() {
        assertThatThrownBy(() -> adminService.getAuditLog(-1, 20))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Page index cannot be negative");

        assertThatThrownBy(() -> adminService.getAuditLog(0, 0))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Page size must be at least 1");

        assertThatThrownBy(() -> adminService.getAuditLog(0, 101))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Page size cannot exceed 100");
    }
}
