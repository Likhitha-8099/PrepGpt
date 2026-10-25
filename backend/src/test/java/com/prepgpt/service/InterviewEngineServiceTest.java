package com.prepgpt.service;

import com.prepgpt.dto.*;
import com.prepgpt.entity.*;
import com.prepgpt.exception.BadRequestException;
import com.prepgpt.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InterviewEngineServiceTest {

    @Mock
    private InterviewSessionRepository sessionRepository;

    @Mock
    private QuestionRepository questionRepository;

    @Mock
    private InterviewQuestionRepository interviewQuestionRepository;

    @Mock
    private InterviewResponseRepository interviewResponseRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private InterviewEngineService engineService;

    private User testUser;
    private InterviewCategory testCategory;
    private InterviewSession testSession;

    @BeforeEach
    void setUp() {
        Role studentRole = new Role("ROLE_STUDENT");
        testUser = new User("student@example.com", "password", "Student Test", studentRole);
        testUser.setId(1L);

        testCategory = new InterviewCategory("Technical Interview", "Technical description", true);
        testCategory.setId(10L);

        testSession = new InterviewSession(testUser, testCategory, Difficulty.MEDIUM, 3, 15);
        testSession.setId(100L);
    }

    @Test
    void startSession_ThrowsException_WhenInsufficientQuestions() {
        when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));
        when(sessionRepository.findByIdAndUserId(testSession.getId(), testUser.getId())).thenReturn(Optional.of(testSession));
        when(questionRepository.findByCategoryIdAndDifficultyAndActiveTrue(testCategory.getId(), Difficulty.MEDIUM))
                .thenReturn(List.of(new Question(testCategory, "Q1", QuestionType.TECHNICAL, Difficulty.MEDIUM, "Guidance")));

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                engineService.startSession(testUser.getEmail(), testSession.getId())
        );

        assertTrue(ex.getMessage().contains("Not enough questions available"));
    }

    @Test
    void startSession_Success_WhenEnoughQuestionsExist() {
        when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));
        when(sessionRepository.findByIdAndUserId(testSession.getId(), testUser.getId())).thenReturn(Optional.of(testSession));

        List<Question> availableQuestions = List.of(
                new Question(testCategory, "Q1", QuestionType.TECHNICAL, Difficulty.MEDIUM, "G1"),
                new Question(testCategory, "Q2", QuestionType.TECHNICAL, Difficulty.MEDIUM, "G2"),
                new Question(testCategory, "Q3", QuestionType.TECHNICAL, Difficulty.MEDIUM, "G3")
        );

        when(questionRepository.findByCategoryIdAndDifficultyAndActiveTrue(testCategory.getId(), Difficulty.MEDIUM))
                .thenReturn(availableQuestions);
        when(sessionRepository.save(any(InterviewSession.class))).thenAnswer(inv -> inv.getArgument(0));

        InterviewSessionResponse response = engineService.startSession(testUser.getEmail(), testSession.getId());

        assertNotNull(response);
        assertEquals(SessionStatus.IN_PROGRESS, response.getStatus());
        verify(interviewQuestionRepository, times(3)).save(any(InterviewQuestion.class));
    }

    @Test
    void submitAnswer_ThrowsException_WhenDuplicateAnswer() {
        testSession.setStatus(SessionStatus.IN_PROGRESS);

        Question q = new Question(testCategory, "Q1", QuestionType.TECHNICAL, Difficulty.MEDIUM, "G1");
        InterviewQuestion iq = new InterviewQuestion(testSession, q, 1);
        iq.setId(500L);

        when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));
        when(sessionRepository.findByIdAndUserId(testSession.getId(), testUser.getId())).thenReturn(Optional.of(testSession));
        when(interviewQuestionRepository.findById(iq.getId())).thenReturn(Optional.of(iq));
        when(interviewResponseRepository.existsByInterviewQuestionId(iq.getId())).thenReturn(true);

        SubmitAnswerRequest request = new SubmitAnswerRequest("Sample Answer");

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                engineService.submitAnswer(testUser.getEmail(), testSession.getId(), iq.getId(), request)
        );

        assertEquals("This question has already been answered.", ex.getMessage());
    }

    @Test
    void submitAnswer_CompletesSession_WhenFinalQuestionAnswered() {
        testSession.setStatus(SessionStatus.IN_PROGRESS);

        Question q = new Question(testCategory, "Q3", QuestionType.TECHNICAL, Difficulty.MEDIUM, "G3");
        InterviewQuestion iq = new InterviewQuestion(testSession, q, 3);
        iq.setId(502L);

        when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));
        when(sessionRepository.findByIdAndUserId(testSession.getId(), testUser.getId())).thenReturn(Optional.of(testSession));
        when(interviewQuestionRepository.findById(iq.getId())).thenReturn(Optional.of(iq));
        when(interviewResponseRepository.existsByInterviewQuestionId(iq.getId())).thenReturn(false);

        when(interviewQuestionRepository.countByInterviewSessionId(testSession.getId())).thenReturn(3L);
        when(interviewResponseRepository.countByInterviewQuestionInterviewSessionId(testSession.getId())).thenReturn(3L);

        SubmitAnswerRequest request = new SubmitAnswerRequest("Final Answer Text");
        AnswerResponse response = engineService.submitAnswer(testUser.getEmail(), testSession.getId(), iq.getId(), request);

        assertTrue(response.getSuccess());
        assertTrue(response.getCompleted());
        assertEquals(SessionStatus.COMPLETED, testSession.getStatus());
        assertNotNull(testSession.getCompletedAt());
    }
}
