import { cn } from '../../lib/cn';
import type { ReactNode } from 'react';

interface GridProps {
  children: ReactNode;
  /** Number of columns */
  cols?: 1 | 2 | 3 | 4;
  /** Gap between items */
  gap?: 'sm' | 'md' | 'lg';
  /** Additional class names */
  className?: string;
}

const colClasses = {
  1: 'grid-cols-1',
  2: 'grid-cols-1 sm:grid-cols-2',
  3: 'grid-cols-1 sm:grid-cols-2 lg:grid-cols-3',
  4: 'grid-cols-1 sm:grid-cols-2 lg:grid-cols-4',
};

const gapClasses = {
  sm: 'gap-4',
  md: 'gap-6',
  lg: 'gap-8',
};

/**
 * Grid component for responsive layouts
 */
export function Grid({
  children,
  cols = 3,
  gap = 'md',
  className,
}: GridProps) {
  return (
    <div
      className={cn(
        'grid',
        colClasses[cols],
        gapClasses[gap],
        className
      )}
    >
      {children}
    </div>
  );
}
