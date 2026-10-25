import React, { useState, useEffect, useRef } from 'react';
import { synthesizeText } from '../services/interviewService';

const AudioQuestionPlayer = ({ sessionId, questionText, autoPlay = false }) => {
  const [loading, setLoading] = useState(false);
  const [isPlaying, setIsPlaying] = useState(false);
  const [error, setError] = useState('');
  const audioRef = useRef(null);
  const objectUrlRef = useRef(null);

  useEffect(() => {
    cleanupAudio();
    setError('');

    if (autoPlay && questionText) {
      handlePlaySpeech();
    }

    return () => {
      cleanupAudio();
    };
  }, [sessionId, questionText]);

  const cleanupAudio = () => {
    if (audioRef.current) {
      audioRef.current.pause();
      audioRef.current.onended = null;
      audioRef.current.onerror = null;
      audioRef.current = null;
    }
    if (objectUrlRef.current) {
      URL.revokeObjectURL(objectUrlRef.current);
      objectUrlRef.current = null;
    }
    setIsPlaying(false);
  };

  const handlePlaySpeech = async () => {
    if (isPlaying) {
      if (audioRef.current) {
        audioRef.current.pause();
      }
      setIsPlaying(false);
      return;
    }

    if (audioRef.current && objectUrlRef.current) {
      try {
        await audioRef.current.play();
        setIsPlaying(true);
      } catch (err) {
        console.error('Audio play error:', err);
        setIsPlaying(false);
      }
      return;
    }

    if (!questionText || !sessionId) return;

    setLoading(true);
    setError('');

    try {
      const audioBlob = await synthesizeText(sessionId, questionText);
      const audioUrl = URL.createObjectURL(audioBlob);
      objectUrlRef.current = audioUrl;

      const audio = new Audio(audioUrl);
      audioRef.current = audio;

      audio.onended = () => {
        setIsPlaying(false);
      };

      audio.onerror = (e) => {
        console.error('Audio element playback error:', e);
        setIsPlaying(false);
        setError('Voice synthesis playback unavailable.');
      };

      await audio.play();
      setIsPlaying(true);
    } catch (err) {
      console.error('Failed to synthesize speech:', err);
      setError('Voice synthesis playback unavailable.');
      setIsPlaying(false);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div style={{ marginTop: '0.875rem' }}>
      <button
        type="button"
        onClick={handlePlaySpeech}
        disabled={loading || !questionText}
        className={`btn ${isPlaying ? 'btn-primary' : 'btn-secondary'} btn-sm`}
        style={{ cursor: loading ? 'wait' : 'pointer' }}
      >
        <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
          <polygon points="11 5 6 9 2 9 2 15 6 15 11 19 11 5" />
          <path d="M15.54 8.46a5 5 0 0 1 0 7.07" />
          <path d="M19.07 4.93a10 10 0 0 1 0 14.14" />
        </svg>

        {loading ? (
          'Synthesizing speech...'
        ) : isPlaying ? (
          <span style={{ display: 'inline-flex', alignItems: 'center', gap: '0.4rem' }}>
            <span>Pause spoken prompt</span>
            <span className="waveform-container" style={{ height: '14px', gap: '2px', padding: 0 }}>
              <span className="waveform-bar active" style={{ width: '2px', background: '#ffffff', height: '12px' }} />
              <span className="waveform-bar active" style={{ width: '2px', background: '#ffffff', height: '16px', animationDelay: '0.2s' }} />
              <span className="waveform-bar active" style={{ width: '2px', background: '#ffffff', height: '8px', animationDelay: '0.4s' }} />
            </span>
          </span>
        ) : (
          'Play spoken question'
        )}
      </button>

      {error && (
        <div style={{ marginTop: '0.35rem', fontSize: '0.75rem', color: 'var(--text-muted)' }}>
          {error}
        </div>
      )}
    </div>
  );
};

export default AudioQuestionPlayer;
