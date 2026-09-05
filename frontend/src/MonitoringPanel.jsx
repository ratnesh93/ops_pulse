import { useCallback, useEffect, useMemo, useState } from 'react';
import {
  confirmMonitoringAction,
  dismissMonitoringAction,
  fetchActivityLog,
  fetchMonitoring,
  fetchMonitoringScenarios,
  simulateMonitoringFeed,
} from './api';

const SIM_PANEL_STORAGE_KEY = 'opspulse-hide-sim-panel';

function readSimPanelHidden() {
  try {
    return localStorage.getItem(SIM_PANEL_STORAGE_KEY) === 'true';
  } catch {
    return false;
  }
}
const VENDORS = [
  { id: 'rohan', label: 'Rohan Travel (Vendor B) — focus vendor' },
  { id: 'priya', label: 'Priya Travel — peer vendor' },
];

const FALLBACK_SCENARIOS = [
  { id: 'NEGATIVE_ROAD_BLOCK', label: 'Road block on route', sentiment: 'NEGATIVE', description: 'ORR corridor closure' },
  { id: 'NEGATIVE_ETA_MISSED', label: 'ETA missed for vendor', sentiment: 'NEGATIVE', description: 'Trip past pickup window' },
  { id: 'NEGATIVE_DRIVER_ABSENT', label: 'Drivers absent', sentiment: 'NEGATIVE', description: 'No-shows at login shift' },
  { id: 'NEGATIVE_DELAY_SPIKE', label: 'ETA slip spike', sentiment: 'NEGATIVE', description: 'Sustained route delays' },
  { id: 'NEGATIVE_VEHICLE_BREAKDOWN', label: 'Vehicle breakdown', sentiment: 'NEGATIVE', description: 'Shuttle stranded' },
  { id: 'POSITIVE_OTA_RECOVERY', label: 'OTA recovery', sentiment: 'POSITIVE', description: 'Vendor back above SLA' },
  { id: 'POSITIVE_ROUTE_CLEARED', label: 'Road block cleared', sentiment: 'POSITIVE', description: 'Corridor reopened' },
  { id: 'NEUTRAL_OCCUPANCY', label: 'Occupancy normal', sentiment: 'NEUTRAL', description: 'Within capacity range' },
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

function sentimentLogClass(sentiment) {
  if (sentiment === 'POSITIVE') return 'log-stage-positive';
  if (sentiment === 'NEGATIVE') return 'log-stage-negative';
  return 'log-stage-neutral';
}

function groupScenarios(scenarios) {
  const groups = { NEGATIVE: [], POSITIVE: [], NEUTRAL: [] };
  for (const s of scenarios) {
    const bucket = groups[s.sentiment] ?? groups.NEUTRAL;
    bucket.push(s);
  }
  return groups;
}

export default function MonitoringPanel() {
  const [data, setData] = useState(null);
  const [scenarios, setScenarios] = useState(FALLBACK_SCENARIOS);
  const [agentLog, setAgentLog] = useState([]);
  const [loading, setLoading] = useState(true);
  const [feeding, setFeeding] = useState(false);
  const [acting, setActing] = useState(null);
  const [error, setError] = useState(null);
  const [vendor, setVendor] = useState('rohan');
  const [scenario, setScenario] = useState('NEGATIVE_ROAD_BLOCK');
  const [simPanelHidden, setSimPanelHidden] = useState(readSimPanelHidden);

  function hideSimPanel() {
    setSimPanelHidden(true);
    try {
      localStorage.setItem(SIM_PANEL_STORAGE_KEY, 'true');
    } catch {
      // ignore storage errors
    }
  }

  function showSimPanel() {
    setSimPanelHidden(false);
    try {
      localStorage.removeItem(SIM_PANEL_STORAGE_KEY);
    } catch {
      // ignore storage errors
    }
  }

  const refresh = useCallback(async () => {
    try {
      const [dashboard, log] = await Promise.all([fetchMonitoring(), fetchActivityLog()]);
      setData(dashboard);
      setAgentLog(log);
      setError(null);
    } catch (e) {
      setError(e.message);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchMonitoringScenarios()
      .then((list) => {
        if (list?.length) {
          setScenarios(list);
          setScenario((prev) => (list.some((s) => s.id === prev) ? prev : list[0].id));
        }
      })
      .catch(() => {});
  }, []);

  useEffect(() => {
    refresh();
    const interval = setInterval(refresh, 5000);
    return () => clearInterval(interval);
  }, [refresh]);

  const scenarioGroups = useMemo(() => groupScenarios(scenarios), [scenarios]);

  async function handleSimulate() {
    if (!scenario) return;
    setFeeding(true);
    try {
      const dashboard = await simulateMonitoringFeed(scenario, vendor);
      setData(dashboard);
      setError(null);
      const log = await fetchActivityLog();
      setAgentLog(log);
    } catch (e) {
      setError(e.message);
    } finally {
      setFeeding(false);
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

  const activityStream = useMemo(() => {
    const feedEntries = (data?.feedEvents ?? []).map((e) => ({
      id: `feed-${e.id}`,
      timestamp: e.createdAt,
      stage: e.sentiment,
      stageClass: sentimentLogClass(e.sentiment),
      message: `${e.title} — ${e.detail} (${e.office} · ${e.shiftId})`,
      kind: 'LIVE',
    }));
    const agentEntries = agentLog.map((e) => ({
      id: `agent-${e.id}`,
      timestamp: e.timestamp,
      stage: e.stage,
      stageClass: 'log-stage-agent',
      message: e.message,
      kind: 'AGENT',
    }));
    return [...feedEntries, ...agentEntries].sort(
      (a, b) => new Date(b.timestamp) - new Date(a.timestamp),
    );
  }, [data?.feedEvents, agentLog]);

  if (loading && !data) {
    return <p className="loading">Loading live monitoring…</p>;
  }

  const actions = data?.actionItems ?? [];
  const pending = actions.filter((a) => a.status === 'PENDING');
  const selectedVendor = VENDORS.find((v) => v.id === vendor)?.label ?? vendor;

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

      {simPanelHidden ? (
        <div className="monitoring-simulate demo-dropdown-panel demo-sim-collapsed">
          <div className="demo-panel-header">
            <h3 className="demo-only-heading">Demo only — simulated live signals</h3>
            <button type="button" className="demo-panel-toggle" onClick={showSimPanel}>
              Show
            </button>
          </div>
        </div>
      ) : (
        <div className="monitoring-simulate demo-dropdown-panel">
          <div className="demo-panel-header">
            <h3 className="demo-only-heading">Demo only — simulated live signals</h3>
            <button
              type="button"
              className="demo-panel-toggle"
              onClick={hideSimPanel}
              aria-label="Hide simulated live signals panel"
            >
              Hide
            </button>
          </div>
          <p className="demo-only-hint">
            Pick a vendor and scenario to inject into the live feed. Events appear in the activity stream below with actionable insights.
          </p>
          <div className="demo-dropdown-row">
            <label className="demo-field">
              <span className="demo-field-label">Vendor / travel</span>
              <select
                className="demo-select"
                value={vendor}
                onChange={(e) => setVendor(e.target.value)}
                disabled={feeding}
              >
                {VENDORS.map((v) => (
                  <option key={v.id} value={v.id}>{v.label}</option>
                ))}
              </select>
            </label>
            <label className="demo-field demo-field-grow">
              <span className="demo-field-label">Scenario</span>
              <select
                className="demo-select"
                value={scenario}
                onChange={(e) => setScenario(e.target.value)}
                disabled={feeding}
              >
                <optgroup label="Negative signals">
                  {scenarioGroups.NEGATIVE.map((s) => (
                    <option key={s.id} value={s.id} title={s.description}>
                      {s.label}
                    </option>
                  ))}
                </optgroup>
                <optgroup label="Positive signals">
                  {scenarioGroups.POSITIVE.map((s) => (
                    <option key={s.id} value={s.id} title={s.description}>
                      {s.label}
                    </option>
                  ))}
                </optgroup>
                <optgroup label="Neutral">
                  {scenarioGroups.NEUTRAL.map((s) => (
                    <option key={s.id} value={s.id} title={s.description}>
                      {s.label}
                    </option>
                  ))}
                </optgroup>
              </select>
            </label>
            <button
              type="button"
              className="primary demo-inject-btn"
              onClick={handleSimulate}
              disabled={feeding || !scenario}
            >
              {feeding ? 'Injecting…' : 'Inject demo signal'}
            </button>
          </div>
          <p className="demo-preview">
            Preview: <strong>{selectedVendor}</strong> · {scenarios.find((s) => s.id === scenario)?.description ?? scenario}
          </p>
        </div>
      )}

      <div className="monitoring-grid">
        <div className="panel monitoring-actions">
          <h3>Actionable insights {pending.length > 0 && <span className="action-count">({pending.length} pending)</span>}</h3>
          {actions.length === 0 ? (
            <p style={{ color: '#94a3b8' }}>Inject a demo signal to generate AI-backed action items.</p>
          ) : (
            <div className="live-action-list">
              {actions.map((a) => (
                <div key={a.id} className={`live-action-card severity-${a.severity?.toLowerCase()}`}>
                  <div className="live-action-top">
                    <span className={`status-badge ${a.status === 'PENDING' ? 'pending' : 'confirmed'}`}>
                      {a.status}
                    </span>
                    <span className="action-type-tag">{a.actionType}</span>
                    {(data?.openAiConfigured || (a.openaiModel && a.openaiModel !== 'template-fallback')) && (
                      <span
                        className="ai-badge"
                        title={
                          a.openaiModel && a.openaiModel !== 'template-fallback'
                            ? a.openaiModel
                            : 'OpenAI insights enabled'
                        }
                      >
                        OpenAI
                      </span>
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

        <div className="panel monitoring-feed">
          <h3>Live activity stream</h3>
          <p className="feed-hint">Live feed events + agent audit log, newest first</p>
          {activityStream.length === 0 ? (
            <p style={{ color: '#94a3b8' }}>No activity yet. Use the demo dropdown above.</p>
          ) : (
            <div className="activity-stream">
              {activityStream.map((entry) => (
                <div key={entry.id} className="log-entry activity-stream-entry">
                  <span className="log-time">{formatTime(entry.timestamp)}</span>
                  <span className={`log-stage ${entry.stageClass}`}>
                    {entry.kind === 'LIVE' ? entry.stage : entry.stage}
                  </span>
                  <span>{entry.message}</span>
                </div>
              ))}
            </div>
          )}
        </div>
      </div>
    </section>
  );
}
