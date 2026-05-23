import { cn } from '../../lib/cn';
import type { ReactNode } from 'react';

interface SectionProps {
  children: ReactNode;
  /** Vertical padding size */
  spacing?: 'sm' | 'md' | 'lg';
  /** Background variant */
  background?: 'default' | 'elevated' | 'gradient';
  /** Additional class names */
  className?: string;
  /** HTML id for anchor links */
  id?: string;
}

const spacingClasses = {
  sm: 'py-12 md:py-16',
  md: 'py-16 md:py-20',
  lg: 'py-20 md:py-28',
};

const backgroundClasses = {
  default: 'bg-background',
  elevated: 'bg-background-elevated',
  gradient: 'bg-gradient-to-b from-transparent via-surface/50 to-transparent',
};

/**
 * Section component for consistent vertical spacing and backgrounds
 */
export function Section({
  children,
  spacing = 'md',
  background = 'default',
  className,
  id,
}: SectionProps) {
  return (
    <section
      id={id}
      className={cn(
        spacingClasses[spacing],
        backgroundClasses[background],
        'relative',
        className
      )}
    >
      {children}
    </section>
  );
}
