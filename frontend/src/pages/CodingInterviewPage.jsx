import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { codingService } from '../services/codingService';
import { TerminalIcon, CodeIcon, PlayIcon, CheckIcon, WarningIcon, ArrowRightIcon } from '../components/common/Icons';

const CodingInterviewPage = () => {
  const { sessionId } = useParams();
  const navigate = useNavigate();

  const [problem, setProblem] = useState(null);
  const [questionId, setQuestionId] = useState(null);
  const [language, setLanguage] = useState('JAVA');
  const [code, setCode] = useState('');
  const [activeTab, setActiveTab] = useState('description');
  const [isExecuting, setIsExecuting] = useState(false);
  const [executionResult, setExecutionResult] = useState(null);
  const [submitResult, setSubmitResult] = useState(null);
  const [errorMessage, setErrorMessage] = useState('');

  useEffect(() => {
    fetchProblemDetails();
  }, [sessionId]);

  const fetchProblemDetails = async () => {
    try {
      const data = await codingService.getProblemById(1);
      setProblem(data);
      setQuestionId(1);
      setCode(data.starterCodeJava || '');
    } catch (err) {
      setErrorMessage(err.response?.data?.message || 'Failed to load coding problem.');
    }
  };

  const handleLanguageChange = (e) => {
    const selectedLang = e.target.value;
    setLanguage(selectedLang);
    if (problem) {
      if (selectedLang === 'JAVA') {
        setCode(problem.starterCodeJava || '');
      } else if (selectedLang === 'PYTHON') {
        setCode(problem.starterCodePython || '');
      }
    }
  };

  const handleRunCode = async () => {
    if (!code.trim()) return;
    setIsExecuting(true);
    setErrorMessage('');
    setExecutionResult(null);
    setActiveTab('testcases');
    try {
      const res = await codingService.runCode(sessionId, questionId, language, code);
      setExecutionResult(res);
    } catch (err) {
      if (err.response?.status === 503) {
        setErrorMessage('Code execution service is temporarily unavailable. Sandbox environment is initializing.');
      } else {
        setErrorMessage(err.response?.data?.message || 'Failed to execute code.');
      }
    } finally {
      setIsExecuting(false);
    }
  };

  const handleSubmitCode = async () => {
    if (!code.trim()) return;
    setIsExecuting(true);
    setErrorMessage('');
    setSubmitResult(null);
    setActiveTab('results');
    try {
      const res = await codingService.submitCode(sessionId, questionId, language, code);
      setSubmitResult(res);
      if (res.isCompleted) {
        setTimeout(() => {
          navigate(`/interview/report/${sessionId}`);
        }, 3000);
      }
    } catch (err) {
      if (err.response?.status === 503) {
        setErrorMessage('Code execution service is temporarily unavailable. Sandbox environment is initializing.');
      } else {
        setErrorMessage(err.response?.data?.message || 'Failed to submit solution.');
      }
    } finally {
      setIsExecuting(false);
    }
  };

  if (!problem) {
    return (
      <div style={{ maxWidth: '600px', margin: '4rem auto', textAlign: 'center' }}>
        <div className="skeleton" style={{ width: '40%', height: '24px', margin: '0 auto 1rem auto' }} />
        <div className="skeleton" style={{ width: '70%', height: '14px', margin: '0 auto' }} />
        <p style={{ color: 'var(--text-muted)', fontSize: '0.875rem', marginTop: '1rem' }}>
          Initializing coding studio sandbox...
        </p>
      </div>
    );
  }

  const getDifficultyBadge = (diff) => {
    switch (diff) {
      case 'EASY':
        return <span className="badge badge-success">Easy</span>;
      case 'MEDIUM':
        return <span className="badge badge-warning">Medium</span>;
      default:
        return <span className="badge badge-danger">Hard</span>;
    }
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', height: '100vh', background: 'var(--bg-primary)', color: 'var(--text-primary)' }}>
      {/* Top IDE Studio Header */}
      <div style={{
        display: 'flex',
        justifyContent: 'space-between',
        alignItems: 'center',
        padding: '0.65rem 1.25rem',
        background: 'var(--surface-sunken)',
        borderBottom: '1px solid var(--border-editor)',
        zIndex: 10
      }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.85rem' }}>
          <button
            type="button"
            onClick={() => navigate('/interview/history')}
            className="btn btn-secondary btn-sm"
            style={{ padding: '0.25rem 0.6rem' }}
          >
            ← Exit IDE
          </button>
          <div style={{ height: '16px', width: '1px', background: 'var(--border)' }} />
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
            <CodeIcon size={16} color="var(--primary)" />
            <h1 style={{ fontSize: '0.95rem', fontWeight: 700, margin: 0, color: '#ffffff' }}>
              {problem.title}
            </h1>
          </div>
          {getDifficultyBadge(problem.difficulty)}
          {problem.dsaCategory && (
            <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)', background: 'var(--surface-raised)', padding: '0.15rem 0.45rem', borderRadius: 'var(--radius-xs)', border: '1px solid var(--border)' }}>
              {problem.dsaCategory}
            </span>
          )}
        </div>

        <div style={{ display: 'flex', alignItems: 'center', gap: '0.625rem' }}>
          <select
            value={language}
            onChange={handleLanguageChange}
            className="form-select"
            style={{ padding: '0.35rem 0.75rem', fontSize: '0.8125rem', width: 'auto', background: 'var(--surface-raised)', borderColor: 'var(--border-editor)' }}
          >
            <option value="JAVA">Java (JDK 17)</option>
            <option value="PYTHON">Python 3</option>
          </select>

          <button
            type="button"
            onClick={handleRunCode}
            disabled={isExecuting}
            className="btn btn-secondary btn-sm"
          >
            <PlayIcon size={13} />
            <span>{isExecuting ? 'Executing...' : 'Run Testcases'}</span>
          </button>

          <button
            type="button"
            onClick={handleSubmitCode}
            disabled={isExecuting}
            className="btn btn-primary btn-sm"
          >
            <CheckIcon size={13} />
            <span>{isExecuting ? 'Grading...' : 'Submit Solution'}</span>
          </button>
        </div>
      </div>

      {/* Main Split Panels */}
      <div style={{ display: 'flex', flex: 1, overflow: 'hidden' }}>
        {/* Left: Problem & Tabs */}
        <div style={{
          width: '45%',
          borderRight: '1px solid var(--border-editor)',
          display: 'flex',
          flexDirection: 'column',
          background: 'var(--surface-card)'
        }}>
          {/* Tabs */}
          <div style={{
            display: 'flex',
            borderBottom: '1px solid var(--border-editor)',
            background: 'var(--surface-sunken)',
            padding: '0 0.5rem'
          }}>
            <button
              type="button"
              onClick={() => setActiveTab('description')}
              style={{
                padding: '0.65rem 1rem',
                background: 'none',
                border: 'none',
                borderBottom: activeTab === 'description' ? '2px solid var(--primary)' : '2px solid transparent',
                color: activeTab === 'description' ? 'var(--text-primary)' : 'var(--text-muted)',
                fontWeight: activeTab === 'description' ? 600 : 400,
                fontSize: '0.8125rem',
                cursor: 'pointer'
              }}
            >
              Problem Statement
            </button>
            <button
              type="button"
              onClick={() => setActiveTab('testcases')}
              style={{
                padding: '0.65rem 1rem',
                background: 'none',
                border: 'none',
                borderBottom: activeTab === 'testcases' ? '2px solid var(--primary)' : '2px solid transparent',
                color: activeTab === 'testcases' ? 'var(--text-primary)' : 'var(--text-muted)',
                fontWeight: activeTab === 'testcases' ? 600 : 400,
                fontSize: '0.8125rem',
                cursor: 'pointer'
              }}
            >
              Test Cases {executionResult && `(${executionResult.passedCount}/${executionResult.totalCount})`}
            </button>
            {submitResult && (
              <button
                type="button"
                onClick={() => setActiveTab('results')}
                style={{
                  padding: '0.65rem 1rem',
                  background: 'none',
                  border: 'none',
                  borderBottom: activeTab === 'results' ? '2px solid var(--primary)' : '2px solid transparent',
                  color: activeTab === 'results' ? 'var(--text-primary)' : 'var(--text-muted)',
                  fontWeight: activeTab === 'results' ? 600 : 400,
                  fontSize: '0.8125rem',
                  cursor: 'pointer'
                }}
              >
                Evaluation Dossier
              </button>
            )}
          </div>

          {/* Panel Body */}
          <div style={{ flex: 1, padding: '1.25rem', overflowY: 'auto' }}>
            {errorMessage && (
              <div className="alert alert-danger" style={{ marginBottom: '1rem' }}>
                {errorMessage}
              </div>
            )}

            {activeTab === 'description' && (
              <div>
                <p style={{ lineHeight: 1.6, fontSize: '0.875rem', color: 'var(--text-primary)', whiteSpace: 'pre-line', margin: '0 0 1.25rem 0' }}>
                  {problem.description}
                </p>

                {problem.constraintsText && (
                  <div style={{ marginBottom: '1.25rem' }}>
                    <div style={{ fontSize: '0.75rem', fontWeight: 600, color: 'var(--text-muted)', textTransform: 'uppercase', marginBottom: '0.35rem', letterSpacing: '0.04em' }}>
                      Constraints
                    </div>
                    <pre style={{
                      background: 'var(--surface-sunken)',
                      padding: '0.75rem',
                      borderRadius: 'var(--radius-sm)',
                      fontSize: '0.8125rem',
                      color: 'var(--text-secondary)',
                      border: '1px solid var(--border-editor)',
                      fontFamily: 'var(--font-mono)'
                    }}>
                      {problem.constraintsText}
                    </pre>
                  </div>
                )}

                <div>
                  <div style={{ fontSize: '0.75rem', fontWeight: 600, color: 'var(--text-muted)', textTransform: 'uppercase', marginBottom: '0.5rem', letterSpacing: '0.04em' }}>
                    Sample Test Scenarios
                  </div>
                  {problem.visibleTestCases && problem.visibleTestCases.map((tc, idx) => (
                    <div key={tc.id || idx} style={{
                      marginBottom: '0.75rem',
                      background: 'var(--surface-sunken)',
                      padding: '0.85rem 1rem',
                      borderRadius: 'var(--radius-sm)',
                      border: '1px solid var(--border-editor)',
                      fontSize: '0.8125rem'
                    }}>
                      <div style={{ fontWeight: 600, color: '#ffffff', marginBottom: '0.35rem' }}>
                        Example {idx + 1}
                      </div>
                      <div style={{ marginBottom: '0.2rem' }}>
                        <span style={{ color: 'var(--text-muted)' }}>Input: </span>
                        <code style={{ fontFamily: 'var(--font-mono)', background: 'var(--surface-raised)', padding: '0.1rem 0.35rem', borderRadius: '3px' }}>
                          {tc.inputData}
                        </code>
                      </div>
                      <div style={{ marginBottom: tc.explanation ? '0.2rem' : 0 }}>
                        <span style={{ color: 'var(--text-muted)' }}>Output: </span>
                        <code style={{ fontFamily: 'var(--font-mono)', background: 'var(--surface-raised)', padding: '0.1rem 0.35rem', borderRadius: '3px' }}>
                          {tc.expectedOutput}
                        </code>
                      </div>
                      {tc.explanation && (
                        <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', marginTop: '0.25rem' }}>
                          Explanation: {tc.explanation}
                        </div>
                      )}
                    </div>
                  ))}
                </div>
              </div>
            )}

            {activeTab === 'testcases' && (
              <div>
                {!executionResult ? (
                  <div className="empty-state" style={{ padding: '2rem 1rem' }}>
                    <p>Click "Run Testcases" to execute code against sample tests.</p>
                  </div>
                ) : (
                  <div>
                    <div style={{
                      padding: '0.85rem 1rem',
                      borderRadius: 'var(--radius-sm)',
                      background: executionResult.status === 'PASSED' ? 'var(--success-bg)' : 'var(--danger-bg)',
                      border: `1px solid ${executionResult.status === 'PASSED' ? 'rgba(16, 185, 129, 0.3)' : 'rgba(239, 68, 68, 0.3)'}`,
                      marginBottom: '1rem'
                    }}>
                      <div style={{
                        fontWeight: 600,
                        fontSize: '0.875rem',
                        color: executionResult.status === 'PASSED' ? 'var(--success)' : 'var(--danger)'
                      }}>
                        Status: {executionResult.status} ({executionResult.passedCount} of {executionResult.totalCount} passed)
                      </div>
                      {executionResult.executionTimeMs != null && (
                        <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', marginTop: '0.2rem' }}>
                          Execution latency: {executionResult.executionTimeMs}ms
                        </div>
                      )}
                    </div>

                    {executionResult.compileOutput && (
                      <pre style={{
                        background: 'var(--surface-sunken)',
                        color: 'var(--danger)',
                        padding: '0.75rem',
                        borderRadius: 'var(--radius-sm)',
                        fontSize: '0.75rem',
                        fontFamily: 'var(--font-mono)',
                        border: '1px solid var(--border-editor)',
                        marginBottom: '1rem'
                      }}>
                        {executionResult.compileOutput}
                      </pre>
                    )}

                    {executionResult.testResults && executionResult.testResults.map((tr, idx) => (
                      <div key={idx} style={{
                        padding: '0.75rem',
                        background: 'var(--surface-sunken)',
                        borderRadius: 'var(--radius-sm)',
                        marginBottom: '0.625rem',
                        border: '1px solid var(--border-editor)',
                        borderLeft: `3px solid ${tr.passed ? 'var(--success)' : 'var(--danger)'}`,
                        fontSize: '0.8125rem'
                      }}>
                        <div style={{ fontWeight: 600, color: tr.passed ? 'var(--success)' : 'var(--danger)', marginBottom: '0.35rem' }}>
                          Test Case {idx + 1}: {tr.passed ? 'Passed' : 'Failed'}
                        </div>
                        <div style={{ color: 'var(--text-muted)', fontSize: '0.75rem' }}>Input: {tr.inputData}</div>
                        <div style={{ color: 'var(--text-muted)', fontSize: '0.75rem' }}>Expected: {tr.expectedOutput}</div>
                        <div style={{ color: 'var(--text-muted)', fontSize: '0.75rem' }}>Actual: {tr.actualOutput}</div>
                        {tr.errorMessage && (
                          <div style={{ color: 'var(--danger)', fontSize: '0.75rem', marginTop: '0.25rem' }}>
                            {tr.errorMessage}
                          </div>
                        )}
                      </div>
                    ))}
                  </div>
                )}
              </div>
            )}

            {activeTab === 'results' && submitResult && (
              <div>
                <div style={{
                  padding: '1rem',
                  borderRadius: 'var(--radius-sm)',
                  background: 'var(--surface-sunken)',
                  border: '1px solid var(--border-editor)',
                  marginBottom: '1rem'
                }}>
                  <div style={{ fontSize: '0.875rem', fontWeight: 600, color: '#ffffff', marginBottom: '0.5rem' }}>
                    Complexity Evaluation
                  </div>
                  <div style={{ display: 'flex', gap: '1.5rem', fontSize: '0.8125rem' }}>
                    <div>Time Complexity: <strong style={{ color: 'var(--accent-cyan)' }}>{submitResult.evaluation?.timeComplexity || 'N/A'}</strong></div>
                    <div>Space Complexity: <strong style={{ color: 'var(--accent-cyan)' }}>{submitResult.evaluation?.spaceComplexity || 'N/A'}</strong></div>
                  </div>
                </div>

                {submitResult.evaluation?.strengths && (
                  <div style={{ marginBottom: '1rem' }}>
                    <div style={{ fontSize: '0.75rem', fontWeight: 600, color: 'var(--text-muted)', textTransform: 'uppercase', marginBottom: '0.35rem' }}>
                      Strengths
                    </div>
                    <ul style={{ margin: 0, paddingLeft: '1.125rem', fontSize: '0.8125rem', color: 'var(--text-primary)' }}>
                      {submitResult.evaluation.strengths.map((s, i) => <li key={i}>{s}</li>)}
                    </ul>
                  </div>
                )}

                {submitResult.evaluation?.weaknesses && (
                  <div style={{ marginBottom: '1rem' }}>
                    <div style={{ fontSize: '0.75rem', fontWeight: 600, color: 'var(--text-muted)', textTransform: 'uppercase', marginBottom: '0.35rem' }}>
                      Optimization Suggestions
                    </div>
                    <ul style={{ margin: 0, paddingLeft: '1.125rem', fontSize: '0.8125rem', color: 'var(--text-primary)' }}>
                      {submitResult.evaluation.weaknesses.map((w, i) => <li key={i}>{w}</li>)}
                    </ul>
                  </div>
                )}

                {submitResult.evaluation?.overallFeedback && (
                  <div>
                    <div style={{ fontSize: '0.75rem', fontWeight: 600, color: 'var(--text-muted)', textTransform: 'uppercase', marginBottom: '0.35rem' }}>
                      Detailed Feedback
                    </div>
                    <p style={{ lineHeight: 1.5, fontSize: '0.8125rem', color: 'var(--text-primary)', margin: 0 }}>
                      {submitResult.evaluation.overallFeedback}
                    </p>
                  </div>
                )}
              </div>
            )}
          </div>
        </div>

        {/* Right: Code Editor Panel */}
        <div style={{ width: '55%', display: 'flex', flexDirection: 'column', background: 'var(--editor-bg)' }}>
          <div style={{
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'center',
            padding: '0.45rem 1rem',
            background: 'var(--surface-sunken)',
            borderBottom: '1px solid var(--border-editor)',
            fontSize: '0.75rem',
            color: 'var(--text-muted)',
            fontFamily: 'var(--font-mono)'
          }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
              <span className="terminal-dot red" style={{ width: '8px', height: '8px' }} />
              <span className="terminal-dot yellow" style={{ width: '8px', height: '8px' }} />
              <span className="terminal-dot green" style={{ width: '8px', height: '8px' }} />
              <span style={{ color: 'var(--text-secondary)' }}>{language === 'JAVA' ? 'Solution.java' : 'solution.py'}</span>
            </div>
            <span>UTF-8 &bull; LF</span>
          </div>

          <textarea
            value={code}
            onChange={(e) => setCode(e.target.value)}
            placeholder="Write your solution here..."
            spellCheck="false"
            style={{
              flex: 1,
              width: '100%',
              padding: '1.25rem',
              backgroundColor: 'var(--editor-bg)',
              color: '#f8fafc',
              fontFamily: 'var(--font-mono)',
              fontSize: '0.875rem',
              lineHeight: 1.6,
              border: 'none',
              outline: 'none',
              resize: 'none'
            }}
          />
        </div>
      </div>
    </div>
  );
};

export default CodingInterviewPage;

