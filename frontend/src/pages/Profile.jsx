import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { User, Mail, Phone, ShieldCheck, LogOut, Package, Truck, AlertCircle } from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import { useApp } from '../context/AppContext';
import Button from '../components/common/Button';
import Badge from '../components/common/Badge';
import api from '../services/api';

const Profile = () => {
  const navigate = useNavigate();
  const { user, isAuthenticated, logout } = useAuth();
  const { showNotification } = useApp();
  const apiBaseUrl = import.meta.env.VITE_API_BASE_URL || (import.meta.env.DEV ? 'http://localhost:8080/api' : '/api');

  const [shipments, setShipments] = useState([]);
  const [loadingShipments, setLoadingShipments] = useState(false);

  useEffect(() => {
    if (!isAuthenticated) {
      navigate('/login');
      return;
    }

    // Fetch shipments for customer
    const fetchShipments = async () => {
      setLoadingShipments(true);
      try {
        const response = await api.get('/customer/shipments');
        setShipments(Array.isArray(response.data) ? response.data : []);
      } catch (err) {
        // Shipments may not exist yet for this customer — silently ignore 404
        if (err?.response?.status !== 404) {
          console.error('Customer shipments fetch failed:', err?.userMessage || err?.message);
        }
      } finally {
        setLoadingShipments(false);
      }
    };

    fetchShipments();
  }, [isAuthenticated, navigate]);

  if (!isAuthenticated || !user) {
    return null;
  }

  const handleLogout = () => {
    logout();
    showNotification('Logged out successfully', 'info');
    navigate('/');
  };

  return (
    <div className="flex-1 max-w-5xl mx-auto px-4 sm:px-6 lg:px-8 py-8 w-full space-y-8">
      
      {/* Header Banner */}
      <div className="p-6 sm:p-8 rounded-3xl bg-slate-950/80 border border-slate-800 flex flex-col sm:flex-row items-start sm:items-center justify-between gap-6 shadow-xl">
        <div className="flex items-center gap-4">
          <div className="w-16 h-16 rounded-2xl bg-gradient-to-tr from-indigo-600 to-violet-500 flex items-center justify-center text-white text-2xl font-black shadow-lg shadow-indigo-500/25">
            {user.firstName ? user.firstName.charAt(0).toUpperCase() : 'U'}
          </div>
          <div>
            <div className="flex items-center gap-2">
              <h1 className="text-2xl font-extrabold text-white">
                {user.firstName} {user.lastName}
              </h1>
              <Badge color="indigo" size="sm">
                {user.role || 'CUSTOMER'}
              </Badge>
            </div>
            <p className="text-xs text-slate-400 mt-1">{user.email}</p>
          </div>
        </div>

        <Button
          variant="outline"
          size="md"
          onClick={handleLogout}
          icon={LogOut}
          className="hover:bg-rose-500/10 hover:text-rose-400 hover:border-rose-500/30"
        >
          Sign Out
        </Button>
      </div>

      {/* Grid details */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-8 items-start">
        
        {/* User Account Info */}
        <div className="lg:col-span-1 p-6 rounded-2xl bg-slate-950/70 border border-slate-800 space-y-4">
          <h2 className="text-base font-bold text-white border-b border-slate-800 pb-3 flex items-center gap-2">
            <User className="w-4 h-4 text-indigo-400" />
            Account Overview
          </h2>

          <div className="space-y-3 text-xs">
            <div>
              <span className="text-slate-500 block uppercase font-semibold text-[10px]">Full Name</span>
              <span className="text-slate-200 font-medium">{user.firstName} {user.lastName}</span>
            </div>

            <div>
              <span className="text-slate-500 block uppercase font-semibold text-[10px]">Email Address</span>
              <span className="text-slate-200 font-medium flex items-center gap-1.5">
                <Mail className="w-3.5 h-3.5 text-slate-400" />
                {user.email}
              </span>
            </div>

            <div>
              <span className="text-slate-500 block uppercase font-semibold text-[10px]">Phone Number</span>
              <span className="text-slate-200 font-medium flex items-center gap-1.5">
                <Phone className="w-3.5 h-3.5 text-slate-400" />
                {user.phoneNumber || 'Not provided'}
              </span>
            </div>

            <div>
              <span className="text-slate-500 block uppercase font-semibold text-[10px]">Account Status</span>
              <span className="text-emerald-400 font-medium flex items-center gap-1">
                <ShieldCheck className="w-3.5 h-3.5" />
                Verified Customer (Active)
              </span>
            </div>
          </div>
        </div>

        {/* Customer Shipments & Activity */}
        <div className="lg:col-span-2 space-y-6">
          <div className="p-6 rounded-2xl bg-slate-950/70 border border-slate-800 space-y-4">
            <h2 className="text-base font-bold text-white border-b border-slate-800 pb-3 flex items-center gap-2">
              <Truck className="w-4 h-4 text-indigo-400" />
              Customer Shipments &amp; Deliveries
            </h2>

            {loadingShipments ? (
              <p className="text-xs text-slate-400">Loading shipments...</p>
            ) : shipments.length > 0 ? (
              <div className="space-y-3">
                {shipments.map((s) => (
                  <div key={s.id} className="p-4 rounded-xl bg-slate-900 border border-slate-800 flex items-center justify-between text-xs">
                    <div>
                      <p className="font-semibold text-white">Tracking #{s.trackingNumber || s.id}</p>
                      <p className="text-slate-400">Order ID: {s.orderId}</p>
                    </div>
                    <Badge color={s.status === 'DELIVERED' ? 'emerald' : 'indigo'} size="sm">
                      {s.status}
                    </Badge>
                  </div>
                ))}
              </div>
            ) : (
              <div className="p-4 rounded-xl bg-slate-900/60 border border-slate-800/80 text-xs text-slate-400 space-y-2">
                <div className="flex items-center gap-2 text-indigo-300 font-semibold">
                  <Package className="w-4 h-4 text-indigo-400" />
                  <span>No active shipments found</span>
                </div>
                <p className="text-[11px] text-slate-400 leading-relaxed">
                  Your placed orders and tracking updates will appear here once dispatched by vendor partners.
                </p>
              </div>
            )}
          </div>

          <div className="p-4 rounded-xl bg-indigo-500/5 border border-indigo-500/20 text-xs text-slate-300 flex items-start gap-3">
            <AlertCircle className="w-4 h-4 text-indigo-400 shrink-0 mt-0.5" />
            <div>
              <span className="font-bold text-white">Backend Integration Status</span>
              <p className="text-[11px] text-slate-400 mt-0.5">
                Connected to Spring Boot backend at <code className="font-mono text-indigo-300">{apiBaseUrl}</code> using standard JWT authentication.
              </p>
            </div>
          </div>
        </div>

      </div>

    </div>
  );
};

export default Profile;
