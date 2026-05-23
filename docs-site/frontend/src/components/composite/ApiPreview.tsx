import { cn } from '../../lib/cn';
import { MethodBadge } from '../ui/Badge';
import type { HttpMethod } from '../../types/api';

interface ApiPreviewProps {
  /** HTTP method */
  method?: HttpMethod;
  /** API endpoint path */
  endpoint?: string;
  /** Response JSON to display */
  response?: object;
  /** Additional class names */
  className?: string;
}

/**
 * API Preview component for showing code examples
 */
export function ApiPreview({
  method = 'GET',
  endpoint = '/v3/merchants/{mId}/orders',
  response = {
    elements: [
      {
        id: 'ABC123',
        total: 2499,
        paymentState: 'PAID',
        title: 'Order #1001',
        createdTime: 1699900000000,
      },
    ],
  },
  className,
}: ApiPreviewProps) {
  return (
    <div
      className={cn(
        'rounded-card-lg overflow-hidden border border-surface-border bg-background-elevated',
        className
      )}
    >
      {/* Header bar */}
      <div className="flex items-center gap-3 px-4 py-3 bg-surface border-b border-surface-border">
        {/* Traffic lights */}
        <div className="flex items-center gap-1.5">
          <span className="w-3 h-3 rounded-full bg-red-500/80" />
          <span className="w-3 h-3 rounded-full bg-yellow-500/80" />
          <span className="w-3 h-3 rounded-full bg-green-500/80" />
        </div>
        
        {/* Method and endpoint */}
        <div className="flex items-center gap-2 ml-2">
          <MethodBadge method={method} />
          <code className="text-sm text-content-secondary font-mono">{endpoint}</code>
        </div>
      </div>

      {/* Response body */}
      <div className="p-4">
        <div className="text-xs uppercase tracking-wider text-content-muted mb-2 font-medium">
          Response
        </div>
        <pre className="text-sm text-content-secondary font-mono overflow-x-auto">
          <code>{JSON.stringify(response, null, 2)}</code>
        </pre>
      </div>
    </div>
  );
}
