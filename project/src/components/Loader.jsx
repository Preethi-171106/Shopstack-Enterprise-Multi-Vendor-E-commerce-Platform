import { Loader2 } from 'lucide-react';

export function Spinner({ className = '' }) {
  return <Loader2 className={`animate-spin ${className}`} size={18} />;
}

export function PageLoader({ label = 'Loading…' }) {
  return (
    <div className="flex min-h-[50vh] flex-col items-center justify-center gap-3 text-slate-400">
      <Spinner className="text-brand-500" size={28} />
      <p className="text-sm font-medium">{label}</p>
    </div>
  );
}

export function InlineLoader({ label = '' }) {
  return (
    <div className="flex items-center justify-center gap-2 py-6 text-slate-400">
      <Spinner className="text-brand-500" />
      {label && <span className="text-sm">{label}</span>}
    </div>
  );
}

export function ButtonLoader() {
  return <Spinner className="text-white" size={16} />;
}
