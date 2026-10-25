import React from 'react';
import { RESUME_TEMPLATES_METADATA } from './utils/defaultResumeData';

const TemplateSelector = ({ selectedTemplate, onSelectTemplate }) => {
  return (
    <div>
      <div className="template-grid">
        {RESUME_TEMPLATES_METADATA.map((tmpl) => {
          const isSelected = selectedTemplate === tmpl.id;

          return (
            <div
              key={tmpl.id}
              className={`template-card ${isSelected ? 'active' : ''}`}
              onClick={() => onSelectTemplate(tmpl.id)}
            >
              <div className="template-card-header">
                <h3 className="template-card-title">{tmpl.name}</h3>
                <span className="template-card-badge">
                  ATS Score {tmpl.atsScore}%
                </span>
              </div>

              <div style={{ fontSize: '0.8rem', color: 'var(--text-accent)', fontWeight: 500, marginBottom: '0.35rem' }}>
                {tmpl.subtitle}
              </div>

              {/* Simplified visual skeleton representing the template layout */}
              <div className="template-mini-preview">
                {tmpl.id === 'classic-professional' && (
                  <>
                    <div className="mini-line title" />
                    <div className="mini-line rule" />
                    <div className="mini-line sub" style={{ margin: '0 auto 4px auto' }} />
                    <div className="mini-line" style={{ width: '90%' }} />
                    <div className="mini-line" style={{ width: '80%' }} />
                    <div className="mini-line rule" style={{ marginTop: '2px' }} />
                    <div className="mini-line" style={{ width: '95%' }} />
                  </>
                )}

                {tmpl.id === 'modern-minimal' && (
                  <>
                    <div className="mini-line header-left" style={{ borderLeft: '2px solid #4f46e5', paddingLeft: '4px' }} />
                    <div className="mini-line sub" />
                    <div className="mini-line accent" style={{ width: '35%', marginTop: '4px' }} />
                    <div className="mini-line" style={{ width: '95%' }} />
                    <div className="mini-line" style={{ width: '75%' }} />
                    <div className="mini-line accent" style={{ width: '30%', marginTop: '4px' }} />
                    <div className="mini-line" style={{ width: '85%' }} />
                  </>
                )}

                {tmpl.id === 'software-engineer' && (
                  <>
                    <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                      <div className="mini-line" style={{ width: '40%', height: '6px', background: '#0284c7' }} />
                      <div className="mini-line" style={{ width: '30%', height: '4px' }} />
                    </div>
                    <div style={{ background: '#f0f9ff', padding: '3px', borderRadius: '2px', margin: '2px 0' }}>
                      <div className="mini-line" style={{ width: '85%', background: '#0369a1' }} />
                    </div>
                    <div className="mini-line" style={{ width: '35%', background: '#0284c7', height: '4px', marginTop: '2px' }} />
                    <div className="mini-line" style={{ width: '90%' }} />
                    <div className="mini-line" style={{ width: '75%' }} />
                  </>
                )}

                {tmpl.id === 'graduate-fresher' && (
                  <>
                    <div className="mini-line title" style={{ background: '#059669' }} />
                    <div className="mini-line sub" style={{ margin: '0 auto 4px auto' }} />
                    <div className="mini-line" style={{ width: '40%', background: '#059669', height: '5px' }} />
                    <div className="mini-line" style={{ width: '85%' }} />
                    <div className="mini-line" style={{ width: '40%', background: '#059669', height: '5px', marginTop: '3px' }} />
                    <div className="mini-line" style={{ width: '90%' }} />
                  </>
                )}

                {tmpl.id === 'experienced-professional' && (
                  <>
                    <div className="mini-line header-left" style={{ background: '#4338ca', width: '60%' }} />
                    <div className="mini-line sub" style={{ width: '40%' }} />
                    <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: '2px', margin: '2px 0' }}>
                      <div className="mini-line" style={{ height: '3px' }} />
                      <div className="mini-line" style={{ height: '3px' }} />
                      <div className="mini-line" style={{ height: '3px' }} />
                    </div>
                    <div className="mini-line" style={{ width: '45%', background: '#4338ca', height: '4px' }} />
                    <div className="mini-line" style={{ width: '95%' }} />
                    <div className="mini-line" style={{ width: '80%' }} />
                  </>
                )}
              </div>

              <p style={{ fontSize: '0.8125rem', color: 'var(--text-secondary)', lineHeight: 1.45, margin: '0 0 0.75rem 0', flexGrow: 1 }}>
                {tmpl.description}
              </p>

              <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', marginBottom: '0.75rem' }}>
                <strong style={{ color: 'var(--text-primary)' }}>Best For:</strong> {tmpl.bestFor}
              </div>

              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <div style={{ display: 'flex', flexWrap: 'wrap', gap: '0.25rem' }}>
                  {tmpl.tags.slice(0, 2).map((t, idx) => (
                    <span key={idx} className="badge badge-secondary" style={{ fontSize: '0.68rem', padding: '0.1rem 0.4rem' }}>
                      {t}
                    </span>
                  ))}
                </div>

                <button
                  type="button"
                  className={`btn btn-sm ${isSelected ? 'btn-primary' : 'btn-secondary'}`}
                  style={{ pointerEvents: 'none' }}
                >
                  {isSelected ? '✓ Selected' : 'Choose'}
                </button>
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
};

export default TemplateSelector;
