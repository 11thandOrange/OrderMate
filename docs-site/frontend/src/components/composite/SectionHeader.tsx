import { cn } from '../../lib/cn';

interface SectionHeaderProps {
  /** Section title */
  title: string;
  /** Section description */
  description?: string;
  /** Text alignment */
  align?: 'left' | 'center';
  /** Additional class names */
  className?: string;
}

/**
 * Section Header component for consistent section titles
 */
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
      <h2 className="text-2xl md:text-3xl font-bold text-content mb-4">
        {title}
      </h2>
      {description && (
        <p
          className={cn(
            'text-content-secondary',
            align === 'center' && 'max-w-2xl mx-auto'
          )}
        >
          {description}
        </p>
      )}
    </div>
  );
}
