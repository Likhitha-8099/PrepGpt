import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import MainLayout from '../../layouts/MainLayout';
import AdminNav from '../../components/AdminNav';
import { getPlatformStats, getAuditLog } from '../../services/adminService';

const AdminDashboardPage = () => {
  const [stats, setStats] = useState(null);
  const [recentLogs, setRecentLogs] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    fetchDashboardData();
  }, []);

  const fetchDashboardData = async () => {
    setLoading(true);
    setError('');
    try {
      const [statsRes, auditRes] = await Promise.all([
        getPlatformStats(),
        getAuditLog(0, 8)
      ]);
      setStats(statsRes.data);
      setRecentLogs(auditRes.data.entries || auditRes.data.content || []);
    } catch (err) {
      console.error('Failed to load admin dashboard:', err);
      setError('Unable to load platform statistics and audit events.');
    } finally {
      setLoading(false);
    }
  };

  const formatDate = (isoString) => {
    if (!isoString) return '—';
    return new Date(isoString).toLocaleString('en-US', {
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  };

  return (
    <MainLayout>
      <div style={{ maxWidth: '1120px', margin: '0 auto', paddingBottom: '3rem' }}>
        {/* Top Header */}
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '1.75rem', flexWrap: 'wrap', gap: '1rem' }}>
          <div>
            <div style={{ fontSize: '0.75rem', fontWeight: 600, color: 'var(--text-muted)', textTransform: 'uppercase', letterSpacing: '0.05em', marginBottom: '0.25rem' }}>
              Administration
            </div>
            <h1 style={{ fontSize: '1.625rem', fontWeight: 600, letterSpacing: '-0.02em', margin: 0 }}>
              Platform Overview
            </h1>
            <p style={{ color: 'var(--text-muted)', fontSize: '0.875rem', margin: '0.25rem 0 0 0' }}>
              System health, active user base, and platform telemetry.
            </p>
          </div>

          <AdminNav />
        </div>

        {loading ? (
          <div className="card" style={{ padding: '2.5rem', textAlign: 'center' }}>
            <div className="skeleton" style={{ width: '40%', height: '20px', margin: '0 auto 1rem auto' }} />
            <div className="skeleton" style={{ width: '70%', height: '14px', margin: '0 auto' }} />
          </div>
        ) : error ? (
          <div className="card" style={{ padding: '2rem', textAlign: 'center' }}>
            <div className="alert alert-danger" style={{ marginBottom: '1rem' }}>{error}</div>
            <button onClick={fetchDashboardData} className="btn btn-secondary">Retry</button>
          </div>
        ) : (
          <>
            {/* STATS TILES */}
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))', gap: '1rem', marginBottom: '2rem' }}>
              <div className="card" style={{ padding: '1.25rem' }}>
                <div style={{ fontSize: '0.75rem', fontWeight: 600, textTransform: 'uppercase', letterSpacing: '0.04em', color: 'var(--text-muted)' }}>
                  Total Users
                </div>
                <div style={{ fontSize: '2rem', fontWeight: 700, letterSpacing: '-0.03em', color: 'var(--text-primary)', margin: '0.35rem 0' }}>
                  {stats?.totalUsers ?? 0}
                </div>
                <div style={{ fontSize: '0.8125rem', color: 'var(--text-secondary)' }}>
                  <span style={{ color: 'var(--success)', fontWeight: 500 }}>{stats?.activeUsers ?? 0}</span> active accounts
                </div>
              </div>

              <div className="card" style={{ padding: '1.25rem' }}>
                <div style={{ fontSize: '0.75rem', fontWeight: 600, textTransform: 'uppercase', letterSpacing: '0.04em', color: 'var(--text-muted)' }}>
                  Question Bank
                </div>
                <div style={{ fontSize: '2rem', fontWeight: 700, letterSpacing: '-0.03em', color: 'var(--text-primary)', margin: '0.35rem 0' }}>
                  {stats?.totalQuestions ?? 0}
                </div>
                <div style={{ fontSize: '0.8125rem', color: 'var(--text-secondary)' }}>
                  <span style={{ color: 'var(--primary)', fontWeight: 500 }}>{stats?.activeQuestions ?? 0}</span> active questions
                </div>
              </div>

              <div className="card" style={{ padding: '1.25rem' }}>
                <div style={{ fontSize: '0.75rem', fontWeight: 600, textTransform: 'uppercase', letterSpacing: '0.04em', color: 'var(--text-muted)' }}>
                  Mock Sessions
                </div>
                <div style={{ fontSize: '2rem', fontWeight: 700, letterSpacing: '-0.03em', color: 'var(--text-primary)', margin: '0.35rem 0' }}>
                  {stats?.totalSessions ?? 0}
                </div>
                <div style={{ fontSize: '0.8125rem', color: 'var(--text-secondary)' }}>
                  {stats?.completedSessions ?? 0} completed · {stats?.inProgressSessions ?? 0} in progress
                </div>
              </div>

              <div className="card" style={{ padding: '1.25rem' }}>
                <div style={{ fontSize: '0.75rem', fontWeight: 600, textTransform: 'uppercase', letterSpacing: '0.04em', color: 'var(--text-muted)' }}>
                  Platform Average Score
                </div>
                <div style={{ fontSize: '2rem', fontWeight: 700, letterSpacing: '-0.03em', color: 'var(--text-primary)', margin: '0.35rem 0' }}>
                  {stats?.averagePlatformScore != null ? stats.averagePlatformScore.toFixed(1) : '—'}
                  <span style={{ fontSize: '1rem', color: 'var(--text-muted)', fontWeight: 400 }}> / 10</span>
                </div>
                <div style={{ fontSize: '0.8125rem', color: 'var(--text-secondary)' }}>
                  Across all completed evaluations
                </div>
              </div>
            </div>

            {/* RECENT AUDIT ACTIVITY */}
            <div className="card" style={{ padding: '1.5rem' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.25rem' }}>
                <div>
                  <h2 style={{ fontSize: '1.0625rem', fontWeight: 600, margin: 0 }}>
                    Recent Audit Events
                  </h2>
                  <p style={{ color: 'var(--text-muted)', fontSize: '0.8125rem', margin: '0.2rem 0 0 0' }}>
                    Privileged administrative actions and security modifications.
                  </p>
                </div>
                <Link to="/admin/audit-log" style={{ color: 'var(--primary)', fontSize: '0.8125rem', fontWeight: 500, textDecoration: 'none' }}>
                  View full trail →
                </Link>
              </div>

              {recentLogs.length === 0 ? (
                <div className="empty-state" style={{ padding: '2rem 1rem' }}>
                  <p>No administrative events recorded yet.</p>
                </div>
              ) : (
                <div className="table-container">
                  <table className="data-table">
                    <thead>
                      <tr>
                        <th>Timestamp</th>
                        <th>Admin</th>
                        <th>Action</th>
                        <th>Target</th>
                        <th>Details</th>
                      </tr>
                    </thead>
                    <tbody>
                      {recentLogs.map((log) => (
                        <tr key={log.id}>
                          <td style={{ color: 'var(--text-muted)', fontSize: '0.8125rem', whiteSpace: 'nowrap' }}>
                            {formatDate(log.performedAt)}
                          </td>
                          <td style={{ fontWeight: 500, color: 'var(--text-primary)' }}>
                            {log.performedByEmail}
                          </td>
                          <td>
                            <span className={`badge ${
                              log.action?.includes('DEACTIVATE') ? 'badge-danger' :
                              log.action?.includes('ROLE') ? 'badge-warning' :
                              'badge-secondary'
                            }`} style={{ fontSize: '0.75rem' }}>
                              {log.action}
                            </span>
                          </td>
                          <td style={{ color: 'var(--text-secondary)', fontSize: '0.8125rem' }}>
                            {log.targetEntity ? `${log.targetEntity} #${log.targetEntityId}` : '—'}
                          </td>
                          <td style={{ color: 'var(--text-muted)', fontSize: '0.8125rem', maxWidth: '300px', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                            {log.details || '—'}
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}
            </div>
          </>
        )}
      </div>
    </MainLayout>
  );
};

export default AdminDashboardPage;
