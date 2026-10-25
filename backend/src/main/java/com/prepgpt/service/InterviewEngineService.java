package com.prepgpt.service;

import com.prepgpt.dto.*;
import com.prepgpt.entity.*;
import com.prepgpt.exception.BadRequestException;
import com.prepgpt.exception.ResourceNotFoundException;
import com.prepgpt.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class InterviewEngineService {

    private final InterviewSessionRepository sessionRepository;
    private final QuestionRepository questionRepository;
    private final InterviewQuestionRepository interviewQuestionRepository;
    private final InterviewResponseRepository interviewResponseRepository;
    private final UserRepository userRepository;

    public InterviewEngineService(InterviewSessionRepository sessionRepository,
                                  QuestionRepository questionRepository,
                                  InterviewQuestionRepository interviewQuestionRepository,
                                  InterviewResponseRepository interviewResponseRepository,
                                  UserRepository userRepository) {
        this.sessionRepository = sessionRepository;
        this.questionRepository = questionRepository;
        this.interviewQuestionRepository = interviewQuestionRepository;
        this.interviewResponseRepository = interviewResponseRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public InterviewSessionResponse startSession(String userEmail, Long sessionId) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", userEmail));

        InterviewSession session = sessionRepository.findByIdAndUserId(sessionId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("InterviewSession not found with id " + sessionId + " for current user"));

        if (session.getStatus() == SessionStatus.COMPLETED) {
            throw new BadRequestException("Completed interview sessions cannot be restarted");
        }
        if (session.getStatus() == SessionStatus.ABANDONED) {
            throw new BadRequestException("Abandoned interview sessions cannot be restarted");
        }

        // Idempotency: If session is already IN_PROGRESS, do not re-assign questions
        if (session.getStatus() == SessionStatus.IN_PROGRESS) {
            return mapToSessionResponse(session);
        }

        // 1. Fetch eligible questions for category and difficulty
        List<Question> eligibleQuestions = questionRepository
                .findByCategoryIdAndDifficultyAndActiveTrue(session.getCategory().getId(), session.getDifficulty());

        if (eligibleQuestions.size() < session.getQuestionCount()) {
            eligibleQuestions = questionRepository.findByCategoryIdAndActiveTrue(session.getCategory().getId());
        }

        if (eligibleQuestions.size() < session.getQuestionCount()) {
            throw new BadRequestException("Not enough questions available for this category and difficulty.");
        }

        // 2. Randomize question selection
        List<Question> shuffled = new ArrayList<>(eligibleQuestions);
        Collections.shuffle(shuffled);
        List<Question> selectedQuestions = shuffled.subList(0, session.getQuestionCount());

        // 3. Assign questions with stable questionOrder starting from 1
        int order = 1;
        for (Question q : selectedQuestions) {
            InterviewQuestion iq = new InterviewQuestion(session, q, order++);
            interviewQuestionRepository.save(iq);
        }

        // 4. Update session status
        session.setStatus(SessionStatus.IN_PROGRESS);
        session.setStartedAt(LocalDateTime.now());
        InterviewSession updatedSession = sessionRepository.save(session);

        return mapToSessionResponse(updatedSession);
    }

    @Transactional
    public QuestionResponse getCurrentQuestion(String userEmail, Long sessionId) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", userEmail));

        InterviewSession session = sessionRepository.findByIdAndUserId(sessionId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("InterviewSession not found with id " + sessionId + " for current user"));

        if (session.getStatus() == SessionStatus.CONFIGURED) {
            throw new BadRequestException("Interview session must be started first.");
        }

        List<InterviewQuestion> assignedQuestions = interviewQuestionRepository
                .findByInterviewSessionIdOrderByQuestionOrderAsc(sessionId);

        if (assignedQuestions.isEmpty()) {
            throw new BadRequestException("No questions assigned to this interview session.");
        }

        int totalQuestions = assignedQuestions.size();
        int answeredCount = 0;
        InterviewQuestion currentQuestion = null;

        for (InterviewQuestion iq : assignedQuestions) {
            boolean isAnswered = interviewResponseRepository.existsByInterviewQuestionId(iq.getId());
            if (isAnswered) {
                answeredCount++;
            } else if (currentQuestion == null) {
                currentQuestion = iq;
            }
        }

        int progress = (int) Math.round(((double) answeredCount / totalQuestions) * 100);

        // If all questions are answered and session is IN_PROGRESS, mark COMPLETED
        if (answeredCount == totalQuestions) {
            if (session.getStatus() == SessionStatus.IN_PROGRESS) {
                session.setStatus(SessionStatus.COMPLETED);
                session.setCompletedAt(LocalDateTime.now());
                sessionRepository.save(session);
            }

            // Return last question marked answered
            InterviewQuestion lastQuestion = assignedQuestions.get(totalQuestions - 1);
            return new QuestionResponse(
                    lastQuestion.getId(),
                    lastQuestion.getQuestionOrder(),
                    totalQuestions,
                    lastQuestion.getQuestion().getQuestionText(),
                    lastQuestion.getQuestion().getQuestionType(),
                    lastQuestion.getQuestion().getDifficulty(),
                    true,
                    100
            );
        }

        return new QuestionResponse(
                currentQuestion.getId(),
                currentQuestion.getQuestionOrder(),
                totalQuestions,
                currentQuestion.getQuestion().getQuestionText(),
                currentQuestion.getQuestion().getQuestionType(),
                currentQuestion.getQuestion().getDifficulty(),
                false,
                progress
        );
    }

    @Transactional
    public AnswerResponse submitAnswer(String userEmail, Long sessionId, Long questionId, SubmitAnswerRequest request) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", userEmail));

        InterviewSession session = sessionRepository.findByIdAndUserId(sessionId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("InterviewSession not found with id " + sessionId + " for current user"));

        if (session.getStatus() != SessionStatus.IN_PROGRESS) {
            throw new BadRequestException("Interview session is not currently accepting answers.");
        }

        InterviewQuestion interviewQuestion = interviewQuestionRepository.findById(questionId)
                .orElseThrow(() -> new ResourceNotFoundException("InterviewQuestion", "id", questionId));

        if (!interviewQuestion.getInterviewSession().getId().equals(sessionId)) {
            throw new BadRequestException("Question does not belong to this interview session.");
        }

        if (interviewResponseRepository.existsByInterviewQuestionId(questionId)) {
            throw new BadRequestException("This question has already been answered.");
        }

        String answerText = request.getAnswerText() != null ? request.getAnswerText().trim() : "";
        if (answerText.isEmpty()) {
            throw new BadRequestException("Answer text cannot be empty.");
        }

        InterviewResponse response = new InterviewResponse(interviewQuestion, answerText, request.getInputMethod());
        interviewResponseRepository.save(response);

        // Check if all questions are answered now
        long totalQuestions = interviewQuestionRepository.countByInterviewSessionId(sessionId);
        long answeredQuestions = interviewResponseRepository.countByInterviewQuestionInterviewSessionId(sessionId);

        boolean completed = false;
        if (answeredQuestions >= totalQuestions) {
            session.setStatus(SessionStatus.COMPLETED);
            session.setCompletedAt(LocalDateTime.now());
            sessionRepository.save(session);
            completed = true;
        }

        return new AnswerResponse(true, questionId, true, completed);
    }

    @Transactional(readOnly = true)
    public QuestionResponse getNextQuestion(String userEmail, Long sessionId) {
        return getCurrentQuestion(userEmail, sessionId);
    }

    @Transactional(readOnly = true)
    public InterviewStatusResponse getInterviewStatus(String userEmail, Long sessionId) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", userEmail));

        InterviewSession session = sessionRepository.findByIdAndUserId(sessionId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("InterviewSession not found with id " + sessionId + " for current user"));

        List<InterviewQuestion> assignedQuestions = interviewQuestionRepository
                .findByInterviewSessionIdOrderByQuestionOrderAsc(sessionId);

        int totalQuestions = session.getQuestionCount();
        int answeredQuestions = 0;
        int currentQuestionOrder = 1;

        if (!assignedQuestions.isEmpty()) {
            totalQuestions = assignedQuestions.size();
            for (InterviewQuestion iq : assignedQuestions) {
                if (interviewResponseRepository.existsByInterviewQuestionId(iq.getId())) {
                    answeredQuestions++;
                } else if (currentQuestionOrder == 1 || currentQuestionOrder <= answeredQuestions) {
                    currentQuestionOrder = iq.getQuestionOrder();
                }
            }
            if (answeredQuestions == totalQuestions) {
                currentQuestionOrder = totalQuestions;
            }
        }

        int progress = totalQuestions > 0 ? (int) Math.round(((double) answeredQuestions / totalQuestions) * 100) : 0;

        return new InterviewStatusResponse(
                sessionId,
                session.getStatus(),
                currentQuestionOrder,
                totalQuestions,
                answeredQuestions,
                progress
        );
    }

    @Transactional(readOnly = true)
    public InterviewSessionResponse getSessionDetails(String userEmail, Long sessionId) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", userEmail));

        InterviewSession session = sessionRepository.findByIdAndUserId(sessionId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("InterviewSession not found with id " + sessionId + " for current user"));

        InterviewSessionResponse response = mapToSessionResponse(session);

        List<InterviewQuestion> assignedQuestions = interviewQuestionRepository
                .findByInterviewSessionIdOrderByQuestionOrderAsc(sessionId);

        if (!assignedQuestions.isEmpty()) {
            int totalQuestions = assignedQuestions.size();
            int answeredQuestions = 0;

            List<InterviewQuestionDetailResponse> questionDetails = new ArrayList<>();
            for (InterviewQuestion iq : assignedQuestions) {
                Optional<InterviewResponse> respOpt = interviewResponseRepository.findByInterviewQuestionId(iq.getId());
                String answerText = null;
                LocalDateTime answeredAt = null;

                if (respOpt.isPresent()) {
                    answeredQuestions++;
                    answerText = respOpt.get().getAnswerText();
                    answeredAt = respOpt.get().getAnsweredAt();
                }

                questionDetails.add(new InterviewQuestionDetailResponse(
                        iq.getId(),
                        iq.getQuestionOrder(),
                        iq.getQuestion().getQuestionText(),
                        iq.getQuestion().getQuestionType(),
                        iq.getQuestion().getDifficulty(),
                        answerText,
                        answeredAt
                ));
            }

            int progress = (int) Math.round(((double) answeredQuestions / totalQuestions) * 100);
            response.setProgress(progress);
            response.setQuestions(questionDetails);
        } else {
            response.setProgress(0);
            response.setQuestions(Collections.emptyList());
        }

        return response;
    }

    private InterviewSessionResponse mapToSessionResponse(InterviewSession session) {
        return new InterviewSessionResponse(
                session.getId(),
                session.getCategory().getId(),
                session.getCategory().getName(),
                session.getDifficulty(),
                session.getStatus(),
                session.getMode(),
                session.getInteractionMode(),
                session.getQuestionCount(),
                session.getDurationMinutes(),
                session.getCreatedAt(),
                session.getStartedAt(),
                session.getCompletedAt()
        );
    }
}
