import apiClient from './api';

export const getCategories = async () => {
  const response = await apiClient.get('/interview-categories');
  return response.data;
};
