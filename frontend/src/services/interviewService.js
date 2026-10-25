import apiClient from './api';

export const createInterviewSession = async (sessionData) => {
  const response = await apiClient.post('/interview-sessions', sessionData);
  return response.data;
};

export const getInterviewSessions = async () => {
  const response = await apiClient.get('/interview-sessions');
  return response.data;
};

export const getInterviewSessionById = async (sessionId) => {
  const response = await apiClient.get(`/interview-sessions/${sessionId}`);
  return response.data;
};

export const startInterviewSession = async (sessionId) => {
  const response = await apiClient.post(`/interview-sessions/${sessionId}/start`);
  return response.data;
};

export const getCurrentQuestion = async (sessionId) => {
  const response = await apiClient.get(`/interview-sessions/${sessionId}/current-question`);
  return response.data;
};

export const submitQuestionAnswer = async (sessionId, questionId, answerText) => {
  const response = await apiClient.post(`/interview-sessions/${sessionId}/questions/${questionId}/answer`, { answerText });
  return response.data;
};

export const getNextQuestion = async (sessionId) => {
  const response = await apiClient.get(`/interview-sessions/${sessionId}/next-question`);
  return response.data;
};

export const getInterviewStatus = async (sessionId) => {
  const response = await apiClient.get(`/interview-sessions/${sessionId}/status`);
  return response.data;
};

export const abandonInterviewSession = async (sessionId) => {
  const response = await apiClient.post(`/interview-sessions/${sessionId}/abandon`);
  return response.data;
};

export const transcribeAudio = async (sessionId, audioBlob) => {
  const formData = new FormData();
  formData.append('file', audioBlob, 'answer.webm');
  const response = await apiClient.post(
    `/interview-sessions/${sessionId}/voice/transcribe`,
    formData,
    { headers: { 'Content-Type': 'multipart/form-data' } }
  );
  return response.data;
};

export const synthesizeText = async (sessionId, text) => {
  const response = await apiClient.post(
    `/interview-sessions/${sessionId}/voice/synthesize`,
    { text },
    { responseType: 'blob' }
  );
  return response.data;
};

