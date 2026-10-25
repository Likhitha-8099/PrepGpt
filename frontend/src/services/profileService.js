import apiClient from './api';

export const getStudentProfile = async () => {
  const response = await apiClient.get('/profile');
  return response.data;
};

export const updateStudentProfile = async (profileData) => {
  const response = await apiClient.put('/profile', profileData);
  return response.data;
};
