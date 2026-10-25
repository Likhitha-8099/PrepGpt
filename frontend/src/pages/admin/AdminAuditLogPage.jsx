import React, { useState, useEffect } from 'react';
import MainLayout from '../../layouts/MainLayout';
import AdminNav from '../../components/AdminNav';
import { getAuditLog } from '../../services/adminService';

const AdminAuditLogPage = () => {
  const [logs, setLogs] = useState([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(1);
  const [totalElements, setTotalElements] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    fetchLogs(page);
  }, [page]);

  const fetchLogs = async (pageNum) => {
    setLoading(true);
    setError('');
    try {
      const res = await getAuditLog(pageNum, 20);
      const data = res.data;
      setLogs(data.entries || data.content || []);
      setTotalPages(data.totalPages || 1);
      setTotalElements(data.totalElements || 0);
    } catch (err) {
      console.error('Failed to load audit logs:', err);
      setError('Failed to load administrative audit events.');
    } finally {
      setLoading(false);
    }
  };

  const formatDate = (isoString) => {
    if (!isoString) return '—';
    return new Date(isoString).toLocaleString('en-US', {
      month: 'short',
      day: 'numeric',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit'
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
              Audit Trail
            </h1>
            <p style={{ color: 'var(--text-muted)', fontSize: '0.875rem', margin: '0.25rem 0 0 0' }}>
              {totalElements} recorded compliance and administrative events in immutable storage.
            </p>
          </div>

          <AdminNav />
        </div>

        {/* Security Notice */}
        <div className="card" style={{ padding: '0.875rem 1.25rem', marginBottom: '1.25rem', background: 'var(--bg-secondary)', border: '1px solid var(--border)', fontSize: '0.8125rem', color: 'var(--text-secondary)' }}>
          <strong style={{ color: 'var(--text-primary)' }}>Audit Integrity:</strong> Privileged events (role modifications, account state toggles, question curation) are captured synchronously and cannot be modified or deleted via API.
        </div>

        {error && (
          <div className="alert alert-danger" style={{ marginBottom: '1.25rem' }}>
            {error}
          </div>
        )}

        {/* Audit Table */}
        {loading ? (
          <div className="card" style={{ padding: '2.5rem', textAlign: 'center' }}>
            <div className="skeleton" style={{ width: '40%', height: '20px', margin: '0 auto 1rem auto' }} />
            <div className="skeleton" style={{ width: '70%', height: '14px', margin: '0 auto' }} />
          </div>
        ) : logs.length === 0 ? (
          <div className="empty-state">
            <h3>No audit records</h3>
            <p>Administrative events will be logged here as actions occur.</p>
          </div>
        ) : (
          <div className="card" style={{ padding: '1.25rem' }}>
            <div className="table-container">
              <table className="data-table">
                <thead>
                  <tr>
                    <th style={{ width: '180px' }}>Timestamp</th>
                    <th>Administrator</th>
                    <th>Action</th>
                    <th>Target Entity</th>
                    <th>Details</th>
                  </tr>
                </thead>
                <tbody>
                  {logs.map((log) => (
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
                      <td style={{ color: 'var(--text-muted)', fontSize: '0.8125rem', maxWidth: '360px', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                        {log.details || '—'}
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

export default AdminAuditLogPage;
