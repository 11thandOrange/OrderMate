import { cn } from '../../lib/cn';

interface SectionHeaderProps {
  title: string;
  description?: string;
  align?: 'left' | 'center';
  className?: string;
}

export function SectionHeader({
  title,
  description,
  align = 'center',
  className,
}: SectionHeaderProps) {
  return (
    <div
      className={cn(
        'mb-12',
        align === 'center' && 'text-center',
        className
      )}
    >
      <h2 className="text-2xl md:text-3xl font-bold text-white mb-4">
        {title}
      </h2>
      {description && (
        <p className={cn('text-gray-400', align === 'center' && 'max-w-2xl mx-auto')}>
          {description}
        </p>
      )}
    </div>
  );
}
