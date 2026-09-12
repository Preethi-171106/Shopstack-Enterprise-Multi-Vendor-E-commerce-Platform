import React, { useState, useEffect } from 'react';
import { X, Mail, Lock, User, Phone, UserPlus, Loader2 } from 'lucide-react';
import { useAuth } from '../../context/AuthContext';
import { useApp } from '../../context/AppContext';
import Input from '../common/Input';
import Button from '../common/Button';

const RegisterModal = ({ isOpen, onClose, onSwitchToLogin }) => {
  const { register, login } = useAuth();
  const { showNotification } = useApp();

  const [formData, setFormData] = useState({
    firstName: '',
    lastName: '',
    email: '',
    password: '',
    phoneNumber: '',
  });

  const [loading, setLoading] = useState(false);
  const [isWakingUp, setIsWakingUp] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    let timer;
    if (loading) {
      timer = setTimeout(() => {
        setIsWakingUp(true);
      }, 3000);
    } else {
      setIsWakingUp(false);
    }
    return () => clearTimeout(timer);
  }, [loading]);

  if (!isOpen) return null;

  const handleChange = (field, value) => {
    setFormData((prev) => ({ ...prev, [field]: value }));
    if (error) setError('');
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!formData.firstName.trim() || !formData.lastName.trim() || !formData.email.trim() || !formData.password.trim()) {
      setError('Please fill in all required fields.');
      return;
    }

    if (formData.password.length < 6) {
      setError('Password must be at least 6 characters long.');
      return;
    }

    setError('');
    setLoading(true);

    try {
      await register(formData);
      showNotification('Account created successfully! Logging you in...', 'success', 'Registration Complete');
      await login(formData.email.trim(), formData.password);
      onClose();
    } catch (err) {
      console.error('Registration error:', err);
      setError(err.userMessage || 'Registration failed. Please check details and try again.');
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

        {/* Header */}
        <div className="space-y-2 text-center">
          <div className="w-12 h-12 rounded-2xl bg-indigo-500/10 border border-indigo-500/20 flex items-center justify-center mx-auto text-indigo-400">
            <UserPlus className="w-6 h-6" />
          </div>
          <h2 className="text-2xl font-extrabold text-white tracking-tight">Create an Account</h2>
          <p className="text-xs text-slate-400">Join ShopStack to shop from verified global vendors.</p>
        </div>

        {error && (
          <div className="p-3 rounded-xl bg-rose-500/10 border border-rose-500/30 text-rose-400 text-xs text-center font-medium space-y-1">
            <p>{error}</p>
          </div>
        )}

        {/* Form */}
        <form onSubmit={handleSubmit} className="space-y-3.5" autoComplete="off">
          <div className="grid grid-cols-2 gap-3">
            <Input
              label="First Name"
              name="firstName"
              required
              value={formData.firstName}
              onChange={(e) => handleChange('firstName', e.target.value)}
              placeholder="First Name"
              icon={User}
              autoComplete="off"
            />
            <Input
              label="Last Name"
              name="lastName"
              required
              value={formData.lastName}
              onChange={(e) => handleChange('lastName', e.target.value)}
              placeholder="Last Name"
              autoComplete="off"
            />
          </div>

          <Input
            label="Email Address"
            type="email"
            name="email"
            required
            value={formData.email}
            onChange={(e) => handleChange('email', e.target.value)}
            placeholder="Email Address"
            icon={Mail}
            autoComplete="off"
          />

          <Input
            label="Phone Number"
            type="tel"
            name="phoneNumber"
            value={formData.phoneNumber}
            onChange={(e) => handleChange('phoneNumber', e.target.value)}
            placeholder="Phone Number"
            icon={Phone}
            autoComplete="off"
          />

          <Input
            label="Password"
            type="password"
            name="password"
            required
            value={formData.password}
            onChange={(e) => handleChange('password', e.target.value)}
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
            icon={UserPlus}
          >
            {loading ? (isWakingUp ? 'Connecting to Server...' : 'Creating Account...') : 'Register'}
          </Button>

          {loading && isWakingUp && (
            <div className="flex items-center justify-center gap-2 text-[11px] text-amber-400/90 text-center animate-pulse pt-1">
              <Loader2 className="w-3 h-3 animate-spin" />
              <span>Free-tier cloud backend is starting up. Please wait...</span>
            </div>
          )}
        </form>

        {/* Footer Toggle */}
        <div className="text-center pt-2 text-xs text-slate-400 border-t border-slate-800">
          Already have an account?{' '}
          <button
            onClick={() => {
              onClose();
              if (onSwitchToLogin) onSwitchToLogin();
            }}
            className="font-bold text-indigo-400 hover:underline"
          >
            Sign In
          </button>
        </div>

      </div>
    </div>
  );
};

export default RegisterModal;
