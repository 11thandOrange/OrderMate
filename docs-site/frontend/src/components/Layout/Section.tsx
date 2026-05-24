import { cn } from '../../lib/cn';
import type { ReactNode } from 'react';

interface SectionProps {
  children: ReactNode;
  spacing?: 'sm' | 'md' | 'lg';
  background?: 'default' | 'elevated' | 'gradient' | 'dark-navy';
  className?: string;
  id?: string;
}

const spacingClasses = {
  sm: 'py-12 md:py-16',
  md: 'py-16 md:py-20',
  lg: 'py-[120px]',
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
      className={cn(spacingClasses[spacing], 'relative', className)}
      style={
        background === 'dark-navy'
          ? { background: 'linear-gradient(135deg, #1a1a2e 0%, #16213e 50%, #1a1a2e 100%)' }
          : background === 'elevated'
          ? { background: 'rgba(0,0,0,0.15)' }
          : undefined
      }
    >
      {children}
    </section>
  );
}
