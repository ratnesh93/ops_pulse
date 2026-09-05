import { useCallback, useEffect, useState } from 'react';
import {
  confirmMonitoringAction,
  dismissMonitoringAction,
  fetchMonitoring,
  simulateMonitoringFeed,
} from './api';

const SCENARIOS = [
  {
    id: 'NEGATIVE_DRIVER_ABSENT',
    label: '4 drivers absent',
    sentiment: 'negative',
    description: 'Cedar Ridge 08:00 — no-shows',
  },
  {
    id: 'NEGATIVE_DELAY_SPIKE',
    label: 'ETA slip +18 min',
    sentiment: 'negative',
    description: 'Rohan Travel route delays',
  },
  {
    id: 'POSITIVE_OTA_RECOVERY',
    label: 'OTA recovery 96%',
    sentiment: 'positive',
    description: 'Priya Travel morning shift',
  },
  {
    id: 'NEUTRAL_OCCUPANCY',
    label: 'Occupancy normal',
    sentiment: 'neutral',
    description: 'Clearwater 15:30 at 82%',
  },
];

function formatTime(iso) {
  if (!iso) return '—';
  const d = new Date(iso);
  return d.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', second: '2-digit' });
}

function sentimentClass(sentiment) {
  if (sentiment === 'POSITIVE') return 'sentiment-positive';
  if (sentiment === 'NEGATIVE') return 'sentiment-negative';
  return 'sentiment-neutral';
}

export default function MonitoringPanel() {
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [feeding, setFeeding] = useState(null);
  const [acting, setActing] = useState(null);
  const [error, setError] = useState(null);

  const refresh = useCallback(async () => {
    try {
      const dashboard = await fetchMonitoring();
      setData(dashboard);
      setError(null);
    } catch (e) {
      setError(e.message);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    refresh();
    const interval = setInterval(refresh, 5000);
    return () => clearInterval(interval);
  }, [refresh]);

  async function handleSimulate(scenarioId) {
    setFeeding(scenarioId);
    try {
      const dashboard = await simulateMonitoringFeed(scenarioId);
      setData(dashboard);
      setError(null);
    } catch (e) {
      setError(e.message);
    } finally {
      setFeeding(null);
    }
  }

  async function handleConfirm(id) {
    setActing(id);
    try {
      await confirmMonitoringAction(id);
      await refresh();
    } catch (e) {
      setError(e.message);
    } finally {
      setActing(null);
    }
  }

  async function handleDismiss(id) {
    setActing(id);
    try {
      await dismissMonitoringAction(id);
      await refresh();
    } catch (e) {
      setError(e.message);
    } finally {
      setActing(null);
    }
  }

  if (loading && !data) {
    return <p className="loading">Loading live monitoring…</p>;
  }

  const feed = data?.feedEvents ?? [];
  const actions = data?.actionItems ?? [];
  const pending = actions.filter((a) => a.status === 'PENDING');

  return (
    <section className="monitoring-panel">
      <div className="monitoring-header">
        <div>
          <h2>Live Monitoring</h2>
          <p className="monitoring-subtitle">
            Simulated live feed + July benchmarks
            {data?.openAiConfigured ? ' · OpenAI insights on' : ' · OpenAI not configured (template fallback)'}
          </p>
        </div>
        <div className="monitoring-pending-badge">
          {data?.pendingCount ?? 0} actionable
        </div>
      </div>

      {error && <div className="error" style={{ marginBottom: '1rem' }}>{error}</div>}

      <div className="monitoring-simulate">
        <span className="simulate-label">Feed live signal:</span>
        <div className="simulate-buttons">
          {SCENARIOS.map((s) => (
            <button
              key={s.id}
              type="button"
              className={`simulate-btn simulate-${s.sentiment}`}
              onClick={() => handleSimulate(s.id)}
              disabled={feeding != null}
              title={s.description}
            >
              {feeding === s.id ? 'Feeding…' : s.label}
            </button>
          ))}
        </div>
      </div>

      <div className="monitoring-grid">
        <div className="panel monitoring-feed">
          <h3>Live feed</h3>
          {feed.length === 0 ? (
            <p style={{ color: '#94a3b8' }}>No live events yet. Click a feed button above.</p>
          ) : (
            <ul className="feed-list">
              {feed.map((e) => (
                <li key={e.id} className={`feed-item ${sentimentClass(e.sentiment)}`}>
                  <div className="feed-item-top">
                    <span className={`sentiment-tag ${sentimentClass(e.sentiment)}`}>{e.sentiment}</span>
                    <span className="feed-time">{formatTime(e.createdAt)}</span>
                  </div>
                  <strong>{e.title}</strong>
                  <p>{e.detail}</p>
                  <span className="feed-meta">{e.office} · {e.shiftId} shift</span>
                </li>
              ))}
            </ul>
          )}
        </div>

        <div className="panel monitoring-actions">
          <h3>Actionable insights {pending.length > 0 && <span className="action-count">({pending.length} pending)</span>}</h3>
          {actions.length === 0 ? (
            <p style={{ color: '#94a3b8' }}>Feed a live signal to generate AI-backed action items.</p>
          ) : (
            <div className="live-action-list">
              {actions.map((a) => (
                <div key={a.id} className={`live-action-card severity-${a.severity?.toLowerCase()}`}>
                  <div className="live-action-top">
                    <span className={`status-badge ${a.status === 'PENDING' ? 'pending' : 'confirmed'}`}>
                      {a.status}
                    </span>
                    <span className="action-type-tag">{a.actionType}</span>
                    {a.openaiModel && a.openaiModel !== 'template-fallback' && (
                      <span className="ai-badge">OpenAI</span>
                    )}
                  </div>
                  <strong>{a.title}</strong>
                  <p className="ai-insight">{a.aiInsight}</p>
                  <div className="recommended-action">
                    <span className="rec-label">Recommended:</span> {a.recommendedAction}
                  </div>
                  {a.status === 'PENDING' && (
                    <div className="action-buttons">
                      <button
                        className="confirm"
                        onClick={() => handleConfirm(a.id)}
                        disabled={acting === a.id}
                      >
                        {acting === a.id ? '…' : 'Confirm'}
                      </button>
                      <button
                        className="dismiss-btn"
                        onClick={() => handleDismiss(a.id)}
                        disabled={acting === a.id}
                      >
                        Dismiss
                      </button>
                    </div>
                  )}
                </div>
              ))}
            </div>
          )}
        </div>
      </div>
    </section>
  );
}
