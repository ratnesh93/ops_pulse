export default function ActionInsightCard({
  action,
  acting,
  onConfirm,
  onDismiss,
  badgeLabel,
  badgeTitle,
}) {
  const severity = action.severity?.toLowerCase() || 'medium';

  return (
    <div className={`live-action-card severity-${severity}`}>
      <div className="live-action-top">
        <span className={`status-badge ${action.status === 'PENDING' ? 'pending' : action.status === 'CONFIRMED' ? 'confirmed' : ''}`}>
          {action.status}
        </span>
        <span className="action-type-tag">{action.actionType}</span>
        {badgeLabel && (
          <span className="ai-badge" title={badgeTitle || badgeLabel}>
            {badgeLabel}
          </span>
        )}
      </div>
      <strong>{action.title || action.actionType?.replace(/_/g, ' ')}</strong>
      <p className="ai-insight">{action.aiInsight || action.draftedMessage}</p>
      {action.recommendedAction && (
        <div className="recommended-action">
          <span className="rec-label">Recommended:</span> {action.recommendedAction}
        </div>
      )}
      {action.status === 'PENDING' && (
        <div className="action-buttons">
          <button
            className="confirm"
            onClick={() => onConfirm(action.id)}
            disabled={acting === action.id}
          >
            {acting === action.id ? '…' : 'Confirm'}
          </button>
          {onDismiss && (
            <button
              className="dismiss-btn"
              onClick={() => onDismiss(action.id)}
              disabled={acting === action.id}
            >
              Dismiss
            </button>
          )}
        </div>
      )}
    </div>
  );
}
