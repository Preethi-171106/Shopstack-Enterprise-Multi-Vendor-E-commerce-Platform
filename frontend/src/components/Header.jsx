import React, { useState, useEffect, useCallback } from 'react';
import { Link, useNavigate, useLocation } from 'react-router-dom';
import { useSelector, useDispatch } from 'react-redux';
import {
  ShoppingBag,
  Search,
  Heart,
  ShoppingCart,
  User,
  Menu,
  X,
  ChevronRight,
  LogOut,
  UserCheck,
  Bell,
  CheckCheck,
  Clock,
  Package,
  AlertCircle,
  Sparkles,
  RotateCcw,
  Store,
  ExternalLink,
  Warehouse,
  ShieldCheck,
} from 'lucide-react';
import { APP_NAME } from '../utils/constants';
import { useAuth } from '../context/AuthContext';
import { useApp } from '../context/AppContext';
import { notificationService } from '../services/notificationService';
import Button from './common/Button';
import LoginModal from './auth/LoginModal';
import RegisterModal from './auth/RegisterModal';
import { selectCartTotalCount, fetchCartThunk, clearCart } from '../store/slices/cartSlice';
import { selectWishlistCount, fetchWishlistThunk } from '../store/slices/wishlistSlice';

const Header = () => {
  const [searchQuery, setSearchQuery] = useState('');
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);
  const [loginModalOpen, setLoginModalOpen] = useState(false);
  const [registerModalOpen, setRegisterModalOpen] = useState(false);
  const [userMenuOpen, setUserMenuOpen] = useState(false);
  const [notificationOpen, setNotificationOpen] = useState(false);
  const [unreadNotifications, setUnreadNotifications] = useState(0);
  const [recentNotifications, setRecentNotifications] = useState([]);
  const [loadingNotifications, setLoadingNotifications] = useState(false);

  const navigate = useNavigate();
  const location = useLocation();
  const dispatch = useDispatch();
  const { user, isAuthenticated, logout } = useAuth();
  const { showNotification } = useApp();

  const loadUnreadCount = useCallback(async () => {
    if (!isAuthenticated) return;
    try {
      const data = await notificationService.getUnreadCount();
      setUnreadNotifications(data.unreadCount || 0);
    } catch (e) {
      // quiet fail for poll
    }
  }, [isAuthenticated]);

  const loadRecentNotifications = useCallback(async () => {
    if (!isAuthenticated) return;
    try {
      setLoadingNotifications(true);
      const data = await notificationService.getMyNotifications('ALL', 0, 5);
      setRecentNotifications(data.content || []);
    } catch (e) {
      console.error('Failed to load notifications', e);
    } finally {
      setLoadingNotifications(false);
    }
  }, [isAuthenticated]);

  useEffect(() => {
    if (isAuthenticated) {
      dispatch(fetchCartThunk());
      dispatch(fetchWishlistThunk());
      loadUnreadCount();
    }
  }, [dispatch, isAuthenticated, loadUnreadCount]);

  useEffect(() => {
    if (notificationOpen) {
      loadRecentNotifications();
    }
  }, [notificationOpen, loadRecentNotifications]);

  const handleMarkNotificationAsRead = async (id, e) => {
    if (e) e.stopPropagation();
    try {
      await notificationService.markAsRead(id);
      setRecentNotifications((prev) =>
        prev.map((n) => (n.id === id ? { ...n, readStatus: true } : n))
      );
      setUnreadNotifications((prev) => Math.max(0, prev - 1));
    } catch (err) {
      console.error('Failed to mark read', err);
    }
  };

  const handleMarkAllNotificationsRead = async () => {
    try {
      await notificationService.markAllAsRead();
      setRecentNotifications((prev) => prev.map((n) => ({ ...n, readStatus: true })));
      setUnreadNotifications(0);
      showNotification('All notifications marked as read', 'success');
    } catch (err) {
      console.error('Failed to mark all read', err);
    }
  };

  const cartCount = useSelector(selectCartTotalCount);
  const wishlistCount = useSelector(selectWishlistCount);

  const handleSearchSubmit = (e) => {
    e.preventDefault();
    if (searchQuery.trim()) {
      navigate(`/products?search=${encodeURIComponent(searchQuery.trim())}`);
      setMobileMenuOpen(false);
    }
  };

  const handleLogout = () => {
    dispatch(clearCart());
    logout();
    setUserMenuOpen(false);
    showNotification('Logged out successfully', 'info');
    navigate('/');
  };

  const isActiveRoute = (path) => {
    if (path === '/') return location.pathname === '/';
    return location.pathname.startsWith(path);
  };

  return (
    <>
      <header className="sticky top-0 z-40 backdrop-blur-md bg-slate-950/85 border-b border-slate-800 text-white transition-all">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex items-center justify-between h-16 gap-4">
            
            {/* Logo Branding */}
            <Link to="/" className="flex items-center gap-3 shrink-0 group">
              <div className="h-10 w-10 rounded-xl bg-gradient-to-tr from-indigo-600 via-indigo-500 to-violet-500 flex items-center justify-center shadow-lg shadow-indigo-500/25 group-hover:scale-105 transition-transform">
                <ShoppingBag className="w-5 h-5 text-white" />
              </div>
              <div>
                <div className="flex items-center gap-2">
                  <span className="font-extrabold text-xl tracking-tight bg-gradient-to-r from-white via-slate-200 to-indigo-300 bg-clip-text text-transparent">
                    {APP_NAME}
                  </span>
                  <span className="text-[10px] font-bold tracking-wider uppercase px-2 py-0.5 rounded-full bg-indigo-500/10 text-indigo-400 border border-indigo-500/20 hidden sm:inline-block">
                    Marketplace
                  </span>
                </div>
              </div>
            </Link>

            {/* Desktop Navigation Links */}
            <nav className="hidden md:flex items-center gap-6 text-sm font-medium text-slate-300">
              <Link
                to="/"
                className={`transition-colors hover:text-white ${
                  isActiveRoute('/') ? 'text-indigo-400 font-semibold' : ''
                }`}
              >
                Home
              </Link>
              <Link
                to="/products"
                className={`transition-colors hover:text-white ${
                  location.pathname === '/products' ? 'text-indigo-400 font-semibold' : ''
                }`}
              >
                Explore Products
              </Link>
            </nav>

            {/* Search Bar - Center Desktop */}
            <form
              onSubmit={handleSearchSubmit}
              className="hidden lg:flex flex-1 max-w-xs xl:max-w-md relative items-center"
            >
              <input
                type="text"
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                placeholder="Search products, brands, categories..."
                className="w-full bg-slate-900/90 text-xs text-white rounded-xl border border-slate-800 pl-9 pr-8 py-2 placeholder-slate-500 focus:outline-none focus:border-indigo-500 focus:ring-1 focus:ring-indigo-500 transition-all"
              />
              <Search className="w-4 h-4 text-slate-400 absolute left-3 pointer-events-none" />
              {searchQuery && (
                <button
                  type="button"
                  onClick={() => setSearchQuery('')}
                  className="absolute right-2.5 text-slate-400 hover:text-white"
                >
                  <X className="w-3.5 h-3.5" />
                </button>
              )}
            </form>

            {/* Right Action Icons & Auth */}
            <div className="flex items-center gap-2 sm:gap-3">
              {/* Wishlist Icon */}
              <Link
                to="/wishlist"
                className="relative p-2 rounded-xl text-slate-300 hover:text-white hover:bg-slate-800/60 transition-colors"
                title="View Wishlist"
              >
                <Heart className="w-5 h-5" />
                {wishlistCount > 0 && (
                  <span className="absolute top-1 right-1 w-4 h-4 rounded-full bg-rose-500 text-white text-[10px] font-bold flex items-center justify-center animate-in zoom-in-50">
                    {wishlistCount}
                  </span>
                )}
              </Link>

              {/* Cart Icon */}
              <Link
                to="/cart"
                className="relative p-2 rounded-xl text-slate-300 hover:text-white hover:bg-slate-800/60 transition-colors"
                title="View Cart"
              >
                <ShoppingCart className="w-5 h-5" />
                {cartCount > 0 && (
                  <span className="absolute top-1 right-1 w-4 h-4 rounded-full bg-indigo-500 text-white text-[10px] font-bold flex items-center justify-center animate-in zoom-in-50">
                    {cartCount}
                  </span>
                )}
              </Link>

              {/* Notification Bell Icon & Dropdown */}
              {isAuthenticated && (
                <div className="relative">
                  <button
                    onClick={() => {
                      setNotificationOpen(!notificationOpen);
                      setUserMenuOpen(false);
                    }}
                    className="relative p-2 rounded-xl text-slate-300 hover:text-white hover:bg-slate-800/60 transition-colors"
                    title="Notifications"
                  >
                    <Bell className="w-5 h-5" />
                    {unreadNotifications > 0 && (
                      <span className="absolute top-1 right-1 w-4 h-4 rounded-full bg-indigo-500 text-white text-[10px] font-bold flex items-center justify-center animate-pulse">
                        {unreadNotifications > 99 ? '99+' : unreadNotifications}
                      </span>
                    )}
                  </button>

                  {/* Notification Dropdown Panel */}
                  {notificationOpen && (
                    <div className="absolute right-0 mt-2 w-80 sm:w-96 bg-slate-900 border border-slate-800 rounded-2xl shadow-2xl py-3 text-xs space-y-2 z-50 animate-in fade-in duration-150">
                      <div className="px-4 pb-2 border-b border-slate-800 flex items-center justify-between">
                        <div className="flex items-center gap-2">
                          <span className="font-bold text-white text-sm">Notifications</span>
                          {unreadNotifications > 0 && (
                            <span className="px-2 py-0.5 rounded-full text-[10px] font-bold bg-indigo-500/20 text-indigo-400 border border-indigo-500/30">
                              {unreadNotifications} new
                            </span>
                          )}
                        </div>
                        {unreadNotifications > 0 && (
                          <button
                            onClick={handleMarkAllNotificationsRead}
                            className="text-[11px] text-indigo-400 hover:text-indigo-300 flex items-center gap-1 font-semibold"
                          >
                            <CheckCheck className="w-3.5 h-3.5" /> Mark all read
                          </button>
                        )}
                      </div>

                      {/* Notifications List */}
                      <div className="max-h-72 overflow-y-auto px-2 space-y-1.5">
                        {loadingNotifications ? (
                          <div className="py-6 text-center text-slate-500">Loading notifications...</div>
                        ) : recentNotifications.length === 0 ? (
                          <div className="py-6 text-center text-slate-500">No notifications yet</div>
                        ) : (
                          recentNotifications.map((n) => (
                            <div
                              key={n.id}
                              onClick={() => {
                                if (!n.readStatus) handleMarkNotificationAsRead(n.id);
                                setNotificationOpen(false);
                                navigate('/notifications');
                              }}
                              className={`p-2.5 rounded-xl transition-all cursor-pointer flex items-start gap-2.5 ${
                                !n.readStatus
                                  ? 'bg-slate-800/90 border border-indigo-500/30'
                                  : 'hover:bg-slate-800/50'
                              }`}
                            >
                              <div className="shrink-0 mt-0.5 p-1.5 rounded-lg bg-slate-800 border border-slate-700 text-indigo-400">
                                {n.notificationType?.startsWith('ORDER_') ? (
                                  <Package className="w-4 h-4" />
                                ) : n.notificationType?.startsWith('RETURN_') || n.notificationType?.startsWith('REFUND_') ? (
                                  <RotateCcw className="w-4 h-4 text-amber-400" />
                                ) : n.notificationType?.startsWith('VENDOR_') ? (
                                  <Store className="w-4 h-4 text-emerald-400" />
                                ) : n.notificationType === 'LOW_STOCK' ? (
                                  <AlertCircle className="w-4 h-4 text-rose-400" />
                                ) : (
                                  <Bell className="w-4 h-4" />
                                )}
                              </div>
                              <div className="flex-1 min-w-0">
                                <div className="flex items-center justify-between">
                                  <p className={`font-semibold text-xs truncate ${!n.readStatus ? 'text-white' : 'text-slate-300'}`}>
                                    {n.title}
                                  </p>
                                  <span className="text-[10px] text-slate-500 shrink-0">
                                    {new Date(n.createdAt).toLocaleDateString(undefined, { month: 'short', day: 'numeric' })}
                                  </span>
                                </div>
                                <p className="text-[11px] text-slate-400 line-clamp-1 mt-0.5">
                                  {n.message}
                                </p>
                              </div>
                            </div>
                          ))
                        )}
                      </div>

                      {/* Footer */}
                      <div className="pt-2 px-3 border-t border-slate-800 text-center">
                        <Link
                          to="/notifications"
                          onClick={() => setNotificationOpen(false)}
                          className="text-xs font-semibold text-indigo-400 hover:text-indigo-300 flex items-center justify-center gap-1 py-1"
                        >
                          <span>View all notifications</span>
                          <ExternalLink className="w-3.5 h-3.5" />
                        </Link>
                      </div>
                    </div>
                  )}
                </div>
              )}

              {/* Auth Controls */}
              <div className="hidden sm:flex items-center gap-2 border-l border-slate-800 pl-3 ml-1 relative">
                {isAuthenticated && user ? (
                  <div className="relative">
                    <button
                      onClick={() => setUserMenuOpen(!userMenuOpen)}
                      className="flex items-center gap-2 px-3 py-1.5 rounded-xl bg-slate-900 border border-slate-800 hover:border-slate-700 text-xs font-semibold text-white transition-all"
                    >
                      <div className="w-6 h-6 rounded-lg bg-indigo-600 text-white text-xs flex items-center justify-center font-bold">
                        {user.firstName ? user.firstName.charAt(0).toUpperCase() : 'U'}
                      </div>
                      <span className="max-w-[100px] truncate">{user.firstName || 'User'}</span>
                    </button>

                    {/* User Dropdown */}
                    {userMenuOpen && (
                      <div className="absolute right-0 mt-2 w-48 bg-slate-900 border border-slate-800 rounded-2xl shadow-xl py-2 text-xs space-y-1 animate-in fade-in duration-150">
                        <div className="px-4 py-2 border-b border-slate-800">
                          <p className="font-bold text-white truncate">{user.firstName} {user.lastName}</p>
                          <p className="text-slate-400 text-[11px] truncate">{user.email}</p>
                        </div>
                        <Link
                          to="/profile"
                          onClick={() => setUserMenuOpen(false)}
                          className="flex items-center gap-2 px-4 py-2 text-slate-300 hover:text-white hover:bg-slate-800 transition-colors"
                        >
                          <UserCheck className="w-4 h-4 text-indigo-400" />
                          <span>My Profile</span>
                        </Link>

                        {user?.role === 'ADMIN' && (
                          <Link
                            to="/dashboard/admin"
                            onClick={() => setUserMenuOpen(false)}
                            className="flex items-center gap-2 px-4 py-2 text-rose-400 hover:text-rose-300 hover:bg-slate-800 transition-colors font-semibold"
                          >
                            <ShieldCheck className="w-4 h-4" />
                            <span>Admin Portal</span>
                          </Link>
                        )}

                        {user?.role === 'WAREHOUSE_STAFF' && (
                          <Link
                            to="/dashboard/warehouse"
                            onClick={() => setUserMenuOpen(false)}
                            className="flex items-center gap-2 px-4 py-2 text-amber-400 hover:text-amber-300 hover:bg-slate-800 transition-colors font-semibold"
                          >
                            <Warehouse className="w-4 h-4" />
                            <span>Warehouse Portal</span>
                          </Link>
                        )}

                        {user?.role === 'VENDOR' && (
                          <Link
                            to="/dashboard/vendor"
                            onClick={() => setUserMenuOpen(false)}
                            className="flex items-center gap-2 px-4 py-2 text-violet-400 hover:text-violet-300 hover:bg-slate-800 transition-colors font-semibold"
                          >
                            <User className="w-4 h-4" />
                            <span>Vendor Portal</span>
                          </Link>
                        )}

                        <Link
                          to="/dashboard/customer"
                          onClick={() => setUserMenuOpen(false)}
                          className="flex items-center gap-2 px-4 py-2 text-indigo-400 hover:text-indigo-300 hover:bg-slate-800 transition-colors"
                        >
                          <User className="w-4 h-4" />
                          <span>My Dashboard</span>
                        </Link>
                        <button
                          onClick={handleLogout}
                          className="w-full flex items-center gap-2 px-4 py-2 text-rose-400 hover:bg-rose-500/10 transition-colors text-left font-semibold"
                        >
                          <LogOut className="w-4 h-4" />
                          <span>Sign Out</span>
                        </button>
                      </div>
                    )}
                  </div>
                ) : (
                  <>
                    <Button
                      variant="ghost"
                      size="sm"
                      onClick={() => setLoginModalOpen(true)}
                      icon={User}
                    >
                      Sign In
                    </Button>
                    <Button
                      variant="primary"
                      size="sm"
                      onClick={() => setRegisterModalOpen(true)}
                    >
                      Register
                    </Button>
                  </>
                )}
              </div>

              {/* Mobile Hamburger Toggle */}
              <button
                onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
                className="md:hidden p-2 rounded-xl text-slate-300 hover:text-white hover:bg-slate-800"
                aria-label="Toggle Navigation Menu"
              >
                {mobileMenuOpen ? <X className="w-6 h-6" /> : <Menu className="w-6 h-6" />}
              </button>
            </div>
          </div>

          {/* Mobile Navigation Drawer */}
          {mobileMenuOpen && (
            <div className="md:hidden border-t border-slate-800 py-4 space-y-4 animate-in slide-in-from-top duration-200">
              <form onSubmit={handleSearchSubmit} className="relative flex items-center">
                <input
                  type="text"
                  value={searchQuery}
                  onChange={(e) => setSearchQuery(e.target.value)}
                  placeholder="Search products..."
                  className="w-full bg-slate-900 text-sm text-white rounded-xl border border-slate-800 pl-10 pr-4 py-2.5 placeholder-slate-500"
                />
                <Search className="w-4 h-4 text-slate-400 absolute left-3.5" />
              </form>

              <nav className="flex flex-col space-y-2">
                <Link
                  to="/"
                  onClick={() => setMobileMenuOpen(false)}
                  className="flex items-center justify-between px-3 py-2 rounded-lg text-slate-200 hover:bg-slate-800 text-sm font-medium"
                >
                  <span>Home</span>
                  <ChevronRight className="w-4 h-4 text-slate-500" />
                </Link>
                <Link
                  to="/products"
                  onClick={() => setMobileMenuOpen(false)}
                  className="flex items-center justify-between px-3 py-2 rounded-lg text-slate-200 hover:bg-slate-800 text-sm font-medium"
                >
                  <span>Explore Products</span>
                  <ChevronRight className="w-4 h-4 text-slate-500" />
                </Link>
                <Link
                  to="/cart"
                  onClick={() => setMobileMenuOpen(false)}
                  className="flex items-center justify-between px-3 py-2 rounded-lg text-slate-200 hover:bg-slate-800 text-sm font-medium"
                >
                  <span>Cart ({cartCount})</span>
                  <ChevronRight className="w-4 h-4 text-slate-500" />
                </Link>
                <Link
                  to="/wishlist"
                  onClick={() => setMobileMenuOpen(false)}
                  className="flex items-center justify-between px-3 py-2 rounded-lg text-slate-200 hover:bg-slate-800 text-sm font-medium"
                >
                  <span>Wishlist ({wishlistCount})</span>
                  <ChevronRight className="w-4 h-4 text-slate-500" />
                </Link>
                {isAuthenticated && (
                  <Link
                    to="/profile"
                    onClick={() => setMobileMenuOpen(false)}
                    className="flex items-center justify-between px-3 py-2 rounded-lg text-slate-200 hover:bg-slate-800 text-sm font-medium"
                  >
                    <span>My Profile</span>
                    <ChevronRight className="w-4 h-4 text-slate-500" />
                  </Link>
                )}
                {user?.role === 'WAREHOUSE_STAFF' && (
                  <Link
                    to="/dashboard/warehouse"
                    onClick={() => setMobileMenuOpen(false)}
                    className="flex items-center justify-between px-3 py-2 rounded-lg text-amber-400 hover:bg-slate-800 text-sm font-semibold"
                  >
                    <span className="flex items-center gap-2">
                      <Warehouse className="w-4 h-4" />
                      Warehouse Portal
                    </span>
                    <ChevronRight className="w-4 h-4 text-slate-500" />
                  </Link>
                )}
                {user?.role === 'ADMIN' && (
                  <Link
                    to="/dashboard/admin"
                    onClick={() => setMobileMenuOpen(false)}
                    className="flex items-center justify-between px-3 py-2 rounded-lg text-rose-400 hover:bg-slate-800 text-sm font-semibold"
                  >
                    <span className="flex items-center gap-2">
                      <ShieldCheck className="w-4 h-4" />
                      Admin Portal
                    </span>
                    <ChevronRight className="w-4 h-4 text-slate-500" />
                  </Link>
                )}
              </nav>

              <div className="pt-3 border-t border-slate-800 flex gap-2">
                {isAuthenticated ? (
                  <Button
                    variant="outline"
                    size="sm"
                    className="w-full hover:bg-rose-500/10 hover:text-rose-400"
                    onClick={() => {
                      setMobileMenuOpen(false);
                      handleLogout();
                    }}
                  >
                    Sign Out
                  </Button>
                ) : (
                  <>
                    <Button
                      variant="outline"
                      size="sm"
                      className="w-full"
                      onClick={() => {
                        setMobileMenuOpen(false);
                        setLoginModalOpen(true);
                      }}
                    >
                      Sign In
                    </Button>
                    <Button
                      variant="primary"
                      size="sm"
                      className="w-full"
                      onClick={() => {
                        setMobileMenuOpen(false);
                        setRegisterModalOpen(true);
                      }}
                    >
                      Register
                    </Button>
                  </>
                )}
              </div>
            </div>
          )}
        </div>
      </header>

      {/* Auth Modals */}
      <LoginModal
        isOpen={loginModalOpen}
        onClose={() => setLoginModalOpen(false)}
        onSwitchToRegister={() => setRegisterModalOpen(true)}
      />

      <RegisterModal
        isOpen={registerModalOpen}
        onClose={() => setRegisterModalOpen(false)}
        onSwitchToLogin={() => setLoginModalOpen(true)}
      />
    </>
  );
};

export default Header;
