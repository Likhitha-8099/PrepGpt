import api from './api';

/**
 * Fetch candidate initial data: profile info, uploaded resumes with extracted skills/experience,
 * and portfolio projects to prefill the ATS Resume Builder.
 */
export const getResumeBuilderInitialData = async () => {
  const response = await api.get('/resume-builder/initial-data');
  return response.data;
};

/**
 * Request Groq AI assistance for resume sections:
 * - IMPROVE_SUMMARY: rewrites summary into concise, punchy ATS format
 * - IMPROVE_BULLET: rewrites experience bullet into STAR action-verb impact format
 * - SUGGEST_SKILLS: recommends relevant skills based on actual experience
 * - JD_KEYWORD_MATCH: compares resume against a pasted Job Description
 * - FORMAT_AUDIT: audits ATS readability and structure
 */
export const requestAiAssist = async (payload) => {
  const response = await api.post('/resume-builder/ai-assist', payload);
  return response.data;
};

/**
 * Save the compiled ATS resume as an active PrepGPT resume so the candidate
 * can immediately launch mock interviews against it.
 */
export const saveResumeToResumes = async (payload) => {
  const response = await api.post('/resume-builder/save-to-resumes', payload);
  return response.data;
};
