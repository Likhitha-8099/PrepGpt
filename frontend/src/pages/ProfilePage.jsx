import React, { useState, useEffect } from 'react';
import MainLayout from '../layouts/MainLayout';
import {
  SparklesIcon,
  CheckIcon,
  BriefcaseIcon,
  CodeIcon,
  ShieldIcon
} from '../components/common/Icons';
import { getStudentProfile, updateStudentProfile } from '../services/profileService';

const ProfilePage = () => {
  const [profile, setProfile] = useState({
    fullName: '',
    email: '',
    role: '',
    bio: '',
    targetRole: 'Software Engineer',
    experienceLevel: 'FRESHER',
    phone: '',
    githubUrl: '',
    linkedinUrl: ''
  });

  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [successMessage, setSuccessMessage] = useState('');
  const [errorMessage, setErrorMessage] = useState('');

  useEffect(() => {
    fetchProfile();
  }, []);

  const fetchProfile = async () => {
    setLoading(true);
    try {
      const data = await getStudentProfile();
      setProfile({
        fullName: data.fullName || '',
        email: data.email || '',
        role: data.role || 'ROLE_STUDENT',
        bio: data.bio || '',
        targetRole: data.targetRole || 'Software Engineer',
        experienceLevel: data.experienceLevel || 'FRESHER',
        phone: data.phone || '',
        githubUrl: data.githubUrl || '',
        linkedinUrl: data.linkedinUrl || ''
      });
    } catch (err) {
      console.error('Failed to load profile:', err);
      setErrorMessage('Unable to load student profile details.');
    } finally {
      setLoading(false);
    }
  };

  const handleChange = (e) => {
    setProfile({ ...profile, [e.target.name]: e.target.value });
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setSaving(true);
    setSuccessMessage('');
    setErrorMessage('');

    try {
      await updateStudentProfile({
        bio: profile.bio,
        targetRole: profile.targetRole,
        experienceLevel: profile.experienceLevel,
        phone: profile.phone,
        githubUrl: profile.githubUrl,
        linkedinUrl: profile.linkedinUrl
      });
      setSuccessMessage('Candidate profile settings updated successfully.');
    } catch (err) {
      console.error('Failed to update profile:', err);
      setErrorMessage('Unable to save changes. Please try again.');
    } finally {
      setSaving(false);
    }
  };

  return (
    <MainLayout>
      <div style={{ maxWidth: '820px', margin: '0 auto', paddingBottom: '3.5rem' }}>
        {/* Header */}
        <div style={{ marginBottom: '2rem' }}>
          <div style={{ display: 'inline-flex', alignItems: 'center', gap: '0.5rem', marginBottom: '0.35rem' }}>
            <span className="studio-pill">
              <span className="studio-dot" /> CANDIDATE PROFILE
            </span>
          </div>
          <h1 style={{ fontSize: '1.875rem', fontWeight: 700, letterSpacing: '-0.025em', margin: 0, color: 'var(--text-primary)' }}>
            Profile & Interview Calibration
          </h1>
          <p style={{ color: 'var(--text-muted)', fontSize: '0.9rem', margin: '0.35rem 0 0 0' }}>
            Configure candidate identity, target engineering seniority, and career links to calibrate difficulty algorithms.
          </p>
        </div>

        {loading ? (
          <div className="card" style={{ padding: '3rem', textAlign: 'center' }}>
            <div className="skeleton" style={{ width: '40%', height: '22px', margin: '0 auto 1rem auto' }} />
            <div className="skeleton" style={{ width: '70%', height: '14px', margin: '0 auto' }} />
          </div>
        ) : (
          <div className="card studio-terminal" style={{ padding: '2rem', border: '1px solid var(--border-editor)' }}>
            {successMessage && (
              <div className="alert alert-success" style={{ marginBottom: '1.5rem', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                <CheckIcon size={16} />
                <span>{successMessage}</span>
              </div>
            )}

            {errorMessage && (
              <div className="alert alert-danger" style={{ marginBottom: '1.5rem' }}>
                {errorMessage}
              </div>
            )}

            <form onSubmit={handleSubmit}>
              {/* Account Identity (Read-only) */}
              <div style={{
                display: 'grid',
                gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))',
                gap: '1.25rem',
                marginBottom: '2rem',
                padding: '1.25rem',
                background: 'var(--surface-raised)',
                borderRadius: 'var(--radius-md)',
                border: '1px solid var(--border)'
              }}>
                <div>
                  <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', textTransform: 'uppercase', letterSpacing: '0.04em', fontWeight: 600 }}>
                    Full Name
                  </div>
                  <div style={{ fontWeight: 600, marginTop: '0.35rem', fontSize: '1rem', color: 'var(--text-primary)' }}>
                    {profile.fullName || '—'}
                  </div>
                </div>
                <div>
                  <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', textTransform: 'uppercase', letterSpacing: '0.04em', fontWeight: 600 }}>
                    Email Address
                  </div>
                  <div style={{ fontWeight: 500, marginTop: '0.35rem', fontSize: '0.9375rem', color: 'var(--text-secondary)' }}>
                    {profile.email || '—'}
                  </div>
                </div>
                <div>
                  <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', textTransform: 'uppercase', letterSpacing: '0.04em', fontWeight: 600 }}>
                    System Role
                  </div>
                  <div style={{ marginTop: '0.35rem', display: 'flex', alignItems: 'center', gap: '0.35rem' }}>
                    <ShieldIcon size={14} style={{ color: 'var(--primary)' }} />
                    <span className="badge badge-secondary" style={{ fontSize: '0.75rem' }}>
                      {profile.role.replace('ROLE_', '')}
                    </span>
                  </div>
                </div>
              </div>

              {/* Editable Fields */}
              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(260px, 1fr))', gap: '1.25rem' }}>
                <div className="form-group">
                  <label className="form-label" style={{ display: 'flex', alignItems: 'center', gap: '0.35rem' }}>
                    <BriefcaseIcon size={14} style={{ color: 'var(--primary)' }} />
                    <span>Target Engineering Role</span>
                  </label>
                  <input
                    type="text"
                    name="targetRole"
                    className="form-input"
                    placeholder="e.g. Java Backend Developer, Full Stack Engineer"
                    value={profile.targetRole}
                    onChange={handleChange}
                  />
                </div>

                <div className="form-group">
                  <label className="form-label" style={{ display: 'flex', alignItems: 'center', gap: '0.35rem' }}>
                    <CodeIcon size={14} style={{ color: 'var(--primary)' }} />
                    <span>Experience Level</span>
                  </label>
                  <select
                    name="experienceLevel"
                    className="form-select"
                    value={profile.experienceLevel}
                    onChange={handleChange}
                  >
                    <option value="FRESHER">Fresher / Graduate (0 years)</option>
                    <option value="INTERN">Intern / Working Student</option>
                    <option value="JUNIOR">Junior Engineer (1-2 years)</option>
                    <option value="MID_LEVEL">Mid-Level Engineer (3-5 years)</option>
                    <option value="SENIOR">Senior Staff / Lead (5+ years)</option>
                  </select>
                </div>
              </div>

              <div className="form-group">
                <label className="form-label">Contact Phone</label>
                <input
                  type="text"
                  name="phone"
                  className="form-input"
                  placeholder="+1 (555) 000-0000"
                  value={profile.phone}
                  onChange={handleChange}
                />
              </div>

              <div className="form-group">
                <label className="form-label">Professional Summary & Engineering Focus</label>
                <textarea
                  name="bio"
                  className="form-textarea"
                  rows="3"
                  placeholder="Summary of engineering competencies, distributed systems background, and career aspirations..."
                  value={profile.bio}
                  onChange={handleChange}
                  style={{ lineHeight: 1.5 }}
                />
              </div>

              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(260px, 1fr))', gap: '1.25rem' }}>
                <div className="form-group">
                  <label className="form-label">GitHub Profile URL</label>
                  <input
                    type="url"
                    name="githubUrl"
                    className="form-input"
                    placeholder="https://github.com/username"
                    value={profile.githubUrl}
                    onChange={handleChange}
                  />
                </div>

                <div className="form-group">
                  <label className="form-label">LinkedIn Profile URL</label>
                  <input
                    type="url"
                    name="linkedinUrl"
                    className="form-input"
                    placeholder="https://linkedin.com/in/username"
                    value={profile.linkedinUrl}
                    onChange={handleChange}
                  />
                </div>
              </div>

              <div style={{ display: 'flex', justifyContent: 'flex-end', marginTop: '1.75rem', paddingTop: '1.25rem', borderTop: '1px solid var(--border)' }}>
                <button
                  type="submit"
                  className="btn btn-primary"
                  disabled={saving}
                  style={{ display: 'inline-flex', alignItems: 'center', gap: '0.5rem', padding: '0.65rem 1.5rem' }}
                >
                  <CheckIcon size={15} />
                  <span>{saving ? 'Updating Settings...' : 'Save Profile Settings'}</span>
                </button>
              </div>
            </form>
          </div>
        )}
      </div>
    </MainLayout>
  );
};

export default ProfilePage;
