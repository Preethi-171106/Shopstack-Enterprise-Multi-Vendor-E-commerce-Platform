import { Link, NavLink, useNavigate } from 'react-router-dom';
import { ShoppingCart, Heart, Bell, Search, Store, Menu, X, LogOut, User, LayoutDashboard } from 'lucide-react';
import { useState } from 'react';
import { useAuth } from '../context/AuthContext';
import { useCart } from '../context/CartContext';
import { useWishlist } from '../context/WishlistContext';
import { useToast } from '../context/ToastContext';

export function Navbar() {
  const { isAuthenticated, profile, role, logout } = useAuth();
  const { itemCount } = useCart();
  const { count: wishCount } = useWishlist();
  const toast = useToast();
  const navigate = useNavigate();
  const [open, setOpen] = useState(false);
  const [query, setQuery] = useState('');

  const onLogout = async () => {
    await logout();
    toast.info('Signed out');
    navigate('/');
  };

  const onSearch = (e) => {
    e.preventDefault();
    if (query.trim()) navigate(`/products?search=${encodeURIComponent(query.trim())}`);
  };

  const dashLink =
    role === 'ADMIN' ? '/admin' : role === 'VENDOR' ? '/vendor' : null;

  return (
    <header className="sticky top-0 z-40 border-b border-slate-200 bg-white/90 backdrop-blur">
      <div className="mx-auto flex h-16 max-w-7xl items-center gap-4 px-4 sm:px-6">
        <Link to="/" className="flex items-center gap-2 text-lg font-bold text-brand-700">
          <span className="grid h-8 w-8 place-items-center rounded-lg bg-brand-600 text-white">
            <Store size={18} />
          </span>
          <span className="hidden sm:inline">ShopStack</span>
        </Link>

        <form onSubmit={onSearch} className="relative hidden flex-1 md:block">
          <Search className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" size={18} />
          <input
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            placeholder="Search products…"
            className="input !py-2 pl-10"
          />
        </form>

        <nav className="ml-auto flex items-center gap-1">
          <NavLink to="/categories" className="nav-link hidden lg:flex">
            Categories
          </NavLink>

          <NavLink to="/wishlist" className="nav-link relative" aria-label="Wishlist">
            <Heart size={20} />
            {isAuthenticated && wishCount > 0 && (
              <span className="absolute -right-0.5 -top-0.5 grid h-4 min-w-4 place-items-center rounded-full bg-rose-500 px-1 text-[10px] font-bold text-white">
                {wishCount}
              </span>
            )}
          </NavLink>

          <NavLink to="/notifications" className="nav-link relative" aria-label="Notifications">
            <Bell size={20} />
          </NavLink>

          <NavLink to="/cart" className="nav-link relative" aria-label="Cart">
            <ShoppingCart size={20} />
            {isAuthenticated && itemCount > 0 && (
              <span className="absolute -right-0.5 -top-0.5 grid h-4 min-w-4 place-items-center rounded-full bg-brand-600 px-1 text-[10px] font-bold text-white">
                {itemCount}
              </span>
            )}
          </NavLink>

          {isAuthenticated ? (
            <>
              {dashLink && (
                <NavLink to={dashLink} className="nav-link hidden sm:flex">
                  <LayoutDashboard size={18} /> Dashboard
                </NavLink>
              )}
              <NavLink to="/profile" className="nav-link" aria-label="Profile">
                <User size={20} />
              </NavLink>
              <button onClick={onLogout} className="nav-link" aria-label="Sign out">
                <LogOut size={20} />
              </button>
            </>
          ) : (
            <>
              <Link to="/login" className="btn-secondary !py-2">Sign in</Link>
              <Link to="/register" className="btn-primary !py-2 hidden sm:inline-flex">Sign up</Link>
            </>
          )}

          <button className="nav-link md:hidden" onClick={() => setOpen((o) => !o)} aria-label="Menu">
            {open ? <X size={20} /> : <Menu size={20} />}
          </button>
        </nav>
      </div>

      {open && (
        <div className="border-t border-slate-200 bg-white px-4 py-3 md:hidden">
          <form onSubmit={onSearch} className="relative mb-3">
            <Search className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" size={18} />
            <input
              value={query}
              onChange={(e) => setQuery(e.target.value)}
              placeholder="Search products…"
              className="input !py-2 pl-10"
            />
          </form>
          <div className="flex flex-col gap-1">
            <NavLink to="/categories" className="nav-link" onClick={() => setOpen(false)}>Categories</NavLink>
            <NavLink to="/products" className="nav-link" onClick={() => setOpen(false)}>All products</NavLink>
            {isAuthenticated && (
              <>
                <NavLink to="/orders" className="nav-link" onClick={() => setOpen(false)}>My orders</NavLink>
                <NavLink to="/profile" className="nav-link" onClick={() => setOpen(false)}>Profile</NavLink>
              </>
            )}
          </div>
        </div>
      )}
    </header>
  );
}
