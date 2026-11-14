import React, { useState } from 'react';
import { X, Eye, EyeOff, Phone, Lock, User, AlertCircle, CheckCircle2 } from 'lucide-react';
import { loginWithMobile, registerWithMobile, normalizeMobile } from '../lib/supabase';

export default function AuthModal({ isOpen, onClose, onAuthSuccess }) {
  const [mode, setMode] = useState('login'); // 'login' | 'register' | 'forgot'
  const [fullName, setFullName] = useState('');
  const [mobileNumber, setMobileNumber] = useState('');
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);
  const [loading, setLoading] = useState(false);
  const [errorMessage, setErrorMessage] = useState('');
  const [successMessage, setSuccessMessage] = useState('');

  if (!isOpen) return null;

  const handleMobileChange = (e) => {
    // Only allow digits, max 10
    const raw = e.target.value.replace(/\D/g, '').slice(0, 10);
    setMobileNumber(raw);
    setErrorMessage('');
  };

  const resetForm = () => {
    setFullName('');
    setMobileNumber('');
    setPassword('');
    setConfirmPassword('');
    setErrorMessage('');
    setSuccessMessage('');
  };

  const switchMode = (newMode) => {
    resetForm();
    setMode(newMode);
  };

  const handleLogin = async (e) => {
    e.preventDefault();
    setErrorMessage('');
    const { valid } = normalizeMobile(mobileNumber);
    if (!valid) {
      setErrorMessage('Please enter a valid 10-digit Indian mobile number starting with 6-9.');
      return;
    }
    if (!password) {
      setErrorMessage('Please enter your password.');
      return;
    }

    setLoading(true);
    try {
      const user = await loginWithMobile({ mobileNumber, password });
      onAuthSuccess(user);
      onClose();
      resetForm();
    } catch (err) {
      setErrorMessage(err.message || 'Login failed. Please verify credentials.');
    } finally {
      setLoading(false);
    }
  };

  const handleRegister = async (e) => {
    e.preventDefault();
    setErrorMessage('');
    const { valid } = normalizeMobile(mobileNumber);
    if (!valid) {
      setErrorMessage('Please enter a valid 10-digit Indian mobile number (e.g. 9845012345).');
      return;
    }
    if (!fullName || fullName.trim().length < 2) {
      setErrorMessage('Please enter your full name (minimum 2 characters).');
      return;
    }
    if (!password || password.length < 6) {
      setErrorMessage('Password must be at least 6 characters long.');
      return;
    }
    if (password !== confirmPassword) {
      setErrorMessage('Passwords do not match. Please re-enter confirm password.');
      return;
    }

    setLoading(true);
    try {
      const user = await registerWithMobile({
        mobileNumber,
        password,
        fullName: fullName.trim(),
      });
      setSuccessMessage('Account created successfully! Redirecting...');
      setTimeout(() => {
        onAuthSuccess(user);
        onClose();
        resetForm();
      }, 500);
    } catch (err) {
      setErrorMessage(err.message || 'Registration failed. Please try again.');
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
        maxWidth: '440px',
        width: '100%',
        padding: '32px',
        boxShadow: 'var(--shadow-lg)',
        position: 'relative',
        animation: 'modalFadeIn 0.2s ease',
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

        {/* Modal Header */}
        <div style={{ textAlign: 'center', marginBottom: '24px' }}>
          <div style={{
            width: '48px',
            height: '48px',
            borderRadius: '14px',
            background: 'var(--primary-light)',
            color: 'var(--primary)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            margin: '0 auto 12px auto',
          }}>
            <Phone size={24} />
          </div>
          <h2 style={{ fontSize: '1.4rem', fontWeight: 800, color: 'var(--text-primary)' }}>
            {mode === 'login' ? 'Citizen Sign In' : mode === 'register' ? 'Create Citizen Account' : 'Reset Password'}
          </h2>
          <p style={{ fontSize: '0.875rem', color: 'var(--text-secondary)', marginTop: '4px' }}>
            {mode === 'login'
              ? 'Enter your registered mobile number and password'
              : mode === 'register'
              ? 'Join your local ward governance participatory platform'
              : 'Contact your ward administrative desk for credentials'}
          </p>
        </div>

        {/* Alert Banners */}
        {errorMessage && (
          <div style={{
            background: 'var(--danger-light)',
            color: 'var(--danger)',
            padding: '10px 14px',
            borderRadius: 'var(--radius-md)',
            fontSize: '0.85rem',
            fontWeight: 500,
            display: 'flex',
            alignItems: 'center',
            gap: '8px',
            marginBottom: '16px',
          }}>
            <AlertCircle size={18} style={{ flexShrink: 0 }} />
            <span>{errorMessage}</span>
          </div>
        )}

        {successMessage && (
          <div style={{
            background: 'var(--success-light)',
            color: 'var(--success)',
            padding: '10px 14px',
            borderRadius: 'var(--radius-md)',
            fontSize: '0.85rem',
            fontWeight: 500,
            display: 'flex',
            alignItems: 'center',
            gap: '8px',
            marginBottom: '16px',
          }}>
            <CheckCircle2 size={18} style={{ flexShrink: 0 }} />
            <span>{successMessage}</span>
          </div>
        )}

        {/* Login Form */}
        {mode === 'login' && (
          <form onSubmit={handleLogin}>
            {/* Mobile Number */}
            <div className="input-group">
              <label className="input-label">Indian Mobile Number</label>
              <div className="input-wrapper">
                <span style={{
                  position: 'absolute',
                  left: '12px',
                  fontWeight: 600,
                  color: 'var(--text-secondary)',
                  fontSize: '0.95rem',
                  display: 'flex',
                  alignItems: 'center',
                  gap: '4px',
                  pointerEvents: 'none',
                  zIndex: 2,
                }}>
                  🇮🇳 +91
                </span>
                <input
                  type="tel"
                  className="input-field"
                  placeholder="98450 12345"
                  value={mobileNumber}
                  onChange={handleMobileChange}
                  style={{ paddingLeft: '72px' }}
                  autoFocus
                  required
                />
              </div>
            </div>

            {/* Password */}
            <div className="input-group">
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <label className="input-label">Password</label>
                <button
                  type="button"
                  onClick={() => switchMode('forgot')}
                  style={{ fontSize: '0.75rem', color: 'var(--primary)', fontWeight: 600 }}
                >
                  Forgot Password?
                </button>
              </div>
              <div className="input-wrapper">
                <Lock size={18} style={{ position: 'absolute', left: '12px', color: 'var(--text-tertiary)' }} />
                <input
                  type={showPassword ? 'text' : 'password'}
                  className="input-field"
                  placeholder="••••••••"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  style={{ paddingLeft: '40px', paddingRight: '40px' }}
                  required
                />
                <button
                  type="button"
                  onClick={() => setShowPassword(!showPassword)}
                  style={{
                    position: 'absolute',
                    right: '12px',
                    color: 'var(--text-tertiary)',
                  }}
                >
                  {showPassword ? <EyeOff size={18} /> : <Eye size={18} />}
                </button>
              </div>
            </div>

            <button
              type="submit"
              className="btn-primary"
              style={{ width: '100%', marginTop: '8px', padding: '14px' }}
              disabled={loading}
            >
              {loading ? 'Authenticating...' : 'Sign In to Ward'}
            </button>

            {/* Switch to Register */}
            <div style={{ textAlign: 'center', marginTop: '20px', fontSize: '0.875rem', color: 'var(--text-secondary)' }}>
              New to SuggestBox?{' '}
              <button
                type="button"
                onClick={() => switchMode('register')}
                style={{ color: 'var(--primary)', fontWeight: 700 }}
              >
                Create Account
              </button>
            </div>
          </form>
        )}

        {/* Register Form */}
        {mode === 'register' && (
          <form onSubmit={handleRegister}>
            {/* Full Name */}
            <div className="input-group">
              <label className="input-label">Full Name</label>
              <div className="input-wrapper">
                <User size={18} style={{ position: 'absolute', left: '12px', color: 'var(--text-tertiary)' }} />
                <input
                  type="text"
                  className="input-field"
                  placeholder="e.g. Ramesh Gowda"
                  value={fullName}
                  onChange={(e) => setFullName(e.target.value)}
                  style={{ paddingLeft: '40px' }}
                  autoFocus
                  required
                />
              </div>
            </div>

            {/* Mobile Number */}
            <div className="input-group">
              <label className="input-label">10-Digit Mobile Number</label>
              <div className="input-wrapper">
                <span style={{
                  position: 'absolute',
                  left: '12px',
                  fontWeight: 600,
                  color: 'var(--text-secondary)',
                  fontSize: '0.95rem',
                  pointerEvents: 'none',
                }}>
                  🇮🇳 +91
                </span>
                <input
                  type="tel"
                  className="input-field"
                  placeholder="98450 12345"
                  value={mobileNumber}
                  onChange={handleMobileChange}
                  style={{ paddingLeft: '72px' }}
                  required
                />
              </div>
            </div>

            {/* Password */}
            <div className="input-group">
              <label className="input-label">Password (Min. 6 chars)</label>
              <div className="input-wrapper">
                <Lock size={18} style={{ position: 'absolute', left: '12px', color: 'var(--text-tertiary)' }} />
                <input
                  type={showPassword ? 'text' : 'password'}
                  className="input-field"
                  placeholder="Create password"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  style={{ paddingLeft: '40px', paddingRight: '40px' }}
                  required
                />
                <button
                  type="button"
                  onClick={() => setShowPassword(!showPassword)}
                  style={{ position: 'absolute', right: '12px', color: 'var(--text-tertiary)' }}
                >
                  {showPassword ? <EyeOff size={18} /> : <Eye size={18} />}
                </button>
              </div>
            </div>

            {/* Confirm Password */}
            <div className="input-group">
              <label className="input-label">Confirm Password</label>
              <div className="input-wrapper">
                <Lock size={18} style={{ position: 'absolute', left: '12px', color: 'var(--text-tertiary)' }} />
                <input
                  type={showConfirmPassword ? 'text' : 'password'}
                  className="input-field"
                  placeholder="Repeat password"
                  value={confirmPassword}
                  onChange={(e) => setConfirmPassword(e.target.value)}
                  style={{ paddingLeft: '40px', paddingRight: '40px' }}
                  required
                />
                <button
                  type="button"
                  onClick={() => setShowConfirmPassword(!showConfirmPassword)}
                  style={{ position: 'absolute', right: '12px', color: 'var(--text-tertiary)' }}
                >
                  {showConfirmPassword ? <EyeOff size={18} /> : <Eye size={18} />}
                </button>
              </div>
            </div>

            <button
              type="submit"
              className="btn-primary"
              style={{ width: '100%', marginTop: '8px', padding: '14px' }}
              disabled={loading}
            >
              {loading ? 'Creating Account...' : 'Register Account'}
            </button>

            {/* Switch to Login */}
            <div style={{ textAlign: 'center', marginTop: '20px', fontSize: '0.875rem', color: 'var(--text-secondary)' }}>
              Already registered?{' '}
              <button
                type="button"
                onClick={() => switchMode('login')}
                style={{ color: 'var(--primary)', fontWeight: 700 }}
              >
                Sign In
              </button>
            </div>
          </form>
        )}

        {/* Forgot Password View */}
        {mode === 'forgot' && (
          <div style={{ textAlign: 'center', padding: '12px 0' }}>
            <p style={{ fontSize: '0.9rem', color: 'var(--text-secondary)', marginBottom: '16px', lineHeight: 1.6 }}>
              In accordance with civic governance policies, to reset your mobile account password, please visit your local <strong>Ward 14 Administrative Helpdesk</strong> with proof of phone ownership, or contact the municipal hotline at <strong>1912</strong>.
            </p>
            <button
              className="btn-secondary"
              style={{ width: '100%' }}
              onClick={() => switchMode('login')}
            >
              Back to Sign In
            </button>
          </div>
        )}
      </div>
    </div>
  );
}
