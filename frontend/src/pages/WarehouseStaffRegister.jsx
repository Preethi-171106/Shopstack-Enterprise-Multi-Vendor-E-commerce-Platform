import React, { useState, useEffect } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { Mail, Lock, User, Phone, Building2, ShieldAlert, ArrowLeft, CheckCircle2, Eye, EyeOff, Boxes } from 'lucide-react';
import { useApp } from '../context/AppContext';
import Button from '../components/common/Button';
import warehouseStaffService from '../services/warehouseStaffService';
import api from '../services/api';

const WarehouseStaffRegister = () => {
  const navigate = useNavigate();
  const { showNotification } = useApp();

  const [formData, setFormData] = useState({
    firstName: '',
    lastName: '',
    email: '',
    phoneNumber: '',
    password: '',
    confirmPassword: '',
    warehouseId: '',
  });

  const [facilities, setFacilities] = useState([]);
  const [loadingFacilities, setLoadingFacilities] = useState(false);
  const [showPassword, setShowPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [isSuccess, setIsSuccess] = useState(false);

  // Load available facilities for preferred assignment
  useEffect(() => {
    const fetchFacilities = async () => {
      setLoadingFacilities(true);
      try {
        const response = await api.get('/categories'); // ping or lightweight fetch
        // Attempt to fetch active warehouses if available
        try {
          const whRes = await api.get('/admin/warehouses');
          if (Array.isArray(whRes.data)) {
            setFacilities(whRes.data);
          }
        } catch {
          // Warehouses endpoint may be protected, ignore if public cannot load
        }
      } catch (err) {
        console.warn('Facility list fetch notice:', err?.message);
      } finally {
        setLoadingFacilities(false);
      }
    };

    fetchFacilities();
  }, []);

  const handleChange = (field, value) => {
    setFormData((prev) => ({ ...prev, [field]: value }));
    if (error) setError('');
  };

  const handleSubmit = async (e) => {
    e.preventDefault();

    if (!formData.firstName.trim() || !formData.lastName.trim()) {
      setError('Please provide your full name (first and last name).');
      return;
    }
    if (!formData.phoneNumber.trim()) {
      setError('Phone number is required for warehouse verification.');
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
      setError('Passwords do not match. Please check and try again.');
      return;
    }

    setError('');
    setLoading(true);

    try {
      await warehouseStaffService.register({
        firstName: formData.firstName.trim(),
        lastName: formData.lastName.trim(),
        email: formData.email.trim(),
        phoneNumber: formData.phoneNumber.trim(),
        password: formData.password,
        warehouseId: formData.warehouseId ? Number(formData.warehouseId) : null,
      });

      setIsSuccess(true);
      showNotification('Registration submitted for review!', 'info', 'Pending Approval');
    } catch (err) {
      console.error('Warehouse staff registration error:', err);
      const status = err.response?.status;
      if (status === 409) {
        setError('An account with this email already exists. Please sign in or use a different email.');
      } else if (status === 400) {
        setError(err.userMessage || 'Please verify your information and try again.');
      } else {
        setError(err.userMessage || 'Registration failed. Please try again later.');
      }
    } finally {
      setLoading(false);
    }
  };

  if (isSuccess) {
    return (
      <div className="flex-1 flex items-center justify-center p-4 py-12 max-w-lg mx-auto w-full">
        <div className="w-full bg-slate-950/80 border border-slate-800 rounded-3xl p-6 sm:p-10 shadow-2xl space-y-6 text-center">
          <div className="w-16 h-16 rounded-3xl bg-amber-500/10 border border-amber-500/20 flex items-center justify-center mx-auto text-amber-400">
            <CheckCircle2 className="w-8 h-8" />
          </div>

          <div className="space-y-2">
            <h1 className="text-2xl font-black text-white tracking-tight">Application Submitted</h1>
            <p className="text-sm text-slate-300 leading-relaxed">
              Your Warehouse Staff registration has been received and is currently <span className="text-amber-400 font-bold uppercase tracking-wider">Pending Review</span>.
            </p>
          </div>

          <div className="p-4 rounded-2xl bg-slate-900/90 border border-slate-800 text-left space-y-2 text-xs text-slate-400">
            <div className="flex items-center gap-2 text-slate-200 font-semibold">
              <ShieldAlert className="w-4 h-4 text-indigo-400 shrink-0" />
              <span>Administrative Approval Required</span>
            </div>
            <p>
              To ensure warehouse operational security, all fulfillment staff accounts require verification. Once an administrator approves your account, you will be able to sign in and access the picking, packing, and inventory management consoles.
            </p>
          </div>

          <div className="pt-2 flex flex-col sm:flex-row gap-3">
            <Button
              variant="primary"
              className="w-full"
              onClick={() => navigate('/login')}
            >
              Go to Sign In
            </Button>
            <Button
              variant="outline"
              className="w-full"
              onClick={() => navigate('/')}
            >
              Back to Marketplace
            </Button>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="flex-1 flex items-center justify-center p-4 py-10 max-w-xl mx-auto w-full">
      <div className="w-full bg-slate-950/80 border border-slate-800 rounded-3xl p-6 sm:p-8 shadow-2xl space-y-6">

        <Link
          to="/login"
          className="inline-flex items-center gap-1.5 text-xs text-slate-400 hover:text-white transition-colors"
        >
          <ArrowLeft className="w-3.5 h-3.5" />
          <span>Back to Sign In</span>
        </Link>

        <div className="space-y-2 text-center">
          <div className="w-12 h-12 rounded-2xl bg-indigo-500/10 border border-indigo-500/20 flex items-center justify-center mx-auto text-indigo-400">
            <Boxes className="w-6 h-6" />
          </div>
          <h1 className="text-2xl font-black text-white tracking-tight">Warehouse Staff Registration</h1>
          <p className="text-xs text-slate-400">Join the ShopStack logistics and fulfillment operations team.</p>
        </div>

        {/* Security Alert Banner */}
        <div className="p-3.5 rounded-2xl bg-indigo-500/10 border border-indigo-500/20 flex items-start gap-3">
          <ShieldAlert className="w-4 h-4 text-indigo-400 shrink-0 mt-0.5" />
          <p className="text-xs text-indigo-200 leading-relaxed">
            <span className="font-semibold text-white">Notice: </span>
            Your Warehouse Staff registration will be reviewed and approved by an administrator before access to the fulfillment console is activated.
          </p>
        </div>

        {error && (
          <div className="p-3 rounded-xl bg-rose-500/10 border border-rose-500/30 text-rose-400 text-xs text-center font-medium">
            {error}
          </div>
        )}

        <form onSubmit={handleSubmit} className="space-y-4" autoComplete="off" spellCheck={false}>
          {/* First & Last Name */}
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
            <div className="space-y-1.5">
              <label className="block text-xs font-semibold uppercase tracking-wider text-slate-300">
                First Name <span className="text-rose-400">*</span>
              </label>
              <div className="relative">
                <User className="w-4 h-4 text-slate-400 absolute left-3.5 top-1/2 -translate-y-1/2 pointer-events-none" />
                <input
                  type="text"
                  required
                  value={formData.firstName}
                  onChange={(e) => handleChange('firstName', e.target.value)}
                  placeholder="First Name"
                  className="w-full bg-slate-900/90 text-white text-sm rounded-xl border border-slate-700/80 pl-10 pr-3.5 py-2.5 placeholder-slate-500 focus:outline-none focus:border-indigo-500 focus:ring-1 focus:ring-indigo-500 transition-all duration-200"
                />
              </div>
            </div>

            <div className="space-y-1.5">
              <label className="block text-xs font-semibold uppercase tracking-wider text-slate-300">
                Last Name <span className="text-rose-400">*</span>
              </label>
              <div className="relative">
                <User className="w-4 h-4 text-slate-400 absolute left-3.5 top-1/2 -translate-y-1/2 pointer-events-none" />
                <input
                  type="text"
                  required
                  value={formData.lastName}
                  onChange={(e) => handleChange('lastName', e.target.value)}
                  placeholder="Last Name"
                  className="w-full bg-slate-900/90 text-white text-sm rounded-xl border border-slate-700/80 pl-10 pr-3.5 py-2.5 placeholder-slate-500 focus:outline-none focus:border-indigo-500 focus:ring-1 focus:ring-indigo-500 transition-all duration-200"
                />
              </div>
            </div>
          </div>

          {/* Email Address */}
          <div className="space-y-1.5">
            <label className="block text-xs font-semibold uppercase tracking-wider text-slate-300">
              Work / Contact Email <span className="text-rose-400">*</span>
            </label>
            <div className="relative">
              <Mail className="w-4 h-4 text-slate-400 absolute left-3.5 top-1/2 -translate-y-1/2 pointer-events-none" />
              <input
                type="email"
                required
                value={formData.email}
                onChange={(e) => handleChange('email', e.target.value)}
                placeholder="staff@warehouse.shopstack.com"
                className="w-full bg-slate-900/90 text-white text-sm rounded-xl border border-slate-700/80 pl-10 pr-3.5 py-2.5 placeholder-slate-500 focus:outline-none focus:border-indigo-500 focus:ring-1 focus:ring-indigo-500 transition-all duration-200"
              />
            </div>
          </div>

          {/* Phone Number */}
          <div className="space-y-1.5">
            <label className="block text-xs font-semibold uppercase tracking-wider text-slate-300">
              Phone Number <span className="text-rose-400">*</span>
            </label>
            <div className="relative">
              <Phone className="w-4 h-4 text-slate-400 absolute left-3.5 top-1/2 -translate-y-1/2 pointer-events-none" />
              <input
                type="tel"
                required
                value={formData.phoneNumber}
                onChange={(e) => handleChange('phoneNumber', e.target.value)}
                placeholder="+91 98765 43210"
                className="w-full bg-slate-900/90 text-white text-sm rounded-xl border border-slate-700/80 pl-10 pr-3.5 py-2.5 placeholder-slate-500 focus:outline-none focus:border-indigo-500 focus:ring-1 focus:ring-indigo-500 transition-all duration-200"
              />
            </div>
          </div>

          {/* Preferred Facility (Optional) */}
          {facilities.length > 0 && (
            <div className="space-y-1.5">
              <label className="block text-xs font-semibold uppercase tracking-wider text-slate-300">
                Preferred Facility / Depot (Optional)
              </label>
              <div className="relative">
                <Building2 className="w-4 h-4 text-slate-400 absolute left-3.5 top-1/2 -translate-y-1/2 pointer-events-none" />
                <select
                  value={formData.warehouseId}
                  onChange={(e) => handleChange('warehouseId', e.target.value)}
                  className="w-full bg-slate-900/90 text-white text-sm rounded-xl border border-slate-700/80 pl-10 pr-3.5 py-2.5 focus:outline-none focus:border-indigo-500 focus:ring-1 focus:ring-indigo-500 transition-all duration-200"
                >
                  <option value="">-- Select Preferred Warehouse --</option>
                  {facilities.map((wh) => (
                    <option key={wh.id} value={wh.id}>
                      {wh.name} ({wh.warehouseCode}) - {wh.city}
                    </option>
                  ))}
                </select>
              </div>
            </div>
          )}

          {/* Password & Confirm Password */}
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
            <div className="space-y-1.5">
              <label className="block text-xs font-semibold uppercase tracking-wider text-slate-300">
                Password <span className="text-rose-400">*</span>
              </label>
              <div className="relative">
                <Lock className="w-4 h-4 text-slate-400 absolute left-3.5 top-1/2 -translate-y-1/2 pointer-events-none" />
                <input
                  type={showPassword ? 'text' : 'password'}
                  required
                  value={formData.password}
                  onChange={(e) => handleChange('password', e.target.value)}
                  placeholder="Min 6 characters"
                  className="w-full bg-slate-900/90 text-white text-sm rounded-xl border border-slate-700/80 pl-10 pr-10 py-2.5 placeholder-slate-500 focus:outline-none focus:border-indigo-500 focus:ring-1 focus:ring-indigo-500 transition-all duration-200"
                />
                <button
                  type="button"
                  onClick={() => setShowPassword((prev) => !prev)}
                  className="absolute right-3 top-1/2 -translate-y-1/2 text-slate-400 hover:text-slate-200 transition-colors"
                >
                  {showPassword ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                </button>
              </div>
            </div>

            <div className="space-y-1.5">
              <label className="block text-xs font-semibold uppercase tracking-wider text-slate-300">
                Confirm Password <span className="text-rose-400">*</span>
              </label>
              <div className="relative">
                <Lock className="w-4 h-4 text-slate-400 absolute left-3.5 top-1/2 -translate-y-1/2 pointer-events-none" />
                <input
                  type={showConfirmPassword ? 'text' : 'password'}
                  required
                  value={formData.confirmPassword}
                  onChange={(e) => handleChange('confirmPassword', e.target.value)}
                  placeholder="Confirm password"
                  className="w-full bg-slate-900/90 text-white text-sm rounded-xl border border-slate-700/80 pl-10 pr-10 py-2.5 placeholder-slate-500 focus:outline-none focus:border-indigo-500 focus:ring-1 focus:ring-indigo-500 transition-all duration-200"
                />
                <button
                  type="button"
                  onClick={() => setShowConfirmPassword((prev) => !prev)}
                  className="absolute right-3 top-1/2 -translate-y-1/2 text-slate-400 hover:text-slate-200 transition-colors"
                >
                  {showConfirmPassword ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                </button>
              </div>
            </div>
          </div>

          <Button
            type="submit"
            variant="primary"
            size="lg"
            className="w-full shadow-lg shadow-indigo-600/25 mt-4"
            isLoading={loading}
            disabled={loading}
            icon={Boxes}
          >
            {loading ? 'Submitting Application...' : 'Submit Staff Registration'}
          </Button>
        </form>

        <div className="text-center pt-2 text-xs text-slate-400 border-t border-slate-800 space-y-2">
          <div>
            Already have an active account?{' '}
            <Link to="/login" className="font-bold text-indigo-400 hover:underline">
              Sign In
            </Link>
          </div>
          <div>
            Registering as shopper or seller?{' '}
            <Link to="/register" className="font-semibold text-slate-300 hover:text-white underline">
              Customer / Vendor Registration
            </Link>
          </div>
        </div>

      </div>
    </div>
  );
};

export default WarehouseStaffRegister;
