import { useState, useEffect } from 'react';
import { useSearchParams } from 'react-router-dom';
import { bookingsAPI } from '../services/api';
import './PnrStatusPage.css';

const BERTH_LABELS = {
  LOWER: 'LB', MIDDLE: 'MB', UPPER: 'UB',
  SIDE_LOWER: 'SL', SIDE_UPPER: 'SU',
};

export default function PnrStatusPage() {
  const [searchParams] = useSearchParams();
  const [pnr, setPnr] = useState(searchParams.get('pnr') || '');
  const [status, setStatus] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    if (pnr && searchParams.get('pnr')) fetchStatus();
  }, []);

  const fetchStatus = async () => {
    if (!pnr.trim()) return;
    setLoading(true);
    setError('');
    setStatus(null);
    try {
      const res = await bookingsAPI.getPnrStatus(pnr.trim());
      setStatus(res.data.data);
    } catch (err) {
      setError(err.response?.status === 404 ? `PNR ${pnr} not found. Please check and try again.` : 'Failed to fetch PNR status.');
    } finally { setLoading(false); }
  };

  const handleSubmit = (e) => { e.preventDefault(); fetchStatus(); };

  const statusColor = status?.bookingStatus === 'CONFIRMED' ? 'var(--color-success)'
    : status?.bookingStatus === 'CANCELLED' ? 'var(--color-danger)'
    : 'var(--color-warning)';

  return (
    <div className="pnr-page page-enter">
      <div className="container">
        <div className="page-hero">
          <h1>PNR Status</h1>
          <p>Check your ticket booking status, seat assignment and journey details</p>
        </div>

        {/* PNR Search Box */}
        <div className="pnr-search-box card">
          <form onSubmit={handleSubmit} className="pnr-form">
            <div className="form-group" style={{ flex: 1 }}>
              <label className="form-label">PNR Number</label>
              <input
                id="pnr-input"
                className="form-input pnr-input"
                placeholder="Enter 10-digit PNR (e.g. 4523671890)"
                value={pnr}
                onChange={(e) => setPnr(e.target.value.replace(/\D/g, '').slice(0, 10))}
                maxLength={10}
                required
              />
            </div>
            <button id="pnr-check-btn" type="submit" className="btn btn-primary btn-lg" disabled={loading || pnr.length < 10} style={{ alignSelf: 'flex-end' }}>
              {loading ? <><span className="spinner" /> Checking...</> : '🔍 Check Status'}
            </button>
          </form>
        </div>

        {error && <div className="alert alert-error" style={{ marginTop: '24px' }}><span>⚠️</span> {error}</div>}

        {/* PNR Result */}
        {status && (
          <div className="pnr-result animate-fadeInUp">
            {/* Status Banner */}
            <div className="pnr-status-banner card" style={{ '--status-color': statusColor }}>
              <div className="psb-left">
                <div className="psb-status-dot" />
                <div>
                  <div className="psb-label">Booking Status</div>
                  <div className="psb-status">{status.bookingStatus?.replace(/_/g, ' ')}</div>
                </div>
              </div>
              <div className="psb-pnr">
                <div className="psb-label">PNR Number</div>
                <div className="psb-value">{status.pnrNumber}</div>
              </div>
            </div>

            {/* Journey Details */}
            <div className="card pnr-details">
              <h2>Journey Details</h2>
              <div className="pnr-journey">
                <div className="pnr-endpoint">
                  <span className="pnr-time">{status.departureTime || '—'}</span>
                  <span className="pnr-station">{status.sourceStation}</span>
                  <span className="pnr-date">{status.journeyDate}</span>
                </div>
                <div className="pnr-middle">
                  <div className="pnr-line" />
                  <span className="pnr-train">{status.trainName}</span>
                  <span className="pnr-num">{status.trainNumber}</span>
                </div>
                <div className="pnr-endpoint" style={{ textAlign: 'right' }}>
                  <span className="pnr-time">{status.arrivalTime || '—'}</span>
                  <span className="pnr-station">{status.destinationStation}</span>
                  <span className="pnr-date">{status.arrivalDate || status.journeyDate}</span>
                </div>
              </div>

              <div className="pnr-meta-grid">
                <div className="pnr-meta-item">
                  <span className="meta-key">Class</span>
                  <span className="meta-val">{status.seatClass?.replace(/_/g, ' ')}</span>
                </div>
                <div className="pnr-meta-item">
                  <span className="meta-key">Passengers</span>
                  <span className="meta-val">{status.passengers?.length || 0}</span>
                </div>
                <div className="pnr-meta-item">
                  <span className="meta-key">Total Fare</span>
                  <span className="meta-val" style={{ color: 'var(--color-primary-light)', fontWeight: 800 }}>₹{status.totalFare || '—'}</span>
                </div>
              </div>
            </div>

            {/* Passengers Table */}
            {status.passengers && status.passengers.length > 0 && (
              <div className="card pnr-passengers">
                <h3>Passenger Details</h3>
                <div className="table-wrapper" style={{ border: 'none', marginTop: '12px' }}>
                  <table>
                    <thead>
                      <tr>
                        <th>#</th>
                        <th>Name</th>
                        <th>Age</th>
                        <th>Gender</th>
                        <th>Coach</th>
                        <th>Seat</th>
                        <th>Berth</th>
                        <th>Status</th>
                      </tr>
                    </thead>
                    <tbody>
                      {status.passengers.map((p, i) => (
                        <tr key={i}>
                          <td>{i + 1}</td>
                          <td style={{ fontWeight: 600 }}>{p.passengerName}</td>
                          <td>{p.passengerAge}</td>
                          <td>{p.passengerGender?.charAt(0)}</td>
                          <td>{p.coachNumber || '—'}</td>
                          <td>{p.seatNumber || '—'}</td>
                          <td>{BERTH_LABELS[p.berthAllotted] || p.berthAllotted || '—'}</td>
                          <td>
                            <span className={`badge ${p.status === 'CONFIRMED' ? 'badge-success' : p.status === 'WAITING_LIST' ? 'badge-warning' : 'badge-danger'}`}>
                              {p.status || 'CNF'}
                            </span>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              </div>
            )}
          </div>
        )}

        {/* Info Cards */}
        {!status && !loading && (
          <div className="pnr-info-grid">
            <div className="card pnr-info-card">
              <span className="pii">📱</span>
              <h3>Quick Check</h3>
              <p>Enter your 10-digit PNR number from your booking confirmation to instantly check status.</p>
            </div>
            <div className="card pnr-info-card">
              <span className="pii">🔄</span>
              <h3>Live Updates</h3>
              <p>PNR status is updated in real-time as your waitlist position changes or seat is confirmed.</p>
            </div>
            <div className="card pnr-info-card">
              <span className="pii">🎫</span>
              <h3>Seat Details</h3>
              <p>View exact coach, seat number and berth allocation for confirmed passengers.</p>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
