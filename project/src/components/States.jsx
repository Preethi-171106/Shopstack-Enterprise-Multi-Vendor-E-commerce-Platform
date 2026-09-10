import { AlertCircle, Inbox } from 'lucide-react';

export function ErrorState({ message = 'Something went wrong', onRetry }) {
  return (
    <div className="flex flex-col items-center justify-center gap-3 rounded-xl bg-rose-50 px-6 py-12 text-center ring-1 ring-rose-100">
      <AlertCircle className="text-rose-500" size={28} />
      <p className="text-sm font-medium text-rose-700">{message}</p>
      {onRetry && (
        <button onClick={onRetry} className="btn-secondary mt-1">
          Try again
        </button>
      )}
    </div>
  );
}

export function EmptyState({ title = 'Nothing here yet', description, icon: Icon = Inbox, action }) {
  return (
    <div className="flex flex-col items-center justify-center gap-3 rounded-xl bg-white px-6 py-16 text-center ring-1 ring-slate-100">
      <div className="rounded-full bg-slate-100 p-3 text-slate-400">
        <Icon size={26} />
      </div>
      <p className="text-base font-semibold text-slate-800">{title}</p>
      {description && <p className="max-w-sm text-sm text-slate-500">{description}</p>}
      {action}
    </div>
  );
}
