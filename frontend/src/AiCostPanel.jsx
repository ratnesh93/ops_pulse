function formatInr(value) {
  const num = Number(value ?? 0);
  if (num >= 1) return `₹${num.toFixed(2)}`;
  if (num >= 0.01) return `₹${num.toFixed(4)}`;
  return `₹${num.toFixed(6)}`;
}

function formatTime(iso) {
  if (!iso) return '—';
  const d = new Date(iso);
  return d.toLocaleString([], { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' });
}

function opLabel(type) {
  if (type === 'STT') return 'Speech-to-text';
  if (type === 'LLM') return 'LLM';
  return type?.replace(/_/g, ' ') ?? '—';
}

function providerLabel(provider) {
  if (provider === 'SARVAM') return 'Sarvam AI';
  if (provider === 'OPENAI') return 'OpenAI';
  return provider ?? '—';
}

function pct(part, total) {
  if (!total) return '0%';
  return `${((part / total) * 100).toFixed(1)}%`;
}

const ROUTING_INSIGHTS = [
  {
    layer: 'Voice input',
    provider: 'Sarvam STT',
    use: 'Mic → transcript in Chat drawer',
    cost: 'Per audio minute',
  },
  {
    layer: 'Live monitoring',
    provider: 'OpenAI gpt-4o-mini',
    use: 'Feed events → actionable insights',
    cost: 'Per insight call (~500–1.5k tokens)',
  },
  {
    layer: 'Agent narration',
    provider: 'Template (free)',
    use: 'Findings & leadership memo',
    cost: '₹0 — deterministic Java',
  },
  {
    layer: 'Direction C target',
    provider: 'OpenAI + tools',
    use: 'LLM chat, narration, multi-step agent',
    cost: 'Route cheap vs strong by severity',
  },
];

export default function AiCostPanel({ aiCosts, loading, fullPage = false }) {
  if (loading) {
    return <p style={{ color: '#94a3b8' }}>Loading AI cost data…</p>;
  }

  if (!aiCosts) {
    return <p style={{ color: '#94a3b8' }}>AI cost data unavailable.</p>;
  }

  const breakdown = aiCosts.byOperation ?? [];
  const byProvider = aiCosts.byProvider ?? [];
  const recent = aiCosts.recentUsage ?? [];
  const totalCost = Number(aiCosts.totalCostInr ?? 0);
  const totalRequests = aiCosts.totalRequests ?? 0;
  const avgCostPerCall = totalRequests > 0 ? totalCost / totalRequests : 0;

  return (
    <section className={`ai-cost-panel ${fullPage ? 'ai-cost-panel--full' : ''}`}>
      <div className="ai-cost-header">
        <div>
          <h2>AI Cost Analysis</h2>
          <span className="ai-cost-subtitle">
            Every agentic call logged · Sarvam STT + OpenAI LLM · INR estimates
          </span>
        </div>
        {fullPage && (
          <div className="ai-cost-badge">
            Agent cost discipline
          </div>
        )}
      </div>

      <div className="ai-cost-kpis">
        <div className="ai-cost-kpi">
          <span className="ai-cost-kpi-value">{formatInr(aiCosts.totalCostInr)}</span>
          <span className="ai-cost-kpi-label">Total spend</span>
        </div>
        <div className="ai-cost-kpi">
          <span className="ai-cost-kpi-value">{aiCosts.totalInputTokens?.toLocaleString() ?? 0}</span>
          <span className="ai-cost-kpi-label">Input tokens</span>
        </div>
        <div className="ai-cost-kpi">
          <span className="ai-cost-kpi-value">{aiCosts.totalOutputTokens?.toLocaleString() ?? 0}</span>
          <span className="ai-cost-kpi-label">Output tokens</span>
        </div>
        <div className="ai-cost-kpi">
          <span className="ai-cost-kpi-value">{totalRequests}</span>
          <span className="ai-cost-kpi-label">API calls</span>
        </div>
        <div className="ai-cost-kpi">
          <span className="ai-cost-kpi-value">{formatInr(avgCostPerCall)}</span>
          <span className="ai-cost-kpi-label">Avg / call</span>
        </div>
      </div>

      {fullPage && (
        <div className="ai-cost-routing">
          <h3>Agent routing map</h3>
          <div className="ai-cost-routing-grid">
            {ROUTING_INSIGHTS.map((row) => (
              <div key={row.layer} className="ai-cost-routing-card">
                <div className="ai-cost-routing-layer">{row.layer}</div>
                <div className="ai-cost-routing-provider">{row.provider}</div>
                <p>{row.use}</p>
                <span className="ai-cost-routing-cost">{row.cost}</span>
              </div>
            ))}
          </div>
        </div>
      )}

      <div className="ai-cost-split">
        {byProvider.length > 0 && (
          <div className="ai-cost-breakdown">
            <h3>By provider</h3>
            <table className="ai-cost-table">
              <thead>
                <tr>
                  <th>Provider</th>
                  <th>Calls</th>
                  <th>Share</th>
                  <th>Tokens</th>
                  <th>Cost</th>
                </tr>
              </thead>
              <tbody>
                {byProvider.map((row) => (
                  <tr key={row.provider}>
                    <td>{providerLabel(row.provider)}</td>
                    <td>{row.requestCount}</td>
                    <td>{pct(Number(row.costInr), totalCost)}</td>
                    <td>{(row.inputTokens + row.outputTokens).toLocaleString()}</td>
                    <td>{formatInr(row.costInr)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}

        {breakdown.length > 0 && (
          <div className="ai-cost-breakdown">
            <h3>By operation</h3>
            <table className="ai-cost-table">
              <thead>
                <tr>
                  <th>Operation</th>
                  <th>Calls</th>
                  <th>Input</th>
                  <th>Output</th>
                  <th>Cost</th>
                </tr>
              </thead>
              <tbody>
                {breakdown.map((row) => (
                  <tr key={row.operationType}>
                    <td>{opLabel(row.operationType)}</td>
                    <td>{row.requestCount}</td>
                    <td>{row.inputTokens.toLocaleString()}</td>
                    <td>{row.outputTokens.toLocaleString()}</td>
                    <td>{formatInr(row.costInr)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      <div className="ai-cost-recent">
        <h3>Recent usage</h3>
        {recent.length === 0 ? (
          <p style={{ color: '#94a3b8', margin: 0 }}>
            No AI usage yet. Try voice chat (mic) or simulate a monitoring event.
          </p>
        ) : (
          <table className="ai-cost-table">
            <thead>
              <tr>
                <th>Time</th>
                <th>Operation</th>
                <th>Provider</th>
                <th>Model</th>
                <th>In</th>
                <th>Out</th>
                <th>Cost</th>
              </tr>
            </thead>
            <tbody>
              {recent.map((row) => (
                <tr key={row.id}>
                  <td>{formatTime(row.createdAt)}</td>
                  <td>{opLabel(row.operationType)}</td>
                  <td>{providerLabel(row.provider)}</td>
                  <td>{row.model ?? '—'}</td>
                  <td>{row.inputTokens}</td>
                  <td>{row.outputTokens}</td>
                  <td>{formatInr(row.costInr)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
    </section>
  );
}
