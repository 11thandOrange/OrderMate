import { cn } from '../../lib/cn';
import type { ReactNode } from 'react';

interface SectionProps {
  children: ReactNode;
  spacing?: 'sm' | 'md' | 'lg';
  background?: 'default' | 'elevated' | 'gradient';
  className?: string;
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
