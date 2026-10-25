import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import MainLayout from '../layouts/MainLayout';
import { useAuth } from '../hooks/useAuth';
import { TerminalIcon, CheckIcon, CpuIcon, ShieldIcon } from '../components/common/Icons';

const LoginPage = () => {
  const navigate = useNavigate();
  const { login } = useAuth();

  const [formData, setFormData] = useState({
    email: '',
    password: ''
  });
  const [loading, setLoading] = useState(false);
  const [errorMessage, setErrorMessage] = useState('');
  const [fieldErrors, setFieldErrors] = useState({});

  const validateForm = () => {
    const errors = {};
    if (!formData.email.trim()) {
      errors.email = 'Email address is required';
    } else if (!/\S+@\S+\.\S+/.test(formData.email)) {
      errors.email = 'Please enter a valid email address';
    }

    if (!formData.password) {
      errors.password = 'Password is required';
    }

    setFieldErrors(errors);
    return Object.keys(errors).length === 0;
  };

  const handleChange = (e) => {
    setFormData({ ...formData, [e.target.name]: e.target.value });
    if (fieldErrors[e.target.name]) {
      setFieldErrors({ ...fieldErrors, [e.target.name]: '' });
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setErrorMessage('');

    if (!validateForm()) return;

    setLoading(true);
    try {
      await login(formData.email, formData.password);
      navigate('/dashboard');
    } catch (err) {
      console.error('Login error:', err);
      if (err.response) {
        if (err.response.status === 401) {
          setErrorMessage('Invalid email or password. Please verify your credentials.');
        } else {
          setErrorMessage(err.response.data?.message || 'Login failed. Please try again.');
        }
      } else {
        setErrorMessage('Unable to connect to the server. Please check your network connection.');
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <MainLayout>
      <div className="auth-split-wrapper">
        
        {/* Left Side: Technical Product Showcase */}
        <div className="auth-showcase-panel">
          <div style={{
            display: 'inline-flex',
            alignItems: 'center',
            gap: '0.45rem',
            padding: '0.2rem 0.65rem',
            borderRadius: 'var(--radius-full)',
            background: 'rgba(99, 102, 241, 0.1)',
            border: '1px solid rgba(99, 102, 241, 0.25)',
            color: 'var(--text-accent)',
            fontSize: '0.75rem',
            fontWeight: 600,
            width: 'fit-content'
          }}>
            <TerminalIcon size={14} />
            <span>PREPGPT STUDIO PLATFORM</span>
          </div>

          <h2 className="auth-showcase-title">
            Step into your private technical interview workspace.
          </h2>

          <p style={{ color: 'var(--text-secondary)', fontSize: '0.95rem', lineHeight: 1.6, margin: 0 }}>
            Practice algorithm challenges, voice interview rounds, resume defense, and JD alignment with instantaneous, actionable scoring.
          </p>

          <div className="auth-feature-list">
            <div className="auth-feature-item">
              <CheckIcon size={16} color="var(--accent-teal)" />
              <span>Realistic multi-turn adaptive follow-ups probing edge cases</span>
            </div>
            <div className="auth-feature-item">
              <CheckIcon size={16} color="var(--accent-teal)" />
              <span>5 Industry-certified ATS resume templates with instant Groq enhancement</span>
            </div>
            <div className="auth-feature-item">
              <CheckIcon size={16} color="var(--accent-teal)" />
              <span>Full local developer sandbox with zero external data telemetry</span>
            </div>
          </div>

          <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginTop: '1rem', color: 'var(--text-muted)', fontSize: '0.75rem' }}>
            <ShieldIcon size={14} color="var(--accent-teal)" />
            <span>Secure local authentication &bull; BCrypt hash &bull; JWT stateless tokens</span>
          </div>
        </div>

        {/* Right Side: Studio Sign In Card */}
        <div className="card" style={{ padding: '2.25rem 2rem' }}>
          <div style={{ marginBottom: '1.5rem' }}>
            <h1 style={{ fontSize: '1.4rem', fontWeight: 700, letterSpacing: '-0.02em', margin: '0 0 0.35rem 0', color: '#ffffff' }}>
              Welcome back
            </h1>
            <p style={{ color: 'var(--text-muted)', fontSize: '0.85rem', margin: 0 }}>
              Enter your credentials to access your interview workspace
            </p>
          </div>

          {errorMessage && (
            <div className="alert alert-danger" style={{ marginBottom: '1.25rem' }}>
              {errorMessage}
            </div>
          )}

          <form onSubmit={handleSubmit}>
            <div className="form-group">
              <label className="form-label">Email address</label>
              <input
                type="email"
                name="email"
                className="form-input"
                placeholder="developer@example.com"
                value={formData.email}
                onChange={handleChange}
                autoComplete="email"
              />
              {fieldErrors.email && (
                <span style={{ color: 'var(--danger)', fontSize: '0.75rem', marginTop: '0.25rem', display: 'block' }}>
                  {fieldErrors.email}
                </span>
              )}
            </div>

            <div className="form-group">
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.35rem' }}>
                <label className="form-label" style={{ margin: 0 }}>Password</label>
              </div>
              <input
                type="password"
                name="password"
                className="form-input"
                placeholder="••••••••"
                value={formData.password}
                onChange={handleChange}
                autoComplete="current-password"
              />
              {fieldErrors.password && (
                <span style={{ color: 'var(--danger)', fontSize: '0.75rem', marginTop: '0.25rem', display: 'block' }}>
                  {fieldErrors.password}
                </span>
              )}
            </div>

            <button
              type="submit"
              className="btn btn-primary"
              disabled={loading}
              style={{ width: '100%', marginTop: '1rem', padding: '0.65rem' }}
            >
              {loading ? 'Authenticating...' : 'Sign In to Workspace →'}
            </button>
          </form>

          <div style={{ textAlign: 'center', marginTop: '1.5rem', paddingTop: '1.25rem', borderTop: '1px solid var(--border)', fontSize: '0.8125rem', color: 'var(--text-secondary)' }}>
            New to PrepGPT?{' '}
            <Link to="/register" style={{ color: 'var(--primary)', fontWeight: 600 }}>
              Create an account
            </Link>
          </div>
        </div>

      </div>
    </MainLayout>
  );
};

export default LoginPage;

