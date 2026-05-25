import { cn } from '../../lib/cn';
import type { ReactNode } from 'react';

interface ContainerProps {
  children: ReactNode;
  /** Maximum width variant */
  size?: 'sm' | 'md' | 'lg' | 'xl';
  /** Center content horizontally */
  center?: boolean;
  /** Additional class names */
  className?: string;
  /** HTML element to render */
  as?: 'div' | 'section' | 'article' | 'main';
}

const sizeClasses = {
  sm: 'max-w-2xl',      // 672px
  md: 'max-w-4xl',      // 896px - content
  lg: 'max-w-6xl',      // 1152px - default
  xl: 'max-w-7xl',      // 1280px - wide
};

/**
 * Container component for consistent max-width and horizontal padding
 */
export function Container({
  children,
  size = 'lg',
  center = true,
  className,
  as: Component = 'div',
}: ContainerProps) {
  return (
    <Component
      className={cn(
        'w-full px-4 sm:px-6 lg:px-10',
        sizeClasses[size],
        center && 'mx-auto',
        className
      )}
    >
      {children}
    </Component>
  );
}
