import React from 'react';
import { Link } from 'react-router-dom';
import MainLayout from '../layouts/MainLayout';
import { TerminalIcon, ArrowRightIcon } from '../components/common/Icons';

const NotFoundPage = () => {
  return (
    <MainLayout>
      <div style={{ maxWidth: '580px', margin: '4rem auto', textAlign: 'center', padding: '0 1rem' }}>
        <div className="card studio-terminal" style={{ padding: '3rem 2rem', border: '1px solid var(--border-editor)' }}>
          <div style={{ display: 'inline-flex', alignItems: 'center', justifyContent: 'center', width: '56px', height: '56px', borderRadius: 'var(--radius-md)', background: 'var(--surface-raised)', border: '1px solid var(--border)', marginBottom: '1.25rem' }}>
            <TerminalIcon size={28} style={{ color: 'var(--primary)' }} />
          </div>
          <div style={{ fontSize: '0.8125rem', fontFamily: 'var(--font-mono, monospace)', color: 'var(--primary)', letterSpacing: '0.08em', marginBottom: '0.5rem' }}>
            HTTP ERROR 404 // ROUTE_NOT_FOUND
          </div>
          <h1 style={{ fontSize: '2.25rem', fontWeight: 700, letterSpacing: '-0.03em', margin: '0 0 0.75rem 0', color: 'var(--text-primary)' }}>
            Endpoint Not Resolved
          </h1>
          <p style={{ color: 'var(--text-muted)', fontSize: '0.9rem', lineHeight: 1.5, marginBottom: '2rem' }}>
            The requested technical workspace, session ID, or document route does not exist or has been relocated.
          </p>
          <div style={{ display: 'flex', justifyContent: 'center', gap: '0.75rem', flexWrap: 'wrap' }}>
            <Link to="/dashboard" className="btn btn-primary" style={{ display: 'inline-flex', alignItems: 'center', gap: '0.5rem' }}>
              <span>Command Center</span>
              <ArrowRightIcon size={14} />
            </Link>
            <Link to="/" className="btn btn-secondary">
              Home Studio
            </Link>
          </div>
        </div>
      </div>
    </MainLayout>
  );
};

export default NotFoundPage;
