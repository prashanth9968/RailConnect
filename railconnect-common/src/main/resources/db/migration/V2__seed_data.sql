-- ============================================================
-- RailConnect Seed Data - V2
-- ============================================================

-- ── STATIONS ─────────────────────────────────────────────────
INSERT INTO stations (station_code, station_name, city, state, zone, latitude, longitude) VALUES
('NDLS', 'New Delhi',          'New Delhi',  'Delhi',             'NR',  28.6429, 77.2194),
('MMCT', 'Mumbai Central',     'Mumbai',     'Maharashtra',       'WR',  18.9690, 72.8192),
('MAS',  'Chennai Central',    'Chennai',    'Tamil Nadu',        'SR',  13.0836, 80.2759),
('HYB',  'Hyderabad Deccan',   'Hyderabad',  'Telangana',         'SCR', 17.3850, 78.4867),
('SBC',  'Bangalore City',     'Bengaluru',  'Karnataka',         'SWR', 12.9779, 77.5713),
('CSTM', 'Mumbai CST',         'Mumbai',     'Maharashtra',       'CR',  18.9400, 72.8355),
('HWH',  'Howrah Junction',    'Kolkata',    'West Bengal',       'ER',  22.5837, 88.3425),
('SC',   'Secunderabad Jn',    'Hyderabad',  'Telangana',         'SCR', 17.4366, 78.4999),
('BZA',  'Vijayawada Jn',      'Vijayawada', 'Andhra Pradesh',    'SCR', 16.5177, 80.6192),
('PUNE', 'Pune Junction',      'Pune',       'Maharashtra',       'CR',  18.5293, 73.8742),
('JP',   'Jaipur Junction',    'Jaipur',     'Rajasthan',         'NWR', 26.9124, 75.7873),
('ADI',  'Ahmedabad Jn',       'Ahmedabad',  'Gujarat',           'WR',  23.0225, 72.5714),
('LKO',  'Lucknow NR',         'Lucknow',    'Uttar Pradesh',     'NR',  26.8467, 80.9462),
('PNBE', 'Patna Junction',     'Patna',      'Bihar',             'ECR', 25.6127, 85.1790),
('NGP',  'Nagpur Junction',    'Nagpur',     'Maharashtra',       'CR',  21.1458, 79.0882),
('GNT',  'Guntur Junction',    'Guntur',     'Andhra Pradesh',    'SCR', 16.3067, 80.4365),
('TPTY', 'Tirupati',           'Tirupati',   'Andhra Pradesh',    'SCR', 13.6288, 79.4192),
('VSKP', 'Visakhapatnam',      'Vizag',      'Andhra Pradesh',    'ECoR',17.6868, 83.2185),
('NZM',  'Hazrat Nizamuddin',  'New Delhi',  'Delhi',             'NR',  28.5893, 77.2508),
('BCT',  'Mumbai Bandra T',    'Mumbai',     'Maharashtra',       'WR',  19.0549, 72.8405);

-- ── TRAINS ───────────────────────────────────────────────────
INSERT INTO trains (train_number, train_name, train_type, running_days) VALUES
('12301', 'Howrah Rajdhani Express',      'RAJDHANI',    127),
('12951', 'Mumbai Rajdhani Express',      'RAJDHANI',    127),
('12621', 'Tamil Nadu Express',           'SUPERFAST',   127),
('12723', 'Telangana Express',            'SUPERFAST',   127),
('22693', 'KSK Rajdhani Express',         'RAJDHANI',    120),
('12028', 'Shatabdi Express',             'SHATABDI',    62),
('22221', 'Rajdhani Express NR-BCT',      'RAJDHANI',    127),
('12649', 'Karnataka Sampark Kranti',     'SUPERFAST',   113),
('22475', 'Bikaner AC SF Express',        'SUPERFAST',   99),
('06001', 'Vande Bharat Express',         'VANDE_BHARAT',62);

-- ── TRAIN ROUTES (Rajdhani NDLS→HWH as example) ──────────────
-- Train 12301 (Howrah Rajdhani): NDLS → NZM → PNBE → HWH
INSERT INTO train_routes (train_id, station_id, stop_number, arrival_time, departure_time, distance_from_source, day_number, halt_minutes)
SELECT t.id, s.id, stops.stop_number, stops.arrival_time::TIME, stops.dep_time::TIME, stops.dist, stops.day_num, stops.halt
FROM trains t, stations s,
(VALUES
  ('NDLS', 1, NULL,    '17:00', 0,   1, 0),
  ('NZM',  2, '17:15', '17:20', 14,  1, 5),
  ('PNBE', 3, '05:30', '05:40', 1000,2, 10),
  ('HWH',  4, '10:00', NULL,    1450,2, 0)
) AS stops(code, stop_number, arrival_time, dep_time, dist, day_num, halt)
WHERE t.train_number = '12301' AND s.station_code = stops.code;

-- Train 12951 (Mumbai Rajdhani): NDLS → JP → ADI → MMCT
INSERT INTO train_routes (train_id, station_id, stop_number, arrival_time, departure_time, distance_from_source, day_number, halt_minutes)
SELECT t.id, s.id, stops.stop_number, stops.arrival_time::TIME, stops.dep_time::TIME, stops.dist, stops.day_num, stops.halt
FROM trains t, stations s,
(VALUES
  ('NDLS', 1, NULL,    '16:55', 0,   1, 0),
  ('JP',   2, '20:30', '20:35', 308, 1, 5),
  ('ADI',  3, '02:05', '02:10', 943, 2, 5),
  ('MMCT', 4, '07:55', NULL,    1384,2, 0)
) AS stops(code, stop_number, arrival_time, dep_time, dist, day_num, halt)
WHERE t.train_number = '12951' AND s.station_code = stops.code;

-- Train 12621 (Tamil Nadu Express): NDLS → HYB → MAS
INSERT INTO train_routes (train_id, station_id, stop_number, arrival_time, departure_time, distance_from_source, day_number, halt_minutes)
SELECT t.id, s.id, stops.stop_number, stops.arrival_time::TIME, stops.dep_time::TIME, stops.dist, stops.day_num, stops.halt
FROM trains t, stations s,
(VALUES
  ('NDLS', 1, NULL,    '22:30', 0,    1, 0),
  ('NGP',  2, '12:20', '12:30', 1093, 2, 10),
  ('SC',   3, '21:30', '21:45', 1598, 2, 15),
  ('MAS',  4, '07:10', NULL,    2180, 3, 0)
) AS stops(code, stop_number, arrival_time, dep_time, dist, day_num, halt)
WHERE t.train_number = '12621' AND s.station_code = stops.code;

-- Train 12723 (Telangana Express): NDLS → HYB
INSERT INTO train_routes (train_id, station_id, stop_number, arrival_time, departure_time, distance_from_source, day_number, halt_minutes)
SELECT t.id, s.id, stops.stop_number, stops.arrival_time::TIME, stops.dep_time::TIME, stops.dist, stops.day_num, stops.halt
FROM trains t, stations s,
(VALUES
  ('NDLS', 1, NULL,    '06:25', 0,    1, 0),
  ('LKO',  2, '15:45', '15:55', 510,  1, 10),
  ('NGP',  3, '05:30', '05:45', 1189, 2, 15),
  ('SC',   4, '15:55', '16:10', 1734, 2, 15),
  ('HYB',  5, '17:00', NULL,    1751, 2, 0)
) AS stops(code, stop_number, arrival_time, dep_time, dist, day_num, halt)
WHERE t.train_number = '12723' AND s.station_code = stops.code;

-- ── COACHES (for each train, create standard coach set) ───────
INSERT INTO train_coaches (train_id, coach_number, seat_class, total_seats, tatkal_quota, premium_tatkal_quota, max_rac_seats, max_waitlist_seats)
SELECT t.id, c.coach_number, c.seat_class, c.total_seats, c.tatkal_q, c.prem_tatkal_q, c.rac, c.wl
FROM trains t,
(VALUES
  ('A1',  'S1', 48,  4,  2, 8,  20),
  ('B1',  'S2', 48,  4,  2, 8,  20),
  ('B2',  'S2', 48,  4,  2, 8,  20),
  ('C1',  'S3', 64,  6,  3, 12, 40),
  ('C2',  'S3', 64,  6,  3, 12, 40),
  ('C3',  'S3', 64,  6,  3, 12, 40),
  ('S1',  'SL', 72,  7,  3, 18, 50),
  ('S2',  'SL', 72,  7,  3, 18, 50),
  ('S3',  'SL', 72,  7,  3, 18, 50),
  ('S4',  'SL', 72,  7,  3, 18, 50),
  ('GEN', 'GN', 100, 0,  0, 0,  0)
) AS c(coach_number, seat_class, total_seats, tatkal_q, prem_tatkal_q, rac, wl)
WHERE t.train_number IN ('12301', '12951', '12621', '12723');

-- ── DEFAULT ADMIN USER ────────────────────────────────────────
-- Password: Admin@12345 (BCrypt hash)
INSERT INTO users (email, password, first_name, last_name, role, email_verified, enabled) VALUES
('admin@railconnect.in',
 '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewdBPj0oijcte5lm',
 'Rail', 'Admin', 'ROLE_ADMIN', TRUE, TRUE);

