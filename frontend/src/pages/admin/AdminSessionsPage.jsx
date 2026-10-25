import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import MainLayout from '../../layouts/MainLayout';
import AdminNav from '../../components/AdminNav';
import { getAdminSessions } from '../../services/adminService';

const STATUSES = ['CONFIGURED', 'IN_PROGRESS', 'COMPLETED', 'ABANDONED'];

const AdminSessionsPage = () => {
  const [sessions, setSessions] = useState([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(1);
  const [totalElements, setTotalElements] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  // Filters
  const [filterStatus, setFilterStatus] = useState('');
  const [filterUserId, setFilterUserId] = useState('');

  useEffect(() => {
    fetchSessions(page, filterUserId, filterStatus);
  }, [page, filterStatus]);

  const fetchSessions = async (pageNum, uId, st) => {
    setLoading(true);
    setError('');
    try {
      const res = await getAdminSessions(
        pageNum,
        15,
        uId ? Number(uId) : null,
        st || null
      );
      const data = res.data;
      setSessions(data.sessions || data.content || []);
      setTotalPages(data.totalPages || 1);
      setTotalElements(data.totalElements || 0);
    } catch (err) {
      console.error('Failed to load admin sessions:', err);
      setError('Failed to load interview sessions overview.');
    } finally {
      setLoading(false);
    }
  };

  const handleApplyUserFilter = (e) => {
    e.preventDefault();
    setPage(0);
    fetchSessions(0, filterUserId, filterStatus);
  };

  const handleResetFilters = () => {
    setFilterUserId('');
    setFilterStatus('');
    setPage(0);
    fetchSessions(0, null, null);
  };

  const formatDate = (isoString) => {
    if (!isoString) return '—';
    return new Date(isoString).toLocaleDateString('en-US', {
      month: 'short',
      day: 'numeric',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  };

  const getStatusBadge = (status) => {
    switch (status) {
      case 'COMPLETED':
        return <span className="badge badge-success">Completed</span>;
      case 'IN_PROGRESS':
        return <span className="badge badge-warning">In Progress</span>;
      case 'ABANDONED':
        return <span className="badge badge-danger">Abandoned</span>;
      default:
        return <span className="badge badge-secondary">{status}</span>;
    }
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
              Session Oversight
            </h1>
            <p style={{ color: 'var(--text-muted)', fontSize: '0.875rem', margin: '0.25rem 0 0 0' }}>
              {totalElements} total student interview sessions conducted on the platform.
            </p>
          </div>

          <AdminNav />
        </div>

        {/* Filters */}
        <div className="card" style={{ padding: '0.875rem 1.25rem', marginBottom: '1.25rem' }}>
          <form onSubmit={handleApplyUserFilter} style={{ display: 'flex', gap: '0.875rem', flexWrap: 'wrap', alignItems: 'center' }}>
            <div style={{ minWidth: '180px' }}>
              <select
                value={filterStatus}
                onChange={(e) => { setFilterStatus(e.target.value); setPage(0); }}
                className="form-select"
                style={{ fontSize: '0.8125rem' }}
              >
                <option value="">All Statuses</option>
                {STATUSES.map(s => (
                  <option key={s} value={s}>{s}</option>
                ))}
              </select>
            </div>

            <div style={{ minWidth: '180px' }}>
              <input
                type="number"
                min="1"
                placeholder="Filter by user ID..."
                value={filterUserId}
                onChange={(e) => setFilterUserId(e.target.value)}
                className="form-input"
                style={{ fontSize: '0.8125rem' }}
              >
              </input>
            </div>

            <button type="submit" className="btn btn-secondary btn-sm">
              Search
            </button>

            {(filterStatus || filterUserId) && (
              <button type="button" onClick={handleResetFilters} className="btn btn-secondary btn-sm">
                Reset
              </button>
            )}
          </form>
        </div>

        {error && (
          <div className="alert alert-danger" style={{ marginBottom: '1.25rem' }}>
            {error}
          </div>
        )}

        {/* Sessions Table */}
        {loading ? (
          <div className="card" style={{ padding: '2.5rem', textAlign: 'center' }}>
            <div className="skeleton" style={{ width: '40%', height: '20px', margin: '0 auto 1rem auto' }} />
            <div className="skeleton" style={{ width: '70%', height: '14px', margin: '0 auto' }} />
          </div>
        ) : sessions.length === 0 ? (
          <div className="empty-state">
            <h3>No interview sessions found</h3>
            <p>Try adjusting your search criteria or status filter.</p>
          </div>
        ) : (
          <div className="card" style={{ padding: '1.25rem' }}>
            <div className="table-container">
              <table className="data-table">
                <thead>
                  <tr>
                    <th style={{ width: '60px' }}>ID</th>
                    <th>Candidate</th>
                    <th>Category</th>
                    <th>Difficulty</th>
                    <th>Status</th>
                    <th>Date</th>
                    <th>Score</th>
                    <th style={{ textAlign: 'right' }}>Action</th>
                  </tr>
                </thead>
                <tbody>
                  {sessions.map((s) => (
                    <tr key={s.sessionId}>
                      <td style={{ color: 'var(--text-muted)', fontFamily: 'var(--font-mono, monospace)', fontSize: '0.8125rem' }}>
                        #{s.sessionId}
                      </td>
                      <td>
                        <div style={{ fontWeight: 500, color: 'var(--text-primary)' }}>
                          {s.userFullName || 'Candidate'}
                        </div>
                        <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>
                          {s.userEmail} · ID #{s.userId}
                        </div>
                      </td>
                      <td style={{ color: 'var(--text-secondary)', fontSize: '0.8125rem' }}>
                        {s.categoryName}
                      </td>
                      <td style={{ color: 'var(--text-muted)', fontSize: '0.8125rem' }}>
                        {s.difficulty}
                      </td>
                      <td>
                        {getStatusBadge(s.status)}
                      </td>
                      <td style={{ color: 'var(--text-muted)', fontSize: '0.8125rem', whiteSpace: 'nowrap' }}>
                        {formatDate(s.createdAt)}
                      </td>
                      <td style={{ fontWeight: 600, fontSize: '0.8125rem' }}>
                        {s.averageScore != null ? (
                          <span style={{ color: s.averageScore >= 70 ? 'var(--success)' : s.averageScore >= 50 ? 'var(--primary)' : 'var(--danger)' }}>
                            {s.averageScore.toFixed(1)} / 100
                          </span>
                        ) : (
                          <span style={{ color: 'var(--text-muted)' }}>—</span>
                        )}
                      </td>
                      <td style={{ textAlign: 'right', whiteSpace: 'nowrap' }}>
                        {s.status === 'COMPLETED' ? (
                          <Link
                            to={`/interview/report/${s.sessionId}`}
                            className="btn btn-secondary btn-sm"
                          >
                            Report
                          </Link>
                        ) : (
                          <span style={{ color: 'var(--text-muted)', fontSize: '0.75rem' }}>Active</span>
                        )}
                      </td>
                    </tr>
                  ))}
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
      </div>
    </MainLayout>
  );
};

export default AdminSessionsPage;
