import React, { useState } from 'react';
import {
  Plus,
  MapPin,
  Clock,
  CheckCircle,
  Building2,
  Calendar,
  AlertCircle,
  Filter,
  CheckCircle2,
  HelpCircle,
  MessageSquare,
  Shield,
  Eye,
  UserCheck
} from 'lucide-react';

export default function Dashboard({
  user,
  suggestions,
  onOpenNewSuggestion,
  onOpenAuth,
  activeView,
  onTriageSuggestion,
}) {
  const [filter, setFilter] = useState('All');

  const getStatusBadgeClass = (status) => {
    switch (status) {
      case 'Submitted': return 'badge-submitted';
      case 'Under Review': return 'badge-under-review';
      case 'In Progress': return 'badge-in-progress';
      case 'Resolved': return 'badge-resolved';
      case 'Rejected': return 'badge-rejected';
      default: return 'badge-submitted';
    }
  };

  const filteredSuggestions = suggestions.filter((s) => {
    if (filter === 'All') return true;
    return s.status === filter;
  });

  return (
    <main className="container" style={{ padding: '32px 20px', minHeight: 'calc(100vh - 140px)' }}>
      {/* Ward Hero Card */}
      <div style={{
        background: 'linear-gradient(135deg, #0A4DA2 0%, #032b69 100%)',
        borderRadius: 'var(--radius-xl)',
        padding: '36px',
        color: '#ffffff',
        marginBottom: '32px',
        boxShadow: 'var(--shadow-md)',
        position: 'relative',
        overflow: 'hidden',
      }}>
        <div style={{ position: 'relative', zIndex: 2 }}>
          <div style={{
            display: 'inline-flex',
            alignItems: 'center',
            gap: '6px',
            background: 'rgba(255, 255, 255, 0.15)',
            backdropFilter: 'blur(8px)',
            padding: '4px 12px',
            borderRadius: '20px',
            fontSize: '0.8rem',
            fontWeight: 600,
            marginBottom: '12px',
          }}>
            <MapPin size={14} /> Ward 14 • Indiranagar • BBMP City Council
          </div>
          <h2 style={{ fontSize: '2rem', fontWeight: 800, letterSpacing: '-0.02em', marginBottom: '8px' }}>
            {activeView === 'admin' ? 'Official Governance Triage Console' : 'Citizen Suggestion & Grievance Portal'}
          </h2>
          <p style={{ maxWidth: '640px', color: '#e2e8f0', fontSize: '0.95rem', lineHeight: 1.6 }}>
            {activeView === 'admin'
              ? 'Triage, assign municipal departments, enforce resolution SLAs, and broadcast public updates to ward residents.'
              : 'Submit geotagged infrastructure concerns (Fix-It) or community initiatives (Build-It). Your submissions are directly routed to the local ward committee.'}
          </p>

          <div style={{ display: 'flex', gap: '12px', marginTop: '24px', flexWrap: 'wrap' }}>
            {user ? (
              <button
                className="btn-primary"
                onClick={onOpenNewSuggestion}
                style={{
                  background: '#ffffff',
                  color: 'var(--primary)',
                  fontWeight: 700,
                  boxShadow: '0 4px 12px rgba(0, 0, 0, 0.15)',
                }}
              >
                <Plus size={18} />
                New Suggestion
              </button>
            ) : (
              <button
                className="btn-primary"
                onClick={onOpenAuth}
                style={{
                  background: '#ffffff',
                  color: 'var(--primary)',
                  fontWeight: 700,
                }}
              >
                Sign In with Mobile to Submit
              </button>
            )}
          </div>
        </div>
      </div>

      {/* Filter and Title Row */}
      <div style={{
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'space-between',
        flexWrap: 'wrap',
        gap: '16px',
        marginBottom: '24px',
      }}>
        <div>
          <h3 style={{ fontSize: '1.25rem', fontWeight: 800, color: 'var(--text-primary)' }}>
            {activeView === 'admin' ? 'All Ward Suggestions' : 'My Suggestion Submissions'}
          </h3>
          <p style={{ fontSize: '0.85rem', color: 'var(--text-secondary)' }}>
            {activeView === 'admin'
              ? `Displaying all ${filteredSuggestions.length} tickets across ward divisions`
              : `Tracking ${filteredSuggestions.length} ticket(s) linked to your mobile profile`}
          </p>
        </div>

        {/* Filter Pills */}
        <div style={{ display: 'flex', gap: '8px', flexWrap: 'wrap' }}>
          {['All', 'Submitted', 'Under Review', 'In Progress', 'Resolved'].map((st) => (
            <button
              key={st}
              onClick={() => setFilter(st)}
              style={{
                padding: '6px 14px',
                borderRadius: 'var(--radius-sm)',
                fontSize: '0.85rem',
                fontWeight: 600,
                background: filter === st ? 'var(--primary)' : '#ffffff',
                color: filter === st ? '#ffffff' : 'var(--text-secondary)',
                border: filter === st ? '1px solid var(--primary)' : '1px solid var(--border)',
                transition: 'all 0.15s ease',
              }}
            >
              {st}
            </button>
          ))}
        </div>
      </div>

      {/* Suggestions List */}
      {filteredSuggestions.length === 0 ? (
        <div className="card" style={{ textAlign: 'center', padding: '60px 20px' }}>
          <div style={{
            width: '56px',
            height: '56px',
            borderRadius: '50%',
            background: 'var(--primary-light)',
            color: 'var(--primary)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            margin: '0 auto 16px auto',
          }}>
            <HelpCircle size={28} />
          </div>
          <h4 style={{ fontSize: '1.1rem', fontWeight: 700, color: 'var(--text-primary)', marginBottom: '6px' }}>
            No Suggestions Found
          </h4>
          <p style={{ fontSize: '0.875rem', color: 'var(--text-secondary)', maxWidth: '400px', margin: '0 auto 20px auto' }}>
            {filter === 'All'
              ? 'You have not submitted any ward suggestions yet. Submit your first infrastructure report or idea today!'
              : `There are currently no tickets under '${filter}' status.`}
          </p>
          {user && (
            <button className="btn-primary" onClick={onOpenNewSuggestion}>
              <Plus size={16} />
              Submit Suggestion
            </button>
          )}
        </div>
      ) : (
        <div style={{
          display: 'grid',
          gridTemplateColumns: 'repeat(auto-fill, minmax(340px, 1fr))',
          gap: '20px',
        }}>
          {filteredSuggestions.map((item) => (
            <div
              key={item.id}
              className="card"
              style={{
                display: 'flex',
                flexDirection: 'column',
                justifyContent: 'space-between',
                transition: 'transform 0.15s ease, box-shadow 0.15s ease',
              }}
            >
              <div>
                {/* Card Top: Tracking ID & Status Badge */}
                <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '12px' }}>
                  <span style={{
                    fontFamily: 'monospace',
                    fontSize: '0.8rem',
                    fontWeight: 700,
                    color: 'var(--text-secondary)',
                    background: '#f1f5f9',
                    padding: '3px 8px',
                    borderRadius: '6px',
                  }}>
                    {item.tracking_id}
                  </span>
                  <span className={`badge ${getStatusBadgeClass(item.status)}`}>
                    {item.status}
                  </span>
                </div>

                {/* Category & Title */}
                <div style={{
                  fontSize: '0.75rem',
                  fontWeight: 700,
                  color: 'var(--primary)',
                  textTransform: 'uppercase',
                  letterSpacing: '0.04em',
                  marginBottom: '4px',
                }}>
                  {item.category}
                </div>
                <h4 style={{ fontSize: '1.05rem', fontWeight: 700, color: 'var(--text-primary)', marginBottom: '8px', lineHeight: 1.35 }}>
                  {item.title}
                </h4>

                {/* Description */}
                <p style={{
                  fontSize: '0.875rem',
                  color: 'var(--text-secondary)',
                  lineHeight: 1.5,
                  marginBottom: '16px',
                  display: '-webkit-box',
                  WebkitLineClamp: 3,
                  WebkitBoxOrient: 'vertical',
                  overflow: 'hidden',
                }}>
                  {item.description}
                </p>

                {/* Official Response Callout */}
                {item.admin_response && (
                  <div style={{
                    background: 'var(--success-light)',
                    borderLeft: '4px solid var(--success)',
                    padding: '10px 14px',
                    borderRadius: '0 8px 8px 0',
                    marginBottom: '16px',
                  }}>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '6px', fontSize: '0.75rem', fontWeight: 700, color: 'var(--success)', marginBottom: '2px' }}>
                      <CheckCircle2 size={14} /> Official Response:
                    </div>
                    <p style={{ fontSize: '0.8rem', color: '#166534', lineHeight: 1.4 }}>
                      {item.admin_response}
                    </p>
                  </div>
                )}
              </div>

              {/* Card Footer */}
              <div style={{
                borderTop: '1px solid var(--border)',
                paddingTop: '12px',
                marginTop: '12px',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'space-between',
                fontSize: '0.75rem',
                color: 'var(--text-tertiary)',
              }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '4px' }}>
                  <Building2 size={14} />
                  <span>{item.department?.slice(0, 24)}...</span>
                </div>
                <div style={{ display: 'flex', alignItems: 'center', gap: '4px' }}>
                  <Calendar size={14} />
                  <span>{new Date(item.created_at).toLocaleDateString()}</span>
                </div>
              </div>

              {/* Admin Action Button */}
              {activeView === 'admin' && (
                <button
                  className="btn-secondary"
                  onClick={() => onTriageSuggestion(item)}
                  style={{ width: '100%', marginTop: '12px', padding: '8px', fontSize: '0.8rem' }}
                >
                  <MessageSquare size={14} /> Update Ticket Status
                </button>
              )}
            </div>
          ))}
        </div>
      )}
    </main>
  );
}
