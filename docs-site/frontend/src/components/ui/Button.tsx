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

const baseStyles = 'inline-flex items-center justify-center gap-2 font-semibold transition-all duration-300 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand focus-visible:ring-offset-2 disabled:opacity-50 disabled:pointer-events-none';

const variantStyles: Record<ButtonVariant, string> = {
  primary: 'bg-brand hover:bg-brand-hover text-white shadow-[0_4px_16px_rgba(255,159,67,0.4)] hover:shadow-[0_8px_24px_rgba(255,159,67,0.5)] hover:-translate-y-0.5',
  secondary: 'bg-transparent text-content border-2 border-glass-border hover:bg-glass-bg',
  ghost: 'hover:bg-glass-bg text-content-secondary hover:text-content',
  outline: 'border border-glass-border hover:bg-glass-bg text-content',
  danger: 'bg-accent-red/15 hover:bg-accent-red/25 text-accent-red border border-accent-red/30',
};

const sizeStyles: Record<ButtonSize, string> = {
  sm: 'h-9 px-4 text-sm rounded-full',
  md: 'h-11 px-6 text-sm rounded-full',
  lg: 'h-12 px-8 text-base rounded-full',
};

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
