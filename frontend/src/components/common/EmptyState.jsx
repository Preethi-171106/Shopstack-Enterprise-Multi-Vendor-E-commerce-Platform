import React from 'react';
import { PackageSearch } from 'lucide-react';
import Button from './Button';

const EmptyState = ({
  title = 'No items found',
  description = 'Try adjusting your search criteria or clear active filters to discover products.',
  actionLabel = 'Reset Filters',
  onAction = null,
  icon: Icon = PackageSearch,
}) => {
  return (
    <div className="py-16 px-6 text-center max-w-md mx-auto rounded-3xl bg-slate-950/60 border border-slate-800/80 my-8">
      <div className="w-16 h-16 rounded-2xl bg-indigo-500/10 border border-indigo-500/20 flex items-center justify-center text-indigo-400 mx-auto mb-4">
        <Icon className="w-8 h-8" />
      </div>
      <h3 className="text-lg font-bold text-white mb-2">{title}</h3>
      <p className="text-sm text-slate-400 leading-relaxed mb-6">{description}</p>
      {onAction && (
        <Button variant="outline" size="sm" onClick={onAction}>
          {actionLabel}
        </Button>
      )}
    </div>
  );
};

export default EmptyState;
