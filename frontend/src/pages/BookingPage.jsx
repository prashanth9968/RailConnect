import { useState, useEffect } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { bookingsAPI, paymentsAPI } from '../services/api';
import { toast } from 'react-hot-toast';
import './BookingPage.css';

const CLASS_NAMES = {
  SLEEPER: 'Sleeper (SL)',
  AC_3_TIER: 'AC 3 Tier (3A)',
  AC_2_TIER: 'AC 2 Tier (2A)',
  AC_1_TIER: 'AC First Class (1A)',
  CHAIR_CAR: 'Chair Car (CC)',
  EXECUTIVE: 'Executive Chair Car (EC)',
};

const FARE_MAP = {
  SLEEPER: 450, AC_3_TIER: 1200, AC_2_TIER: 1800,
  AC_1_TIER: 3000, CHAIR_CAR: 700, EXECUTIVE: 1400,
};

const GENDERS = ['MALE', 'FEMALE', 'OTHER'];
const BERTH_PREF = ['LOWER', 'MIDDLE', 'UPPER', 'SIDE_LOWER', 'SIDE_UPPER', 'NO_PREFERENCE'];

export default function BookingPage() {
  const { state } = useLocation();
  const navigate = useNavigate();
  const [step, setStep] = useState(1); // 1=Passengers, 2=Review, 3=Payment
  const [passengers, setPassengers] = useState([{ name: '', age: '', gender: 'MALE', berthPreference: 'NO_PREFERENCE' }]);
  const [booking, setBooking] = useState(null);
  const [payment, setPayment] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const { train, seatClass, date } = state || {};

  useEffect(() => {
    if (!train || !seatClass || !date) navigate('/search');
  }, []);

  if (!train) return null;

  const fare = FARE_MAP[seatClass] || 1000;
  const totalFare = fare * passengers.length;

  const addPassenger = () => {
    if (passengers.length >= 6) return;
    setPassengers([...passengers, { name: '', age: '', gender: 'MALE', berthPreference: 'NO_PREFERENCE' }]);
  };

  const removePassenger = (i) => {
    if (passengers.length <= 1) return;
    setPassengers(passengers.filter((_, idx) => idx !== i));
  };

  const updatePassenger = (i, k, v) => {
    setPassengers(passengers.map((p, idx) => idx === i ? { ...p, [k]: v } : p));
  };

  const handleInitiateBooking = async () => {
    setLoading(true);
    setError('');
    try {
      const res = await bookingsAPI.initiate({
        trainId: train.trainId,
        journeyDate: date,
        seatClass,
        passengers: passengers.map((p) => ({
          passengerName: p.name,
          passengerAge: parseInt(p.age),
          passengerGender: p.gender,
          berthPreference: p.berthPreference,
        })),
      });
      setBooking(res.data.data);
      setStep(2);
      toast.success('Passenger details saved.');
    } catch (err) {
      const msg = err.response?.data?.message || 'Booking initiation failed. Please try again.';
      setError(msg);
      toast.error(msg);
    } finally { setLoading(false); }
  };

  const handleInitiatePayment = async () => {
    setLoading(true);
    setError('');
    try {
      const res = await paymentsAPI.initiate({ bookingId: booking.bookingId, amount: totalFare });
      setPayment(res.data.data);
      setStep(3);
    } catch (err) {
      const msg = err.response?.data?.message || 'Payment initiation failed.';
      setError(msg);
      toast.error(msg);
    } finally { setLoading(false); }
  };

  const handleVerifyPayment = async (method) => {
    setLoading(true);
    setError('');
    try {
      await paymentsAPI.verify({
        bookingId: booking.bookingId,
        razorpayOrderId: payment?.razorpayOrderId,
        razorpayPaymentId: `pay_demo_${Date.now()}`,
        razorpaySignature: 'demo_sig',
        paymentMethod: method,
      });
      navigate('/my-bookings', { state: { success: true } });
    } catch (err) {
      const msg = err.response?.data?.message || 'Payment verification failed.';
      setError(msg);
      toast.error(msg);
    } finally { setLoading(false); }
  };

  return (
    <div className="booking-page page-enter">
      <div className="container">
        <div className="booking-header">
          <button className="btn btn-ghost btn-sm" onClick={() => navigate(-1)}>← Back</button>
          <h1 className="booking-title">Book Ticket</h1>
        </div>

        {/* Steps */}
        <div className="booking-steps">
          <div className={`booking-step ${step >= 1 ? 'active' : ''} ${step > 1 ? 'completed' : ''}`}>
            <div className="bs-number">{step > 1 ? '✓' : '1'}</div>
            <span>Passengers</span>
          </div>
          <div className="bs-line" />
          <div className={`booking-step ${step >= 2 ? 'active' : ''} ${step > 2 ? 'completed' : ''}`}>
            <div className="bs-number">{step > 2 ? '✓' : '2'}</div>
            <span>Review</span>
          </div>
          <div className="bs-line" />
          <div className={`booking-step ${step >= 3 ? 'active' : ''}`}>
            <div className="bs-number">3</div>
            <span>Payment</span>
          </div>
        </div>

        <div className="booking-layout">
          {/* Main Content */}
          <div className="booking-main">
            {error && <div className="alert alert-error"><span>⚠️</span>{error}</div>}

            {/* Step 1: Passengers */}
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
                        <button className="btn btn-danger btn-sm" onClick={() => removePassenger(i)}>Remove</button>
                      )}
                    </div>
                    <div className="passenger-fields">
                      <div className="form-group" style={{ flex: 2 }}>
                        <label className="form-label">Full Name</label>
                        <input
                          className="form-input"
                          placeholder="As on ID proof"
                          value={p.name}
                          onChange={(e) => updatePassenger(i, 'name', e.target.value)}
                          required
                        />
                      </div>
                      <div className="form-group" style={{ width: '80px' }}>
                        <label className="form-label">Age</label>
                        <input
                          type="number"
                          className="form-input"
                          placeholder="Age"
                          value={p.age}
                          min="1" max="120"
                          onChange={(e) => updatePassenger(i, 'age', e.target.value)}
                          required
                        />
                      </div>
                      <div className="form-group" style={{ flex: 1 }}>
                        <label className="form-label">Gender</label>
                        <select className="form-input" value={p.gender} onChange={(e) => updatePassenger(i, 'gender', e.target.value)}>
                          {GENDERS.map((g) => <option key={g}>{g}</option>)}
                        </select>
                      </div>
                      <div className="form-group" style={{ flex: 1.5 }}>
                        <label className="form-label">Berth Preference</label>
                        <select className="form-input" value={p.berthPreference} onChange={(e) => updatePassenger(i, 'berthPreference', e.target.value)}>
                          {BERTH_PREF.map((b) => <option key={b} value={b}>{b.replace(/_/g, ' ')}</option>)}
                        </select>
                      </div>
                    </div>
                  </div>
                ))}

                <div className="booking-actions">
                  <button className="btn btn-ghost" onClick={addPassenger} disabled={passengers.length >= 6}>
                    + Add Passenger
                  </button>
                  <button
                    className="btn btn-primary"
                    onClick={handleInitiateBooking}
                    disabled={loading || passengers.some((p) => !p.name || !p.age)}
                  >
                    {loading ? <><span className="spinner" /> Booking...</> : 'Continue to Review →'}
                  </button>
                </div>
              </div>
            )}

            {/* Step 2: Review */}
            {step === 2 && booking && (
              <div className="card booking-card animate-fadeIn">
                <div className="card-header-row">
                  <h2>Review Booking</h2>
                  <span className="badge badge-success">PNR: {booking.pnrNumber}</span>
                </div>

                <div className="review-info">
                  <div className="review-row">
                    <span className="review-label">Train</span>
                    <span className="review-val">{train.trainName} ({train.trainNumber})</span>
                  </div>
                  <div className="review-row">
                    <span className="review-label">Route</span>
                    <span className="review-val">{train.sourceStation} → {train.destinationStation}</span>
                  </div>
                  <div className="review-row">
                    <span className="review-label">Date</span>
                    <span className="review-val">{date}</span>
                  </div>
                  <div className="review-row">
                    <span className="review-label">Class</span>
                    <span className="review-val">{CLASS_NAMES[seatClass]}</span>
                  </div>
                  <div className="review-row">
                    <span className="review-label">Passengers</span>
                    <span className="review-val">{passengers.length}</span>
                  </div>
                </div>

                <div className="passenger-summary">
                  {passengers.map((p, i) => (
                    <div key={i} className="pax-chip">
                      {p.name}, {p.age}, {p.gender.charAt(0)}
                    </div>
                  ))}
                </div>

                <div className="fare-summary">
                  <div className="fare-row">
                    <span>Base fare × {passengers.length}</span>
                    <span>₹{fare} × {passengers.length}</span>
                  </div>
                  <div className="fare-row fare-total">
                    <strong>Total Amount</strong>
                    <strong className="fare-amount">₹{totalFare}</strong>
                  </div>
                </div>

                <button className="btn btn-primary btn-lg w-full" onClick={handleInitiatePayment} disabled={loading}>
                  {loading ? <><span className="spinner" /> Processing...</> : `Pay ₹${totalFare} Now →`}
                </button>
              </div>
            )}

            {/* Step 3: Payment */}
            {step === 3 && payment && (
              <div className="card booking-card animate-fadeIn">
                <h2>Complete Payment</h2>
                <p className="step-hint">Choose your preferred payment method</p>

                <div className="payment-methods">
                  <button className="payment-method" onClick={() => handleVerifyPayment('UPI_GPAY')} disabled={loading}>
                    <span className="payment-icon">📱</span>
                    <div>
                      <div className="payment-name">Google Pay</div>
                      <div className="payment-desc">UPI instant payment</div>
                    </div>
                    <span className="payment-arrow">→</span>
                  </button>

                  <button className="payment-method" onClick={() => handleVerifyPayment('UPI_PHONEPE')} disabled={loading}>
                    <span className="payment-icon">💜</span>
                    <div>
                      <div className="payment-name">PhonePe</div>
                      <div className="payment-desc">UPI instant payment</div>
                    </div>
                    <span className="payment-arrow">→</span>
                  </button>

                  <button className="payment-method" onClick={() => handleVerifyPayment('CARD')} disabled={loading}>
                    <span className="payment-icon">💳</span>
                    <div>
                      <div className="payment-name">Credit / Debit Card</div>
                      <div className="payment-desc">Visa, Mastercard, RuPay</div>
                    </div>
                    <span className="payment-arrow">→</span>
                  </button>

                  <button className="payment-method" onClick={() => handleVerifyPayment('NET_BANKING')} disabled={loading}>
                    <span className="payment-icon">🏦</span>
                    <div>
                      <div className="payment-name">Net Banking</div>
                      <div className="payment-desc">All major banks supported</div>
                    </div>
                    <span className="payment-arrow">→</span>
                  </button>
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
                  <div className="summary-class">{CLASS_NAMES[seatClass]}</div>
                </div>
              </div>
              <div className="summary-route">
                <div className="summary-station">
                  <span className="summary-time">{train.departureTime}</span>
                  <span className="summary-code">{train.sourceStation}</span>
                </div>
                <div className="summary-arrow">↓</div>
                <div className="summary-station">
                  <span className="summary-time">{train.arrivalTime}</span>
                  <span className="summary-code">{train.destinationStation}</span>
                </div>
              </div>
              <div className="summary-date">{date}</div>
              <div className="summary-fare">
                <span>Total Fare</span>
                <span className="fare-total-big">₹{totalFare}</span>
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
