import React from 'react';

const ModernTemplate = ({ data }) => {
  if (!data) return null;

  const contacts = [
    data.email,
    data.phone,
    data.location,
    data.linkedinUrl ? <a key="li" href={data.linkedinUrl} target="_blank" rel="noreferrer" style={{ color: '#4f46e5' }}>LinkedIn</a> : null,
    data.githubUrl ? <a key="gh" href={data.githubUrl} target="_blank" rel="noreferrer" style={{ color: '#4f46e5' }}>GitHub</a> : null,
    data.portfolioUrl ? <a key="port" href={data.portfolioUrl} target="_blank" rel="noreferrer" style={{ color: '#4f46e5' }}>Portfolio</a> : null
  ].filter(Boolean);

  return (
    <div className="resume-doc-modern">
      {/* Header */}
      <header className="res-header">
        <h1 className="res-name">{data.fullName || 'Candidate Name'}</h1>
        {data.headline && <div className="res-headline">{data.headline}</div>}
        {contacts.length > 0 && (
          <div className="res-contact-bar">
            {contacts.map((item, idx) => (
              <span key={idx}>
                {item}
                {idx < contacts.length - 1 && <span style={{ margin: '0 6px', color: '#cbd5e1' }}>|</span>}
              </span>
            ))}
          </div>
        )}
      </header>

      {/* Professional Summary */}
      {data.summary && (
        <section className="res-section">
          <h2 className="res-section-title">Summary</h2>
          <p style={{ margin: 0, fontSize: '9.5pt', lineHeight: 1.5, color: '#334155' }}>
            {data.summary}
          </p>
        </section>
      )}

      {/* Technical Skills */}
      {data.skills && data.skills.length > 0 && (
        <section className="res-section">
          <h2 className="res-section-title">Skills & Technologies</h2>
          <div style={{ fontSize: '9.5pt', lineHeight: 1.45 }}>
            {data.skills.map((group, i) => {
              if (!group.items || group.items.length === 0) return null;
              return (
                <div key={i} style={{ marginBottom: '2.5pt' }}>
                  <strong style={{ fontWeight: 600, color: '#1e293b' }}>{group.category}:</strong>{' '}
                  <span style={{ color: '#475569' }}>{group.items.join(' • ')}</span>
                </div>
              );
            })}
          </div>
        </section>
      )}

      {/* Work Experience */}
      {data.experience && data.experience.length > 0 && (
        <section className="res-section">
          <h2 className="res-section-title">Experience</h2>
          {data.experience.map((exp) => (
            <div key={exp.id} className="res-entry">
              <div className="res-entry-head">
                <span className="res-entry-title">
                  {exp.title} <span style={{ color: '#64748b', fontWeight: 500 }}>at {exp.company}</span>
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

      {/* Projects */}
      {data.projects && data.projects.length > 0 && (
        <section className="res-section">
          <h2 className="res-section-title">Key Projects</h2>
          {data.projects.map((proj) => (
            <div key={proj.id} className="res-entry">
              <div className="res-entry-head">
                <span className="res-entry-title">
                  {proj.title}
                  {proj.role ? <span style={{ fontWeight: 400, color: '#64748b' }}> — {proj.role}</span> : null}
                </span>
                {proj.linkUrl && (
                  <span className="res-entry-date">
                    <a href={proj.linkUrl} target="_blank" rel="noreferrer" style={{ color: '#4f46e5' }}>
                      View Project ↗
                    </a>
                  </span>
                )}
              </div>
              {proj.technologies && (
                <div className="res-entry-sub">
                  <span style={{ fontWeight: 600, color: '#334155' }}>Technologies:</span> {proj.technologies}
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

      {/* Education */}
      {data.education && data.education.length > 0 && (
        <section className="res-section">
          <h2 className="res-section-title">Education</h2>
          {data.education.map((edu) => (
            <div key={edu.id} className="res-entry">
              <div className="res-entry-head">
                <span className="res-entry-title">
                  {edu.degree} <span style={{ fontWeight: 500, color: '#64748b' }}>— {edu.institution}</span>
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
                  Focus: {edu.coursework}
                </div>
              )}
            </div>
          ))}
        </section>
      )}

      {/* Certifications */}
      {data.certifications && data.certifications.length > 0 && (
        <section className="res-section">
          <h2 className="res-section-title">Certifications</h2>
          <ul className="res-bullets">
            {data.certifications.map((cert) => (
              <li key={cert.id}>
                <strong>{cert.name}</strong> • {cert.issuer} ({cert.issueDate})
                {cert.credentialUrl && (
                  <a href={cert.credentialUrl} target="_blank" rel="noreferrer" style={{ marginLeft: '6px', color: '#4f46e5' }}>
                    Credential ↗
                  </a>
                )}
              </li>
            ))}
          </ul>
        </section>
      )}

      {/* Achievements */}
      {data.achievements && data.achievements.length > 0 && (
        <section className="res-section">
          <h2 className="res-section-title">Achievements</h2>
          <ul className="res-bullets">
            {data.achievements.map((ach) => (
              <li key={ach.id}>
                <strong>{ach.title}</strong> {ach.year ? `(${ach.year})` : ''}
                {ach.description ? ` — ${ach.description}` : ''}
              </li>
            ))}
          </ul>
        </section>
      )}
    </div>
  );
};

export default ModernTemplate;
