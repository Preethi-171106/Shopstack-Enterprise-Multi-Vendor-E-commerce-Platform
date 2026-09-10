import { Star } from 'lucide-react';

export function Rating({ value = 0, count, size = 16, onChange, readOnly = true }) {
  const stars = [1, 2, 3, 4, 5];
  return (
    <div className="flex items-center gap-1">
      <div className="flex items-center">
        {stars.map((s) => (
          <button
            key={s}
            type="button"
            disabled={readOnly}
            onClick={() => onChange?.(s)}
            className={readOnly ? 'cursor-default' : 'cursor-pointer'}
            aria-label={`${s} star${s > 1 ? 's' : ''}`}
          >
            <Star
              size={size}
              className={
                s <= Math.round(value)
                  ? 'fill-amber-400 text-amber-400'
                  : 'fill-slate-200 text-slate-200'
              }
            />
          </button>
        ))}
      </div>
      {count != null && (
        <span className="text-xs font-medium text-slate-500">({count})</span>
      )}
    </div>
  );
}
