import { cn } from '../../lib/cn';
import type { HttpMethod } from '../../types/api';
import type { ReactNode } from 'react';

type BadgeVariant = 'default' | 'success' | 'warning' | 'error' | 'info' | 'brand';
type BadgeSize = 'sm' | 'md';

interface BadgeProps {
  children: ReactNode;
  variant?: BadgeVariant;
  size?: BadgeSize;
  icon?: ReactNode;
  className?: string;
}

const variantStyles: Record<BadgeVariant, string> = {
  default: 'bg-content-muted/20 text-content-secondary border-content-muted/30',
  success: 'bg-status-success/15 text-status-success border-status-success/30',
  warning: 'bg-status-warning/15 text-status-warning border-status-warning/30',
  error: 'bg-status-error/15 text-status-error border-status-error/30',
  info: 'bg-status-info/15 text-status-info border-status-info/30',
  brand: 'bg-brand-muted text-brand border-brand/30',
};

const sizeStyles: Record<BadgeSize, string> = {
  sm: 'px-2 py-0.5 text-xs',
  md: 'px-2.5 py-1 text-sm',
};

/**
 * Badge component for labels and status indicators
 */
export function Badge({ children, variant = 'default', size = 'sm', icon, className }: BadgeProps) {
  return (
    <span
      className={cn(
        'inline-flex items-center gap-1.5 font-medium rounded-badge border',
        variantStyles[variant],
        sizeStyles[size],
        className
      )}
    >
      {icon}
      {children}
    </span>
  );
}

/**
 * HTTP Method Badge for API documentation
 */
interface MethodBadgeProps {
  method: HttpMethod;
  className?: string;
}

const methodStyles: Record<HttpMethod, string> = {
  GET: 'bg-status-success/15 text-status-success border-status-success/30',
  POST: 'bg-status-info/15 text-status-info border-status-info/30',
  PUT: 'bg-status-warning/15 text-status-warning border-status-warning/30',
  DELETE: 'bg-status-error/15 text-status-error border-status-error/30',
  PATCH: 'bg-accent-purple/15 text-accent-purple border-accent-purple/30',
};

export function MethodBadge({ method, className }: MethodBadgeProps) {
  return (
    <span
      className={cn(
        'inline-flex items-center px-2 py-0.5 text-xs font-mono font-semibold rounded border',
        methodStyles[method],
        className
      )}
    >
      {method}
    </span>
  );
}

/**
 * Status indicator dot
 */
interface StatusDotProps {
  status?: 'online' | 'offline' | 'warning' | 'error';
  className?: string;
}

const statusDotStyles = {
  online: 'bg-status-success',
  offline: 'bg-content-muted',
  warning: 'bg-status-warning',
  error: 'bg-status-error',
};

export function StatusDot({ status = 'online', className }: StatusDotProps) {
  return (
    <span className={cn('inline-flex items-center gap-2', className)}>
      <span className={cn('w-2 h-2 rounded-full', statusDotStyles[status])} />
    </span>
  );
}
