import React from 'react';

const colorVariants = {
  indigo: 'bg-indigo-500/10 text-indigo-400 border-indigo-500/20',
  emerald: 'bg-emerald-500/10 text-emerald-400 border-emerald-500/20',
  amber: 'bg-amber-500/10 text-amber-400 border-amber-500/20',
  rose: 'bg-rose-500/10 text-rose-400 border-rose-500/20',
  slate: 'bg-slate-800 text-slate-300 border-slate-700',
  violet: 'bg-violet-500/10 text-violet-400 border-violet-500/20',
  blue: 'bg-blue-500/10 text-blue-400 border-blue-500/20',
  teal: 'bg-teal-500/10 text-teal-400 border-teal-500/20',
  sky: 'bg-sky-500/10 text-sky-400 border-sky-500/20',
};

const sizes = {
  sm: 'px-2 py-0.5 text-[10px]',
  md: 'px-2.5 py-1 text-xs',
  lg: 'px-3 py-1.5 text-sm',
};

const Badge = ({
  children,
  color = 'indigo',
  size = 'md',
  icon: Icon = null,
  className = '',
}) => {
  const colorStyle = colorVariants[color] || colorVariants.indigo;
  const sizeStyle = sizes[size] || sizes.md;

  return (
    <span
      className={`inline-flex items-center gap-1 font-semibold uppercase tracking-wider rounded-full border shadow-sm ${colorStyle} ${sizeStyle} ${className}`}
    >
      {Icon && <Icon className="w-3 h-3 shrink-0" />}
      <span>{children}</span>
    </span>
  );
};

export default Badge;
