import { useCallback, useEffect, useRef, useState } from 'react';
import { fetchChatStatus, sendChatMessage, transcribeSpeech } from './api';

export default function ChatPanel({ onClose }) {
  const [messages, setMessages] = useState([
    { role: 'assistant', text: 'Hi — ask about OTA, safety alerts, cost, vendors, or fleet summary. Use the mic for voice input.' },
  ]);
  const [input, setInput] = useState('');
  const [loading, setLoading] = useState(false);
  const [recording, setRecording] = useState(false);
  const [voicePreview, setVoicePreview] = useState(false);
  const [sarvamReady, setSarvamReady] = useState(false);
  const [openAiReady, setOpenAiReady] = useState(false);
  const [sarvamHint, setSarvamHint] = useState('');
  const messagesEndRef = useRef(null);
  const mediaRecorderRef = useRef(null);
  const chunksRef = useRef([]);

  useEffect(() => {
    fetchChatStatus()
      .then((s) => {
        setSarvamReady(s.sarvamConfigured);
        setOpenAiReady(s.openAiConfigured);
        setSarvamHint(s.hint || '');
      })
      .catch(() => setSarvamHint('Chat status unavailable'));
  }, []);

  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages]);

  const sendText = useCallback(async (text, { fromVoice = false } = {}) => {
    const trimmed = text.trim();
    if (!trimmed || loading) return;

    setInput('');
    setVoicePreview(false);
    setMessages((prev) => {
      const next = [...prev];
      if (fromVoice) {
        next.push({ role: 'user', text: trimmed, isTranscript: true });
      } else {
        next.push({ role: 'user', text: trimmed });
      }
      return next;
    });
    setLoading(true);
    try {
      const data = await sendChatMessage(trimmed);
      setMessages((prev) => [...prev, { role: 'assistant', text: data.reply }]);
    } catch (err) {
      setMessages((prev) => [...prev, { role: 'assistant', text: `Error: ${err.message}`, error: true }]);
    } finally {
      setLoading(false);
    }
  }, [loading]);

  async function handleSend(e) {
    e?.preventDefault();
    await sendText(input, { fromVoice: voicePreview });
  }

  function handleCancelVoice() {
    setVoicePreview(false);
    setInput('');
  }

  async function handleMic() {
    if (recording) {
      mediaRecorderRef.current?.stop();
      return;
    }

    if (!sarvamReady) {
      setMessages((prev) => [
        ...prev,
        {
          role: 'assistant',
          text: 'Speech input needs a Sarvam API key. Copy opspulse/.env.example to .env, add your key, then restart: docker compose up -d backend',
          error: true,
        },
      ]);
      return;
    }

    try {
      setVoicePreview(false);
      const stream = await navigator.mediaDevices.getUserMedia({ audio: true });
      const recorder = new MediaRecorder(stream);
      chunksRef.current = [];
      recorder.ondataavailable = (ev) => {
        if (ev.data.size > 0) chunksRef.current.push(ev.data);
      };
      recorder.onstop = async () => {
        stream.getTracks().forEach((t) => t.stop());
        setRecording(false);
        const blob = new Blob(chunksRef.current, { type: recorder.mimeType || 'audio/webm' });
        if (blob.size === 0) {
          return;
        }
        setLoading(true);
        try {
          const data = await transcribeSpeech(blob);
          const transcript = (data.transcript || '').trim();
          if (!transcript) {
            setMessages((prev) => [
              ...prev,
              { role: 'assistant', text: 'Could not detect speech. Try again or type your message.', error: true },
            ]);
            return;
          }
          setInput(transcript);
          setVoicePreview(true);
        } catch (err) {
          setMessages((prev) => [...prev, { role: 'assistant', text: `Speech error: ${err.message}`, error: true }]);
        } finally {
          setLoading(false);
        }
      };
      mediaRecorderRef.current = recorder;
      recorder.start();
      setRecording(true);
    } catch (err) {
      setMessages((prev) => [
        ...prev,
        { role: 'assistant', text: `Microphone access denied: ${err.message}`, error: true },
      ]);
    }
  }

  return (
    <section className="chat-panel">
      <div className="chat-header">
        <h2>Ops Assistant</h2>
        <div style={{ display: 'flex', gap: '0.5rem', alignItems: 'center' }}>
          <span className={`chat-status ${sarvamReady ? 'ready' : 'pending'}`}>
            {sarvamReady ? 'Sarvam STT ready' : 'STT: add API key'}
          </span>
          {openAiReady && <span className="chat-status ready">OpenAI chat</span>}
          {onClose && (
            <button type="button" className="chat-close" onClick={onClose} aria-label="Close chat">
              ✕
            </button>
          )}
        </div>
      </div>

      {!sarvamReady && sarvamHint && (
        <p className="chat-hint">{sarvamHint}</p>
      )}

      <div className="chat-messages">
        {messages.map((m, i) => (
          <div key={i} className={`chat-bubble ${m.role} ${m.error ? 'error' : ''} ${m.isTranscript ? 'transcript' : ''}`}>
            {m.isTranscript && <span className="transcript-label">🎤 </span>}
            {m.text}
          </div>
        ))}
        {loading && <div className="chat-bubble assistant">Thinking…</div>}
        <div ref={messagesEndRef} />
      </div>

      {voicePreview && (
        <div className="voice-review-bar">
          <div className="voice-review-text">
            <span className="transcript-label">🎤 Voice transcript</span>
            <p>Review or edit below, then send or cancel.</p>
          </div>
          <div className="voice-review-actions">
            <button
              type="button"
              className="primary"
              onClick={() => sendText(input, { fromVoice: true })}
              disabled={loading || !input.trim()}
            >
              Send
            </button>
            <button
              type="button"
              className="voice-cancel-btn"
              onClick={handleCancelVoice}
              disabled={loading}
            >
              Cancel
            </button>
          </div>
        </div>
      )}

      <form className="chat-input-row" onSubmit={handleSend}>
        <input
          type="text"
          value={input}
          onChange={(e) => setInput(e.target.value)}
          placeholder={recording ? 'Recording…' : voicePreview ? 'Edit voice transcript…' : 'Ask about OTA, delays, cost…'}
          disabled={loading || recording}
        />
        <button
          type="button"
          className={`mic-btn ${recording ? 'recording' : ''}`}
          onClick={handleMic}
          disabled={loading}
          title={sarvamReady ? 'Voice input (Sarvam)' : 'Configure SARVAM_API_KEY first'}
        >
          {recording ? '⏹' : '🎤'}
        </button>
        <button type="submit" className="primary" disabled={loading || recording || !input.trim()}>
          Send
        </button>
      </form>
    </section>
  );
}
