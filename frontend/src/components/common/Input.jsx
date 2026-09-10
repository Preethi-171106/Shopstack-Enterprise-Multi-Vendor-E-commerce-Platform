import React from 'react';
import { X } from 'lucide-react';

const Input = ({
  label,
  error,
  icon: Icon = null,
  rightIcon: RightIcon = null,
  onClear = null,
  value,
  onChange,
  placeholder,
  type = 'text',
  className = '',
  id,
  required = false,
  disabled = false,
  ...props
}) => {
  const inputId = id || (label ? label.toLowerCase().replace(/\s+/g, '-') : undefined);

  return (
    <div className="w-full space-y-1.5">
      {label && (
        <label htmlFor={inputId} className="block text-xs font-semibold uppercase tracking-wider text-slate-300">
          {label} {required && <span className="text-rose-400">*</span>}
        </label>
      )}

      <div className="relative flex items-center">
        {Icon && (
          <div className="absolute left-3.5 text-slate-400 pointer-events-none flex items-center justify-center">
            <Icon className="w-4 h-4" />
          </div>
        )}

        <input
          id={inputId}
          type={type}
          value={value}
          onChange={onChange}
          placeholder={placeholder}
          disabled={disabled}
          className={`w-full bg-slate-900/90 text-white text-sm rounded-xl border border-slate-700/80 px-3.5 py-2.5 placeholder-slate-500 focus:outline-none focus:border-indigo-500 focus:ring-1 focus:ring-indigo-500 transition-all duration-200 disabled:opacity-50 disabled:cursor-not-allowed ${
            Icon ? 'pl-10' : ''
          } ${onClear && value ? 'pr-10' : RightIcon ? 'pr-10' : ''} ${
            error ? 'border-rose-500 focus:border-rose-500 focus:ring-rose-500' : ''
          } ${className}`}
          {...props}
        />

        {onClear && value ? (
          <button
            type="button"
            onClick={onClear}
            className="absolute right-3 text-slate-400 hover:text-white p-1 rounded-full hover:bg-slate-800 transition-colors"
          >
            <X className="w-4 h-4" />
          </button>
        ) : RightIcon ? (
          <div className="absolute right-3.5 text-slate-400 pointer-events-none flex items-center justify-center">
            <RightIcon className="w-4 h-4" />
          </div>
        ) : null}
      </div>

      {error && <p className="text-xs text-rose-400 font-medium mt-1">{error}</p>}
    </div>
  );
};

export default Input;
