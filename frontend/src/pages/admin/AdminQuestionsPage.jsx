import React, { useState, useEffect } from 'react';
import MainLayout from '../../layouts/MainLayout';
import AdminNav from '../../components/AdminNav';
import {
  getAdminQuestions,
  createAdminQuestion,
  updateAdminQuestion,
  deactivateQuestion,
  reactivateQuestion
} from '../../services/adminService';

const CATEGORIES = [
  { id: 1, name: 'Technical Interview' },
  { id: 2, name: 'DSA Interview' },
  { id: 3, name: 'Coding Interview' },
  { id: 4, name: 'HR Interview' },
  { id: 5, name: 'Behavioral Interview' },
  { id: 6, name: 'Project Interview' },
  { id: 7, name: 'Resume Interview' },
  { id: 8, name: 'Job Description Interview' }
];

const QUESTION_TYPES = [
  'TECHNICAL',
  'DSA',
  'CODING',
  'HR',
  'BEHAVIORAL',
  'PROJECT',
  'RESUME',
  'JOB_DESCRIPTION'
];

const DIFFICULTIES = ['EASY', 'MEDIUM', 'HARD'];

const AdminQuestionsPage = () => {
  const [questions, setQuestions] = useState([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(1);
  const [totalElements, setTotalElements] = useState(0);
  const [loading, setLoading] = useState(true);
  const [actionLoading, setActionLoading] = useState(false);
  const [banner, setBanner] = useState({ message: '', type: '' });

  // Filters
  const [filterCategoryId, setFilterCategoryId] = useState('');
  const [filterDifficulty, setFilterDifficulty] = useState('');

  // Modal State (Create / Edit)
  const [modalMode, setModalMode] = useState(null);
  const [editingId, setEditingId] = useState(null);
  const [formData, setFormData] = useState({
    categoryId: 1,
    questionText: '',
    questionType: 'TECHNICAL',
    difficulty: 'MEDIUM',
    expectedAnswerGuidance: ''
  });

  useEffect(() => {
    fetchQuestions(page, filterCategoryId, filterDifficulty);
  }, [page, filterCategoryId, filterDifficulty]);

  const fetchQuestions = async (pageNum, catId, diff) => {
    setLoading(true);
    try {
      const res = await getAdminQuestions(
        pageNum,
        15,
        catId ? Number(catId) : null,
        diff || null
      );
      const data = res.data;
      setQuestions(data.questions || data.content || []);
      setTotalPages(data.totalPages || 1);
      setTotalElements(data.totalElements || 0);
    } catch (err) {
      console.error('Failed to fetch admin questions:', err);
      showBanner('Failed to load questions from administrative bank.', 'danger');
    } finally {
      setLoading(false);
    }
  };

  const showBanner = (message, type = 'success') => {
    setBanner({ message, type });
    setTimeout(() => {
      setBanner({ message: '', type: '' });
    }, 6000);
  };

  const openCreateModal = () => {
    setFormData({
      categoryId: 1,
      questionText: '',
      questionType: 'TECHNICAL',
      difficulty: 'MEDIUM',
      expectedAnswerGuidance: ''
    });
    setEditingId(null);
    setModalMode('create');
  };

  const openEditModal = (q) => {
    setFormData({
      categoryId: q.categoryId || 1,
      questionText: q.questionText || '',
      questionType: q.questionType || 'TECHNICAL',
      difficulty: q.difficulty || 'MEDIUM',
      expectedAnswerGuidance: q.expectedAnswerGuidance || ''
    });
    setEditingId(q.id);
    setModalMode('edit');
  };

  const handleSaveQuestion = async (e) => {
    e.preventDefault();
    if (!formData.questionText.trim()) {
      showBanner('Question text cannot be blank.', 'danger');
      return;
    }

    setActionLoading(true);
    try {
      const payload = {
        categoryId: Number(formData.categoryId),
        questionText: formData.questionText.trim(),
        questionType: formData.questionType,
        difficulty: formData.difficulty,
        expectedAnswerGuidance: formData.expectedAnswerGuidance.trim()
      };

      if (modalMode === 'create') {
        await createAdminQuestion(payload);
        showBanner('New question added to bank.', 'success');
      } else {
        await updateAdminQuestion(editingId, payload);
        showBanner(`Question #${editingId} updated.`, 'success');
      }

      setModalMode(null);
      fetchQuestions(page, filterCategoryId, filterDifficulty);
    } catch (err) {
      const errorMsg = err.response?.data?.message || err.message || 'Operation failed.';
      showBanner(errorMsg, 'danger');
    } finally {
      setActionLoading(false);
    }
  };

  const handleToggleActive = async (q) => {
    const isDeactivating = q.active;
    const confirmMessage = isDeactivating
      ? `Deactivate Question #${q.id}? It will no longer appear in student sessions.`
      : `Reactivate Question #${q.id}?`;

    if (!window.confirm(confirmMessage)) return;

    setActionLoading(true);
    try {
      if (isDeactivating) {
        await deactivateQuestion(q.id);
        showBanner(`Question #${q.id} deactivated.`, 'success');
      } else {
        await reactivateQuestion(q.id);
        showBanner(`Question #${q.id} reactivated.`, 'success');
      }
      fetchQuestions(page, filterCategoryId, filterDifficulty);
    } catch (err) {
      const errorMsg = err.response?.data?.message || err.message || 'Toggle failed.';
      showBanner(errorMsg, 'danger');
    } finally {
      setActionLoading(false);
    }
  };

  return (
    <MainLayout>
      <div style={{ maxWidth: '1120px', margin: '0 auto', paddingBottom: '3rem' }}>
        {/* Header */}
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '1.75rem', flexWrap: 'wrap', gap: '1rem' }}>
          <div>
            <div style={{ fontSize: '0.75rem', fontWeight: 600, color: 'var(--text-muted)', textTransform: 'uppercase', letterSpacing: '0.05em', marginBottom: '0.25rem' }}>
              Administration
            </div>
            <h1 style={{ fontSize: '1.625rem', fontWeight: 600, letterSpacing: '-0.02em', margin: 0 }}>
              Question Bank
            </h1>
            <p style={{ color: 'var(--text-muted)', fontSize: '0.875rem', margin: '0.25rem 0 0 0' }}>
              {totalElements} curated interview questions across all technical and behavioral categories.
            </p>
          </div>

          <div style={{ display: 'flex', gap: '0.75rem', alignItems: 'center', flexWrap: 'wrap' }}>
            <AdminNav />
            <button type="button" onClick={openCreateModal} className="btn btn-primary btn-sm">
              New Question
            </button>
          </div>
        </div>

        {banner.message && (
          <div className={`alert alert-${banner.type === 'danger' ? 'danger' : 'success'}`} style={{ marginBottom: '1.25rem' }}>
            {banner.message}
          </div>
        )}

        {/* Filters */}
        <div className="card" style={{ padding: '0.875rem 1.25rem', marginBottom: '1.25rem' }}>
          <div style={{ display: 'flex', gap: '1rem', flexWrap: 'wrap', alignItems: 'center' }}>
            <div style={{ minWidth: '200px' }}>
              <select
                value={filterCategoryId}
                onChange={(e) => { setFilterCategoryId(e.target.value); setPage(0); }}
                className="form-select"
                style={{ fontSize: '0.8125rem' }}
              >
                <option value="">All Categories</option>
                {CATEGORIES.map(c => (
                  <option key={c.id} value={c.id}>{c.name}</option>
                ))}
              </select>
            </div>

            <div style={{ minWidth: '160px' }}>
              <select
                value={filterDifficulty}
                onChange={(e) => { setFilterDifficulty(e.target.value); setPage(0); }}
                className="form-select"
                style={{ fontSize: '0.8125rem' }}
              >
                <option value="">All Difficulties</option>
                {DIFFICULTIES.map(d => (
                  <option key={d} value={d}>{d}</option>
                ))}
              </select>
            </div>

            {(filterCategoryId || filterDifficulty) && (
              <button
                type="button"
                onClick={() => { setFilterCategoryId(''); setFilterDifficulty(''); setPage(0); }}
                className="btn btn-secondary btn-sm"
              >
                Clear Filters
              </button>
            )}
          </div>
        </div>

        {/* Questions Table */}
        {loading ? (
          <div className="card" style={{ padding: '2.5rem', textAlign: 'center' }}>
            <div className="skeleton" style={{ width: '40%', height: '20px', margin: '0 auto 1rem auto' }} />
            <div className="skeleton" style={{ width: '70%', height: '14px', margin: '0 auto' }} />
          </div>
        ) : questions.length === 0 ? (
          <div className="empty-state">
            <h3>No questions found</h3>
            <p>Try adjusting your category or difficulty filters.</p>
          </div>
        ) : (
          <div className="card" style={{ padding: '1.25rem' }}>
            <div className="table-container">
              <table className="data-table">
                <thead>
                  <tr>
                    <th style={{ width: '60px' }}>ID</th>
                    <th>Question</th>
                    <th>Category</th>
                    <th>Type</th>
                    <th>Difficulty</th>
                    <th>Status</th>
                    <th style={{ textAlign: 'right' }}>Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {questions.map((q) => (
                    <tr key={q.id}>
                      <td style={{ color: 'var(--text-muted)', fontFamily: 'var(--font-mono, monospace)', fontSize: '0.8125rem' }}>
                        #{q.id}
                      </td>
                      <td style={{ maxWidth: '440px' }}>
                        <div style={{ fontWeight: 500, color: 'var(--text-primary)', marginBottom: '0.2rem', lineHeight: 1.4 }}>
                          {q.questionText}
                        </div>
                        {q.expectedAnswerGuidance && (
                          <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                            Guidance: {q.expectedAnswerGuidance}
                          </div>
                        )}
                      </td>
                      <td style={{ color: 'var(--text-secondary)', fontSize: '0.8125rem', whiteSpace: 'nowrap' }}>
                        {q.categoryName || `Category #${q.categoryId}`}
                      </td>
                      <td>
                        <span className="badge badge-secondary" style={{ fontSize: '0.7rem' }}>
                          {q.questionType}
                        </span>
                      </td>
                      <td>
                        <span className={`badge ${
                          q.difficulty === 'HARD' ? 'badge-danger' :
                          q.difficulty === 'MEDIUM' ? 'badge-warning' :
                          'badge-success'
                        }`} style={{ fontSize: '0.7rem' }}>
                          {q.difficulty}
                        </span>
                      </td>
                      <td>
                        <span className={`badge ${q.active ? 'badge-success' : 'badge-secondary'}`} style={{ fontSize: '0.7rem' }}>
                          {q.active ? 'Active' : 'Inactive'}
                        </span>
                      </td>
                      <td style={{ textAlign: 'right', whiteSpace: 'nowrap' }}>
                        <div style={{ display: 'inline-flex', gap: '0.375rem' }}>
                          <button
                            type="button"
                            onClick={() => openEditModal(q)}
                            disabled={actionLoading}
                            className="btn btn-secondary btn-sm"
                          >
                            Edit
                          </button>
                          <button
                            type="button"
                            onClick={() => handleToggleActive(q)}
                            disabled={actionLoading}
                            className={`btn btn-sm ${q.active ? 'btn-danger' : 'btn-secondary'}`}
                          >
                            {q.active ? 'Deactivate' : 'Reactivate'}
                          </button>
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>

            {/* Pagination */}
            {totalPages > 1 && (
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginTop: '1.25rem', paddingTop: '1rem', borderTop: '1px solid var(--border)' }}>
                <span style={{ fontSize: '0.8125rem', color: 'var(--text-muted)' }}>
                  Page {page + 1} of {totalPages}
                </span>
                <div style={{ display: 'flex', gap: '0.375rem' }}>
                  <button
                    type="button"
                    className="btn btn-secondary btn-sm"
                    disabled={page === 0}
                    onClick={() => setPage(page - 1)}
                  >
                    Previous
                  </button>
                  <button
                    type="button"
                    className="btn btn-secondary btn-sm"
                    disabled={page >= totalPages - 1}
                    onClick={() => setPage(page + 1)}
                  >
                    Next
                  </button>
                </div>
              </div>
            )}
          </div>
        )}

        {/* Modal */}
        {modalMode && (
          <div style={{
            position: 'fixed',
            top: 0, left: 0, right: 0, bottom: 0,
            background: 'rgba(15, 23, 42, 0.45)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            zIndex: 1000,
            padding: '1rem'
          }}>
            <div className="card" style={{ maxWidth: '600px', width: '100%', maxHeight: '90vh', overflowY: 'auto', padding: '1.5rem' }}>
              <h3 style={{ margin: '0 0 1.25rem 0', fontSize: '1.125rem', fontWeight: 600 }}>
                {modalMode === 'create' ? 'New Question' : `Edit Question #${editingId}`}
              </h3>

              <form onSubmit={handleSaveQuestion}>
                <div className="form-group">
                  <label className="form-label">Category</label>
                  <select
                    value={formData.categoryId}
                    onChange={(e) => setFormData({ ...formData, categoryId: Number(e.target.value) })}
                    className="form-select"
                  >
                    {CATEGORIES.map(c => (
                      <option key={c.id} value={c.id}>{c.name}</option>
                    ))}
                  </select>
                </div>

                <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))', gap: '0.875rem' }}>
                  <div className="form-group">
                    <label className="form-label">Question Type</label>
                    <select
                      value={formData.questionType}
                      onChange={(e) => setFormData({ ...formData, questionType: e.target.value })}
                      className="form-select"
                    >
                      {QUESTION_TYPES.map(t => (
                        <option key={t} value={t}>{t}</option>
                      ))}
                    </select>
                  </div>

                  <div className="form-group">
                    <label className="form-label">Difficulty</label>
                    <select
                      value={formData.difficulty}
                      onChange={(e) => setFormData({ ...formData, difficulty: e.target.value })}
                      className="form-select"
                    >
                      {DIFFICULTIES.map(d => (
                        <option key={d} value={d}>{d}</option>
                      ))}
                    </select>
                  </div>
                </div>

                <div className="form-group">
                  <label className="form-label">Question Text *</label>
                  <textarea
                    rows="4"
                    required
                    value={formData.questionText}
                    onChange={(e) => setFormData({ ...formData, questionText: e.target.value })}
                    className="form-textarea"
                    placeholder="Enter the complete question prompt..."
                  />
                </div>

                <div className="form-group">
                  <label className="form-label">Expected Answer Guidance</label>
                  <textarea
                    rows="3"
                    value={formData.expectedAnswerGuidance}
                    onChange={(e) => setFormData({ ...formData, expectedAnswerGuidance: e.target.value })}
                    className="form-textarea"
                    placeholder="Key concepts, terminology, or trade-offs candidate should discuss..."
                  />
                </div>

                <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '0.5rem', marginTop: '1.25rem' }}>
                  <button
                    type="button"
                    className="btn btn-secondary"
                    onClick={() => setModalMode(null)}
                    disabled={actionLoading}
                  >
                    Cancel
                  </button>
                  <button
                    type="submit"
                    className="btn btn-primary"
                    disabled={actionLoading}
                  >
                    {actionLoading ? 'Saving...' : 'Save Question'}
                  </button>
                </div>
              </form>
            </div>
          </div>
        )}
      </div>
    </MainLayout>
  );
};

export default AdminQuestionsPage;
