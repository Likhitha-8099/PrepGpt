package com.prepgpt.ai;

import com.prepgpt.dto.AIEvaluationRequest;
import com.prepgpt.dto.AIEvaluationResponse;
import com.prepgpt.dto.AIFollowUpRequest;
import com.prepgpt.dto.AIFollowUpResponse;
import com.prepgpt.dto.AIQuestionRequest;
import com.prepgpt.dto.AIQuestionResponse;

public interface AIService {

    AIQuestionResponse generateQuestion(AIQuestionRequest request);

    AIEvaluationResponse evaluateAnswer(AIEvaluationRequest request);

    AIFollowUpResponse generateFollowUp(AIFollowUpRequest request);
}
