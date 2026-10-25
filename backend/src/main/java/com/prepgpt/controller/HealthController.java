package com.prepgpt.controller;

import com.prepgpt.dto.HealthCheckResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;

@RestController
@RequestMapping("/api/v1/health")
@Tag(name = "Health API", description = "Endpoints for application health status check")
public class HealthController {

    private final DataSource dataSource;

    public HealthController(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @GetMapping
    @Operation(summary = "Check Backend Health Status", description = "Returns UP status if backend and database connectivity are healthy")
    public ResponseEntity<HealthCheckResponse> getHealth() {
        try (Connection connection = dataSource.getConnection()) {
            if (connection.isValid(2)) {
                return ResponseEntity.ok(new HealthCheckResponse("UP", "PrepGPT"));
            } else {
                return ResponseEntity.status(org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE)
                        .body(new HealthCheckResponse("DOWN", "Database validation check failed"));
            }
        } catch (Exception e) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE)
                    .body(new HealthCheckResponse("DOWN", "Database connection unreachable"));
        }
    }
}
