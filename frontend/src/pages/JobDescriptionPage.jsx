import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import MainLayout from '../layouts/MainLayout';
import {
  BriefcaseIcon,
  DocumentIcon,
  SparklesIcon,
  CheckIcon,
  PlayIcon,
  ArrowRightIcon,
  CodeIcon,
  LayersIcon
} from '../components/common/Icons';
import {
  createJobDescription,
  getUserJobDescriptions,
  deleteJobDescription
} from '../services/jobDescriptionService';

export default function JobDescriptionPage() {
  const navigate = useNavigate();
  const [jds, setJds] = useState([]);
  const [loading, setLoading] = useState(false);
  const [analyzing, setAnalyzing] = useState(false);
  const [error, setError] = useState(null);
  const [successMsg, setSuccessMsg] = useState(null);

  // Form State
  const [targetRole, setTargetRole] = useState('');
  const [companyName, setCompanyName] = useState('');
  const [rawText, setRawText] = useState('');
  const [selectedJd, setSelectedJd] = useState(null);

  useEffect(() => {
    fetchJds();
  }, []);

  const fetchJds = async () => {
    try {
      setLoading(true);
      const data = await getUserJobDescriptions();
      setJds(data || []);
      if (data && data.length > 0 && !selectedJd) {
        setSelectedJd(data[0]);
      }
    } catch (err) {
      setError('Failed to load job descriptions.');
    } finally {
      setLoading(false);
    }
  };

  const handleAnalyze = async (e) => {
    e.preventDefault();
    if (!rawText.trim()) {
      setError('Job description text cannot be empty.');
      return;
    }
    if (rawText.length > 10000) {
      setError('Job description exceeds maximum allowed length of 10,000 characters.');
      return;
    }

    try {
      setAnalyzing(true);
      setError(null);
      setSuccessMsg(null);
      const result = await createJobDescription({
        targetRole,
        companyName,
        rawText
      });
      setSuccessMsg('Job description parsed and technical profile cataloged.');
      setSelectedJd(result);
      setRawText('');
      setTargetRole('');
      setCompanyName('');
      await fetchJds();
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to analyze job description.');
    } finally {
      setAnalyzing(false);
    }
  };

  const handleDelete = async (id) => {
    if (!window.confirm('Delete this job description?')) return;
    try {
      await deleteJobDescription(id);
      if (selectedJd && selectedJd.id === id) {
        setSelectedJd(null);
      }
      await fetchJds();
    } catch (err) {
      setError('Failed to delete job description.');
    }
  };

  const handleStartInterview = (jdId) => {
    navigate('/interview/setup', { state: { selectedJdId: jdId, mode: 'JOB_DESCRIPTION' } });
  };

  const parseJsonArray = (str) => {
    if (!str) return [];
    try {
      return JSON.parse(str);
    } catch {
      return [];
    }
  };

  return (
    <MainLayout>
      <div style={{ maxWidth: '1120px', margin: '0 auto', paddingBottom: '3.5rem' }}>
        {/* Header */}
        <div style={{ marginBottom: '2rem' }}>
          <div style={{ display: 'inline-flex', alignItems: 'center', gap: '0.5rem', marginBottom: '0.35rem' }}>
            <span className="studio-pill">
              <span className="studio-dot" /> ROLE TARGETING
            </span>
          </div>
          <h1 style={{ fontSize: '1.875rem', fontWeight: 700, letterSpacing: '-0.025em', margin: 0, color: 'var(--text-primary)' }}>
            Job Description Analysis Studio
          </h1>
          <p style={{ color: 'var(--text-muted)', fontSize: '0.9rem', margin: '0.35rem 0 0 0' }}>
            Extract core competencies, required tech stacks, and generate highly targeted interview simulations tailored to specific job postings.
          </p>
        </div>

        {error && (
          <div className="alert alert-danger" style={{ marginBottom: '1.25rem' }}>
            {error}
          </div>
        )}

        {successMsg && (
          <div className="alert alert-success" style={{ marginBottom: '1.25rem' }}>
            <CheckIcon size={16} style={{ flexShrink: 0 }} />
            <span>{successMsg}</span>
          </div>
        )}

        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(380px, 1fr))', gap: '1.5rem', alignItems: 'start' }}>
          {/* Form Card */}
          <div className="card" style={{ padding: '1.75rem', border: '1px solid var(--border)' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '0.5rem' }}>
              <BriefcaseIcon size={18} style={{ color: 'var(--primary)' }} />
              <h2 style={{ fontSize: '1.125rem', fontWeight: 600, margin: 0, color: 'var(--text-primary)' }}>
                Catalog Target Posting
              </h2>
            </div>
            <p style={{ fontSize: '0.8125rem', color: 'var(--text-muted)', margin: '0 0 1.25rem 0' }}>
              Paste requirements and qualifications directly from LinkedIn, Indeed, or company careers portals.
            </p>

            <form onSubmit={handleAnalyze}>
              <div className="form-group">
                <label className="form-label">Target Role / Title</label>
                <input
                  type="text"
                  value={targetRole}
                  onChange={(e) => setTargetRole(e.target.value)}
                  placeholder="e.g. Senior Backend Engineer"
                  className="form-input"
                />
              </div>

              <div className="form-group">
                <label className="form-label">Company Name (Optional)</label>
                <input
                  type="text"
                  value={companyName}
                  onChange={(e) => setCompanyName(e.target.value)}
                  placeholder="e.g. Stripe, Linear, Datadog"
                  className="form-input"
                />
              </div>

              <div className="form-group">
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.35rem' }}>
                  <label className="form-label" style={{ margin: 0 }}>
                    Job Description Content *
                  </label>
                  <span style={{ fontSize: '0.75rem', color: rawText.length > 10000 ? 'var(--danger)' : 'var(--text-muted)' }}>
                    {rawText.length.toLocaleString()} / 10,000 characters
                  </span>
                </div>
                <textarea
                  rows="9"
                  value={rawText}
                  onChange={(e) => setRawText(e.target.value)}
                  placeholder="Paste the role requirements, responsibilities, system architecture challenges, and expectations..."
                  required
                  className="form-textarea"
                  style={{ fontFamily: 'var(--font-mono, monospace)', fontSize: '0.8125rem', lineHeight: 1.5 }}
                />
              </div>

              <button
                type="submit"
                disabled={analyzing || rawText.length > 10000}
                className="btn btn-primary"
                style={{ width: '100%', display: 'inline-flex', alignItems: 'center', justifyContent: 'center', gap: '0.5rem' }}
              >
                <SparklesIcon size={16} />
                <span>{analyzing ? 'Extracting Core Competencies...' : 'Analyze & Extract Skills'}</span>
              </button>
            </form>
          </div>

          {/* Selected JD Detail Card */}
          <div className="card studio-terminal" style={{ padding: '1.75rem', border: '1px solid var(--border-editor)' }}>
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '1rem', paddingBottom: '0.75rem', borderBottom: '1px solid var(--border)' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                <LayersIcon size={18} style={{ color: 'var(--primary)' }} />
                <h2 style={{ fontSize: '1.125rem', fontWeight: 600, margin: 0, color: 'var(--text-primary)' }}>
                  Technical Blueprint
                </h2>
              </div>
              {selectedJd && (
                <span className="badge badge-secondary" style={{ fontSize: '0.7rem' }}>
                  {selectedJd.experienceLevel || 'MID'} LEVEL
                </span>
              )}
            </div>

            {selectedJd ? (
              <div>
                <div style={{ marginBottom: '1.25rem' }}>
                  <h3 style={{ fontSize: '1.25rem', fontWeight: 700, margin: '0 0 0.25rem 0', color: 'var(--text-primary)' }}>
                    {selectedJd.targetRole || 'Target Role'}
                  </h3>
                  {selectedJd.companyName && (
                    <div style={{ fontSize: '0.875rem', color: 'var(--primary)', fontWeight: 500 }}>
                      {selectedJd.companyName}
                    </div>
                  )}
                </div>

                {selectedJd.aiAnalysisSummary && (
                  <div style={{ marginBottom: '1.25rem', background: 'var(--surface-raised)', padding: '0.875rem 1rem', borderRadius: 'var(--radius-sm)', border: '1px solid var(--border)' }}>
                    <div style={{ fontSize: '0.75rem', fontWeight: 600, color: 'var(--text-muted)', textTransform: 'uppercase', letterSpacing: '0.04em', marginBottom: '0.35rem' }}>
                      AI Role Summary
                    </div>
                    <p style={{ margin: 0, color: 'var(--text-primary)', fontSize: '0.875rem', lineHeight: 1.55 }}>
                      {selectedJd.aiAnalysisSummary}
                    </p>
                  </div>
                )}

                <div style={{ marginBottom: '1.25rem' }}>
                  <div style={{ fontSize: '0.75rem', fontWeight: 600, color: 'var(--text-muted)', textTransform: 'uppercase', letterSpacing: '0.04em', marginBottom: '0.5rem' }}>
                    Required Technical Skills
                  </div>
                  <div style={{ display: 'flex', flexWrap: 'wrap', gap: '0.35rem' }}>
                    {parseJsonArray(selectedJd.extractedSkillsJson).length > 0 ? (
                      parseJsonArray(selectedJd.extractedSkillsJson).map((skill, idx) => (
                        <span key={idx} className="badge badge-secondary" style={{ fontSize: '0.75rem', padding: '0.3rem 0.55rem' }}>
                          {skill}
                        </span>
                      ))
                    ) : (
                      <span style={{ color: 'var(--text-muted)', fontSize: '0.8125rem' }}>No specific skills extracted.</span>
                    )}
                  </div>
                </div>

                <div style={{ marginBottom: '1.5rem' }}>
                  <div style={{ fontSize: '0.75rem', fontWeight: 600, color: 'var(--text-muted)', textTransform: 'uppercase', letterSpacing: '0.04em', marginBottom: '0.5rem' }}>
                    Key Engineering Responsibilities
                  </div>
                  <ul style={{ paddingLeft: '1.25rem', margin: 0, color: 'var(--text-secondary)', fontSize: '0.8125rem', lineHeight: 1.55, display: 'flex', flexDirection: 'column', gap: '0.4rem' }}>
                    {parseJsonArray(selectedJd.keyResponsibilitiesJson).length > 0 ? (
                      parseJsonArray(selectedJd.keyResponsibilitiesJson).map((resp, idx) => (
                        <li key={idx}>{resp}</li>
                      ))
                    ) : (
                      <li style={{ color: 'var(--text-muted)' }}>General software engineering responsibilities.</li>
                    )}
                  </ul>
                </div>

                <button
                  type="button"
                  onClick={() => handleStartInterview(selectedJd.id)}
                  className="btn btn-primary"
                  style={{ width: '100%', display: 'inline-flex', alignItems: 'center', justifyContent: 'center', gap: '0.5rem', padding: '0.75rem 1rem' }}
                >
                  <PlayIcon size={16} />
                  <span>Launch Tailored Mock Interview</span>
                  <ArrowRightIcon size={14} />
                </button>
              </div>
            ) : (
              <div className="empty-state" style={{ padding: '3.5rem 1.5rem', border: '1px dashed var(--border)' }}>
                <DocumentIcon size={32} style={{ color: 'var(--text-muted)', marginBottom: '0.75rem' }} />
                <p style={{ margin: 0, color: 'var(--text-muted)', fontSize: '0.875rem' }}>
                  Select a saved posting below or paste a new one on the left to inspect requirements and generate targeted practice questions.
                </p>
              </div>
            )}
          </div>
        </div>

        {/* Saved JDs Grid */}
        <div style={{ marginTop: '2.5rem' }}>
          <div style={{ marginBottom: '1rem', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
            <BriefcaseIcon size={18} style={{ color: 'var(--text-muted)' }} />
            <h2 style={{ fontSize: '1.15rem', fontWeight: 600, margin: 0, color: 'var(--text-primary)' }}>
              Saved Target Roles ({jds.length})
            </h2>
          </div>

          {loading ? (
            <div className="card" style={{ padding: '2.5rem', textAlign: 'center' }}>
              <div className="skeleton" style={{ width: '40%', height: '20px', margin: '0 auto' }} />
            </div>
          ) : jds.length === 0 ? (
            <div className="empty-state" style={{ padding: '2.5rem 1.5rem', border: '1px dashed var(--border)' }}>
              <h3 style={{ fontSize: '1.05rem', fontWeight: 600, margin: '0 0 0.4rem 0' }}>No saved job descriptions</h3>
              <p style={{ color: 'var(--text-muted)', fontSize: '0.85rem', margin: 0 }}>
                Paste a job description above to parse its tech stack and practice role-specific questions.
              </p>
            </div>
          ) : (
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(320px, 1fr))', gap: '1rem' }}>
              {jds.map((jd) => {
                const isSelected = selectedJd && selectedJd.id === jd.id;
                return (
                  <div
                    key={jd.id}
                    onClick={() => setSelectedJd(jd)}
                    className="card"
                    style={{
                      padding: '1.25rem',
                      cursor: 'pointer',
                      borderColor: isSelected ? 'var(--primary)' : 'var(--border)',
                      background: isSelected ? 'var(--surface-raised)' : 'var(--surface)',
                      boxShadow: isSelected ? '0 0 0 1px var(--primary)' : 'none',
                      transition: 'all 0.15s ease'
                    }}
                  >
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', gap: '0.5rem', marginBottom: '0.35rem' }}>
                      <h3 style={{ fontSize: '0.975rem', fontWeight: 600, color: 'var(--text-primary)', margin: 0 }}>
                        {jd.targetRole || 'Target Role'}
                      </h3>
                      <button
                        type="button"
                        onClick={(e) => { e.stopPropagation(); handleDelete(jd.id); }}
                        style={{
                          background: 'none',
                          border: 'none',
                          color: 'var(--text-muted)',
                          cursor: 'pointer',
                          fontSize: '0.85rem',
                          padding: '0.1rem 0.35rem',
                          borderRadius: 'var(--radius-sm)'
                        }}
                        title="Delete"
                      >
                        ✕
                      </button>
                    </div>
                    {jd.companyName && (
                      <p style={{ margin: '0 0 0.65rem 0', color: 'var(--text-muted)', fontSize: '0.8125rem' }}>
                        {jd.companyName}
                      </p>
                    )}
                    <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', fontSize: '0.75rem', color: 'var(--text-muted)', paddingTop: '0.5rem', borderTop: '1px solid var(--border)' }}>
                      <span>Added {new Date(jd.createdAt).toLocaleDateString()}</span>
                      <span style={{ color: isSelected ? 'var(--primary)' : 'var(--text-muted)', fontWeight: 500 }}>
                        {isSelected ? 'Active Selection' : 'Click to View →'}
                      </span>
                    </div>
                  </div>
                );
              })}
            </div>
          )}
        </div>
      </div>
    </MainLayout>
  );
}
