import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { toast } from 'react-hot-toast';
import { useAuth } from '../context/AuthContext';
import './AuthPage.css';

export default function RegisterPage() {
  const { register } = useAuth();
  const navigate = useNavigate();
  const [form, setForm] = useState({ name: '', email: '', phone: '', password: '', confirm: '' });
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    if (form.password !== form.confirm) {
      const msg = 'Passwords do not match.';
      setError(msg);
      toast.error(msg);
      return;
    }
    if (!form.name.trim()) {
      const msg = 'Full name is required.';
      setError(msg);
      toast.error(msg);
      return;
    }
    if (!/^(?=.*[A-Z])(?=.*[0-9])(?=.*[^A-Za-z0-9]).{8,}$/.test(form.password)) {
      const msg = 'Password must be at least 8 characters and include uppercase, number, and special character.';
      setError(msg);
      toast.error(msg);
      return;
    }
    if (form.phone && !/^[6-9]\d{9}$/.test(form.phone.replace(/\D/g, ''))) {
      const msg = 'Phone number must be a valid 10 digit Indian mobile number.';
      setError(msg);
      toast.error(msg);
      return;
    }
    setLoading(true);
    try {
      await register(form.name, form.email, form.password, form.phone);
      toast.success('Account created successfully!');
      navigate('/');
    } catch (err) {
      const msg = err.response?.data?.message || 'Registration failed. Please try again.';
      setError(msg);
      toast.error(msg);
    } finally {
      setLoading(false);
    }
  };

  const set = (k) => (e) => setForm((f) => ({ ...f, [k]: e.target.value }));

  const strength = (pw) => {
    let s = 0;
    if (pw.length >= 8) s++;
    if (/[A-Z]/.test(pw)) s++;
    if (/[0-9]/.test(pw)) s++;
    if (/[^A-Za-z0-9]/.test(pw)) s++;
    return s;
  };
  const pwStrength = strength(form.password);
  const strengthLabels = ['', 'Weak', 'Fair', 'Good', 'Strong'];
  const strengthColors = ['', '#ef4444', '#f59e0b', '#06b6d4', '#10b981'];

  return (
    <div className="auth-page">
      <div className="auth-bg">
        <div className="auth-orb auth-orb-1" />
        <div className="auth-orb auth-orb-2" />
      </div>

      <div className="auth-card glass animate-fadeInUp">
        <div className="auth-logo">
          <span className="auth-logo-icon">🚂</span>
          <span className="auth-logo-text">RailConnect</span>
        </div>

        <h1 className="auth-title">Create account</h1>
        <p className="auth-subtitle">Join millions of smart travellers today</p>

        {error && (
          <div className="alert alert-error">
            <span>⚠️</span> {error}
          </div>
        )}

        <form onSubmit={handleSubmit} className="auth-form">
          <div className="form-group">
            <label className="form-label">Full Name</label>
            <input
              id="reg-name"
              className="form-input"
              placeholder="Rahul Sharma"
              value={form.name}
              onChange={set('name')}
              required
              autoFocus
            />
          </div>

          <div className="form-group">
            <label className="form-label">Email Address</label>
            <input
              id="reg-email"
              type="email"
              className="form-input"
              placeholder="you@example.com"
              value={form.email}
              onChange={set('email')}
              required
            />
          </div>

          <div className="form-group">
            <label className="form-label">Phone Number</label>
            <input
              id="reg-phone"
              type="tel"
              className="form-input"
              placeholder="9876543210"
              value={form.phone}
              onChange={set('phone')}
              pattern="[6-9][0-9]{9}"
            />
          </div>

          <div className="form-group">
            <label className="form-label">Password</label>
            <input
              id="reg-password"
              type="password"
              className="form-input"
              placeholder="At least 8 characters"
              value={form.password}
              onChange={set('password')}
              required
            />
            {form.password && (
              <div className="pw-strength">
                <div className="pw-strength-bar">
                  {[1,2,3,4].map((n) => (
                    <div
                      key={n}
                      className="pw-bar-seg"
                      style={{ background: n <= pwStrength ? strengthColors[pwStrength] : undefined }}
                    />
                  ))}
                </div>
                <span className="pw-label" style={{ color: strengthColors[pwStrength] }}>
                  {strengthLabels[pwStrength]}
                </span>
              </div>
            )}
          </div>

          <div className="form-group">
            <label className="form-label">Confirm Password</label>
            <input
              id="reg-confirm"
              type="password"
              className="form-input"
              placeholder="Re-enter your password"
              value={form.confirm}
              onChange={set('confirm')}
              required
            />
          </div>

          <button id="reg-submit" type="submit" className="btn btn-primary btn-lg w-full" disabled={loading}>
            {loading ? <><span className="spinner" /> Creating account...</> : 'Create Account'}
          </button>
        </form>

        <p className="auth-switch">
          Already have an account?{' '}
          <Link to="/login" className="auth-link">Sign in →</Link>
        </p>
      </div>
    </div>
  );
}
