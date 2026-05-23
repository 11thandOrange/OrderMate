import { cn } from '../../lib/cn';
import type { ReactNode, ElementType } from 'react';

type TextVariant = 'display' | 'display-sm' | 'heading' | 'heading-sm' | 'body' | 'body-sm' | 'caption';
type TextColor = 'default' | 'secondary' | 'muted' | 'brand' | 'inherit';
type TextElement = 'p' | 'span' | 'h1' | 'h2' | 'h3' | 'h4' | 'h5' | 'h6' | 'div' | 'label';

interface TextProps {
  children: ReactNode;
  variant?: TextVariant;
  color?: TextColor;
  className?: string;
  as?: TextElement;
}

const variantStyles: Record<TextVariant, string> = {
  'display': 'text-4xl sm:text-5xl md:text-6xl font-bold leading-tight',
  'display-sm': 'text-3xl sm:text-4xl font-bold leading-tight',
  'heading': 'text-2xl sm:text-3xl font-bold leading-snug',
  'heading-sm': 'text-lg sm:text-xl font-semibold leading-snug',
  'body': 'text-base leading-relaxed',
  'body-sm': 'text-sm leading-relaxed',
  'caption': 'text-xs leading-normal',
};

const colorStyles: Record<TextColor, string> = {
  default: 'text-content',
  secondary: 'text-content-secondary',
  muted: 'text-content-muted',
  brand: 'text-brand',
  inherit: 'text-inherit',
};

// Default element for each variant
const variantElements: Record<TextVariant, TextElement> = {
  'display': 'h1',
  'display-sm': 'h2',
  'heading': 'h2',
  'heading-sm': 'h3',
  'body': 'p',
  'body-sm': 'p',
  'caption': 'span',
};

/**
 * Text component for consistent typography
 */
export function Text({
  children,
  variant = 'body',
  color = 'default',
  className,
  as,
}: TextProps) {
  const Component: ElementType = as || variantElements[variant];
  
  return (
    <Component
      className={cn(
        variantStyles[variant],
        colorStyles[color],
        className
      )}
    >
      {children}
    </Component>
  );
}

/**
 * Heading shorthand components
 */
export function DisplayText({ children, className, color }: Omit<TextProps, 'variant' | 'as'>) {
  return <Text variant="display" color={color} className={className}>{children}</Text>;
}

export function Heading({ children, className, color }: Omit<TextProps, 'variant' | 'as'>) {
  return <Text variant="heading" color={color} className={className}>{children}</Text>;
}

export function SubHeading({ children, className, color }: Omit<TextProps, 'variant' | 'as'>) {
  return <Text variant="heading-sm" color={color} className={className}>{children}</Text>;
}

export function Body({ children, className, color = 'secondary' }: Omit<TextProps, 'variant' | 'as'>) {
  return <Text variant="body" color={color} className={className}>{children}</Text>;
}

export function Caption({ children, className, color = 'muted' }: Omit<TextProps, 'variant' | 'as'>) {
  return <Text variant="caption" color={color} className={className}>{children}</Text>;
}
