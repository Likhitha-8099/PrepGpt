package com.prepgpt.controller;

import com.prepgpt.dto.*;
import com.prepgpt.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * AdminController — all endpoints require ROLE_ADMIN via:
 *  1. @PreAuthorize("hasRole('ADMIN')") per method (method-level)
 *  2. SecurityConfig URL-level rule: /api/v1/admin/** -> hasRole("ADMIN") (defense-in-depth)
 *
 * No DELETE /admin/users/{id} endpoint exists — soft-delete only.
 * No write endpoint for audit logs exists — audit log is immutable through the API.
 * Session oversight is strictly read-only.
 */
@RestController
@RequestMapping("/api/v1/admin")
@Tag(name = "Admin API", description = "Administrative endpoints for platform management. Requires ROLE_ADMIN.")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    // ── PLATFORM STATISTICS ──────────────────────────────────────────────

    @GetMapping("/stats")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get platform statistics",
               description = "Returns aggregate counts and metrics for the entire platform. Uses DB-level aggregate queries only.")
    public ResponseEntity<AdminPlatformStatsResponse> getPlatformStats() {
        return ResponseEntity.ok(adminService.getPlatformStats());
    }

    // ── USER MANAGEMENT ──────────────────────────────────────────────────

    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "List all users (paginated)",
               description = "Returns paginated list of all users with session counts. No passwordHash exposed.")
    public ResponseEntity<AdminUserListResponse> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(adminService.getAllUsers(page, size));
    }

    @GetMapping("/users/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get user by ID", description = "Returns full user detail for admin inspection.")
    public ResponseEntity<AdminUserResponse> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(adminService.getUserById(id));
    }

    @PatchMapping("/users/{id}/deactivate")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Soft-deactivate user",
               description = "Marks user as inactive. Existing JWTs immediately rejected. Preserves all history. Blocks if target is last active admin.")
    public ResponseEntity<Void> deactivateUser(@PathVariable Long id, Authentication authentication) {
        adminService.deactivateUser(authentication.getName(), id);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/users/{id}/reactivate")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Reactivate user",
               description = "Marks user as active again. Previously issued JWTs become valid again immediately.")
    public ResponseEntity<Void> reactivateUser(@PathVariable Long id, Authentication authentication) {
        adminService.reactivateUser(authentication.getName(), id);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/users/{id}/role")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Change user role",
               description = "Changes user role. Only ROLE_STUDENT and ROLE_ADMIN are valid. Cannot change own role. Cannot demote last active admin.")
    public ResponseEntity<AdminUserResponse> changeUserRole(
            @PathVariable Long id,
            @Valid @RequestBody AdminChangeRoleRequest request,
            Authentication authentication) {
        AdminUserResponse response = adminService.changeUserRole(authentication.getName(), id, request.getNewRole());
        return ResponseEntity.ok(response);
    }

    // ── QUESTION MANAGEMENT ──────────────────────────────────────────────

    @GetMapping("/questions")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "List all questions (paginated, incl. inactive)",
               description = "Returns all questions including deactivated ones. Supports optional categoryId and difficulty filters.")
    public ResponseEntity<AdminQuestionListResponse> getAllQuestions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String difficulty) {
        return ResponseEntity.ok(adminService.getAllQuestions(page, size, categoryId, difficulty));
    }

    @PostMapping("/questions")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create question",
               description = "Adds a new question to the question bank. Returns 409 if duplicate text+category exists.")
    public ResponseEntity<AdminQuestionResponse> createQuestion(
            @Valid @RequestBody AdminQuestionRequest request,
            Authentication authentication) {
        AdminQuestionResponse response = adminService.createQuestion(authentication.getName(), request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PutMapping("/questions/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update question",
               description = "Updates an existing question. Returns 409 if new text+category duplicates another question.")
    public ResponseEntity<AdminQuestionResponse> updateQuestion(
            @PathVariable Long id,
            @Valid @RequestBody AdminQuestionRequest request,
            Authentication authentication) {
        AdminQuestionResponse response = adminService.updateQuestion(authentication.getName(), id, request);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/questions/{id}/deactivate")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Soft-deactivate question",
               description = "Sets question active=false. Deactivated questions are not served in new interview sessions.")
    public ResponseEntity<Void> deactivateQuestion(@PathVariable Long id, Authentication authentication) {
        adminService.deactivateQuestion(authentication.getName(), id);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/questions/{id}/reactivate")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Reactivate question",
               description = "Sets question active=true. Question becomes eligible for new interview sessions.")
    public ResponseEntity<Void> reactivateQuestion(@PathVariable Long id, Authentication authentication) {
        adminService.reactivateQuestion(authentication.getName(), id);
        return ResponseEntity.ok().build();
    }

    // ── SESSION OVERSIGHT (READ-ONLY) ────────────────────────────────────

    @GetMapping("/sessions")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Browse all interview sessions (read-only)",
               description = "Cross-user session browsing with optional userId and status filters. No write capability.")
    public ResponseEntity<AdminSessionSummaryListResponse> getAllSessions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String status) {
        return ResponseEntity.ok(adminService.getAllSessions(page, size, userId, status));
    }

    // ── AUDIT LOG (READ-ONLY) ────────────────────────────────────────────

    @GetMapping("/audit-log")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get audit log (paginated)",
               description = "Returns paginated admin action audit log. Max page size 100. Immutable — no create/update/delete endpoints.")
    public ResponseEntity<AdminAuditLogListResponse> getAuditLog(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(adminService.getAuditLog(page, size));
    }
}
