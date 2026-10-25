import React from 'react';

const ExperiencedTemplate = ({ data }) => {
  if (!data) return null;

  const contacts = [
    data.email,
    data.phone,
    data.location,
    data.linkedinUrl ? <a key="li" href={data.linkedinUrl} target="_blank" rel="noreferrer" style={{ color: '#4338ca' }}>LinkedIn</a> : null,
    data.githubUrl ? <a key="gh" href={data.githubUrl} target="_blank" rel="noreferrer" style={{ color: '#4338ca' }}>GitHub</a> : null,
    data.portfolioUrl ? <a key="port" href={data.portfolioUrl} target="_blank" rel="noreferrer" style={{ color: '#4338ca' }}>Portfolio</a> : null
  ].filter(Boolean);

  // Extract all skill items as a flat array for a clean Core Competencies display
  const allSkillsList = (data.skills || []).flatMap(g => g.items || []);

  return (
    <div className="resume-doc-experienced">
      {/* Header */}
      <header className="res-header">
        <h1 className="res-name">{data.fullName || 'Candidate Name'}</h1>
        {data.headline && <div className="res-headline">{data.headline}</div>}
        {contacts.length > 0 && (
          <div className="res-contact-bar">
            {contacts.map((item, idx) => (
              <span key={idx}>
                {item}
                {idx < contacts.length - 1 && <span style={{ margin: '0 6px', color: '#818cf8' }}>|</span>}
              </span>
            ))}
          </div>
        )}
      </header>

      {/* Executive Summary */}
      {data.summary && (
        <section className="res-section">
          <h2 className="res-section-title">Executive Summary</h2>
          <p style={{ margin: 0, fontSize: '9.5pt', lineHeight: 1.5, textAlign: 'justify', color: '#1f2937' }}>
            {data.summary}
          </p>
        </section>
      )}

      {/* Core Competencies Matrix */}
      {allSkillsList.length > 0 && (
        <section className="res-section">
          <h2 className="res-section-title">Core Competencies & Domain Expertise</h2>
          <div style={{
            display: 'grid',
            gridTemplateColumns: 'repeat(3, 1fr)',
            gap: '2pt 12pt',
            fontSize: '9pt',
            color: '#1f2937'
          }}>
            {allSkillsList.slice(0, 15).map((skill, idx) => (
              <div key={idx} style={{ display: 'flex', alignItems: 'center', gap: '4px' }}>
                <span style={{ color: '#4338ca' }}>▪</span>
                <span style={{ fontWeight: 600 }}>{skill}</span>
              </div>
            ))}
          </div>
        </section>
      )}

      {/* Career History & Leadership Experience */}
      {data.experience && data.experience.length > 0 && (
        <section className="res-section">
          <h2 className="res-section-title">Career History & Leadership Experience</h2>
          {data.experience.map((exp) => (
            <div key={exp.id} className="res-entry">
              <div className="res-entry-head">
                <span className="res-entry-title">
                  {exp.title} <span className="res-entry-company">— {exp.company}</span>
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

      {/* Strategic Technical Projects */}
      {data.projects && data.projects.length > 0 && (
        <section className="res-section">
          <h2 className="res-section-title">Strategic Technical Projects</h2>
          {data.projects.map((proj) => (
            <div key={proj.id} className="res-entry">
              <div className="res-entry-head">
                <span className="res-entry-title">
                  {proj.title}
                  {proj.role ? <span style={{ fontWeight: 400, color: '#4b5563' }}> ({proj.role})</span> : null}
                </span>
                {proj.linkUrl && (
                  <span className="res-entry-date">
                    <a href={proj.linkUrl} target="_blank" rel="noreferrer" style={{ color: '#4338ca' }}>
                      Architecture & Demo ↗
                    </a>
                  </span>
                )}
              </div>
              {proj.technologies && (
                <div className="res-entry-sub">
                  <strong>Stack:</strong> {proj.technologies}
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
          <h2 className="res-section-title">Education & Academic Background</h2>
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
            </div>
          ))}
        </section>
      )}

      {/* Certifications */}
      {data.certifications && data.certifications.length > 0 && (
        <section className="res-section">
          <h2 className="res-section-title">Executive Certifications & Credentials</h2>
          <ul className="res-bullets">
            {data.certifications.map((cert) => (
              <li key={cert.id}>
                <strong>{cert.name}</strong> — {cert.issuer} ({cert.issueDate})
                {cert.credentialUrl && (
                  <a href={cert.credentialUrl} target="_blank" rel="noreferrer" style={{ marginLeft: '6px', color: '#4338ca' }}>
                    Verify ↗
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
          <h2 className="res-section-title">Key Honors & Industry Recognition</h2>
          <ul className="res-bullets">
            {data.achievements.map((ach) => (
              <li key={ach.id}>
                <strong>{ach.title}</strong> {ach.year ? `(${ach.year})` : ''}
                {ach.description ? `: ${ach.description}` : ''}
              </li>
            ))}
          </ul>
        </section>
      )}
    </div>
  );
};

export default ExperiencedTemplate;
