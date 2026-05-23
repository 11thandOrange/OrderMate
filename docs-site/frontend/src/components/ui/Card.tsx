import { cn } from '../../lib/cn';
import type { ReactNode } from 'react';
import { Link } from 'react-router-dom';

type CardVariant = 'default' | 'interactive' | 'elevated';

interface CardBaseProps {
  children: ReactNode;
  variant?: CardVariant;
  className?: string;
  padding?: 'none' | 'sm' | 'md' | 'lg';
}

interface CardAsDiv extends CardBaseProps {
  as?: 'div';
  to?: never;
  href?: never;
  onClick?: () => void;
}

interface CardAsLink extends CardBaseProps {
  as: 'link';
  to: string;
  href?: never;
  onClick?: never;
}

interface CardAsAnchor extends CardBaseProps {
  as: 'anchor';
  href: string;
  to?: never;
  target?: string;
  rel?: string;
  onClick?: never;
}

type CardProps = CardAsDiv | CardAsLink | CardAsAnchor;

const baseStyles = 'block rounded-card border border-surface-border transition-all duration-200';

const variantStyles: Record<CardVariant, string> = {
  default: 'bg-surface',
  interactive: 'bg-surface hover:bg-surface-hover hover:border-surface-border-hover cursor-pointer',
  elevated: 'bg-background-elevated shadow-card hover:shadow-card-hover',
};

const paddingStyles = {
  none: '',
  sm: 'p-4',
  md: 'p-6',
  lg: 'p-8',
};

/**
 * Card component for content containers
 * Can render as div, Link, or anchor tag
 */
export function Card({
  children,
  variant = 'default',
  className,
  padding = 'md',
  ...props
}: CardProps) {
  const classes = cn(
    baseStyles,
    variantStyles[variant],
    paddingStyles[padding],
    className
  );

  if (props.as === 'link') {
    return (
      <Link to={props.to} className={classes}>
        {children}
      </Link>
    );
  }

  if (props.as === 'anchor') {
    return (
      <a href={props.href} target={props.target} rel={props.rel} className={classes}>
        {children}
      </a>
    );
  }

  return (
    <div className={classes} onClick={props.onClick}>
      {children}
    </div>
  );
}

/**
 * Card Header component
 */
interface CardHeaderProps {
  children: ReactNode;
  className?: string;
}

export function CardHeader({ children, className }: CardHeaderProps) {
  return (
    <div className={cn('mb-4', className)}>
      {children}
    </div>
  );
}

/**
 * Card Title component
 */
interface CardTitleProps {
  children: ReactNode;
  className?: string;
  as?: 'h2' | 'h3' | 'h4';
}

export function CardTitle({ children, className, as: Component = 'h3' }: CardTitleProps) {
  return (
    <Component className={cn('text-heading-sm text-content font-semibold', className)}>
      {children}
    </Component>
  );
}

/**
 * Card Description component
 */
interface CardDescriptionProps {
  children: ReactNode;
  className?: string;
}

export function CardDescription({ children, className }: CardDescriptionProps) {
  return (
    <p className={cn('text-body-sm text-content-secondary leading-relaxed', className)}>
      {children}
    </p>
  );
}

/**
 * Card Content component
 */
interface CardContentProps {
  children: ReactNode;
  className?: string;
}

export function CardContent({ children, className }: CardContentProps) {
  return (
    <div className={cn(className)}>
      {children}
    </div>
  );
}

/**
 * Card Footer component
 */
interface CardFooterProps {
  children: ReactNode;
  className?: string;
}

export function CardFooter({ children, className }: CardFooterProps) {
  return (
    <div className={cn('mt-4 pt-4 border-t border-surface-border', className)}>
      {children}
    </div>
  );
}
