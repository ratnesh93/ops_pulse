import { useCallback, useEffect, useRef, useState } from 'react';
import { fetchChatStatus, sendChatMessage, sendSpeechMessage } from './api';

export default function ChatPanel({ onClose }) {
  const [messages, setMessages] = useState([
    { role: 'assistant', text: 'Hi — ask about OTA, delays, cost, or pending actions. Use the mic for voice input.' },
  ]);
  const [input, setInput] = useState('');
  const [loading, setLoading] = useState(false);
  const [recording, setRecording] = useState(false);
  const [sarvamReady, setSarvamReady] = useState(false);
  const [sarvamHint, setSarvamHint] = useState('');
  const messagesEndRef = useRef(null);
  const mediaRecorderRef = useRef(null);
  const chunksRef = useRef([]);

  useEffect(() => {
    fetchChatStatus()
      .then((s) => {
        setSarvamReady(s.sarvamConfigured);
        setSarvamHint(s.hint || '');
      })
      .catch(() => setSarvamHint('Chat status unavailable'));
  }, []);

  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages]);

  const appendMessage = useCallback((role, text, transcript) => {
    setMessages((prev) => {
      const next = [...prev, { role, text }];
      if (transcript) {
        next.splice(next.length - 1, 0, { role: 'user', text: transcript, isTranscript: true });
      }
      return next;
    });
  }, []);

  async function handleSend(e) {
    e?.preventDefault();
    const text = input.trim();
    if (!text || loading) return;

    setInput('');
    setMessages((prev) => [...prev, { role: 'user', text }]);
    setLoading(true);
    try {
      const data = await sendChatMessage(text);
      setMessages((prev) => [...prev, { role: 'assistant', text: data.reply }]);
    } catch (err) {
      setMessages((prev) => [...prev, { role: 'assistant', text: `Error: ${err.message}`, error: true }]);
    } finally {
      setLoading(false);
    }
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
        setLoading(true);
        try {
          const data = await sendSpeechMessage(blob);
          setMessages((prev) => {
            const next = [...prev];
            if (data.transcript) {
              next.push({ role: 'user', text: data.transcript, isTranscript: true });
            }
            next.push({ role: 'assistant', text: data.reply });
            return next;
          });
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

      <form className="chat-input-row" onSubmit={handleSend}>
        <input
          type="text"
          value={input}
          onChange={(e) => setInput(e.target.value)}
          placeholder="Ask about OTA, delays, cost…"
          disabled={loading}
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
        <button type="submit" className="primary" disabled={loading || !input.trim()}>
          Send
        </button>
      </form>
    </section>
  );
}
