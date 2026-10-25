package com.prepgpt.service;

import com.prepgpt.dto.*;
import com.prepgpt.entity.SessionStatus;
import com.prepgpt.entity.User;
import com.prepgpt.exception.ResourceNotFoundException;
import com.prepgpt.repository.InterviewSessionRepository;
import com.prepgpt.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class DashboardService {

    private final UserRepository userRepository;
    private final StudentProfileService profileService;
    private final InterviewSessionRepository sessionRepository;
    private final InterviewSessionService sessionService;

    public DashboardService(UserRepository userRepository,
                            StudentProfileService profileService,
                            InterviewSessionRepository sessionRepository,
                            InterviewSessionService sessionService) {
        this.userRepository = userRepository;
        this.profileService = profileService;
        this.sessionRepository = sessionRepository;
        this.sessionService = sessionService;
    }

    @Transactional(readOnly = true)
    public DashboardResponse getDashboardSummary(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", userEmail));

        UserSummaryResponse userSummary = new UserSummaryResponse(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getRole().getName()
        );

        StudentProfileResponse profileSummary = profileService.getProfileByEmail(userEmail);

        long totalCount = sessionRepository.countByUserId(user.getId());
        long completedCount = sessionRepository.countByUserIdAndStatus(user.getId(), SessionStatus.COMPLETED);
        long inProgressCount = sessionRepository.countByUserIdAndStatus(user.getId(), SessionStatus.IN_PROGRESS);
        long configuredCount = sessionRepository.countByUserIdAndStatus(user.getId(), SessionStatus.CONFIGURED);

        DashboardStatisticsResponse statistics = new DashboardStatisticsResponse(
                totalCount,
                completedCount,
                inProgressCount,
                configuredCount
        );

        List<InterviewSessionResponse> recentSessions = sessionRepository
                .findTop5ByUserIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(sessionService::mapToResponse)
                .collect(Collectors.toList());

        return new DashboardResponse(
                userSummary,
                profileSummary,
                statistics,
                recentSessions
        );
    }
}
