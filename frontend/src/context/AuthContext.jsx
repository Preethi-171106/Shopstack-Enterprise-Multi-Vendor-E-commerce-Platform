import React, { createContext, useContext, useState, useEffect, useCallback } from 'react';
import authService from '../services/authService';

const AuthContext = createContext(null);

export const AuthProvider = ({ children }) => {
  const [user, setUser] = useState(() => {
    try {
      const saved = localStorage.getItem('shopstack_user');
      return saved ? JSON.parse(saved) : null;
    } catch {
      return null;
    }
  });
  const [token, setToken] = useState(() => localStorage.getItem('shopstack_token'));
  const [loading, setLoading] = useState(() => {
    const storedToken = localStorage.getItem('shopstack_token');
    const storedUser = localStorage.getItem('shopstack_user');
    // If we already have storedToken and storedUser, we can render immediately without blocking loading
    return Boolean(storedToken && !storedUser);
  });

  // Restore authenticated session on initial app load if token exists
  useEffect(() => {
    const initAuth = async () => {
      const storedToken = localStorage.getItem('shopstack_token');
      if (storedToken) {
        try {
          const userData = await authService.getCurrentUser();
          setUser(userData);
          setToken(storedToken);
          localStorage.setItem('shopstack_user', JSON.stringify(userData));
        } catch (err) {
          console.error('Session restoration check:', err);
          if (err.response?.status === 401 || err.response?.status === 403) {
            localStorage.removeItem('shopstack_token');
            localStorage.removeItem('shopstack_user');
            setToken(null);
            setUser(null);
          }
        }
      }
      setLoading(false);
    };

    initAuth();
  }, []);

  const login = useCallback(async (email, password) => {
    const loginData = await authService.login({ email, password });
    const accessToken = loginData.accessToken;
    const userData = loginData.user;

    localStorage.setItem('shopstack_token', accessToken);
    if (userData) {
      localStorage.setItem('shopstack_user', JSON.stringify(userData));
    }
    setToken(accessToken);
    setUser(userData);
    return loginData; // Return full loginData including user.role for post-login routing
  }, []);

  const register = useCallback(async (formData) => {
    const registeredUser = await authService.register(formData);
    return registeredUser;
  }, []);

  const logout = useCallback(() => {
    localStorage.removeItem('shopstack_token');
    localStorage.removeItem('shopstack_user');
    setToken(null);
    setUser(null);
  }, []);

  const value = {
    user,
    token,
    isAuthenticated: Boolean(token && user),
    loading,
    login,
    register,
    logout,
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
};

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};

export default AuthContext;
