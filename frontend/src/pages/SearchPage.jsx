import { useState, useEffect } from 'react';
import { useSearchParams, useNavigate } from 'react-router-dom';
import { trainsAPI } from '../services/api';
import './SearchPage.css';

const CLASS_LABELS = {
  SLEEPER: { label: 'SL', name: 'Sleeper', color: '#6366f1' },
  AC_3_TIER: { label: '3A', name: 'AC 3 Tier', color: '#8b5cf6' },
  AC_2_TIER: { label: '2A', name: 'AC 2 Tier', color: '#06b6d4' },
  AC_1_TIER: { label: '1A', name: 'AC First Class', color: '#f59e0b' },
  CHAIR_CAR: { label: 'CC', name: 'Chair Car', color: '#10b981' },
  EXECUTIVE: { label: 'EC', name: 'Executive', color: '#ef4444' },
};

function AvailBadge({ avail }) {
  if (!avail) return null;
  const { availableSeats, status } = avail;
  if (status === 'AVAILABLE')
    return <span className="badge badge-success">✓ {availableSeats} Avail</span>;
  if (status === 'WAITING')
    return <span className="badge badge-warning">WL {avail.waitingListCount}</span>;
  return <span className="badge badge-danger">RAC</span>;
}

function TrainCard({ train, date, onBook }) {
  const [expanded, setExpanded] = useState(false);
  const [avail, setAvail] = useState(null);
  const [loadingAvail, setLoadingAvail] = useState(false);

  const fetchAvail = async () => {
    if (avail) { setExpanded(!expanded); return; }
    setExpanded(true);
    setLoadingAvail(true);
    try {
      const res = await trainsAPI.getAvailability(train.trainId, date);
      setAvail(res.data.data);
    } catch { setAvail({}); }
    finally { setLoadingAvail(false); }
  };

  return (
    <div className="card train-card">
      <div className="train-header">
        <div className="train-info">
          <div className="train-number-badge">{train.trainNumber}</div>
          <div>
            <div className="train-name">{train.trainName}</div>
            <div className="train-type">{train.trainType?.replace(/_/g, ' ')}</div>
          </div>
        </div>
        <div className="train-route">
          <div className="route-time">
            <span className="time">{train.departureTime}</span>
            <span className="station">{train.sourceStation}</span>
          </div>
          <div className="route-duration">
            <div className="duration-line">
              <div className="duration-dot" />
              <div className="duration-track" />
              <div className="duration-dot" />
            </div>
            <span className="duration-text">{train.duration || '—'}</span>
          </div>
          <div className="route-time" style={{ textAlign: 'right' }}>
            <span className="time">{train.arrivalTime}</span>
            <span className="station">{train.destinationStation}</span>
          </div>
        </div>
        <div className="train-actions">
          <div className="train-runs">
            {train.runsOn?.join(', ') || 'Daily'}
          </div>
          <button className="btn btn-secondary btn-sm" onClick={fetchAvail}>
            {expanded ? '▲ Hide' : '▼ Check Seats'}
          </button>
        </div>
      </div>

      {expanded && (
        <div className="train-classes">
          {loadingAvail ? (
            <div style={{ display: 'flex', justifyContent: 'center', padding: '20px' }}>
              <div className="spinner" />
            </div>
          ) : (
            <div className="class-grid">
              {train.availableClasses?.map((cls) => {
                const info = CLASS_LABELS[cls] || { label: cls, name: cls, color: '#64748b' };
                const clsAvail = avail?.[cls];
                return (
                  <button
                    key={cls}
                    className="class-card"
                    style={{ '--cls-color': info.color }}
                    onClick={() => onBook(train, cls, date)}
                  >
                    <div className="class-badge" style={{ color: info.color, borderColor: info.color }}>
                      {info.label}
                    </div>
                    <div className="class-name">{info.name}</div>
                    {clsAvail && <div className="class-fare">₹{clsAvail.fare || '—'}</div>}
                    <AvailBadge avail={clsAvail} />
                  </button>
                );
              })}
            </div>
          )}
        </div>
      )}
    </div>
  );
}

export default function SearchPage() {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const [trains, setTrains] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [from, setFrom] = useState(searchParams.get('from') || '');
  const [to, setTo] = useState(searchParams.get('to') || '');
  const [date, setDate] = useState(searchParams.get('date') || new Date().toISOString().split('T')[0]);
  const [sortBy, setSortBy] = useState('departure');

  useEffect(() => {
    if (from && to && date) fetchTrains();
  }, []);

  const fetchTrains = async () => {
    if (!from || !to) return;
    setLoading(true);
    setError('');
    try {
      const res = await trainsAPI.search({ sourceStation: from, destinationStation: to, journeyDate: date });
      setTrains(res.data.data || []);
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to search trains. Please check the stations and try again.');
      setTrains([]);
    } finally { setLoading(false); }
  };

  const handleSearch = (e) => { e.preventDefault(); fetchTrains(); };
  const swap = () => { setFrom(to); setTo(from); };

  const handleBook = (train, seatClass, date) => {
    navigate(`/book/${train.trainId}`, { state: { train, seatClass, date } });
  };

  const sortedTrains = [...trains].sort((a, b) => {
    if (sortBy === 'departure') return (a.departureTime || '').localeCompare(b.departureTime || '');
    if (sortBy === 'duration') return (a.durationMinutes || 0) - (b.durationMinutes || 0);
    return 0;
  });

  const today = new Date().toISOString().split('T')[0];

  return (
    <div className="search-page page-enter">
      {/* Search Bar */}
      <div className="search-bar-wrapper glass">
        <div className="container">
          <form className="search-bar-form" onSubmit={handleSearch}>
            <div className="sbar-group">
              <label className="form-label">From</label>
              <input
                className="form-input"
                placeholder="Station code or name"
                value={from}
                onChange={(e) => setFrom(e.target.value.toUpperCase())}
                required
              />
            </div>
            <button type="button" className="swap-btn-sm" onClick={swap}>⇄</button>
            <div className="sbar-group">
              <label className="form-label">To</label>
              <input
                className="form-input"
                placeholder="Station code or name"
                value={to}
                onChange={(e) => setTo(e.target.value.toUpperCase())}
                required
              />
            </div>
            <div className="sbar-group sbar-date">
              <label className="form-label">Date</label>
              <input type="date" className="form-input" value={date} min={today} onChange={(e) => setDate(e.target.value)} required />
            </div>
            <button type="submit" className="btn btn-primary" disabled={loading}>
              {loading ? <span className="spinner" /> : '🔍 Search'}
            </button>
          </form>
        </div>
      </div>

      <div className="container search-content">
        {/* Results Header */}
        {trains.length > 0 && (
          <div className="results-header">
            <div className="results-info">
              <h2 className="results-title">
                {from} → {to}
              </h2>
              <span className="results-count">{trains.length} trains found</span>
            </div>
            <div className="sort-options">
              <span className="sort-label">Sort by:</span>
              <button className={`sort-btn ${sortBy === 'departure' ? 'active' : ''}`} onClick={() => setSortBy('departure')}>Departure</button>
              <button className={`sort-btn ${sortBy === 'duration' ? 'active' : ''}`} onClick={() => setSortBy('duration')}>Duration</button>
            </div>
          </div>
        )}

        {/* Loading */}
        {loading && (
          <div className="loading-state">
            {[1, 2, 3].map((n) => (
              <div key={n} className="skeleton" style={{ height: '120px', borderRadius: 'var(--radius-lg)' }} />
            ))}
          </div>
        )}

        {/* Error */}
        {error && !loading && (
          <div className="alert alert-error" style={{ marginTop: '24px' }}>
            <span>⚠️</span> {error}
          </div>
        )}

        {/* Empty State */}
        {!loading && !error && trains.length === 0 && (from || to) && (
          <div className="empty-state">
            <div className="empty-icon">🔍</div>
            <h3>No trains found</h3>
            <p>Try searching with a different date or stations.</p>
          </div>
        )}

        {/* Initial State */}
        {!loading && !error && trains.length === 0 && !from && !to && (
          <div className="empty-state">
            <div className="empty-icon">🚂</div>
            <h3>Find your train</h3>
            <p>Enter source and destination stations to search for trains.</p>
          </div>
        )}

        {/* Train Cards */}
        {!loading && (
          <div className="train-list">
            {sortedTrains.map((train) => (
              <TrainCard key={train.trainId} train={train} date={date} onBook={handleBook} />
            ))}
          </div>
        )}
      </div>
    </div>
  );
}
