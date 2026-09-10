import React from 'react';
import { Link } from 'react-router-dom';
import { Star, ShieldCheck, MapPin, Package } from 'lucide-react';
import Badge from './Badge';

const VendorCard = ({ vendor }) => {
  return (
    <div className="p-5 rounded-2xl bg-slate-950/70 border border-slate-800/80 hover:border-indigo-500/40 transition-all duration-300 hover:shadow-lg hover:shadow-indigo-500/5 group flex flex-col justify-between">
      <div className="space-y-4">
        {/* Header avatar & Info */}
        <div className="flex items-start gap-3.5">
          <img
            src={vendor.logo}
            alt={vendor.name}
            className="w-12 h-12 rounded-xl object-cover border border-slate-700/60 shadow-md group-hover:scale-105 transition-transform"
          />
          <div className="flex-1 min-w-0">
            <div className="flex items-center gap-1.5">
              <h4 className="font-bold text-sm text-white truncate group-hover:text-indigo-300 transition-colors">
                {vendor.name}
              </h4>
              {vendor.verified && (
                <ShieldCheck className="w-4 h-4 text-indigo-400 shrink-0" title="Verified Vendor" />
              )}
            </div>

            <div className="flex items-center gap-2 text-xs text-slate-400 mt-0.5">
              <span className="flex items-center gap-1 text-amber-400 font-semibold">
                <Star className="w-3.5 h-3.5 fill-amber-400" />
                {vendor.rating}
              </span>
              <span>&bull;</span>
              <span className="truncate">{vendor.category}</span>
            </div>
          </div>
        </div>

        {/* Description */}
        <p className="text-xs text-slate-400 line-clamp-2 leading-relaxed">
          {vendor.description}
        </p>
      </div>

      {/* Footer Info */}
      <div className="pt-3 mt-4 border-t border-slate-800/80 flex items-center justify-between text-xs text-slate-400">
        <div className="flex items-center gap-1">
          <MapPin className="w-3.5 h-3.5 text-slate-500" />
          <span>{vendor.location}</span>
        </div>

        <div className="flex items-center gap-1 font-medium text-slate-300">
          <Package className="w-3.5 h-3.5 text-indigo-400" />
          <span>{vendor.totalProducts} Products</span>
        </div>
      </div>
    </div>
  );
};

export default VendorCard;
