import React, { useState } from 'react';

const ResumeEditor = ({
  resumeData,
  onChange,
  onOpenAiModal,
  onOpenImportModal
}) => {
  // Accordion active sections
  const [openSections, setOpenSections] = useState({
    contact: true,
    summary: true,
    skills: true,
    experience: true,
    projects: true,
    education: true,
    certifications: false,
    achievements: false
  });

  const toggleSection = (sec) => {
    setOpenSections(prev => ({ ...prev, [sec]: !prev[sec] }));
  };

  // Generic field updater
  const handleFieldChange = (field, value) => {
    onChange({ ...resumeData, [field]: value });
  };

  // Reorder helper
  const moveItem = (listName, index, direction) => {
    const list = [...(resumeData[listName] || [])];
    const targetIndex = index + direction;
    if (targetIndex < 0 || targetIndex >= list.length) return;
    const temp = list[index];
    list[index] = list[targetIndex];
    list[targetIndex] = temp;
    onChange({ ...resumeData, [listName]: list });
  };

  // Remove helper
  const removeItem = (listName, index) => {
    const list = (resumeData[listName] || []).filter((_, i) => i !== index);
    onChange({ ...resumeData, [listName]: list });
  };

  // ==================== WORK EXPERIENCE HANDLERS ====================
  const addExperience = () => {
    const newExp = {
      id: `exp-${Date.now()}`,
      title: '',
      company: '',
      location: '',
      startDate: '',
      endDate: '',
      isCurrent: false,
      highlights: ['Accomplished [X], measured by [Y], by doing [Z]']
    };
    onChange({
      ...resumeData,
      experience: [...(resumeData.experience || []), newExp]
    });
  };

  const updateExperience = (index, field, value) => {
    const updated = [...(resumeData.experience || [])];
    updated[index] = { ...updated[index], [field]: value };
    onChange({ ...resumeData, experience: updated });
  };

  const addExperienceBullet = (expIndex) => {
    const updated = [...(resumeData.experience || [])];
    const highlights = [...(updated[expIndex].highlights || []), ''];
    updated[expIndex] = { ...updated[expIndex], highlights };
    onChange({ ...resumeData, experience: updated });
  };

  const updateExperienceBullet = (expIndex, bulletIndex, value) => {
    const updated = [...(resumeData.experience || [])];
    const highlights = [...(updated[expIndex].highlights || [])];
    highlights[bulletIndex] = value;
    updated[expIndex] = { ...updated[expIndex], highlights };
    onChange({ ...resumeData, experience: updated });
  };

  const removeExperienceBullet = (expIndex, bulletIndex) => {
    const updated = [...(resumeData.experience || [])];
    const highlights = (updated[expIndex].highlights || []).filter((_, i) => i !== bulletIndex);
    updated[expIndex] = { ...updated[expIndex], highlights };
    onChange({ ...resumeData, experience: updated });
  };

  // ==================== PROJECTS HANDLERS ====================
  const addProject = () => {
    const newProj = {
      id: `proj-${Date.now()}`,
      title: '',
      role: 'Developer',
      technologies: '',
      linkUrl: '',
      highlights: ['Engineered key functionalities and scalable endpoints.']
    };
    onChange({
      ...resumeData,
      projects: [...(resumeData.projects || []), newProj]
    });
  };

  const updateProject = (index, field, value) => {
    const updated = [...(resumeData.projects || [])];
    updated[index] = { ...updated[index], [field]: value };
    onChange({ ...resumeData, projects: updated });
  };

  const addProjectBullet = (projIndex) => {
    const updated = [...(resumeData.projects || [])];
    const highlights = [...(updated[projIndex].highlights || []), ''];
    updated[projIndex] = { ...updated[projIndex], highlights };
    onChange({ ...resumeData, projects: updated });
  };

  const updateProjectBullet = (projIndex, bulletIndex, value) => {
    const updated = [...(resumeData.projects || [])];
    const highlights = [...(updated[projIndex].highlights || [])];
    highlights[bulletIndex] = value;
    updated[projIndex] = { ...updated[projIndex], highlights };
    onChange({ ...resumeData, projects: updated });
  };

  const removeProjectBullet = (projIndex, bulletIndex) => {
    const updated = [...(resumeData.projects || [])];
    const highlights = (updated[projIndex].highlights || []).filter((_, i) => i !== bulletIndex);
    updated[projIndex] = { ...updated[projIndex], highlights };
    onChange({ ...resumeData, projects: updated });
  };

  // ==================== EDUCATION HANDLERS ====================
  const addEducation = () => {
    const newEdu = {
      id: `edu-${Date.now()}`,
      degree: 'B.S. in Computer Science',
      institution: '',
      location: '',
      graduationYear: '',
      gpa: '',
      honors: '',
      coursework: ''
    };
    onChange({
      ...resumeData,
      education: [...(resumeData.education || []), newEdu]
    });
  };

  const updateEducation = (index, field, value) => {
    const updated = [...(resumeData.education || [])];
    updated[index] = { ...updated[index], [field]: value };
    onChange({ ...resumeData, education: updated });
  };

  // ==================== SKILLS HANDLERS ====================
  const updateSkillCategory = (catIndex, field, value) => {
    const skills = [...(resumeData.skills || [])];
    skills[catIndex] = { ...skills[catIndex], [field]: value };
    onChange({ ...resumeData, skills });
  };

  const updateSkillItems = (catIndex, commaSeparatedString) => {
    const skills = [...(resumeData.skills || [])];
    const items = commaSeparatedString.split(',').map(s => s.trim()).filter(Boolean);
    skills[catIndex] = { ...skills[catIndex], items };
    onChange({ ...resumeData, skills });
  };

  const addSkillCategory = () => {
    const newGroup = { category: 'Tools & Platforms', items: ['Git', 'Docker'] };
    onChange({
      ...resumeData,
      skills: [...(resumeData.skills || []), newGroup]
    });
  };

  const removeSkillCategory = (catIndex) => {
    const skills = (resumeData.skills || []).filter((_, i) => i !== catIndex);
    onChange({ ...resumeData, skills });
  };

  // ==================== CERTIFICATIONS & ACHIEVEMENTS ====================
  const addCertification = () => {
    const newCert = {
      id: `cert-${Date.now()}`,
      name: '',
      issuer: '',
      issueDate: '',
      credentialUrl: ''
    };
    onChange({
      ...resumeData,
      certifications: [...(resumeData.certifications || []), newCert]
    });
  };

  const updateCertification = (index, field, value) => {
    const updated = [...(resumeData.certifications || [])];
    updated[index] = { ...updated[index], [field]: value };
    onChange({ ...resumeData, certifications: updated });
  };

  const addAchievement = () => {
    const newAch = {
      id: `ach-${Date.now()}`,
      title: '',
      issuer: '',
      year: '',
      description: ''
    };
    onChange({
      ...resumeData,
      achievements: [...(resumeData.achievements || []), newAch]
    });
  };

  const updateAchievement = (index, field, value) => {
    const updated = [...(resumeData.achievements || [])];
    updated[index] = { ...updated[index], [field]: value };
    onChange({ ...resumeData, achievements: updated });
  };

  return (
    <div className="editor-panel">
      {/* Top Action Bar */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '0.75rem' }}>
        <button
          type="button"
          className="btn btn-secondary btn-sm"
          onClick={onOpenImportModal}
          style={{ display: 'inline-flex', alignItems: 'center', gap: '0.35rem' }}
        >
          <span>📥</span> Import Uploaded Resume or Profile
        </button>

        <button
          type="button"
          className="ai-btn-sm"
          onClick={() => onOpenAiModal('JD_KEYWORD_MATCH')}
        >
          <span className="ai-pulse-dot" style={{ width: '6px', height: '6px' }} />
          ATS Keyword Matcher
        </button>
      </div>

      {/* SECTION 1: CONTACT & IDENTITY */}
      <div className="editor-card">
        <div
          className="editor-section-header"
          style={{ cursor: 'pointer', margin: openSections.contact ? '0 0 1rem 0' : 0 }}
          onClick={() => toggleSection('contact')}
        >
          <h3 className="editor-section-title">
            <span>👤</span> Contact & Identification
          </h3>
          <span style={{ fontSize: '0.8125rem', color: 'var(--text-muted)' }}>
            {openSections.contact ? '▲' : '▼'}
          </span>
        </div>

        {openSections.contact && (
          <div>
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))', gap: '0.875rem' }}>
              <div className="form-group">
                <label className="form-label">Full Name *</label>
                <input
                  type="text"
                  className="form-input"
                  value={resumeData.fullName || ''}
                  onChange={(e) => handleFieldChange('fullName', e.target.value)}
                  placeholder="e.g. Alex Morgan"
                />
              </div>

              <div className="form-group">
                <label className="form-label">Professional Headline</label>
                <input
                  type="text"
                  className="form-input"
                  value={resumeData.headline || ''}
                  onChange={(e) => handleFieldChange('headline', e.target.value)}
                  placeholder="e.g. Senior Backend Engineer"
                />
              </div>
            </div>

            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(180px, 1fr))', gap: '0.875rem', marginTop: '0.75rem' }}>
              <div className="form-group">
                <label className="form-label">Email *</label>
                <input
                  type="email"
                  className="form-input"
                  value={resumeData.email || ''}
                  onChange={(e) => handleFieldChange('email', e.target.value)}
                  placeholder="name@example.com"
                />
              </div>

              <div className="form-group">
                <label className="form-label">Phone</label>
                <input
                  type="text"
                  className="form-input"
                  value={resumeData.phone || ''}
                  onChange={(e) => handleFieldChange('phone', e.target.value)}
                  placeholder="+1 (555) 000-0000"
                />
              </div>

              <div className="form-group">
                <label className="form-label">Location (City, State)</label>
                <input
                  type="text"
                  className="form-input"
                  value={resumeData.location || ''}
                  onChange={(e) => handleFieldChange('location', e.target.value)}
                  placeholder="San Francisco, CA"
                />
              </div>
            </div>

            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(180px, 1fr))', gap: '0.875rem', marginTop: '0.75rem' }}>
              <div className="form-group">
                <label className="form-label">LinkedIn URL</label>
                <input
                  type="text"
                  className="form-input"
                  value={resumeData.linkedinUrl || ''}
                  onChange={(e) => handleFieldChange('linkedinUrl', e.target.value)}
                  placeholder="https://linkedin.com/in/username"
                />
              </div>

              <div className="form-group">
                <label className="form-label">GitHub URL</label>
                <input
                  type="text"
                  className="form-input"
                  value={resumeData.githubUrl || ''}
                  onChange={(e) => handleFieldChange('githubUrl', e.target.value)}
                  placeholder="https://github.com/username"
                />
              </div>

              <div className="form-group">
                <label className="form-label">Portfolio / Website</label>
                <input
                  type="text"
                  className="form-input"
                  value={resumeData.portfolioUrl || ''}
                  onChange={(e) => handleFieldChange('portfolioUrl', e.target.value)}
                  placeholder="https://alexmorgan.dev"
                />
              </div>
            </div>
          </div>
        )}
      </div>

      {/* SECTION 2: PROFESSIONAL SUMMARY */}
      <div className="editor-card">
        <div
          className="editor-section-header"
          style={{ cursor: 'pointer', margin: openSections.summary ? '0 0 1rem 0' : 0 }}
          onClick={() => toggleSection('summary')}
        >
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.625rem' }}>
            <h3 className="editor-section-title">
              <span>📝</span> Professional Summary
            </h3>
            <button
              type="button"
              className="ai-btn-sm"
              onClick={(e) => {
                e.stopPropagation();
                onOpenAiModal('IMPROVE_SUMMARY');
              }}
            >
              ✨ AI Polish
            </button>
          </div>
          <span style={{ fontSize: '0.8125rem', color: 'var(--text-muted)' }}>
            {openSections.summary ? '▲' : '▼'}
          </span>
        </div>

        {openSections.summary && (
          <div className="form-group">
            <textarea
              className="form-textarea"
              rows="3"
              value={resumeData.summary || ''}
              onChange={(e) => handleFieldChange('summary', e.target.value)}
              placeholder="2-3 high-impact sentences describing your technical strengths and core achievements..."
            />
            <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', marginTop: '0.25rem' }}>
              Tip: Keep under 60 words. Avoid generic buzzwords; highlight concrete competencies and technologies.
            </div>
          </div>
        )}
      </div>

      {/* SECTION 3: TECHNICAL SKILLS */}
      <div className="editor-card">
        <div
          className="editor-section-header"
          style={{ cursor: 'pointer', margin: openSections.skills ? '0 0 1rem 0' : 0 }}
          onClick={() => toggleSection('skills')}
        >
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.625rem' }}>
            <h3 className="editor-section-title">
              <span>⚡</span> Technical Skills
            </h3>
            <button
              type="button"
              className="ai-btn-sm"
              onClick={(e) => {
                e.stopPropagation();
                onOpenAiModal('SUGGEST_SKILLS');
              }}
            >
              ✨ Suggest Skills
            </button>
          </div>
          <span style={{ fontSize: '0.8125rem', color: 'var(--text-muted)' }}>
            {openSections.skills ? '▲' : '▼'}
          </span>
        </div>

        {openSections.skills && (
          <div>
            {(resumeData.skills || []).map((cat, catIdx) => (
              <div key={catIdx} className="repeatable-item">
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.5rem' }}>
                  <input
                    type="text"
                    className="form-input"
                    style={{ fontWeight: 600, width: '220px' }}
                    value={cat.category}
                    onChange={(e) => updateSkillCategory(catIdx, 'category', e.target.value)}
                    placeholder="e.g. Languages"
                  />
                  <button
                    type="button"
                    className="control-icon-btn danger"
                    onClick={() => removeSkillCategory(catIdx)}
                    title="Remove Skill Category"
                  >
                    🗑
                  </button>
                </div>
                <div>
                  <input
                    type="text"
                    className="form-input"
                    value={(cat.items || []).join(', ')}
                    onChange={(e) => updateSkillItems(catIdx, e.target.value)}
                    placeholder="Java, Python, TypeScript, SQL (comma-separated)"
                  />
                </div>
              </div>
            ))}

            <button
              type="button"
              className="btn btn-secondary btn-sm"
              onClick={addSkillCategory}
              style={{ marginTop: '0.5rem' }}
            >
              + Add Skill Category
            </button>
          </div>
        )}
      </div>

      {/* SECTION 4: WORK EXPERIENCE */}
      <div className="editor-card">
        <div
          className="editor-section-header"
          style={{ cursor: 'pointer', margin: openSections.experience ? '0 0 1rem 0' : 0 }}
          onClick={() => toggleSection('experience')}
        >
          <h3 className="editor-section-title">
            <span>💼</span> Work Experience ({(resumeData.experience || []).length})
          </h3>
          <span style={{ fontSize: '0.8125rem', color: 'var(--text-muted)' }}>
            {openSections.experience ? '▲' : '▼'}
          </span>
        </div>

        {openSections.experience && (
          <div>
            {(resumeData.experience || []).map((exp, expIdx) => (
              <div key={exp.id || expIdx} className="repeatable-item">
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.75rem' }}>
                  <span style={{ fontSize: '0.8125rem', fontWeight: 600, color: 'var(--text-accent)' }}>
                    Entry #{expIdx + 1}
                  </span>
                  <div className="repeatable-controls">
                    <button
                      type="button"
                      className="control-icon-btn"
                      onClick={() => moveItem('experience', expIdx, -1)}
                      disabled={expIdx === 0}
                      title="Move Up"
                    >
                      ▲
                    </button>
                    <button
                      type="button"
                      className="control-icon-btn"
                      onClick={() => moveItem('experience', expIdx, 1)}
                      disabled={expIdx === (resumeData.experience || []).length - 1}
                      title="Move Down"
                    >
                      ▼
                    </button>
                    <button
                      type="button"
                      className="control-icon-btn danger"
                      onClick={() => removeItem('experience', expIdx)}
                      title="Remove Entry"
                    >
                      ✕
                    </button>
                  </div>
                </div>

                <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', gap: '0.75rem' }}>
                  <div className="form-group">
                    <label className="form-label">Job Title *</label>
                    <input
                      type="text"
                      className="form-input"
                      value={exp.title || ''}
                      onChange={(e) => updateExperience(expIdx, 'title', e.target.value)}
                      placeholder="e.g. Software Engineer"
                    />
                  </div>
                  <div className="form-group">
                    <label className="form-label">Company Name *</label>
                    <input
                      type="text"
                      className="form-input"
                      value={exp.company || ''}
                      onChange={(e) => updateExperience(expIdx, 'company', e.target.value)}
                      placeholder="e.g. Apex Solutions"
                    />
                  </div>
                </div>

                <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(140px, 1fr))', gap: '0.75rem', marginTop: '0.5rem' }}>
                  <div className="form-group">
                    <label className="form-label">Start Date</label>
                    <input
                      type="text"
                      className="form-input"
                      value={exp.startDate || ''}
                      onChange={(e) => updateExperience(expIdx, 'startDate', e.target.value)}
                      placeholder="e.g. Aug 2022"
                    />
                  </div>
                  <div className="form-group">
                    <label className="form-label">End Date</label>
                    <input
                      type="text"
                      className="form-input"
                      disabled={exp.isCurrent}
                      value={exp.isCurrent ? 'Present' : exp.endDate || ''}
                      onChange={(e) => updateExperience(expIdx, 'endDate', e.target.value)}
                      placeholder="e.g. Present"
                    />
                  </div>
                  <div className="form-group">
                    <label className="form-label">Location</label>
                    <input
                      type="text"
                      className="form-input"
                      value={exp.location || ''}
                      onChange={(e) => updateExperience(expIdx, 'location', e.target.value)}
                      placeholder="e.g. San Francisco, CA"
                    />
                  </div>
                </div>

                <div style={{ marginTop: '0.5rem', marginBottom: '0.75rem' }}>
                  <label style={{ display: 'inline-flex', alignItems: 'center', gap: '0.4rem', fontSize: '0.8125rem', color: 'var(--text-secondary)', cursor: 'pointer' }}>
                    <input
                      type="checkbox"
                      checked={Boolean(exp.isCurrent)}
                      onChange={(e) => updateExperience(expIdx, 'isCurrent', e.target.checked)}
                    />
                    I currently work in this role
                  </label>
                </div>

                {/* Bullets */}
                <div>
                  <label className="form-label" style={{ marginBottom: '0.35rem' }}>
                    Responsibilities & Achievements (STAR Bullets)
                  </label>
                  {(exp.highlights || []).map((bullet, bIdx) => (
                    <div key={bIdx} style={{ display: 'flex', gap: '0.35rem', alignItems: 'flex-start', marginBottom: '0.5rem' }}>
                      <span style={{ color: 'var(--text-muted)', paddingTop: '0.4rem' }}>•</span>
                      <textarea
                        className="form-textarea"
                        rows="2"
                        style={{ flexGrow: 1 }}
                        value={bullet}
                        onChange={(e) => updateExperienceBullet(expIdx, bIdx, e.target.value)}
                        placeholder="Action verb + task + quantifiable metric..."
                      />
                      <button
                        type="button"
                        className="ai-btn-sm"
                        style={{ padding: '0.35rem 0.5rem', flexShrink: 0 }}
                        onClick={() => onOpenAiModal('IMPROVE_BULLET', { expIndex: expIdx, bulletIndex: bIdx, bullet })}
                        title="AI Rewrite using STAR format"
                      >
                        ✨
                      </button>
                      <button
                        type="button"
                        className="control-icon-btn danger"
                        style={{ padding: '0.35rem 0.5rem', flexShrink: 0 }}
                        onClick={() => removeExperienceBullet(expIdx, bIdx)}
                        title="Delete Bullet"
                      >
                        ✕
                      </button>
                    </div>
                  ))}
                  <button
                    type="button"
                    className="btn btn-secondary btn-sm"
                    onClick={() => addExperienceBullet(expIdx)}
                    style={{ fontSize: '0.75rem', marginTop: '0.25rem' }}
                  >
                    + Add Bullet Point
                  </button>
                </div>
              </div>
            ))}

            <button
              type="button"
              className="btn btn-secondary btn-sm"
              onClick={addExperience}
            >
              + Add Work Experience
            </button>
          </div>
        )}
      </div>

      {/* SECTION 5: TECHNICAL PROJECTS */}
      <div className="editor-card">
        <div
          className="editor-section-header"
          style={{ cursor: 'pointer', margin: openSections.projects ? '0 0 1rem 0' : 0 }}
          onClick={() => toggleSection('projects')}
        >
          <h3 className="editor-section-title">
            <span>🚀</span> Technical Projects ({(resumeData.projects || []).length})
          </h3>
          <span style={{ fontSize: '0.8125rem', color: 'var(--text-muted)' }}>
            {openSections.projects ? '▲' : '▼'}
          </span>
        </div>

        {openSections.projects && (
          <div>
            {(resumeData.projects || []).map((proj, pIdx) => (
              <div key={proj.id || pIdx} className="repeatable-item">
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.75rem' }}>
                  <span style={{ fontSize: '0.8125rem', fontWeight: 600, color: 'var(--text-accent)' }}>
                    Project #{pIdx + 1}
                  </span>
                  <div className="repeatable-controls">
                    <button
                      type="button"
                      className="control-icon-btn"
                      onClick={() => moveItem('projects', pIdx, -1)}
                      disabled={pIdx === 0}
                      title="Move Up"
                    >
                      ▲
                    </button>
                    <button
                      type="button"
                      className="control-icon-btn"
                      onClick={() => moveItem('projects', pIdx, 1)}
                      disabled={pIdx === (resumeData.projects || []).length - 1}
                      title="Move Down"
                    >
                      ▼
                    </button>
                    <button
                      type="button"
                      className="control-icon-btn danger"
                      onClick={() => removeItem('projects', pIdx)}
                      title="Remove Entry"
                    >
                      ✕
                    </button>
                  </div>
                </div>

                <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', gap: '0.75rem' }}>
                  <div className="form-group">
                    <label className="form-label">Project Title *</label>
                    <input
                      type="text"
                      className="form-input"
                      value={proj.title || ''}
                      onChange={(e) => updateProject(pIdx, 'title', e.target.value)}
                      placeholder="e.g. Distributed Rate Limiter"
                    />
                  </div>
                  <div className="form-group">
                    <label className="form-label">Role</label>
                    <input
                      type="text"
                      className="form-input"
                      value={proj.role || ''}
                      onChange={(e) => updateProject(pIdx, 'role', e.target.value)}
                      placeholder="e.g. Lead Developer"
                    />
                  </div>
                </div>

                <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', gap: '0.75rem', marginTop: '0.5rem' }}>
                  <div className="form-group">
                    <label className="form-label">Technologies Used</label>
                    <input
                      type="text"
                      className="form-input"
                      value={proj.technologies || ''}
                      onChange={(e) => updateProject(pIdx, 'technologies', e.target.value)}
                      placeholder="Java, Redis, Docker, Spring Boot"
                    />
                  </div>
                  <div className="form-group">
                    <label className="form-label">Repository / Demo URL</label>
                    <input
                      type="text"
                      className="form-input"
                      value={proj.linkUrl || ''}
                      onChange={(e) => updateProject(pIdx, 'linkUrl', e.target.value)}
                      placeholder="https://github.com/..."
                    />
                  </div>
                </div>

                {/* Bullets */}
                <div style={{ marginTop: '0.75rem' }}>
                  <label className="form-label" style={{ marginBottom: '0.35rem' }}>
                    Key Engineering Contributions & System Metrics
                  </label>
                  {(proj.highlights || []).map((bullet, bIdx) => (
                    <div key={bIdx} style={{ display: 'flex', gap: '0.35rem', alignItems: 'flex-start', marginBottom: '0.5rem' }}>
                      <span style={{ color: 'var(--text-muted)', paddingTop: '0.4rem' }}>•</span>
                      <textarea
                        className="form-textarea"
                        rows="2"
                        style={{ flexGrow: 1 }}
                        value={bullet}
                        onChange={(e) => updateProjectBullet(pIdx, bIdx, e.target.value)}
                        placeholder="Implemented algorithms, benchmarked latency..."
                      />
                      <button
                        type="button"
                        className="ai-btn-sm"
                        style={{ padding: '0.35rem 0.5rem', flexShrink: 0 }}
                        onClick={() => onOpenAiModal('IMPROVE_BULLET', { projIndex: pIdx, bulletIndex: bIdx, bullet })}
                        title="AI Rewrite bullet"
                      >
                        ✨
                      </button>
                      <button
                        type="button"
                        className="control-icon-btn danger"
                        style={{ padding: '0.35rem 0.5rem', flexShrink: 0 }}
                        onClick={() => removeProjectBullet(pIdx, bIdx)}
                        title="Delete Bullet"
                      >
                        ✕
                      </button>
                    </div>
                  ))}
                  <button
                    type="button"
                    className="btn btn-secondary btn-sm"
                    onClick={() => addProjectBullet(pIdx)}
                    style={{ fontSize: '0.75rem', marginTop: '0.25rem' }}
                  >
                    + Add Bullet Point
                  </button>
                </div>
              </div>
            ))}

            <button
              type="button"
              className="btn btn-secondary btn-sm"
              onClick={addProject}
            >
              + Add Technical Project
            </button>
          </div>
        )}
      </div>

      {/* SECTION 6: EDUCATION */}
      <div className="editor-card">
        <div
          className="editor-section-header"
          style={{ cursor: 'pointer', margin: openSections.education ? '0 0 1rem 0' : 0 }}
          onClick={() => toggleSection('education')}
        >
          <h3 className="editor-section-title">
            <span>🎓</span> Education ({(resumeData.education || []).length})
          </h3>
          <span style={{ fontSize: '0.8125rem', color: 'var(--text-muted)' }}>
            {openSections.education ? '▲' : '▼'}
          </span>
        </div>

        {openSections.education && (
          <div>
            {(resumeData.education || []).map((edu, eduIdx) => (
              <div key={edu.id || eduIdx} className="repeatable-item">
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.75rem' }}>
                  <span style={{ fontSize: '0.8125rem', fontWeight: 600, color: 'var(--text-accent)' }}>
                    Education #{eduIdx + 1}
                  </span>
                  <div className="repeatable-controls">
                    <button
                      type="button"
                      className="control-icon-btn"
                      onClick={() => moveItem('education', eduIdx, -1)}
                      disabled={eduIdx === 0}
                      title="Move Up"
                    >
                      ▲
                    </button>
                    <button
                      type="button"
                      className="control-icon-btn"
                      onClick={() => moveItem('education', eduIdx, 1)}
                      disabled={eduIdx === (resumeData.education || []).length - 1}
                      title="Move Down"
                    >
                      ▼
                    </button>
                    <button
                      type="button"
                      className="control-icon-btn danger"
                      onClick={() => removeItem('education', eduIdx)}
                      title="Remove Entry"
                    >
                      ✕
                    </button>
                  </div>
                </div>

                <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', gap: '0.75rem' }}>
                  <div className="form-group">
                    <label className="form-label">Degree / Qualification *</label>
                    <input
                      type="text"
                      className="form-input"
                      value={edu.degree || ''}
                      onChange={(e) => updateEducation(eduIdx, 'degree', e.target.value)}
                      placeholder="e.g. B.S. Computer Science"
                    />
                  </div>
                  <div className="form-group">
                    <label className="form-label">Institution Name *</label>
                    <input
                      type="text"
                      className="form-input"
                      value={edu.institution || ''}
                      onChange={(e) => updateEducation(eduIdx, 'institution', e.target.value)}
                      placeholder="e.g. University of California, Davis"
                    />
                  </div>
                </div>

                <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(130px, 1fr))', gap: '0.75rem', marginTop: '0.5rem' }}>
                  <div className="form-group">
                    <label className="form-label">Graduation Year</label>
                    <input
                      type="text"
                      className="form-input"
                      value={edu.graduationYear || ''}
                      onChange={(e) => updateEducation(eduIdx, 'graduationYear', e.target.value)}
                      placeholder="e.g. 2024"
                    />
                  </div>
                  <div className="form-group">
                    <label className="form-label">Location</label>
                    <input
                      type="text"
                      className="form-input"
                      value={edu.location || ''}
                      onChange={(e) => updateEducation(eduIdx, 'location', e.target.value)}
                      placeholder="Davis, CA"
                    />
                  </div>
                  <div className="form-group">
                    <label className="form-label">GPA (Optional)</label>
                    <input
                      type="text"
                      className="form-input"
                      value={edu.gpa || ''}
                      onChange={(e) => updateEducation(eduIdx, 'gpa', e.target.value)}
                      placeholder="3.8 / 4.0"
                    />
                  </div>
                </div>

                <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', gap: '0.75rem', marginTop: '0.5rem' }}>
                  <div className="form-group">
                    <label className="form-label">Honors / Distinctions (Optional)</label>
                    <input
                      type="text"
                      className="form-input"
                      value={edu.honors || ''}
                      onChange={(e) => updateEducation(eduIdx, 'honors', e.target.value)}
                      placeholder="Dean's List, Magna Cum Laude"
                    />
                  </div>
                  <div className="form-group">
                    <label className="form-label">Relevant Coursework (Optional)</label>
                    <input
                      type="text"
                      className="form-input"
                      value={edu.coursework || ''}
                      onChange={(e) => updateEducation(eduIdx, 'coursework', e.target.value)}
                      placeholder="Algorithms, Operating Systems, Distributed Systems"
                    />
                  </div>
                </div>
              </div>
            ))}

            <button
              type="button"
              className="btn btn-secondary btn-sm"
              onClick={addEducation}
            >
              + Add Education
            </button>
          </div>
        )}
      </div>

      {/* SECTION 7: CERTIFICATIONS */}
      <div className="editor-card">
        <div
          className="editor-section-header"
          style={{ cursor: 'pointer', margin: openSections.certifications ? '0 0 1rem 0' : 0 }}
          onClick={() => toggleSection('certifications')}
        >
          <h3 className="editor-section-title">
            <span>📜</span> Certifications ({(resumeData.certifications || []).length})
          </h3>
          <span style={{ fontSize: '0.8125rem', color: 'var(--text-muted)' }}>
            {openSections.certifications ? '▲' : '▼'}
          </span>
        </div>

        {openSections.certifications && (
          <div>
            {(resumeData.certifications || []).map((cert, cIdx) => (
              <div key={cert.id || cIdx} className="repeatable-item">
                <div style={{ display: 'flex', justifyContent: 'flex-end', marginBottom: '0.5rem' }}>
                  <button
                    type="button"
                    className="control-icon-btn danger"
                    onClick={() => removeItem('certifications', cIdx)}
                    title="Remove Certification"
                  >
                    ✕
                  </button>
                </div>
                <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', gap: '0.75rem' }}>
                  <div className="form-group">
                    <label className="form-label">Certification Name *</label>
                    <input
                      type="text"
                      className="form-input"
                      value={cert.name || ''}
                      onChange={(e) => updateCertification(cIdx, 'name', e.target.value)}
                      placeholder="AWS Certified Solutions Architect"
                    />
                  </div>
                  <div className="form-group">
                    <label className="form-label">Issuing Organization</label>
                    <input
                      type="text"
                      className="form-input"
                      value={cert.issuer || ''}
                      onChange={(e) => updateCertification(cIdx, 'issuer', e.target.value)}
                      placeholder="Amazon Web Services"
                    />
                  </div>
                  <div className="form-group">
                    <label className="form-label">Year / Date</label>
                    <input
                      type="text"
                      className="form-input"
                      value={cert.issueDate || ''}
                      onChange={(e) => updateCertification(cIdx, 'issueDate', e.target.value)}
                      placeholder="2023"
                    />
                  </div>
                </div>
              </div>
            ))}

            <button
              type="button"
              className="btn btn-secondary btn-sm"
              onClick={addCertification}
            >
              + Add Certification
            </button>
          </div>
        )}
      </div>

      {/* SECTION 8: ACHIEVEMENTS & AWARDS */}
      <div className="editor-card">
        <div
          className="editor-section-header"
          style={{ cursor: 'pointer', margin: openSections.achievements ? '0 0 1rem 0' : 0 }}
          onClick={() => toggleSection('achievements')}
        >
          <h3 className="editor-section-title">
            <span>🏆</span> Achievements & Awards ({(resumeData.achievements || []).length})
          </h3>
          <span style={{ fontSize: '0.8125rem', color: 'var(--text-muted)' }}>
            {openSections.achievements ? '▲' : '▼'}
          </span>
        </div>

        {openSections.achievements && (
          <div>
            {(resumeData.achievements || []).map((ach, aIdx) => (
              <div key={ach.id || aIdx} className="repeatable-item">
                <div style={{ display: 'flex', justifyContent: 'flex-end', marginBottom: '0.5rem' }}>
                  <button
                    type="button"
                    className="control-icon-btn danger"
                    onClick={() => removeItem('achievements', aIdx)}
                    title="Remove Achievement"
                  >
                    ✕
                  </button>
                </div>
                <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', gap: '0.75rem' }}>
                  <div className="form-group">
                    <label className="form-label">Award / Honor Title *</label>
                    <input
                      type="text"
                      className="form-input"
                      value={ach.title || ''}
                      onChange={(e) => updateAchievement(aIdx, 'title', e.target.value)}
                      placeholder="1st Place Hackathon Winner"
                    />
                  </div>
                  <div className="form-group">
                    <label className="form-label">Year</label>
                    <input
                      type="text"
                      className="form-input"
                      value={ach.year || ''}
                      onChange={(e) => updateAchievement(aIdx, 'year', e.target.value)}
                      placeholder="2022"
                    />
                  </div>
                </div>
                <div className="form-group" style={{ marginTop: '0.5rem' }}>
                  <label className="form-label">Details / Impact</label>
                  <input
                    type="text"
                    className="form-input"
                    value={ach.description || ''}
                    onChange={(e) => updateAchievement(aIdx, 'description', e.target.value)}
                    placeholder="Built an accessibility tool chosen out of 200+ teams."
                  />
                </div>
              </div>
            ))}

            <button
              type="button"
              className="btn btn-secondary btn-sm"
              onClick={addAchievement}
            >
              + Add Achievement
            </button>
          </div>
        )}
      </div>
    </div>
  );
};

export default ResumeEditor;
