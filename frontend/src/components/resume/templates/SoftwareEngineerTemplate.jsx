import React from 'react';

const SoftwareEngineerTemplate = ({ data }) => {
  if (!data) return null;

  return (
    <div className="resume-doc-engineer">
      {/* Header */}
      <header className="res-header">
        <div>
          <h1 className="res-name">{data.fullName || 'Candidate Name'}</h1>
          <div className="res-headline">{data.headline || 'Software Engineer'}</div>
        </div>

        <div className="res-contact-bar">
          <div>{data.email} {data.phone ? `• ${data.phone}` : ''}</div>
          <div>
            {data.location && <span>{data.location} • </span>}
            {data.githubUrl && (
              <a href={data.githubUrl} target="_blank" rel="noreferrer" style={{ color: '#0284c7' }}>
                GitHub
              </a>
            )}
            {data.linkedinUrl && (
              <span>
                {' • '}
                <a href={data.linkedinUrl} target="_blank" rel="noreferrer" style={{ color: '#0284c7' }}>
                  LinkedIn
                </a>
              </span>
            )}
            {data.portfolioUrl && (
              <span>
                {' • '}
                <a href={data.portfolioUrl} target="_blank" rel="noreferrer" style={{ color: '#0284c7' }}>
                  Portfolio
                </a>
              </span>
            )}
          </div>
        </div>
      </header>

      {/* Summary */}
      {data.summary && (
        <section className="res-section">
          <h2 className="res-section-title">Summary</h2>
          <p style={{ margin: 0, fontSize: '9.5pt', lineHeight: 1.45, color: '#1e293b' }}>
            {data.summary}
          </p>
        </section>
      )}

      {/* Categorized Technical Skills (Front and Center for Eng Screening) */}
      {data.skills && data.skills.length > 0 && (
        <section className="res-section">
          <h2 className="res-section-title">Technical Expertise</h2>
          <div className="res-skills-box">
            {data.skills.map((group, i) => {
              if (!group.items || group.items.length === 0) return null;
              return (
                <div key={i} style={{ fontSize: '9pt', marginBottom: '2pt', display: 'flex' }}>
                  <span style={{ fontWeight: 700, width: '130px', flexShrink: 0, color: '#0369a1' }}>
                    {group.category}:
                  </span>
                  <span style={{ color: '#0f172a' }}>{group.items.join(', ')}</span>
                </div>
              );
            })}
          </div>
        </section>
      )}

      {/* Featured Technical Projects */}
      {data.projects && data.projects.length > 0 && (
        <section className="res-section">
          <h2 className="res-section-title">Engineering Projects</h2>
          {data.projects.map((proj) => (
            <div key={proj.id} className="res-entry">
              <div className="res-entry-head">
                <span className="res-entry-title">
                  {proj.title}
                  {proj.role ? <span style={{ fontWeight: 400, color: '#475569' }}> — {proj.role}</span> : null}
                </span>
                {proj.linkUrl && (
                  <span className="res-entry-date">
                    <a href={proj.linkUrl} target="_blank" rel="noreferrer" style={{ color: '#0284c7' }}>
                      Repository / Demo ↗
                    </a>
                  </span>
                )}
              </div>
              {proj.technologies && (
                <div className="res-entry-sub">
                  <strong style={{ color: '#0369a1' }}>Core Stack:</strong> {proj.technologies}
                </div>
              )}
              {proj.highlights && proj.highlights.length > 0 && (
                <ul className="res-bullets">
                  {proj.highlights.map((h, idx) => (
                    h && h.trim() ? <li key={idx}>{h.trim()}</li> : null
                  ))}
                </ul>
              )}
            </div>
          ))}
        </section>
      )}

      {/* Work Experience */}
      {data.experience && data.experience.length > 0 && (
        <section className="res-section">
          <h2 className="res-section-title">Professional Experience</h2>
          {data.experience.map((exp) => (
            <div key={exp.id} className="res-entry">
              <div className="res-entry-head">
                <span className="res-entry-title">
                  {exp.title} <span style={{ color: '#0369a1', fontWeight: 600 }}>@ {exp.company}</span>
                </span>
                <span className="res-entry-date">
                  {exp.startDate} – {exp.isCurrent ? 'Present' : exp.endDate}
                  {exp.location ? ` | ${exp.location}` : ''}
                </span>
              </div>
              {exp.highlights && exp.highlights.length > 0 && (
                <ul className="res-bullets">
                  {exp.highlights.map((h, idx) => (
                    h && h.trim() ? <li key={idx}>{h.trim()}</li> : null
                  ))}
                </ul>
              )}
            </div>
          ))}
        </section>
      )}

      {/* Education */}
      {data.education && data.education.length > 0 && (
        <section className="res-section">
          <h2 className="res-section-title">Education</h2>
          {data.education.map((edu) => (
            <div key={edu.id} className="res-entry">
              <div className="res-entry-head">
                <span className="res-entry-title">
                  {edu.degree} — <span style={{ fontWeight: 500 }}>{edu.institution}</span>
                </span>
                <span className="res-entry-date">
                  {edu.graduationYear} {edu.location ? `| ${edu.location}` : ''}
                </span>
              </div>
              {(edu.gpa || edu.honors) && (
                <div className="res-entry-sub">
                  {edu.gpa && <span>GPA: {edu.gpa} </span>}
                  {edu.honors && <span>• {edu.honors}</span>}
                </div>
              )}
              {edu.coursework && (
                <div className="res-entry-sub">
                  Key Coursework: {edu.coursework}
                </div>
              )}
            </div>
          ))}
        </section>
      )}

      {/* Certifications */}
      {data.certifications && data.certifications.length > 0 && (
        <section className="res-section">
          <h2 className="res-section-title">Certifications & Accreditations</h2>
          <ul className="res-bullets">
            {data.certifications.map((cert) => (
              <li key={cert.id}>
                <strong>{cert.name}</strong> — {cert.issuer} ({cert.issueDate})
                {cert.credentialUrl && (
                  <a href={cert.credentialUrl} target="_blank" rel="noreferrer" style={{ marginLeft: '6px', color: '#0284c7' }}>
                    Verify ↗
                  </a>
                )}
              </li>
            ))}
          </ul>
        </section>
      )}
    </div>
  );
};

export default SoftwareEngineerTemplate;
