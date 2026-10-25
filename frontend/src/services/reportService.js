import api from './api';

export const getInterviewReport = async (sessionId) => {
  const response = await api.get(`/reports/sessions/${sessionId}`);
  return response.data;
};

export const getAnalyticsOverview = async () => {
  const response = await api.get('/analytics/overview');
  return response.data;
};

export const getCategoryAnalytics = async () => {
  const response = await api.get('/analytics/categories');
  return response.data;
};
