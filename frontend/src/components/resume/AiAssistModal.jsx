import React, { useState } from 'react';
import { requestAiAssist } from '../../services/resumeBuilderService';

const AiAssistModal = ({ isOpen, onClose, initialAction = 'IMPROVE_SUMMARY', resumeData, onApply, targetContext }) => {
  const [action, setAction] = useState(initialAction);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [aiResult, setAiResult] = useState(null);

  // Form states for modal
  const [customSummary, setCustomSummary] = useState(resumeData?.summary || '');
  const [customBullet, setCustomBullet] = useState(targetContext?.bullet || '');
  const [jobDescription, setJobDescription] = useState('');

  if (!isOpen) return null;

  const handleGenerate = async () => {
    setLoading(true);
    setError('');
    setAiResult(null);

    try {
      const skillsContext = (resumeData?.skills || [])
        .map(g => `${g.category}: ${(g.items || []).join(', ')}`)
        .join(' | ');

      const expContext = (resumeData?.experience || [])
        .map(e => `${e.title} at ${e.company}: ${(e.highlights || []).join('; ')}`)
        .join(' \n');

      const projContext = (resumeData?.projects || [])
        .map(p => `${p.title} (${p.technologies}): ${(p.highlights || []).join('; ')}`)
        .join(' \n');

      const payload = {
        action,
        summary: action === 'IMPROVE_SUMMARY' ? customSummary : resumeData?.summary,
        bullet: action === 'IMPROVE_BULLET' ? customBullet : targetContext?.bullet,
        targetRole: resumeData?.headline || 'Software Engineer',
        skillsContext,
        experienceContext: expContext,
        projectsContext: projContext,
        jobDescription: action === 'JD_KEYWORD_MATCH' ? jobDescription : ''
      };

      const result = await requestAiAssist(payload);
      setAiResult(result);
    } catch (err) {
      console.error('AI assistance failed:', err);
      setError('AI assistant request encountered an issue. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  const handleApplyText = (text) => {
    if (onApply) {
      onApply(action, text, targetContext);
    }
    onClose();
  };

  const handleAddSuggestedSkill = (skill) => {
    if (onApply) {
      onApply('ADD_SKILL', skill);
    }
  };

  return (
    <div style={{
      position: 'fixed',
      top: 0, left: 0, right: 0, bottom: 0,
      background: 'rgba(9, 13, 22, 0.75)',
      backdropFilter: 'blur(6px)',
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'center',
      zIndex: 2000,
      padding: '1rem'
    }}>
      <div className="card" style={{
        maxWidth: '680px',
        width: '100%',
        maxHeight: '90vh',
        overflowY: 'auto',
        padding: '1.75rem',
        border: '1px solid var(--border-glow)'
      }}>
        {/* Modal Header */}
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.25rem' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.625rem' }}>
            <span className="ai-pulse-dot" />
            <h3 style={{ margin: 0, fontSize: '1.15rem', fontWeight: 600, color: 'var(--text-primary)' }}>
              Groq AI Resume Copilot
            </h3>
          </div>
          <button
            type="button"
            className="control-icon-btn"
            onClick={onClose}
            style={{ fontSize: '1.1rem', padding: '0.2rem 0.5rem' }}
          >
            ✕
          </button>
        </div>

        {/* Action Tabs */}
        <div className="segmented-control" style={{ marginBottom: '1.25rem' }}>
          <button
            type="button"
            className={`segmented-btn ${action === 'IMPROVE_SUMMARY' ? 'active' : ''}`}
            onClick={() => { setAction('IMPROVE_SUMMARY'); setAiResult(null); }}
          >
            Summary
          </button>
          <button
            type="button"
            className={`segmented-btn ${action === 'IMPROVE_BULLET' ? 'active' : ''}`}
            onClick={() => { setAction('IMPROVE_BULLET'); setAiResult(null); }}
          >
            Bullet STAR
          </button>
          <button
            type="button"
            className={`segmented-btn ${action === 'SUGGEST_SKILLS' ? 'active' : ''}`}
            onClick={() => { setAction('SUGGEST_SKILLS'); setAiResult(null); }}
          >
            Suggest Skills
          </button>
          <button
            type="button"
            className={`segmented-btn ${action === 'JD_KEYWORD_MATCH' ? 'active' : ''}`}
            onClick={() => { setAction('JD_KEYWORD_MATCH'); setAiResult(null); }}
          >
            Match Against JD
          </button>
        </div>

        {error && (
          <div className="alert alert-danger" style={{ marginBottom: '1rem' }}>
            {error}
          </div>
        )}

        {/* Action specific inputs */}
        {action === 'IMPROVE_SUMMARY' && (
          <div className="form-group" style={{ marginBottom: '1rem' }}>
            <label className="form-label">Current Summary Draft</label>
            <textarea
              className="form-textarea"
              rows="3"
              value={customSummary}
              onChange={(e) => setCustomSummary(e.target.value)}
              placeholder="Paste or write your raw summary here..."
            />
            <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', marginTop: '0.25rem' }}>
              Groq AI will rewrite your summary into a high-impact, ATS-optimized 2-3 sentence overview without inventing false metrics.
            </div>
          </div>
        )}

        {action === 'IMPROVE_BULLET' && (
          <div className="form-group" style={{ marginBottom: '1rem' }}>
            <label className="form-label">Bullet Point to Enhance</label>
            <textarea
              className="form-textarea"
              rows="2"
              value={customBullet}
              onChange={(e) => setCustomBullet(e.target.value)}
              placeholder="e.g. Worked on the backend APIs and helped with bugs."
            />
            <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', marginTop: '0.25rem' }}>
              Will rewrite using Google XYZ / STAR format: Strong Action Verb + Technical Scope + Result.
            </div>
          </div>
        )}

        {action === 'SUGGEST_SKILLS' && (
          <div style={{ marginBottom: '1rem', background: 'var(--bg-secondary)', padding: '0.875rem', borderRadius: 'var(--radius-sm)' }}>
            <div style={{ fontSize: '0.8125rem', color: 'var(--text-secondary)' }}>
              Analyzes your listed work experience and projects to identify authentic technical skills you might have forgotten to mention for ATS keyword coverage.
            </div>
          </div>
        )}

        {action === 'JD_KEYWORD_MATCH' && (
          <div className="form-group" style={{ marginBottom: '1rem' }}>
            <label className="form-label">Target Job Description *</label>
            <textarea
              className="form-textarea"
              rows="4"
              value={jobDescription}
              onChange={(e) => setJobDescription(e.target.value)}
              placeholder="Paste the requirements or job description from LinkedIn, Indeed, or the employer portal..."
            />
          </div>
        )}

        {/* Generate Trigger */}
        <div style={{ display: 'flex', justifyContent: 'flex-end', marginBottom: '1.25rem' }}>
          <button
            type="button"
            className="btn btn-primary"
            onClick={handleGenerate}
            disabled={loading || (action === 'JD_KEYWORD_MATCH' && !jobDescription.trim())}
          >
            {loading ? 'AI Processing...' : '✨ Run Groq AI Assist'}
          </button>
        </div>

        {/* AI Results Section */}
        {aiResult && (
          <div style={{
            background: 'var(--surface-raised)',
            border: '1px solid var(--border-glow)',
            borderRadius: 'var(--radius-sm)',
            padding: '1.25rem'
          }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.75rem' }}>
              <span style={{ fontSize: '0.75rem', fontWeight: 600, color: 'var(--text-accent)', textTransform: 'uppercase' }}>
                AI Recommendations
              </span>
              {aiResult.atsReadinessScore != null && (
                <span className="badge badge-success">
                  ATS Match: {aiResult.atsReadinessScore}%
                </span>
              )}
            </div>

            {/* Primary Recommended Text */}
            {aiResult.improvedText && (
              <div style={{ marginBottom: '1rem' }}>
                <div style={{ fontSize: '0.8125rem', fontWeight: 600, color: 'var(--text-primary)', marginBottom: '0.35rem' }}>
                  Recommended Version:
                </div>
                <div style={{
                  background: 'var(--surface)',
                  padding: '0.75rem 1rem',
                  borderRadius: 'var(--radius-xs)',
                  border: '1px solid var(--border)',
                  fontSize: '0.875rem',
                  lineHeight: 1.5,
                  color: 'var(--text-primary)'
                }}>
                  {aiResult.improvedText}
                </div>
                {(action === 'IMPROVE_SUMMARY' || action === 'IMPROVE_BULLET') && (
                  <button
                    type="button"
                    className="btn btn-primary btn-sm"
                    style={{ marginTop: '0.5rem' }}
                    onClick={() => handleApplyText(aiResult.improvedText)}
                  >
                    ✓ Apply to Resume
                  </button>
                )}
              </div>
            )}

            {/* Alternative Suggestions */}
            {aiResult.suggestions && aiResult.suggestions.length > 0 && (
              <div style={{ marginBottom: '1rem' }}>
                <div style={{ fontSize: '0.8125rem', fontWeight: 600, color: 'var(--text-secondary)', marginBottom: '0.35rem' }}>
                  {action === 'SUGGEST_SKILLS' ? 'Suggested Skills to Add:' : 'Alternative Variations:'}
                </div>
                <div style={{ display: 'flex', flexDirection: 'column', gap: '0.5rem' }}>
                  {aiResult.suggestions.map((sug, i) => (
                    <div
                      key={i}
                      style={{
                        background: 'var(--surface)',
                        padding: '0.625rem 0.875rem',
                        borderRadius: 'var(--radius-xs)',
                        border: '1px solid var(--border-light)',
                        display: 'flex',
                        justifyContent: 'space-between',
                        alignItems: 'center',
                        gap: '0.5rem',
                        fontSize: '0.8125rem'
                      }}
                    >
                      <span style={{ color: 'var(--text-primary)' }}>{sug}</span>
                      {action === 'SUGGEST_SKILLS' ? (
                        <button
                          type="button"
                          className="btn btn-secondary btn-sm"
                          style={{ padding: '0.2rem 0.5rem', fontSize: '0.75rem' }}
                          onClick={() => handleAddSuggestedSkill(sug)}
                        >
                          + Add
                        </button>
                      ) : (
                        <button
                          type="button"
                          className="btn btn-secondary btn-sm"
                          style={{ padding: '0.2rem 0.5rem', fontSize: '0.75rem' }}
                          onClick={() => handleApplyText(sug)}
                        >
                          Apply
                        </button>
                      )}
                    </div>
                  ))}
                </div>
              </div>
            )}

            {/* Matched vs Missing Keywords for JD */}
            {action === 'JD_KEYWORD_MATCH' && (
              <div>
                {aiResult.matchedKeywords && aiResult.matchedKeywords.length > 0 && (
                  <div style={{ marginBottom: '0.75rem' }}>
                    <div style={{ fontSize: '0.75rem', fontWeight: 600, color: 'var(--success)', textTransform: 'uppercase', marginBottom: '0.35rem' }}>
                      ✓ Matched Keywords ({aiResult.matchedKeywords.length})
                    </div>
                    <div style={{ display: 'flex', flexWrap: 'wrap', gap: '0.35rem' }}>
                      {aiResult.matchedKeywords.map((kw, i) => (
                        <span key={i} className="badge badge-success" style={{ fontSize: '0.75rem' }}>
                          {kw}
                        </span>
                      ))}
                    </div>
                  </div>
                )}

                {aiResult.missingKeywords && aiResult.missingKeywords.length > 0 && (
                  <div style={{ marginBottom: '0.75rem' }}>
                    <div style={{ fontSize: '0.75rem', fontWeight: 600, color: 'var(--warning)', textTransform: 'uppercase', marginBottom: '0.35rem' }}>
                      ⚠ Missing Keywords in Resume ({aiResult.missingKeywords.length})
                    </div>
                    <div style={{ display: 'flex', flexWrap: 'wrap', gap: '0.35rem' }}>
                      {aiResult.missingKeywords.map((kw, i) => (
                        <span key={i} className="badge badge-warning" style={{ fontSize: '0.75rem' }}>
                          {kw}
                        </span>
                      ))}
                    </div>
                  </div>
                )}

                {aiResult.auditTips && aiResult.auditTips.length > 0 && (
                  <div>
                    <div style={{ fontSize: '0.75rem', fontWeight: 600, color: 'var(--text-muted)', textTransform: 'uppercase', marginBottom: '0.35rem' }}>
                      ATS Alignment Tips
                    </div>
                    <ul style={{ margin: 0, paddingLeft: '1.2rem', fontSize: '0.8125rem', color: 'var(--text-secondary)' }}>
                      {aiResult.auditTips.map((tip, i) => (
                        <li key={i} style={{ marginBottom: '0.25rem' }}>{tip}</li>
                      ))}
                    </ul>
                  </div>
                )}
              </div>
            )}
          </div>
        )}
      </div>
    </div>
  );
};

export default AiAssistModal;
