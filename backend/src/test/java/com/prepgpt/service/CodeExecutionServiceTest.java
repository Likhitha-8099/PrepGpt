package com.prepgpt.service;

import com.prepgpt.entity.CodingTestCase;
import com.prepgpt.exception.CodeExecutionUnavailableException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CodeExecutionServiceTest {

    private CodeExecutionService codeExecutionService;

    @BeforeEach
    void setUp() {
        codeExecutionService = new CodeExecutionService();
    }

    @Test
    @DisplayName("isDockerAvailable returns boolean status without crashing")
    void testIsDockerAvailable() {
        assertDoesNotThrow(() -> {
            boolean available = codeExecutionService.isDockerAvailable();
            System.out.println("Docker available in test env: " + available);
        });
    }

    @Test
    @DisplayName("Throws CodeExecutionUnavailableException when Docker daemon is not accessible")
    void testExecutionThrowsUnavailableWhenDockerOffline() {
        // If Docker is not available, executeCode must throw CodeExecutionUnavailableException (HTTP 503)
        // and NEVER fall back to host ProcessBuilder execution
        if (!codeExecutionService.isDockerAvailable()) {
            CodingTestCase testCase = new CodingTestCase(null, "2 7\n9", "0 1", false, "Test", 1);
            assertThrows(CodeExecutionUnavailableException.class, () -> {
                codeExecutionService.executeCode("JAVA", "class Solution {}", List.of(testCase), 3000);
            });
        }
    }
}
