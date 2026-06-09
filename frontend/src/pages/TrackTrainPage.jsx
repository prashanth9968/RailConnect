import { useState, useEffect, useRef } from 'react';
import { useSearchParams } from 'react-router-dom';
import { trainsAPI } from '../services/api';
import './TrackTrainPage.css';

export default function TrackTrainPage() {
  const [searchParams] = useSearchParams();
  const [trainNum, setTrainNum] = useState(searchParams.get('train') || '');
  const [trackData, setTrackData] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [refreshing, setRefreshing] = useState(false);
  const intervalRef = useRef(null);

  useEffect(() => {
    if (trainNum && searchParams.get('train')) fetchStatus();
    return () => clearInterval(intervalRef.current);
  }, []);

  const fetchStatus = async (isRefresh = false) => {
    if (!trainNum.trim()) return;
    if (isRefresh) setRefreshing(true);
    else { setLoading(true); setError(''); setTrackData(null); }
    try {
      const res = await trainsAPI.getLiveStatus(trainNum.trim());
      setTrackData(res.data.data);
    } catch (err) {
      if (!isRefresh) {
        setError(err.response?.status === 404 ? `Train ${trainNum} not found. Check the train number.` : 'Failed to fetch live status. Ensure the train is running today.');
      }
    } finally { setLoading(false); setRefreshing(false); }
  };

  const handleSubmit = (e) => { e.preventDefault(); fetchStatus(); };

  const startAutoRefresh = () => {
    clearInterval(intervalRef.current);
    intervalRef.current = setInterval(() => fetchStatus(true), 60000);
  };

  useEffect(() => {
    if (trackData) startAutoRefresh();
    return () => clearInterval(intervalRef.current);
  }, [trackData]);

  const delayColor = (delay) => {
    if (!delay || delay <= 0) return 'var(--color-success)';
    if (delay <= 15) return 'var(--color-warning)';
    return 'var(--color-danger)';
  };

  const delayLabel = (delay) => {
    if (!delay || delay <= 0) return 'On Time';
    return `${delay} mins late`;
  };

  return (
    <div className="track-page page-enter">
      <div className="container">
        <div className="page-hero">
          <h1>Live Train Tracking</h1>
          <p>Real-time GPS location and station-wise running status</p>
        </div>

        {/* Search */}
        <div className="card track-search-box">
          <form onSubmit={handleSubmit} className="track-form">
            <div className="form-group" style={{ flex: 1 }}>
              <label className="form-label">Train Number</label>
              <input
                id="track-input"
                className="form-input"
                placeholder="e.g. 12951 for Mumbai Rajdhani"
                value={trainNum}
                onChange={(e) => setTrainNum(e.target.value.trim())}
                required
              />
            </div>
            <button id="track-submit-btn" type="submit" className="btn btn-primary btn-lg" disabled={loading} style={{ alignSelf: 'flex-end' }}>
              {loading ? <><span className="spinner" /> Tracking...</> : '📍 Track Now'}
            </button>
          </form>
        </div>

        {error && <div className="alert alert-error" style={{ marginTop: '24px' }}><span>⚠️</span> {error}</div>}

        {/* Live Status */}
        {trackData && (
          <div className="track-result animate-fadeInUp">
            {/* Train Header */}
            <div className="card track-header-card">
              <div className="th-main">
                <div className="th-info">
                  <div className="th-number">{trackData.trainNumber || trainNum}</div>
                  <div className="th-name">{trackData.trainName}</div>
                  <div className="th-route">{trackData.sourceStation} → {trackData.destinationStation}</div>
                </div>
                <div className="th-status">
                  <div className="delay-indicator" style={{ '--delay-color': delayColor(trackData.delayMinutes) }}>
                    <div className="delay-dot" />
                    <div>
                      <div className="delay-label">Running Status</div>
                      <div className="delay-value">{delayLabel(trackData.delayMinutes)}</div>
                    </div>
                  </div>
                  <button
                    className={`btn btn-secondary btn-sm refresh-btn ${refreshing ? 'refreshing' : ''}`}
                    onClick={() => fetchStatus(true)}
                    disabled={refreshing}
                  >
                    {refreshing ? '⟳ Updating...' : '⟳ Refresh'}
                  </button>
                  <span className="auto-refresh-label">Auto-refreshes every 60s</span>
                </div>
              </div>

              {/* Current Location */}
              {trackData.currentStation && (
                <div className="current-location">
                  <div className="cl-label">📍 Current Location</div>
                  <div className="cl-station">{trackData.currentStation}</div>
                  {trackData.nextStation && (
                    <div className="cl-next">Next Stop: <strong>{trackData.nextStation}</strong>
                      {trackData.expectedArrival && ` · ETA ${trackData.expectedArrival}`}
                    </div>
                  )}
                </div>
              )}

              {/* GPS Badge */}
              {trackData.latitude && trackData.longitude && (
                <div className="gps-badge">
                  <span>🛰️</span>
                  <span>GPS: {trackData.latitude?.toFixed(4)}, {trackData.longitude?.toFixed(4)}</span>
                  <span className="live-badge">LIVE</span>
                </div>
              )}
            </div>

            {/* Station Schedule */}
            {trackData.stations && trackData.stations.length > 0 && (
              <div className="card track-schedule">
                <h2>Station Schedule</h2>
                <div className="schedule-list">
                  {trackData.stations.map((s, i) => (
                    <div key={i} className={`schedule-item ${s.status === 'PASSED' ? 'passed' : s.status === 'CURRENT' ? 'current' : 'upcoming'}`}>
                      <div className="si-marker">
                        <div className="si-dot" />
                        {i < trackData.stations.length - 1 && <div className="si-line" />}
                      </div>
                      <div className="si-content">
                        <div className="si-name">{s.stationName}</div>
                        <div className="si-code">{s.stationCode}</div>
                      </div>
                      <div className="si-times">
                        <div className="si-scheduled">Sch: {s.scheduledArrival || 'Origin'}</div>
                        {s.actualArrival && <div className="si-actual">Act: {s.actualArrival}</div>}
                      </div>
                      <div className="si-delay">
                        {s.delay != null && s.delay !== 0 && (
                          <span className="si-delay-badge" style={{ color: delayColor(s.delay) }}>
                            +{s.delay}m
                          </span>
                        )}
                        {s.status === 'PASSED' && <span className="badge badge-success" style={{ fontSize: '0.7rem' }}>✓</span>}
                        {s.status === 'CURRENT' && <span className="badge badge-info">Here</span>}
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            )}
          </div>
        )}

        {/* Info when empty */}
        {!trackData && !loading && (
          <div className="track-info-grid">
            <div className="card track-info-card">
              <span style={{ fontSize: '2rem' }}>🛰️</span>
              <h3>GPS Tracking</h3>
              <p>Get real-time GPS coordinates of your train updated every 60 seconds.</p>
            </div>
            <div className="card track-info-card">
              <span style={{ fontSize: '2rem' }}>⏱️</span>
              <h3>Delay Alerts</h3>
              <p>Know exactly how many minutes your train is running behind schedule.</p>
            </div>
            <div className="card track-info-card">
              <span style={{ fontSize: '2rem' }}>📊</span>
              <h3>Station Timeline</h3>
              <p>View scheduled vs actual arrival times for every station en route.</p>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
