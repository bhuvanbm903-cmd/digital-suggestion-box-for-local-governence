import React, { useState } from 'react';
import { X, CheckCircle, Clock, AlertTriangle, MessageSquare } from 'lucide-react';

const STATUS_OPTIONS = ['Submitted', 'Under Review', 'In Progress', 'Resolved', 'Rejected'];

export default function AdminTriageModal({ isOpen, onClose, suggestion, onUpdateStatus }) {
  if (!isOpen || !suggestion) return null;

  const [status, setStatus] = useState(suggestion.status || 'Submitted');
  const [response, setResponse] = useState(suggestion.admin_response || '');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const handleSave = async (e) => {
    e.preventDefault();
    setLoading(true);
    setError('');
    try {
      await onUpdateStatus(suggestion.id, status, response.trim());
      onClose();
    } catch (err) {
      setError(err.message || 'Failed to update status.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div style={{
      position: 'fixed',
      top: 0,
      left: 0,
      right: 0,
      bottom: 0,
      backgroundColor: 'rgba(15, 23, 42, 0.65)',
      backdropFilter: 'blur(6px)',
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'center',
      zIndex: 50,
      padding: '16px',
    }}>
      <div style={{
        background: '#ffffff',
        borderRadius: 'var(--radius-xl)',
        maxWidth: '520px',
        width: '100%',
        padding: '32px',
        boxShadow: 'var(--shadow-lg)',
        position: 'relative',
      }}>
        {/* Close */}
        <button
          onClick={onClose}
          style={{
            position: 'absolute',
            top: '20px',
            right: '20px',
            color: 'var(--text-tertiary)',
            padding: '4px',
            borderRadius: '50%',
          }}
        >
          <X size={20} />
        </button>

        <div style={{ marginBottom: '16px' }}>
          <span className="badge badge-submitted" style={{ marginBottom: '6px' }}>
            {suggestion.tracking_id}
          </span>
          <h2 style={{ fontSize: '1.25rem', fontWeight: 800, color: 'var(--text-primary)' }}>
            Official Triage & Resolution
          </h2>
          <p style={{ fontSize: '0.85rem', color: 'var(--text-secondary)' }}>
            {suggestion.title}
          </p>
        </div>

        {error && (
          <div style={{
            background: 'var(--danger-light)',
            color: 'var(--danger)',
            padding: '10px 14px',
            borderRadius: 'var(--radius-md)',
            fontSize: '0.85rem',
            marginBottom: '14px',
          }}>
            {error}
          </div>
        )}

        <form onSubmit={handleSave}>
          <div className="input-group">
            <label className="input-label">Update Status</label>
            <select
              className="input-field"
              value={status}
              onChange={(e) => setStatus(e.target.value)}
            >
              {STATUS_OPTIONS.map((st) => (
                <option key={st} value={st}>{st}</option>
              ))}
            </select>
          </div>

          <div className="input-group">
            <label className="input-label">Official Public Response</label>
            <textarea
              className="input-field"
              rows={4}
              placeholder="e.g. Field engineer dispatched. Asphalt crew completed patching. Resolved on 14-Nov-2026."
              value={response}
              onChange={(e) => setResponse(e.target.value)}
              required
            />
          </div>

          <button
            type="submit"
            className="btn-primary"
            style={{ width: '100%', padding: '12px' }}
            disabled={loading}
          >
            {loading ? 'Updating Ticket...' : 'Save & Notify Citizen'}
          </button>
        </form>
      </div>
    </div>
  );
}
