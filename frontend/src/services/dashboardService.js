import apiClient from './api';

export const getDashboardSummary = async () => {
  const response = await apiClient.get('/dashboard');
  return response.data;
};
