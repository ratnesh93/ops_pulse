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

export default function AiCostPanel({ aiCosts, loading }) {
  if (loading) {
    return <p style={{ color: '#94a3b8' }}>Loading AI cost data…</p>;
  }

  if (!aiCosts) {
    return <p style={{ color: '#94a3b8' }}>AI cost data unavailable.</p>;
  }

  const breakdown = aiCosts.byOperation ?? [];
  const recent = aiCosts.recentUsage ?? [];

  return (
    <section className="ai-cost-panel">
      <div className="ai-cost-header">
        <h2>AI Usage &amp; Cost</h2>
        <span className="ai-cost-subtitle">STT today · LLM-ready tracking</span>
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
          <span className="ai-cost-kpi-value">{aiCosts.totalRequests ?? 0}</span>
          <span className="ai-cost-kpi-label">API calls</span>
        </div>
      </div>

      {breakdown.length > 0 && (
        <div className="ai-cost-breakdown">
          <h3>By operation</h3>
          <table className="ai-cost-table">
            <thead>
              <tr>
                <th>Operation</th>
                <th>Calls</th>
                <th>Input tokens</th>
                <th>Output tokens</th>
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

      <div className="ai-cost-recent">
        <h3>Recent usage</h3>
        {recent.length === 0 ? (
          <p style={{ color: '#94a3b8', margin: 0 }}>
            No AI usage yet. Use voice chat (mic) to record STT costs.
          </p>
        ) : (
          <table className="ai-cost-table">
            <thead>
              <tr>
                <th>Time</th>
                <th>Operation</th>
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
