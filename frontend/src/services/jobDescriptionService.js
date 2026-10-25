import api from './api';

export const createJobDescription = async (jdData) => {
  const response = await api.post('/job-descriptions', jdData);
  return response.data;
};

export const getUserJobDescriptions = async () => {
  const response = await api.get('/job-descriptions');
  return response.data;
};

export const getJobDescriptionById = async (id) => {
  const response = await api.get(`/job-descriptions/${id}`);
  return response.data;
};

export const deleteJobDescription = async (id) => {
  const response = await api.delete(`/job-descriptions/${id}`);
  return response.data;
};

