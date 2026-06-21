import { useState, useEffect } from 'react';
import { useSearchParams, useNavigate } from 'react-router-dom';
import { trainsAPI } from '../services/api';
import StationAutocomplete from '../components/StationAutocomplete';
import './SearchPage.css';

const CLASS_LABELS = {
  SLEEPER: { label: 'SL', name: 'Sleeper', color: '#4f46e5' },
  AC_3_TIER: { label: '3A', name: 'AC 3 Tier', color: '#7c3aed' },
  AC_2_TIER: { label: '2A', name: 'AC 2 Tier', color: '#0891b2' },
  AC_1_TIER: { label: '1A', name: 'AC First Class', color: '#f59e0b' },
  CHAIR_CAR: { label: 'CC', name: 'Chair Car', color: '#10b981' },
  EXECUTIVE: { label: 'EC', name: 'Executive', color: '#ef4444' },
};

function AvailBadge({ avail }) {
  if (!avail) return null;
  const { availableSeats, status, waitlistCount } = avail;
  if (status === 'AVAILABLE' && availableSeats > 0)
    return <span className="badge badge-success">✓ {availableSeats} Avail</span>;
  if (status === 'RAC')
    return <span className="badge badge-warning">RAC {availableSeats}</span>;
  if (status === 'NOT_AVAILABLE' || availableSeats === 0)
    return <span className="badge badge-danger">Not Available</span>;
  if (waitlistCount > 0)
    return <span className="badge badge-danger">WL {waitlistCount}</span>;
  return null;
}

function TrainCard({ train, date, onBook, onViewRoute }) {
  const [expanded, setExpanded] = useState(false);
  const [loadingAvail, setLoadingAvail] = useState(false);

  // classAvailability comes directly from the search response — use it immediately
  const avail = train.classAvailability || {};
  // Derive class list from the availability map keys (S3, S2, S1, etc.)
  const classKeys = Object.keys(avail);

  const toggleExpand = () => {
    if (classKeys.length > 0) {
      setExpanded(!expanded);
    } else {
      // Fallback: fetch separately if not in search result
      setExpanded(true);
      setLoadingAvail(true);
      trainsAPI.getAvailability(train.trainId, date)
        .then(res => { /* avail is derived from train.classAvailability */ })
        .catch(() => {})
        .finally(() => setLoadingAvail(false));
    }
  };

  return (
    <div className="card train-card">
      <div className="train-header">
        <div className="train-info">
          <div className="train-number-badge">{train.trainNumber}</div>
          <div>
            <div className="train-name" onClick={() => onViewRoute(train)}>
              {train.trainName}
            </div>
            <div className="train-type">{train.trainType?.replace(/_/g, ' ')}</div>
          </div>
        </div>
        <div className="train-route">
          <div className="route-time">
            <span className="time">{train.departureTime}</span>
            <span className="station">{train.sourceStationCode || train.sourceStation}</span>
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
            <span className="station">{train.destinationStationCode || train.destinationStation}</span>
          </div>
        </div>
        <div className="train-actions">
          <div className="train-runs">
            {train.runsOn?.join(', ') || 'Daily'}
          </div>
          <div style={{ display: 'flex', gap: '8px' }}>
            <button className="btn btn-ghost btn-sm" onClick={() => onViewRoute(train)}>
              🗺️ Route
            </button>
            <button className="btn btn-secondary btn-sm" onClick={toggleExpand}>
              {expanded ? '▲ Hide' : `▼ Check Seats (${classKeys.length})`}
            </button>
          </div>
        </div>
      </div>

      {expanded && (
        <div className="train-classes">
          {loadingAvail ? (
            <div style={{ display: 'flex', justifyContent: 'center', padding: '20px' }}>
              <div className="spinner" />
            </div>
          ) : classKeys.length === 0 ? (
            <div style={{ padding: '20px', textAlign: 'center', color: 'var(--color-text-muted)' }}>
              No seat classes available for this date.
            </div>
          ) : (
            <div className="class-grid">
              {classKeys.map((cls) => {
                const info = CLASS_LABELS[cls] || { label: cls, name: cls, color: '#64748b' };
                const clsAvail = avail[cls];
                const notAvail = clsAvail?.status === 'NOT_AVAILABLE' || clsAvail?.availableSeats === 0;
                return (
                  <button
                    key={cls}
                    className={`class-card ${notAvail ? 'class-card-unavail' : ''}`}
                    style={{ '--cls-color': notAvail ? '#94a3b8' : info.color }}
                    onClick={() => !notAvail && onBook(train, cls, date)}
                    disabled={notAvail}
                  >
                    <div className="class-badge" style={{
                      color: notAvail ? '#94a3b8' : info.color,
                      borderColor: notAvail ? '#94a3b8' : info.color
                    }}>
                      {info.label}
                    </div>
                    <div className="class-name">{info.name}</div>
                    {clsAvail?.baseFare && (
                      <div className="class-fare">₹{clsAvail.baseFare}</div>
                    )}
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

function TrainDetailsDrawer({ train, onClose, date }) {
  const [schedule, setSchedule] = useState([]);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (train) fetchSchedule();
  }, [train]);

  const fetchSchedule = async () => {
    setLoading(true);
    try {
      const res = await trainsAPI.getLiveStatus(train.trainNumber, date);
      setSchedule(res.data.data.routeStatus || []);
    } catch {
      setSchedule([]);
    } finally {
      setLoading(false);
    }
  };

  if (!train) return null;

  return (
    <div className="drawer-overlay" onClick={onClose}>
      <div className="drawer-content" onClick={(e) => e.stopPropagation()}>
        <div className="drawer-header">
          <div>
            <div className="drawer-train-num">{train.trainNumber}</div>
            <h3 style={{ fontSize: '1.25rem' }}>{train.trainName}</h3>
            <p className="drawer-train-type">{train.trainType?.replace(/_/g, ' ')}</p>
          </div>
          <button className="drawer-close-btn" onClick={onClose}>&times;</button>
        </div>
        <div className="drawer-body">
          {loading ? (
            <div className="drawer-loading">
              <div className="spinner spinner-lg" />
              <p style={{ marginTop: '12px' }}>Loading schedule...</p>
            </div>
          ) : schedule.length > 0 ? (
            <div className="drawer-timeline">
              <h4>Route & Timings</h4>
              <div className="drawer-steps">
                {schedule.map((s, idx) => (
                  <div key={idx} className="drawer-step">
                    <div className="ds-marker">
                      <div className="ds-dot" />
                      {idx < schedule.length - 1 && <div className="ds-line" />}
                    </div>
                    <div className="ds-content">
                      <div className="ds-station">
                        <span className="ds-code">{s.stationCode}</span>
                        <span className="ds-name">{s.stationName}</span>
                      </div>
                      <div className="ds-times">
                        {s.scheduledArrival ? `Arr: ${s.scheduledArrival}` : 'Origin'}
                        {s.scheduledDeparture ? ` · Dep: ${s.scheduledDeparture}` : ' · Terminals'}
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          ) : (
            <div className="drawer-loading">
              <p>No route schedule found for this train.</p>
            </div>
          )}
        </div>
      </div>
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
  const [selectedTrainDetail, setSelectedTrainDetail] = useState(null);

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
      <div className="search-bar-wrapper">
        <div className="container">
          <form className="search-bar-form" onSubmit={handleSearch}>
            <div style={{ flex: 1 }}>
              <StationAutocomplete
                label="From"
                placeholder="Station code or name"
                value={from}
                onChange={setFrom}
              />
            </div>
            <button type="button" className="swap-btn-sm" onClick={swap}>⇄</button>
            <div style={{ flex: 1 }}>
              <StationAutocomplete
                label="To"
                placeholder="Station code or name"
                value={to}
                onChange={setTo}
              />
            </div>
            <div className="sbar-group sbar-date">
              <label className="form-label">Date</label>
              <input type="date" className="form-input" value={date} min={today} onChange={(e) => setDate(e.target.value)} required />
            </div>
            <button type="submit" className="btn btn-primary" style={{ height: '46px' }} disabled={loading}>
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
                <span className="results-count">{trains.length} trains found</span>
              </h2>
            </div>
            <div className="sort-options">
              <span className="sort-label">Sort by:</span>
              <button className={`sort-btn ${sortBy === 'departure' ? 'active' : ''}`} onClick={() => setSortBy('departure')}>Departure</button>
              <button className={`sort-btn ${sortBy === 'duration' ? 'active' : ''}`} onClick={() => setSortBy('duration')}>Duration</button>
            </div>
          </div>
        )}

        {/* Loading Skeletons */}
        {loading && (
          <div className="loading-state">
            {[1, 2, 3].map((n) => (
              <div key={n} className="skeleton" style={{ height: '140px', borderRadius: '20px' }} />
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
            <div className="empty-icon">🚆</div>
            <h3>No trains available</h3>
            <p>Try searching with a different date or different stations.</p>
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
              <TrainCard
                key={train.trainId}
                train={train}
                date={date}
                onBook={handleBook}
                onViewRoute={setSelectedTrainDetail}
              />
            ))}
          </div>
        )}
      </div>

      {/* Train Details Drawer */}
      {selectedTrainDetail && (
        <TrainDetailsDrawer
          train={selectedTrainDetail}
          date={date}
          onClose={() => setSelectedTrainDetail(null)}
        />
      )}
    </div>
  );
}
