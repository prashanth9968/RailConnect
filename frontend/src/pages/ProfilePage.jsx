import { useAuth } from '../context/AuthContext';
import { useNavigate } from 'react-router-dom';
import './ProfilePage.css';

export default function ProfilePage() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  const handleLogout = async () => {
    await logout();
    navigate('/login');
  };

  if (!user) return null;

  return (
    <div className="profile-page page-enter">
      <div className="container">
        <h1 className="profile-title">My Profile</h1>

        <div className="profile-layout">
          {/* Profile Card */}
          <div className="card profile-card">
            <div className="profile-avatar-ring">
              <div className="profile-avatar">
                {user.name?.charAt(0).toUpperCase()}
              </div>
            </div>
            <div className="profile-name">{user.name}</div>
            <div className="profile-email">{user.email}</div>
            <div className="profile-role">
              <span className={`badge ${user.role === 'ADMIN' ? 'badge-warning' : 'badge-primary'}`}>
                {user.role === 'ADMIN' ? '🛡️ Admin' : '👤 Passenger'}
              </span>
            </div>
          </div>

          {/* Info & Actions */}
          <div className="profile-details">
            <div className="card profile-info-card">
              <h2>Account Information</h2>
              <div className="profile-fields">
                <div className="profile-field">
                  <span className="pf-label">Full Name</span>
                  <span className="pf-value">{user.name}</span>
                </div>
                <div className="profile-field">
                  <span className="pf-label">Email Address</span>
                  <span className="pf-value">{user.email}</span>
                </div>
                <div className="profile-field">
                  <span className="pf-label">Account Role</span>
                  <span className="pf-value">{user.role}</span>
                </div>
                <div className="profile-field">
                  <span className="pf-label">Account ID</span>
                  <span className="pf-value pf-mono">{user.id || '—'}</span>
                </div>
              </div>
            </div>

            <div className="card profile-actions-card">
              <h2>Quick Actions</h2>
              <div className="profile-action-list">
                <button className="profile-action-btn" onClick={() => navigate('/my-bookings')}>
                  <span className="pa-icon">🎫</span>
                  <div>
                    <div className="pa-title">My Bookings</div>
                    <div className="pa-desc">View and manage your tickets</div>
                  </div>
                  <span className="pa-arrow">→</span>
                </button>
                <button className="profile-action-btn" onClick={() => navigate('/search')}>
                  <span className="pa-icon">🚂</span>
                  <div>
                    <div className="pa-title">Book a Train</div>
                    <div className="pa-desc">Search and book tickets</div>
                  </div>
                  <span className="pa-arrow">→</span>
                </button>
                <button className="profile-action-btn" onClick={() => navigate('/track')}>
                  <span className="pa-icon">📍</span>
                  <div>
                    <div className="pa-title">Track Train</div>
                    <div className="pa-desc">Live GPS train tracking</div>
                  </div>
                  <span className="pa-arrow">→</span>
                </button>
                <button className="profile-action-btn danger" onClick={handleLogout}>
                  <span className="pa-icon">🚪</span>
                  <div>
                    <div className="pa-title">Sign Out</div>
                    <div className="pa-desc">Log out of your account</div>
                  </div>
                  <span className="pa-arrow">→</span>
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
