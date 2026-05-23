import type { Endpoint } from '../../types/api';
import { MethodBadge } from '../ui/Badge';
import { ParamTable } from './ParamTable';
import { CodeBlock } from './CodeBlock';

interface EndpointDocProps {
  endpoint: Endpoint;
}

export function EndpointDoc({ endpoint }: EndpointDocProps) {
  return (
    <div id={endpoint.id} className="scroll-mt-24">
      {/* Header */}
      <div className="mb-6">
        <div className="flex items-center gap-3 mb-3">
          <MethodBadge method={endpoint.method} className="text-sm px-3 py-1" />
          <code className="text-lg font-mono text-gray-300">{endpoint.path}</code>
        </div>
        <h2 className="text-2xl font-bold text-white mb-2">{endpoint.title}</h2>
        <p className="text-gray-400">{endpoint.description}</p>
      </div>

      {/* Parameters */}
      <ParamTable parameters={endpoint.parameters} />

      {/* Request Body */}
      {endpoint.requestBody && (
        <div className="mt-8">
          <h3 className="text-lg font-semibold text-white mb-4">Request Body</h3>
          <p className="text-sm text-gray-400 mb-3">{endpoint.requestBody.description}</p>
          <CodeBlock
            code={JSON.stringify(endpoint.requestBody.example, null, 2)}
            language="json"
            title="Example Request"
          />
        </div>
      )}

      {/* Response */}
      <div className="mt-8">
        <h3 className="text-lg font-semibold text-white mb-4">Response</h3>
        <div className="space-y-3">
          <div className="flex items-center gap-2">
            <span className="px-2 py-0.5 text-xs font-medium rounded bg-emerald-500/20 text-emerald-400 border border-emerald-500/30">
              200
            </span>
            <span className="text-sm text-gray-400">Success</span>
          </div>
          <CodeBlock
            code={JSON.stringify(endpoint.exampleResponse, null, 2)}
            language="json"
            title="Example Response"
          />
        </div>
      </div>
    </div>
  );
}
