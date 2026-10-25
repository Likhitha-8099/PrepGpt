import apiClient from './api';

const ADMIN_BASE = '/admin';

// ── PLATFORM STATISTICS ──────────────────────────────────────────────────────
export const getPlatformStats = () =>
  apiClient.get(`${ADMIN_BASE}/stats`);

// ── USER MANAGEMENT ──────────────────────────────────────────────────────────
export const getAdminUsers = (page = 0, size = 20) =>
  apiClient.get(`${ADMIN_BASE}/users`, { params: { page, size } });

export const getAdminUserById = (id) =>
  apiClient.get(`${ADMIN_BASE}/users/${id}`);

export const deactivateUser = (id) =>
  apiClient.patch(`${ADMIN_BASE}/users/${id}/deactivate`);

export const reactivateUser = (id) =>
  apiClient.patch(`${ADMIN_BASE}/users/${id}/reactivate`);

export const changeUserRole = (id, newRole) =>
  apiClient.patch(`${ADMIN_BASE}/users/${id}/role`, { newRole });

// ── QUESTION MANAGEMENT ──────────────────────────────────────────────────────
export const getAdminQuestions = (page = 0, size = 20, categoryId = null, difficulty = null) => {
  const params = { page, size };
  if (categoryId) params.categoryId = categoryId;
  if (difficulty) params.difficulty = difficulty;
  return apiClient.get(`${ADMIN_BASE}/questions`, { params });
};

export const createAdminQuestion = (data) =>
  apiClient.post(`${ADMIN_BASE}/questions`, data);

export const updateAdminQuestion = (id, data) =>
  apiClient.put(`${ADMIN_BASE}/questions/${id}`, data);

export const deactivateQuestion = (id) =>
  apiClient.patch(`${ADMIN_BASE}/questions/${id}/deactivate`);

export const reactivateQuestion = (id) =>
  apiClient.patch(`${ADMIN_BASE}/questions/${id}/reactivate`);

// ── SESSION OVERSIGHT (READ-ONLY) ────────────────────────────────────────────
export const getAdminSessions = (page = 0, size = 20, userId = null, status = null) => {
  const params = { page, size };
  if (userId) params.userId = userId;
  if (status) params.status = status;
  return apiClient.get(`${ADMIN_BASE}/sessions`, { params });
};

// ── AUDIT LOG (READ-ONLY) ────────────────────────────────────────────────────
export const getAuditLog = (page = 0, size = 20) =>
  apiClient.get(`${ADMIN_BASE}/audit-log`, { params: { page, size } });
