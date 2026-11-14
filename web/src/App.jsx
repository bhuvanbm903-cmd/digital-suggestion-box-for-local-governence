import React, { useState, useEffect } from 'react';
import Navbar from './components/Navbar';
import Dashboard from './components/Dashboard';
import AuthModal from './components/AuthModal';
import NewSuggestionModal from './components/NewSuggestionModal';
import AdminTriageModal from './components/AdminTriageModal';
import SupabaseGuideModal from './components/SupabaseGuideModal';
import {
  getActiveSession,
  logoutUser,
  fetchSuggestions,
  submitSuggestion,
  updateSuggestionStatus,
  isSupabaseConfigured,
} from './lib/supabase';
import { AlertTriangle, Database } from 'lucide-react';

export default function App() {
  const [user, setUser] = useState(null);
  const [loadingUser, setLoadingUser] = useState(true);
  const [suggestions, setSuggestions] = useState([]);
  const [activeView, setActiveView] = useState('citizen'); // 'citizen' | 'admin'

  const [authModalOpen, setAuthModalOpen] = useState(false);
  const [newSuggestionOpen, setNewSuggestionOpen] = useState(false);
  const [triageItem, setTriageItem] = useState(null);
  const [guideModalOpen, setGuideModalOpen] = useState(false);

  // 1. Session Persistence across page refresh
  useEffect(() => {
    const initSession = async () => {
      try {
        const session = await getActiveSession();
        setUser(session);
      } catch (err) {
        console.error('Failed to load session:', err);
      } finally {
        setLoadingUser(false);
      }
    };
    initSession();
  }, []);

  // 2. Load suggestions when user or active view changes
  useEffect(() => {
    loadSuggestions();
  }, [user, activeView]);

  const loadSuggestions = async () => {
    if (!user) {
      setSuggestions([]);
      return;
    }
    try {
      const data = await fetchSuggestions(user.id, activeView);
      setSuggestions(data);
    } catch (err) {
      console.error('Failed to fetch suggestions:', err);
    }
  };

  const handleLogout = async () => {
    await logoutUser();
    setUser(null);
    setSuggestions([]);
  };

  const handleCreateSuggestion = async (formData) => {
    const created = await submitSuggestion(formData, user);
    setSuggestions((prev) => [created, ...prev]);
  };

  const handleUpdateStatus = async (id, status, responseText) => {
    const updated = await updateSuggestionStatus(id, status, responseText);
    setSuggestions((prev) =>
      prev.map((item) => (item.id === id ? { ...item, status, admin_response: responseText } : item))
    );
  };

  if (loadingUser) {
    return (
      <div style={{
        minHeight: '100vh',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        background: 'var(--bg-primary)',
        color: 'var(--text-secondary)',
        fontSize: '1rem',
        fontWeight: 600,
      }}>
        Initializing SuggestBox...
      </div>
    );
  }

  const isConfigured = isSupabaseConfigured();

  return (
    <div style={{ minHeight: '100vh', display: 'flex', flexDirection: 'column' }}>
      {/* Notice Banner when running in Local Demo Mode */}
      {!isConfigured && (
        <div style={{
          background: '#fffbeb',
          borderBottom: '1px solid #fef3c7',
          padding: '10px 20px',
          fontSize: '0.85rem',
          color: '#92400e',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          gap: '8px',
          flexWrap: 'wrap',
        }}>
          <AlertTriangle size={16} />
          <span>
            <strong>Local Demo Active:</strong> You can test Mobile Number registration and login right now with demo storage. To connect live Supabase PostgreSQL and RLS:
          </span>
          <button
            onClick={() => setGuideModalOpen(true)}
            style={{
              fontWeight: 700,
              textDecoration: 'underline',
              color: '#b45309',
              display: 'inline-flex',
              alignItems: 'center',
              gap: '4px',
            }}
          >
            <Database size={14} /> Open Supabase Setup Guide
          </button>
        </div>
      )}

      {/* Navigation Bar */}
      <Navbar
        user={user}
        onLogout={handleLogout}
        onOpenAuth={() => setAuthModalOpen(true)}
        onOpenGuide={() => setGuideModalOpen(true)}
        activeView={activeView}
        setActiveView={setActiveView}
      />

      {/* Main Dashboard */}
      <Dashboard
        user={user}
        suggestions={suggestions}
        onOpenNewSuggestion={() => {
          if (!user) {
            setAuthModalOpen(true);
          } else {
            setNewSuggestionOpen(true);
          }
        }}
        onOpenAuth={() => setAuthModalOpen(true)}
        activeView={activeView}
        onTriageSuggestion={(item) => setTriageItem(item)}
      />

      {/* Modals */}
      <AuthModal
        isOpen={authModalOpen}
        onClose={() => setAuthModalOpen(false)}
        onAuthSuccess={(authedUser) => setUser(authedUser)}
      />

      <NewSuggestionModal
        isOpen={newSuggestionOpen}
        onClose={() => setNewSuggestionOpen(false)}
        onSubmitSuggestion={handleCreateSuggestion}
      />

      <AdminTriageModal
        isOpen={!!triageItem}
        onClose={() => setTriageItem(null)}
        suggestion={triageItem}
        onUpdateStatus={handleUpdateStatus}
      />

      <SupabaseGuideModal
        isOpen={guideModalOpen}
        onClose={() => setGuideModalOpen(false)}
      />
    </div>
  );
}
