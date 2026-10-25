package com.prepgpt.ai.prompt;

import com.prepgpt.entity.Project;
import org.springframework.stereotype.Component;

@Component
public class ProjectAnalysisPromptBuilder {

    public String buildProjectAnalysisPrompt(Project project) {
        return """
                You are a principal software architect and technical interviewer.
                Perform a technical analysis on the following candidate software engineering project to prepare relevant technical interview topics and questions.
                
                PROJECT DETAILS:
                - Title: %s
                - Description: %s
                - Technologies Used: %s
                - Candidate Role & Contribution: %s
                - Key Features: %s
                - Architecture Details: %s
                
                INSTRUCTIONS:
                1. Summarize the technical architecture and complexity of the project.
                2. Identify 3-5 key architectural topics and engineering concepts (e.g., REST API Design, Database Normalization, Asynchronous Processing, Security).
                3. Identify 2-3 technical challenges or potential bottlenecks in this system design.
                4. Suggest 3-4 targeted, high-quality interview questions to test the candidate's real engineering understanding of their project.
                5. Return ONLY a valid JSON object matching the exact schema below.
                
                REQUIRED JSON SCHEMA:
                {
                  "projectTitle": "%s",
                  "technicalSummary": "Technical summary of project architecture",
                  "architectureTopics": ["Topic 1", "Topic 2", "Topic 3"],
                  "keyChallenges": ["Challenge 1", "Challenge 2"],
                  "suggestedInterviewQuestions": [
                    "Question 1 testing candidate's specific architecture choice",
                    "Question 2 testing trade-offs and scalability"
                  ]
                }
                """.formatted(
                project.getTitle(),
                project.getDescription() != null ? project.getDescription() : "None provided",
                project.getTechnologies() != null ? project.getTechnologies() : "Not specified",
                project.getRoleContribution() != null ? project.getRoleContribution() : "General developer",
                project.getKeyFeatures() != null ? project.getKeyFeatures() : "Standard features",
                project.getArchitectureDetails() != null ? project.getArchitectureDetails() : "Standard monolithic/web architecture",
                project.getTitle()
        );
    }
}
