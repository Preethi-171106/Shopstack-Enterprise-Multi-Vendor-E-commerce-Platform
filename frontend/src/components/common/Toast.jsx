import React from 'react';
import { CheckCircle2, AlertCircle, Info, XCircle, X } from 'lucide-react';
import { useApp } from '../../context/AppContext';

const toastTypes = {
  success: {
    icon: CheckCircle2,
    bg: 'bg-emerald-950/90 border-emerald-500/30 text-emerald-200',
    iconColor: 'text-emerald-400',
  },
  error: {
    icon: XCircle,
    bg: 'bg-rose-950/90 border-rose-500/30 text-rose-200',
    iconColor: 'text-rose-400',
  },
  warning: {
    icon: AlertCircle,
    bg: 'bg-amber-950/90 border-amber-500/30 text-amber-200',
    iconColor: 'text-amber-400',
  },
  info: {
    icon: Info,
    bg: 'bg-indigo-950/90 border-indigo-500/30 text-indigo-200',
    iconColor: 'text-indigo-400',
  },
};

const Toast = () => {
  const { toast, hideToast } = useApp();

  if (!toast) return null;

  const style = toastTypes[toast.type] || toastTypes.info;
  const IconComponent = style.icon;

  return (
    <div className="fixed bottom-6 right-6 z-50 animate-bounce-short max-w-md w-full px-4">
      <div
        className={`flex items-start gap-3 p-4 rounded-2xl border shadow-xl backdrop-blur-md transition-all duration-300 ${style.bg}`}
      >
        <IconComponent className={`w-5 h-5 shrink-0 mt-0.5 ${style.iconColor}`} />
        <div className="flex-1 min-w-0">
          {toast.title && (
            <h4 className="text-sm font-semibold mb-0.5 capitalize">{toast.title}</h4>
          )}
          <p className="text-xs leading-relaxed">{toast.message}</p>
        </div>
        <button
          onClick={hideToast}
          className="text-slate-400 hover:text-white p-1 rounded-lg hover:bg-slate-800/50 transition-colors"
        >
          <X className="w-4 h-4" />
        </button>
      </div>
    </div>
  );
};

export default Toast;
