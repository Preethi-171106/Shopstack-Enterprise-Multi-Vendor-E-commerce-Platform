import React from 'react';
import { Link } from 'react-router-dom';
import { ShoppingBag, ShieldCheck, Truck, Headphones, RefreshCw } from 'lucide-react';
import { APP_NAME, APP_TAGLINE } from '../utils/constants';

const Footer = () => {
  return (
    <footer className="bg-slate-950 border-t border-slate-800 text-slate-400 text-sm mt-auto">
      {/* Value Proposition Highlights Bar */}
      <div className="border-b border-slate-800/80 py-8 bg-slate-900/40">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6">
          <div className="flex items-center gap-3">
            <div className="p-2.5 rounded-xl bg-indigo-500/10 border border-indigo-500/20 text-indigo-400">
              <Truck className="w-5 h-5" />
            </div>
            <div>
              <h4 className="font-semibold text-white text-xs uppercase tracking-wider">Fast Worldwide Shipping</h4>
              <p className="text-xs text-slate-400">Tracked delivery on all vendor items</p>
            </div>
          </div>

          <div className="flex items-center gap-3">
            <div className="p-2.5 rounded-xl bg-indigo-500/10 border border-indigo-500/20 text-indigo-400">
              <ShieldCheck className="w-5 h-5" />
            </div>
            <div>
              <h4 className="font-semibold text-white text-xs uppercase tracking-wider">Verified Vendors</h4>
              <p className="text-xs text-slate-400">Strict merchant quality standards</p>
            </div>
          </div>

          <div className="flex items-center gap-3">
            <div className="p-2.5 rounded-xl bg-indigo-500/10 border border-indigo-500/20 text-indigo-400">
              <Headphones className="w-5 h-5" />
            </div>
            <div>
              <h4 className="font-semibold text-white text-xs uppercase tracking-wider">24/7 Buyer Support</h4>
              <p className="text-xs text-slate-400">Dedicated multi-vendor assistance</p>
            </div>
          </div>

          <div className="flex items-center gap-3">
            <div className="p-2.5 rounded-xl bg-indigo-500/10 border border-indigo-500/20 text-indigo-400">
              <RefreshCw className="w-5 h-5" />
            </div>
            <div>
              <h4 className="font-semibold text-white text-xs uppercase tracking-wider">30-Day Money Back</h4>
              <p className="text-xs text-slate-400">Hassle-free return policy</p>
            </div>
          </div>
        </div>
      </div>

      {/* Main Footer Links */}
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-12 grid grid-cols-1 md:grid-cols-2 lg:grid-cols-5 gap-8">
        
        {/* Brand & About Column */}
        <div className="lg:col-span-2 space-y-4">
          <div className="flex items-center gap-3">
            <div className="h-9 w-9 rounded-xl bg-indigo-600 flex items-center justify-center text-white font-bold shadow-md shadow-indigo-600/30">
              <ShoppingBag className="w-5 h-5" />
            </div>
            <span className="font-extrabold text-xl text-white tracking-tight">{APP_NAME}</span>
          </div>
          <p className="text-xs text-slate-400 leading-relaxed max-w-sm">
            ShopStack is an enterprise-grade multi-vendor e-commerce platform connecting discerning shoppers with verified independent sellers and premium global brands.
          </p>
          <div className="text-xs text-slate-500 pt-2">
            <span>Powered by React 18, Redux Toolkit &amp; Tailwind CSS</span>
          </div>
        </div>

        {/* Customer Service Links */}
        <div className="space-y-3">
          <h4 className="font-semibold text-white text-xs uppercase tracking-wider">Customer Care</h4>
          <ul className="space-y-2 text-xs">
            <li><a href="#help" className="hover:text-white transition-colors">Help Center &amp; FAQ</a></li>
            <li><a href="#shipping" className="hover:text-white transition-colors">Shipping &amp; Delivery</a></li>
            <li><a href="#returns" className="hover:text-white transition-colors">Returns &amp; Exchanges</a></li>
            <li><a href="#tracking" className="hover:text-white transition-colors">Order Tracking</a></li>
            <li><a href="#buyer-protection" className="hover:text-white transition-colors">Buyer Protection</a></li>
          </ul>
        </div>

        {/* Vendor & Marketplace Links */}
        <div className="space-y-3">
          <h4 className="font-semibold text-white text-xs uppercase tracking-wider">Sell on ShopStack</h4>
          <ul className="space-y-2 text-xs">
            <li><a href="#become-vendor" className="hover:text-white transition-colors">Become a Vendor</a></li>
            <li><a href="#vendor-hub" className="hover:text-white transition-colors">Merchant Portal</a></li>
            <li><a href="#selling-guide" className="hover:text-white transition-colors">Selling Guidelines</a></li>
            <li><a href="#commissions" className="hover:text-white transition-colors">Fee Structure</a></li>
            <li><a href="#fulfillment" className="hover:text-white transition-colors">Vendor Fulfillment</a></li>
          </ul>
        </div>

        {/* Quick Navigation Links */}
        <div className="space-y-3">
          <h4 className="font-semibold text-white text-xs uppercase tracking-wider">Explore Stack</h4>
          <ul className="space-y-2 text-xs">
            <li><Link to="/products" className="hover:text-white transition-colors">All Products</Link></li>
            <li><Link to="/products?category=cat-electronics" className="hover:text-white transition-colors">Electronics &amp; Tech</Link></li>
            <li><Link to="/products?category=cat-fashion" className="hover:text-white transition-colors">Fashion Apparel</Link></li>
            <li><Link to="/products?category=cat-home" className="hover:text-white transition-colors">Home &amp; Living</Link></li>
            <li><Link to="/products?category=cat-sports" className="hover:text-white transition-colors">Sports Gear</Link></li>
          </ul>
        </div>

      </div>

      {/* Copyright & Disclaimer Bar */}
      <div className="border-t border-slate-800/80 py-6 bg-slate-950">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 flex flex-col sm:flex-row items-center justify-between gap-4 text-xs text-slate-500">
          <div>
            &copy; {new Date().getFullYear()} {APP_NAME} Platform Inc. All rights reserved.
          </div>
          <div className="flex items-center gap-6">
            <a href="#privacy" className="hover:text-slate-400">Privacy Policy</a>
            <a href="#terms" className="hover:text-slate-400">Terms of Service</a>
            <a href="#cookies" className="hover:text-slate-400">Cookie Settings</a>
          </div>
        </div>
      </div>
    </footer>
  );
};

export default Footer;
