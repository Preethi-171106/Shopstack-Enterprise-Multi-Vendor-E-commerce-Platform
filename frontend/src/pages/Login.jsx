import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { Mail, Lock, LogIn, ArrowLeft, Eye, EyeOff } from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import { useApp } from '../context/AppContext';
import Button from '../components/common/Button';

const Login = () => {
  const navigate = useNavigate();
  const { login } = useAuth();
  const { showNotification } = useApp();

  // Both fields start empty — no default credentials, no autofill
  const [email, setEmail]             = useState('');
  const [password, setPassword]       = useState('');
  const [showPassword, setShowPassword] = useState(false); // hidden by default
  const [loading, setLoading]         = useState(false);
  const [error, setError]             = useState('');

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!email.trim() || !password.trim()) {
      setError('Please enter both email and password.');
      return;
    }

    setError('');
    setLoading(true);

    try {
      const loginData = await login(email.trim(), password);
      showNotification('Welcome back!', 'success', 'Sign In Successful');

      // Route to role-specific dashboard
      const role = loginData?.user?.role;
      if (role === 'ADMIN') {
        navigate('/dashboard/admin');
      } else if (role === 'VENDOR') {
        navigate('/dashboard/vendor');
      } else if (role === 'WAREHOUSE_STAFF') {
        navigate('/dashboard/warehouse');
      } else if (role === 'CUSTOMER') {
        navigate('/dashboard/customer');
      } else {
        navigate('/');
      }
    } catch (err) {
      console.error('Login error:', err);
      setError(err.userMessage || 'Invalid email or password.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="flex-1 flex items-center justify-center p-4 py-12 max-w-md mx-auto w-full">
      <div className="w-full bg-slate-950/80 border border-slate-800 rounded-3xl p-6 sm:p-8 shadow-2xl space-y-6">

        <Link
          to="/"
          className="inline-flex items-center gap-1.5 text-xs text-slate-400 hover:text-white transition-colors"
        >
          <ArrowLeft className="w-3.5 h-3.5" />
          <span>Back to Marketplace</span>
        </Link>

        <div className="space-y-2 text-center">
          <div className="w-12 h-12 rounded-2xl bg-indigo-500/10 border border-indigo-500/20 flex items-center justify-center mx-auto text-indigo-400">
            <LogIn className="w-6 h-6" />
          </div>
          <h1 className="text-2xl font-extrabold text-white tracking-tight">Sign In to ShopStack</h1>
          <p className="text-xs text-slate-400">Access your account and order preferences.</p>
        </div>

        {error && (
          <div className="p-3 rounded-xl bg-rose-500/10 border border-rose-500/30 text-rose-400 text-xs text-center font-medium">
            {error}
          </div>
        )}

        {/* autocomplete="off" on the form discourages browser-level autofill */}
        <form onSubmit={handleSubmit} className="space-y-4" autoComplete="off" spellCheck={false}>

          {/* Email — always empty on open, no default value */}
          <div className="space-y-1.5">
            <label className="block text-xs font-semibold uppercase tracking-wider text-slate-300">
              Email Address
            </label>
            <div className="relative">
              <Mail className="w-4 h-4 text-slate-400 absolute left-3.5 top-1/2 -translate-y-1/2 pointer-events-none" />
              <input
                type="email"
                name="shopstack_page_email"
                id="shopstack_page_email"
                required
                value={email}
                onChange={(e) => { setEmail(e.target.value); if (error) setError(''); }}
                placeholder="Email Address"
                autoComplete="new-password"
                className="w-full bg-slate-900/90 text-white text-sm rounded-xl border border-slate-700/80 pl-10 pr-3.5 py-2.5 placeholder-slate-500 focus:outline-none focus:border-indigo-500 focus:ring-1 focus:ring-indigo-500 transition-all duration-200"
              />
            </div>
          </div>

          {/* Password — hidden by default, toggle with eye button */}
          <div className="space-y-1.5">
            <label className="block text-xs font-semibold uppercase tracking-wider text-slate-300">
              Password
            </label>
            <div className="relative">
              <Lock className="w-4 h-4 text-slate-400 absolute left-3.5 top-1/2 -translate-y-1/2 pointer-events-none" />
              <input
                type={showPassword ? 'text' : 'password'}
                name="shopstack_page_password"
                id="shopstack_page_password"
                required
                value={password}
                onChange={(e) => { setPassword(e.target.value); if (error) setError(''); }}
                placeholder="Password"
                autoComplete="new-password"
                className="w-full bg-slate-900/90 text-white text-sm rounded-xl border border-slate-700/80 pl-10 pr-10 py-2.5 placeholder-slate-500 focus:outline-none focus:border-indigo-500 focus:ring-1 focus:ring-indigo-500 transition-all duration-200"
              />
              {/* Show / Hide password toggle */}
              <button
                type="button"
                onClick={() => setShowPassword((prev) => !prev)}
                className="absolute right-3.5 top-1/2 -translate-y-1/2 text-slate-400 hover:text-slate-200 transition-colors focus:outline-none"
                aria-label={showPassword ? 'Hide password' : 'Show password'}
                tabIndex={-1}
              >
                {showPassword ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
              </button>
            </div>
          </div>

          <div className="flex justify-end">
            <Link to="/forgot-password" className="text-xs font-medium text-indigo-400 hover:text-indigo-300 transition-colors">
              Forgot Password?
            </Link>
          </div>

          <Button
            type="submit"
            variant="primary"
            size="lg"
            className="w-full shadow-lg shadow-indigo-600/25 mt-2"
            isLoading={loading}
            disabled={loading}
            icon={LogIn}
          >
            {loading ? 'Signing in...' : 'Sign In'}
          </Button>
        </form>

        <div className="text-center pt-2 text-xs text-slate-400 border-t border-slate-800 space-y-2">
          <div>
            Don't have an account?{' '}
            <Link to="/register" className="font-bold text-indigo-400 hover:underline">
              Register now
            </Link>
          </div>
          <div>
            Fulfillment staff?{' '}
            <Link to="/register/warehouse-staff" className="font-semibold text-slate-300 hover:text-white underline">
              Register as Warehouse Staff
            </Link>
          </div>
        </div>

      </div>
    </div>
  );
};

export default Login;
