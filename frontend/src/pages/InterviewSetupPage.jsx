import React, { useState, useEffect } from 'react';
import { useNavigate, useSearchParams, useLocation } from 'react-router-dom';
import MainLayout from '../layouts/MainLayout';
import { getCategories } from '../services/categoryService';
import { createInterviewSession, startInterviewSession } from '../services/interviewService';
import { getUserResumes } from '../services/resumeService';
import { getUserProjects } from '../services/projectService';
import { getUserJobDescriptions } from '../services/jobDescriptionService';
import { 
  TerminalIcon, 
  CpuIcon, 
  LayersIcon, 
  DocumentIcon, 
  BriefcaseIcon, 
  CodeIcon, 
  SparklesIcon, 
  PlayIcon, 
  CheckIcon, 
  MicIcon 
} from '../components/common/Icons';

const DIFFICULTY_CONFIG = {
  EASY: {
    label: 'Fundamental',
    desc: 'Core syntax, fundamental definitions, and common patterns.',
    color: '#34d399'
  },
  MEDIUM: {
    label: 'Production',
    desc: 'Architectural trade-offs, edge cases, and real-world system patterns.',
    color: '#fbbf24'
  },
  HARD: {
    label: 'Advanced',
    desc: 'High-scale concurrency, distributed failure scenarios, and performance optimization.',
    color: '#fb7185'
  }
};

const InterviewSetupPage = () => {
  const navigate = useNavigate();
  const location = useLocation();
  const [searchParams] = useSearchParams();

  const initialMode = location.state?.mode || searchParams.get('mode') || 'AI';
  const initialCategoryParam = searchParams.get('categoryId');
  const initialDifficultyParam = searchParams.get('difficulty') || 'MEDIUM';
  const initialResumeId = searchParams.get('resumeId') || '';
  const initialProjectId = searchParams.get('projectId') || '';
  const initialJdId = location.state?.selectedJdId || searchParams.get('jobDescriptionId') || '';

  const [categories, setCategories] = useState([]);
  const [resumes, setResumes] = useState([]);
  const [projects, setProjects] = useState([]);
  const [jds, setJds] = useState([]);

  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState('');

  // Configuration Form State
  const [mode, setMode] = useState(initialMode);
  const [interactionMode, setInteractionMode] = useState('TEXT');
  const [selectedCategory, setSelectedCategory] = useState(null);
  const [difficulty, setDifficulty] = useState(initialDifficultyParam);
  const [questionCount, setQuestionCount] = useState(5);
  const [durationMinutes, setDurationMinutes] = useState(30);

  const [selectedResumeId, setSelectedResumeId] = useState(initialResumeId);
  const [selectedProjectId, setSelectedProjectId] = useState(initialProjectId);
  const [selectedJdId, setSelectedJdId] = useState(initialJdId);

  useEffect(() => {
    fetchData();
  }, []);

  const fetchData = async () => {
    setLoading(true);
    try {
      const [catData, resumeData, projectData, jdData] = await Promise.all([
        getCategories(),
        getUserResumes().catch(() => []),
        getUserProjects().catch(() => []),
        getUserJobDescriptions().catch(() => [])
      ]);
      setCategories(catData || []);
      setResumes(resumeData || []);
      setProjects(projectData || []);
      setJds(jdData || []);

      if (catData && catData.length > 0) {
        if (initialCategoryParam) {
          const matched = catData.find(c => c.id.toString() === initialCategoryParam.toString());
          setSelectedCategory(matched || catData[0]);
        } else {
          setSelectedCategory(catData[0]);
        }
      }

      if (!initialResumeId && resumeData && resumeData.length > 0) {
        const primary = resumeData.find(r => r.isPrimary) || resumeData[0];
        setSelectedResumeId(primary.id.toString());
      }

      if (!initialProjectId && projectData && projectData.length > 0) {
        setSelectedProjectId(projectData[0].id.toString());
      }

      if (!initialJdId && jdData && jdData.length > 0) {
        setSelectedJdId(jdData[0].id.toString());
      }

    } catch (err) {
      console.error('Failed to load interview setup data:', err);
      setError('Unable to load interview configuration tracks.');
    } finally {
      setLoading(false);
    }
  };

  const handleStartInterview = async () => {
    if (!selectedCategory) {
      setError('Please select an interview category track.');
      return;
    }

    if (mode === 'RESUME' && !selectedResumeId) {
      setError('Please select a resume document to ground this session.');
      return;
    }

    if (mode === 'PROJECT' && !selectedProjectId) {
      setError('Please select a software project to defend.');
      return;
    }

    if (mode === 'JOB_DESCRIPTION' && !selectedJdId) {
      setError('Please select or paste a Job Description for targeted practice.');
      return;
    }

    setSubmitting(true);
    setError('');

    try {
      const createdSession = await createInterviewSession({
        categoryId: selectedCategory.id,
        difficulty,
        questionCount: parseInt(questionCount),
        durationMinutes: parseInt(durationMinutes),
        mode,
        interactionMode,
        resumeId: (mode === 'RESUME' || mode === 'JOB_DESCRIPTION') && selectedResumeId ? parseInt(selectedResumeId) : null,
        projectId: (mode === 'PROJECT' || mode === 'JOB_DESCRIPTION') && selectedProjectId ? parseInt(selectedProjectId) : null,
        jobDescriptionId: mode === 'JOB_DESCRIPTION' && selectedJdId ? parseInt(selectedJdId) : null
      });

      await startInterviewSession(createdSession.id);

      if (mode === 'CODING') {
        navigate(`/interview/coding/${createdSession.id}`);
      } else {
        navigate(`/interview/${createdSession.id}`);
      }
    } catch (err) {
      console.error('Failed to configure interview session:', err);
      setError(err.response?.data?.message || 'Failed to initialize interview session.');
    } finally {
      setSubmitting(false);
    }
  };

  const interviewModes = [
    {
      id: 'AI',
      className: 'mode-ai',
      icon: CpuIcon,
      name: 'Adaptive AI Simulation',
      tag: 'Recommended',
      description: 'Generates progressive questions that adapt dynamically based on candidate reasoning and depth.'
    },
    {
      id: 'QUESTION_BANK',
      className: 'mode-bank',
      icon: LayersIcon,
      name: 'Curated Question Bank',
      tag: 'Standard',
      description: 'Standard domain questions drawn from vetted top-tier tech interview questions.'
    },
    {
      id: 'RESUME',
      className: 'mode-resume',
      icon: DocumentIcon,
      name: 'Resume Deep Dive',
      tag: 'Personalized',
      description: 'Questions formulated directly from your uploaded skills, work history, and stated metrics.'
    },
    {
      id: 'PROJECT',
      className: 'mode-project',
      icon: BriefcaseIcon,
      name: 'Project Defense',
      tag: 'Portfolio',
      description: 'Defend software architecture, technical trade-offs, and implementation choices.'
    },
    {
      id: 'CODING',
      className: 'mode-coding',
      icon: CodeIcon,
      name: 'Coding & Algorithms',
      tag: 'IDE Sandbox',
      description: 'Interactive code editor with test cases, execution output, and algorithmic grading.'
    },
    {
      id: 'JOB_DESCRIPTION',
      className: 'mode-jd',
      icon: SparklesIcon,
      name: 'Job Description Match',
      tag: 'Targeted',
      description: 'Simulates the exact interview round mapped against a target role description.'
    }
  ];

  return (
    <MainLayout>
      <div style={{ maxWidth: '1100px', margin: '0 auto', paddingBottom: '3.5rem' }}>
        
        {/* Header */}
        <div style={{ marginBottom: '2rem' }}>
          <div style={{ display: 'inline-flex', alignItems: 'center', gap: '0.45rem', padding: '0.2rem 0.65rem', borderRadius: 'var(--radius-full)', background: 'rgba(99, 102, 241, 0.12)', border: '1px solid rgba(99, 102, 241, 0.25)', marginBottom: '0.5rem' }}>
            <span className="ai-pulse-dot" />
            <span style={{ fontSize: '0.75rem', fontWeight: 600, color: 'var(--text-accent)', textTransform: 'uppercase' }}>
              Interview Studio Configuration
            </span>
          </div>
          <h1 style={{ fontSize: '1.8rem', fontWeight: 800, letterSpacing: '-0.025em', margin: 0, color: '#ffffff' }}>
            Configure Mock Interview
          </h1>
          <p style={{ color: 'var(--text-muted)', fontSize: '0.875rem', margin: '0.25rem 0 0 0' }}>
            Customize generation mode, domain track, technical depth, and response interface.
          </p>
        </div>

        {error && (
          <div className="alert alert-danger" style={{ marginBottom: '1.5rem' }}>
            {error}
          </div>
        )}

        {loading ? (
          <div className="card" style={{ textAlign: 'center', padding: '3.5rem' }}>
            <div className="skeleton" style={{ width: '40%', height: '24px', margin: '0 auto 1rem auto' }} />
            <div className="skeleton" style={{ width: '60%', height: '14px', margin: '0 auto' }} />
          </div>
        ) : (
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 340px', gap: '1.75rem', alignItems: 'start' }}>
            
            {/* Left Column: Interactive Configuration Form */}
            <div style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
              
              {/* STEP 1: Format Mode */}
              <div className="card" style={{ padding: '1.5rem' }}>
                <div style={{ marginBottom: '1rem' }}>
                  <h2 style={{ fontSize: '1.05rem', fontWeight: 600, margin: 0, color: 'var(--text-primary)' }}>
                    1. Choose Interview Mode
                  </h2>
                  <p style={{ color: 'var(--text-muted)', fontSize: '0.8125rem', margin: '0.2rem 0 0 0' }}>
                    Select how the interviewer will structure questions.
                  </p>
                </div>

                <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(240px, 1fr))', gap: '0.85rem' }}>
                  {interviewModes.map((item) => {
                    const isSelected = mode === item.id;
                    const Icon = item.icon;
                    return (
                      <div
                        key={item.id}
                        onClick={() => setMode(item.id)}
                        className={`mode-card-studio ${item.className} ${isSelected ? 'selected' : ''}`}
                      >
                        <div className="mode-header">
                          <div className="mode-icon-wrapper">
                            <Icon size={20} />
                          </div>
                          <div>
                            <div className="mode-title">{item.name}</div>
                            <span className="badge badge-secondary" style={{ fontSize: '0.65rem' }}>
                              {item.tag}
                            </span>
                          </div>
                        </div>
                        <p className="mode-desc">
                          {item.description}
                        </p>
                        <div className="mode-footer">
                          <span style={{ color: isSelected ? 'var(--text-primary)' : 'var(--text-muted)' }}>
                            {isSelected ? '✓ Selected Track' : 'Click to select'}
                          </span>
                          {isSelected && <span className="ai-pulse-dot" style={{ width: '6px', height: '6px' }} />}
                        </div>
                      </div>
                    );
                  })}
                </div>

                {/* Sub-selector: Resume */}
                {mode === 'RESUME' && (
                  <div style={{ marginTop: '1.25rem', paddingTop: '1rem', borderTop: '1px solid var(--border)' }}>
                    <label className="form-label">Select Grounding Resume *</label>
                    {resumes.length === 0 ? (
                      <div style={{ fontSize: '0.8125rem', color: 'var(--danger)', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                        No resumes uploaded yet.
                        <button type="button" onClick={() => navigate('/resume-projects')} className="btn btn-secondary btn-sm">
                          Upload Resume
                        </button>
                      </div>
                    ) : (
                      <select
                        className="form-select"
                        value={selectedResumeId}
                        onChange={(e) => setSelectedResumeId(e.target.value)}
                      >
                        {resumes.map((r) => (
                          <option key={r.id} value={r.id}>
                            {r.fileName} {r.isPrimary ? '· [Primary]' : ''} ({new Date(r.createdAt).toLocaleDateString()})
                          </option>
                        ))}
                      </select>
                    )}
                  </div>
                )}

                {/* Sub-selector: Project */}
                {mode === 'PROJECT' && (
                  <div style={{ marginTop: '1.25rem', paddingTop: '1rem', borderTop: '1px solid var(--border)' }}>
                    <label className="form-label">Select Project to Defend *</label>
                    {projects.length === 0 ? (
                      <div style={{ fontSize: '0.8125rem', color: 'var(--danger)', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                        No software projects saved yet.
                        <button type="button" onClick={() => navigate('/resume-projects')} className="btn btn-secondary btn-sm">
                          Add Project
                        </button>
                      </div>
                    ) : (
                      <select
                        className="form-select"
                        value={selectedProjectId}
                        onChange={(e) => setSelectedProjectId(e.target.value)}
                      >
                        {projects.map((p) => (
                          <option key={p.id} value={p.id}>
                            {p.title} · ({p.technologies || 'General Stack'})
                          </option>
                        ))}
                      </select>
                    )}
                  </div>
                )}

                {/* Sub-selector: Job Description */}
                {mode === 'JOB_DESCRIPTION' && (
                  <div style={{ marginTop: '1.25rem', paddingTop: '1rem', borderTop: '1px solid var(--border)' }}>
                    <label className="form-label">Target Job Description *</label>
                    {jds.length === 0 ? (
                      <div style={{ fontSize: '0.8125rem', color: 'var(--danger)', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                        No job descriptions saved yet.
                        <button type="button" onClick={() => navigate('/job-descriptions')} className="btn btn-secondary btn-sm">
                          Add Job Description
                        </button>
                      </div>
                    ) : (
                      <select
                        className="form-select"
                        value={selectedJdId}
                        onChange={(e) => setSelectedJdId(e.target.value)}
                      >
                        {jds.map((jd) => (
                          <option key={jd.id} value={jd.id}>
                            {jd.targetRole} {jd.companyName ? `at ${jd.companyName}` : ''}
                          </option>
                        ))}
                      </select>
                    )}
                  </div>
                )}
              </div>

              {/* STEP 2: Category Track */}
              <div className="card" style={{ padding: '1.5rem' }}>
                <div style={{ marginBottom: '1rem' }}>
                  <h2 style={{ fontSize: '1.05rem', fontWeight: 600, margin: 0, color: 'var(--text-primary)' }}>
                    2. Select Technical Domain Track
                  </h2>
                  <p style={{ color: 'var(--text-muted)', fontSize: '0.8125rem', margin: '0.2rem 0 0 0' }}>
                    Choose the primary subject focus for this session.
                  </p>
                </div>

                <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(180px, 1fr))', gap: '0.75rem' }}>
                  {categories.map((cat) => {
                    const isSelected = selectedCategory && selectedCategory.id === cat.id;
                    return (
                      <div
                        key={cat.id}
                        onClick={() => setSelectedCategory(cat)}
                        className="card card-interactive"
                        style={{
                          padding: '0.875rem 1rem',
                          background: isSelected ? 'var(--surface-raised)' : 'var(--surface)',
                          borderColor: isSelected ? 'var(--primary)' : 'var(--border)',
                          boxShadow: isSelected ? '0 0 14px rgba(99, 102, 241, 0.2)' : 'none'
                        }}
                      >
                        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                          <span style={{ fontSize: '0.875rem', fontWeight: 600, color: isSelected ? '#ffffff' : 'var(--text-primary)' }}>
                            {cat.name}
                          </span>
                          {isSelected && <span style={{ width: '6px', height: '6px', borderRadius: '50%', background: 'var(--primary)' }} />}
                        </div>
                      </div>
                    );
                  })}
                </div>
              </div>

              {/* STEP 3: Parameters & Experience Mode */}
              <div className="card" style={{ padding: '1.5rem' }}>
                <div style={{ marginBottom: '1.25rem' }}>
                  <h2 style={{ fontSize: '1.05rem', fontWeight: 600, margin: 0, color: 'var(--text-primary)' }}>
                    3. Technical Depth & Response Format
                  </h2>
                </div>

                {/* Difficulty Selector */}
                <div style={{ marginBottom: '1.5rem' }}>
                  <label className="form-label" style={{ marginBottom: '0.5rem' }}>Target Difficulty Level</label>
                  <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: '0.75rem' }}>
                    {Object.entries(DIFFICULTY_CONFIG).map(([level, cfg]) => {
                      const isSelected = difficulty === level;
                      return (
                        <div
                          key={level}
                          onClick={() => setDifficulty(level)}
                          className="card card-interactive"
                          style={{
                            padding: '0.875rem',
                            background: isSelected ? 'var(--surface-raised)' : 'var(--surface)',
                            borderColor: isSelected ? cfg.color : 'var(--border)',
                            textAlign: 'center'
                          }}
                        >
                          <div style={{ fontWeight: 600, fontSize: '0.875rem', color: isSelected ? '#ffffff' : 'var(--text-primary)', marginBottom: '0.2rem' }}>
                            {cfg.label}
                          </div>
                          <div style={{ fontSize: '0.7rem', color: 'var(--text-muted)', lineHeight: 1.4 }}>
                            {cfg.desc}
                          </div>
                        </div>
                      );
                    })}
                  </div>
                </div>

                {/* Steppers & Response Format Grid */}
                <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', gap: '1.25rem' }}>
                  
                  {/* Question Counter Stepper */}
                  <div>
                    <label className="form-label">Questions</label>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
                      <button
                        type="button"
                        onClick={() => setQuestionCount(Math.max(3, questionCount - 1))}
                        className="btn btn-secondary"
                        style={{ padding: '0.4rem 0.85rem' }}
                      >
                        −
                      </button>
                      <span style={{ fontSize: '1.25rem', fontWeight: 700, minWidth: '40px', textAlign: 'center', color: 'var(--text-primary)' }}>
                        {questionCount}
                      </span>
                      <button
                        type="button"
                        onClick={() => setQuestionCount(Math.min(15, questionCount + 1))}
                        className="btn btn-secondary"
                        style={{ padding: '0.4rem 0.85rem' }}
                      >
                        +
                      </button>
                    </div>
                  </div>

                  {/* Interaction Mode */}
                  <div>
                    <label className="form-label">Response Interface</label>
                    <div className="segmented-control" style={{ width: '100%' }}>
                      <button
                        type="button"
                        className={`segmented-btn ${interactionMode === 'TEXT' ? 'active' : ''}`}
                        onClick={() => setInteractionMode('TEXT')}
                        style={{ flex: 1, textAlign: 'center' }}
                      >
                        Text Input
                      </button>
                      <button
                        type="button"
                        className={`segmented-btn ${interactionMode === 'VOICE' ? 'active' : ''}`}
                        onClick={() => setInteractionMode('VOICE')}
                        style={{ flex: 1, textAlign: 'center' }}
                      >
                        Voice / Spoken
                      </button>
                    </div>
                  </div>
                </div>
              </div>

            </div>

            {/* Right Column: Live Session Blueprint Sidebar */}
            <div style={{ position: 'sticky', top: '5.5rem' }}>
              <div className="card card-hero" style={{ padding: '1.5rem', borderRadius: 'var(--radius-md)' }}>
                <div style={{ fontSize: '0.75rem', fontWeight: 600, color: 'var(--text-accent)', textTransform: 'uppercase', letterSpacing: '0.04em', marginBottom: '0.5rem' }}>
                  Session Blueprint
                </div>
                <h3 style={{ fontSize: '1.15rem', fontWeight: 600, color: '#ffffff', margin: '0 0 1rem 0' }}>
                  {selectedCategory?.name || 'Interview Session'}
                </h3>

                <div style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem', fontSize: '0.8125rem', color: 'var(--text-secondary)', marginBottom: '1.5rem' }}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', paddingBottom: '0.5rem', borderBottom: '1px solid rgba(255,255,255,0.06)' }}>
                    <span>Structure:</span>
                    <strong style={{ color: '#ffffff' }}>{interviewModes.find(m => m.id === mode)?.name}</strong>
                  </div>
                  <div style={{ display: 'flex', justifyContent: 'space-between', paddingBottom: '0.5rem', borderBottom: '1px solid rgba(255,255,255,0.06)' }}>
                    <span>Target Depth:</span>
                    <strong style={{ color: DIFFICULTY_CONFIG[difficulty]?.color }}>{difficulty}</strong>
                  </div>
                  <div style={{ display: 'flex', justifyContent: 'space-between', paddingBottom: '0.5rem', borderBottom: '1px solid rgba(255,255,255,0.06)' }}>
                    <span>Questions:</span>
                    <strong style={{ color: '#ffffff' }}>{questionCount} prompts</strong>
                  </div>
                  <div style={{ display: 'flex', justifyContent: 'space-between', paddingBottom: '0.5rem', borderBottom: '1px solid rgba(255,255,255,0.06)' }}>
                    <span>Input Mode:</span>
                    <strong style={{ color: '#ffffff' }}>{interactionMode === 'VOICE' ? 'Spoken Speech-to-Text' : 'Keyboard Text Editor'}</strong>
                  </div>
                  <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                    <span>Est. Time:</span>
                    <strong style={{ color: '#ffffff' }}>~{questionCount * 4} minutes</strong>
                  </div>
                </div>

                <button
                  type="button"
                  onClick={handleStartInterview}
                  disabled={submitting}
                  className="btn btn-primary btn-lg"
                  style={{ width: '100%', justifyContent: 'center' }}
                >
                  <PlayIcon size={16} />
                  <span>{submitting ? 'Initializing AI Session...' : 'Start Interview Session →'}</span>
                </button>

                <p style={{ color: 'var(--text-muted)', fontSize: '0.75rem', textAlign: 'center', margin: '0.875rem 0 0 0', lineHeight: 1.4 }}>
                  Answers are evaluated with Groq LLM across correctness, technical depth, and communicative clarity.
                </p>
              </div>
            </div>

          </div>
        )}
      </div>
    </MainLayout>
  );
};

export default InterviewSetupPage;

