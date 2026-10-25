package com.prepgpt.service;

import com.prepgpt.dto.ProfileUpdateRequest;
import com.prepgpt.dto.StudentProfileResponse;
import com.prepgpt.entity.StudentProfile;
import com.prepgpt.exception.ResourceNotFoundException;
import com.prepgpt.repository.StudentProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StudentProfileService {

    private final StudentProfileRepository studentProfileRepository;

    public StudentProfileService(StudentProfileRepository studentProfileRepository) {
        this.studentProfileRepository = studentProfileRepository;
    }

    @Transactional(readOnly = true)
    public StudentProfileResponse getProfileByEmail(String email) {
        StudentProfile profile = studentProfileRepository.findByUserEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("StudentProfile", "user email", email));

        return mapToResponse(profile);
    }

    @Transactional
    public StudentProfileResponse updateProfile(String email, ProfileUpdateRequest request) {
        StudentProfile profile = studentProfileRepository.findByUserEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("StudentProfile", "user email", email));

        if (request.getBio() != null) profile.setBio(request.getBio());
        if (request.getTargetRole() != null) profile.setTargetRole(request.getTargetRole());
        if (request.getExperienceLevel() != null) profile.setExperienceLevel(request.getExperienceLevel());
        if (request.getPhone() != null) profile.setPhone(request.getPhone());
        if (request.getGithubUrl() != null) profile.setGithubUrl(request.getGithubUrl());
        if (request.getLinkedinUrl() != null) profile.setLinkedinUrl(request.getLinkedinUrl());

        StudentProfile updatedProfile = studentProfileRepository.save(profile);
        return mapToResponse(updatedProfile);
    }

    private StudentProfileResponse mapToResponse(StudentProfile profile) {
        return new StudentProfileResponse(
                profile.getId(),
                profile.getUser().getId(),
                profile.getUser().getFullName(),
                profile.getUser().getEmail(),
                profile.getUser().getRole().getName(),
                profile.getBio(),
                profile.getTargetRole(),
                profile.getExperienceLevel(),
                profile.getPhone(),
                profile.getGithubUrl(),
                profile.getLinkedinUrl()
        );
    }
}
