import React, { useState, useEffect } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import MainLayout from '../layouts/MainLayout';
import { getInterviewReport } from '../services/reportService';
import { 
  TerminalIcon, 
  CpuIcon, 
  CheckIcon, 
  WarningIcon, 
  ArrowRightIcon, 
  ChartIcon, 
  DocumentIcon 
} from '../components/common/Icons';

const InterviewReportPage = () => {
  const { id } = useParams();
  const navigate = useNavigate();

  const [report, setReport] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [expandedQuestion, setExpandedQuestion] = useState(null);

  useEffect(() => {
    fetchReport();
  }, [id]);

  const fetchReport = async () => {
    setLoading(true);
    setError('');
    try {
      const data = await getInterviewReport(id);
      setReport(data);
      if (data?.questions && data.questions.length > 0) {
        setExpandedQuestion(data.questions[0].questionOrder);
      }
    } catch (err) {
      console.error('Failed to fetch interview report:', err);
      if (err.response?.status === 404) {
        setError('Interview report not found or you do not have permission to access it.');
      } else {
        setError(err.response?.data?.message || 'Failed to load interview report.');
      }
    } finally {
      setLoading(false);
    }
  };

  const handlePrint = () => {
    window.print();
  };

  const getScoreColor = (score) => {
    if (score >= 8.0) return '#34d399';
    if (score >= 6.5) return '#818cf8';
    if (score >= 5.0) return '#fbbf24';
    return '#fb7185';
  };

  const getPerformanceBadge = (level) => {
    switch (level) {
      case 'EXCELLENT':
        return <span className="badge badge-success">Top Percentile · Excellent</span>;
      case 'STRONG':
        return <span className="badge badge-info">Competitive · Strong</span>;
      case 'GOOD':
        return <span className="badge badge-warning">Proficient · Good</span>;
      default:
        return <span className="badge badge-danger">{level || 'Needs Review'}</span>;
    }
  };

  const overallScore = report?.overallScore != null ? Number(report.overallScore) : 0;
  const scorePercent = Math.min(100, Math.round((overallScore / 10) * 100));

  return (
    <MainLayout>
      <div style={{ maxWidth: '980px', margin: '0 auto', paddingBottom: '3.5rem' }}>
        {loading ? (
          <div className="card" style={{ padding: '4rem 2rem', textAlign: 'center' }}>
            <div className="ai-pulse-dot" style={{ width: '16px', height: '16px', margin: '0 auto 1.5rem auto' }} />
            <h2 style={{ fontSize: '1.2rem', color: 'var(--text-primary)', marginBottom: '0.35rem' }}>
              Synthesizing Comprehensive Evaluation Report...
            </h2>
            <p style={{ color: 'var(--text-muted)', fontSize: '0.8125rem', margin: 0 }}>
              Aggregating technical correctness, communicative clarity, and adaptive question depth.
            </p>
          </div>
        ) : error ? (
          <div className="card" style={{ padding: '2.5rem', textAlign: 'center' }}>
            <div className="alert alert-danger" style={{ marginBottom: '1.25rem' }}>{error}</div>
            <button onClick={() => navigate('/interview/history')} className="btn btn-secondary">
              Back to History
            </button>
          </div>
        ) : (
          <div>
            {/* Top Navigation & Export Actions */}
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '1.75rem', flexWrap: 'wrap', gap: '1rem' }} className="no-print">
              <div>
                <Link to="/interview/history" style={{ color: 'var(--text-muted)', fontSize: '0.8125rem', textDecoration: 'none', display: 'inline-flex', alignItems: 'center', gap: '0.35rem', marginBottom: '0.4rem' }}>
                  <span>←</span> Back to Interview History
                </Link>
                <h1 style={{ fontSize: '1.8rem', fontWeight: 800, letterSpacing: '-0.025em', margin: 0, color: '#ffffff' }}>
                  Candidate Assessment Dossier
                </h1>
                <p style={{ color: 'var(--text-muted)', fontSize: '0.875rem', margin: '0.25rem 0 0 0' }}>
                  Evaluated {report?.completedAt ? new Date(report.completedAt).toLocaleDateString(undefined, { year: 'numeric', month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' }) : 'Recently'}
                </p>
              </div>

              <div style={{ display: 'flex', gap: '0.625rem' }}>
                <button type="button" onClick={handlePrint} className="btn btn-secondary">
                  Export Dossier / Print
                </button>
                <button type="button" onClick={() => navigate('/interview/setup')} className="btn btn-primary">
                  Start New Interview →
                </button>
              </div>
            </div>

            {/* Print Header */}
            <div className="print-only" style={{ display: 'none', marginBottom: '1.5rem', borderBottom: '1px solid #e2e8f0', paddingBottom: '0.75rem' }}>
              <h2 style={{ margin: 0, fontSize: '1.25rem' }}>PrepGPT Candidate Performance Intelligence Report</h2>
              <p style={{ margin: '0.25rem 0 0 0', color: '#64748b', fontSize: '0.8125rem' }}>Generated on {new Date().toLocaleDateString()}</p>
            </div>

            {/* HERO OVERALL SCORE & RADIAL GAUGE CARD */}
            <div className="card card-hero" style={{ marginBottom: '1.75rem', padding: '2rem 2.25rem', borderRadius: 'var(--radius-lg)' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '2rem', marginBottom: '1.75rem' }}>
                <div>
                  <div style={{ display: 'flex', alignItems: 'center', gap: '0.65rem', marginBottom: '0.65rem', flexWrap: 'wrap' }}>
                    <h2 style={{ fontSize: '1.4rem', fontWeight: 700, margin: 0, color: '#ffffff' }}>
                      {report?.projectTitle ? `${report.projectTitle} · ${report.categoryName}` : report?.categoryName}
                    </h2>
                    {getPerformanceBadge(report?.performanceLevel)}
                    <span className="badge badge-secondary">
                      {report?.mode === 'AI' ? 'Adaptive AI' : report?.mode === 'PROJECT' ? 'Project' : report?.mode === 'RESUME' ? 'Resume' : report?.mode === 'JOB_DESCRIPTION' ? 'Job Description' : report?.mode === 'CODING' ? 'Coding DSA' : 'Question Bank'}
                    </span>
                    {report?.interactionMode === 'VOICE' && (
                      <span className="badge badge-info">Voice Mode</span>
                    )}
                  </div>
                  <div style={{ display: 'flex', gap: '1.5rem', color: 'var(--text-secondary)', fontSize: '0.875rem', flexWrap: 'wrap' }}>
                    <span>Target Depth: <strong style={{ color: '#ffffff' }}>{report?.difficulty}</strong></span>
                    <span>Completed: <strong style={{ color: '#ffffff' }}>{report?.answeredQuestions} of {report?.totalQuestions} prompts</strong> ({report?.completionRatePercentage}%)</span>
                  </div>
                </div>

                {/* Circular Score Visualizer */}
                <div style={{ display: 'flex', alignItems: 'center', gap: '1.5rem' }}>
                  <div style={{ position: 'relative', width: '96px', height: '96px' }}>
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
                        stroke={getScoreColor(overallScore)}
                        strokeWidth="3.5"
                        strokeDasharray={`${scorePercent}, 100`}
                        strokeLinecap="round"
                      />
                    </svg>
                    <div style={{
                      position: 'absolute',
                      top: 0, left: 0, right: 0, bottom: 0,
                      display: 'flex',
                      flexDirection: 'column',
                      alignItems: 'center',
                      justifyContent: 'center'
                    }}>
                      <span style={{ fontSize: '1.6rem', fontWeight: 800, color: '#ffffff', lineHeight: 1 }}>
                        {report?.overallScore != null ? report.overallScore.toFixed(1) : '—'}
                      </span>
                      <span style={{ fontSize: '0.65rem', color: 'var(--text-muted)', textTransform: 'uppercase', marginTop: '2px' }}>
                        out of 10
                      </span>
                    </div>
                  </div>

                  <div>
                    <div style={{ fontSize: '0.75rem', fontWeight: 600, color: 'var(--text-muted)', textTransform: 'uppercase', letterSpacing: '0.04em' }}>
                      Overall Score
                    </div>
                    <div style={{ fontSize: '1.1rem', fontWeight: 700, color: '#ffffff', margin: '0.2rem 0' }}>
                      {overallScore >= 8 ? 'Exceeds Expectations' : overallScore >= 6.5 ? 'Meets Bar' : 'Needs Reinforcement'}
                    </div>
                    <div style={{ fontSize: '0.75rem', color: 'var(--text-secondary)' }}>
                      Evaluated across 4 multi-dimensional criteria
                    </div>
                  </div>
                </div>
              </div>

              {/* Multi-Dimension Attribute Bars */}
              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(180px, 1fr))', gap: '1rem', paddingTop: '1.5rem', borderTop: '1px solid rgba(255, 255, 255, 0.08)' }}>
                {[
                  { label: 'Technical Accuracy', score: report?.technicalAccuracyScore, color: '#818cf8' },
                  { label: 'Conceptual Correctness', score: report?.correctnessScore, color: '#34d399' },
                  { label: 'Response Relevance', score: report?.relevanceScore, color: '#38bdf8' },
                  { label: 'Communicative Clarity', score: report?.clarityScore, color: '#fbbf24' }
                ].map((dim, i) => {
                  const val = dim.score != null ? Number(dim.score) : 0;
                  const pct = Math.min(100, Math.round((val / 10) * 100));
                  return (
                    <div key={i} style={{ background: 'rgba(15, 23, 42, 0.5)', padding: '0.875rem 1rem', borderRadius: 'var(--radius-sm)', border: '1px solid var(--border)' }}>
                      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.4rem' }}>
                        <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>{dim.label}</span>
                        <span style={{ fontSize: '0.875rem', fontWeight: 700, color: '#ffffff' }}>{val > 0 ? `${val.toFixed(1)}/10` : '—'}</span>
                      </div>
                      <div style={{ width: '100%', height: '5px', background: 'var(--surface-hover)', borderRadius: '3px', overflow: 'hidden' }}>
                        <div style={{ width: `${pct}%`, height: '100%', background: dim.color, borderRadius: '3px', transition: 'width 0.6s ease' }} />
                      </div>
                    </div>
                  );
                })}
              </div>
            </div>

            {/* STRENGTHS & GROWTH AREAS */}
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(360px, 1fr))', gap: '1.25rem', marginBottom: '2rem' }}>
              <div className="card" style={{ padding: '1.5rem', borderLeft: '3px solid var(--success)' }}>
                <div style={{ fontSize: '0.9375rem', fontWeight: 600, color: '#ffffff', marginBottom: '0.875rem', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                  <CheckIcon size={16} color="var(--success)" />
                  <span>Validated Technical Strengths</span>
                </div>
                {report?.keyStrengths && report.keyStrengths.length > 0 ? (
                  <ul style={{ margin: 0, paddingLeft: '1.2rem', fontSize: '0.875rem', lineHeight: 1.6, color: 'var(--text-primary)' }}>
                    {report.keyStrengths.map((item, idx) => (
                      <li key={idx} style={{ marginBottom: '0.4rem' }}>{item}</li>
                    ))}
                  </ul>
                ) : (
                  <p style={{ color: 'var(--text-muted)', fontSize: '0.8125rem', margin: 0 }}>No specific strengths highlighted.</p>
                )}
              </div>

              <div className="card" style={{ padding: '1.5rem', borderLeft: '3px solid var(--warning)' }}>
                <div style={{ fontSize: '0.9375rem', fontWeight: 600, color: '#ffffff', marginBottom: '0.875rem', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                  <WarningIcon size={16} color="var(--warning)" />
                  <span>Priority Areas for Optimization</span>
                </div>
                {report?.keyWeaknesses && report.keyWeaknesses.length > 0 ? (
                  <ul style={{ margin: 0, paddingLeft: '1.2rem', fontSize: '0.875rem', lineHeight: 1.6, color: 'var(--text-primary)' }}>
                    {report.keyWeaknesses.map((item, idx) => (
                      <li key={idx} style={{ marginBottom: '0.4rem' }}>{item}</li>
                    ))}
                  </ul>
                ) : (
                  <p style={{ color: 'var(--text-muted)', fontSize: '0.8125rem', margin: 0 }}>No critical weaknesses identified.</p>
                )}
              </div>
            </div>

            {/* ADAPTIVE ASSESSMENT JOURNEY */}
            {report?.adaptiveSummary && (
              <div className="card" style={{ marginBottom: '2rem', padding: '1.5rem' }}>
                <div style={{ fontSize: '0.9375rem', fontWeight: 600, color: 'var(--text-primary)', marginBottom: '0.875rem' }}>
                  Adaptive Decision Matrix
                </div>
                <div style={{ display: 'flex', flexWrap: 'wrap', gap: '2rem', alignItems: 'center' }}>
                  {report.adaptiveSummary.difficultyTimeline && report.adaptiveSummary.difficultyTimeline.length > 0 && (
                    <div>
                      <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', marginBottom: '0.4rem', textTransform: 'uppercase' }}>Difficulty Trajectory</div>
                      <div style={{ display: 'flex', gap: '0.4rem', alignItems: 'center', flexWrap: 'wrap' }}>
                        {report.adaptiveSummary.difficultyTimeline.map((diff, idx) => (
                          <React.Fragment key={idx}>
                            <span className="badge badge-secondary" style={{ fontSize: '0.75rem', padding: '0.25rem 0.6rem' }}>
                              Q{idx + 1}: {diff}
                            </span>
                            {idx < report.adaptiveSummary.difficultyTimeline.length - 1 && (
                              <span style={{ color: 'var(--text-muted)', fontSize: '0.75rem' }}>→</span>
                            )}
                          </React.Fragment>
                        ))}
                      </div>
                    </div>
                  )}

                  {report.adaptiveSummary.focusedConcepts && report.adaptiveSummary.focusedConcepts.length > 0 && (
                    <div>
                      <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', marginBottom: '0.4rem', textTransform: 'uppercase' }}>Target Concepts Explored</div>
                      <div style={{ display: 'flex', gap: '0.4rem', flexWrap: 'wrap' }}>
                        {report.adaptiveSummary.focusedConcepts.map((concept, idx) => (
                          <span key={idx} className="badge badge-info" style={{ fontSize: '0.75rem' }}>
                            {concept}
                          </span>
                        ))}
                      </div>
                    </div>
                  )}
                </div>
              </div>
            )}

            {/* QUESTION-BY-QUESTION EXPLORABLE BREAKDOWN */}
            <div style={{ marginBottom: '1rem', marginTop: '2rem' }}>
              <h2 style={{ fontSize: '1.25rem', fontWeight: 600, margin: 0, color: 'var(--text-primary)' }}>
                Question-by-Question Deep Dive
              </h2>
              <p style={{ color: 'var(--text-muted)', fontSize: '0.8125rem', margin: '0.25rem 0 0 0' }}>
                Detailed rubric analysis, candidate transcript, and personalized improvement guidance.
              </p>
            </div>

            <div style={{ display: 'flex', flexDirection: 'column', gap: '1.25rem', marginBottom: '3rem' }}>
              {report?.questions?.map((q, idx) => {
                const isExpanded = expandedQuestion === q.questionOrder;
                return (
                  <div key={idx} className="card" style={{ padding: '1.25rem' }}>
                    <div
                      onClick={() => setExpandedQuestion(isExpanded ? null : q.questionOrder)}
                      style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', cursor: 'pointer', flexWrap: 'wrap', gap: '0.5rem' }}
                    >
                      <div style={{ display: 'flex', alignItems: 'center', gap: '0.625rem' }}>
                        <span style={{ fontSize: '0.875rem', fontWeight: 600, color: 'var(--text-primary)' }}>
                          Question {q.questionOrder}
                        </span>
                        <span className="badge badge-secondary" style={{ fontSize: '0.7rem' }}>
                          {q.questionType || 'TECHNICAL'}
                        </span>
                        <span className="badge badge-secondary" style={{ fontSize: '0.7rem' }}>
                          {q.difficulty}
                        </span>
                        {q.inputMethod === 'VOICE' && (
                          <span className="badge badge-info" style={{ fontSize: '0.7rem' }}>Voice</span>
                        )}
                      </div>
                      
                      <div style={{ display: 'flex', alignItems: 'center', gap: '1rem' }}>
                        {q.score !== null && (
                          <span style={{ fontSize: '1rem', fontWeight: 700, color: getScoreColor(q.score) }}>
                            {q.score.toFixed(1)} <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)', fontWeight: 400 }}>/ 10</span>
                          </span>
                        )}
                        <span style={{ color: 'var(--text-muted)', fontSize: '0.8125rem' }}>
                          {isExpanded ? '▲' : '▼'}
                        </span>
                      </div>
                    </div>

                    <p style={{ fontWeight: 500, fontSize: '0.9375rem', color: 'var(--text-primary)', margin: '0.75rem 0 0 0', lineHeight: 1.5 }}>
                      {q.questionText}
                    </p>

                    {isExpanded && (
                      <div style={{ marginTop: '1rem', paddingTop: '1rem', borderTop: '1px solid var(--border)' }}>
                        {/* Candidate Answer */}
                        <div style={{
                          background: 'var(--surface-raised)',
                          padding: '0.875rem 1rem',
                          borderRadius: 'var(--radius-sm)',
                          border: '1px solid var(--border)',
                          fontSize: '0.875rem',
                          color: 'var(--text-primary)',
                          marginBottom: '1rem',
                          lineHeight: 1.5
                        }}>
                          <div style={{ fontSize: '0.75rem', fontWeight: 600, color: 'var(--text-muted)', marginBottom: '0.25rem', textTransform: 'uppercase', letterSpacing: '0.04em' }}>
                            Your Submitted Response
                          </div>
                          <div style={{ whiteSpace: 'pre-wrap' }}>
                            {q.candidateAnswer || <span style={{ color: 'var(--text-muted)', fontStyle: 'italic' }}>No answer submitted.</span>}
                          </div>
                        </div>

                        {/* Evaluator Feedback */}
                        {q.score !== null ? (
                          <div style={{
                            padding: '1rem',
                            background: 'rgba(15, 23, 42, 0.65)',
                            borderRadius: 'var(--radius-sm)',
                            border: '1px solid var(--border)'
                          }}>
                            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: '0.75rem', marginBottom: '0.875rem', fontSize: '0.75rem', color: 'var(--text-muted)' }}>
                              <div>Accuracy: <strong style={{ color: '#ffffff' }}>{q.correctness != null ? q.correctness : '—'}/10</strong></div>
                              <div>Relevance: <strong style={{ color: '#ffffff' }}>{q.relevance != null ? q.relevance : '—'}/10</strong></div>
                              <div>Clarity: <strong style={{ color: '#ffffff' }}>{q.clarity != null ? q.clarity : '—'}/10</strong></div>
                              <div>Depth: <strong style={{ color: '#ffffff' }}>{q.technicalAccuracy != null ? q.technicalAccuracy : '—'}/10</strong></div>
                            </div>

                            {q.userFacingMessage && (
                              <div style={{ fontSize: '0.8125rem', fontWeight: 600, color: 'var(--text-accent)', marginBottom: '0.5rem' }}>
                                {q.userFacingMessage}
                              </div>
                            )}

                            <p style={{ fontSize: '0.875rem', color: 'var(--text-primary)', margin: '0 0 0.5rem 0', lineHeight: 1.5 }}>
                              <strong style={{ fontWeight: 600, color: 'var(--text-accent)' }}>Feedback: </strong> {q.feedback}
                            </p>

                            {q.improvementSuggestion && (
                              <p style={{ fontSize: '0.8125rem', color: 'var(--text-secondary)', margin: 0, lineHeight: 1.5 }}>
                                <strong style={{ fontWeight: 600, color: 'var(--warning)' }}>Refinement Guidance: </strong> {q.improvementSuggestion}
                              </p>
                            )}
                          </div>
                        ) : null}
                      </div>
                    )}
                  </div>
                );
              })}
            </div>

            {/* Bottom Actions */}
            <div style={{ display: 'flex', justifyContent: 'center', gap: '0.875rem' }} className="no-print">
              <button onClick={() => navigate('/dashboard')} className="btn btn-secondary btn-lg">
                Return to Dashboard
              </button>
              <button onClick={() => navigate('/interview/history')} className="btn btn-primary btn-lg">
                <span>View All Past Sessions</span>
                <ArrowRightIcon size={15} />
              </button>
            </div>
          </div>
        )}
      </div>
    </MainLayout>
  );
};

export default InterviewReportPage;

