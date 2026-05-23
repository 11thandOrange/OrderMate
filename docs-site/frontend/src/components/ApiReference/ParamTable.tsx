import type { Parameter } from '../../types/api';
import { Badge } from '../ui/Badge';

interface ParamTableProps {
  parameters: Parameter[];
  title?: string;
}

export function ParamTable({ parameters, title = 'Parameters' }: ParamTableProps) {
  if (parameters.length === 0) return null;

  const pathParams = parameters.filter((p) => p.in === 'path');
  const queryParams = parameters.filter((p) => p.in === 'query');
  const headerParams = parameters.filter((p) => p.in === 'header');

  const renderParams = (params: Parameter[], sectionTitle: string) => {
    if (params.length === 0) return null;

    return (
      <div className="mb-6">
        <h4 className="text-sm font-medium text-gray-400 uppercase tracking-wider mb-3">
          {sectionTitle}
        </h4>
        <div className="space-y-4">
          {params.map((param) => (
            <div
              key={param.name}
              className="pb-4 border-b border-white/5 last:border-0 last:pb-0"
            >
              <div className="flex items-center gap-2 mb-1">
                <code className="text-sm font-mono text-white">{param.name}</code>
                <span className="text-xs text-gray-500">{param.type}</span>
                {param.required && (
                  <Badge variant="warning">required</Badge>
                )}
              </div>
              <p className="text-sm text-gray-400">{param.description}</p>
              {param.default && (
                <p className="text-xs text-gray-500 mt-1">
                  Default: <code className="text-gray-400">{param.default}</code>
                </p>
              )}
              {param.enum && (
                <p className="text-xs text-gray-500 mt-1">
                  Enum: {param.enum.map((e) => (
                    <code key={e} className="text-gray-400 mr-1">{e}</code>
                  ))}
                </p>
              )}
            </div>
          ))}
        </div>
      </div>
    );
  };

  return (
    <div className="mt-8">
      <h3 className="text-lg font-semibold text-white mb-4">{title}</h3>
      {renderParams(pathParams, 'Path Parameters')}
      {renderParams(queryParams, 'Query Parameters')}
      {renderParams(headerParams, 'Header Parameters')}
    </div>
  );
}
