import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import MainLayout from '../layouts/MainLayout';
import {
  DocumentIcon,
  BriefcaseIcon,
  SparklesIcon,
  CheckIcon,
  LayersIcon,
  CodeIcon,
  ArrowRightIcon,
  PlayIcon
} from '../components/common/Icons';
import {
  uploadResume,
  getUserResumes,
  analyzeResume,
  setPrimaryResume,
  deleteResume
} from '../services/resumeService';
import {
  createProject,
  getUserProjects,
  updateProject,
  analyzeProject,
  deleteProject
} from '../services/projectService';

const ResumeProjectPage = () => {
  const navigate = useNavigate();
  const [activeTab, setActiveTab] = useState('resumes');

  // Resume states
  const [resumes, setResumes] = useState([]);
  const [selectedFile, setSelectedFile] = useState(null);
  const [uploading, setUploading] = useState(false);

  // Project states
  const [projects, setProjects] = useState([]);
  const [showProjectModal, setShowProjectModal] = useState(false);
  const [editingProject, setEditingProject] = useState(null);
  const [projectForm, setProjectForm] = useState({
    title: '',
    description: '',
    technologies: '',
    roleContribution: '',
    keyFeatures: '',
    architectureDetails: '',
    difficulty: 'MEDIUM'
  });

  // UI state
  const [loading, setLoading] = useState(true);
  const [analyzingId, setAnalyzingId] = useState(null);
  const [successMessage, setSuccessMessage] = useState('');
  const [errorMessage, setErrorMessage] = useState('');
  const [expandedResumeId, setExpandedResumeId] = useState(null);
  const [expandedProjectId, setExpandedProjectId] = useState(null);

  useEffect(() => {
    fetchData();
  }, []);

  const fetchData = async () => {
    setLoading(true);
    try {
      const [resumesData, projectsData] = await Promise.all([
        getUserResumes(),
        getUserProjects()
      ]);
      setResumes(resumesData || []);
      setProjects(projectsData || []);
    } catch (err) {
      console.error('Failed to load candidate assets:', err);
      setErrorMessage('Unable to load resumes and projects. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  // Resume handlers
  const handleFileChange = (e) => {
    if (e.target.files && e.target.files[0]) {
      const file = e.target.files[0];
      if (file.size > 5 * 1024 * 1024) {
        setErrorMessage('File size exceeds the 5MB limit.');
        return;
      }
      setSelectedFile(file);
      setErrorMessage('');
    }
  };

  const handleUploadResume = async (e) => {
    e.preventDefault();
    if (!selectedFile) return;

    setUploading(true);
    setSuccessMessage('');
    setErrorMessage('');

    try {
      const newResume = await uploadResume(selectedFile);
      setSuccessMessage(`Resume "${newResume.fileName}" uploaded and analyzed.`);
      setSelectedFile(null);
      fetchData();
    } catch (err) {
      console.error('Upload failed:', err);
      setErrorMessage(err.response?.data?.message || 'Failed to upload and parse resume.');
    } finally {
      setUploading(false);
    }
  };

  const handleAnalyzeResume = async (id) => {
    setAnalyzingId(id);
    setSuccessMessage('');
    setErrorMessage('');
    try {
      await analyzeResume(id);
      setSuccessMessage('Resume technical analysis updated.');
      fetchData();
    } catch (err) {
      setErrorMessage('Failed to perform resume analysis.');
    } finally {
      setAnalyzingId(null);
    }
  };

  const handleSetPrimary = async (id) => {
    try {
      await setPrimaryResume(id);
      setSuccessMessage('Primary resume updated.');
      fetchData();
    } catch (err) {
      setErrorMessage('Failed to set primary resume.');
    }
  };

  const handleDeleteResume = async (id) => {
    if (!window.confirm('Delete this resume document?')) return;
    try {
      await deleteResume(id);
      setSuccessMessage('Resume removed.');
      fetchData();
    } catch (err) {
      setErrorMessage('Failed to delete resume.');
    }
  };

  // Project handlers
  const handleProjectSubmit = async (e) => {
    e.preventDefault();
    setSuccessMessage('');
    setErrorMessage('');

    try {
      if (editingProject) {
        await updateProject(editingProject.id, projectForm);
        setSuccessMessage('Project updated.');
      } else {
        await createProject(projectForm);
        setSuccessMessage('Project created.');
      }
      setShowProjectModal(false);
      resetProjectForm();
      fetchData();
    } catch (err) {
      setErrorMessage(err.response?.data?.message || 'Failed to save project.');
    }
  };

  const handleAnalyzeProject = async (id) => {
    setAnalyzingId(id);
    setSuccessMessage('');
    setErrorMessage('');
    try {
      await analyzeProject(id);
      setSuccessMessage('Project architecture analyzed.');
      setExpandedProjectId(id);
      fetchData();
    } catch (err) {
      setErrorMessage('Failed to analyze project architecture.');
    } finally {
      setAnalyzingId(null);
    }
  };

  const handleDeleteProject = async (id) => {
    if (!window.confirm('Delete this project?')) return;
    try {
      await deleteProject(id);
      setSuccessMessage('Project removed.');
      fetchData();
    } catch (err) {
      setErrorMessage('Failed to delete project.');
    }
  };

  const openEditProject = (proj) => {
    setEditingProject(proj);
    setProjectForm({
      title: proj.title || '',
      description: proj.description || '',
      technologies: proj.technologies || '',
      roleContribution: proj.roleContribution || '',
      keyFeatures: proj.keyFeatures || '',
      architectureDetails: proj.architectureDetails || '',
      difficulty: proj.difficulty || 'MEDIUM'
    });
    setShowProjectModal(true);
  };

  const resetProjectForm = () => {
    setEditingProject(null);
    setProjectForm({
      title: '',
      description: '',
      technologies: '',
      roleContribution: '',
      keyFeatures: '',
      architectureDetails: '',
      difficulty: 'MEDIUM'
    });
  };

  const parseJsonSafe = (jsonStr, defaultVal) => {
    if (!jsonStr) return defaultVal;
    try {
      return JSON.parse(jsonStr);
    } catch (e) {
      return defaultVal;
    }
  };

  return (
    <MainLayout>
      <div style={{ maxWidth: '1060px', margin: '0 auto', paddingBottom: '3rem' }}>
        {/* Header */}
        <div style={{ marginBottom: '2rem', display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', flexWrap: 'wrap', gap: '1.25rem' }}>
          <div>
            <div style={{ display: 'inline-flex', alignItems: 'center', gap: '0.5rem', marginBottom: '0.35rem' }}>
              <span className="studio-pill">
                <span className="studio-dot" /> CANDIDATE ASSETS
              </span>
            </div>
            <h1 style={{ fontSize: '1.875rem', fontWeight: 700, letterSpacing: '-0.025em', margin: 0, color: 'var(--text-primary)' }}>
              Credentials & Portfolio
            </h1>
            <p style={{ color: 'var(--text-muted)', fontSize: '0.9rem', margin: '0.35rem 0 0 0' }}>
              Manage your engineering resume documents and production projects to generate deeply contextual technical interviews.
            </p>
          </div>
          <button
            type="button"
            className="btn btn-primary"
            onClick={() => navigate('/resume-templates')}
            style={{ display: 'inline-flex', alignItems: 'center', gap: '0.5rem' }}
          >
            <DocumentIcon size={16} />
            <span>Build ATS Resume</span>
            <ArrowRightIcon size={14} />
          </button>
        </div>

        {/* Global Notifications */}
        {successMessage && (
          <div className="alert alert-success" style={{ marginBottom: '1.25rem' }}>
            <CheckIcon size={16} style={{ flexShrink: 0 }} />
            <span>{successMessage}</span>
          </div>
        )}

        {errorMessage && (
          <div className="alert alert-danger" style={{ marginBottom: '1.25rem' }}>
            {errorMessage}
          </div>
        )}

        {/* Segmented Tab Controls */}
        <div style={{ marginBottom: '1.75rem' }}>
          <div className="segmented-control" style={{ maxWidth: '400px' }}>
            <button
              type="button"
              className={`segmented-btn ${activeTab === 'resumes' ? 'active' : ''}`}
              onClick={() => setActiveTab('resumes')}
              style={{ display: 'inline-flex', alignItems: 'center', justifyContent: 'center', gap: '0.5rem' }}
            >
              <DocumentIcon size={15} />
              <span>Resume Documents ({resumes.length})</span>
            </button>
            <button
              type="button"
              className={`segmented-btn ${activeTab === 'projects' ? 'active' : ''}`}
              onClick={() => setActiveTab('projects')}
              style={{ display: 'inline-flex', alignItems: 'center', justifyContent: 'center', gap: '0.5rem' }}
            >
              <LayersIcon size={15} />
              <span>Projects ({projects.length})</span>
            </button>
          </div>
        </div>

        {/* RESUMES TAB */}
        {activeTab === 'resumes' && (
          <div>
            {/* Resume Upload Box */}
            <div className="card" style={{ marginBottom: '1.75rem', padding: '1.5rem', border: '1px solid var(--border)' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '0.5rem' }}>
                <DocumentIcon size={18} style={{ color: 'var(--primary)' }} />
                <h2 style={{ fontSize: '1.0625rem', fontWeight: 600, margin: 0, color: 'var(--text-primary)' }}>
                  Upload Engineering Resume
                </h2>
              </div>
              <p style={{ fontSize: '0.8125rem', color: 'var(--text-muted)', margin: '0 0 1rem 0' }}>
                Our parser automatically extracts technical skills, years of experience, and previous project domains.
              </p>

              <form onSubmit={handleUploadResume} style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
                <div style={{
                  border: '1px dashed var(--border-strong)',
                  borderRadius: 'var(--radius-md)',
                  padding: '1.75rem 1.5rem',
                  textAlign: 'center',
                  background: 'var(--surface-raised)',
                  transition: 'border-color 0.15s ease'
                }}>
                  <input
                    type="file"
                    accept=".pdf,.docx,.txt"
                    onChange={handleFileChange}
                    id="resumeFileInput"
                    style={{ display: 'none' }}
                  />
                  <label htmlFor="resumeFileInput" className="btn btn-secondary btn-sm" style={{ cursor: 'pointer', display: 'inline-flex', alignItems: 'center', gap: '0.5rem' }}>
                    <DocumentIcon size={14} />
                    <span>Choose Resume File (.pdf, .docx, .txt)</span>
                  </label>
                  {selectedFile ? (
                    <div style={{ marginTop: '0.875rem', fontSize: '0.875rem', fontWeight: 600, color: 'var(--primary)' }}>
                      Selected: {selectedFile.name} · {(selectedFile.size / 1024).toFixed(1)} KB
                    </div>
                  ) : (
                    <div style={{ marginTop: '0.625rem', fontSize: '0.8125rem', color: 'var(--text-muted)' }}>
                      PDF, DOCX, or plain text up to 5MB. No personal data shared externally.
                    </div>
                  )}
                </div>

                {selectedFile && (
                  <div style={{ display: 'flex', justifyContent: 'flex-end' }}>
                    <button
                      type="submit"
                      className="btn btn-primary"
                      disabled={uploading}
                      style={{ display: 'inline-flex', alignItems: 'center', gap: '0.5rem' }}
                    >
                      <SparklesIcon size={15} />
                      <span>{uploading ? 'Parsing Technical Profile...' : 'Upload & Parse Resume'}</span>
                    </button>
                  </div>
                )}
              </form>
            </div>

            {/* Resume List */}
            {loading ? (
              <div className="card" style={{ padding: '3rem', textAlign: 'center' }}>
                <div className="skeleton" style={{ width: '40%', height: '20px', margin: '0 auto 1rem auto' }} />
                <div className="skeleton" style={{ width: '70%', height: '14px', margin: '0 auto' }} />
              </div>
            ) : resumes.length === 0 ? (
              <div className="empty-state" style={{ padding: '3rem 1.5rem', border: '1px dashed var(--border)' }}>
                <DocumentIcon size={36} style={{ color: 'var(--text-muted)', marginBottom: '0.75rem' }} />
                <h3 style={{ fontSize: '1.125rem', fontWeight: 600, margin: '0 0 0.5rem 0' }}>No resume documents uploaded</h3>
                <p style={{ color: 'var(--text-muted)', fontSize: '0.875rem', maxWidth: '420px', margin: '0 auto 1.25rem auto' }}>
                  Upload your CV or technical resume above to enable AI-powered resume deep-dive mock interviews.
                </p>
              </div>
            ) : (
              <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
                {resumes.map((resume) => {
                  const skills = parseJsonSafe(resume.skillsJson, []);
                  const experience = parseJsonSafe(resume.experienceJson, []);
                  const education = parseJsonSafe(resume.educationJson, []);
                  const isExpanded = expandedResumeId === resume.id;

                  return (
                    <div key={resume.id} className="card" style={{ padding: '1.5rem', border: '1px solid var(--border)' }}>
                      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', flexWrap: 'wrap', gap: '1rem' }}>
                        <div>
                          <div style={{ display: 'flex', alignItems: 'center', gap: '0.625rem', flexWrap: 'wrap' }}>
                            <DocumentIcon size={18} style={{ color: resume.isPrimary ? 'var(--primary)' : 'var(--text-muted)' }} />
                            <h3 style={{ fontSize: '1.0625rem', fontWeight: 600, margin: 0, color: 'var(--text-primary)' }}>
                              {resume.fileName}
                            </h3>
                            {resume.isPrimary && (
                              <span className="badge badge-success" style={{ display: 'inline-flex', alignItems: 'center', gap: '0.3rem' }}>
                                <CheckIcon size={12} /> Primary
                              </span>
                            )}
                            <span className="badge badge-secondary" style={{ textTransform: 'uppercase', fontSize: '0.7rem' }}>
                              {resume.fileType}
                            </span>
                          </div>
                          <div style={{ fontSize: '0.8125rem', color: 'var(--text-muted)', marginTop: '0.35rem' }}>
                            Added {new Date(resume.createdAt).toLocaleDateString()} · {(resume.fileSize / 1024).toFixed(1)} KB
                          </div>
                        </div>

                        {/* Actions */}
                        <div style={{ display: 'flex', gap: '0.5rem', flexWrap: 'wrap' }}>
                          <button
                            type="button"
                            className="btn btn-primary btn-sm"
                            onClick={() => navigate(`/interview/setup?mode=RESUME&resumeId=${resume.id}`)}
                            style={{ display: 'inline-flex', alignItems: 'center', gap: '0.4rem' }}
                          >
                            <PlayIcon size={13} />
                            <span>Practice</span>
                          </button>
                          {!resume.isPrimary && (
                            <button
                              type="button"
                              className="btn btn-secondary btn-sm"
                              onClick={() => handleSetPrimary(resume.id)}
                            >
                              Set Primary
                            </button>
                          )}
                          <button
                            type="button"
                            className="btn btn-secondary btn-sm"
                            onClick={() => handleAnalyzeResume(resume.id)}
                            disabled={analyzingId === resume.id}
                          >
                            {analyzingId === resume.id ? 'Analyzing...' : 'Re-analyze'}
                          </button>
                          <button
                            type="button"
                            className="btn btn-secondary btn-sm"
                            style={{ color: 'var(--danger)' }}
                            onClick={() => handleDeleteResume(resume.id)}
                          >
                            Delete
                          </button>
                        </div>
                      </div>

                      {/* Skills Tags */}
                      {skills.length > 0 && (
                        <div style={{ marginTop: '1rem' }}>
                          <div style={{ fontSize: '0.75rem', fontWeight: 600, color: 'var(--text-muted)', textTransform: 'uppercase', letterSpacing: '0.04em', marginBottom: '0.35rem' }}>
                            Extracted Skills
                          </div>
                          <div style={{ display: 'flex', flexWrap: 'wrap', gap: '0.35rem' }}>
                            {skills.map((skill, idx) => (
                              <span key={idx} className="badge badge-secondary" style={{ fontSize: '0.75rem' }}>
                                {skill}
                              </span>
                            ))}
                          </div>
                        </div>
                      )}

                      {/* Expandable Details */}
                      <div style={{ marginTop: '1rem', paddingTop: '0.75rem', borderTop: '1px solid var(--border)' }}>
                        <button
                          type="button"
                          onClick={() => setExpandedResumeId(isExpanded ? null : resume.id)}
                          style={{
                            background: 'none',
                            border: 'none',
                            color: 'var(--primary)',
                            cursor: 'pointer',
                            fontWeight: 500,
                            fontSize: '0.8125rem',
                            padding: 0
                          }}
                        >
                          {isExpanded ? 'Hide extracted details' : 'View extracted details & raw text'}
                        </button>

                        {isExpanded && (
                          <div style={{ marginTop: '0.875rem', display: 'flex', flexDirection: 'column', gap: '0.75rem' }}>
                            {experience.length > 0 && (
                              <div style={{ background: 'var(--bg-secondary)', padding: '0.75rem 1rem', borderRadius: 'var(--radius-sm)' }}>
                                <div style={{ fontSize: '0.8125rem', fontWeight: 600, color: 'var(--text-primary)', marginBottom: '0.5rem' }}>
                                  Work Experience
                                </div>
                                {experience.map((exp, i) => (
                                  <div key={i} style={{ marginBottom: '0.5rem', fontSize: '0.8125rem' }}>
                                    <div style={{ fontWeight: 500, color: 'var(--text-primary)' }}>
                                      {exp.role} · <span style={{ color: 'var(--text-muted)' }}>{exp.company}</span>
                                    </div>
                                    <div style={{ color: 'var(--text-muted)', fontSize: '0.75rem' }}>{exp.duration}</div>
                                  </div>
                                ))}
                              </div>
                            )}

                            {education.length > 0 && (
                              <div style={{ background: 'var(--bg-secondary)', padding: '0.75rem 1rem', borderRadius: 'var(--radius-sm)' }}>
                                <div style={{ fontSize: '0.8125rem', fontWeight: 600, color: 'var(--text-primary)', marginBottom: '0.5rem' }}>
                                  Education
                                </div>
                                {education.map((edu, i) => (
                                  <div key={i} style={{ fontSize: '0.8125rem' }}>
                                    <div style={{ fontWeight: 500 }}>{edu.degree}</div>
                                    <div style={{ color: 'var(--text-muted)', fontSize: '0.75rem' }}>{edu.institution} {edu.graduationYear ? `(${edu.graduationYear})` : ''}</div>
                                  </div>
                                ))}
                              </div>
                            )}

                            <div>
                              <div style={{ fontSize: '0.75rem', fontWeight: 600, color: 'var(--text-muted)', marginBottom: '0.35rem', textTransform: 'uppercase' }}>
                                Raw Extracted Text
                              </div>
                              <pre style={{
                                background: 'var(--bg-secondary)',
                                padding: '0.75rem',
                                borderRadius: 'var(--radius-sm)',
                                fontSize: '0.75rem',
                                color: 'var(--text-secondary)',
                                whiteSpace: 'pre-wrap',
                                maxHeight: '180px',
                                overflowY: 'auto',
                                border: '1px solid var(--border)',
                                fontFamily: 'var(--font-mono, monospace)'
                              }}>
                                {resume.extractedText}
                              </pre>
                            </div>
                          </div>
                        )}
                      </div>
                    </div>
                  );
                })}
              </div>
            )}
          </div>
        )}

        {/* PROJECTS TAB */}
        {activeTab === 'projects' && (
          <div>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.5rem', flexWrap: 'wrap', gap: '1rem' }}>
              <div>
                <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '0.25rem' }}>
                  <LayersIcon size={18} style={{ color: 'var(--primary)' }} />
                  <h2 style={{ fontSize: '1.25rem', fontWeight: 600, margin: 0, color: 'var(--text-primary)' }}>
                    Engineering Portfolio Projects
                  </h2>
                </div>
                <p style={{ color: 'var(--text-muted)', fontSize: '0.85rem', margin: 0 }}>
                  Catalog production services, fullstack applications, and distributed systems to simulate technical deep dives.
                </p>
              </div>
              <button
                type="button"
                className="btn btn-primary"
                onClick={() => { resetProjectForm(); setShowProjectModal(true); }}
                style={{ display: 'inline-flex', alignItems: 'center', gap: '0.5rem' }}
              >
                <CodeIcon size={15} />
                <span>Add Software Project</span>
              </button>
            </div>

            {/* Modal for Create/Edit Project */}
            {showProjectModal && (
              <div style={{
                position: 'fixed',
                top: 0, left: 0, right: 0, bottom: 0,
                background: 'rgba(5, 8, 15, 0.75)',
                backdropFilter: 'blur(4px)',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                zIndex: 1000,
                padding: '1rem'
              }}>
                <div className="card studio-terminal" style={{ maxWidth: '640px', width: '100%', maxHeight: '90vh', overflowY: 'auto', padding: '1.75rem', border: '1px solid var(--border-editor)' }}>
                  <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '1.25rem', paddingBottom: '0.75rem', borderBottom: '1px solid var(--border)' }}>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                      <CodeIcon size={18} style={{ color: 'var(--primary)' }} />
                      <h3 style={{ margin: 0, fontSize: '1.15rem', fontWeight: 600, color: 'var(--text-primary)' }}>
                        {editingProject ? 'Edit Technical Project' : 'Register New Software Project'}
                      </h3>
                    </div>
                    <button
                      type="button"
                      onClick={() => setShowProjectModal(false)}
                      style={{ background: 'none', border: 'none', color: 'var(--text-muted)', cursor: 'pointer', fontSize: '1.25rem' }}
                    >
                      ✕
                    </button>
                  </div>

                  <form onSubmit={handleProjectSubmit}>
                    <div className="form-group">
                      <label className="form-label">Project Title *</label>
                      <input
                        type="text"
                        required
                        className="form-input"
                        placeholder="e.g. Distributed Event Streamer & Cache"
                        value={projectForm.title}
                        onChange={(e) => setProjectForm({ ...projectForm, title: e.target.value })}
                      />
                    </div>

                    <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))', gap: '1rem' }}>
                      <div className="form-group">
                        <label className="form-label">Technologies Used *</label>
                        <input
                          type="text"
                          required
                          className="form-input"
                          placeholder="e.g. Java, Redis, Kafka, Docker, Spring Boot"
                          value={projectForm.technologies}
                          onChange={(e) => setProjectForm({ ...projectForm, technologies: e.target.value })}
                        />
                      </div>

                      <div className="form-group">
                        <label className="form-label">Architectural Complexity</label>
                        <select
                          className="form-select"
                          value={projectForm.difficulty}
                          onChange={(e) => setProjectForm({ ...projectForm, difficulty: e.target.value })}
                        >
                          <option value="EASY">Academic / Prototype</option>
                          <option value="MEDIUM">Production Web Application</option>
                          <option value="HARD">Distributed / High Scale Infrastructure</option>
                        </select>
                      </div>
                    </div>

                    <div className="form-group">
                      <label className="form-label">Your Role & Specific Technical Contribution</label>
                      <textarea
                        className="form-textarea"
                        rows="2"
                        placeholder="What specific components, concurrency patterns, or algorithms did you architect?"
                        value={projectForm.roleContribution}
                        onChange={(e) => setProjectForm({ ...projectForm, roleContribution: e.target.value })}
                      />
                    </div>

                    <div className="form-group">
                      <label className="form-label">System Overview & Core Problem Solved</label>
                      <textarea
                        className="form-textarea"
                        rows="3"
                        placeholder="High-level architecture, throughput requirements, trade-offs made..."
                        value={projectForm.description}
                        onChange={(e) => setProjectForm({ ...projectForm, description: e.target.value })}
                      />
                    </div>

                    <div className="form-group">
                      <label className="form-label">System Architecture Details (Optional)</label>
                      <textarea
                        className="form-textarea"
                        rows="2"
                        placeholder="Database schema, isolation levels, caching strategy, messaging pipelines..."
                        value={projectForm.architectureDetails}
                        onChange={(e) => setProjectForm({ ...projectForm, architectureDetails: e.target.value })}
                      />
                    </div>

                    <div style={{ display: 'flex', gap: '0.75rem', justifyContent: 'flex-end', marginTop: '1.5rem', paddingTop: '1rem', borderTop: '1px solid var(--border)' }}>
                      <button
                        type="button"
                        className="btn btn-secondary"
                        onClick={() => setShowProjectModal(false)}
                      >
                        Cancel
                      </button>
                      <button type="submit" className="btn btn-primary" style={{ display: 'inline-flex', alignItems: 'center', gap: '0.4rem' }}>
                        <CheckIcon size={14} />
                        <span>{editingProject ? 'Save Changes' : 'Register Project'}</span>
                      </button>
                    </div>
                  </form>
                </div>
              </div>
            )}

            {/* Project List */}
            {loading ? (
              <div className="card" style={{ padding: '3rem', textAlign: 'center' }}>
                <div className="skeleton" style={{ width: '40%', height: '20px', margin: '0 auto 1rem auto' }} />
                <div className="skeleton" style={{ width: '70%', height: '14px', margin: '0 auto' }} />
              </div>
            ) : projects.length === 0 ? (
              <div className="empty-state" style={{ padding: '3rem 1.5rem', border: '1px dashed var(--border)' }}>
                <LayersIcon size={36} style={{ color: 'var(--text-muted)', marginBottom: '0.75rem' }} />
                <h3 style={{ fontSize: '1.125rem', fontWeight: 600, margin: '0 0 0.5rem 0' }}>No portfolio projects cataloged</h3>
                <p style={{ color: 'var(--text-muted)', fontSize: '0.875rem', maxWidth: '440px', margin: '0 auto 1.25rem auto' }}>
                  Add your software engineering projects to practice system architecture and project-specific technical defenses.
                </p>
                <button
                  type="button"
                  className="btn btn-primary"
                  onClick={() => { resetProjectForm(); setShowProjectModal(true); }}
                  style={{ display: 'inline-flex', alignItems: 'center', gap: '0.5rem' }}
                >
                  <CodeIcon size={14} />
                  <span>Register First Project</span>
                </button>
              </div>
            ) : (
              <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
                {projects.map((proj) => {
                  const aiAnalysis = parseJsonSafe(proj.aiAnalysisJson, null);
                  const isExpanded = expandedProjectId === proj.id;

                  return (
                    <div key={proj.id} className="card" style={{ padding: '1.5rem', border: '1px solid var(--border)' }}>
                      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', flexWrap: 'wrap', gap: '1rem' }}>
                        <div>
                          <div style={{ display: 'flex', alignItems: 'center', gap: '0.625rem', flexWrap: 'wrap' }}>
                            <CodeIcon size={18} style={{ color: 'var(--primary)' }} />
                            <h3 style={{ fontSize: '1.1rem', fontWeight: 600, margin: 0, color: 'var(--text-primary)' }}>
                              {proj.title}
                            </h3>
                            <span className="badge badge-secondary" style={{ fontSize: '0.7rem' }}>
                              {proj.difficulty}
                            </span>
                          </div>
                          <div style={{ marginTop: '0.5rem', color: 'var(--text-secondary)', fontSize: '0.875rem', lineHeight: 1.5 }}>
                            {proj.description || 'No description provided.'}
                          </div>
                        </div>

                        {/* Actions */}
                        <div style={{ display: 'flex', gap: '0.5rem', flexWrap: 'wrap' }}>
                          <button
                            type="button"
                            className="btn btn-primary btn-sm"
                            onClick={() => navigate(`/interview/setup?mode=PROJECT&projectId=${proj.id}`)}
                            style={{ display: 'inline-flex', alignItems: 'center', gap: '0.4rem' }}
                          >
                            <PlayIcon size={13} />
                            <span>Practice</span>
                          </button>
                          <button
                            type="button"
                            className="btn btn-secondary btn-sm"
                            onClick={() => handleAnalyzeProject(proj.id)}
                            disabled={analyzingId === proj.id}
                            style={{ display: 'inline-flex', alignItems: 'center', gap: '0.4rem' }}
                          >
                            <SparklesIcon size={13} />
                            <span>{analyzingId === proj.id ? 'Analyzing...' : 'Analyze Architecture'}</span>
                          </button>
                          <button
                            type="button"
                            className="btn btn-secondary btn-sm"
                            onClick={() => openEditProject(proj)}
                          >
                            Edit
                          </button>
                          <button
                            type="button"
                            className="btn btn-secondary btn-sm"
                            style={{ color: 'var(--danger)' }}
                            onClick={() => handleDeleteProject(proj.id)}
                          >
                            Delete
                          </button>
                        </div>
                      </div>

                      {/* Tech Stack Pills */}
                      <div style={{ marginTop: '0.875rem', display: 'flex', flexWrap: 'wrap', gap: '0.35rem', alignItems: 'center' }}>
                        <span style={{ fontSize: '0.75rem', fontWeight: 600, color: 'var(--text-muted)', textTransform: 'uppercase', letterSpacing: '0.04em' }}>
                          Stack:
                        </span>
                        {proj.technologies ? (
                          proj.technologies.split(',').map((tech, i) => (
                            <span key={i} className="badge badge-secondary" style={{ fontSize: '0.75rem' }}>
                              {tech.trim()}
                            </span>
                          ))
                        ) : (
                          <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>Not specified</span>
                        )}
                      </div>

                      {/* Candidate Role */}
                      {proj.roleContribution && (
                        <div style={{ marginTop: '0.625rem', fontSize: '0.8125rem', color: 'var(--text-secondary)' }}>
                          <strong style={{ fontWeight: 600, color: 'var(--text-primary)' }}>Role:</strong> {proj.roleContribution}
                        </div>
                      )}

                      {/* AI Analysis Accordion */}
                      {aiAnalysis && (
                        <div style={{ marginTop: '1rem', paddingTop: '0.75rem', borderTop: '1px solid var(--border)' }}>
                          <button
                            type="button"
                            onClick={() => setExpandedProjectId(isExpanded ? null : proj.id)}
                            style={{
                              background: 'none',
                              border: 'none',
                              color: 'var(--primary)',
                              cursor: 'pointer',
                              fontWeight: 500,
                              fontSize: '0.8125rem',
                              padding: 0
                            }}
                          >
                            {isExpanded ? 'Hide architectural breakdown' : 'View architectural analysis & suggested questions'}
                          </button>

                          {isExpanded && (
                            <div style={{
                              marginTop: '0.75rem',
                              padding: '0.875rem',
                              background: 'var(--bg-secondary)',
                              borderRadius: 'var(--radius-sm)',
                              border: '1px solid var(--border)',
                              display: 'flex',
                              flexDirection: 'column',
                              gap: '0.75rem'
                            }}>
                              <div>
                                <div style={{ fontSize: '0.75rem', fontWeight: 600, color: 'var(--text-muted)', textTransform: 'uppercase', marginBottom: '0.25rem' }}>
                                  Technical Summary
                                </div>
                                <div style={{ fontSize: '0.875rem', lineHeight: 1.5, color: 'var(--text-primary)' }}>{aiAnalysis.technicalSummary}</div>
                              </div>

                              {aiAnalysis.architectureTopics && aiAnalysis.architectureTopics.length > 0 && (
                                <div>
                                  <div style={{ fontSize: '0.75rem', fontWeight: 600, color: 'var(--text-muted)', textTransform: 'uppercase', marginBottom: '0.35rem' }}>
                                    Core Topics
                                  </div>
                                  <div style={{ display: 'flex', flexWrap: 'wrap', gap: '0.35rem' }}>
                                    {aiAnalysis.architectureTopics.map((t, idx) => (
                                      <span key={idx} className="badge badge-info" style={{ fontSize: '0.75rem' }}>
                                        {t}
                                      </span>
                                    ))}
                                  </div>
                                </div>
                              )}

                              {aiAnalysis.suggestedInterviewQuestions && aiAnalysis.suggestedInterviewQuestions.length > 0 && (
                                <div>
                                  <div style={{ fontSize: '0.75rem', fontWeight: 600, color: 'var(--text-muted)', textTransform: 'uppercase', marginBottom: '0.35rem' }}>
                                    Suggested Practice Questions
                                  </div>
                                  <ul style={{ margin: 0, paddingLeft: '1.125rem', fontSize: '0.8125rem', display: 'flex', flexDirection: 'column', gap: '0.35rem', color: 'var(--text-primary)' }}>
                                    {aiAnalysis.suggestedInterviewQuestions.map((q, idx) => (
                                      <li key={idx}>{q}</li>
                                    ))}
                                  </ul>
                                </div>
                              )}
                            </div>
                          )}
                        </div>
                      )}
                    </div>
                  );
                })}
              </div>
            )}
          </div>
        )}
      </div>
    </MainLayout>
  );
};

export default ResumeProjectPage;
