import React, { useState, useEffect, useRef } from 'react';
import { transcribeAudio } from '../services/interviewService';

const VoiceRecorder = ({ sessionId, onTranscriptConfirmed, disabled }) => {
  const [recordingState, setRecordingState] = useState('IDLE'); // IDLE, RECORDING, PROCESSING, TRANSCRIBED, ERROR
  const [timer, setTimer] = useState(0);
  const [transcript, setTranscript] = useState('');
  const [errorMessage, setErrorMessage] = useState('');

  const mediaRecorderRef = useRef(null);
  const mediaStreamRef = useRef(null);
  const audioChunksRef = useRef([]);
  const timerIntervalRef = useRef(null);
  const speechRecognitionRef = useRef(null);

  useEffect(() => {
    return () => {
      stopAndReleaseMediaStream();
      if (timerIntervalRef.current) {
        clearInterval(timerIntervalRef.current);
      }
      if (speechRecognitionRef.current) {
        try {
          speechRecognitionRef.current.stop();
        } catch (e) {
          // ignore
        }
      }
    };
  }, []);

  const stopAndReleaseMediaStream = () => {
    if (mediaStreamRef.current) {
      mediaStreamRef.current.getTracks().forEach((track) => track.stop());
      mediaStreamRef.current = null;
    }
  };

  const formatTimer = (seconds) => {
    const mins = Math.floor(seconds / 60);
    const secs = seconds % 60;
    return `${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`;
  };

  const getSupportedMimeType = () => {
    if (typeof window.MediaRecorder === 'undefined') return '';
    const types = [
      'audio/webm;codecs=opus',
      'audio/webm',
      'audio/ogg;codecs=opus',
      'audio/mp4',
      'audio/wav'
    ];
    for (const type of types) {
      if (MediaRecorder.isTypeSupported(type)) {
        return type;
      }
    }
    return '';
  };

  const startRecording = async () => {
    setErrorMessage('');
    setTranscript('');
    audioChunksRef.current = [];
    setTimer(0);

    const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition;

    try {
      const stream = await navigator.mediaDevices.getUserMedia({ audio: true });
      mediaStreamRef.current = stream;

      const mimeType = getSupportedMimeType();
      const options = mimeType ? { mimeType } : {};
      const mediaRecorder = new MediaRecorder(stream, options);
      mediaRecorderRef.current = mediaRecorder;

      mediaRecorder.ondataavailable = (event) => {
        if (event.data && event.data.size > 0) {
          audioChunksRef.current.push(event.data);
        }
      };

      mediaRecorder.onstop = async () => {
        stopAndReleaseMediaStream();
        if (timerIntervalRef.current) {
          clearInterval(timerIntervalRef.current);
        }

        const audioBlob = new Blob(audioChunksRef.current, { type: mimeType || 'audio/webm' });
        if (audioBlob.size === 0 && !transcript) {
          setRecordingState('ERROR');
          setErrorMessage('No audio captured. Please try speaking again.');
          return;
        }

        await processTranscription(audioBlob);
      };

      if (SpeechRecognition) {
        try {
          const recognition = new SpeechRecognition();
          recognition.continuous = true;
          recognition.interimResults = true;
          recognition.lang = 'en-US';

          let liveText = '';
          recognition.onresult = (event) => {
            let current = '';
            for (let i = event.resultIndex; i < event.results.length; ++i) {
              current += event.results[i][0].transcript;
            }
            if (current) {
              liveText = current;
              setTranscript(liveText);
            }
          };
          recognition.start();
          speechRecognitionRef.current = recognition;
        } catch (srErr) {
          console.warn('Browser SpeechRecognition fallback skipped:', srErr);
        }
      }

      mediaRecorder.start(250);
      setRecordingState('RECORDING');

      timerIntervalRef.current = setInterval(() => {
        setTimer((prev) => {
          if (prev >= 120) {
            stopRecording();
            return 120;
          }
          return prev + 1;
        });
      }, 1000);

    } catch (err) {
      console.error('Microphone access or MediaRecorder error:', err);
      stopAndReleaseMediaStream();
      setRecordingState('ERROR');
      if (err.name === 'NotAllowedError' || err.name === 'PermissionDeniedError') {
        setErrorMessage('Microphone access was denied. You can type your answer instead.');
      } else {
        setErrorMessage('Could not access microphone. Please check your browser audio permissions or type your answer.');
      }
    }
  };

  const stopRecording = () => {
    if (speechRecognitionRef.current) {
      try {
        speechRecognitionRef.current.stop();
      } catch (e) {
        // ignore
      }
    }
    if (mediaRecorderRef.current && mediaRecorderRef.current.state === 'recording') {
      mediaRecorderRef.current.stop();
    }
    setRecordingState('PROCESSING');
  };

  const processTranscription = async (audioBlob) => {
    setRecordingState('PROCESSING');
    try {
      const resp = await transcribeAudio(sessionId, audioBlob);
      const serverTranscript = resp.transcript || '';
      
      const finalTranscript = serverTranscript.trim() || transcript.trim();
      if (!finalTranscript) {
        setRecordingState('ERROR');
        setErrorMessage('Could not transcribe audio into text. Please try recording again or enter text.');
        return;
      }

      setTranscript(finalTranscript);
      setRecordingState('TRANSCRIBED');
    } catch (err) {
      console.warn('Server audio transcription error:', err);
      if (transcript && transcript.trim()) {
        setRecordingState('TRANSCRIBED');
      } else {
        setRecordingState('ERROR');
        setErrorMessage(err.response?.data?.message || 'Failed to process audio recording. Please type your answer or retry.');
      }
    }
  };

  const handleConfirmSubmit = () => {
    if (!transcript || !transcript.trim()) return;
    if (onTranscriptConfirmed) {
      onTranscriptConfirmed(transcript.trim());
    }
  };

  return (
    <div style={{ marginTop: '0.875rem' }}>
      {recordingState === 'IDLE' && (
        <div style={{
          textAlign: 'center',
          padding: '2.5rem 1.5rem',
          background: 'var(--surface-raised)',
          borderRadius: 'var(--radius-md)',
          border: '1px solid var(--border)'
        }}>
          <div style={{
            width: '48px',
            height: '48px',
            borderRadius: '50%',
            background: 'rgba(99, 102, 241, 0.15)',
            border: '1px solid rgba(99, 102, 241, 0.3)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            margin: '0 auto 1rem auto',
            fontSize: '1.25rem',
            color: 'var(--primary)'
          }}>
            <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
              <path d="M12 2a3 3 0 0 0-3 3v7a3 3 0 0 0 6 0V5a3 3 0 0 0-3-3Z" />
              <path d="M19 10v2a7 7 0 0 1-14 0v-2" />
              <line x1="12" x2="12" y1="19" y2="22" />
            </svg>
          </div>
          <div style={{ fontSize: '1rem', fontWeight: 600, color: 'var(--text-primary)', marginBottom: '0.25rem' }}>
            Interactive Voice Capture
          </div>
          <p style={{ color: 'var(--text-muted)', fontSize: '0.8125rem', maxWidth: '380px', margin: '0 auto 1.25rem auto', lineHeight: 1.5 }}>
            Speak naturally into your microphone. Your verbal explanation will be transcribed and evaluated for technical clarity.
          </p>
          <button
            type="button"
            onClick={startRecording}
            disabled={disabled}
            className="btn btn-primary btn-lg"
          >
            Start Voice Recording
          </button>
        </div>
      )}

      {recordingState === 'RECORDING' && (
        <div style={{
          textAlign: 'center',
          padding: '2.5rem 1.5rem',
          background: 'rgba(15, 23, 42, 0.75)',
          borderRadius: 'var(--radius-md)',
          border: '1px solid rgba(244, 63, 94, 0.4)',
          boxShadow: '0 0 24px -4px rgba(244, 63, 94, 0.25)'
        }}>
          {/* Recording Status Header */}
          <div style={{ display: 'inline-flex', alignItems: 'center', gap: '0.5rem', padding: '0.35rem 0.85rem', borderRadius: 'var(--radius-full)', background: 'var(--danger-bg)', border: '1px solid var(--danger-border)', marginBottom: '1.25rem' }}>
            <span style={{ width: '8px', height: '8px', borderRadius: '50%', background: 'var(--danger)', animation: 'pulse-ring 1.5s infinite' }} />
            <span style={{ fontSize: '0.8125rem', fontWeight: 700, color: '#fda4af', letterSpacing: '0.04em' }}>
              RECORDING ACTIVE · {formatTimer(timer)} / 02:00
            </span>
          </div>

          {/* Animated Waveform Visualizer */}
          <div className="waveform-container" style={{ margin: '1rem 0 1.5rem 0', gap: '5px' }}>
            {[18, 28, 12, 34, 22, 16, 30, 26, 14, 32, 20, 24, 10, 28].map((h, i) => (
              <div
                key={i}
                className="waveform-bar active"
                style={{
                  height: `${h}px`,
                  background: 'linear-gradient(180deg, #f43f5e 0%, #6366f1 100%)',
                  animationDelay: `${(i % 5) * 0.15}s`
                }}
              />
            ))}
          </div>

          <p style={{ color: 'var(--text-secondary)', fontSize: '0.8125rem', margin: '0 0 1.25rem 0' }}>
            Microphone listening. Click "Finish Response" when you have concluded your explanation.
          </p>

          <button
            type="button"
            onClick={stopRecording}
            className="btn btn-danger btn-lg"
          >
            Finish Response
          </button>
        </div>
      )}

      {recordingState === 'PROCESSING' && (
        <div style={{
          textAlign: 'center',
          padding: '3rem 1.5rem',
          background: 'var(--surface-raised)',
          borderRadius: 'var(--radius-md)',
          border: '1px solid var(--border)'
        }}>
          <div className="ai-pulse-dot" style={{ width: '16px', height: '16px', margin: '0 auto 1.25rem auto' }} />
          <div style={{ fontSize: '1rem', fontWeight: 600, color: 'var(--text-primary)', marginBottom: '0.35rem' }}>
            Transcribing Spoken Response...
          </div>
          <p style={{ color: 'var(--text-muted)', fontSize: '0.8125rem', margin: 0 }}>
            Converting audio stream into technical response text using AI speech engine.
          </p>
        </div>
      )}

      {recordingState === 'TRANSCRIBED' && (
        <div style={{
          background: 'var(--surface-raised)',
          padding: '1.25rem',
          borderRadius: 'var(--radius-md)',
          border: '1px solid var(--border)'
        }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.75rem' }}>
            <label className="form-label" style={{ margin: 0, fontWeight: 600, color: 'var(--text-primary)' }}>
              Transcribed Answer (Review & Edit Before Submission)
            </label>
            <button
              type="button"
              onClick={startRecording}
              className="btn btn-secondary btn-sm"
            >
              Re-record Voice
            </button>
          </div>

          <textarea
            className="form-textarea"
            rows={6}
            value={transcript}
            onChange={(e) => setTranscript(e.target.value)}
            disabled={disabled}
            placeholder="Edit your transcribed response..."
            style={{ marginBottom: '0.875rem' }}
          />

          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>
              {transcript.length} characters
            </span>
            <button
              type="button"
              onClick={handleConfirmSubmit}
              disabled={disabled || !transcript.trim()}
              className="btn btn-primary"
            >
              Confirm & Submit Answer →
            </button>
          </div>
        </div>
      )}

      {recordingState === 'ERROR' && (
        <div className="alert alert-danger" style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem', alignItems: 'flex-start' }}>
          <div>{errorMessage}</div>
          <button
            type="button"
            onClick={startRecording}
            className="btn btn-secondary btn-sm"
          >
            Retry Recording
          </button>
        </div>
      )}
    </div>
  );
};

export default VoiceRecorder;
