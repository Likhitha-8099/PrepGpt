import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import MainLayout from '../layouts/MainLayout';
import AudioQuestionPlayer from '../components/AudioQuestionPlayer';
import VoiceRecorder from '../components/VoiceRecorder';
import {
  TerminalIcon,
  CpuIcon,
  MicIcon,
  CheckIcon,
  SparklesIcon,
  ArrowRightIcon,
  PlayIcon
} from '../components/common/Icons';
import {
  getInterviewSessionById,
  startInterviewSession,
  getCurrentQuestion,
  submitQuestionAnswer,
  getInterviewStatus,
  abandonInterviewSession
} from '../services/interviewService';

const InterviewSessionPage = () => {
  const { id } = useParams();
  const navigate = useNavigate();

  const [session, setSession] = useState(null);
  const [currentQ, setCurrentQ] = useState(null);
  const [status, setStatus] = useState(null);
  const [answerText, setAnswerText] = useState('');
  
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState('');
  const [aiError, setAiError] = useState('');
  const [validationError, setValidationError] = useState('');
  const [exiting, setExiting] = useState(false);
  const [latestEvaluation, setLatestEvaluation] = useState(null);
  const [completedDetails, setCompletedDetails] = useState(null);
  const [answerTab, setAnswerTab] = useState('TEXT');

  useEffect(() => {
    initSession();
  }, [id]);

  const initSession = async () => {
    setLoading(true);
    setError('');
    setAiError('');
    try {
      const sessData = await getInterviewSessionById(id);
      setSession(sessData);
      if (sessData.interactionMode === 'VOICE') {
        setAnswerTab('VOICE');
      }

      if (sessData.status === 'CONFIGURED') {
        const startedSess = await startInterviewSession(id);
        setSession(startedSess);
        if (startedSess.interactionMode === 'VOICE') {
          setAnswerTab('VOICE');
        }
      }

      if (sessData.status === 'COMPLETED') {
        setCompletedDetails(sessData);
        setLoading(false);
        return;
      }

      await loadCurrentState();
    } catch (err) {
      console.error('Failed to initialize interview session:', err);
      if (err.response?.status === 503) {
        setAiError(err.response?.data?.message || 'The AI interviewer service is currently unconfigured. Set AI_API_KEY in environment configuration or use Question Bank mode.');
      } else if (err.response?.status === 404) {
        setError('Interview session not found or you do not have permission to access it.');
      } else {
        setError(err.response?.data?.message || 'Failed to initialize interview session.');
      }
    } finally {
      setLoading(false);
    }
  };

  const loadCurrentState = async () => {
    setAiError('');
    try {
      const qData = await getCurrentQuestion(id);
      setCurrentQ(qData);

      const statusData = await getInterviewStatus(id);
      setStatus(statusData);

      if (statusData.status === 'COMPLETED' || qData.answered) {
        const updatedDetails = await getInterviewSessionById(id);
        setCompletedDetails(updatedDetails);
      }
    } catch (err) {
      console.error('Failed to load current interview state:', err);
      if (err.response?.status === 503) {
        setAiError(err.response?.data?.message || 'The AI interviewer service is unconfigured. Set AI_API_KEY in environment variables.');
      } else {
        setError(err.response?.data?.message || 'Failed to load question.');
      }
    }
  };

  const handleVoiceTranscriptSubmitted = async (transcriptText) => {
    if (!transcriptText || !transcriptText.trim()) return;
    setAnswerText(transcriptText);
    await submitAnswerInternal(transcriptText.trim(), 'VOICE');
  };

  const handleSubmitAnswer = async (e) => {
    e.preventDefault();
    await submitAnswerInternal(answerText, 'TEXT');
  };

  const submitAnswerInternal = async (rawAnswerText, inputMethod = 'TEXT') => {
    setValidationError('');
    setAiError('');

    const trimmedAnswer = rawAnswerText.trim();
    if (!trimmedAnswer) {
      setValidationError('Please formulate and enter your technical response before submitting.');
      return;
    }

    if (trimmedAnswer.length > 10000) {
      setValidationError('Answer exceeds maximum length of 10,000 characters.');
      return;
    }

    if (!currentQ) return;

    setSubmitting(true);
    try {
      const resp = await submitQuestionAnswer(id, currentQ.id, trimmedAnswer);
      setAnswerText('');

      if (resp.evaluation) {
        setLatestEvaluation(resp.evaluation);
      }

      if (resp.completed) {
        const fullDetails = await getInterviewSessionById(id);
        setSession(fullDetails);
        setCompletedDetails(fullDetails);
      } else {
        await loadCurrentState();
      }
    } catch (err) {
      console.error('Error submitting answer:', err);
      if (err.response?.status === 503) {
        setAiError(err.response?.data?.message || 'AI service unavailable for evaluation.');
      } else {
        setValidationError(err.response?.data?.message || 'Failed to submit answer. Please try again.');
      }
    } finally {
      setSubmitting(false);
    }
  };

  const handleExitInterview = async () => {
    setExiting(true);
    try {
      if (session && session.status !== 'COMPLETED' && session.status !== 'ABANDONED') {
        await abandonInterviewSession(id);
      }
      navigate('/dashboard');
    } catch (err) {
      console.error('Error exiting interview session:', err);
      navigate('/dashboard');
    } finally {
      setExiting(false);
    }
  };

  const isCompleted = completedDetails || session?.status === 'COMPLETED' || status?.status === 'COMPLETED';

  const progressPercent = status?.progress || currentQ?.progress || (currentQ?.questionOrder && session?.questionCount ? Math.round((currentQ.questionOrder / session.questionCount) * 100) : 0);

  return (
    <MainLayout>
      <div style={{ maxWidth: '920px', margin: '0 auto', paddingBottom: '3.5rem' }}>
        
        {loading ? (
          <div className="card" style={{ textAlign: 'center', padding: '4rem 1.5rem' }}>
            <div className="ai-pulse-dot" style={{ width: '16px', height: '16px', margin: '0 auto 1.5rem auto' }} />
            <h2 style={{ fontSize: '1.15rem', color: 'var(--text-primary)', marginBottom: '0.35rem' }}>
              Initializing Technical Interview Cockpit...
            </h2>
            <p style={{ color: 'var(--text-muted)', fontSize: '0.8125rem', margin: 0 }}>
              Connecting to adaptive evaluator and compiling question framework.
            </p>
          </div>
        ) : error ? (
          <div className="alert alert-danger" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <div>{error}</div>
            <button onClick={() => navigate('/dashboard')} className="btn btn-secondary btn-sm">
              Return to dashboard
            </button>
          </div>
        ) : aiError ? (
          <div className="card" style={{ padding: '2.5rem', textAlign: 'center' }}>
            <div className="ai-pulse-dot" style={{ background: 'var(--warning)', width: '14px', height: '14px', margin: '0 auto 1rem auto' }} />
            <h2 style={{ color: 'var(--warning)', fontSize: '1.25rem', margin: '0 0 0.5rem 0' }}>AI Interviewer Unavailable</h2>
            <p style={{ color: 'var(--text-secondary)', fontSize: '0.875rem', maxWidth: '540px', margin: '0 auto 1.5rem auto', lineHeight: 1.5 }}>
              {aiError}
            </p>
            <div style={{ display: 'flex', justifyContent: 'center', gap: '0.75rem' }}>
              <button onClick={() => navigate('/interview/setup')} className="btn btn-primary btn-sm">
                Configure Question Bank Mode
              </button>
              <button onClick={() => navigate('/dashboard')} className="btn btn-secondary btn-sm">
                Return to Dashboard
              </button>
            </div>
          </div>
        ) : isCompleted ? (
          /* ====================================================================
             COMPLETION STAGE
             ==================================================================== */
          <div className="card card-hero" style={{ padding: '2.5rem', borderRadius: 'var(--radius-lg)' }}>
            <div style={{ paddingBottom: '1.5rem', marginBottom: '1.5rem', borderBottom: '1px solid var(--border)' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '1rem' }}>
                <div>
                  <div style={{ display: 'inline-flex', alignItems: 'center', gap: '0.4rem', padding: '0.2rem 0.65rem', borderRadius: 'var(--radius-full)', background: 'var(--success-bg)', border: '1px solid var(--success-border)', marginBottom: '0.5rem' }}>
                    <CheckIcon size={14} color="var(--success)" />
                    <span style={{ fontSize: '0.75rem', fontWeight: 600, color: 'var(--success)', textTransform: 'uppercase' }}>Simulation Completed</span>
                  </div>
                  <h1 style={{ fontSize: '1.75rem', fontWeight: 800, color: '#ffffff', margin: 0, letterSpacing: '-0.02em' }}>
                    {completedDetails?.categoryName || session?.categoryName} Assessment
                  </h1>
                </div>

                <div style={{ display: 'flex', gap: '0.65rem' }}>
                  <button onClick={() => navigate(`/interview/report/${id}`)} className="btn btn-primary btn-lg">
                    <span>View Performance Dossier</span>
                    <ArrowRightIcon size={15} />
                  </button>
                  <button onClick={() => navigate('/dashboard')} className="btn btn-secondary btn-lg">
                    Dashboard
                  </button>
                </div>
              </div>
            </div>

            {/* Quick Metrics Strip */}
            <div style={{
              display: 'grid',
              gridTemplateColumns: 'repeat(auto-fit, minmax(140px, 1fr))',
              gap: '1rem',
              marginBottom: '2rem',
              background: 'rgba(15, 23, 42, 0.65)',
              padding: '1.25rem',
              borderRadius: 'var(--radius-md)',
              border: '1px solid var(--border)'
            }}>
              <div>
                <div style={{ fontSize: '0.725rem', color: 'var(--text-muted)', textTransform: 'uppercase', letterSpacing: '0.04em' }}>Track</div>
                <div style={{ fontWeight: 600, fontSize: '0.95rem', color: '#ffffff', marginTop: '0.2rem' }}>
                  {session?.mode}
                </div>
              </div>
              <div>
                <div style={{ fontSize: '0.725rem', color: 'var(--text-muted)', textTransform: 'uppercase', letterSpacing: '0.04em' }}>Target Depth</div>
                <div style={{ fontWeight: 600, fontSize: '0.95rem', color: 'var(--accent-cyan)', marginTop: '0.2rem' }}>
                  {completedDetails?.difficulty || session?.difficulty}
                </div>
              </div>
              <div>
                <div style={{ fontSize: '0.725rem', color: 'var(--text-muted)', textTransform: 'uppercase', letterSpacing: '0.04em' }}>Interface</div>
                <div style={{ fontWeight: 600, fontSize: '0.95rem', color: '#ffffff', marginTop: '0.2rem' }}>
                  {session?.interactionMode === 'VOICE' ? 'Voice Audio' : 'Code / Text Editor'}
                </div>
              </div>
              <div>
                <div style={{ fontSize: '0.725rem', color: 'var(--text-muted)', textTransform: 'uppercase', letterSpacing: '0.04em' }}>Prompts Completed</div>
                <div style={{ fontWeight: 600, fontSize: '0.95rem', color: 'var(--success)', marginTop: '0.2rem' }}>
                  {completedDetails?.questions?.length || session?.questionCount} of {session?.questionCount}
                </div>
              </div>
            </div>

            {/* Complete Transcript with Evaluator Feedback */}
            <h2 style={{ fontSize: '1.15rem', fontWeight: 600, color: '#ffffff', marginBottom: '1rem' }}>
              Interview Question & Response Transcript
            </h2>

            {completedDetails?.questions && completedDetails.questions.length > 0 ? (
              <div style={{ display: 'flex', flexDirection: 'column', gap: '1.25rem' }}>
                {completedDetails.questions.map((q, idx) => (
                  <div key={q.id || idx} className="card" style={{ padding: '1.25rem' }}>
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.625rem', flexWrap: 'wrap', gap: '0.5rem' }}>
                      <span style={{ fontSize: '0.8125rem', fontWeight: 600, color: 'var(--text-accent)' }}>
                        Question {q.questionOrder} · {q.questionType}
                      </span>
                      {q.evaluation?.score != null && (
                        <span className="badge badge-primary" style={{ fontSize: '0.8125rem' }}>
                          Score: {q.evaluation.score}/10
                        </span>
                      )}
                    </div>

                    <p style={{ fontWeight: 600, fontSize: '1rem', color: 'var(--text-primary)', margin: '0 0 0.875rem 0', lineHeight: 1.5 }}>
                      {q.questionText}
                    </p>

                    <div style={{
                      background: 'var(--surface-raised)',
                      padding: '0.875rem 1rem',
                      borderRadius: 'var(--radius-sm)',
                      border: '1px solid var(--border)',
                      fontSize: '0.875rem',
                      whiteSpace: 'pre-wrap',
                      color: 'var(--text-primary)',
                      marginBottom: q.evaluation ? '0.875rem' : 0
                    }}>
                      <div style={{ fontSize: '0.75rem', fontWeight: 600, color: 'var(--text-muted)', textTransform: 'uppercase', marginBottom: '0.25rem' }}>
                        Your Submitted Response
                      </div>
                      {q.answerText || 'No answer submitted.'}
                    </div>

                    {q.evaluation && (
                      <div style={{
                        background: 'rgba(15, 23, 42, 0.5)',
                        padding: '1rem',
                        borderRadius: 'var(--radius-sm)',
                        border: '1px solid var(--border)',
                        fontSize: '0.8125rem'
                      }}>
                        <p style={{ fontSize: '0.875rem', color: 'var(--text-primary)', margin: '0 0 0.35rem 0', lineHeight: 1.5 }}>
                          <strong style={{ color: 'var(--text-accent)' }}>AI Evaluator Feedback:</strong> {q.evaluation.feedback}
                        </p>
                        {q.evaluation.improvementSuggestion && (
                          <p style={{ fontSize: '0.8125rem', color: 'var(--text-secondary)', margin: 0 }}>
                            <strong style={{ color: 'var(--warning)' }}>Refinement Suggestion:</strong> {q.evaluation.improvementSuggestion}
                          </p>
                        )}
                      </div>
                    )}
                  </div>
                ))}
              </div>
            ) : null}
          </div>
        ) : (
          /* ====================================================================
             ACTIVE INTERVIEW EXPERIENCE (COCKPIT ROOM)
             ==================================================================== */
          <>
            {/* Top Cockpit Header */}
            <div className="cockpit-header">
              <div style={{ display: 'flex', alignItems: 'center', gap: '0.85rem' }}>
                <button
                  type="button"
                  onClick={handleExitInterview}
                  className="btn btn-secondary btn-sm"
                  disabled={exiting}
                >
                  {exiting ? 'Exiting...' : '← Exit Session'}
                </button>
                <div>
                  <span style={{ fontSize: '0.95rem', fontWeight: 700, color: '#ffffff' }}>
                    {session?.categoryName}
                  </span>
                  <span style={{ fontSize: '0.8125rem', color: 'var(--text-muted)', marginLeft: '0.5rem' }}>
                    · {session?.difficulty}
                  </span>
                </div>
              </div>

              {/* Status and Question Tracker */}
              <div className="cockpit-status-group">
                <div style={{ display: 'flex', alignItems: 'center', gap: '0.45rem', background: 'var(--surface-sunken)', padding: '0.3rem 0.75rem', borderRadius: 'var(--radius-full)', border: '1px solid var(--border)' }}>
                  <span className="ai-pulse-dot" />
                  <span style={{ fontSize: '0.75rem', fontWeight: 600, color: 'var(--text-primary)' }}>
                    Interviewer Live
                  </span>
                </div>
                <div style={{ fontSize: '0.85rem', color: 'var(--text-secondary)', fontWeight: 600 }}>
                  Question {currentQ?.questionOrder || 1} of {currentQ?.totalQuestions || session?.questionCount}
                </div>
              </div>
            </div>

            {/* Glowing Linear Progress Bar */}
            <div style={{
              height: '4px',
              width: '100%',
              backgroundColor: 'var(--surface-raised)',
              borderRadius: '2px',
              overflow: 'hidden',
              marginBottom: '1.75rem'
            }}>
              <div style={{
                height: '100%',
                width: `${progressPercent}%`,
                background: 'linear-gradient(90deg, #6366f1, #38bdf8)',
                boxShadow: '0 0 10px rgba(99, 102, 241, 0.5)',
                transition: 'width 0.4s ease'
              }} />
            </div>

            {/* Adaptive Journey Feedback Pill (from previous turn) */}
            {latestEvaluation && (
              <div className="card" style={{ marginBottom: '1.5rem', padding: '1rem 1.25rem', borderLeft: '3px solid var(--primary)', background: 'var(--surface-raised)' }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.35rem' }}>
                  <span style={{ fontSize: '0.75rem', fontWeight: 600, color: 'var(--text-muted)', textTransform: 'uppercase' }}>
                    Previous Response Evaluation
                  </span>
                  <span style={{ fontSize: '0.8125rem', fontWeight: 700, color: 'var(--text-accent)' }}>
                    Score: {latestEvaluation.score} / 10
                  </span>
                </div>
                {latestEvaluation.userFacingMessage && (
                  <p style={{ margin: '0 0 0.35rem 0', fontSize: '0.875rem', fontWeight: 500, color: '#ffffff' }}>
                    {latestEvaluation.userFacingMessage}
                  </p>
                )}
                <p style={{ margin: 0, fontSize: '0.8125rem', color: 'var(--text-secondary)', lineHeight: 1.4 }}>
                  {latestEvaluation.feedback}
                </p>
              </div>
            )}

            {/* Question Card (Immersive Studio Presentation) */}
            <div className="card card-hero" style={{ padding: '2rem', marginBottom: '1.75rem', borderRadius: 'var(--radius-lg)' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '0.875rem' }}>
                <span className="badge badge-info">{currentQ?.questionType || 'TECHNICAL'}</span>
                <span className="badge badge-secondary">{currentQ?.difficulty || session?.difficulty}</span>
              </div>

              <h2 style={{ fontSize: '1.35rem', fontWeight: 600, color: '#ffffff', lineHeight: 1.5, margin: '0 0 0.75rem 0' }}>
                {currentQ?.questionText}
              </h2>

              <AudioQuestionPlayer
                sessionId={id}
                questionText={currentQ?.questionText}
                autoPlay={session?.interactionMode === 'VOICE'}
              />
            </div>

            {/* Answer Dock */}
            <div className="card" style={{ padding: '1.75rem' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1rem' }}>
                <span style={{ fontSize: '0.875rem', fontWeight: 600, color: 'var(--text-primary)' }}>
                  Your Response
                </span>
                
                <div className="segmented-control">
                  <button
                    type="button"
                    onClick={() => setAnswerTab('TEXT')}
                    className={`segmented-btn ${answerTab === 'TEXT' ? 'active' : ''}`}
                  >
                    Text Input
                  </button>
                  <button
                    type="button"
                    onClick={() => setAnswerTab('VOICE')}
                    className={`segmented-btn ${answerTab === 'VOICE' ? 'active' : ''}`}
                  >
                    Voice Recording
                  </button>
                </div>
              </div>

              {answerTab === 'VOICE' ? (
                <VoiceRecorder
                  sessionId={id}
                  onTranscriptConfirmed={handleVoiceTranscriptSubmitted}
                  disabled={submitting}
                />
              ) : (
                <form onSubmit={handleSubmitAnswer}>
                  <textarea
                    className="answer-textarea-studio"
                    rows={9}
                    placeholder="Provide your complete technical reasoning, architecture trade-offs, and implementation details..."
                    value={answerText}
                    onChange={(e) => setAnswerText(e.target.value)}
                    disabled={submitting}
                    style={{ marginBottom: '0.75rem' }}
                  />

                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1rem' }}>
                    <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>
                      {answerText.length} characters (10,000 max)
                    </span>
                    {validationError && (
                      <span style={{ fontSize: '0.8125rem', color: 'var(--danger)' }}>
                        {validationError}
                      </span>
                    )}
                  </div>

                  <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '0.75rem' }}>
                    <button
                      type="submit"
                      className="btn btn-primary btn-lg"
                      disabled={submitting || !answerText.trim()}
                    >
                      {submitting ? 'Synthesizing & Evaluating...' : 'Submit Technical Answer →'}
                    </button>
                  </div>
                </form>
              )}
            </div>
          </>
        )}
      </div>
    </MainLayout>
  );
};

export default InterviewSessionPage;

