import { createContext, useContext, useEffect, useState, useCallback } from 'react';
import { supabase } from '../lib/supabase';
import * as api from '../lib/api';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [profile, setProfile] = useState(null);
  const [loading, setLoading] = useState(true);

  const loadProfile = useCallback(async (userId) => {
    try {
      let p = await api.getProfile(userId);
      if (!p) {
        // create profile row on first login
        const { data: authUser } = await supabase.auth.getUser();
        const meta = authUser?.user?.user_metadata || {};
        p = await api.upsertProfile({
          id: userId,
          email: authUser?.user?.email || '',
          full_name: meta.full_name || '',
          role: meta.role || 'CUSTOMER',
        });
      }
      setProfile(p);
      return p;
    } catch (e) {
      setProfile(null);
      return null;
    }
  }, []);

  useEffect(() => {
    let mounted = true;
    (async () => {
      try {
        const { data } = await supabase.auth.getSession();
        if (data.session?.user) {
          if (mounted) setUser(data.session.user);
          await loadProfile(data.session.user.id);
        }
      } catch {
        // ignore
      } finally {
        if (mounted) setLoading(false);
      }
    })();
    const { data: sub } = supabase.auth.onAuthStateChange((_event, session) => {
      (async () => {
        if (session?.user) {
          setUser(session.user);
          await loadProfile(session.user.id);
        } else {
          setUser(null);
          setProfile(null);
        }
      })();
    });
    return () => {
      mounted = false;
      sub.subscription.unsubscribe();
    };
  }, [loadProfile]);

  const login = useCallback(async (email, password) => {
    const data = await api.signIn({ email, password });
    await loadProfile(data.user.id);
    setUser(data.user);
    return data;
  }, [loadProfile]);

  const register = useCallback(async ({ email, password, fullName, role }) => {
    const data = await api.signUp({ email, password, fullName, role });
    // On signUp with email confirmation OFF, a session may be returned.
    if (data.user) {
      // ensure profile row exists
      await api.upsertProfile({
        id: data.user.id,
        email,
        full_name: fullName,
        role: role || 'CUSTOMER',
      });
    }
    return data;
  }, []);

  const logout = useCallback(async () => {
    await api.signOut();
    setUser(null);
    setProfile(null);
  }, []);

  const refreshProfile = useCallback(async () => {
    if (user) await loadProfile(user.id);
  }, [user, loadProfile]);

  const value = {
    user,
    profile,
    loading,
    isAuthenticated: !!user,
    role: profile?.role || 'GUEST',
    login,
    register,
    logout,
    refreshProfile,
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used within AuthProvider');
  return ctx;
}
