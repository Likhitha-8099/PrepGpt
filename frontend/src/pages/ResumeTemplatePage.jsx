import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import MainLayout from '../layouts/MainLayout';
import TemplateSelector from '../components/resume/TemplateSelector';
import ResumeEditor from '../components/resume/ResumeEditor';
import ResumePreviewRenderer from '../components/resume/ResumePreviewRenderer';
import AiAssistModal from '../components/resume/AiAssistModal';
import ResumeImportModal from '../components/resume/ResumeImportModal';
import {
  INITIAL_RESUME_DATA,
  RESUME_TEMPLATES_METADATA
} from '../components/resume/utils/defaultResumeData';
import { downloadPlainTextResume, generatePlainTextResume } from '../components/resume/utils/exportPlainText';
import { downloadWordResume } from '../components/resume/utils/exportDocx';
import {
  getResumeBuilderInitialData,
  saveResumeToResumes
} from '../services/resumeBuilderService';
import '../assets/styles/resume-templates.css';

const LOCAL_STORAGE_DRAFT_KEY = 'prepgpt_resume_builder_draft';
const LOCAL_STORAGE_TEMPLATE_KEY = 'prepgpt_resume_builder_template';

const ResumeTemplatePage = () => {
  const navigate = useNavigate();

  // Active view: 'gallery' | 'builder'
  const [activeTab, setActiveTab] = useState('gallery');

  // Selected template
  const [selectedTemplate, setSelectedTemplate] = useState(() => {
    return localStorage.getItem(LOCAL_STORAGE_TEMPLATE_KEY) || 'classic-professional';
  });

  // Resume data
  const [resumeData, setResumeData] = useState(() => {
    try {
      const saved = localStorage.getItem(LOCAL_STORAGE_DRAFT_KEY);
      if (saved) return JSON.parse(saved);
    } catch (e) {
      console.warn('Failed to parse cached resume draft:', e);
    }
    return INITIAL_RESUME_DATA;
  });

  // Server initial data (profile, uploaded resumes, projects)
  const [initialData, setInitialData] = useState(null);
  const [loadingInitial, setLoadingInitial] = useState(false);

  // Modals state
  const [showAiModal, setShowAiModal] = useState(false);
  const [aiModalAction, setAiModalAction] = useState('IMPROVE_SUMMARY');
  const [aiModalContext, setAiModalContext] = useState(null);
  const [showImportModal, setShowImportModal] = useState(false);

  // Saving state
  const [savingToResumes, setSavingToResumes] = useState(false);
  const [successMessage, setSuccessMessage] = useState('');
  const [errorMessage, setErrorMessage] = useState('');

  // Persist draft to localStorage on change
  useEffect(() => {
    try {
      localStorage.setItem(LOCAL_STORAGE_DRAFT_KEY, JSON.stringify(resumeData));
    } catch (e) {
      console.warn('LocalStorage save failed:', e);
    }
  }, [resumeData]);

  // Persist selected template to localStorage
  useEffect(() => {
    localStorage.setItem(LOCAL_STORAGE_TEMPLATE_KEY, selectedTemplate);
  }, [selectedTemplate]);

  // Fetch initial profile & resume data on mount
  useEffect(() => {
    fetchInitialData();
  }, []);

  const fetchInitialData = async () => {
    setLoadingInitial(true);
    try {
      const data = await getResumeBuilderInitialData();
      setInitialData(data);
    } catch (err) {
      console.warn('Could not fetch candidate assets for prefill:', err);
    } finally {
      setLoadingInitial(false);
    }
  };

  // Switch template
  const handleSelectTemplate = (templateId) => {
    setSelectedTemplate(templateId);
    setActiveTab('builder');
  };

  // Open AI modal with specific action
  const handleOpenAiModal = (action = 'IMPROVE_SUMMARY', context = null) => {
    setAiModalAction(action);
    setAiModalContext(context);
    setShowAiModal(true);
  };

  // Apply AI result to resume data
  const handleApplyAiResult = (action, text, context) => {
    if (action === 'IMPROVE_SUMMARY') {
      setResumeData(prev => ({ ...prev, summary: text }));
      setSuccessMessage('Professional summary updated with AI enhancement.');
    } else if (action === 'IMPROVE_BULLET' && context) {
      if (context.expIndex != null && context.bulletIndex != null) {
        const updatedExp = [...(resumeData.experience || [])];
        const highlights = [...(updatedExp[context.expIndex].highlights || [])];
        highlights[context.bulletIndex] = text;
        updatedExp[context.expIndex] = { ...updatedExp[context.expIndex], highlights };
        setResumeData(prev => ({ ...prev, experience: updatedExp }));
        setSuccessMessage('Work experience bullet point updated with STAR optimization.');
      } else if (context.projIndex != null && context.bulletIndex != null) {
        const updatedProj = [...(resumeData.projects || [])];
        const highlights = [...(updatedProj[context.projIndex].highlights || [])];
        highlights[context.bulletIndex] = text;
        updatedProj[context.projIndex] = { ...updatedProj[context.projIndex], highlights };
        setResumeData(prev => ({ ...prev, projects: updatedProj }));
        setSuccessMessage('Project bullet point updated with STAR optimization.');
      }
    } else if (action === 'ADD_SKILL') {
      // Add skill to first category or create one
      const skills = [...(resumeData.skills || [])];
      if (skills.length > 0) {
        if (!skills[0].items.includes(text)) {
          skills[0].items.push(text);
          setResumeData(prev => ({ ...prev, skills }));
          setSuccessMessage(`Added skill "${text}" to ${skills[0].category}.`);
        }
      } else {
        setResumeData(prev => ({
          ...prev,
          skills: [{ category: 'Technical Skills', items: [text] }]
        }));
      }
    }
  };

  // Import from uploaded resume
  const handleImportResume = (importedData, sourceFileName) => {
    setResumeData(prev => ({
      ...prev,
      ...importedData,
      // Retain existing items if imported section is empty
      experience: importedData.experience || prev.experience,
      education: importedData.education || prev.education,
      skills: importedData.skills || prev.skills,
      projects: importedData.projects || prev.projects,
      certifications: importedData.certifications || prev.certifications
    }));
    setSuccessMessage(`Successfully imported structured data from "${sourceFileName}". You can now review and edit.`);
  };

  // Import profile
  const handleImportProfile = (profileData) => {
    setResumeData(prev => ({
      ...prev,
      fullName: profileData.fullName || prev.fullName,
      email: profileData.email || prev.email,
      phone: profileData.phone || prev.phone,
      headline: profileData.headline || prev.headline,
      summary: profileData.summary || prev.summary,
      linkedinUrl: profileData.linkedinUrl || prev.linkedinUrl,
      githubUrl: profileData.githubUrl || prev.githubUrl
    }));
    setSuccessMessage('Imported profile information into contact details and summary.');
  };

  // Reset to default sample template
  const handleResetSample = () => {
    if (window.confirm('Reset current resume to the default engineering sample template? Any unsaved edits will be lost.')) {
      setResumeData(INITIAL_RESUME_DATA);
      setSuccessMessage('Reset to default ATS sample resume.');
    }
  };

  // Clear all fields
  const handleClearAll = () => {
    if (window.confirm('Clear all fields to start with a blank resume?')) {
      setResumeData({
        fullName: '',
        headline: '',
        email: '',
        phone: '',
        location: '',
        linkedinUrl: '',
        githubUrl: '',
        portfolioUrl: '',
        summary: '',
        skills: [{ category: 'Languages', items: [] }, { category: 'Frameworks', items: [] }],
        experience: [],
        education: [],
        projects: [],
        certifications: [],
        achievements: []
      });
      setSuccessMessage('Editor cleared for fresh manual entry.');
    }
  };

  // Export handlers
  const handleDownloadPdf = () => {
    window.print();
  };

  const handleDownloadWord = () => {
    const activeMeta = RESUME_TEMPLATES_METADATA.find(t => t.id === selectedTemplate);
    const cleanName = (resumeData.fullName || 'Candidate').replace(/\s+/g, '_');
    const fileName = `${cleanName}_Resume.doc`;
    downloadWordResume(resumeData, activeMeta?.name || 'Classic Professional', fileName);
    setSuccessMessage(`Generated Word document "${fileName}" suitable for Microsoft Word & Google Docs.`);
  };

  const handleDownloadTxt = () => {
    const cleanName = (resumeData.fullName || 'Candidate').replace(/\s+/g, '_');
    const fileName = `${cleanName}_Resume.txt`;
    downloadPlainTextResume(resumeData, fileName);
    setSuccessMessage(`Exported plain-text resume "${fileName}" for online ATS paste-ins.`);
  };

  // Save to PrepGPT Resumes
  const handleSaveToPrepGptResumes = async () => {
    setSavingToResumes(true);
    setSuccessMessage('');
    setErrorMessage('');

    try {
      const cleanName = (resumeData.fullName || 'Candidate').replace(/\s+/g, '_');
      const plainText = generatePlainTextResume(resumeData);

      const payload = {
        fileName: `${cleanName}_ATS_Resume.txt`,
        templateId: selectedTemplate,
        plainTextContent: plainText,
        skillsJson: JSON.stringify(resumeData.skills || []),
        experienceJson: JSON.stringify(resumeData.experience || []),
        educationJson: JSON.stringify(resumeData.education || []),
        certificationsJson: JSON.stringify(resumeData.certifications || [])
      };

      const savedResume = await saveResumeToResumes(payload);
      setSuccessMessage(`Resume "${savedResume.fileName}" saved to your PrepGPT account! You can now launch mock interviews with it.`);
    } catch (err) {
      console.error('Failed to save resume to database:', err);
      setErrorMessage(err.response?.data?.message || 'Failed to save resume to PrepGPT.');
    } finally {
      setSavingToResumes(false);
    }
  };

  const currentTemplateMeta = RESUME_TEMPLATES_METADATA.find(t => t.id === selectedTemplate) || RESUME_TEMPLATES_METADATA[0];

  return (
    <MainLayout>
      <div className="resume-builder-container">
        {/* Header */}
        <div className="builder-header no-print">
          <div>
            <h1 className="builder-title">ATS Resume Template Builder</h1>
            <p className="builder-subtitle">
              Construct clean, machine-readable resumes tailored for Applicant Tracking Systems. Preview in real-time, optimize with Groq AI, and download as vector PDF, Word, or plain text.
            </p>
          </div>

          <div style={{ display: 'flex', gap: '0.625rem', alignItems: 'center' }}>
            <button
              type="button"
              className="btn btn-secondary btn-sm"
              onClick={() => navigate('/resume-projects')}
            >
              ← Back to Resumes & Projects
            </button>
          </div>
        </div>

        {/* Global Notifications */}
        {successMessage && (
          <div className="alert alert-success no-print" style={{ marginBottom: '1.25rem', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <div>{successMessage}</div>
            <button
              type="button"
              onClick={() => setSuccessMessage('')}
              style={{ background: 'none', border: 'none', color: 'inherit', cursor: 'pointer', fontSize: '0.9rem' }}
            >
              ✕
            </button>
          </div>
        )}

        {errorMessage && (
          <div className="alert alert-danger no-print" style={{ marginBottom: '1.25rem', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <div>{errorMessage}</div>
            <button
              type="button"
              onClick={() => setErrorMessage('')}
              style={{ background: 'none', border: 'none', color: 'inherit', cursor: 'pointer', fontSize: '0.9rem' }}
            >
              ✕
            </button>
          </div>
        )}

        {/* ATS Informational Compliance Notice */}
        <div className="ats-notice-banner no-print">
          <span className="ats-notice-badge">ATS Standard</span>
          <div>
            <strong>ATS Compatibility Notice:</strong> All five templates enforce single-column reading flow, semantic heading tags, standard date notation, and selectable text with zero parser-breaking tables or multi-column grids. Parsing accuracy ultimately depends on employer software, vendor heuristics, and job-specific keyword scoring.
          </div>
        </div>

        {/* Segmented Navigation: Template Library vs Interactive Builder */}
        <div className="no-print" style={{ marginBottom: '1.5rem' }}>
          <div className="segmented-control" style={{ maxWidth: '420px' }}>
            <button
              type="button"
              className={`segmented-btn ${activeTab === 'gallery' ? 'active' : ''}`}
              onClick={() => setActiveTab('gallery')}
            >
              1. Choose Template ({RESUME_TEMPLATES_METADATA.length})
            </button>
            <button
              type="button"
              className={`segmented-btn ${activeTab === 'builder' ? 'active' : ''}`}
              onClick={() => setActiveTab('builder')}
            >
              2. Resume Editor & Live Preview
            </button>
          </div>
        </div>

        {/* TAB 1: TEMPLATE GALLERY */}
        {activeTab === 'gallery' && (
          <div className="no-print">
            <div style={{ marginBottom: '1.25rem' }}>
              <h2 style={{ fontSize: '1.2rem', fontWeight: 600, margin: 0, color: 'var(--text-primary)' }}>
                Curated ATS Resume Templates
              </h2>
              <p style={{ color: 'var(--text-muted)', fontSize: '0.85rem', margin: '0.2rem 0 0 0' }}>
                Select an ATS-validated architecture tailored to your career stage and engineering domain.
              </p>
            </div>

            <TemplateSelector
              selectedTemplate={selectedTemplate}
              onSelectTemplate={handleSelectTemplate}
            />

            <div style={{
              background: 'var(--surface-card)',
              border: '1px solid var(--border)',
              borderRadius: 'var(--radius-md)',
              padding: '1.25rem 1.5rem',
              display: 'flex',
              justifyContent: 'space-between',
              alignItems: 'center',
              flexWrap: 'wrap',
              gap: '1rem'
            }}>
              <div>
                <span style={{ fontSize: '0.875rem', fontWeight: 600, color: 'var(--text-primary)' }}>
                  Selected Template: <span style={{ color: 'var(--text-accent)' }}>{currentTemplateMeta.name}</span>
                </span>
                <span style={{ fontSize: '0.8125rem', color: 'var(--text-muted)', marginLeft: '0.5rem' }}>
                  ({currentTemplateMeta.subtitle})
                </span>
              </div>
              <button
                type="button"
                className="btn btn-primary"
                onClick={() => setActiveTab('builder')}
              >
                Proceed to Editor & Live Preview →
              </button>
            </div>
          </div>
        )}

        {/* TAB 2: INTERACTIVE BUILDER (EDITOR + LIVE PREVIEW) */}
        {activeTab === 'builder' && (
          <div className="builder-workspace">
            {/* LEFT COLUMN: RESUME EDITOR */}
            <div className="no-print">
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1rem' }}>
                <h2 style={{ fontSize: '1.15rem', fontWeight: 600, margin: 0, color: 'var(--text-primary)' }}>
                  Resume Information
                </h2>
                <div style={{ display: 'flex', gap: '0.5rem' }}>
                  <button
                    type="button"
                    className="btn btn-secondary btn-sm"
                    onClick={handleResetSample}
                    title="Reset to default engineering sample"
                  >
                    Load Sample
                  </button>
                  <button
                    type="button"
                    className="btn btn-secondary btn-sm"
                    style={{ color: 'var(--danger)' }}
                    onClick={handleClearAll}
                    title="Clear all fields"
                  >
                    Clear All
                  </button>
                </div>
              </div>

              <ResumeEditor
                resumeData={resumeData}
                onChange={setResumeData}
                onOpenAiModal={handleOpenAiModal}
                onOpenImportModal={() => setShowImportModal(true)}
              />
            </div>

            {/* RIGHT COLUMN: LIVE RESUME PREVIEW & TOOLBAR */}
            <div className="preview-panel">
              {/* Preview Toolbar */}
              <div className="preview-toolbar no-print">
                <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', flexWrap: 'wrap' }}>
                  <label style={{ fontSize: '0.75rem', fontWeight: 600, color: 'var(--text-muted)', textTransform: 'uppercase' }}>
                    Template:
                  </label>
                  <select
                    className="form-select"
                    style={{ width: 'auto', padding: '0.35rem 0.75rem', fontSize: '0.8125rem' }}
                    value={selectedTemplate}
                    onChange={(e) => setSelectedTemplate(e.target.value)}
                  >
                    {RESUME_TEMPLATES_METADATA.map((tmpl) => (
                      <option key={tmpl.id} value={tmpl.id}>
                        {tmpl.name} (ATS {tmpl.atsScore}%)
                      </option>
                    ))}
                  </select>
                </div>

                {/* Export Buttons */}
                <div style={{ display: 'flex', gap: '0.4rem', flexWrap: 'wrap', alignItems: 'center' }}>
                  <button
                    type="button"
                    className="btn btn-primary btn-sm"
                    onClick={handleDownloadPdf}
                    title="Print or Save as Vector PDF with Selectable Text"
                  >
                    📄 Download PDF
                  </button>
                  <button
                    type="button"
                    className="btn btn-secondary btn-sm"
                    onClick={handleDownloadWord}
                    title="Download Microsoft Word & Google Docs compatible file"
                  >
                    📝 Word (.doc)
                  </button>
                  <button
                    type="button"
                    className="btn btn-secondary btn-sm"
                    onClick={handleDownloadTxt}
                    title="Download clean plain text for ATS application forms"
                  >
                    Plain Text (.txt)
                  </button>
                </div>
              </div>

              {/* PrepGPT Integration Action Bar */}
              <div className="no-print" style={{
                background: 'rgba(99, 102, 241, 0.08)',
                border: '1px solid rgba(99, 102, 241, 0.25)',
                borderRadius: 'var(--radius-sm)',
                padding: '0.65rem 1rem',
                marginBottom: '1rem',
                display: 'flex',
                justifyContent: 'space-between',
                alignItems: 'center',
                flexWrap: 'wrap',
                gap: '0.5rem'
              }}>
                <div style={{ fontSize: '0.8125rem', color: 'var(--text-primary)' }}>
                  <span>🎯</span> Ready to test your newly built resume against realistic interview questions?
                </div>
                <button
                  type="button"
                  className="btn btn-primary btn-sm"
                  onClick={handleSaveToPrepGptResumes}
                  disabled={savingToResumes}
                >
                  {savingToResumes ? 'Saving...' : 'Save to PrepGPT Resumes'}
                </button>
              </div>

              {/* Physical Paper Simulation */}
              <div className="resume-paper-wrapper">
                <div id="resume-print-area" className="resume-paper">
                  <ResumePreviewRenderer
                    templateId={selectedTemplate}
                    resumeData={resumeData}
                  />
                </div>
              </div>
            </div>
          </div>
        )}

        {/* AI Assist Copilot Modal */}
        <AiAssistModal
          isOpen={showAiModal}
          onClose={() => setShowAiModal(false)}
          initialAction={aiModalAction}
          resumeData={resumeData}
          targetContext={aiModalContext}
          onApply={handleApplyAiResult}
        />

        {/* Import Candidate Data Modal */}
        <ResumeImportModal
          isOpen={showImportModal}
          onClose={() => setShowImportModal(false)}
          initialData={initialData}
          onImportResume={handleImportResume}
          onImportProfile={handleImportProfile}
        />
      </div>
    </MainLayout>
  );
};

export default ResumeTemplatePage;
