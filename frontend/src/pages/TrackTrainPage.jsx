import { useState, useEffect, useRef } from 'react';
import { useSearchParams } from 'react-router-dom';
import { trainsAPI } from '../services/api';
import { toast } from 'react-hot-toast';
import { MapContainer, TileLayer, Marker, Popup, Polyline } from 'react-leaflet';
import 'leaflet/dist/leaflet.css';
import L from 'leaflet';
import './TrackTrainPage.css';

// Fix default marker icon issue in Leaflet + React
import markerIcon from 'leaflet/dist/images/marker-icon.png';
import markerIcon2x from 'leaflet/dist/images/marker-icon-2x.png';
import markerShadow from 'leaflet/dist/images/marker-shadow.png';

delete L.Icon.Default.prototype._getIconUrl;
L.Icon.Default.mergeOptions({
  iconUrl: markerIcon,
  iconRetinaUrl: markerIcon2x,
  shadowUrl: markerShadow,
});

// Custom icons
const trainIcon = new L.Icon({
  iconUrl: 'https://cdn-icons-png.flaticon.com/512/821/821355.png',
  iconSize: [32, 32],
  iconAnchor: [16, 16],
  popupAnchor: [0, -16]
});

const stationIcon = new L.Icon({
  iconUrl: 'https://cdn-icons-png.flaticon.com/512/3448/3448639.png',
  iconSize: [20, 20],
  iconAnchor: [10, 10],
  popupAnchor: [0, -10]
});

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
      if (isRefresh) {
        toast.success('Live status updated!');
      }
    } catch (err) {
      const msg = err.response?.status === 404
        ? `Train ${trainNum} not found. Check the train number.`
        : 'Failed to fetch live status. Ensure the train is running today.';
      if (!isRefresh) {
        setError(msg);
        toast.error(msg);
      } else {
        toast.error('Failed to update live status.');
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

        {loading && (
          <div className="loading-state" style={{ marginTop: '24px' }}>
            <div className="skeleton" style={{ height: '140px', borderRadius: '16px' }} />
            <div className="skeleton" style={{ height: '400px', borderRadius: '16px', marginTop: '16px' }} />
            <div className="skeleton" style={{ height: '300px', borderRadius: '16px', marginTop: '16px' }} />
          </div>
        )}

        {error && !loading && (
          <div className="empty-state" style={{ marginTop: '24px' }}>
            <div className="empty-icon">🛰️</div>
            <h3>Tracking Unavailable</h3>
            <p>{error}</p>
          </div>
        )}

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

            {/* Live Map */}
            {trackData.currentLatitude && trackData.currentLongitude && (
              <div className="card map-card animate-fadeInUp" style={{ height: '400px', width: '100%', padding: 0, overflow: 'hidden', borderRadius: 'var(--radius-lg, 12px)', border: '1px solid var(--color-border, rgba(255,255,255,0.1))' }}>
                <MapContainer
                  center={[trackData.currentLatitude, trackData.currentLongitude]}
                  zoom={6}
                  style={{ height: '100%', width: '100%', background: '#1e1b4b' }}
                >
                  <TileLayer
                    attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>'
                    url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
                  />
                  
                  {/* Station Markers */}
                  {trackData.routeStatus && trackData.routeStatus.map((s, idx) => (
                    s.latitude && s.longitude && (
                      <Marker key={idx} position={[s.latitude, s.longitude]} icon={stationIcon}>
                        <Popup>
                          <strong>{s.stationName} ({s.stationCode})</strong><br />
                          Status: {s.status}<br />
                          {s.scheduledArrival ? `Sch: ${s.scheduledArrival}` : 'Origin'}
                        </Popup>
                      </Marker>
                    )
                  ))}

                  {/* Route Polyline */}
                  {trackData.routeStatus && (
                    <Polyline
                      positions={trackData.routeStatus
                        .filter(s => s.latitude && s.longitude)
                        .map(s => [s.latitude, s.longitude])
                      }
                      color="#6366f1"
                      weight={4}
                      opacity={0.8}
                    />
                  )}

                  {/* Live Train Marker */}
                  <Marker position={[trackData.currentLatitude, trackData.currentLongitude]} icon={trainIcon}>
                    <Popup>
                      <strong>{trackData.trainName} ({trackData.trainNumber})</strong><br />
                      Speed: {trackData.speedKmph} kmph<br />
                      Delay: {trackData.delayMinutes} mins
                    </Popup>
                  </Marker>
                </MapContainer>
              </div>
            )}

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
        {!trackData && !loading && !error && (
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
