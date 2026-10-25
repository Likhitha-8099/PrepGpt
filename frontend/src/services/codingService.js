import axios from 'axios';

const API_URL = '/api/v1/coding';

const getAuthHeaders = () => {
  const token = localStorage.getItem('token');
  return {
    headers: {
      Authorization: `Bearer ${token}`
    }
  };
};

export const codingService = {
  getAllProblems: async () => {
    const response = await axios.get(`${API_URL}/problems`, getAuthHeaders());
    return response.data;
  },

  getProblemById: async (problemId) => {
    const response = await axios.get(`${API_URL}/problems/${problemId}`, getAuthHeaders());
    return response.data;
  },

  runCode: async (sessionId, questionId, language, code) => {
    const response = await axios.post(
      `${API_URL}/sessions/${sessionId}/questions/${questionId}/run`,
      { language, code },
      getAuthHeaders()
    );
    return response.data;
  },

  submitCode: async (sessionId, questionId, language, code) => {
    const response = await axios.post(
      `${API_URL}/sessions/${sessionId}/questions/${questionId}/submit`,
      { language, code },
      getAuthHeaders()
    );
    return response.data;
  }
};
