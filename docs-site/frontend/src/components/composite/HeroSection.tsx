import { cn } from '../../lib/cn';
import { Container } from '../Layout/Container';
import { Badge } from '../ui/Badge';
import { ButtonLink, ButtonAnchor } from '../ui/Button';
import { ArrowRight } from 'lucide-react';
import type { ReactNode } from 'react';
import type { LucideIcon } from 'lucide-react';

interface HeroCTA {
  label: string;
  to?: string;
  href?: string;
  variant?: 'primary' | 'secondary';
  icon?: LucideIcon;
}

interface HeroSectionProps {
  /** Badge text shown above title */
  badge?: {
    text: string;
    icon?: LucideIcon;
  };
  /** Main title - can include JSX for styling parts */
  title: ReactNode;
  /** Subtitle/description */
  subtitle: string;
  /** Call-to-action buttons */
  ctas?: HeroCTA[];
  /** Text alignment */
  align?: 'left' | 'center';
  /** Additional class names */
  className?: string;
}

/**
 * Hero Section component for page headers
 * Handles fixed header offset automatically
 */
export function HeroSection({
  badge,
  title,
  subtitle,
  ctas = [],
  align = 'center',
  className,
}: HeroSectionProps) {
  return (
    <section
      className={cn(
        // Padding: pt-24 = 96px (accounts for 64px header + 32px breathing room)
        'relative pt-24 pb-16 md:pt-28 md:pb-20',
        className
      )}
    >
      <Container size="md">
        <div className={cn(align === 'center' && 'text-center')}>
          {/* Badge */}
          {badge && (() => {
            const BadgeIcon = badge.icon;
            return (
              <div className="mb-6">
                <Badge
                  variant="brand"
                  size="md"
                  icon={BadgeIcon ? <BadgeIcon className="w-4 h-4" /> : undefined}
                >
                  {badge.text}
                </Badge>
              </div>
            );
          })()}

          {/* Title */}
          <h1 className="text-4xl sm:text-5xl md:text-6xl font-bold text-content mb-6 leading-tight">
            {title}
          </h1>

          {/* Subtitle */}
          <p
            className={cn(
              'text-lg text-content-secondary mb-10 leading-relaxed',
              align === 'center' && 'max-w-2xl mx-auto'
            )}
          >
            {subtitle}
          </p>

          {/* CTAs */}
          {ctas.length > 0 && (
            <div
              className={cn(
                'flex flex-wrap gap-4',
                align === 'center' && 'justify-center'
              )}
            >
              {ctas.map((cta, index) => {
                const variant = cta.variant || (index === 0 ? 'primary' : 'secondary');
                const IconComponent = cta.icon;
                const icon = IconComponent ? <IconComponent className="w-4 h-4" /> : null;
                const arrow = index === 0 && !cta.icon ? <ArrowRight className="w-4 h-4" /> : null;

                if (cta.href) {
                  return (
                    <ButtonAnchor
                      key={cta.label}
                      href={cta.href}
                      variant={variant}
                      size="lg"
                      leftIcon={icon}
                      rightIcon={arrow}
                      target="_blank"
                      rel="noopener noreferrer"
                    >
                      {cta.label}
                    </ButtonAnchor>
                  );
                }

                return (
                  <ButtonLink
                    key={cta.label}
                    to={cta.to || '/'}
                    variant={variant}
                    size="lg"
                    leftIcon={icon}
                    rightIcon={arrow}
                  >
                    {cta.label}
                  </ButtonLink>
                );
              })}
            </div>
          )}
        </div>
      </Container>
    </section>
  );
}
