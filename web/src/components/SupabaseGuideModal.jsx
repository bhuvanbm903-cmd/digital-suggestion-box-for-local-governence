import React, { useState } from 'react';
import { X, Copy, Check, ExternalLink, ShieldCheck, Database, KeyRound } from 'lucide-react';

export default function SupabaseGuideModal({ isOpen, onClose }) {
  const [copied, setCopied] = useState(false);

  if (!isOpen) return null;

  const sqlSnippet = `-- Execute in Supabase SQL Editor:
-- 1. Create User Profiles
CREATE TABLE IF NOT EXISTS public.user_profiles (
    id UUID PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
    full_name TEXT NOT NULL,
    mobile_number VARCHAR(15) UNIQUE NOT NULL,
    role VARCHAR(20) DEFAULT 'citizen' CHECK (role IN ('citizen', 'official', 'admin')),
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- 2. Create Suggestions Table
CREATE TABLE IF NOT EXISTS public.suggestions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tracking_id TEXT UNIQUE NOT NULL,
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    category TEXT NOT NULL,
    department TEXT NOT NULL,
    title TEXT NOT NULL,
    description TEXT NOT NULL,
    ward_id TEXT DEFAULT 'ward-14',
    ward_name TEXT DEFAULT 'Ward 14 • Indiranagar',
    status TEXT DEFAULT 'Submitted' CHECK (status IN ('Submitted', 'Under Review', 'In Progress', 'Resolved', 'Rejected')),
    admin_response TEXT,
    is_anonymous BOOLEAN DEFAULT false,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- 3. Enable RLS
ALTER TABLE public.user_profiles ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.suggestions ENABLE ROW LEVEL SECURITY;

-- 4. RLS Policy: Citizens view only their own suggestions
CREATE POLICY "Users can view own suggestions"
    ON public.suggestions FOR SELECT
    USING (auth.uid() = user_id OR EXISTS (SELECT 1 FROM public.user_profiles WHERE id = auth.uid() AND role IN ('admin', 'official')));

CREATE POLICY "Users can insert own suggestions"
    ON public.suggestions FOR INSERT
    WITH CHECK (auth.uid() = user_id);`;

  const copySql = () => {
    navigator.clipboard.writeText(sqlSnippet);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
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
      zIndex: 60,
      padding: '16px',
    }}>
      <div style={{
        background: '#ffffff',
        borderRadius: 'var(--radius-xl)',
        maxWidth: '680px',
        width: '100%',
        padding: '32px',
        boxShadow: 'var(--shadow-lg)',
        position: 'relative',
        maxHeight: '85vh',
        overflowY: 'auto',
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

        <div style={{ display: 'flex', alignItems: 'center', gap: '10px', marginBottom: '8px' }}>
          <Database size={24} color="var(--primary)" />
          <h2 style={{ fontSize: '1.35rem', fontWeight: 800, color: 'var(--text-primary)' }}>
            Supabase Dashboard Setup Guide
          </h2>
        </div>
        <p style={{ fontSize: '0.875rem', color: 'var(--text-secondary)', marginBottom: '20px' }}>
          Follow these 3 quick steps to connect your free Supabase PostgreSQL database and Mobile Number authentication.
        </p>

        {/* Step 1 */}
        <div style={{
          background: '#f8fafc',
          border: '1px solid var(--border)',
          borderRadius: 'var(--radius-md)',
          padding: '16px',
          marginBottom: '16px',
        }}>
          <h3 style={{ fontSize: '0.95rem', fontWeight: 700, color: 'var(--primary)', marginBottom: '6px', display: 'flex', alignItems: 'center', gap: '6px' }}>
            <span>Step 1:</span> Run SQL Schema in Supabase
          </h3>
          <p style={{ fontSize: '0.85rem', color: 'var(--text-secondary)', marginBottom: '10px' }}>
            In your Supabase project dashboard, navigate to <strong>SQL Editor</strong>, paste this script, and click <strong>Run</strong>:
          </p>
          <div style={{ position: 'relative' }}>
            <pre style={{
              background: '#0f172a',
              color: '#38bdf8',
              padding: '14px',
              borderRadius: 'var(--radius-md)',
              fontSize: '0.75rem',
              overflowX: 'auto',
              maxHeight: '160px',
              fontFamily: 'monospace',
            }}>
              {sqlSnippet}
            </pre>
            <button
              onClick={copySql}
              style={{
                position: 'absolute',
                top: '8px',
                right: '8px',
                background: 'rgba(255,255,255,0.15)',
                color: '#ffffff',
                padding: '4px 10px',
                borderRadius: '6px',
                fontSize: '0.75rem',
                display: 'flex',
                alignItems: 'center',
                gap: '4px',
              }}
            >
              {copied ? <Check size={14} color="#4ade80" /> : <Copy size={14} />}
              {copied ? 'Copied!' : 'Copy SQL'}
            </button>
          </div>
        </div>

        {/* Step 2 */}
        <div style={{
          background: '#f8fafc',
          border: '1px solid var(--border)',
          borderRadius: 'var(--radius-md)',
          padding: '16px',
          marginBottom: '16px',
        }}>
          <h3 style={{ fontSize: '0.95rem', fontWeight: 700, color: 'var(--primary)', marginBottom: '6px', display: 'flex', alignItems: 'center', gap: '6px' }}>
            <KeyRound size={16} />
            <span>Step 2:</span> Copy API Keys to <code>.env</code>
          </h3>
          <p style={{ fontSize: '0.85rem', color: 'var(--text-secondary)', lineHeight: 1.5 }}>
            Go to <strong>Project Settings → API</strong> in your Supabase dashboard:
          </p>
          <ul style={{ fontSize: '0.85rem', color: 'var(--text-secondary)', marginLeft: '20px', marginTop: '6px', lineHeight: 1.6 }}>
            <li>Copy <strong>Project URL</strong> → paste into <code>VITE_SUPABASE_URL</code> in <code>web/.env</code></li>
            <li>Copy <strong>anon public API key</strong> → paste into <code>VITE_SUPABASE_ANON_KEY</code> in <code>web/.env</code></li>
          </ul>
        </div>

        {/* Step 3 */}
        <div style={{
          background: '#f8fafc',
          border: '1px solid var(--border)',
          borderRadius: 'var(--radius-md)',
          padding: '16px',
          marginBottom: '20px',
        }}>
          <h3 style={{ fontSize: '0.95rem', fontWeight: 700, color: 'var(--primary)', marginBottom: '6px', display: 'flex', alignItems: 'center', gap: '6px' }}>
            <ShieldCheck size={16} />
            <span>Step 3:</span> Free Mobile + Password Auth (No SMS Cost)
          </h3>
          <p style={{ fontSize: '0.85rem', color: 'var(--text-secondary)', lineHeight: 1.5 }}>
            Under <strong>Authentication → Providers → Email</strong>, ensure Email is enabled and uncheck <em>"Confirm email"</em> so citizen accounts can log in immediately upon registration without needing external SMTP or paid SMS OTP gateways.
          </p>
        </div>

        <button className="btn-primary" style={{ width: '100%' }} onClick={onClose}>
          Got it, Close Guide
        </button>
      </div>
    </div>
  );
}
