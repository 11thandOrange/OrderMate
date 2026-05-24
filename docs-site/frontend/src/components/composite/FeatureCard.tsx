import { cn } from '../../lib/cn';
import { Link } from 'react-router-dom';
import { ChevronRight } from 'lucide-react';
import type { LucideIcon } from 'lucide-react';

interface FeatureCardProps {
  title: string;
  description: string;
  icon: LucideIcon;
  to: string;
  showArrow?: boolean;
  className?: string;
}

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
        'group flex flex-col p-6',
        'min-h-[200px]',
        'bg-surface border border-surface-border rounded-card',
        'hover:bg-surface-hover hover:border-surface-border-hover',
        'transition-all duration-200',
        className
      )}
    >
      <div className="w-12 h-12 mb-5 rounded-xl bg-brand-muted flex items-center justify-center group-hover:bg-brand/20 transition-colors">
        <Icon className="w-6 h-6 text-brand" />
      </div>

      <h3 className="text-lg font-semibold text-white mb-2 group-hover:text-brand transition-colors">
        {title}
      </h3>

      <p className="text-sm text-gray-400 leading-relaxed flex-grow">
        {description}
      </p>

      {showArrow && (
        <div className="mt-4 flex items-center text-sm text-gray-500 group-hover:text-brand transition-colors">
          <span>Read more</span>
          <ChevronRight className="w-4 h-4 ml-1 group-hover:translate-x-1 transition-transform" />
        </div>
      )}
    </Link>
  );
}
