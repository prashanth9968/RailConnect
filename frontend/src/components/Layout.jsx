import { Outlet, NavLink, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { useState } from 'react';
import './Layout.css';

const TrainIcon = () => (
  <svg width="28" height="28" viewBox="0 0 28 28" fill="none">
    <rect width="28" height="28" rx="8" fill="url(#lg)"/>
    <defs>
      <linearGradient id="lg" x1="0" y1="0" x2="28" y2="28">
        <stop offset="0%" stopColor="#6366f1"/>
        <stop offset="100%" stopColor="#8b5cf6"/>
      </linearGradient>
    </defs>
    <rect x="5" y="9" width="18" height="12" rx="3" fill="white" opacity="0.9"/>
    <rect x="7" y="11" width="6" height="5" rx="1.5" fill="#6366f1"/>
    <rect x="15" y="11" width="6" height="5" rx="1.5" fill="#6366f1"/>
    <rect x="5" y="18" width="18" height="2" rx="1" fill="white" opacity="0.9"/>
    <circle cx="9" cy="22" r="2" fill="#6366f1"/>
    <circle cx="19" cy="22" r="2" fill="#6366f1"/>
    <rect x="11" y="6" width="6" height="4" rx="1" fill="white" opacity="0.7"/>
  </svg>
);

export default function Layout() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const [menuOpen, setMenuOpen] = useState(false);

  const handleLogout = async () => {
    await logout();
    navigate('/login');
  };

  const navLinks = [
    { to: '/', label: 'Home', end: true },
    { to: '/search', label: 'Find Trains' },
    { to: '/pnr', label: 'PNR Status' },
    { to: '/track', label: 'Track Train' },
    ...(user ? [{ to: '/my-bookings', label: 'My Bookings' }] : []),
  ];

  return (
    <div className="app-shell">
      {/* Navigation Bar */}
      <nav className="navbar glass">
        <div className="container navbar-inner">
          <NavLink to="/" className="navbar-brand">
            <TrainIcon />
            <span className="brand-name">Rail<span className="brand-accent">Connect</span></span>
          </NavLink>

          <div className={`navbar-links ${menuOpen ? 'open' : ''}`}>
            {navLinks.map((link) => (
              <NavLink
                key={link.to}
                to={link.to}
                end={link.end}
                className={({ isActive }) => `nav-link ${isActive ? 'active' : ''}`}
                onClick={() => setMenuOpen(false)}
              >
                {link.label}
              </NavLink>
            ))}
          </div>

          <div className="navbar-actions">
            {user ? (
              <div className="user-menu">
                <button className="user-avatar" onClick={() => navigate('/profile')}>
                  {user.name?.charAt(0).toUpperCase()}
                </button>
                <div className="user-dropdown">
                  <div className="user-info">
                    <div className="user-name">{user.name}</div>
                    <div className="user-email">{user.email}</div>
                  </div>
                  <div className="dropdown-divider" />
                  <button className="dropdown-item" onClick={() => navigate('/profile')}>
                    👤 Profile
                  </button>
                  <button className="dropdown-item" onClick={() => navigate('/my-bookings')}>
                    🎫 My Bookings
                  </button>
                  <div className="dropdown-divider" />
                  <button className="dropdown-item danger" onClick={handleLogout}>
                    🚪 Sign Out
                  </button>
                </div>
              </div>
            ) : (
              <div className="auth-buttons">
                <NavLink to="/login" className="btn btn-ghost btn-sm">Login</NavLink>
                <NavLink to="/register" className="btn btn-primary btn-sm">Sign Up</NavLink>
              </div>
            )}

            <button
              className="mobile-menu-btn"
              onClick={() => setMenuOpen(!menuOpen)}
              aria-label="Toggle menu"
            >
              <span /><span /><span />
            </button>
          </div>
        </div>
      </nav>

      {/* Main Content */}
      <main className="main-content">
        <Outlet />
      </main>

      {/* Footer */}
      <footer className="footer">
        <div className="container footer-inner">
          <div className="footer-brand">
            <TrainIcon />
            <span className="brand-name" style={{ fontSize: '1.125rem' }}>
              Rail<span className="brand-accent">Connect</span>
            </span>
          </div>
          <p className="footer-copy">
            © 2024 RailConnect · Smart Railway Booking System
          </p>
          <div className="footer-links">
            <span>Secure Payments</span>
            <span>·</span>
            <span>Real-time Tracking</span>
            <span>·</span>
            <span>Instant Confirmation</span>
          </div>
        </div>
      </footer>
    </div>
  );
}
