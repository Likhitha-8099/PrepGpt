import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import MainLayout from '../layouts/MainLayout';
import { getInterviewSessions } from '../services/interviewService';
import { TerminalIcon, PlayIcon, ArrowRightIcon, CheckIcon } from '../components/common/Icons';

const InterviewHistoryPage = () => {
  const [sessions, setSessions] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    fetchSessions();
  }, []);

  const fetchSessions = async () => {
    setLoading(true);
    setError('');
    try {
      const data = await getInterviewSessions();
      setSessions(data || []);
    } catch (err) {
      console.error('Failed to load interview history:', err);
      setError('Unable to load interview sessions. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  const getStatusBadge = (status) => {
    switch (status) {
      case 'COMPLETED':
        return <span className="badge badge-success">Completed</span>;
      case 'IN_PROGRESS':
        return <span className="badge badge-warning">In Progress</span>;
      case 'CONFIGURED':
        return <span className="badge badge-secondary">Ready</span>;
      case 'ABANDONED':
        return <span className="badge badge-danger">Abandoned</span>;
      default:
        return <span className="badge badge-secondary">{status}</span>;
    }
  };

  const getModeLabel = (mode) => {
    switch (mode) {
      case 'AI': return 'Adaptive AI';
      case 'PROJECT': return 'Project';
      case 'RESUME': return 'Resume';
      case 'JOB_DESCRIPTION': return 'Job Description';
      case 'CODING': return 'Coding DSA';
      default: return 'Question Bank';
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
      <div style={{ maxWidth: '1080px', margin: '0 auto', paddingBottom: '3rem' }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '1.75rem', flexWrap: 'wrap', gap: '1rem' }}>
          <div>
            <div style={{ display: 'inline-flex', alignItems: 'center', gap: '0.45rem', padding: '0.2rem 0.65rem', borderRadius: 'var(--radius-full)', background: 'rgba(99, 102, 241, 0.1)', border: '1px solid rgba(99, 102, 241, 0.25)', marginBottom: '0.4rem' }}>
              <TerminalIcon size={14} />
              <span style={{ fontSize: '0.75rem', fontWeight: 600, color: 'var(--text-accent)', textTransform: 'uppercase' }}>
                Session Ledger
              </span>
            </div>
            <h1 style={{ fontSize: '1.75rem', fontWeight: 700, letterSpacing: '-0.02em', margin: 0, color: '#ffffff' }}>
              Interview History & Archives
            </h1>
            <p style={{ color: 'var(--text-muted)', fontSize: '0.875rem', margin: '0.25rem 0 0 0' }}>
              Archive of all configured, active, and completed mock interview evaluations.
            </p>
          </div>

          <Link to="/interview/setup" className="btn btn-primary">
            <PlayIcon size={15} />
            <span>Start New Interview</span>
          </Link>
        </div>

        {loading ? (
          <div className="card" style={{ padding: '2.5rem', textAlign: 'center' }}>
            <div className="skeleton" style={{ width: '40%', height: '20px', margin: '0 auto 1rem auto' }} />
            <div className="skeleton" style={{ width: '70%', height: '14px', margin: '0 auto' }} />
          </div>
        ) : error ? (
          <div className="card" style={{ padding: '2rem', textAlign: 'center' }}>
            <div className="alert alert-danger" style={{ marginBottom: '1rem' }}>{error}</div>
            <button onClick={fetchSessions} className="btn btn-secondary">Retry</button>
          </div>
        ) : sessions.length === 0 ? (
          <div className="empty-state">
            <h3>No interviews yet</h3>
            <p>Your completed and in-progress interview sessions will appear here with performance dossiers.</p>
            <Link to="/interview/setup" className="btn btn-primary">
              Start your first interview
            </Link>
          </div>
        ) : (
          <div className="table-container">
            <table className="data-table">
              <thead>
                <tr>
                  <th style={{ width: '70px' }}>ID</th>
                  <th>Category</th>
                  <th>Mode</th>
                  <th>Difficulty</th>
                  <th>Questions</th>
                  <th>Duration</th>
                  <th>Status</th>
                  <th>Date</th>
                  <th style={{ textAlign: 'right' }}>Action</th>
                </tr>
              </thead>
              <tbody>
                {sessions.map((session) => (
                  <tr key={session.id}>
                    <td style={{ color: 'var(--text-muted)', fontFamily: 'var(--font-mono)', fontSize: '0.8125rem' }}>
                      #{session.id}
                    </td>
                    <td style={{ fontWeight: 600, color: 'var(--text-primary)' }}>
                      {session.categoryName || 'General'}
                    </td>
                    <td>
                      <div style={{ display: 'flex', gap: '0.35rem', flexWrap: 'wrap' }}>
                        <span className="badge badge-secondary" style={{ fontSize: '0.75rem' }}>
                          {getModeLabel(session.mode)}
                        </span>
                        {session.interactionMode === 'VOICE' && (
                          <span className="badge badge-info" style={{ fontSize: '0.75rem' }}>
                            Voice
                          </span>
                        )}
                      </div>
                    </td>
                    <td style={{ color: 'var(--text-secondary)', fontSize: '0.8125rem' }}>
                      {session.difficulty}
                    </td>
                    <td style={{ color: 'var(--text-secondary)', fontSize: '0.8125rem' }}>
                      {session.questionCount} Qs
                    </td>
                    <td style={{ color: 'var(--text-secondary)', fontSize: '0.8125rem' }}>
                      {session.durationMinutes}m
                    </td>
                    <td>
                      {getStatusBadge(session.status)}
                    </td>
                    <td style={{ color: 'var(--text-muted)', fontSize: '0.8125rem', whiteSpace: 'nowrap' }}>
                      {formatDate(session.createdAt)}
                    </td>
                    <td style={{ textAlign: 'right' }}>
                      {session.status === 'COMPLETED' ? (
                        <Link to={`/interview/report/${session.id}`} className="btn btn-secondary btn-sm">
                          View Dossier
                        </Link>
                      ) : session.mode === 'CODING' ? (
                        <Link to={`/interview/coding/${session.id}`} className="btn btn-primary btn-sm">
                          Open IDE
                        </Link>
                      ) : (
                        <Link to={`/interview/${session.id}`} className="btn btn-primary btn-sm">
                          Resume
                        </Link>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </MainLayout>
  );
};

export default InterviewHistoryPage;

