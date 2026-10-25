package com.prepgpt.ai.prompt;

import com.prepgpt.dto.ResumeBuilderAiAssistRequest;
import org.springframework.stereotype.Component;

@Component
public class ResumeBuilderPromptBuilder {

    public String buildPrompt(ResumeBuilderAiAssistRequest request) {
        String action = request.getAction() != null ? request.getAction().toUpperCase() : "IMPROVE_SUMMARY";

        return switch (action) {
            case "IMPROVE_SUMMARY" -> buildImproveSummaryPrompt(request);
            case "IMPROVE_BULLET" -> buildImproveBulletPrompt(request);
            case "SUGGEST_SKILLS" -> buildSuggestSkillsPrompt(request);
            case "JD_KEYWORD_MATCH" -> buildJdKeywordMatchPrompt(request);
            case "FORMAT_AUDIT" -> buildFormatAuditPrompt(request);
            default -> buildImproveSummaryPrompt(request);
        };
    }

    private String buildImproveSummaryPrompt(ResumeBuilderAiAssistRequest request) {
        return """
                You are a senior technical recruiter and ATS resume optimization expert.
                Your task is to rewrite and polish the candidate's professional resume summary to make it compelling, concise, and ATS-friendly.
                
                CANDIDATE INPUT:
                - Target Role: %s
                - Current Summary / Bio: %s
                - Experience Context: %s
                - Skills Context: %s
                
                STRICT GUIDELINES:
                1. Write 2-3 concise, high-impact sentences (under 60 words total).
                2. Use strong, active professional tone. Highlight core competencies and engineering strengths.
                3. DO NOT invent false employment history, years of experience, fake degrees, or metrics not implied by the candidate.
                4. Provide 1 primary improved summary, plus 2 alternative variations (e.g., Technical Depth focus, Results/Impact focus).
                5. Output ONLY valid JSON matching the exact schema below.
                
                REQUIRED JSON SCHEMA:
                {
                  "improvedText": "High impact 2-3 sentence professional summary",
                  "suggestions": [
                    "Alternative variation 1: Focus on engineering craftsmanship",
                    "Alternative variation 2: Focus on business impact and scale"
                  ]
                }
                """.formatted(
                safeString(request.getTargetRole()),
                safeString(request.getSummary()),
                safeString(request.getExperienceContext()),
                safeString(request.getSkillsContext())
        );
    }

    private String buildImproveBulletPrompt(ResumeBuilderAiAssistRequest request) {
        return """
                You are an expert resume writer specializing in ATS-optimized engineering bullet points using the Google XYZ / STAR format:
                "Accomplished [X], as measured by [Y], by doing [Z]" or "Action Verb + Task/Context + Outcome".
                
                CANDIDATE INPUT:
                - Bullet to enhance: "%s"
                - Role / Company Context: %s
                - Tech Stack Context: %s
                
                STRICT GUIDELINES:
                1. Begin with a strong past-tense action verb (e.g., Architected, Engineered, Optimized, Implemented, Streamlined, Reduced, Accelerated).
                2. Keep the sentence concise, clear, and direct (15-28 words).
                3. DO NOT fabricate false metrics or numbers if none were provided; instead frame the scope, reliability, or latency impact clearly.
                4. Provide 1 recommended bullet, plus 2 alternative phrasings.
                5. Output ONLY valid JSON matching the exact schema below.
                
                REQUIRED JSON SCHEMA:
                {
                  "improvedText": "Engineered high-throughput RESTful endpoints using Spring Boot, reducing API response times through optimized query execution and connection pooling.",
                  "suggestions": [
                    "Alternative phrasing emphasizing system reliability and testing",
                    "Alternative phrasing emphasizing cross-functional delivery"
                  ]
                }
                """.formatted(
                safeString(request.getBullet()),
                safeString(request.getExperienceContext()),
                safeString(request.getSkillsContext())
        );
    }

    private String buildSuggestSkillsPrompt(ResumeBuilderAiAssistRequest request) {
        return """
                You are a technical talent assessor.
                Analyze the candidate's work experience and project descriptions to identify relevant, verifiable skills that naturally align with their background for ATS parsing.
                
                CANDIDATE INPUT:
                - Target Role: %s
                - Experience & Projects: %s
                - Current Skills Listed: %s
                
                STRICT GUIDELINES:
                1. Suggest only industry-standard technical skills, frameworks, tools, or methodologies directly supported by their actual work experience.
                2. DO NOT suggest niche tools or advanced technologies that the candidate has never mentioned or used.
                3. Return a list of suggested skills that are not already present in their current skills list.
                4. Output ONLY valid JSON matching the exact schema below.
                
                REQUIRED JSON SCHEMA:
                {
                  "improvedText": "Analyzed background and identified relevant complementary technical skills for ATS keyword matching.",
                  "suggestions": [
                    "RESTful APIs",
                    "Database Optimization",
                    "Unit Testing / JUnit",
                    "Git Version Control",
                    "Microservices Architecture"
                  ]
                }
                """.formatted(
                safeString(request.getTargetRole()),
                safeString(request.getExperienceContext() + " " + request.getProjectsContext()),
                safeString(request.getSkillsContext())
        );
    }

    private String buildJdKeywordMatchPrompt(ResumeBuilderAiAssistRequest request) {
        return """
                You are an ATS (Applicant Tracking System) parser and resume keyword comparator.
                Compare the candidate's resume content against the provided target Job Description.
                
                TARGET JOB DESCRIPTION:
                %s
                
                CANDIDATE RESUME CONTENT:
                - Summary: %s
                - Skills: %s
                - Experience & Projects: %s
                
                STRICT GUIDELINES:
                1. Identify hard technical skills, competencies, and tools mentioned in the Job Description that ARE matched in the candidate's resume.
                2. Identify key requirements or skills in the Job Description that are MISSING or weakly emphasized.
                3. Calculate an estimated ATS match readiness score between 0 and 100 based strictly on keyword and competency overlap.
                4. Provide 3 actionable tips for how the candidate can truthfully incorporate missing skills if they have relevant experience.
                5. Output ONLY valid JSON matching the exact schema below.
                
                REQUIRED JSON SCHEMA:
                {
                  "atsReadinessScore": 78,
                  "matchedKeywords": ["Java", "Spring Boot", "SQL", "REST APIs", "Agile"],
                  "missingKeywords": ["Docker", "Kubernetes", "Redis", "CI/CD"],
                  "auditTips": [
                    "Highlight any containerization or Docker experience in your project descriptions.",
                    "Explicitly state caching mechanisms (e.g., Redis) if you used them in your backend projects.",
                    "Ensure target job title aligns with your professional headline."
                  ],
                  "improvedText": "Your resume currently matches 78%% of key keywords identified in the target job description."
                }
                """.formatted(
                safeString(request.getJobDescription()),
                safeString(request.getSummary()),
                safeString(request.getSkillsContext()),
                safeString(request.getExperienceContext() + " " + request.getProjectsContext())
        );
    }

    private String buildFormatAuditPrompt(ResumeBuilderAiAssistRequest request) {
        return """
                You are an ATS compliance auditor for technical resumes.
                Inspect the candidate's resume information for standard ATS compliance, formatting traps, and readability issues.
                
                CANDIDATE RESUME SUMMARY:
                - Headline / Target Role: %s
                - Summary: %s
                - Skills: %s
                - Experience: %s
                - Projects: %s
                
                STRICT GUIDELINES:
                1. Assess clarity, quantifiable achievements, standard section coverage, and keyword density.
                2. Calculate an ATS format compliance score between 50 and 100.
                3. Provide 3-4 specific, actionable audit tips to maximize ATS parsability.
                4. Output ONLY valid JSON matching the exact schema below.
                
                REQUIRED JSON SCHEMA:
                {
                  "atsReadinessScore": 85,
                  "auditTips": [
                    "All core standard sections (Summary, Skills, Experience, Education) are properly present.",
                    "Ensure every experience item includes 2-4 bullet points highlighting specific outcomes.",
                    "Keep contact details at the top with plain text links to your GitHub and LinkedIn profiles."
                  ],
                  "improvedText": "ATS Audit complete. Layout conforms to single-column machine-readable standards."
                }
                """.formatted(
                safeString(request.getTargetRole()),
                safeString(request.getSummary()),
                safeString(request.getSkillsContext()),
                safeString(request.getExperienceContext()),
                safeString(request.getProjectsContext())
        );
    }

    private String safeString(String input) {
        return input != null ? input.trim() : "";
    }
}
