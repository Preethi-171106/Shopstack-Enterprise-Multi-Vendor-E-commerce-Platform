import React from 'react';
import { Loader2 } from 'lucide-react';

const Loading = ({ message = 'Loading catalog data...', fullPage = false }) => {
  if (fullPage) {
    return (
      <div className="min-h-[60vh] flex flex-col items-center justify-center space-y-4 px-4">
        <div className="relative flex items-center justify-center">
          <div className="w-12 h-12 rounded-full border-2 border-indigo-500/20 border-t-indigo-500 animate-spin" />
        </div>
        <p className="text-sm font-medium text-slate-400">{message}</p>
      </div>
    );
  }

  return (
    <div className="py-12 flex flex-col items-center justify-center space-y-3">
      <Loader2 className="w-8 h-8 text-indigo-400 animate-spin" />
      <p className="text-xs text-slate-400">{message}</p>
    </div>
  );
};

export default Loading;
