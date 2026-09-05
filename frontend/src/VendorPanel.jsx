import { useEffect, useState } from 'react';
import { fetchVendors } from './api';

function formatCost(value) {
  if (!value) return '₹0';
  const num = Number(value);
  if (num >= 1_000_000) return `₹${(num / 1_000_000).toFixed(1)}M`;
  return `₹${num.toLocaleString()}`;
}

function formatCostShort(value) {
  if (!value) return '—';
  const num = Number(value);
  if (num >= 1000) return `₹${Math.round(num).toLocaleString()}`;
  return `₹${num.toFixed(0)}`;
}

function formatCostPerKm(value) {
  if (!value) return '—';
  return `₹${Number(value).toFixed(0)}/km`;
}

export default function VendorPanel() {
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [sortBy, setSortBy] = useState('tripCount');
  const [sortDir, setSortDir] = useState('desc');

  useEffect(() => {
    fetchVendors()
      .then(setData)
      .catch((e) => setError(e.message))
      .finally(() => setLoading(false));
  }, []);

  if (loading) return <div className="panel">Loading vendors…</div>;
  if (error) return <div className="error">Failed to load vendors: {error}</div>;
  if (!data) return null;

  const vendors = [...data.vendors].sort((a, b) => {
    const av = a[sortBy] ?? 0;
    const bv = b[sortBy] ?? 0;
    if (typeof av === 'string') {
      return sortDir === 'asc' ? av.localeCompare(bv) : bv.localeCompare(av);
    }
    return sortDir === 'asc' ? av - bv : bv - av;
  });

  function toggleSort(col) {
    if (sortBy === col) {
      setSortDir((d) => (d === 'asc' ? 'desc' : 'asc'));
    } else {
      setSortBy(col);
      setSortDir(col === 'displayName' ? 'asc' : 'desc');
    }
  }

  const sortIcon = (col) => (sortBy === col ? (sortDir === 'asc' ? ' ▲' : ' ▼') : '');

  return (
    <section className="vendor-panel">
      <div className="vendor-panel-header">
        <h2>Vendor Scorecard — July 2026</h2>
        <p className="vendor-summary">
          {data.vendorCount} vendors · SLA target {data.slaOtaPct}% ·{' '}
          <span className="breach-count">{data.breachCount} below SLA</span>
        </p>
      </div>

      <div className="vendor-table-wrap">
        <table className="vendor-table">
          <thead>
            <tr>
              <th onClick={() => toggleSort('otaRank')}>Rank{sortIcon('otaRank')}</th>
              <th onClick={() => toggleSort('displayName')}>Vendor{sortIcon('displayName')}</th>
              <th onClick={() => toggleSort('otaPct')}>OTA %{sortIcon('otaPct')}</th>
              <th onClick={() => toggleSort('peerGapPct')}>Gap vs peer{sortIcon('peerGapPct')}</th>
              <th onClick={() => toggleSort('priorMonthOtaPct')}>Prior Mo %{sortIcon('priorMonthOtaPct')}</th>
              <th onClick={() => toggleSort('tripCount')}>Trips{sortIcon('tripCount')}</th>
              <th onClick={() => toggleSort('totalCost')}>Cost{sortIcon('totalCost')}</th>
              <th onClick={() => toggleSort('costPerKm')}>Cost/km{sortIcon('costPerKm')}</th>
              <th onClick={() => toggleSort('costPerOnTimeTrip')}>Cost/on-time{sortIcon('costPerOnTimeTrip')}</th>
              <th onClick={() => toggleSort('safetyIncidentCount')}>Safety alerts (Jul){sortIcon('safetyIncidentCount')}</th>
              <th>Status</th>
            </tr>
          </thead>
          <tbody>
            {vendors.map((v) => (
              <tr key={v.vendorId} className={v.focusVendor ? 'focus-row' : ''}>
                <td className="rank-cell">#{v.otaRank}</td>
                <td>
                  {v.displayName}
                  {v.focusVendor && <span className="focus-tag">Focus</span>}
                </td>
                <td className={v.slaBreach ? 'ota-bad' : 'ota-good'}>{v.otaPct.toFixed(1)}%</td>
                <td className={v.peerGapPct > 0 ? 'gap-bad' : 'ota-good'}>
                  {v.peerGapPct != null ? `+${v.peerGapPct.toFixed(1)} pts` : '—'}
                </td>
                <td>{v.priorMonthOtaPct != null ? `${v.priorMonthOtaPct.toFixed(1)}%` : '—'}</td>
                <td>{v.tripCount.toLocaleString()}</td>
                <td>{formatCost(v.totalCost)}</td>
                <td>{formatCostPerKm(v.costPerKm)}</td>
                <td>{formatCostShort(v.costPerOnTimeTrip)}</td>
                <td
                  className={v.sev1Count > 0 ? 'safety-bad' : v.safetyIncidentCount > 0 ? 'safety-warn' : 'safety-ok'}
                  title={`${v.safetyIncidentCount ?? 0} July safety alerts · ${v.sev1Count ?? 0} Sev-1 · ${v.panicCount ?? 0} panic`}
                >
                  {(v.safetyIncidentCount ?? 0).toLocaleString()}
                  {(v.sev1Count ?? 0) > 0 && <span className="safety-detail"> ({v.sev1Count} Sev-1)</span>}
                </td>
                <td>
                  <span className={`status-badge ${v.slaBreach ? 'pending' : 'confirmed'}`}>
                    {v.slaBreach ? 'BREACH' : 'OK'}
                  </span>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </section>
  );
}
