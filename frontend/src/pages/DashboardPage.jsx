import React, { useState, useEffect } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import MainLayout from '../layouts/MainLayout';
import { getDashboardSummary } from '../services/dashboardService';
import { getAnalyticsOverview, getCategoryAnalytics } from '../services/reportService';
import { 
  PlayIcon, 
  CpuIcon, 
  CodeIcon, 
  LayersIcon, 
  DocumentIcon, 
  BriefcaseIcon, 
  SparklesIcon, 
  ArrowRightIcon, 
  CheckIcon, 
  ClockIcon, 
  ChartIcon 
} from '../components/common/Icons';

const RECOMMENDED_TRACKS = [
  { name: 'System Design & Architecture', categoryId: 1, difficulty: 'MEDIUM', icon: CpuIcon, color: '#818cf8' },
  { name: 'DSA & Algorithmic Optimization', categoryId: 2, difficulty: 'MEDIUM', icon: CodeIcon, color: '#38bdf8' },
  { name: 'Core Backend & Concurrency', categoryId: 1, difficulty: 'HARD', icon: LayersIcon, color: '#fbbf24' },
  { name: 'Engineering Leadership & Tradeoffs', categoryId: 5, difficulty: 'EASY', icon: BriefcaseIcon, color: '#34d399' }
];

const DashboardPage = () => {
  const navigate = useNavigate();
  const [dashboardData, setDashboardData] = useState(null);
  const [analyticsOverview, setAnalyticsOverview] = useState(null);
  const [categoryAnalytics, setCategoryAnalytics] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [selectedSkill, setSelectedSkill] = useState(null);

  useEffect(() => {
    fetchDashboard();
  }, []);

  const fetchDashboard = async () => {
    setLoading(true);
    setError('');
    try {
      const [dash, overview, catAnalytics] = await Promise.all([
        getDashboardSummary(),
        getAnalyticsOverview().catch(() => null),
        getCategoryAnalytics().catch(() => [])
      ]);
      setDashboardData(dash);
      setAnalyticsOverview(overview);
      setCategoryAnalytics(catAnalytics || []);
      if (catAnalytics && catAnalytics.length > 0) {
        setSelectedSkill(catAnalytics[0]);
      }
    } catch (err) {
      console.error('Failed to load dashboard:', err);
      setError('Unable to load dashboard data. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  const getGreeting = () => {
    const hour = new Date().getHours();
    if (hour < 12) return 'Good morning';
    if (hour < 18) return 'Good afternoon';
    return 'Good evening';
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

  const avgScore = analyticsOverview?.averageOverallScore != null ? Number(analyticsOverview.averageOverallScore) : 0;
  const scorePercent = Math.min(100, Math.round((avgScore / 10) * 100));

  return (
    <MainLayout>
      <div style={{ maxWidth: '1140px', margin: '0 auto', paddingBottom: '3.5rem' }}>
        {loading ? (
          <div className="card" style={{ padding: '3.5rem 1.5rem', textAlign: 'center' }}>
            <div className="skeleton" style={{ width: '40%', height: '24px', margin: '0 auto 1.25rem auto' }} />
            <div className="skeleton" style={{ width: '65%', height: '14px', margin: '0 auto' }} />
          </div>
        ) : error ? (
          <div className="alert alert-danger" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <div>{error}</div>
            <button onClick={fetchDashboard} className="btn btn-secondary btn-sm">Try again</button>
          </div>
        ) : (
          <>
            {/* HERO COMMAND CENTER: Candidate Readiness & Quick Launch */}
            <div className="card card-hero" style={{ padding: '2.25rem', marginBottom: '2rem', borderRadius: 'var(--radius-lg)' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '2rem' }}>
                <div style={{ maxWidth: '620px' }}>
                  <div style={{ display: 'inline-flex', alignItems: 'center', gap: '0.45rem', padding: '0.2rem 0.75rem', borderRadius: 'var(--radius-full)', background: 'rgba(99, 102, 241, 0.15)', border: '1px solid rgba(99, 102, 241, 0.3)', marginBottom: '0.875rem' }}>
                    <span className="ai-pulse-dot" />
                    <span style={{ fontSize: '0.75rem', fontWeight: 600, color: 'var(--text-accent)', letterSpacing: '0.04em', textTransform: 'uppercase' }}>
                      Career Command Center
                    </span>
                  </div>

                  <h1 style={{ fontSize: '1.9rem', fontWeight: 800, letterSpacing: '-0.025em', margin: '0 0 0.5rem 0', color: '#ffffff' }}>
                    {getGreeting()}, {dashboardData?.user?.fullName?.split(' ')[0] || 'Candidate'}
                  </h1>
                  <p style={{ color: 'var(--text-secondary)', fontSize: '0.9375rem', lineHeight: 1.6, margin: '0 0 1.5rem 0' }}>
                    Candidate Profile: <strong style={{ color: '#ffffff' }}>{dashboardData?.profile?.targetRole || 'Software Engineer'}</strong> &bull; Level: <span style={{ color: 'var(--accent-cyan)' }}>{dashboardData?.profile?.experienceLevel || 'Fresher'}</span>. Practice adaptive technical rounds to benchmark your readiness.
                  </p>

                  <div style={{ display: 'flex', gap: '0.75rem', flexWrap: 'wrap' }}>
                    <Link to="/interview/setup" className="btn btn-primary btn-lg">
                      <PlayIcon size={16} />
                      <span>Start Mock Interview</span>
                    </Link>
                    <Link to="/resume-templates" className="btn btn-secondary btn-lg">
                      <SparklesIcon size={16} color="var(--primary)" />
                      <span>ATS Resume Builder</span>
                    </Link>
                    <Link to="/resume-projects" className="btn btn-secondary btn-lg">
                      <DocumentIcon size={16} />
                      <span>Resumes & Projects</span>
                    </Link>
                  </div>
                </div>

                {/* Circular Score Gauge */}
                <div style={{
                  display: 'flex',
                  alignItems: 'center',
                  gap: '1.5rem',
                  background: 'rgba(15, 23, 42, 0.65)',
                  padding: '1.25rem 1.75rem',
                  borderRadius: 'var(--radius-md)',
                  border: '1px solid rgba(255, 255, 255, 0.08)',
                  backdropFilter: 'blur(8px)'
                }}>
                  <div style={{ position: 'relative', width: '92px', height: '92px' }}>
                    <svg viewBox="0 0 36 36" style={{ width: '100%', height: '100%', transform: 'rotate(-90deg)' }}>
                      <path
                        d="M18 2.0845 a 15.9155 15.9155 0 0 1 0 31.831 a 15.9155 15.9155 0 0 1 0 -31.831"
                        fill="none"
                        stroke="rgba(255, 255, 255, 0.08)"
                        strokeWidth="3.5"
                      />
                      <path
                        d="M18 2.0845 a 15.9155 15.9155 0 0 1 0 31.831 a 15.9155 15.9155 0 0 1 0 -31.831"
                        fill="none"
                        stroke="url(#scoreGrad)"
                        strokeWidth="3.5"
                        strokeDasharray={`${scorePercent}, 100`}
                        strokeLinecap="round"
                      />
                      <defs>
                        <linearGradient id="scoreGrad" x1="0%" y1="0%" x2="100%" y2="100%">
                          <stop offset="0%" stopColor="#6366f1" />
                          <stop offset="100%" stopColor="#38bdf8" />
                        </linearGradient>
                      </defs>
                    </svg>
                    <div style={{
                      position: 'absolute',
                      top: 0, left: 0, right: 0, bottom: 0,
                      display: 'flex',
                      flexDirection: 'column',
                      alignItems: 'center',
                      justifyContent: 'center'
                    }}>
                      <span style={{ fontSize: '1.4rem', fontWeight: 800, color: '#ffffff', lineHeight: 1 }}>
                        {avgScore > 0 ? avgScore.toFixed(1) : '—'}
                      </span>
                      <span style={{ fontSize: '0.625rem', color: 'var(--text-muted)', textTransform: 'uppercase', marginTop: '2px' }}>
                        out of 10
                      </span>
                    </div>
                  </div>

                  <div>
                    <div style={{ fontSize: '0.725rem', fontWeight: 600, color: 'var(--text-muted)', textTransform: 'uppercase', letterSpacing: '0.05em' }}>
                      Readiness Index
                    </div>
                    <div style={{ fontSize: '1.1rem', fontWeight: 700, color: '#ffffff', margin: '0.2rem 0' }}>
                      {avgScore >= 8 ? 'Interview Ready' : avgScore >= 6 ? 'Competitive' : avgScore > 0 ? 'Developing' : 'Not Evaluated'}
                    </div>
                    <div style={{ fontSize: '0.75rem', color: 'var(--text-secondary)' }}>
                      Based on {dashboardData?.statistics?.completedInterviews || 0} evaluated sessions
                    </div>
                  </div>
                </div>
              </div>
            </div>

            {/* INTERACTIVE PREPARATION WORKFLOW TRACK */}
            <div style={{ marginBottom: '2.25rem' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.875rem' }}>
                <span style={{ fontSize: '0.75rem', fontWeight: 600, color: 'var(--text-muted)', textTransform: 'uppercase', letterSpacing: '0.06em' }}>
                  Preparation Workflow
                </span>
                <span style={{ fontSize: '0.75rem', color: 'var(--accent-cyan)' }}>
                  Active Engineering Sandbox
                </span>
              </div>

              <div className="journey-track">
                <div className="journey-step completed">
                  <CheckIcon size={14} color="var(--accent-teal)" />
                  <span>1. Configure Mode</span>
                </div>
                <div className="journey-step active">
                  <span className="ai-pulse-dot" style={{ width: '6px', height: '6px' }} />
                  <span>2. Realistic Simulation</span>
                </div>
                <div className="journey-step">
                  <span>3. Multi-Dimension Grading</span>
                </div>
                <div className="journey-step">
                  <span>4. Targeted Action Plan</span>
                </div>
              </div>
            </div>

            {/* 4 METRIC STAT CARDS */}
            <div style={{
              display: 'grid',
              gridTemplateColumns: 'repeat(auto-fit, minmax(230px, 1fr))',
              gap: '1rem',
              marginBottom: '2.25rem'
            }}>
              <div className="stat-card">
                <div className="stat-label">Total Sessions</div>
                <div className="stat-value">{dashboardData?.statistics?.totalInterviews || 0}</div>
                <div style={{ fontSize: '0.8125rem', color: 'var(--text-secondary)' }}>
                  <strong style={{ color: 'var(--text-primary)' }}>{dashboardData?.statistics?.completedInterviews || 0}</strong> fully evaluated
                </div>
              </div>

              <div className="stat-card">
                <div className="stat-label">Average Score</div>
                <div className="stat-value" style={{ color: avgScore >= 7 ? 'var(--text-primary)' : 'var(--warning)' }}>
                  {avgScore > 0 ? `${avgScore.toFixed(1)}/10` : '—'}
                </div>
                <div style={{ fontSize: '0.8125rem', color: 'var(--text-secondary)' }}>
                  Peak: <strong style={{ color: 'var(--text-primary)' }}>{analyticsOverview?.bestOverallScore || '—'}</strong>/10
                </div>
              </div>

              <div className="stat-card">
                <div className="stat-label">Completion Velocity</div>
                <div className="stat-value" style={{ color: '#38bdf8' }}>
                  {analyticsOverview?.completionRatePercentage != null ? `${analyticsOverview.completionRatePercentage}%` : '0%'}
                </div>
                <div style={{ fontSize: '0.8125rem', color: 'var(--text-secondary)' }}>
                  Finished vs initiated mocks
                </div>
              </div>

              <div className="stat-card">
                <div className="stat-label">Domains Practiced</div>
                <div className="stat-value" style={{ color: '#a855f7' }}>
                  {categoryAnalytics.length}
                </div>
                <div style={{ fontSize: '0.8125rem', color: 'var(--text-secondary)' }}>
                  Active engineering domains
                </div>
              </div>
            </div>

            {/* RECOMMENDED PRACTICE TRACKS */}
            <div style={{ marginBottom: '2.5rem' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1rem' }}>
                <div>
                  <h2 style={{ fontSize: '1.15rem', fontWeight: 600, margin: 0, color: 'var(--text-primary)' }}>
                    Recommended Practice Tracks
                  </h2>
                  <p style={{ color: 'var(--text-muted)', fontSize: '0.8125rem', margin: '0.2rem 0 0 0' }}>
                    Instantly launch targeted mock interviews tailored to your target engineering profile.
                  </p>
                </div>
                <Link to="/interview/setup" style={{ color: 'var(--text-accent)', fontSize: '0.8125rem', fontWeight: 500, display: 'flex', alignItems: 'center', gap: '0.25rem' }}>
                  Customize Setup <ArrowRightIcon size={13} />
                </Link>
              </div>

              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(240px, 1fr))', gap: '1rem' }}>
                {RECOMMENDED_TRACKS.map((track, i) => {
                  const Icon = track.icon;
                  return (
                    <div
                      key={i}
                      onClick={() => navigate(`/interview/setup?categoryId=${track.categoryId}&difficulty=${track.difficulty}`)}
                      className="card card-interactive"
                      style={{ padding: '1.25rem', display: 'flex', flexDirection: 'column', justifyContent: 'space-between' }}
                    >
                      <div>
                        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '0.75rem' }}>
                          <div style={{ width: '36px', height: '36px', borderRadius: 'var(--radius-sm)', background: 'var(--surface-raised)', border: '1px solid var(--border)', display: 'flex', alignItems: 'center', justifyContent: 'center', color: track.color }}>
                            <Icon size={18} />
                          </div>
                          <span className="badge badge-secondary" style={{ fontSize: '0.7rem' }}>
                            {track.difficulty}
                          </span>
                        </div>
                        <h3 style={{ fontSize: '0.9375rem', fontWeight: 600, color: 'var(--text-primary)', margin: '0 0 0.35rem 0' }}>
                          {track.name}
                        </h3>
                        <p style={{ color: 'var(--text-muted)', fontSize: '0.75rem', margin: 0 }}>
                          5 interactive questions with real-time AI follow-ups.
                        </p>
                      </div>

                      <div style={{ marginTop: '1rem', paddingTop: '0.75rem', borderTop: '1px solid var(--border-light)', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                        <span style={{ fontSize: '0.75rem', fontWeight: 500, color: 'var(--text-accent)' }}>Start Practice</span>
                        <ArrowRightIcon size={13} color="var(--text-accent)" />
                      </div>
                    </div>
                  );
                })}
              </div>
            </div>

            {/* PERFORMANCE INTELLIGENCE / SKILL BREAKDOWN */}
            {categoryAnalytics && categoryAnalytics.length > 0 && (
              <div className="card" style={{ padding: '1.5rem', marginBottom: '2.5rem' }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', flexWrap: 'wrap', gap: '1rem', marginBottom: '1.25rem' }}>
                  <div>
                    <h2 style={{ fontSize: '1.15rem', fontWeight: 600, margin: 0, color: 'var(--text-primary)' }}>
                      Skill Mastery Matrix
                    </h2>
                    <p style={{ color: 'var(--text-muted)', fontSize: '0.8125rem', margin: '0.2rem 0 0 0' }}>
                      Average technical performance scored across mock interview categories.
                    </p>
                  </div>
                </div>

                <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(260px, 1fr))', gap: '1rem' }}>
                  {categoryAnalytics.map((cat) => {
                    const score = cat.averageScore != null ? Number(cat.averageScore) : 0;
                    const pct = Math.min(100, Math.round((score / 10) * 100));
                    return (
                      <div
                        key={cat.categoryId}
                        onClick={() => setSelectedSkill(cat)}
                        style={{
                          background: selectedSkill?.categoryId === cat.categoryId ? 'var(--surface-raised)' : 'var(--surface)',
                          border: `1px solid ${selectedSkill?.categoryId === cat.categoryId ? 'var(--primary)' : 'var(--border)'}`,
                          borderRadius: 'var(--radius-sm)',
                          padding: '1rem',
                          cursor: 'pointer',
                          transition: 'all var(--transition-base)'
                        }}
                      >
                        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.5rem' }}>
                          <span style={{ fontWeight: 600, fontSize: '0.875rem', color: 'var(--text-primary)' }}>
                            {cat.categoryName}
                          </span>
                          <span style={{ fontSize: '0.875rem', fontWeight: 700, color: score >= 7 ? 'var(--success)' : score >= 5 ? 'var(--warning)' : 'var(--danger)' }}>
                            {score > 0 ? `${score.toFixed(1)}/10` : '—'}
                          </span>
                        </div>

                        {/* Progress Bar */}
                        <div style={{ width: '100%', height: '6px', background: 'var(--surface-hover)', borderRadius: '3px', overflow: 'hidden' }}>
                          <div style={{
                            width: `${pct}%`,
                            height: '100%',
                            background: score >= 7 ? 'linear-gradient(90deg, #10b981, #34d399)' : score >= 5 ? 'linear-gradient(90deg, #f59e0b, #fbbf24)' : 'linear-gradient(90deg, #f43f5e, #fb7185)',
                            borderRadius: '3px',
                            transition: 'width 0.6s ease'
                          }} />
                        </div>

                        <div style={{ display: 'flex', justifyContent: 'space-between', marginTop: '0.5rem', fontSize: '0.75rem', color: 'var(--text-muted)' }}>
                          <span>{cat.sessionsCount || 0} sessions completed</span>
                          <span>{pct}% proficiency</span>
                        </div>
                      </div>
                    );
                  })}
                </div>
              </div>
            )}

            {/* RECENT INTERVIEW SESSIONS TABLE */}
            <div className="card" style={{ padding: '1.5rem' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.25rem' }}>
                <div>
                  <h2 style={{ fontSize: '1.15rem', fontWeight: 600, margin: 0, color: 'var(--text-primary)' }}>
                    Recent Interview Activity
                  </h2>
                  <p style={{ color: 'var(--text-muted)', fontSize: '0.8125rem', margin: '0.2rem 0 0 0' }}>
                    Inspect past questions, adaptive evaluations, and candidate transcripts.
                  </p>
                </div>
                <Link to="/interview/history" style={{ color: 'var(--text-accent)', fontSize: '0.8125rem', fontWeight: 500, display: 'flex', alignItems: 'center', gap: '0.25rem' }}>
                  <span>View All ({dashboardData?.recentSessions?.length || 0})</span>
                  <ArrowRightIcon size={13} />
                </Link>
              </div>

              {!dashboardData?.recentSessions || dashboardData.recentSessions.length === 0 ? (
                <div className="empty-state">
                  <h3>No interviews recorded yet</h3>
                  <p>Start your first mock interview to receive real-time AI feedback and comprehensive performance reports.</p>
                  <Link to="/interview/setup" className="btn btn-primary btn-sm">
                    Configure your first interview
                  </Link>
                </div>
              ) : (
                <div className="table-container">
                  <table className="data-table">
                    <thead>
                      <tr>
                        <th>Category</th>
                        <th>Mode</th>
                        <th>Difficulty</th>
                        <th>Questions</th>
                        <th>Status</th>
                        <th>Date</th>
                        <th style={{ textAlign: 'right' }}>Action</th>
                      </tr>
                    </thead>
                    <tbody>
                      {dashboardData.recentSessions.slice(0, 5).map((session) => (
                        <tr key={session.id}>
                          <td style={{ fontWeight: 600, color: 'var(--text-primary)' }}>
                            {session.categoryName || 'General'}
                          </td>
                          <td>
                            <span className="badge badge-secondary" style={{ fontSize: '0.75rem' }}>
                              {session.mode === 'AI' ? 'Adaptive AI' : session.mode === 'PROJECT' ? 'Project' : session.mode === 'RESUME' ? 'Resume' : session.mode === 'JOB_DESCRIPTION' ? 'Job Description' : session.mode === 'CODING' ? 'Coding DSA' : 'Question Bank'}
                            </span>
                          </td>
                          <td style={{ color: 'var(--text-secondary)', fontSize: '0.8125rem' }}>
                            {session.difficulty}
                          </td>
                          <td style={{ color: 'var(--text-secondary)', fontSize: '0.8125rem' }}>
                            {session.questionCount} Qs
                          </td>
                          <td>
                            {getStatusBadge(session.status)}
                          </td>
                          <td style={{ color: 'var(--text-muted)', fontSize: '0.8125rem' }}>
                            {session.createdAt ? new Date(session.createdAt).toLocaleDateString() : '—'}
                          </td>
                          <td style={{ textAlign: 'right' }}>
                            {session.status === 'COMPLETED' ? (
                              <Link to={`/interview/report/${session.id}`} className="btn btn-secondary btn-sm">
                                View Report
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
          </>
        )}
      </div>
    </MainLayout>
  );
};

export default DashboardPage;

