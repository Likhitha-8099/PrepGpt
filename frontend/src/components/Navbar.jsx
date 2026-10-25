import React, { useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth';
import { TerminalIcon, CpuIcon, LayersIcon, DocumentIcon, BriefcaseIcon, ClockIcon, ShieldIcon } from './common/Icons';

const Navbar = () => {
  const location = useLocation();
  const navigate = useNavigate();
  const { user, isAuthenticated, logout } = useAuth();
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);

  const handleLogout = () => {
    logout();
    navigate('/login');
    setMobileMenuOpen(false);
  };

  const getInitials = (name) => {
    if (!name) return 'U';
    const parts = name.trim().split(' ');
    if (parts.length >= 2) return `${parts[0][0]}${parts[1][0]}`.toUpperCase();
    return name[0].toUpperCase();
  };

  return (
    <nav className="navbar">
      <div className="container navbar-content">
        <Link to="/" className="logo" onClick={() => setMobileMenuOpen(false)}>
          <div className="logo-icon" style={{ background: 'linear-gradient(135deg, #6366f1 0%, #38bdf8 100%)' }}>
            <TerminalIcon size={16} color="#ffffff" />
          </div>
          <div style={{ display: 'flex', flexDirection: 'column', lineHeight: 1.1 }}>
            <span style={{ fontWeight: 700, letterSpacing: '-0.02em', color: '#ffffff', fontSize: '1.05rem' }}>PrepGPT</span>
            <span style={{ fontSize: '0.625rem', color: 'var(--text-muted)', letterSpacing: '0.04em', textTransform: 'uppercase' }}>STUDIO</span>
          </div>
          <span style={{
            fontSize: '0.625rem',
            padding: '0.15rem 0.5rem',
            borderRadius: 'var(--radius-full)',
            background: 'rgba(56, 189, 248, 0.1)',
            border: '1px solid rgba(56, 189, 248, 0.25)',
            color: 'var(--accent-cyan)',
            fontWeight: 600,
            letterSpacing: '0.05em'
          }}>
            v2.4
          </span>
        </Link>

        {/* Mobile Hamburger Button */}
        <button 
          className="btn btn-secondary btn-sm"
          onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
          style={{ display: 'none', padding: '0.4rem', border: '1px solid var(--border)' }}
          id="mobile-nav-toggle"
          aria-label="Toggle Navigation"
        >
          <svg width="20" height="20" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            {mobileMenuOpen ? (
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M6 18L18 6M6 6l12 12" />
            ) : (
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M4 6h16M4 12h16M4 18h16" />
            )}
          </svg>
        </button>
        
        <div className={`nav-links ${mobileMenuOpen ? 'mobile-open' : ''}`}>
          {isAuthenticated ? (
            <>
              <Link 
                to="/dashboard" 
                className={`nav-link ${location.pathname === '/dashboard' ? 'active' : ''}`}
                onClick={() => setMobileMenuOpen(false)}
              >
                Dashboard
              </Link>

              <Link 
                to="/interview/setup" 
                className={`nav-link ${location.pathname === '/interview/setup' ? 'active' : ''}`}
                onClick={() => setMobileMenuOpen(false)}
              >
                Mock Studio
              </Link>

              <Link 
                to="/resume-projects" 
                className={`nav-link ${location.pathname === '/resume-projects' ? 'active' : ''}`}
                onClick={() => setMobileMenuOpen(false)}
              >
                Resumes & Projects
              </Link>

              <Link 
                to="/resume-templates" 
                className={`nav-link ${location.pathname.startsWith('/resume-templates') || location.pathname.startsWith('/resume-builder') ? 'active' : ''}`}
                onClick={() => setMobileMenuOpen(false)}
              >
                ATS Builder
              </Link>

              <Link 
                to="/job-descriptions" 
                className={`nav-link ${location.pathname === '/job-descriptions' ? 'active' : ''}`}
                onClick={() => setMobileMenuOpen(false)}
              >
                JD Portal
              </Link>

              <Link 
                to="/interview/history" 
                className={`nav-link ${location.pathname === '/interview/history' ? 'active' : ''}`}
                onClick={() => setMobileMenuOpen(false)}
              >
                History
              </Link>

              {user?.role === 'ROLE_ADMIN' && (
                <Link 
                  to="/admin/dashboard" 
                  className={`nav-link ${location.pathname.startsWith('/admin') ? 'active' : ''}`}
                  style={{ color: 'var(--text-accent)' }}
                  onClick={() => setMobileMenuOpen(false)}
                >
                  Admin
                </Link>
              )}

              <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', marginLeft: '0.75rem', paddingLeft: '0.75rem', borderLeft: '1px solid var(--border)' }}>
                <Link 
                  to="/profile" 
                  style={{ 
                    display: 'flex', 
                    alignItems: 'center', 
                    gap: '0.5rem', 
                    color: 'var(--text-secondary)',
                    textDecoration: 'none'
                  }}
                  title="Candidate Profile"
                  onClick={() => setMobileMenuOpen(false)}
                >
                  <span className="avatar-initials">
                    {getInitials(user?.fullName)}
                  </span>
                  <span style={{ fontSize: '0.8125rem', fontWeight: 500, color: 'var(--text-primary)', maxWidth: '120px', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                    {user?.fullName?.split(' ')[0] || 'Candidate'}
                  </span>
                </Link>

                <button onClick={handleLogout} className="btn btn-secondary btn-sm" style={{ padding: '0.3rem 0.65rem' }}>
                  Logout
                </button>
              </div>
            </>
          ) : (
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.65rem', marginLeft: '0.5rem' }}>
              <Link to="/login" className="btn btn-secondary btn-sm" onClick={() => setMobileMenuOpen(false)}>
                Sign In
              </Link>
              <Link to="/register" className="btn btn-primary btn-sm" onClick={() => setMobileMenuOpen(false)}>
                Launch Studio →
              </Link>
            </div>
          )}
        </div>
      </div>
    </nav>
  );
};

export default Navbar;

