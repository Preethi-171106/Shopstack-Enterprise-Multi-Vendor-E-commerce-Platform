import React from 'react';
import { Link } from 'react-router-dom';
import { ArrowRight } from 'lucide-react';
import Badge from './Badge';

const CategoryCard = ({ category }) => {
  return (
    <Link
      to={`/products?category=${category.id}`}
      className="group relative rounded-2xl overflow-hidden border border-slate-800 bg-slate-950/70 hover:border-indigo-500/50 transition-all duration-300 hover:shadow-xl hover:shadow-indigo-500/10 flex flex-col h-56"
    >
      {/* Category Background Image with Dark Overlay */}
      <img
        src={category.image}
        alt={category.name}
        className="absolute inset-0 w-full h-full object-cover group-hover:scale-105 transition-transform duration-500"
      />
      <div className="absolute inset-0 bg-gradient-to-t from-slate-950 via-slate-950/70 to-slate-950/30" />

      {/* Content Container */}
      <div className="relative z-10 p-5 flex flex-col justify-between h-full">
        <div className="flex items-center justify-between">
          <Badge color="slate" size="sm">
            {category.productCount} Items
          </Badge>
          <div className="w-8 h-8 rounded-full bg-slate-900/80 border border-slate-700/80 flex items-center justify-center text-slate-300 group-hover:text-indigo-400 group-hover:border-indigo-500/50 group-hover:bg-indigo-500/10 transition-all">
            <ArrowRight className="w-4 h-4 transform group-hover:translate-x-0.5 transition-transform" />
          </div>
        </div>

        <div>
          <h3 className="text-lg font-bold text-white group-hover:text-indigo-300 transition-colors">
            {category.name}
          </h3>
          <p className="text-xs text-slate-300 line-clamp-2 mt-1 leading-relaxed">
            {category.description}
          </p>
        </div>
      </div>
    </Link>
  );
};

export default CategoryCard;
