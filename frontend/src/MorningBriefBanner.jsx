function formatCost(value) {
  if (!value) return '₹0';
  const num = Number(value);
  if (num >= 1_000_000) return `₹${(num / 1_000_000).toFixed(1)}M`;
  return `₹${num.toLocaleString()}`;
}

export default function MorningBriefBanner({ morningBrief }) {
  if (!morningBrief) return null;

  return (
    <section className="morning-brief">
      <div className="morning-brief-top">
        <h2>{morningBrief.greeting}</h2>
        <span className="morning-brief-time">July 2026 · Pre-shift brief</span>
      </div>
      <p className="morning-brief-summary">{morningBrief.summary}</p>
      <div className="morning-brief-stats">
        <div className="morning-stat">
          <span className="morning-stat-value">{morningBrief.itemsNeedingAttention}</span>
          <span className="morning-stat-label">Need attention</span>
        </div>
        <div className="morning-stat">
          <span className="morning-stat-value">{morningBrief.vendorsBelowSla}</span>
          <span className="morning-stat-label">Below SLA</span>
        </div>
        <div className="morning-stat">
          <span className="morning-stat-value">
            #{morningBrief.focusVendorOtaRank}/{morningBrief.vendorCount}
          </span>
          <span className="morning-stat-label">{morningBrief.focusVendorName} OTA rank</span>
        </div>
        <div className="morning-stat">
          <span className="morning-stat-value">{formatCost(morningBrief.costAtRisk)}</span>
          <span className="morning-stat-label">Cost at risk</span>
        </div>
      </div>
    </section>
  );
}
