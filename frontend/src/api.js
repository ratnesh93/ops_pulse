const API_BASE = '/api';

export async function fetchBrief() {
  const res = await fetch(`${API_BASE}/brief`);
  if (!res.ok) throw new Error('Failed to load brief');
  return res.json();
}

export async function fetchActivityLog() {
  const res = await fetch(`${API_BASE}/activity-log`);
  if (!res.ok) throw new Error('Failed to load activity log');
  return res.json();
}

export async function fetchMemo() {
  const res = await fetch(`${API_BASE}/leadership/memo`);
  if (!res.ok) throw new Error('Failed to load memo');
  return res.json();
}

export async function fetchAiCosts() {
  const res = await fetch(`${API_BASE}/ai/costs`);
  if (!res.ok) throw new Error('Failed to load AI costs');
  return res.json();
}

export async function fetchMonitoring() {
  const res = await fetch(`${API_BASE}/monitoring`);
  if (!res.ok) throw new Error('Failed to load monitoring');
  return res.json();
}

export async function fetchMonitoringScenarios() {
  const res = await fetch(`${API_BASE}/monitoring/scenarios`);
  if (!res.ok) throw new Error('Failed to load monitoring scenarios');
  return res.json();
}

export async function simulateMonitoringFeed(scenario, vendor = 'rohan') {
  const params = new URLSearchParams({ vendor });
  const res = await fetch(`${API_BASE}/monitoring/simulate/${scenario}?${params}`, { method: 'POST' });
  if (!res.ok) throw new Error('Failed to simulate live feed');
  return res.json();
}

export async function confirmMonitoringAction(id) {
  const res = await fetch(`${API_BASE}/monitoring/actions/${id}/confirm`, { method: 'POST' });
  if (!res.ok) throw new Error('Failed to confirm action');
  return res.json();
}

export async function dismissMonitoringAction(id) {
  const res = await fetch(`${API_BASE}/monitoring/actions/${id}/dismiss`, { method: 'POST' });
  if (!res.ok) throw new Error('Failed to dismiss action');
  return res.json();
}

export async function dismissAction(id) {
  const res = await fetch(`${API_BASE}/actions/${id}/dismiss`, { method: 'POST' });
  if (!res.ok) throw new Error('Failed to dismiss action');
  return res.json();
}

export async function confirmAction(id) {
  const res = await fetch(`${API_BASE}/actions/${id}/confirm`, { method: 'POST' });
  if (!res.ok) throw new Error('Failed to confirm action');
  return res.json();
}

export async function runAgent() {
  const res = await fetch(`${API_BASE}/agent/run`, { method: 'POST' });
  if (!res.ok) throw new Error('Failed to run agent');
  return res.json();
}

export async function fetchVendors() {
  const res = await fetch(`${API_BASE}/vendors`);
  if (!res.ok) throw new Error('Failed to load vendors');
  return res.json();
}

export async function fetchFacilitiesSummary() {
  const res = await fetch(`${API_BASE}/facilities/summary`);
  if (!res.ok) throw new Error('Failed to load facilities summary');
  return res.json();
}

export async function fetchChatStatus() {
  const res = await fetch(`${API_BASE}/chat/status`);
  if (!res.ok) throw new Error('Failed to load chat status');
  return res.json();
}

export async function sendChatMessage(message) {
  const res = await fetch(`${API_BASE}/chat`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ message }),
  });
  if (!res.ok) {
    const err = await res.json().catch(() => ({}));
    throw new Error(err.message || 'Chat failed');
  }
  return res.json();
}

export async function transcribeSpeech(audioBlob) {
  const form = new FormData();
  form.append('audio', audioBlob, 'recording.webm');
  const res = await fetch(`${API_BASE}/chat/transcribe`, {
    method: 'POST',
    body: form,
  });
  if (!res.ok) {
    const text = await res.text();
    try {
      const err = JSON.parse(text);
      throw new Error(err.message || text);
    } catch {
      throw new Error(text || 'Speech transcription failed');
    }
  }
  return res.json();
}

export async function sendSpeechMessage(audioBlob) {
  const form = new FormData();
  form.append('audio', audioBlob, 'recording.webm');
  const res = await fetch(`${API_BASE}/chat/speech`, {
    method: 'POST',
    body: form,
  });
  if (!res.ok) {
    const text = await res.text();
    try {
      const err = JSON.parse(text);
      throw new Error(err.message || text);
    } catch {
      throw new Error(text || 'Speech chat failed');
    }
  }
  return res.json();
}
