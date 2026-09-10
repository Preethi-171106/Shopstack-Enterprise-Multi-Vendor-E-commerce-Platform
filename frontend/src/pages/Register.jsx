import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { Mail, Lock, User, Phone, UserPlus, ArrowLeft, Store, ShoppingBag, ShieldCheck, Eye, EyeOff } from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import { useApp } from '../context/AppContext';
import Button from '../components/common/Button';

/**
 * Account types available for public self-service registration.
 * WAREHOUSE_STAFF is excluded — it must be created by an admin via the staff management API.
 */
const ACCOUNT_TYPES = [
  {
    value: 'CUSTOMER',
    label: 'Customer',
    description: 'Shop from verified vendors',
    icon: ShoppingBag,
  },
  {
    value: 'VENDOR',
    label: 'Vendor',
    description: 'Sell your products',
    icon: Store,
  },
  {
    value: 'ADMIN',
    label: 'Admin',
    description: 'Manage the platform',
    icon: ShieldCheck,
  },
];

const Register = () => {
  const navigate = useNavigate();
  const { register, login } = useAuth();
  const { showNotification } = useApp();

  // All fields start empty — no default credentials, no autofill from state
  const [formData, setFormData] = useState({
    firstName: '',
    lastName: '',
    email: '',
    phoneNumber: '',
    password: '',
    confirmPassword: '',
    registrationRole: '',
  });

  const [showPassword, setShowPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const handleChange = (field, value) => {
    setFormData((prev) => ({ ...prev, [field]: value }));
    if (error) setError('');
  };

  const handleSubmit = async (e) => {
    e.preventDefault();

    // ── Frontend validation ────────────────────────────────────────
    if (!formData.firstName.trim() || !formData.lastName.trim()) {
      setError('Full name is required (first and last name).');
      return;
    }
    if (!formData.phoneNumber.trim()) {
      setError('Phone number is required.');
      return;
    }
    if (!formData.email.trim()) {
      setError('Email address is required.');
      return;
    }
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(formData.email.trim())) {
      setError('Please enter a valid email address.');
      return;
    }
    if (!formData.password) {
      setError('Password is required.');
      return;
    }
    if (formData.password.length < 6) {
      setError('Password must be at least 6 characters long.');
      return;
    }
    if (!formData.confirmPassword) {
      setError('Please confirm your password.');
      return;
    }
    if (formData.password !== formData.confirmPassword) {
      setError('Passwords do not match. Please try again.');
      return;
    }
    if (!formData.registrationRole) {
      setError('Please select an account type.');
      return;
    }

    setError('');
    setLoading(true);

    try {
      // Register — backend validates the role server-side
      await register({
        firstName: formData.firstName.trim(),
        lastName: formData.lastName.trim(),
        email: formData.email.trim(),
        password: formData.password,
        phoneNumber: formData.phoneNumber.trim(),
        registrationRole: formData.registrationRole,
      });

      const roleLabel =
        formData.registrationRole === 'VENDOR' ? 'Vendor' :
        formData.registrationRole === 'ADMIN'  ? 'Admin'  : 'Customer';

      showNotification(`${roleLabel} account created!`, 'success', 'Welcome to ShopStack');

      // Auto-login after successful registration
      const loginData = await login(formData.email.trim(), formData.password);

      // Route to the correct dashboard based on role returned in JWT
      const role = loginData?.user?.role;
      if (role === 'ADMIN') {
        navigate('/dashboard/admin');
      } else if (role === 'VENDOR') {
        navigate('/dashboard/vendor');
      } else if (role === 'WAREHOUSE_STAFF') {
        navigate('/dashboard/warehouse');
      } else {
        navigate('/dashboard/customer');
      }
    } catch (err) {
      console.error('Registration error:', err);
      const status = err.response?.status;
      if (status === 409) {
        setError('An account with this email already exists. Please sign in or use a different email.');
      } else if (status === 400) {
        setError(err.userMessage || 'Please check your information and try again.');
      } else if (!err.response) {
        setError(
          'Unable to connect to the backend. ' +
          'Make sure Spring Boot is running on http://localhost:8080.'
        );
      } else if (status === 500) {
        setError('Server error. Please try again later.');
      } else {
        setError(err.userMessage || 'Registration failed. Please try again.');
      }
    } finally {
      setLoading(false);
    }
  };

  // ── Render ────────────────────────────────────────────────────────────────
  return (
    <div className="flex-1 flex items-center justify-center p-4 py-8 w-full">
      <div
        className="w-full bg-slate-950/80 border border-slate-800 rounded-3xl p-6 sm:p-8 shadow-2xl space-y-5"
        style={{ maxWidth: '480px' }}
      >
        {/* Back link */}
        <Link
          to="/"
          className="inline-flex items-center gap-1.5 text-xs text-slate-400 hover:text-white transition-colors"
        >
          <ArrowLeft className="w-3.5 h-3.5" />
          Back to Marketplace
        </Link>

        {/* Heading */}
        <div className="space-y-1 text-center">
          <div className="w-12 h-12 rounded-2xl bg-indigo-500/10 border border-indigo-500/20 flex items-center justify-center mx-auto text-indigo-400">
            <UserPlus className="w-6 h-6" />
          </div>
          <h1 className="text-2xl font-extrabold text-white tracking-tight">
            Create Your ShopStack Account
          </h1>
          <p className="text-xs text-slate-400">Join as a Customer, Vendor, or Admin.</p>
        </div>

        {/* Error banner */}
        {error && (
          <div className="p-3 rounded-xl bg-rose-500/10 border border-rose-500/30 text-rose-400 text-xs text-center font-medium">
            {error}
          </div>
        )}

        {/* Form — autoComplete="off" tells the browser not to autofill the whole form */}
        <form onSubmit={handleSubmit} className="space-y-4" autoComplete="off" spellCheck={false}>

          {/* ── ACCOUNT TYPE ─────────────────────────────────────────────── */}
          <div className="space-y-2">
            <p className="text-xs font-semibold uppercase tracking-wider text-slate-300">
              Choose Account Type <span className="text-rose-400">*</span>
            </p>

            {/* Three equal-width buttons in a single row */}
            <div
              style={{
                display: 'grid',
                gridTemplateColumns: '1fr 1fr 1fr',
                gap: '10px',
              }}
            >
              {ACCOUNT_TYPES.map(({ value, label, description, icon: Icon }) => {
                const isSelected = formData.registrationRole === value;

                // Per-role accent colours
                const accentMap = {
                  CUSTOMER: {
                    selected: 'border-indigo-500 bg-indigo-500/10',
                    iconColor: isSelected ? '#818cf8' : '#64748b',
                    labelColor: isSelected ? '#fff' : '#cbd5e1',
                  },
                  VENDOR: {
                    selected: 'border-violet-500 bg-violet-500/10',
                    iconColor: isSelected ? '#a78bfa' : '#64748b',
                    labelColor: isSelected ? '#fff' : '#cbd5e1',
                  },
                  ADMIN: {
                    selected: 'border-amber-500 bg-amber-500/10',
                    iconColor: isSelected ? '#fbbf24' : '#64748b',
                    labelColor: isSelected ? '#fff' : '#cbd5e1',
                  },
                };
                const accent = accentMap[value];

                return (
                  <button
                    key={value}
                    type="button"
                    onClick={() => handleChange('registrationRole', value)}
                    aria-pressed={isSelected}
                    style={{
                      display: 'flex',
                      flexDirection: 'column',
                      alignItems: 'center',
                      justifyContent: 'center',
                      gap: '6px',
                      padding: '12px 8px',
                      borderRadius: '12px',
                      border: '1px solid',
                      cursor: 'pointer',
                      transition: 'all 0.15s',
                      outline: 'none',
                      // Visibility always on — no display:none anywhere
                    }}
                    className={`
                      focus:ring-2 focus:ring-indigo-500
                      ${isSelected
                        ? `${accent.selected} shadow-lg`
                        : 'border-slate-700 bg-slate-900/60 hover:border-slate-500'
                      }
                    `}
                  >
                    <Icon
                      className="w-5 h-5 flex-shrink-0"
                      style={{ color: accent.iconColor }}
                    />
                    <span
                      className="text-xs font-bold leading-tight text-center"
                      style={{ color: accent.labelColor }}
                    >
                      {label}
                    </span>
                    <span className="text-slate-500 leading-tight text-center" style={{ fontSize: '10px' }}>
                      {description}
                    </span>
                  </button>
                );
              })}
            </div>

            {/* Context notices */}
            {formData.registrationRole === 'VENDOR' && (
              <p className="text-[11px] text-amber-400/80 bg-amber-500/5 border border-amber-500/20 rounded-lg px-3 py-2">
                Vendor accounts require approval before selling products.
              </p>
            )}
            {formData.registrationRole === 'ADMIN' && (
              <p className="text-[11px] text-amber-400/80 bg-amber-500/5 border border-amber-500/20 rounded-lg px-3 py-2">
                Admin accounts have full platform access.
              </p>
            )}
          </div>

          {/* ── FULL NAME ─────────────────────────────────────────────────── */}
          <div>
            <label className="block text-xs font-semibold uppercase tracking-wider text-slate-300 mb-1.5">
              Full Name <span className="text-rose-400">*</span>
            </label>
            <div className="grid grid-cols-2 gap-3">
              <div className="relative">
                <User className="w-4 h-4 text-slate-400 absolute left-3.5 top-1/2 -translate-y-1/2 pointer-events-none" />
                <input
                  type="text"
                  required
                  value={formData.firstName}
                  onChange={(e) => handleChange('firstName', e.target.value)}
                  placeholder="First Name"
                  autoComplete="given-name"
                  className="w-full bg-slate-900/90 text-white text-sm rounded-xl border border-slate-700/80 pl-10 pr-3.5 py-2.5 placeholder-slate-500 focus:outline-none focus:border-indigo-500 focus:ring-1 focus:ring-indigo-500 transition-all"
                />
              </div>
              <div className="relative">
                <input
                  type="text"
                  required
                  value={formData.lastName}
                  onChange={(e) => handleChange('lastName', e.target.value)}
                  placeholder="Last Name"
                  autoComplete="family-name"
                  className="w-full bg-slate-900/90 text-white text-sm rounded-xl border border-slate-700/80 px-3.5 py-2.5 placeholder-slate-500 focus:outline-none focus:border-indigo-500 focus:ring-1 focus:ring-indigo-500 transition-all"
                />
              </div>
            </div>
          </div>

          {/* ── PHONE ─────────────────────────────────────────────────────── */}
          <div>
            <label className="block text-xs font-semibold uppercase tracking-wider text-slate-300 mb-1.5">
              Phone Number <span className="text-rose-400">*</span>
            </label>
            <div className="relative">
              <Phone className="w-4 h-4 text-slate-400 absolute left-3.5 top-1/2 -translate-y-1/2 pointer-events-none" />
              <input
                type="tel"
                required
                value={formData.phoneNumber}
                onChange={(e) => handleChange('phoneNumber', e.target.value)}
                placeholder="Phone Number"
                autoComplete="tel"
                className="w-full bg-slate-900/90 text-white text-sm rounded-xl border border-slate-700/80 pl-10 pr-3.5 py-2.5 placeholder-slate-500 focus:outline-none focus:border-indigo-500 focus:ring-1 focus:ring-indigo-500 transition-all"
              />
            </div>
          </div>

          {/* ── EMAIL ─────────────────────────────────────────────────────── */}
          <div>
            <label className="block text-xs font-semibold uppercase tracking-wider text-slate-300 mb-1.5">
              Email Address <span className="text-rose-400">*</span>
            </label>
            <div className="relative">
              <Mail className="w-4 h-4 text-slate-400 absolute left-3.5 top-1/2 -translate-y-1/2 pointer-events-none" />
              <input
                type="email"
                required
                value={formData.email}
                onChange={(e) => handleChange('email', e.target.value)}
                placeholder="Email Address"
                autoComplete="email"
                className="w-full bg-slate-900/90 text-white text-sm rounded-xl border border-slate-700/80 pl-10 pr-3.5 py-2.5 placeholder-slate-500 focus:outline-none focus:border-indigo-500 focus:ring-1 focus:ring-indigo-500 transition-all"
              />
            </div>
          </div>

          {/* ── PASSWORD ──────────────────────────────────────────────────── */}
          <div>
            <label className="block text-xs font-semibold uppercase tracking-wider text-slate-300 mb-1.5">
              Password <span className="text-rose-400">*</span>
            </label>
            <div className="relative">
              <Lock className="w-4 h-4 text-slate-400 absolute left-3.5 top-1/2 -translate-y-1/2 pointer-events-none" />
              <input
                type={showPassword ? 'text' : 'password'}
                required
                value={formData.password}
                onChange={(e) => handleChange('password', e.target.value)}
                placeholder="Password (min. 6 characters)"
                autoComplete="new-password"
                className="w-full bg-slate-900/90 text-white text-sm rounded-xl border border-slate-700/80 pl-10 pr-10 py-2.5 placeholder-slate-500 focus:outline-none focus:border-indigo-500 focus:ring-1 focus:ring-indigo-500 transition-all"
              />
              <button
                type="button"
                onClick={() => setShowPassword((p) => !p)}
                className="absolute right-3.5 top-1/2 -translate-y-1/2 text-slate-400 hover:text-slate-200 transition-colors focus:outline-none"
                aria-label={showPassword ? 'Hide password' : 'Show password'}
                tabIndex={-1}
              >
                {showPassword ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
              </button>
            </div>
          </div>

          {/* ── CONFIRM PASSWORD ──────────────────────────────────────────── */}
          <div>
            <label className="block text-xs font-semibold uppercase tracking-wider text-slate-300 mb-1.5">
              Confirm Password <span className="text-rose-400">*</span>
            </label>
            <div className="relative">
              <Lock className="w-4 h-4 text-slate-400 absolute left-3.5 top-1/2 -translate-y-1/2 pointer-events-none" />
              <input
                type={showConfirmPassword ? 'text' : 'password'}
                required
                value={formData.confirmPassword}
                onChange={(e) => handleChange('confirmPassword', e.target.value)}
                placeholder="Re-enter your password"
                autoComplete="new-password"
                className="w-full bg-slate-900/90 text-white text-sm rounded-xl border border-slate-700/80 pl-10 pr-10 py-2.5 placeholder-slate-500 focus:outline-none focus:border-indigo-500 focus:ring-1 focus:ring-indigo-500 transition-all"
              />
              <button
                type="button"
                onClick={() => setShowConfirmPassword((p) => !p)}
                className="absolute right-3.5 top-1/2 -translate-y-1/2 text-slate-400 hover:text-slate-200 transition-colors focus:outline-none"
                aria-label={showConfirmPassword ? 'Hide password' : 'Show password'}
                tabIndex={-1}
              >
                {showConfirmPassword ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
              </button>
            </div>
          </div>

          {/* ── SUBMIT ────────────────────────────────────────────────────── */}
          <Button
            type="submit"
            variant="primary"
            size="lg"
            className="w-full shadow-lg shadow-indigo-600/25 mt-1"
            isLoading={loading}
            disabled={loading}
            icon={UserPlus}
          >
            {loading
              ? 'Creating Account...'
              : `Register as ${
                  formData.registrationRole === 'VENDOR' ? 'Vendor' :
                  formData.registrationRole === 'ADMIN'  ? 'Admin'  : 'Customer'
                }`}
          </Button>
        </form>

        {/* Sign in link & Warehouse staff registration */}
        <div className="text-center text-xs text-slate-400 border-t border-slate-800 pt-3 space-y-2">
          <div>
            Already have an account?{' '}
            <Link to="/login" className="font-bold text-indigo-400 hover:underline">
              Sign In
            </Link>
          </div>
          <div>
            Looking to join the logistics & warehouse team?{' '}
            <Link to="/register/warehouse-staff" className="font-semibold text-slate-300 hover:text-white underline">
              Register as Warehouse Staff &rarr;
            </Link>
          </div>
        </div>
      </div>
    </div>
  );
};

export default Register;
