import axios from 'axios';

const api = axios.create({
  baseURL: '/api/v1',
  timeout: 15000,
  headers: { 'Content-Type': 'application/json' },
});

// Request interceptor - attach JWT token
api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('accessToken');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
      localStorage.setItem('lastActivity', Date.now().toString());
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// Response interceptor - handle token refresh
api.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config;
    if ((error.response?.status === 401 || error.response?.status === 403) && !originalRequest._retry) {
      originalRequest._retry = true;
      const refreshToken = localStorage.getItem('refreshToken');
      if (refreshToken) {
        try {
          const res = await axios.post('/api/v1/auth/refresh', null, {
            headers: { 'X-Refresh-Token': refreshToken }
          });
          const { accessToken } = res.data.data;
          localStorage.setItem('accessToken', accessToken);
          originalRequest.headers.Authorization = `Bearer ${accessToken}`;
          return api(originalRequest);
        } catch {
          localStorage.removeItem('accessToken');
          localStorage.removeItem('refreshToken');
          window.location.href = '/login';
        }
      }
    }
    return Promise.reject(error);
  }
);

// ── Auth API ──
export const authAPI = {
  register: (data) => api.post('/auth/register', data),
  login: (data) => api.post('/auth/login', data),
  logout: () => api.post('/auth/logout'),
};

// ── Trains API ──
export const trainsAPI = {
  // Backend expects: fromStation, toStation, journeyDate
  search: ({ sourceStation, destinationStation, journeyDate, ...rest }) =>
    api.get('/trains/search', {
      params: {
        fromStation: sourceStation,
        toStation: destinationStation,
        journeyDate,
        ...rest,
      },
    }),
  getAvailability: (trainId, date, seatClass) =>
    api.get(`/trains/${trainId}/availability`, { params: { date, seatClass } }),
  searchStations: (query) => api.get('/trains/stations/search', { params: { query } }),
  getLiveStatus: (trainNumber, date) =>
    api.get(`/trains/${trainNumber}/live-status`, { params: date ? { date } : {} }),
  getMapsConfig: (trainNumber) => api.get(`/trains/${trainNumber}/maps-config`),
};

// ── Stations API ──
export const stationsAPI = {
  search: (query) => api.get('/stations/search', { params: { query } }),
  getByCode: (code) => api.get(`/stations/${code}`),
};

// ── Bookings API ──
export const bookingsAPI = {
  initiate: (data) => api.post('/bookings', data),
  getMyBookings: (page = 0, size = 10) =>
    api.get('/bookings/my-bookings', { params: { page, size } }),
  getPnrStatus: (pnr) => api.get(`/bookings/pnr/${pnr}`),
  cancel: (data) => api.post('/bookings/cancel', data),
};

// ── Payments API ──
export const paymentsAPI = {
  initiate: (data) => api.post('/payments/initiate', data),
  verify: (data) => api.post('/payments/verify', data),
};

export default api;
