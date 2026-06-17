import { useState, useEffect, useRef } from 'react';
import { trainsAPI } from '../services/api';
import './StationAutocomplete.css';

export default function StationAutocomplete({ label, placeholder, value, onChange, icon }) {
  const [query, setQuery] = useState('');
  const [suggestions, setSuggestions] = useState([]);
  const [loading, setLoading] = useState(false);
  const [isOpen, setIsOpen] = useState(false);
  const wrapperRef = useRef(null);

  // Synchronize initial input display value with station code
  useEffect(() => {
    if (value) {
      // If query is empty or matches code format, we can keep it.
      // If value is a code, we display it, otherwise if we already set a full string, we keep it.
      if (!query || query === value) {
        setQuery(value);
      }
    } else {
      setQuery('');
    }
  }, [value]);

  useEffect(() => {
    const handleClickOutside = (event) => {
      if (wrapperRef.current && !wrapperRef.current.contains(event.target)) {
        setIsOpen(false);
      }
    };
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  const handleInputChange = async (e) => {
    const val = e.target.value;
    setQuery(val);
    onChange(val.toUpperCase()); // Update parent with raw input so standard searches still function

    if (val.trim().length >= 2) {
      setLoading(true);
      setIsOpen(true);
      try {
        const res = await trainsAPI.searchStations(val);
        setSuggestions(res.data.data || []);
      } catch (err) {
        setSuggestions([]);
      } finally {
        setLoading(false);
      }
    } else {
      setSuggestions([]);
      setIsOpen(false);
    }
  };

  const handleSelect = (station) => {
    const displayName = `${station.stationName} (${station.stationCode})`;
    setQuery(displayName);
    onChange(station.stationCode);
    setIsOpen(false);
  };

  return (
    <div className="autocomplete-wrapper" ref={wrapperRef}>
      <label className="form-label">{label}</label>
      <div className="station-input-wrap">
        {icon && <span className="station-icon">{icon}</span>}
        <input
          className="form-input"
          placeholder={placeholder}
          value={query}
          onChange={handleInputChange}
          onFocus={() => { if (query.trim().length >= 2) setIsOpen(true); }}
          required
        />
        {loading && <span className="autocomplete-spinner" />}
      </div>
      {isOpen && suggestions.length > 0 && (
        <ul className="autocomplete-suggestions">
          {suggestions.map((s) => (
            <li key={s.id} onClick={() => handleSelect(s)}>
              <div className="as-header">
                <span className="as-code">{s.stationCode}</span>
                <span className="as-name">{s.stationName}</span>
              </div>
              <span className="as-city">{s.city}, {s.state}</span>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
