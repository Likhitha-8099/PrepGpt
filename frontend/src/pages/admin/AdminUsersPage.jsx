import React, { useState, useEffect } from 'react';
import MainLayout from '../../layouts/MainLayout';
import AdminNav from '../../components/AdminNav';
import { getAdminUsers, deactivateUser, reactivateUser, changeUserRole } from '../../services/adminService';
import { useAuth } from '../../hooks/useAuth';

const AdminUsersPage = () => {
  const { user: currentAdmin } = useAuth();
  const [users, setUsers] = useState([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(1);
  const [totalElements, setTotalElements] = useState(0);
  const [loading, setLoading] = useState(true);
  const [actionLoading, setActionLoading] = useState(false);
  const [banner, setBanner] = useState({ message: '', type: '' });

  // Role change modal state
  const [roleModalUser, setRoleModalUser] = useState(null);
  const [selectedRole, setSelectedRole] = useState('ROLE_STUDENT');

  useEffect(() => {
    fetchUsers(page);
  }, [page]);

  const fetchUsers = async (pageNumber) => {
    setLoading(true);
    try {
      const res = await getAdminUsers(pageNumber, 15);
      const data = res.data;
      setUsers(data.users || data.content || []);
      setTotalPages(data.totalPages || 1);
      setTotalElements(data.totalElements || 0);
    } catch (err) {
      console.error('Failed to fetch admin users:', err);
      showBanner('Failed to load user records from administrative endpoint.', 'danger');
    } finally {
      setLoading(false);
    }
  };

  const showBanner = (message, type = 'success') => {
    setBanner({ message, type });
    setTimeout(() => {
      setBanner({ message: '', type: '' });
    }, 6000);
  };

  const handleToggleActive = async (targetUser) => {
    const isDeactivating = targetUser.isActive;
    const confirmMessage = isDeactivating
      ? `Deactivate ${targetUser.fullName} (${targetUser.email})? Active sessions will be terminated.`
      : `Reactivate ${targetUser.fullName} (${targetUser.email})?`;

    if (!window.confirm(confirmMessage)) return;

    setActionLoading(true);
    try {
      if (isDeactivating) {
        await deactivateUser(targetUser.id);
        showBanner(`User ${targetUser.email} deactivated.`, 'success');
      } else {
        await reactivateUser(targetUser.id);
        showBanner(`User ${targetUser.email} reactivated.`, 'success');
      }
      fetchUsers(page);
    } catch (err) {
      const errorMsg = err.response?.data?.message || err.message || 'Action failed.';
      showBanner(errorMsg, 'danger');
    } finally {
      setActionLoading(false);
    }
  };

  const openRoleModal = (targetUser) => {
    setRoleModalUser(targetUser);
    setSelectedRole(targetUser.role === 'ROLE_ADMIN' ? 'ROLE_STUDENT' : 'ROLE_ADMIN');
  };

  const handleSaveRole = async () => {
    if (!roleModalUser) return;
    setActionLoading(true);
    try {
      await changeUserRole(roleModalUser.id, selectedRole);
      showBanner(`Role for ${roleModalUser.email} updated to ${selectedRole}.`, 'success');
      setRoleModalUser(null);
      fetchUsers(page);
    } catch (err) {
      const errorMsg = err.response?.data?.message || err.message || 'Failed to update role.';
      showBanner(errorMsg, 'danger');
    } finally {
      setActionLoading(false);
    }
  };

  const formatDate = (isoString) => {
    if (!isoString) return '—';
    return new Date(isoString).toLocaleDateString('en-US', {
      month: 'short',
      day: 'numeric',
      year: 'numeric'
    });
  };

  return (
    <MainLayout>
      <div style={{ maxWidth: '1120px', margin: '0 auto', paddingBottom: '3rem' }}>
        {/* Header */}
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '1.75rem', flexWrap: 'wrap', gap: '1rem' }}>
          <div>
            <div style={{ fontSize: '0.75rem', fontWeight: 600, color: 'var(--text-muted)', textTransform: 'uppercase', letterSpacing: '0.05em', marginBottom: '0.25rem' }}>
              Administration
            </div>
            <h1 style={{ fontSize: '1.625rem', fontWeight: 600, letterSpacing: '-0.02em', margin: 0 }}>
              User Management
            </h1>
            <p style={{ color: 'var(--text-muted)', fontSize: '0.875rem', margin: '0.25rem 0 0 0' }}>
              {totalElements} registered candidate accounts across the platform.
            </p>
          </div>

          <AdminNav />
        </div>

        {banner.message && (
          <div className={`alert alert-${banner.type === 'danger' ? 'danger' : 'success'}`} style={{ marginBottom: '1.25rem' }}>
            {banner.message}
          </div>
        )}

        {/* User Table */}
        {loading ? (
          <div className="card" style={{ padding: '2.5rem', textAlign: 'center' }}>
            <div className="skeleton" style={{ width: '40%', height: '20px', margin: '0 auto 1rem auto' }} />
            <div className="skeleton" style={{ width: '70%', height: '14px', margin: '0 auto' }} />
          </div>
        ) : (
          <div className="card" style={{ padding: '1.25rem' }}>
            <div className="table-container">
              <table className="data-table">
                <thead>
                  <tr>
                    <th style={{ width: '70px' }}>ID</th>
                    <th>User</th>
                    <th>Email</th>
                    <th>Role</th>
                    <th>Status</th>
                    <th>Joined</th>
                    <th style={{ textAlign: 'right' }}>Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {users.map((u) => {
                    const isSelf = currentAdmin && currentAdmin.id === u.id;
                    return (
                      <tr key={u.id}>
                        <td style={{ color: 'var(--text-muted)', fontFamily: 'var(--font-mono, monospace)', fontSize: '0.8125rem' }}>
                          #{u.id}
                        </td>
                        <td style={{ fontWeight: 500, color: 'var(--text-primary)' }}>
                          {u.fullName} {isSelf && <span style={{ color: 'var(--text-muted)', fontSize: '0.75rem' }}>(You)</span>}
                        </td>
                        <td style={{ color: 'var(--text-secondary)', fontSize: '0.8125rem' }}>
                          {u.email}
                        </td>
                        <td>
                          <span className={`badge ${u.role === 'ROLE_ADMIN' ? 'badge-info' : 'badge-secondary'}`} style={{ fontSize: '0.75rem' }}>
                            {u.role.replace('ROLE_', '')}
                          </span>
                        </td>
                        <td>
                          <span className={`badge ${u.isActive ? 'badge-success' : 'badge-danger'}`} style={{ fontSize: '0.75rem' }}>
                            {u.isActive ? 'Active' : 'Suspended'}
                          </span>
                        </td>
                        <td style={{ color: 'var(--text-muted)', fontSize: '0.8125rem', whiteSpace: 'nowrap' }}>
                          {formatDate(u.createdAt)}
                        </td>
                        <td style={{ textAlign: 'right' }}>
                          <div style={{ display: 'inline-flex', gap: '0.375rem' }}>
                            <button
                              type="button"
                              className="btn btn-secondary btn-sm"
                              onClick={() => openRoleModal(u)}
                              disabled={actionLoading || isSelf}
                            >
                              Role
                            </button>
                            <button
                              type="button"
                              className={`btn btn-sm ${u.isActive ? 'btn-danger' : 'btn-secondary'}`}
                              onClick={() => handleToggleActive(u)}
                              disabled={actionLoading || isSelf}
                            >
                              {u.isActive ? 'Deactivate' : 'Reactivate'}
                            </button>
                          </div>
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>

            {/* Pagination */}
            {totalPages > 1 && (
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginTop: '1.25rem', paddingTop: '1rem', borderTop: '1px solid var(--border)' }}>
                <span style={{ fontSize: '0.8125rem', color: 'var(--text-muted)' }}>
                  Page {page + 1} of {totalPages}
                </span>
                <div style={{ display: 'flex', gap: '0.375rem' }}>
                  <button
                    type="button"
                    className="btn btn-secondary btn-sm"
                    disabled={page === 0}
                    onClick={() => setPage(page - 1)}
                  >
                    Previous
                  </button>
                  <button
                    type="button"
                    className="btn btn-secondary btn-sm"
                    disabled={page >= totalPages - 1}
                    onClick={() => setPage(page + 1)}
                  >
                    Next
                  </button>
                </div>
              </div>
            )}
          </div>
        )}

        {/* Change Role Modal */}
        {roleModalUser && (
          <div style={{
            position: 'fixed',
            top: 0, left: 0, right: 0, bottom: 0,
            background: 'rgba(15, 23, 42, 0.45)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            zIndex: 1000,
            padding: '1rem'
          }}>
            <div className="card" style={{ maxWidth: '440px', width: '100%', padding: '1.5rem' }}>
              <h3 style={{ margin: '0 0 0.5rem 0', fontSize: '1.125rem', fontWeight: 600 }}>
                Change Account Role
              </h3>
              <p style={{ color: 'var(--text-muted)', fontSize: '0.8125rem', margin: '0 0 1.25rem 0' }}>
                Select role privileges for <strong>{roleModalUser.email}</strong>
              </p>

              <div className="form-group">
                <label className="form-label">Role</label>
                <select
                  value={selectedRole}
                  onChange={(e) => setSelectedRole(e.target.value)}
                  className="form-select"
                >
                  <option value="ROLE_STUDENT">Student Candidate</option>
                  <option value="ROLE_ADMIN">Administrator</option>
                </select>
              </div>

              <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '0.5rem', marginTop: '1.25rem' }}>
                <button
                  type="button"
                  className="btn btn-secondary"
                  onClick={() => setRoleModalUser(null)}
                  disabled={actionLoading}
                >
                  Cancel
                </button>
                <button
                  type="button"
                  className="btn btn-primary"
                  onClick={handleSaveRole}
                  disabled={actionLoading}
                >
                  {actionLoading ? 'Updating...' : 'Save Role'}
                </button>
              </div>
            </div>
          </div>
        )}
      </div>
    </MainLayout>
  );
};

export default AdminUsersPage;
