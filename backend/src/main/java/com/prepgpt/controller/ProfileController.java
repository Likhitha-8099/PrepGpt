package com.prepgpt.controller;

import com.prepgpt.dto.ProfileUpdateRequest;
import com.prepgpt.dto.StudentProfileResponse;
import com.prepgpt.service.StudentProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/profile")
@Tag(name = "Student Profile API", description = "Endpoints for managing student profile details")
public class ProfileController {

    private final StudentProfileService profileService;

    public ProfileController(StudentProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping
    @Operation(summary = "Get current student profile", description = "Fetches the profile details of the authenticated student")
    public ResponseEntity<StudentProfileResponse> getProfile(Authentication authentication) {
        StudentProfileResponse response = profileService.getProfileByEmail(authentication.getName());
        return ResponseEntity.ok(response);
    }

    @PutMapping
    @Operation(summary = "Update student profile", description = "Updates profile fields such as bio, target role, experience, and links")
    public ResponseEntity<StudentProfileResponse> updateProfile(
            Authentication authentication,
            @RequestBody ProfileUpdateRequest updateRequest) {
        StudentProfileResponse response = profileService.updateProfile(authentication.getName(), updateRequest);
        return ResponseEntity.ok(response);
    }
}
