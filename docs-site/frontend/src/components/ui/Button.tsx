import { cn } from '../../lib/cn';
import type { ButtonHTMLAttributes, ReactNode } from 'react';
import { Link } from 'react-router-dom';
import { forwardRef } from 'react';

type ButtonVariant = 'primary' | 'secondary' | 'ghost' | 'outline' | 'danger';
type ButtonSize = 'sm' | 'md' | 'lg';

interface ButtonBaseProps {
  children: ReactNode;
  variant?: ButtonVariant;
  size?: ButtonSize;
  leftIcon?: ReactNode;
  rightIcon?: ReactNode;
  className?: string;
  fullWidth?: boolean;
}

type ButtonProps = ButtonBaseProps & ButtonHTMLAttributes<HTMLButtonElement>;

const baseStyles = 'inline-flex items-center justify-center gap-2 font-semibold transition-all duration-200 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand focus-visible:ring-offset-2 focus-visible:ring-offset-background disabled:opacity-50 disabled:pointer-events-none';

const variantStyles: Record<ButtonVariant, string> = {
  primary: 'bg-brand hover:bg-brand-hover text-content-inverse shadow-button hover:shadow-button-hover',
  secondary: 'bg-surface hover:bg-surface-hover text-content border border-surface-border hover:border-surface-border-hover',
  ghost: 'hover:bg-surface text-content-secondary hover:text-content',
  outline: 'border border-surface-border-hover hover:bg-surface text-content',
  danger: 'bg-accent-red/15 hover:bg-accent-red/25 text-accent-red border border-accent-red/30',
};

const sizeStyles: Record<ButtonSize, string> = {
  sm: 'h-9 px-3 text-sm rounded-button',
  md: 'h-11 px-5 text-sm rounded-button',
  lg: 'h-12 px-6 text-base rounded-button',
};

/**
 * Button component with multiple variants and sizes
 */
export const Button = forwardRef<HTMLButtonElement, ButtonProps>(
  ({ children, variant = 'primary', size = 'md', leftIcon, rightIcon, className, fullWidth, ...props }, ref) => {
    return (
      <button
        ref={ref}
        className={cn(
          baseStyles,
          variantStyles[variant],
          sizeStyles[size],
          fullWidth && 'w-full',
          className
        )}
        {...props}
      >
        {leftIcon}
        {children}
        {rightIcon}
      </button>
    );
  }
);

Button.displayName = 'Button';

/**
 * Button as React Router Link
 */
interface ButtonLinkProps extends ButtonBaseProps {
  to: string;
}

export function ButtonLink({ children, variant = 'primary', size = 'md', leftIcon, rightIcon, className, fullWidth, to }: ButtonLinkProps) {
  return (
    <Link
      to={to}
      className={cn(
        baseStyles,
        variantStyles[variant],
        sizeStyles[size],
        fullWidth && 'w-full',
        className
      )}
    >
      {leftIcon}
      {children}
      {rightIcon}
    </Link>
  );
}

/**
 * Button as anchor tag for external links
 */
interface ButtonAnchorProps extends ButtonBaseProps {
  href: string;
  target?: string;
  rel?: string;
}

export function ButtonAnchor({ children, variant = 'primary', size = 'md', leftIcon, rightIcon, className, fullWidth, href, target, rel }: ButtonAnchorProps) {
  return (
    <a
      href={href}
      target={target}
      rel={rel}
      className={cn(
        baseStyles,
        variantStyles[variant],
        sizeStyles[size],
        fullWidth && 'w-full',
        className
      )}
    >
      {leftIcon}
      {children}
      {rightIcon}
    </a>
  );
}
