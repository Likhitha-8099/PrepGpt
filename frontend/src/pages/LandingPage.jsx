import React from 'react';
import { Link } from 'react-router-dom';
import MainLayout from '../layouts/MainLayout';
import { 
  TerminalIcon, 
  CpuIcon, 
  DocumentIcon, 
  BriefcaseIcon, 
  SparklesIcon, 
  CheckIcon, 
  ArrowRightIcon, 
  PlayIcon,
  ShieldIcon,
  CodeIcon,
  MicIcon
} from '../components/common/Icons';

const LandingPage = () => {
  return (
    <MainLayout>
      <div style={{ maxWidth: '1100px', margin: '0 auto', padding: '3.5rem 0 5rem 0' }}>
        
        {/* Studio Badge */}
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.65rem', marginBottom: '1.25rem' }}>
          <div style={{
            display: 'inline-flex',
            alignItems: 'center',
            gap: '0.45rem',
            padding: '0.25rem 0.75rem',
            borderRadius: 'var(--radius-full)',
            background: 'rgba(99, 102, 241, 0.1)',
            border: '1px solid rgba(99, 102, 241, 0.25)',
            color: 'var(--text-accent)',
            fontSize: '0.8rem',
            fontWeight: 600,
            letterSpacing: '0.04em'
          }}>
            <span className="ai-pulse-dot" />
            <span>MODERN TECHNICAL INTERVIEW STUDIO</span>
          </div>

          <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>
            Groq Llama-3.3 Powered &bull; Evaluation Sandbox
          </span>
        </div>
        
        {/* Hero Section Split: Left Value Prop, Right Studio Terminal */}
        <div style={{
          display: 'grid',
          gridTemplateColumns: '1.15fr 1fr',
          gap: '2.5rem',
          alignItems: 'center',
          marginBottom: '4.5rem'
        }}>
          <div>
            <h1 style={{ 
              fontSize: '3.1rem', 
              fontWeight: 800, 
              lineHeight: 1.12, 
              marginBottom: '1.25rem', 
              letterSpacing: '-0.035em',
              color: '#ffffff'
            }}>
              Engineered for candidates who refuse to settle.
            </h1>

            <p style={{
              fontSize: '1.05rem',
              color: 'var(--text-secondary)',
              lineHeight: 1.6,
              marginBottom: '2rem'
            }}>
              Master technical coding assessments, deep architectural system design rounds, and real-time portfolio project defense. PrepGPT simulates ruthless hiring bars with live follow-up probing and multidimensional scoring.
            </p>

            <div style={{ display: 'flex', gap: '0.85rem', alignItems: 'center', flexWrap: 'wrap', marginBottom: '2rem' }}>
              <Link to="/interview/setup" className="btn btn-primary btn-lg">
                <PlayIcon size={16} />
                <span>Launch Mock Studio</span>
              </Link>
              <Link to="/dashboard" className="btn btn-secondary btn-lg">
                <span>View Dashboard</span>
                <ArrowRightIcon size={15} />
              </Link>
            </div>

            <div style={{ display: 'flex', gap: '1.5rem', flexWrap: 'wrap', fontSize: '0.8125rem', color: 'var(--text-muted)' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '0.4rem' }}>
                <CheckIcon size={14} color="var(--accent-teal)" />
                <span>6 Specialized Modes</span>
              </div>
              <div style={{ display: 'flex', alignItems: 'center', gap: '0.4rem' }}>
                <CheckIcon size={14} color="var(--accent-teal)" />
                <span>Voice & Text Capture</span>
              </div>
              <div style={{ display: 'flex', alignItems: 'center', gap: '0.4rem' }}>
                <CheckIcon size={14} color="var(--accent-teal)" />
                <span>ATS Resume Matcher</span>
              </div>
            </div>
          </div>

          {/* Live Studio Terminal Showcase */}
          <div className="terminal-window">
            <div className="terminal-header">
              <div className="terminal-dots">
                <span className="terminal-dot red" />
                <span className="terminal-dot yellow" />
                <span className="terminal-dot green" />
              </div>
              <span className="terminal-title">prepgpt-session // active_probe.sh</span>
              <span style={{ fontSize: '0.7rem', color: 'var(--accent-cyan)' }}>LIVE_FEED</span>
            </div>
            <div className="terminal-body" style={{ minHeight: '260px' }}>
              <p style={{ margin: '0 0 0.75rem 0' }}>
                <span className="terminal-prompt">$</span>
                <span style={{ color: '#e2e8f0' }}>prepgpt start --track=AI_MOCK --level=ADVANCED</span>
              </p>
              <p style={{ margin: '0 0 0.75rem 0', color: 'var(--accent-cyan)' }}>
                [AI Interviewer]: "In your distributed transaction service, how do you handle partial network partitions between your write-ahead log and read replicas?"
              </p>
              <div style={{
                background: 'rgba(255, 255, 255, 0.03)',
                borderLeft: '3px solid var(--primary)',
                padding: '0.5rem 0.75rem',
                margin: '0.5rem 0',
                fontSize: '0.775rem',
                color: 'var(--text-secondary)'
              }}>
                <span style={{ color: 'var(--text-muted)' }}>Candidate: </span>
                "We enforce Raft consensus with a quorum lease. If a minority partition is isolated, writes fail-fast..."
              </div>
              <p style={{ margin: '0.75rem 0 0 0', color: 'var(--accent-teal)', fontSize: '0.75rem' }}>
                ✓ Multidimensional Rubric: Correctness: 92% &bull; Clarity: 88% &bull; Edge-Cases: 85%
              </p>
            </div>
          </div>
        </div>

        {/* 6 Technical Preparation Modules */}
        <div style={{ marginBottom: '3.5rem' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-end', marginBottom: '1.5rem', flexWrap: 'wrap', gap: '1rem' }}>
            <div>
              <div style={{ fontSize: '0.75rem', fontWeight: 600, color: 'var(--primary)', textTransform: 'uppercase', letterSpacing: '0.06em', marginBottom: '0.25rem' }}>
                Technical Capabilities
              </div>
              <h2 style={{ fontSize: '1.75rem', fontWeight: 700, color: '#ffffff', letterSpacing: '-0.02em', margin: 0 }}>
                A Complete Developer Preparation Arsenal
              </h2>
            </div>
            <Link to="/interview/setup" style={{ fontSize: '0.85rem', color: 'var(--text-accent)', textDecoration: 'none', display: 'flex', alignItems: 'center', gap: '0.35rem' }}>
              Explore All Modes <ArrowRightIcon size={14} />
            </Link>
          </div>

          <div style={{
            display: 'grid',
            gridTemplateColumns: 'repeat(auto-fit, minmax(310px, 1fr))',
            gap: '1.25rem'
          }}>
            <div className="card card-interactive" style={{ padding: '1.5rem' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', marginBottom: '0.85rem' }}>
                <div style={{ width: '38px', height: '38px', borderRadius: 'var(--radius-sm)', background: 'rgba(99, 102, 241, 0.12)', display: 'flex', alignItems: 'center', justifyContent: 'center', color: '#818cf8' }}>
                  <CpuIcon size={20} />
                </div>
                <div>
                  <h3 style={{ fontSize: '1rem', fontWeight: 600, color: '#ffffff', margin: 0 }}>AI Adaptive Probing</h3>
                  <span style={{ fontSize: '0.725rem', color: 'var(--text-muted)' }}>Llama-3.3 Engine</span>
                </div>
              </div>
              <p style={{ color: 'var(--text-secondary)', fontSize: '0.825rem', lineHeight: 1.5, margin: 0 }}>
                Dynamic follow-up questions evaluate your reasoning depth rather than just surface-level textbook definitions.
              </p>
            </div>

            <div className="card card-interactive" style={{ padding: '1.5rem' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', marginBottom: '0.85rem' }}>
                <div style={{ width: '38px', height: '38px', borderRadius: 'var(--radius-sm)', background: 'rgba(56, 189, 248, 0.12)', display: 'flex', alignItems: 'center', justifyContent: 'center', color: '#38bdf8' }}>
                  <CodeIcon size={20} />
                </div>
                <div>
                  <h3 style={{ fontSize: '1rem', fontWeight: 600, color: '#ffffff', margin: 0 }}>Coding & DSA Practice</h3>
                  <span style={{ fontSize: '0.725rem', color: 'var(--text-muted)' }}>In-Browser Editor</span>
                </div>
              </div>
              <p style={{ color: 'var(--text-secondary)', fontSize: '0.825rem', lineHeight: 1.5, margin: 0 }}>
                Write solutions in Java, Python, or C++ with automated test-case evaluation and algorithmic complexity scrutiny.
              </p>
            </div>

            <div className="card card-interactive" style={{ padding: '1.5rem' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', marginBottom: '0.85rem' }}>
                <div style={{ width: '38px', height: '38px', borderRadius: 'var(--radius-sm)', background: 'rgba(16, 185, 129, 0.12)', display: 'flex', alignItems: 'center', justifyContent: 'center', color: '#34d399' }}>
                  <DocumentIcon size={20} />
                </div>
                <div>
                  <h3 style={{ fontSize: '1rem', fontWeight: 600, color: '#ffffff', margin: 0 }}>Resume-Based Mock</h3>
                  <span style={{ fontSize: '0.725rem', color: 'var(--text-muted)' }}>Experience Defense</span>
                </div>
              </div>
              <p style={{ color: 'var(--text-secondary)', fontSize: '0.825rem', lineHeight: 1.5, margin: 0 }}>
                Upload your resume to defend your technical background, claimed libraries, architectural contributions, and metrics.
              </p>
            </div>

            <div className="card card-interactive" style={{ padding: '1.5rem' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', marginBottom: '0.85rem' }}>
                <div style={{ width: '38px', height: '38px', borderRadius: 'var(--radius-sm)', background: 'rgba(245, 158, 11, 0.12)', display: 'flex', alignItems: 'center', justifyContent: 'center', color: '#fbbf24' }}>
                  <BriefcaseIcon size={20} />
                </div>
                <div>
                  <h3 style={{ fontSize: '1rem', fontWeight: 600, color: '#ffffff', margin: 0 }}>Job Description Matcher</h3>
                  <span style={{ fontSize: '0.725rem', color: 'var(--text-muted)' }}>Target Company Prep</span>
                </div>
              </div>
              <p style={{ color: 'var(--text-secondary)', fontSize: '0.825rem', lineHeight: 1.5, margin: 0 }}>
                Paste the exact job posting from Google, Amazon, or high-growth startups to practice targeted questions.
              </p>
            </div>

            <div className="card card-interactive" style={{ padding: '1.5rem' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', marginBottom: '0.85rem' }}>
                <div style={{ width: '38px', height: '38px', borderRadius: 'var(--radius-sm)', background: 'rgba(236, 72, 153, 0.12)', display: 'flex', alignItems: 'center', justifyContent: 'center', color: '#f472b6' }}>
                  <MicIcon size={20} />
                </div>
                <div>
                  <h3 style={{ fontSize: '1rem', fontWeight: 600, color: '#ffffff', margin: 0 }}>Voice Audio Practice</h3>
                  <span style={{ fontSize: '0.725rem', color: 'var(--text-muted)' }}>Verbal Fluency</span>
                </div>
              </div>
              <p style={{ color: 'var(--text-secondary)', fontSize: '0.825rem', lineHeight: 1.5, margin: 0 }}>
                Practice verbal clarity and vocal responses with simulated interviewer audio playback and speech transcription.
              </p>
            </div>

            <div className="card card-interactive" style={{ padding: '1.5rem' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', marginBottom: '0.85rem' }}>
                <div style={{ width: '38px', height: '38px', borderRadius: 'var(--radius-sm)', background: 'rgba(139, 92, 246, 0.12)', display: 'flex', alignItems: 'center', justifyContent: 'center', color: '#a78bfa' }}>
                  <SparklesIcon size={20} />
                </div>
                <div>
                  <h3 style={{ fontSize: '1rem', fontWeight: 600, color: '#ffffff', margin: 0 }}>ATS Resume Templates</h3>
                  <span style={{ fontSize: '0.725rem', color: 'var(--text-muted)' }}>Recruiter Compliant</span>
                </div>
              </div>
              <p style={{ color: 'var(--text-secondary)', fontSize: '0.825rem', lineHeight: 1.5, margin: 0 }}>
                Build ATS-compliant resumes with 5 industry templates, Groq AI content enhancement, and multi-format exports.
              </p>
            </div>
          </div>
        </div>

        {/* Technical Callout Banner */}
        <div className="card card-hero" style={{ padding: '2rem 2.5rem', borderRadius: 'var(--radius-md)' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '1.5rem' }}>
            <div>
              <div style={{ fontSize: '0.75rem', fontWeight: 600, color: 'var(--text-accent)', letterSpacing: '0.05em', textTransform: 'uppercase', marginBottom: '0.25rem' }}>
                Ready to Test Your Engineering Skills?
              </div>
              <h3 style={{ fontSize: '1.35rem', fontWeight: 700, color: '#ffffff', margin: 0 }}>
                Start a session in under 60 seconds. No credit card required.
              </h3>
            </div>
            <div style={{ display: 'flex', gap: '0.75rem' }}>
              <Link to="/interview/setup" className="btn btn-primary">
                Launch Interview Studio →
              </Link>
            </div>
          </div>
        </div>

      </div>
    </MainLayout>
  );
};

export default LandingPage;

