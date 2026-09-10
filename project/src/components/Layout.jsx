import { NavLink, Outlet, Link } from 'react-router-dom';
import { Navbar } from './Navbar';
import { useAuth } from '../context/AuthContext';

const vendorNav = [
  { to: '/vendor', label: 'Dashboard', icon: 'LayoutDashboard', end: true },
  { to: '/vendor/products', label: 'Products', icon: 'Package' },
  { to: '/vendor/inventory', label: 'Inventory', icon: 'Boxes' },
  { to: '/vendor/orders', label: 'Orders', icon: 'ShoppingBag' },
];

const adminNav = [
  { to: '/admin', label: 'Dashboard', icon: 'LayoutDashboard', end: true },
  { to: '/admin/users', label: 'Users', icon: 'Users' },
  { to: '/admin/vendors', label: 'Vendors', icon: 'Store' },
  { to: '/admin/products', label: 'Products', icon: 'Package' },
  { to: '/admin/coupons', label: 'Coupons', icon: 'Ticket' },
  { to: '/admin/reports', label: 'Reports', icon: 'FileBarChart' },
  { to: '/admin/analytics', label: 'Analytics', icon: 'TrendingUp' },
];

import * as Icons from 'lucide-react';

export function DashboardLayout({ nav, title }) {
  const { profile } = useAuth();
  return (
    <div className="min-h-screen bg-slate-50">
      <Navbar />
      <div className="mx-auto flex max-w-7xl gap-6 px-4 py-6 sm:px-6">
        <aside className="hidden w-60 shrink-0 lg:block">
          <div className="sticky top-22 card p-3">
            <p className="px-3 pb-2 text-xs font-semibold uppercase tracking-wide text-slate-400">
              {title}
            </p>
            <nav className="flex flex-col gap-1">
              {nav.map((item) => {
                const Icon = Icons[item.icon] || Icons.Circle;
                return (
                  <NavLink
                    key={item.to}
                    to={item.to}
                    end={item.end}
                    className={({ isActive }) =>
                      `nav-link ${isActive ? 'nav-link-active' : ''}`
                    }
                  >
                    <Icon size={18} />
                    {item.label}
                  </NavLink>
                );
              })}
            </nav>
          </div>
        </aside>

        <main className="min-w-0 flex-1">
          <div className="mb-4 flex items-center justify-between">
            <h1 className="text-xl font-bold text-slate-900">{title}</h1>
            <span className="text-sm text-slate-500">{profile?.full_name}</span>
          </div>

          {/* mobile nav */}
          <div className="mb-4 flex gap-2 overflow-x-auto lg:hidden">
            {nav.map((item) => (
              <NavLink
                key={item.to}
                to={item.to}
                end={item.end}
                className={({ isActive }) =>
                  `whitespace-nowrap rounded-lg px-3 py-1.5 text-sm font-medium ${
                    isActive ? 'bg-brand-50 text-brand-700' : 'bg-white text-slate-600 ring-1 ring-slate-200'
                  }`
                }
              >
                {item.label}
              </NavLink>
            ))}
          </div>

          <Outlet />
        </main>
      </div>
    </div>
  );
}

export function VendorLayout() {
  return <DashboardLayout nav={vendorNav} title="Vendor Console" />;
}

export function AdminLayout() {
  return <DashboardLayout nav={adminNav} title="Admin Console" />;
}

export function StorefrontLayout() {
  return (
    <div className="flex min-h-screen flex-col bg-slate-50">
      <Navbar />
      <main className="mx-auto w-full max-w-7xl flex-1 px-4 py-6 sm:px-6">
        <Outlet />
      </main>
      <footer className="border-t border-slate-200 bg-white">
        <div className="mx-auto flex max-w-7xl flex-col items-center justify-between gap-3 px-4 py-6 text-sm text-slate-500 sm:flex-row sm:px-6">
          <Link to="/" className="font-semibold text-brand-700">ShopStack</Link>
          <p>Enterprise Multi-Vendor Marketplace</p>
          <p>© {new Date().getFullYear()} ShopStack</p>
        </div>
      </footer>
    </div>
  );
}
