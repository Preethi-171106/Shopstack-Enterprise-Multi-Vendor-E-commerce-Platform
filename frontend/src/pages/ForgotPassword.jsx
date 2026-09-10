import React, { useState } from 'react';
import { Link } from 'react-router-dom';
import { ArrowLeft, Mail, KeyRound } from 'lucide-react';
import Button from '../components/common/Button';
import authService from '../services/authService';

const ForgotPassword = () => {
  const [email, setEmail] = useState('');
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');
  const [devResetToken, setDevResetToken] = useState('');

  const handleSubmit = async (e) => {
    e.preventDefault();

    const trimmedEmail = email.trim();
    if (!trimmedEmail) {
      setError('Please enter your email address.');
      return;
    }

    setLoading(true);
    setError('');
    setMessage('');
    setDevResetToken('');

    try {
      const response = await authService.forgotPassword(trimmedEmail);
      const token = response?.resetToken || response?.token || response?.link?.match(/[?&]token=([^&]+)/)?.[1];
      if (token) {
        setDevResetToken(token);
        setMessage('A password reset link has been generated for local testing. Use the token below to reset your password.');
      } else {
        setMessage(response?.message || 'If an account exists for this email, a reset link has been sent.');
      }
    } catch (err) {
      setError(err.userMessage || 'Unable to send a reset link right now.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="flex-1 flex items-center justify-center p-4 py-12 w-full">
      <div className="w-full max-w-md bg-slate-950/80 border border-slate-800 rounded-3xl p-6 sm:p-8 shadow-2xl">
        <Link to="/login" className="inline-flex items-center gap-1.5 text-xs text-slate-400 hover:text-white transition-colors">
          <ArrowLeft className="w-3.5 h-3.5" />
          Back to Login
        </Link>

        <div className="mt-6 text-center space-y-2">
          <div className="w-12 h-12 rounded-2xl bg-indigo-500/10 border border-indigo-500/20 flex items-center justify-center mx-auto text-indigo-400">
            <KeyRound className="w-6 h-6" />
          </div>
          <h1 className="text-2xl font-extrabold text-white tracking-tight">Forgot Password</h1>
          <p className="text-xs text-slate-400">Enter your email to receive a reset link.</p>
        </div>

        {error && (
          <div className="mt-5 p-3 rounded-xl bg-rose-500/10 border border-rose-500/30 text-rose-400 text-xs text-center font-medium">
            {error}
          </div>
        )}

        {message && (
          <div className="mt-5 p-3 rounded-xl bg-emerald-500/10 border border-emerald-500/30 text-emerald-300 text-xs text-center font-medium">
            {message}
          </div>
        )}

        <form onSubmit={handleSubmit} className="mt-6 space-y-4" autoComplete="off" spellCheck={false}>
          <div className="space-y-1.5">
            <label className="block text-xs font-semibold uppercase tracking-wider text-slate-300">Email Address</label>
            <div className="relative">
              <Mail className="w-4 h-4 text-slate-400 absolute left-3.5 top-1/2 -translate-y-1/2 pointer-events-none" />
              <input
                type="email"
                required
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                placeholder="you@example.com"
                autoComplete="username"
                className="w-full bg-slate-900/90 text-white text-sm rounded-xl border border-slate-700/80 pl-10 pr-3.5 py-2.5 placeholder-slate-500 focus:outline-none focus:border-indigo-500 focus:ring-1 focus:ring-indigo-500 transition-all duration-200"
              />
            </div>
          </div>

          <Button type="submit" variant="primary" size="lg" className="w-full" isLoading={loading} disabled={loading}>
            {loading ? 'Sending...' : 'Send Reset Link'}
          </Button>
        </form>

        {devResetToken && (
          <div className="mt-5 rounded-2xl border border-indigo-500/30 bg-indigo-500/10 p-3 text-xs text-indigo-200 break-all">
            <div className="font-semibold mb-1">Development reset token</div>
            <div>{devResetToken}</div>
            <Link to={`/reset-password?token=${encodeURIComponent(devResetToken)}`} className="mt-3 inline-flex font-semibold text-indigo-300 hover:text-white">
              Open reset form →
            </Link>
          </div>
        )}
      </div>
    </div>
  );
};

export default ForgotPassword;
