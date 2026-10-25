import React from 'react';

const ResumeImportModal = ({ isOpen, onClose, initialData, onImportResume, onImportProfile }) => {
  if (!isOpen) return null;

  const uploadedResumes = initialData?.uploadedResumes || [];

  const parseJsonSafe = (jsonStr, defaultVal) => {
    if (!jsonStr) return defaultVal;
    try {
      return JSON.parse(jsonStr);
    } catch (e) {
      return defaultVal;
    }
  };

  const handleSelectResume = (resume) => {
    // Extract structured data from uploaded resume
    const skillsArray = parseJsonSafe(resume.skillsJson, []);
    const experienceArray = parseJsonSafe(resume.experienceJson, []);
    const educationArray = parseJsonSafe(resume.educationJson, []);
    const certificationsArray = parseJsonSafe(resume.certificationsJson, []);
    const rawAi = parseJsonSafe(resume.rawAiAnalysis, {});

    // Map experience
    const mappedExperience = experienceArray.map((exp, idx) => ({
      id: `exp-${Date.now()}-${idx}`,
      title: exp.role || 'Software Engineer',
      company: exp.company || 'Company',
      location: exp.location || '',
      startDate: exp.duration?.split('-')[0]?.trim() || '2022',
      endDate: exp.duration?.split('-')[1]?.trim() || 'Present',
      isCurrent: exp.duration?.toLowerCase().includes('present') || false,
      highlights: exp.highlights && exp.highlights.length > 0 ? exp.highlights : ['Contributed to engineering roadmap and delivered features.']
    }));

    // Map education
    const mappedEducation = educationArray.map((edu, idx) => ({
      id: `edu-${Date.now()}-${idx}`,
      degree: edu.degree || 'B.S. in Computer Science',
      institution: edu.institution || 'University',
      location: edu.location || '',
      graduationYear: edu.graduationYear || '',
      gpa: edu.gpa || '',
      honors: '',
      coursework: ''
    }));

    // Map skills into categories if possible
    let mappedSkills = [];
    if (skillsArray.length > 0) {
      mappedSkills = [
        { category: 'Technical Skills', items: skillsArray }
      ];
    }

    // Map certifications
    const mappedCerts = certificationsArray.map((c, idx) => ({
      id: `cert-${Date.now()}-${idx}`,
      name: typeof c === 'string' ? c : c.name || 'Certification',
      issuer: typeof c === 'object' ? c.issuer || '' : '',
      issueDate: typeof c === 'object' ? c.issueDate || '' : '',
      credentialUrl: ''
    }));

    // Map projects from rawAi or initialData
    let mappedProjects = [];
    if (rawAi?.projects && Array.isArray(rawAi.projects)) {
      mappedProjects = rawAi.projects.map((p, idx) => ({
        id: `proj-${Date.now()}-${idx}`,
        title: p.title || 'Software Project',
        role: 'Developer',
        technologies: p.technologies || '',
        linkUrl: '',
        highlights: p.description ? [p.description] : ['Engineered core system features.']
      }));
    } else if (initialData?.projects && initialData.projects.length > 0) {
      mappedProjects = initialData.projects.map((p, idx) => ({
        id: `proj-${Date.now()}-${idx}`,
        title: p.title,
        role: p.roleContribution || 'Developer',
        technologies: p.technologies || '',
        linkUrl: '',
        highlights: p.description ? [p.description] : []
      }));
    }

    const importedDraft = {
      fullName: initialData?.fullName || '',
      email: initialData?.email || '',
      phone: initialData?.phone || '',
      location: '',
      linkedinUrl: initialData?.linkedinUrl || '',
      githubUrl: initialData?.githubUrl || '',
      portfolioUrl: '',
      headline: initialData?.targetRole || 'Software Engineer',
      summary: rawAi?.summary || initialData?.bio || 'Passionate software engineer experienced in building reliable applications.',
      skills: mappedSkills.length > 0 ? mappedSkills : undefined,
      experience: mappedExperience.length > 0 ? mappedExperience : undefined,
      education: mappedEducation.length > 0 ? mappedEducation : undefined,
      projects: mappedProjects.length > 0 ? mappedProjects : undefined,
      certifications: mappedCerts.length > 0 ? mappedCerts : undefined
    };

    onImportResume(importedDraft, resume.fileName);
    onClose();
  };

  const handleSelectProfileOnly = () => {
    const profileDraft = {
      fullName: initialData?.fullName || '',
      email: initialData?.email || '',
      phone: initialData?.phone || '',
      headline: initialData?.targetRole || '',
      summary: initialData?.bio || '',
      linkedinUrl: initialData?.linkedinUrl || '',
      githubUrl: initialData?.githubUrl || ''
    };
    onImportProfile(profileDraft);
    onClose();
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
        maxWidth: '640px',
        width: '100%',
        maxHeight: '90vh',
        overflowY: 'auto',
        padding: '1.75rem',
        border: '1px solid var(--border)'
      }}>
        {/* Header */}
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.25rem' }}>
          <div>
            <h3 style={{ margin: 0, fontSize: '1.15rem', fontWeight: 600, color: 'var(--text-primary)' }}>
              Import Existing PrepGPT Data
            </h3>
            <p style={{ margin: '0.25rem 0 0 0', fontSize: '0.8125rem', color: 'var(--text-muted)' }}>
              Prefill the ATS Resume Builder with your previously uploaded resume or profile.
            </p>
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

        {/* Profile Card Option */}
        <div style={{
          background: 'var(--surface-raised)',
          border: '1px solid var(--border)',
          borderRadius: 'var(--radius-sm)',
          padding: '1rem',
          marginBottom: '1.25rem',
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          gap: '1rem'
        }}>
          <div>
            <div style={{ fontWeight: 600, fontSize: '0.9rem', color: 'var(--text-primary)' }}>
              PrepGPT Candidate Profile
            </div>
            <div style={{ fontSize: '0.8rem', color: 'var(--text-secondary)', marginTop: '0.2rem' }}>
              {initialData?.fullName || 'Candidate'} · {initialData?.email}
              {initialData?.targetRole ? ` · ${initialData.targetRole}` : ''}
            </div>
          </div>
          <button
            type="button"
            className="btn btn-secondary btn-sm"
            onClick={handleSelectProfileOnly}
          >
            Import Profile
          </button>
        </div>

        {/* Uploaded Resumes List */}
        <div>
          <div style={{ fontSize: '0.8125rem', fontWeight: 600, color: 'var(--text-muted)', textTransform: 'uppercase', marginBottom: '0.65rem' }}>
            My Uploaded Resumes ({uploadedResumes.length})
          </div>

          {uploadedResumes.length === 0 ? (
            <div style={{
              background: 'var(--surface)',
              border: '1px dashed var(--border)',
              borderRadius: 'var(--radius-sm)',
              padding: '1.5rem',
              textAlign: 'center',
              fontSize: '0.8125rem',
              color: 'var(--text-muted)'
            }}>
              No uploaded resumes found. You can fill in the editor directly or upload a resume in the Resumes & Projects tab.
            </div>
          ) : (
            <div style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem' }}>
              {uploadedResumes.map((resume) => {
                const skills = parseJsonSafe(resume.skillsJson, []);
                const exp = parseJsonSafe(resume.experienceJson, []);

                return (
                  <div
                    key={resume.id}
                    style={{
                      background: 'var(--surface)',
                      border: '1px solid var(--border)',
                      borderRadius: 'var(--radius-sm)',
                      padding: '1rem',
                      display: 'flex',
                      justifyContent: 'space-between',
                      alignItems: 'center',
                      gap: '1rem',
                      flexWrap: 'wrap'
                    }}
                  >
                    <div>
                      <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                        <span style={{ fontWeight: 600, fontSize: '0.9rem', color: 'var(--text-primary)' }}>
                          {resume.fileName}
                        </span>
                        {resume.isPrimary && (
                          <span className="badge badge-success" style={{ fontSize: '0.65rem' }}>Primary</span>
                        )}
                      </div>
                      <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', marginTop: '0.25rem' }}>
                        Extracted {skills.length} skills · {exp.length} work experiences
                      </div>
                    </div>

                    <button
                      type="button"
                      className="btn btn-primary btn-sm"
                      onClick={() => handleSelectResume(resume)}
                    >
                      Import & Prefill
                    </button>
                  </div>
                );
              })}
            </div>
          )}
        </div>

        <div style={{ marginTop: '1.25rem', paddingTop: '1rem', borderTop: '1px solid var(--border-light)', display: 'flex', justifyContent: 'flex-end' }}>
          <button
            type="button"
            className="btn btn-secondary"
            onClick={onClose}
          >
            Cancel
          </button>
        </div>
      </div>
    </div>
  );
};

export default ResumeImportModal;
