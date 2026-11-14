import React from 'react';
import { LogOut, User, ShieldCheck, Database, Inbox } from 'lucide-react';
import { isSupabaseConfigured } from '../lib/supabase';

export default function Navbar({
  user,
  onLogout,
  onOpenAuth,
  onOpenGuide,
  activeView,
  setActiveView,
}) {
  const isConfigured = isSupabaseConfigured();

  return (
    <header style={{
      background: '#ffffff',
      borderBottom: '1px solid var(--border)',
      position: 'sticky',
      top: 0,
      zIndex: 40,
    }}>
      <div className="container" style={{
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'space-between',
        height: '70px',
      }}>
        {/* Brand */}
        <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
          <div style={{
            width: '40px',
            height: '40px',
            borderRadius: '10px',
            background: 'var(--primary)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            color: '#ffffff',
            fontWeight: 'bold',
            fontSize: '1.2rem',
          }}>
            SB
          </div>
          <div>
            <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
              <h1 style={{ fontSize: '1.25rem', fontWeight: 700, color: 'var(--primary)', letterSpacing: '-0.02em' }}>
                SuggestBox
              </h1>
              <span style={{
                background: isConfigured ? 'var(--success-light)' : 'var(--warning-light)',
                color: isConfigured ? 'var(--success)' : 'var(--warning)',
                fontSize: '0.7rem',
                fontWeight: 700,
                padding: '2px 8px',
                borderRadius: '12px',
                display: 'inline-flex',
                alignItems: 'center',
                gap: '4px',
              }}>
                <span style={{
                  width: '6px',
                  height: '6px',
                  borderRadius: '50%',
                  background: isConfigured ? 'var(--success)' : 'var(--warning)',
                }} />
                {isConfigured ? 'Supabase Live' : 'Demo Mode'}
              </span>
            </div>
            <p style={{ fontSize: '0.75rem', color: 'var(--text-tertiary)', fontWeight: 500 }}>
              Digital Suggestion Box for Local Governance
            </p>
          </div>
        </div>

        {/* Right Actions */}
        <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
          {/* Guide Modal Trigger */}
          <button
            onClick={onOpenGuide}
            style={{
              padding: '8px 14px',
              borderRadius: 'var(--radius-md)',
              border: '1px solid var(--border)',
              background: '#f8fafc',
              fontSize: '0.85rem',
              fontWeight: 600,
              color: 'var(--text-secondary)',
              display: 'flex',
              alignItems: 'center',
              gap: '6px',
            }}
          >
            <Database size={16} color="var(--primary)" />
            Supabase Config
          </button>

          {user ? (
            <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
              {/* Role Toggle for testing Admin */}
              <div style={{
                background: 'var(--bg-secondary)',
                borderRadius: 'var(--radius-md)',
                padding: '3px',
                display: 'flex',
                gap: '2px',
              }}>
                <button
                  onClick={() => setActiveView('citizen')}
                  style={{
                    padding: '6px 12px',
                    borderRadius: 'var(--radius-sm)',
                    fontSize: '0.8rem',
                    fontWeight: 600,
                    background: activeView === 'citizen' ? '#ffffff' : 'transparent',
                    color: activeView === 'citizen' ? 'var(--primary)' : 'var(--text-secondary)',
                    boxShadow: activeView === 'citizen' ? 'var(--shadow-sm)' : 'none',
                  }}
                >
                  My Suggestions
                </button>
                <button
                  onClick={() => setActiveView('admin')}
                  style={{
                    padding: '6px 12px',
                    borderRadius: 'var(--radius-sm)',
                    fontSize: '0.8rem',
                    fontWeight: 600,
                    background: activeView === 'admin' ? '#ffffff' : 'transparent',
                    color: activeView === 'admin' ? 'var(--primary)' : 'var(--text-secondary)',
                    boxShadow: activeView === 'admin' ? 'var(--shadow-sm)' : 'none',
                    display: 'flex',
                    alignItems: 'center',
                    gap: '4px',
                  }}
                >
                  <ShieldCheck size={14} />
                  Admin Triage
                </button>
              </div>

              {/* User Identity Chip */}
              <div style={{
                display: 'flex',
                alignItems: 'center',
                gap: '8px',
                padding: '6px 12px',
                background: 'var(--primary-light)',
                borderRadius: 'var(--radius-md)',
              }}>
                <User size={16} color="var(--primary)" />
                <div style={{ textAlign: 'left', lineHeight: 1.2 }}>
                  <div style={{ fontSize: '0.8rem', fontWeight: 700, color: 'var(--primary)' }}>
                    {user.full_name}
                  </div>
                  <div style={{ fontSize: '0.7rem', color: 'var(--text-secondary)', fontFamily: 'monospace' }}>
                    {user.mobile_number}
                  </div>
                </div>
              </div>

              {/* Logout Button */}
              <button
                onClick={onLogout}
                title="Sign Out"
                style={{
                  padding: '8px',
                  borderRadius: 'var(--radius-md)',
                  color: 'var(--danger)',
                  display: 'flex',
                  alignItems: 'center',
                  background: 'var(--danger-light)',
                }}
              >
                <LogOut size={18} />
              </button>
            </div>
          ) : (
            <button className="btn-primary" onClick={onOpenAuth}>
              <User size={16} />
              Sign In / Register
            </button>
          )}
        </div>
      </div>
    </header>
  );
}
