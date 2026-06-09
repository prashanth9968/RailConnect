import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import './HomePage.css';

const POPULAR_ROUTES = [
  { from: 'NDLS', to: 'MMCT', fromName: 'New Delhi', toName: 'Mumbai Central', trains: '42 trains' },
  { from: 'NDLS', to: 'HWH', fromName: 'New Delhi', toName: 'Howrah', trains: '28 trains' },
  { from: 'MAS', to: 'MMCT', fromName: 'Chennai Central', toName: 'Mumbai Central', trains: '18 trains' },
  { from: 'BLR', to: 'HWH', fromName: 'Bangalore', toName: 'Howrah', trains: '14 trains' },
  { from: 'PNBE', to: 'NDLS', fromName: 'Patna', toName: 'New Delhi', trains: '22 trains' },
  { from: 'JP', to: 'MAS', fromName: 'Jaipur', toName: 'Chennai Central', trains: '9 trains' },
];

const FEATURES = [
  { icon: '⚡', title: 'Instant Booking', desc: 'Book tickets in under 60 seconds with seamless payment via GPay, PhonePe or cards.' },
  { icon: '📍', title: 'Live GPS Tracking', desc: 'Track your train in real-time on a map — know the exact location at any moment.' },
  { icon: '🔔', title: 'Smart Alerts', desc: 'Get notified about delays, platform changes, and booking confirmations instantly.' },
  { icon: '🎫', title: 'PNR Status', desc: 'Check PNR status anytime — confirm waitlist position and seat allocation.' },
  { icon: '💳', title: 'Razorpay Powered', desc: 'Industry-grade payment security with UPI, cards, and net banking support.' },
  { icon: '🔄', title: 'Easy Cancellation', desc: 'Cancel bookings with one tap and get automatic refunds processed instantly.' },
];

export default function HomePage() {
  const navigate = useNavigate();
  const [from, setFrom] = useState('');
  const [to, setTo] = useState('');
  const [date, setDate] = useState(new Date().toISOString().split('T')[0]);
  const [activeTab, setActiveTab] = useState('book');
  const [pnr, setPnr] = useState('');
  const [trainNum, setTrainNum] = useState('');

  const handleSearch = (e) => {
    e.preventDefault();
    if (!from.trim() || !to.trim()) return;
    navigate(`/search?from=${from.toUpperCase()}&to=${to.toUpperCase()}&date=${date}`);
  };

  const handlePnrCheck = (e) => {
    e.preventDefault();
    if (!pnr.trim()) return;
    navigate(`/pnr?pnr=${pnr}`);
  };

  const handleTrackTrain = (e) => {
    e.preventDefault();
    if (!trainNum.trim()) return;
    navigate(`/track?train=${trainNum}`);
  };

  const swap = () => { setFrom(to); setTo(from); };

  const today = new Date().toISOString().split('T')[0];

  return (
    <div className="home-page">
      {/* Hero Section */}
      <section className="hero">
        <div className="hero-bg">
          <div className="hero-orb hero-orb-1" />
          <div className="hero-orb hero-orb-2" />
          <div className="hero-orb hero-orb-3" />
        </div>

        <div className="container hero-content">
          <div className="hero-badge animate-fadeInUp">
            <span className="badge badge-primary">🚂 India's Smartest Rail App</span>
          </div>

          <h1 className="hero-title animate-fadeInUp" style={{ animationDelay: '0.1s' }}>
            Book Trains,<br />
            <span className="gradient-text">Track Journeys</span>
          </h1>

          <p className="hero-subtitle animate-fadeInUp" style={{ animationDelay: '0.2s' }}>
            Search, book, and track trains across India — all in one place.<br />
            Real-time GPS tracking, instant PNR updates & secure payments.
          </p>

          {/* Search Widget */}
          <div className="search-widget glass animate-fadeInUp" style={{ animationDelay: '0.3s' }}>
            {/* Tabs */}
            <div className="widget-tabs">
              <button
                className={`widget-tab ${activeTab === 'book' ? 'active' : ''}`}
                onClick={() => setActiveTab('book')}
              >
                🎫 Book Ticket
              </button>
              <button
                className={`widget-tab ${activeTab === 'pnr' ? 'active' : ''}`}
                onClick={() => setActiveTab('pnr')}
              >
                🔍 PNR Status
              </button>
              <button
                className={`widget-tab ${activeTab === 'track' ? 'active' : ''}`}
                onClick={() => setActiveTab('track')}
              >
                📍 Track Train
              </button>
            </div>

            {/* Book Form */}
            {activeTab === 'book' && (
              <form className="widget-form" onSubmit={handleSearch}>
                <div className="search-row">
                  <div className="form-group" style={{ flex: 1 }}>
                    <label className="form-label">From Station</label>
                    <div className="station-input-wrap">
                      <span className="station-icon">🏫</span>
                      <input
                        className="form-input"
                        placeholder="e.g. NDLS, New Delhi"
                        value={from}
                        onChange={(e) => setFrom(e.target.value)}
                        required
                      />
                    </div>
                  </div>

                  <button type="button" className="swap-btn" onClick={swap} title="Swap stations">
                    ⇄
                  </button>

                  <div className="form-group" style={{ flex: 1 }}>
                    <label className="form-label">To Station</label>
                    <div className="station-input-wrap">
                      <span className="station-icon">📍</span>
                      <input
                        className="form-input"
                        placeholder="e.g. MMCT, Mumbai"
                        value={to}
                        onChange={(e) => setTo(e.target.value)}
                        required
                      />
                    </div>
                  </div>

                  <div className="form-group" style={{ width: '180px' }}>
                    <label className="form-label">Journey Date</label>
                    <input
                      type="date"
                      className="form-input"
                      value={date}
                      min={today}
                      onChange={(e) => setDate(e.target.value)}
                      required
                    />
                  </div>

                  <button type="submit" className="btn btn-primary btn-lg search-btn">
                    Search Trains
                  </button>
                </div>
              </form>
            )}

            {/* PNR Form */}
            {activeTab === 'pnr' && (
              <form className="widget-form" onSubmit={handlePnrCheck}>
                <div className="pnr-row">
                  <div className="form-group" style={{ flex: 1 }}>
                    <label className="form-label">PNR Number</label>
                    <input
                      className="form-input"
                      placeholder="Enter 10-digit PNR number"
                      value={pnr}
                      onChange={(e) => setPnr(e.target.value)}
                      maxLength={10}
                      required
                    />
                  </div>
                  <button type="submit" className="btn btn-primary btn-lg" style={{ alignSelf: 'flex-end' }}>
                    Check Status
                  </button>
                </div>
              </form>
            )}

            {/* Track Form */}
            {activeTab === 'track' && (
              <form className="widget-form" onSubmit={handleTrackTrain}>
                <div className="pnr-row">
                  <div className="form-group" style={{ flex: 1 }}>
                    <label className="form-label">Train Number</label>
                    <input
                      className="form-input"
                      placeholder="e.g. 12951 (Mumbai Rajdhani)"
                      value={trainNum}
                      onChange={(e) => setTrainNum(e.target.value)}
                      required
                    />
                  </div>
                  <button type="submit" className="btn btn-primary btn-lg" style={{ alignSelf: 'flex-end' }}>
                    Track Now
                  </button>
                </div>
              </form>
            )}
          </div>

          {/* Stats */}
          <div className="hero-stats animate-fadeInUp" style={{ animationDelay: '0.4s' }}>
            <div className="stat-item">
              <span className="stat-number">13,000+</span>
              <span className="stat-label">Trains</span>
            </div>
            <div className="stat-divider" />
            <div className="stat-item">
              <span className="stat-number">8,000+</span>
              <span className="stat-label">Stations</span>
            </div>
            <div className="stat-divider" />
            <div className="stat-item">
              <span className="stat-number">99.9%</span>
              <span className="stat-label">Uptime</span>
            </div>
            <div className="stat-divider" />
            <div className="stat-item">
              <span className="stat-number">₹0</span>
              <span className="stat-label">Booking Fee</span>
            </div>
          </div>
        </div>
      </section>

      {/* Features Section */}
      <section className="section features-section">
        <div className="container">
          <div style={{ textAlign: 'center', marginBottom: '48px' }}>
            <h2 className="section-title">Everything You Need</h2>
            <p className="section-subtitle">A complete platform built for the modern Indian traveler</p>
          </div>
          <div className="grid-3">
            {FEATURES.map((f, i) => (
              <div key={i} className="card feature-card animate-fadeInUp" style={{ animationDelay: `${i * 0.08}s` }}>
                <div className="feature-icon">{f.icon}</div>
                <h3 className="feature-title">{f.title}</h3>
                <p className="feature-desc">{f.desc}</p>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* Popular Routes */}
      <section className="section routes-section">
        <div className="container">
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '32px' }}>
            <div>
              <h2 className="section-title">Popular Routes</h2>
              <p className="section-subtitle" style={{ margin: 0 }}>Most booked routes today</p>
            </div>
          </div>
          <div className="grid-3">
            {POPULAR_ROUTES.map((route, i) => (
              <button
                key={i}
                className="card route-card"
                onClick={() => navigate(`/search?from=${route.from}&to=${route.to}&date=${today}`)}
              >
                <div className="route-path">
                  <div className="route-station">
                    <span className="route-code">{route.from}</span>
                    <span className="route-name">{route.fromName}</span>
                  </div>
                  <div className="route-arrow">→</div>
                  <div className="route-station">
                    <span className="route-code">{route.to}</span>
                    <span className="route-name">{route.toName}</span>
                  </div>
                </div>
                <div className="route-meta">
                  <span className="badge badge-primary">{route.trains}</span>
                </div>
              </button>
            ))}
          </div>
        </div>
      </section>

      {/* CTA */}
      <section className="section cta-section">
        <div className="container">
          <div className="cta-card glass">
            <div className="cta-orb" />
            <div className="cta-content">
              <h2>Ready to Travel Smarter?</h2>
              <p>Join thousands of travellers who book their trains with RailConnect every day.</p>
              <div className="cta-actions">
                <button className="btn btn-primary btn-lg" onClick={() => navigate('/register')}>
                  Create Free Account
                </button>
                <button className="btn btn-secondary btn-lg" onClick={() => navigate('/search')}>
                  Search Trains
                </button>
              </div>
            </div>
          </div>
        </div>
      </section>
    </div>
  );
}
