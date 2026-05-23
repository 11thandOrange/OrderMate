import { cn } from '../../lib/cn';
import type { LucideIcon } from 'lucide-react';

type IconSize = 'xs' | 'sm' | 'md' | 'lg' | 'xl';
type IconColor = 'default' | 'secondary' | 'muted' | 'brand' | 'success' | 'warning' | 'error' | 'inherit';

interface IconProps {
  icon: LucideIcon;
  size?: IconSize;
  color?: IconColor;
  className?: string;
}

const sizeStyles: Record<IconSize, string> = {
  xs: 'w-3 h-3',
  sm: 'w-4 h-4',
  md: 'w-5 h-5',
  lg: 'w-6 h-6',
  xl: 'w-8 h-8',
};

const colorStyles: Record<IconColor, string> = {
  default: 'text-content',
  secondary: 'text-content-secondary',
  muted: 'text-content-muted',
  brand: 'text-brand',
  success: 'text-status-success',
  warning: 'text-status-warning',
  error: 'text-status-error',
  inherit: 'text-inherit',
};

/**
 * Icon wrapper component for consistent sizing and coloring
 */
export function Icon({ icon: IconComponent, size = 'md', color = 'inherit', className }: IconProps) {
  return (
    <IconComponent
      className={cn(
        sizeStyles[size],
        colorStyles[color],
        className
      )}
    />
  );
}

/**
 * Icon Box - Icon with background container
 */
interface IconBoxProps extends IconProps {
  variant?: 'default' | 'brand' | 'success' | 'warning' | 'error';
}

const boxVariantStyles = {
  default: 'bg-surface border-surface-border',
  brand: 'bg-brand-muted border-brand/20',
  success: 'bg-status-success/10 border-status-success/20',
  warning: 'bg-status-warning/10 border-status-warning/20',
  error: 'bg-status-error/10 border-status-error/20',
};

const boxIconColors: Record<IconBoxProps['variant'] & string, IconColor> = {
  default: 'secondary',
  brand: 'brand',
  success: 'success',
  warning: 'warning',
  error: 'error',
};

export function IconBox({ icon, size = 'md', variant = 'brand', className }: IconBoxProps) {
  const boxSize = {
    xs: 'w-8 h-8',
    sm: 'w-10 h-10',
    md: 'w-12 h-12',
    lg: 'w-14 h-14',
    xl: 'w-16 h-16',
  };

  return (
    <div
      className={cn(
        'inline-flex items-center justify-center rounded-xl border transition-colors',
        boxVariantStyles[variant],
        boxSize[size],
        className
      )}
    >
      <Icon icon={icon} size={size} color={boxIconColors[variant]} />
    </div>
  );
}
