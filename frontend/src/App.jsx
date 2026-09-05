import { useCallback, useEffect, useState } from 'react';
import ChatPanel from './ChatPanel';
import MorningBriefBanner from './MorningBriefBanner';
import VendorPanel from './VendorPanel';
import AiCostPanel from './AiCostPanel';
import MonitoringPanel from './MonitoringPanel';
import {
  confirmAction,
  fetchActivityLog,
  fetchAiCosts,
  fetchBrief,
  fetchFacilitiesSummary,
  fetchMemo,
  runAgent,
} from './api';

function formatCost(value) {
  if (!value) return '₹0';
  const num = Number(value);
  if (num >= 1_000_000) {
    return `₹${(num / 1_000_000).toFixed(1)}M`;
  }
  return `₹${num.toLocaleString()}`;
}

function formatAiCost(value) {
  const num = Number(value ?? 0);
  if (num >= 1) return `₹${num.toFixed(2)}`;
  if (num >= 0.01) return `₹${num.toFixed(4)}`;
  return `₹${num.toFixed(6)}`;
}

function formatTime(iso) {
  if (!iso) return '';
  const d = new Date(iso);
  return d.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', second: '2-digit' });
}

export default function App() {
  const [view, setView] = useState('monitoring');
  const [brief, setBrief] = useState(null);
  const [log, setLog] = useState([]);
  const [memo, setMemo] = useState('');
  const [facilitiesSummary, setFacilitiesSummary] = useState(null);
  const [aiCosts, setAiCosts] = useState(null);
  const [aiCostsLoading, setAiCostsLoading] = useState(false);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [confirming, setConfirming] = useState(null);
  const [running, setRunning] = useState(false);
  const [chatOpen, setChatOpen] = useState(false);

  const refresh = useCallback(async () => {
    try {
      const [briefData, logData] = await Promise.all([fetchBrief(), fetchActivityLog()]);
      setBrief(briefData);
      setLog(logData);
      setError(null);
    } catch (e) {
      setError(e.message);
    } finally {
      setLoading(false);
    }
  }, []);

  const loadFacilitiesData = useCallback(async () => {
    try {
      const [memoData, summary] = await Promise.all([fetchMemo(), fetchFacilitiesSummary()]);
      setMemo(memoData.memo || '');
      setFacilitiesSummary(summary);
    } catch {
      setMemo('Leadership memo not yet generated. Run the agent first.');
      setFacilitiesSummary(null);
    }
  }, []);

  const loadAiCosts = useCallback(async () => {
    setAiCostsLoading(true);
    try {
      const costData = await fetchAiCosts();
      setAiCosts(costData);
    } catch {
      setAiCosts(null);
    } finally {
      setAiCostsLoading(false);
    }
  }, []);

  useEffect(() => {
    refresh();
    const interval = setInterval(refresh, 5000);
    return () => clearInterval(interval);
  }, [refresh]);

  useEffect(() => {
    if (view === 'facilities') {
      loadFacilitiesData();
      const interval = setInterval(loadFacilitiesData, 10000);
      return () => clearInterval(interval);
    }
    if (view === 'ai-costs') {
      loadAiCosts();
      const interval = setInterval(loadAiCosts, 10000);
      return () => clearInterval(interval);
    }
  }, [view, loadFacilitiesData, loadAiCosts]);

  async function handleConfirm(actionId) {
    setConfirming(actionId);
    try {
      await confirmAction(actionId);
      await refresh();
    } catch (e) {
      setError(e.message);
    } finally {
      setConfirming(null);
    }
  }

  async function handleRunAgent() {
    setRunning(true);
    try {
      await runAgent();
      await refresh();
      if (view === 'facilities') await loadFacilitiesData();
      if (view === 'ai-costs') await loadAiCosts();
    } catch (e) {
      setError(e.message);
    } finally {
      setRunning(false);
    }
  }

  function copyMemo() {
    navigator.clipboard.writeText(memo);
  }

  if (loading) {
    return <div className="loading">Loading Ops Pulse… (data ingest may take a few minutes on first boot)</div>;
  }

  if (error && !brief) {
    return <div className="error">Error: {error}</div>;
  }

  const kpis = brief?.kpis || {};
  const findings = brief?.findings || [];
  const actions = brief?.pendingActions || [];
  const delta = kpis.otaDeltaVsPriorMonth;
  const deltaLabel = delta != null ? `${delta >= 0 ? '+' : ''}${delta.toFixed(1)} vs last mo` : '';

  return (
    <div className="app">
      <header className="header">
        <div className="brand">Ops Pulse</div>
        <div className="controls">
          <div className="toggle-group">
            <button
              className={view === 'monitoring' ? 'active' : ''}
              onClick={() => setView('monitoring')}
            >
              Monitoring
            </button>
            <button
              className={view === 'transport' ? 'active' : ''}
              onClick={() => setView('transport')}
            >
              Transport Manager
            </button>
            <button
              className={view === 'facilities' ? 'active' : ''}
              onClick={() => setView('facilities')}
            >
              Facilities Head
            </button>
            <button
              className={view === 'vendors' ? 'active' : ''}
              onClick={() => setView('vendors')}
            >
              Vendors
            </button>
            <button
              className={view === 'chat' ? 'active' : ''}
              onClick={() => setView('chat')}
            >
              Chat
            </button>
            <button
              className={view === 'ai-costs' ? 'active' : ''}
              onClick={() => setView('ai-costs')}
            >
              AI Costs
            </button>
          </div>
          <button className="primary" onClick={handleRunAgent} disabled={running}>
            {running ? 'Running…' : 'Run Agent Now'}
          </button>
        </div>
      </header>

      {error && <div className="error" style={{ marginBottom: '1rem' }}>{error}</div>}

      <MorningBriefBanner morningBrief={brief?.morningBrief} />

      <section className="kpi-bar">
        <div className="kpi">
          <span className="kpi-label">OTA ({kpis.vendorDisplayName})</span>
          <span className={`kpi-value ${kpis.otaPct < kpis.slaOtaPct ? 'bad' : 'good'}`}>
            {kpis.otaPct?.toFixed(1)}% {deltaLabel && <small>({deltaLabel})</small>}
          </span>
        </div>
        <div className="kpi">
          <span className="kpi-label">SLA Target</span>
          <span className="kpi-value">{kpis.slaOtaPct}%</span>
        </div>
        <div className="kpi">
          <span className="kpi-label">Prior Month</span>
          <span className="kpi-value">{kpis.priorMonthOtaPct?.toFixed(1) ?? '—'}%</span>
        </div>
        <div className="kpi">
          <span className="kpi-label">Cost Exposure</span>
          <span className="kpi-value">{formatCost(kpis.totalCost)}</span>
        </div>
        <div className="kpi">
          <span className="kpi-label">Trips (July)</span>
          <span className="kpi-value">{kpis.tripCount?.toLocaleString()}</span>
        </div>
      </section>

      {view === 'monitoring' ? (
        <MonitoringPanel />
      ) : view === 'transport' ? (
        <section className="panels">
          <div className="panel">
            <h2>Findings</h2>
            {findings.length === 0 ? (
              <p style={{ color: '#94a3b8' }}>No active findings. Run the agent.</p>
            ) : (
              findings.map((f) => (
                <div key={f.id} className="finding-card">
                  <strong>{f.type?.replace(/_/g, ' ')}</strong>
                  <span className={`status-badge ${f.severity === 'HIGH' ? 'pending' : ''}`} style={{ marginLeft: '0.5rem' }}>
                    {f.severity}
                  </span>
                  <p>{f.narration}</p>
                </div>
              ))
            )}
          </div>
          <div className="panel">
            <h2>Actions</h2>
            {actions.length === 0 ? (
              <p style={{ color: '#94a3b8' }}>No pending actions.</p>
            ) : (
              actions.map((a) => (
                <div key={a.id} className="action-card">
                  <div className="action-type">{a.actionType}</div>
                  <div className="action-message">{a.draftedMessage}</div>
                  <div className="action-buttons">
                    <button
                      className="confirm"
                      onClick={() => handleConfirm(a.id)}
                      disabled={confirming === a.id || a.status !== 'PENDING'}
                    >
                      {a.status === 'CONFIRMED' ? 'Confirmed' : confirming === a.id ? 'Confirming…' : 'Confirm'}
                    </button>
                    <span className={`status-badge ${a.status === 'PENDING' ? 'pending' : 'confirmed'}`}>
                      {a.status}
                    </span>
                  </div>
                </div>
              ))
            )}
          </div>
        </section>
      ) : view === 'facilities' ? (
        <section className="facilities-panel">
          {facilitiesSummary && (
            <div className="facilities-aggregate panel">
              <h2>Fleet Aggregate — July 2026</h2>
              <div className="facilities-kpi-grid">
                <div className="facilities-kpi">
                  <span className="facilities-kpi-label">Fleet OTA</span>
                  <span className={`facilities-kpi-value ${facilitiesSummary.fleetOtaPct < 90 ? 'bad' : 'good'}`}>
                    {facilitiesSummary.fleetOtaPct?.toFixed(1)}%
                  </span>
                </div>
                <div className="facilities-kpi">
                  <span className="facilities-kpi-label">Total Cost</span>
                  <span className="facilities-kpi-value">{formatCost(facilitiesSummary.totalCost)}</span>
                </div>
                <div className="facilities-kpi">
                  <span className="facilities-kpi-label">Cost / km</span>
                  <span className="facilities-kpi-value">
                    {facilitiesSummary.costPerKm != null ? `₹${Number(facilitiesSummary.costPerKm).toFixed(0)}/km` : '—'}
                  </span>
                </div>
                <div className="facilities-kpi">
                  <span className="facilities-kpi-label">Distance</span>
                  <span className="facilities-kpi-value">
                    {facilitiesSummary.totalKm != null ? `${Math.round(Number(facilitiesSummary.totalKm)).toLocaleString()} km` : '—'}
                  </span>
                </div>
                <div className="facilities-kpi">
                  <span className="facilities-kpi-label">Safety alerts (Jul)</span>
                  <span className="facilities-kpi-value bad">
                    {facilitiesSummary.safetyIncidentCount?.toLocaleString() ?? '0'}
                  </span>
                  <span className="facilities-kpi-sub">
                    {facilitiesSummary.sev1Count} Sev-1 · {facilitiesSummary.panicCount} panic
                  </span>
                </div>
                <div className="facilities-kpi">
                  <span className="facilities-kpi-label">Vendors Below SLA</span>
                  <span className="facilities-kpi-value bad">
                    {facilitiesSummary.vendorsBelowSla} / {facilitiesSummary.vendorCount}
                  </span>
                </div>
              </div>
              <p className="facilities-highlight">
                Highest cost/km: <strong>{facilitiesSummary.highestCostPerKmVendor ?? '—'}</strong>
                {facilitiesSummary.highestCostPerKm != null && ` (₹${Number(facilitiesSummary.highestCostPerKm).toFixed(0)}/km)`}
                {' · '}
                Lowest OTA: <strong>{facilitiesSummary.lowestOtaVendor ?? '—'}</strong>
                {facilitiesSummary.lowestOtaPct != null && ` (${facilitiesSummary.lowestOtaPct.toFixed(1)}%)`}
              </p>
            </div>
          )}

          <div className="facilities-ai-panel panel">
            <div className="facilities-ai-header">
              <h2>Monthly AI Expenditure</h2>
              <span className="facilities-ai-month">
                {facilitiesSummary?.aiCostMonth ?? 'This month'}
              </span>
            </div>
            <div className="facilities-ai-body">
              <div className="facilities-ai-stat">
                <span className="facilities-ai-stat-label">Total spend</span>
                <span className="facilities-ai-stat-value">
                  {formatAiCost(facilitiesSummary?.aiMonthlyCostInr)}
                </span>
              </div>
              <div className="facilities-ai-stat">
                <span className="facilities-ai-stat-label">API calls</span>
                <span className="facilities-ai-stat-value">
                  {(facilitiesSummary?.aiMonthlyRequestCount ?? 0).toLocaleString()}
                </span>
              </div>
              <div className="facilities-ai-stat">
                <span className="facilities-ai-stat-label">Providers</span>
                <span className="facilities-ai-stat-value facilities-ai-providers">OpenAI · Sarvam</span>
              </div>
            </div>
            <p className="facilities-ai-hint">
              Tracks LLM insights, chat, and voice-to-text usage. See the AI Costs tab for a full breakdown.
            </p>
          </div>

          {!facilitiesSummary && (
            <p className="loading">Loading fleet aggregate…</p>
          )}

          <div className="memo-panel">
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1rem' }}>
              <h2 style={{ margin: 0 }}>Leadership Memo</h2>
              <button className="primary" onClick={copyMemo}>Copy</button>
            </div>
            <div className="memo-text">{memo}</div>
          </div>
        </section>
      ) : view === 'ai-costs' ? (
        <AiCostPanel aiCosts={aiCosts} loading={aiCostsLoading} fullPage />
      ) : view === 'vendors' ? (
        <VendorPanel />
      ) : view === 'chat' ? (
        <ChatPanel />
      ) : null}

      {view === 'transport' && (
      <section className="log-panel" style={{ marginTop: '1.5rem' }}>
        <h2>Agent Activity Log</h2>
        {log.length === 0 ? (
          <p style={{ color: '#94a3b8' }}>No activity yet.</p>
        ) : (
          log.map((entry) => (
            <div key={entry.id} className="log-entry">
              <span className="log-time">{formatTime(entry.timestamp)}</span>
              <span className="log-stage">{entry.stage}</span>
              <span>{entry.message}</span>
            </div>
          ))
        )}
      </section>
      )}

      <button
        type="button"
        className="chat-fab"
        onClick={() => setChatOpen((o) => !o)}
        title="Ops Assistant — text & voice chat"
      >
        {chatOpen ? '✕' : '💬 Chat'}
      </button>

      {chatOpen && (
        <div className="chat-drawer-backdrop" onClick={() => setChatOpen(false)}>
          <div className="chat-drawer" onClick={(e) => e.stopPropagation()}>
            <ChatPanel onClose={() => setChatOpen(false)} />
          </div>
        </div>
      )}
    </div>
  );
}
