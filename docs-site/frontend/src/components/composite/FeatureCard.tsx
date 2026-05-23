import { cn } from '../../lib/cn';
import { Link } from 'react-router-dom';
import { ChevronRight } from 'lucide-react';
import type { LucideIcon } from 'lucide-react';

interface FeatureCardProps {
  /** Card title */
  title: string;
  /** Card description */
  description: string;
  /** Lucide icon component */
  icon: LucideIcon;
  /** Link destination */
  to: string;
  /** Show arrow indicator */
  showArrow?: boolean;
  /** Additional class names */
  className?: string;
}

/**
 * Feature Card component for showcasing features on landing pages
 * Consistent height with hover effects
 */
export function FeatureCard({
  title,
  description,
  icon: Icon,
  to,
  showArrow = true,
  className,
}: FeatureCardProps) {
  return (
    <Link
      to={to}
      className={cn(
        // Layout
        'group flex flex-col p-6',
        // Sizing - min height for consistency
        'min-h-[200px]',
        // Background & border
        'bg-surface border border-surface-border rounded-card',
        // Hover states
        'hover:bg-surface-hover hover:border-brand/30',
        // Transition
        'transition-all duration-200',
        className
      )}
    >
      {/* Icon */}
      <div className="w-12 h-12 mb-5 rounded-xl bg-brand-muted flex items-center justify-center group-hover:bg-brand/20 transition-colors">
        <Icon className="w-6 h-6 text-brand" />
      </div>

      {/* Title */}
      <h3 className="text-lg font-semibold text-content mb-2 group-hover:text-brand transition-colors">
        {title}
      </h3>

      {/* Description - flex-grow to push footer down */}
      <p className="text-sm text-content-secondary leading-relaxed flex-grow">
        {description}
      </p>

      {/* Footer with arrow */}
      {showArrow && (
        <div className="mt-4 flex items-center text-sm text-content-muted group-hover:text-brand transition-colors">
          <span>Read more</span>
          <ChevronRight className="w-4 h-4 ml-1 group-hover:translate-x-1 transition-transform" />
        </div>
      )}
    </Link>
  );
}
