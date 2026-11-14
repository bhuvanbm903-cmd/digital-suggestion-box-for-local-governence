import React, { useState } from 'react';
import { X, Send, MapPin, Tag, Building2, UserX } from 'lucide-react';

const CATEGORIES = [
  'Roads & Footpaths',
  'Water & Sanitation',
  'Streetlights & Power',
  'Solid Waste Management',
  'Parks & Greenery',
  'Public Health',
  'Traffic & Mobility',
  'Participatory Budget Idea',
];

const DEPARTMENTS = {
  'Roads & Footpaths': 'BBMP Road Infrastructure Engineering',
  'Water & Sanitation': 'BWSSB Water & Sewerage Division',
  'Streetlights & Power': 'BESCOM Electrical Maintenance',
  'Solid Waste Management': 'BBMP Solid Waste Management (SWM)',
  'Parks & Greenery': 'Horticulture & Parks Department',
  'Public Health': 'Ward Health & Vector Control',
  'Traffic & Mobility': 'Traffic Police & Urban Transport Cell',
  'Participatory Budget Idea': 'Ward 14 Participatory Budgeting Committee',
};

export default function NewSuggestionModal({ isOpen, onClose, onSubmitSuggestion }) {
  const [category, setCategory] = useState(CATEGORIES[0]);
  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [isAnonymous, setIsAnonymous] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  if (!isOpen) return null;

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!title.trim() || title.trim().length < 8) {
      setError('Title headline must be at least 8 characters long.');
      return;
    }
    if (!description.trim() || description.trim().length < 20) {
      setError('Description must be at least 20 characters providing location landmarks.');
      return;
    }

    setLoading(true);
    try {
      await onSubmitSuggestion({
        category,
        department: DEPARTMENTS[category] || 'Municipal Civic Desk',
        title: title.trim(),
        description: description.trim(),
        is_anonymous: isAnonymous,
      });
      onClose();
      setTitle('');
      setDescription('');
      setError('');
    } catch (err) {
      setError(err.message || 'Failed to submit suggestion.');
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
        maxWidth: '560px',
        width: '100%',
        padding: '32px',
        boxShadow: 'var(--shadow-lg)',
        position: 'relative',
        maxHeight: '90vh',
        overflowY: 'auto',
      }}>
        {/* Close Button */}
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

        <div style={{ marginBottom: '20px' }}>
          <div style={{ display: 'inline-flex', alignItems: 'center', gap: '6px', color: 'var(--primary)', fontSize: '0.8rem', fontWeight: 700, textTransform: 'uppercase', letterSpacing: '0.05em' }}>
            <MapPin size={14} /> Ward 14 • Indiranagar
          </div>
          <h2 style={{ fontSize: '1.4rem', fontWeight: 800, color: 'var(--text-primary)', marginTop: '4px' }}>
            Submit Civic Suggestion
          </h2>
          <p style={{ fontSize: '0.85rem', color: 'var(--text-secondary)' }}>
            Report an infrastructure issue or propose a community development idea with active municipal SLA tracking.
          </p>
        </div>

        {error && (
          <div style={{
            background: 'var(--danger-light)',
            color: 'var(--danger)',
            padding: '10px 14px',
            borderRadius: 'var(--radius-md)',
            fontSize: '0.85rem',
            fontWeight: 500,
            marginBottom: '16px',
          }}>
            {error}
          </div>
        )}

        <form onSubmit={handleSubmit}>
          {/* Category */}
          <div className="input-group">
            <label className="input-label">Category</label>
            <select
              className="input-field"
              value={category}
              onChange={(e) => setCategory(e.target.value)}
            >
              {CATEGORIES.map((cat) => (
                <option key={cat} value={cat}>{cat}</option>
              ))}
            </select>
          </div>

          {/* Auto Department */}
          <div style={{
            background: 'var(--primary-light)',
            padding: '10px 14px',
            borderRadius: 'var(--radius-md)',
            marginBottom: '16px',
            display: 'flex',
            alignItems: 'center',
            gap: '8px',
            fontSize: '0.8rem',
            color: 'var(--primary)',
            fontWeight: 600,
          }}>
            <Building2 size={16} />
            <span>Assigned Department: <strong>{DEPARTMENTS[category]}</strong></span>
          </div>

          {/* Title */}
          <div className="input-group">
            <label className="input-label">Suggestion Headline / Title</label>
            <input
              type="text"
              className="input-field"
              placeholder="e.g. Broken water valve causing road flooding on 12th Cross"
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              required
            />
          </div>

          {/* Description */}
          <div className="input-group">
            <label className="input-label">Detailed Description & Landmarks</label>
            <textarea
              className="input-field"
              rows={4}
              placeholder="Describe location details, affected residents, safety risk, or proposed civic solution..."
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              required
            />
          </div>

          {/* Anonymous Toggle */}
          <label style={{
            display: 'flex',
            alignItems: 'flex-start',
            gap: '10px',
            background: '#f8fafc',
            border: '1px solid var(--border)',
            padding: '12px 14px',
            borderRadius: 'var(--radius-md)',
            cursor: 'pointer',
            marginBottom: '20px',
          }}>
            <input
              type="checkbox"
              checked={isAnonymous}
              onChange={(e) => setIsAnonymous(e.target.checked)}
              style={{ marginTop: '3px' }}
            />
            <div>
              <div style={{ fontSize: '0.85rem', fontWeight: 600, color: 'var(--text-primary)' }}>
                Submit as Anonymous Whistleblower
              </div>
              <div style={{ fontSize: '0.75rem', color: 'var(--text-tertiary)' }}>
                Your name and phone number will be hidden from public feeds and fellow citizens.
              </div>
            </div>
          </label>

          <button
            type="submit"
            className="btn-primary"
            style={{ width: '100%', padding: '14px' }}
            disabled={loading}
          >
            <Send size={18} />
            {loading ? 'Submitting to Ward...' : 'Submit Suggestion'}
          </button>
        </form>
      </div>
    </div>
  );
}
