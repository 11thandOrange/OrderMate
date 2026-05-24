import { cn } from '../../lib/cn';
import type { LucideIcon } from 'lucide-react';

interface FeatureCardProps {
  title: string;
  description: string;
  icon: LucideIcon;
  to?: string;
  className?: string;
}

export function FeatureCard({
  title,
  description,
  icon: Icon,
  className,
}: FeatureCardProps) {
  return (
    <div
      className={cn(
        'group flex flex-col p-9 rounded-[24px] transition-all duration-300 cursor-default',
        'hover:-translate-y-2',
        className
      )}
      style={{
        background: 'rgba(255,255,255,0.1)',
        border: '1px solid rgba(255,255,255,0.2)',
        backdropFilter: 'blur(20px)',
        WebkitBackdropFilter: 'blur(20px)',
      }}
      onMouseEnter={(e) => {
        (e.currentTarget as HTMLDivElement).style.boxShadow = '0 16px 48px rgba(0,0,0,0.2)';
        (e.currentTarget as HTMLDivElement).style.borderColor = 'rgba(255,159,67,0.3)';
      }}
      onMouseLeave={(e) => {
        (e.currentTarget as HTMLDivElement).style.boxShadow = '';
        (e.currentTarget as HTMLDivElement).style.borderColor = 'rgba(255,255,255,0.2)';
      }}
    >
      {/* Icon */}
      <div
        className="w-14 h-14 mb-6 rounded-xl flex items-center justify-center text-white text-2xl flex-shrink-0"
        style={{
          background: 'linear-gradient(135deg, #FF9F43 0%, #ffcc80 100%)',
          boxShadow: '0 8px 24px rgba(255,159,67,0.3)',
        }}
      >
        <Icon className="w-6 h-6 text-white" />
      </div>

      {/* Title */}
      <h3 className="text-xl font-semibold text-content mb-3">
        {title}
      </h3>

      {/* Description */}
      <p className="text-[15px] text-content-secondary leading-relaxed">
        {description}
      </p>
    </div>
  );
}
