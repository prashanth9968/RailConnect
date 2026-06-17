import { useState, useEffect } from 'react';
import { useLocation } from 'react-router-dom';
import { bookingsAPI } from '../services/api';
import { toast } from 'react-hot-toast';
import './MyBookingsPage.css';

const STATUS_STYLES = {
  CONFIRMED: { cls: 'badge-info', label: '✓ Confirmed' },
  PENDING: { cls: 'badge-warning', label: '⏳ Pending' },
  CANCELLED: { cls: 'badge-danger', label: '✕ Cancelled' },
  WAITING_LIST: { cls: 'badge-danger', label: '📋 Waitlist' },
};

export default function MyBookingsPage() {
  const { state } = useLocation();
  const [bookings, setBookings] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [cancelling, setCancelling] = useState(null);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(1);

  useEffect(() => {
    if (state?.success) {
      toast.success('Booking confirmed! Ticket details sent to email.');
      // clear router state so it doesn't trigger again on refresh
      window.history.replaceState({}, document.title);
    }
  }, [state]);

  useEffect(() => { fetchBookings(page); }, [page]);

  const fetchBookings = async (p) => {
    setLoading(true);
    try {
      const res = await bookingsAPI.getMyBookings(p, 10);
      const data = res.data.data;
      setBookings(data.content || []);
      setTotalPages(data.totalPages || 1);
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to load bookings.');
    } finally { setLoading(false); }
  };

  const handleCancel = async (booking) => {
    if (!window.confirm(`Cancel booking PNR ${booking.pnrNumber}? Refund will be processed automatically.`)) return;
    setCancelling(booking.bookingId);
    try {
      const res = await bookingsAPI.cancel({ bookingId: booking.bookingId, pnrNumber: booking.pnrNumber });
      const refund = res.data?.data?.refundAmount || 0;
      toast.success(`Booking PNR ${booking.pnrNumber} cancelled. Refund of ₹${refund.toFixed(2)} processed!`);
      fetchBookings(page);
    } catch (err) {
      const errMsg = err.response?.data?.message || 'Cancellation failed.';
      toast.error(errMsg);
    } finally { setCancelling(null); }
  };

  return (
    <div className="my-bookings-page page-enter">
      <div className="container">
        <div className="page-header">
          <h1>My Bookings</h1>
          <p className="page-subtitle">Your journey history and upcoming trips</p>
        </div>

        {error && <div className="alert alert-error" style={{ marginBottom: '20px' }}><span>⚠️</span> {error}</div>}

        {loading ? (
          <div style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
            {[1, 2, 3].map((n) => (
              <div key={n} className="skeleton" style={{ height: '150px', borderRadius: 'var(--radius-lg)' }} />
            ))}
          </div>
        ) : bookings.length === 0 ? (
          <div className="empty-state">
            <div className="empty-icon">🎫</div>
            <h3>No bookings yet</h3>
            <p>Your tickets will appear here after booking.</p>
          </div>
        ) : (
          <>
            <div className="bookings-list">
              {bookings.map((b) => {
                const statusStyle = STATUS_STYLES[b.bookingStatus] || { cls: 'badge-primary', label: b.bookingStatus };
                return (
                  <div key={b.bookingId} className="card booking-item">
                    <div className="booking-item-header">
                      <div className="booking-pnr">
                        <span className="pnr-label">PNR</span>
                        <span className="pnr-number">{b.pnrNumber}</span>
                      </div>
                      <span className={`badge ${statusStyle.cls}`}>{statusStyle.label}</span>
                    </div>

                    <div className="booking-item-body">
                      <div className="booking-route-info">
                        <div className="bi-train">
                          <div className="bi-train-num">{b.trainNumber}</div>
                          <div className="bi-train-name">{b.trainName}</div>
                        </div>
                        <div className="bi-journey">
                          <div className="bi-station">
                            <span className="bi-time">{b.departureTime}</span>
                            <span className="bi-code">{b.sourceStation}</span>
                          </div>
                          <div className="bi-arrow">→</div>
                          <div className="bi-station">
                            <span className="bi-time">{b.arrivalTime}</span>
                            <span className="bi-code">{b.destinationStation}</span>
                          </div>
                        </div>
                      </div>

                      <div className="booking-meta">
                        <div className="meta-item">
                          <span className="meta-label">Journey Date</span>
                          <span className="meta-val">{b.journeyDate}</span>
                        </div>
                        <div className="meta-item">
                          <span className="meta-label">Class</span>
                          <span className="meta-val">{b.seatClass?.replace(/_/g, ' ')}</span>
                        </div>
                        <div className="meta-item">
                          <span className="meta-label">Passengers</span>
                          <span className="meta-val">{b.passengers?.length || 1}</span>
                        </div>
                        <div className="meta-item">
                          <span className="meta-label">Total Fare</span>
                          <span className="meta-val fare-highlight">₹{b.totalFare || '—'}</span>
                        </div>
                      </div>
                    </div>

                    {(b.bookingStatus === 'CONFIRMED' || b.bookingStatus === 'WAITING_LIST') && (
                      <div className="booking-item-footer">
                        <button
                          className="btn btn-danger btn-sm"
                          onClick={() => handleCancel(b)}
                          disabled={cancelling === b.bookingId}
                        >
                          {cancelling === b.bookingId ? <><span className="spinner" /> Cancelling...</> : '✕ Cancel Booking'}
                        </button>
                      </div>
                    )}
                  </div>
                );
              })}
            </div>

            {/* Pagination */}
            {totalPages > 1 && (
              <div className="pagination">
                <button className="btn btn-ghost btn-sm" onClick={() => setPage(p => Math.max(0, p - 1))} disabled={page === 0}>
                  ← Previous
                </button>
                <span className="page-info">Page {page + 1} of {totalPages}</span>
                <button className="btn btn-ghost btn-sm" onClick={() => setPage(p => Math.min(totalPages - 1, p + 1))} disabled={page >= totalPages - 1}>
                  Next →
                </button>
              </div>
            )}
          </>
        )}
      </div>
    </div>
  );
}
