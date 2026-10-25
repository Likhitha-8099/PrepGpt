import React from 'react';
import { Link } from 'react-router-dom';
import { TerminalIcon, ShieldIcon } from './common/Icons';

const Footer = () => {
  return (
    <footer className="footer">
      <div className="container footer-content">
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.4rem', color: 'var(--text-primary)', fontWeight: 600 }}>
            <TerminalIcon size={16} color="var(--primary)" />
            <span>PrepGPT Studio</span>
          </div>
          <span style={{ color: 'var(--border)' }}>|</span>
          <span>Technical Interview & Career Preparation Platform</span>
        </div>

        <div className="footer-links">
          <Link to="/interview/setup" className="footer-link">Mock Studio</Link>
          <Link to="/resume-templates" className="footer-link">ATS Resume Builder</Link>
          <Link to="/job-descriptions" className="footer-link">JD Matcher</Link>
          <div style={{ display: 'inline-flex', alignItems: 'center', gap: '0.35rem', color: 'var(--text-muted)', fontSize: '0.75rem', padding: '0.15rem 0.5rem', background: 'var(--surface-raised)', borderRadius: 'var(--radius-xs)', border: '1px solid var(--border)' }}>
            <ShieldIcon size={12} color="var(--accent-teal)" />
            <span>Localhost Sandbox Ready</span>
          </div>
        </div>

        <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>
          &copy; {new Date().getFullYear()} PrepGPT. Built for Software Engineers & Tech Candidates.
        </div>
      </div>
    </footer>
  );
};

export default Footer;

