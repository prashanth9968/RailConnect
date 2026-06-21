import { createContext, useContext, useState, useEffect } from 'react';
import { authAPI } from '../services/api';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    // Restore session from localStorage
    const token = localStorage.getItem('accessToken');
    const savedUser = localStorage.getItem('user');
    const lastActivity = localStorage.getItem('lastActivity');
    
    if (token && savedUser) {
      let shouldLogout = false;
      if (lastActivity) {
        const diff = Date.now() - parseInt(lastActivity, 10);
        if (diff > 24 * 60 * 60 * 1000) {
          shouldLogout = true;
        }
      } else {
        localStorage.setItem('lastActivity', Date.now().toString());
      }

      if (shouldLogout) {
        localStorage.removeItem('accessToken');
        localStorage.removeItem('refreshToken');
        localStorage.removeItem('user');
        localStorage.removeItem('lastActivity');
        setUser(null);
      } else {
        try {
          setUser(JSON.parse(savedUser));
          localStorage.setItem('lastActivity', Date.now().toString());
        } catch {
          localStorage.clear();
        }
      }
    }
    setLoading(false);
  }, []);

  const login = async (email, password) => {
    const res = await authAPI.login({ email, password });
    const data = res.data.data;
    const responseUser = data.user || {};
    localStorage.setItem('accessToken', data.accessToken);
    localStorage.setItem('refreshToken', data.refreshToken);
    localStorage.setItem('lastActivity', Date.now().toString());
    const userData = {
      id: responseUser.id,
      name: [responseUser.firstName, responseUser.lastName].filter(Boolean).join(' ') || email.split('@')[0],
      email: responseUser.email || email,
      role: responseUser.role || 'ROLE_USER',
    };
    localStorage.setItem('user', JSON.stringify(userData));
    setUser(userData);
    return userData;
  };

  const register = async (name, email, password, phone) => {
    const [firstName, ...lastNameParts] = name.trim().split(/\s+/);
    const payload = {
      firstName,
      lastName: lastNameParts.join(' ') || firstName,
      email,
      password,
      phone: phone ? phone.replace(/\D/g, '') : undefined,
    };
    const res = await authAPI.register(payload);
    const data = res.data.data;
    const responseUser = data.user || {};
    localStorage.setItem('accessToken', data.accessToken);
    localStorage.setItem('refreshToken', data.refreshToken);
    localStorage.setItem('lastActivity', Date.now().toString());
    const userData = {
      id: responseUser.id,
      name: [responseUser.firstName, responseUser.lastName].filter(Boolean).join(' ') || name,
      email: responseUser.email || email,
      role: responseUser.role || 'ROLE_USER',
    };
    localStorage.setItem('user', JSON.stringify(userData));
    setUser(userData);
    return userData;
  };

  const logout = async () => {
    try { await authAPI.logout(); } catch { /* ignore */ }
    localStorage.removeItem('accessToken');
    localStorage.removeItem('refreshToken');
    localStorage.removeItem('user');
    localStorage.removeItem('lastActivity');
    setUser(null);
  };

  return (
    <AuthContext.Provider value={{ user, loading, login, register, logout, isAuthenticated: !!user }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used within AuthProvider');
  return ctx;
}
