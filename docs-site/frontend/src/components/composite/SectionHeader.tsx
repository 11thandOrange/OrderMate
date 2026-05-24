import { cn } from '../../lib/cn';

interface SectionHeaderProps {
  label?: string;
  title: string;
  description?: string;
  align?: 'left' | 'center';
  className?: string;
}

export function SectionHeader({
  label,
  title,
  description,
  align = 'center',
  className,
}: SectionHeaderProps) {
  return (
    <div
      className={cn(
        'mb-20',
        align === 'center' && 'text-center max-w-[700px] mx-auto',
        className
      )}
    >
      {label && (
        <span
          className="inline-block px-5 py-2 rounded-full text-[13px] font-semibold uppercase tracking-widest mb-5"
          style={{
            background: 'rgba(255,255,255,0.1)',
            border: '1px solid rgba(255,255,255,0.2)',
            color: '#FF9F43',
          }}
        >
          {label}
        </span>
      )}
      <h2 className="text-[44px] font-bold text-content mb-5 tracking-tight">
        {title}
      </h2>
      {description && (
        <p className={cn('text-lg text-content-secondary leading-relaxed', align === 'center' && 'max-w-2xl mx-auto')}>
          {description}
        </p>
      )}
    </div>
  );
}
