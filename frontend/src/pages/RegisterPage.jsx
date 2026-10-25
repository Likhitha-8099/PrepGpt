import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import MainLayout from '../layouts/MainLayout';
import { useAuth } from '../hooks/useAuth';
import { TerminalIcon, CheckIcon, CpuIcon, ShieldIcon } from '../components/common/Icons';

const RegisterPage = () => {
  const navigate = useNavigate();
  const { register } = useAuth();

  const [formData, setFormData] = useState({
    fullName: '',
    email: '',
    password: ''
  });
  const [loading, setLoading] = useState(false);
  const [errorMessage, setErrorMessage] = useState('');
  const [fieldErrors, setFieldErrors] = useState({});

  const validateForm = () => {
    const errors = {};
    if (!formData.fullName.trim()) {
      errors.fullName = 'Full name is required';
    } else if (formData.fullName.trim().length < 2) {
      errors.fullName = 'Name must be at least 2 characters';
    }

    if (!formData.email.trim()) {
      errors.email = 'Email address is required';
    } else if (!/\S+@\S+\.\S+/.test(formData.email)) {
      errors.email = 'Please enter a valid email address';
    }

    if (!formData.password) {
      errors.password = 'Password is required';
    } else if (formData.password.length < 6) {
      errors.password = 'Password must be at least 6 characters';
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
      await register(formData.fullName, formData.email, formData.password);
      navigate('/dashboard');
    } catch (err) {
      console.error('Registration error:', err);
      if (err.response) {
        if (err.response.status === 409) {
          setErrorMessage('An account with this email already exists. Please sign in instead.');
        } else {
          setErrorMessage(err.response.data?.message || 'Registration failed. Please try again.');
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
            <span>DEVELOPER ONBOARDING</span>
          </div>

          <h2 className="auth-showcase-title">
            Level up your software engineering interview game.
          </h2>

          <p style={{ color: 'var(--text-secondary)', fontSize: '0.95rem', lineHeight: 1.6, margin: 0 }}>
            Join software engineers and students practicing on PrepGPT. Zero cost, complete local execution privacy, and personalized career roadmaps.
          </p>

          <div className="auth-feature-list">
            <div className="auth-feature-item">
              <CheckIcon size={16} color="var(--accent-teal)" />
              <span>Full access to all 6 interview simulation engines</span>
            </div>
            <div className="auth-feature-item">
              <CheckIcon size={16} color="var(--accent-teal)" />
              <span>Complete assessment history with question-by-question critique</span>
            </div>
            <div className="auth-feature-item">
              <CheckIcon size={16} color="var(--accent-teal)" />
              <span>Unlimited ATS resume generation and project portfolio tracking</span>
            </div>
          </div>

          <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginTop: '1rem', color: 'var(--text-muted)', fontSize: '0.75rem' }}>
            <ShieldIcon size={14} color="var(--accent-teal)" />
            <span>Local database sandbox &bull; Zero spam &bull; Pure developer focus</span>
          </div>
        </div>

        {/* Right Side: Studio Registration Card */}
        <div className="card" style={{ padding: '2.25rem 2rem' }}>
          <div style={{ marginBottom: '1.5rem' }}>
            <h1 style={{ fontSize: '1.4rem', fontWeight: 700, letterSpacing: '-0.02em', margin: '0 0 0.35rem 0', color: '#ffffff' }}>
              Create your account
            </h1>
            <p style={{ color: 'var(--text-muted)', fontSize: '0.85rem', margin: 0 }}>
              Set up your candidate profile to start practicing
            </p>
          </div>

          {errorMessage && (
            <div className="alert alert-danger" style={{ marginBottom: '1.25rem' }}>
              {errorMessage}
            </div>
          )}

          <form onSubmit={handleSubmit}>
            <div className="form-group">
              <label className="form-label">Full name</label>
              <input
                type="text"
                name="fullName"
                className="form-input"
                placeholder="Alex Chen"
                value={formData.fullName}
                onChange={handleChange}
                autoComplete="name"
              />
              {fieldErrors.fullName && (
                <span style={{ color: 'var(--danger)', fontSize: '0.75rem', marginTop: '0.25rem', display: 'block' }}>
                  {fieldErrors.fullName}
                </span>
              )}
            </div>

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
              <label className="form-label">Password</label>
              <input
                type="password"
                name="password"
                className="form-input"
                placeholder="Minimum 6 characters"
                value={formData.password}
                onChange={handleChange}
                autoComplete="new-password"
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
              {loading ? 'Creating account...' : 'Create Account & Launch Studio →'}
            </button>
          </form>

          <div style={{ textAlign: 'center', marginTop: '1.5rem', paddingTop: '1.25rem', borderTop: '1px solid var(--border)', fontSize: '0.8125rem', color: 'var(--text-secondary)' }}>
            Already have an account?{' '}
            <Link to="/login" style={{ color: 'var(--primary)', fontWeight: 600 }}>
              Sign in
            </Link>
          </div>
        </div>

      </div>
    </MainLayout>
  );
};

export default RegisterPage;

