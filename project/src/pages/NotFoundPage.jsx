import { Link } from 'react-router-dom';
import { Compass } from 'lucide-react';

export function NotFoundPage() {
  return (
    <div className="flex min-h-[70vh] flex-col items-center justify-center gap-4 text-center">
      <div className="rounded-full bg-brand-50 p-4 text-brand-600">
        <Compass size={32} />
      </div>
      <h1 className="text-3xl font-bold text-slate-900">Page not found</h1>
      <p className="max-w-sm text-slate-500">
        The page you are looking for doesn&apos;t exist or has moved.
      </p>
      <Link to="/" className="btn-primary">Back to home</Link>
    </div>
  );
}
