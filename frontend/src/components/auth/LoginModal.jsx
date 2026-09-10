import React, { useState } from 'react';
import { X, Mail, Lock, LogIn } from 'lucide-react';
import { useAuth } from '../../context/AuthContext';
import { useApp } from '../../context/AppContext';
import Input from '../common/Input';
import Button from '../common/Button';

const LoginModal = ({ isOpen, onClose, onSwitchToRegister }) => {
  const { login } = useAuth();
  const { showNotification } = useApp();

  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  if (!isOpen) return null;

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!email.trim() || !password.trim()) {
      setError('Please enter both email and password.');
      return;
    }

    setError('');
    setLoading(true);

    try {
      await login(email.trim(), password);
      showNotification('Successfully logged in!', 'success', 'Welcome Back');
      onClose();
    } catch (err) {
      console.error('Login error:', err);
      setError(err.userMessage || 'Invalid email or password.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-950/80 backdrop-blur-md animate-in fade-in duration-200">
      <div className="relative w-full max-w-md bg-slate-900 border border-slate-800 rounded-3xl p-6 sm:p-8 shadow-2xl space-y-6">
        
        {/* Close Button */}
        <button
          onClick={onClose}
          className="absolute top-5 right-5 text-slate-400 hover:text-white p-1 rounded-xl hover:bg-slate-800 transition-colors"
        >
          <X className="w-5 h-5" />
        </button>

        {/* Modal Header */}
        <div className="space-y-2 text-center">
          <div className="w-12 h-12 rounded-2xl bg-indigo-500/10 border border-indigo-500/20 flex items-center justify-center mx-auto text-indigo-400">
            <LogIn className="w-6 h-6" />
          </div>
          <h2 className="text-2xl font-extrabold text-white tracking-tight">Sign In to ShopStack</h2>
          <p className="text-xs text-slate-400">Access your account and order details.</p>
        </div>

        {error && (
          <div className="p-3 rounded-xl bg-rose-500/10 border border-rose-500/30 text-rose-400 text-xs text-center font-medium">
            {error}
          </div>
        )}

        {/* Form */}
        <form onSubmit={handleSubmit} className="space-y-4" autoComplete="off">
          <Input
            label="Email Address"
            type="email"
            name="shopstack_login_email"
            id="shopstack_login_email"
            required
            value={email}
            onChange={(e) => { setEmail(e.target.value); if (error) setError(''); }}
            placeholder="Email Address"
            icon={Mail}
            autoComplete="new-password"
          />

          <Input
            label="Password"
            type="password"
            name="shopstack_login_password"
            id="shopstack_login_password"
            required
            value={password}
            onChange={(e) => { setPassword(e.target.value); if (error) setError(''); }}
            placeholder="Password"
            icon={Lock}
            autoComplete="new-password"
          />

          <Button
            type="submit"
            variant="primary"
            size="lg"
            className="w-full shadow-lg shadow-indigo-600/25 mt-2"
            isLoading={loading}
            disabled={loading}
            icon={LogIn}
          >
            {loading ? 'Authenticating...' : 'Sign In'}
          </Button>
        </form>

        {/* Footer Toggle */}
        <div className="text-center pt-2 text-xs text-slate-400 border-t border-slate-800">
          Don't have an account?{' '}
          <button
            onClick={() => {
              onClose();
              if (onSwitchToRegister) onSwitchToRegister();
            }}
            className="font-bold text-indigo-400 hover:underline"
          >
            Create Account
          </button>
        </div>

      </div>
    </div>
  );
};

export default LoginModal;
