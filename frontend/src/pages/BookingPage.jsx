import { useState, useEffect } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { bookingsAPI, paymentsAPI } from '../services/api';
import { toast } from 'react-hot-toast';
import './BookingPage.css';

// Covers both old-style (SLEEPER) and backend seat codes (S3, S2, S1, etc.)
const CLASS_NAMES = {
  SLEEPER: 'Sleeper (SL)',
  AC_3_TIER: 'AC 3 Tier (3A)',
  AC_2_TIER: 'AC 2 Tier (2A)',
  AC_1_TIER: 'AC First Class (1A)',
  CHAIR_CAR: 'Chair Car (CC)',
  EXECUTIVE: 'Executive Chair Car (EC)',
  // Backend seat class codes
  S1: 'AC First Class (1A)',
  S2: 'AC 2 Tier (2A)',
  S3: 'AC 3 Tier (3A)',
  S4: 'Sleeper (SL)',
  S5: 'Chair Car (CC)',
  S6: 'Executive (EC)',
};

const GENDERS = ['MALE', 'FEMALE', 'OTHER'];
const BERTH_PREF = ['LOWER', 'MIDDLE', 'UPPER', 'SIDE_LOWER', 'SIDE_UPPER', 'NO_PREFERENCE'];

export default function BookingPage() {
  const { state } = useLocation();
  const navigate = useNavigate();
  const [step, setStep] = useState(1); // 1=Passengers, 2=Review, 3=Payment
  const [passengers, setPassengers] = useState([
    { name: '', age: '', gender: 'MALE', berthPreference: 'NO_PREFERENCE' },
  ]);
  const [booking, setBooking] = useState(null);
  const [payment, setPayment] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const { train, seatClass, date } = state || {};

  useEffect(() => {
    if (!train || !seatClass || !date) navigate('/search');
  }, []);

  if (!train) return null;

  // Use actual fare from classAvailability if available, otherwise fallback
  const availData = train.classAvailability?.[seatClass];
  const fare = availData?.baseFare || 1000;
  const totalFare = fare * passengers.length;
  const className = CLASS_NAMES[seatClass] || seatClass;

  const addPassenger = () => {
    if (passengers.length >= 6) return;
    setPassengers([...passengers, { name: '', age: '', gender: 'MALE', berthPreference: 'NO_PREFERENCE' }]);
  };

  const removePassenger = (i) => {
    if (passengers.length <= 1) return;
    setPassengers(passengers.filter((_, idx) => idx !== i));
  };

  const updatePassenger = (i, k, v) => {
    setPassengers(passengers.map((p, idx) => (idx === i ? { ...p, [k]: v } : p)));
  };

  // Step 1 → 2: local validation only, no API call yet
  const handleGoToReview = () => {
    setError('');
    const invalid = passengers.find((p) => !p.name.trim() || !p.age);
    if (invalid) {
      setError('Please fill in name and age for all passengers.');
      toast.error('Please fill in all passenger details.');
      return;
    }
    const invalidAge = passengers.find((p) => parseInt(p.age) < 1 || parseInt(p.age) > 120);
    if (invalidAge) {
      setError('Age must be between 1 and 120.');
      return;
    }
    setStep(2);
    window.scrollTo({ top: 0, behavior: 'smooth' });
  };

  // Step 2 → 3: Call backend to initiate booking
  const handleInitiateBooking = async () => {
    setLoading(true);
    setError('');
    try {
      const res = await bookingsAPI.initiate({
        trainId: train.trainId,
        journeyDate: date,
        seatClass,
        // Required by backend
        sourceStationCode: train.sourceStationCode,
        destinationStationCode: train.destinationStationCode,
        passengers: passengers.map((p) => ({
          name: p.name,
          age: parseInt(p.age),
          gender: p.gender,
          berthPreference: p.berthPreference,
        })),
      });
      setBooking(res.data.data);
      setStep(3);
      toast.success('Booking confirmed! Proceeding to payment.');
    } catch (err) {
      const msg = err.response?.data?.message || 'Booking failed. Please try again.';
      const validationErrors = err.response?.data?.errors;
      const detail = validationErrors
        ? Object.values(validationErrors).join(', ')
        : msg;
      setError(detail);
      toast.error(detail);
      if (err.response?.status === 401 || err.response?.status === 403) {
        navigate('/login');
      }
    } finally {
      setLoading(false);
    }
  };

  const handleVerifyPayment = async (method) => {
    setLoading(true);
    setError('');
    try {
      // Simulate payment processing delay for a premium feel
      await new Promise((resolve) => setTimeout(resolve, 1500));
      toast.success('🎉 Payment successful! Booking confirmed.');
      navigate('/my-bookings', { state: { success: true } });
    } catch (err) {
      setError('Payment verification failed.');
      toast.error('Payment verification failed.');
    } finally {
      setLoading(false);
    }
  };

  const stepLabels = ['Passengers', 'Review', 'Payment', 'Confirm'];

  return (
    <div className="booking-page page-enter">
      <div className="container">
        <div className="booking-header">
          <button className="btn btn-ghost btn-sm" onClick={() => navigate(-1)}>← Back</button>
          <h1 className="booking-title">Book Ticket</h1>
        </div>

        {/* Steps */}
        <div className="booking-steps">
          {[1, 2, 3].map((s, idx) => (
            <>
              <div
                key={s}
                className={`booking-step ${step >= s ? 'active' : ''} ${step > s ? 'completed' : ''}`}
              >
                <div className="bs-number">{step > s ? '✓' : s}</div>
                <span>{['Passengers', 'Review', 'Payment'][idx]}</span>
              </div>
              {idx < 2 && <div className={`bs-line ${step > s ? 'done' : ''}`} key={`line-${s}`} />}
            </>
          ))}
        </div>

        <div className="booking-layout">
          {/* Main Content */}
          <div className="booking-main">
            {error && (
              <div className="alert alert-error" style={{ marginBottom: '20px' }}>
                <span>⚠️</span> {error}
              </div>
            )}

            {/* Step 1: Passenger Details */}
            {step === 1 && (
              <div className="card booking-card animate-fadeIn">
                <div className="card-header-row">
                  <h2>Passenger Details</h2>
                  <span className="badge badge-primary">{passengers.length}/6 passengers</span>
                </div>

                {passengers.map((p, i) => (
                  <div key={i} className="passenger-block">
                    <div className="passenger-header">
                      <span className="passenger-num">Passenger {i + 1}</span>
                      {passengers.length > 1 && (
                        <button className="btn btn-danger btn-sm" onClick={() => removePassenger(i)}>
                          Remove
                        </button>
                      )}
                    </div>
                    <div className="passenger-fields">
                      <div className="form-group" style={{ flex: 2 }}>
                        <label className="form-label">Full Name *</label>
                        <input
                          className="form-input"
                          placeholder="As on ID proof"
                          value={p.name}
                          onChange={(e) => updatePassenger(i, 'name', e.target.value)}
                          required
                        />
                      </div>
                      <div className="form-group" style={{ width: '90px' }}>
                        <label className="form-label">Age *</label>
                        <input
                          type="number"
                          className="form-input"
                          placeholder="Age"
                          value={p.age}
                          min="1"
                          max="120"
                          onChange={(e) => updatePassenger(i, 'age', e.target.value)}
                          required
                        />
                      </div>
                      <div className="form-group" style={{ flex: 1 }}>
                        <label className="form-label">Gender</label>
                        <select
                          className="form-input"
                          value={p.gender}
                          onChange={(e) => updatePassenger(i, 'gender', e.target.value)}
                        >
                          {GENDERS.map((g) => (
                            <option key={g} value={g}>
                              {g.charAt(0) + g.slice(1).toLowerCase()}
                            </option>
                          ))}
                        </select>
                      </div>
                      <div className="form-group" style={{ flex: 1.5 }}>
                        <label className="form-label">Berth Preference</label>
                        <select
                          className="form-input"
                          value={p.berthPreference}
                          onChange={(e) => updatePassenger(i, 'berthPreference', e.target.value)}
                        >
                          {BERTH_PREF.map((b) => (
                            <option key={b} value={b}>
                              {b.replace(/_/g, ' ')}
                            </option>
                          ))}
                        </select>
                      </div>
                    </div>
                  </div>
                ))}

                <div className="booking-actions">
                  <button
                    className="btn btn-ghost"
                    onClick={addPassenger}
                    disabled={passengers.length >= 6}
                  >
                    + Add Passenger
                  </button>
                  <button
                    className="btn btn-primary btn-lg"
                    onClick={handleGoToReview}
                    disabled={passengers.some((p) => !p.name.trim() || !p.age)}
                  >
                    Review Booking →
                  </button>
                </div>
              </div>
            )}

            {/* Step 2: Review */}
            {step === 2 && (
              <div className="card booking-card animate-fadeIn">
                <div className="card-header-row">
                  <h2>Review Booking</h2>
                  <span className="badge badge-warning">⏳ Not yet confirmed</span>
                </div>

                <div className="review-info">
                  <div className="review-row">
                    <span className="review-label">Train</span>
                    <span className="review-val">{train.trainName} ({train.trainNumber})</span>
                  </div>
                  <div className="review-row">
                    <span className="review-label">Route</span>
                    <span className="review-val">
                      {train.sourceStationCode || train.sourceStation} → {train.destinationStationCode || train.destinationStation}
                    </span>
                  </div>
                  <div className="review-row">
                    <span className="review-label">Departure</span>
                    <span className="review-val">{train.departureTime} on {date}</span>
                  </div>
                  <div className="review-row">
                    <span className="review-label">Class</span>
                    <span className="review-val">{className}</span>
                  </div>
                  <div className="review-row">
                    <span className="review-label">Passengers</span>
                    <span className="review-val">{passengers.length}</span>
                  </div>
                </div>

                <div className="passenger-summary">
                  {passengers.map((p, i) => (
                    <div key={i} className="pax-chip">
                      {p.name}, Age {p.age}, {p.gender.charAt(0) + p.gender.slice(1).toLowerCase()}
                    </div>
                  ))}
                </div>

                <div className="fare-summary">
                  <div className="fare-row">
                    <span>Base fare</span>
                    <span>₹{fare.toFixed(2)}</span>
                  </div>
                  <div className="fare-row">
                    <span>Passengers</span>
                    <span>× {passengers.length}</span>
                  </div>
                  <div className="fare-row fare-total">
                    <strong>Total Amount</strong>
                    <strong className="fare-amount">₹{totalFare.toFixed(2)}</strong>
                  </div>
                </div>

                <div className="booking-actions">
                  <button className="btn btn-ghost" onClick={() => setStep(1)}>
                    ← Edit Details
                  </button>
                  <button
                    className="btn btn-primary btn-lg"
                    onClick={handleInitiateBooking}
                    disabled={loading}
                  >
                    {loading ? (
                      <><span className="spinner" /> Confirming...</>
                    ) : (
                      `Confirm & Pay ₹${totalFare.toFixed(2)} →`
                    )}
                  </button>
                </div>
              </div>
            )}

            {/* Step 3: Payment */}
            {step === 3 && booking && (
              <div className="card booking-card animate-fadeIn">
                <div className="card-header-row">
                  <h2>Complete Payment</h2>
                  <span className="badge badge-success">PNR: {booking.pnrNumber}</span>
                </div>
                <p className="step-hint">Your seat is reserved. Complete payment to confirm.</p>

                <div className="payment-methods">
                  {[
                    { method: 'UPI_GPAY', icon: '📱', name: 'Google Pay', desc: 'UPI instant payment' },
                    { method: 'UPI_PHONEPE', icon: '💜', name: 'PhonePe', desc: 'UPI instant payment' },
                    { method: 'CARD', icon: '💳', name: 'Credit / Debit Card', desc: 'Visa, Mastercard, RuPay' },
                    { method: 'NET_BANKING', icon: '🏦', name: 'Net Banking', desc: 'All major banks supported' },
                  ].map(({ method, icon, name, desc }) => (
                    <button
                      key={method}
                      className="payment-method"
                      onClick={() => handleVerifyPayment(method)}
                      disabled={loading}
                    >
                      <span className="payment-icon">{icon}</span>
                      <div>
                        <div className="payment-name">{name}</div>
                        <div className="payment-desc">{desc}</div>
                      </div>
                      <span className="payment-arrow">→</span>
                    </button>
                  ))}
                </div>

                {loading && (
                  <div className="payment-loading">
                    <div className="spinner spinner-lg" />
                    <p>Processing your payment...</p>
                  </div>
                )}
              </div>
            )}
          </div>

          {/* Sidebar Summary */}
          <div className="booking-sidebar">
            <div className="card sidebar-card">
              <h3 className="sidebar-title">Journey Summary</h3>
              <div className="summary-train">
                <div className="summary-badge">{train.trainNumber}</div>
                <div>
                  <div className="summary-name">{train.trainName}</div>
                  <div className="summary-class">{className}</div>
                </div>
              </div>
              <div className="summary-route">
                <div className="summary-station">
                  <span className="summary-time">{train.departureTime}</span>
                  <span className="summary-code">
                    {train.sourceStationCode || train.sourceStation}
                  </span>
                </div>
                <div className="summary-arrow">↓</div>
                <div className="summary-station">
                  <span className="summary-time">{train.arrivalTime}</span>
                  <span className="summary-code">
                    {train.destinationStationCode || train.destinationStation}
                  </span>
                </div>
              </div>
              <div className="summary-date">🗓️ {date}</div>
              <div className="summary-fare">
                <span>Total Fare ({passengers.length} pax)</span>
                <span className="fare-total-big">₹{totalFare.toFixed(2)}</span>
              </div>
              <div className="secure-badge">
                <span>🔒</span> Secured by Razorpay
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
