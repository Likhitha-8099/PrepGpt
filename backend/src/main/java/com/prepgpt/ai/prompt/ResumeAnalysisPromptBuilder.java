package com.prepgpt.ai.prompt;

import org.springframework.stereotype.Component;

@Component
public class ResumeAnalysisPromptBuilder {

    public String buildResumeAnalysisPrompt(String extractedText) {
        return """
                You are an expert technical recruiter and resume analyzer on the PrepGPT platform.
                Analyze the following candidate resume text and extract structured information.
                
                RAW RESUME TEXT:
                ---BEGIN RESUME---
                %s
                ---END RESUME---
                
                INSTRUCTIONS:
                1. Extract technical skills (languages, frameworks, databases, tools).
                2. Extract work experience history (company, role, duration, highlights).
                3. Extract education history (degree, institution, graduation year).
                4. Extract key projects mentioned (title, description, technologies, candidate contribution).
                5. Extract relevant certifications.
                6. Produce a brief 2-sentence professional executive summary of the candidate.
                7. Output ONLY a valid JSON object matching the exact schema below. Do not include markdown codeblocks or extra prose.
                
                REQUIRED JSON SCHEMA:
                {
                  "summary": "Brief 2-sentence summary of candidate background",
                  "skills": ["Java", "Spring Boot", "React", "MySQL"],
                  "experience": [
                    {
                      "company": "Company Name",
                      "role": "Software Engineer Intern",
                      "duration": "6 months",
                      "highlights": ["Built REST APIs", "Optimized queries"]
                    }
                  ],
                  "education": [
                    {
                      "institution": "University Name",
                      "degree": "B.Tech Computer Science",
                      "graduationYear": "2024"
                    }
                  ],
                  "projects": [
                    {
                      "title": "Project Title",
                      "description": "Short description of project",
                      "technologies": "Technologies used",
                      "roleContribution": "What candidate specifically implemented"
                    }
                  ],
                  "certifications": ["AWS Cloud Practitioner"]
                }
                """.formatted(extractedText != null ? extractedText : "");
    }
}
