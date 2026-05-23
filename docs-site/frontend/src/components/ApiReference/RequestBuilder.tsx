import { useState, useEffect } from 'react';
import { Play, Loader2 } from 'lucide-react';
import type { Endpoint, ApiResponse } from '../../types/api';
import { Button } from '../ui/Button';
import { Input } from '../ui/Input';
import { MethodBadge } from '../ui/Badge';
import { CodeBlock } from './CodeBlock';

interface RequestBuilderProps {
  endpoint: Endpoint;
  onResponse?: (response: ApiResponse) => void;
}

export function RequestBuilder({ endpoint, onResponse }: RequestBuilderProps) {
  const [loading, setLoading] = useState(false);
  const [response, setResponse] = useState<ApiResponse | null>(null);
  const [activeTab, setActiveTab] = useState<'response' | 'curl' | 'python' | 'kotlin'>('response');
  
  // Form state
  const [apiKey, setApiKey] = useState(() => localStorage.getItem('clover_api_key') || '');
  const [merchantId, setMerchantId] = useState(() => localStorage.getItem('clover_merchant_id') || '');
  const [params, setParams] = useState<Record<string, string>>({});
  const [body, setBody] = useState<string>(
    endpoint.requestBody ? JSON.stringify(endpoint.requestBody.example, null, 2) : ''
  );

  // Save credentials to localStorage
  useEffect(() => {
    if (apiKey) localStorage.setItem('clover_api_key', apiKey);
    if (merchantId) localStorage.setItem('clover_merchant_id', merchantId);
  }, [apiKey, merchantId]);

  const buildUrl = () => {
    let url = endpoint.path;
    url = url.replace('{mId}', merchantId || '{mId}');
    
    // Replace path params
    endpoint.parameters
      .filter((p) => p.in === 'path' && p.name !== 'mId')
      .forEach((p) => {
        url = url.replace(`{${p.name}}`, params[p.name] || `{${p.name}}`);
      });

    // Add query params
    const queryParams = endpoint.parameters
      .filter((p) => p.in === 'query' && params[p.name])
      .map((p) => `${p.name}=${encodeURIComponent(params[p.name])}`)
      .join('&');

    if (queryParams) {
      url += `?${queryParams}`;
    }

    return url;
  };

  const generateCurl = () => {
    const url = `https://api.clover.com${buildUrl()}`;
    let curl = `curl -X ${endpoint.method} "${url}"`;
    curl += ` \\\n  -H "Authorization: Bearer ${apiKey || 'YOUR_API_KEY'}"`;
    curl += ` \\\n  -H "Content-Type: application/json"`;
    
    if (endpoint.requestBody && body) {
      curl += ` \\\n  -d '${body.replace(/\n/g, '')}'`;
    }
    
    return curl;
  };

  const generatePython = () => {
    const url = buildUrl();
    let code = `import requests

url = f"https://api.clover.com${url}"
headers = {
    "Authorization": f"Bearer ${apiKey || 'YOUR_API_KEY'}",
    "Content-Type": "application/json"
}
`;
    
    if (endpoint.requestBody && body) {
      code += `
payload = ${body}

response = requests.${endpoint.method.toLowerCase()}(url, headers=headers, json=payload)`;
    } else {
      code += `
response = requests.${endpoint.method.toLowerCase()}(url, headers=headers)`;
    }
    
    code += `
print(response.json())`;
    
    return code;
  };

  const generateKotlin = () => {
    const url = buildUrl();
    let code = `val client = OkHttpClient()

val request = Request.Builder()
    .url("https://api.clover.com${url}")
    .addHeader("Authorization", "Bearer ${apiKey || 'YOUR_API_KEY'}")
    .addHeader("Content-Type", "application/json")`;
    
    if (endpoint.requestBody && body) {
      code += `
    .${endpoint.method.toLowerCase()}(
        """${body}""".trimIndent()
            .toRequestBody("application/json".toMediaType())
    )`;
    } else if (endpoint.method !== 'GET') {
      code += `
    .${endpoint.method.toLowerCase()}("".toRequestBody())`;
    }
    
    code += `
    .build()

client.newCall(request).execute().use { response ->
    println(response.body?.string())
}`;
    
    return code;
  };

  const handleSubmit = async () => {
    setLoading(true);
    setActiveTab('response');
    
    // Simulate API call with mock response
    await new Promise((resolve) => setTimeout(resolve, 800));
    
    const mockResponse: ApiResponse = {
      status: 200,
      statusText: 'OK',
      headers: {
        'content-type': 'application/json',
        'x-request-id': 'req_' + Math.random().toString(36).substr(2, 9),
      },
      data: endpoint.exampleResponse,
      time: Math.floor(Math.random() * 200) + 100,
    };
    
    setResponse(mockResponse);
    onResponse?.(mockResponse);
    setLoading(false);
  };

  const pathParams = endpoint.parameters.filter((p) => p.in === 'path' && p.name !== 'mId');
  const queryParamsList = endpoint.parameters.filter((p) => p.in === 'query');

  return (
    <div className="space-y-4">
      {/* Endpoint Header */}
      <div className="flex items-center gap-3 p-3 bg-white/5 rounded-lg border border-white/10">
        <MethodBadge method={endpoint.method} />
        <code className="text-sm text-gray-300 font-mono flex-1 truncate">
          {buildUrl()}
        </code>
      </div>

      {/* Credentials */}
      <div className="space-y-3">
        <Input
          label="API Key"
          type="password"
          placeholder="Enter your Clover API key"
          value={apiKey}
          onChange={(e) => setApiKey(e.target.value)}
        />
        <Input
          label="Merchant ID"
          placeholder="Enter merchant ID"
          value={merchantId}
          onChange={(e) => setMerchantId(e.target.value)}
        />
      </div>

      {/* Path Parameters */}
      {pathParams.length > 0 && (
        <div className="space-y-3">
          <h4 className="text-sm font-medium text-gray-400">Path Parameters</h4>
          {pathParams.map((param) => (
            <Input
              key={param.name}
              label={param.name}
              placeholder={param.description}
              value={params[param.name] || ''}
              onChange={(e) => setParams({ ...params, [param.name]: e.target.value })}
            />
          ))}
        </div>
      )}

      {/* Query Parameters */}
      {queryParamsList.length > 0 && (
        <div className="space-y-3">
          <h4 className="text-sm font-medium text-gray-400">Query Parameters</h4>
          {queryParamsList.map((param) => (
            <Input
              key={param.name}
              label={`${param.name}${param.required ? ' *' : ''}`}
              placeholder={param.description}
              value={params[param.name] || ''}
              onChange={(e) => setParams({ ...params, [param.name]: e.target.value })}
            />
          ))}
        </div>
      )}

      {/* Request Body */}
      {endpoint.requestBody && (
        <div className="space-y-2">
          <h4 className="text-sm font-medium text-gray-400">Request Body</h4>
          <textarea
            value={body}
            onChange={(e) => setBody(e.target.value)}
            className="w-full h-40 px-3 py-2 bg-white/5 border border-white/10 rounded-lg text-white font-mono text-sm focus:outline-none focus:ring-2 focus:ring-om-orange/50 resize-none"
          />
        </div>
      )}

      {/* Try It Button */}
      <Button
        onClick={handleSubmit}
        disabled={loading}
        className="w-full"
      >
        {loading ? (
          <>
            <Loader2 className="w-4 h-4 mr-2 animate-spin" />
            Sending...
          </>
        ) : (
          <>
            <Play className="w-4 h-4 mr-2" />
            Try it
          </>
        )}
      </Button>

      {/* Response/Code Tabs */}
      <div className="border-t border-white/10 pt-4">
        <div className="flex gap-1 mb-3">
          {(['response', 'curl', 'python', 'kotlin'] as const).map((tab) => (
            <button
              key={tab}
              onClick={() => setActiveTab(tab)}
              className={`px-3 py-1.5 text-sm rounded-lg transition-colors ${
                activeTab === tab
                  ? 'bg-white/10 text-white'
                  : 'text-gray-400 hover:text-white hover:bg-white/5'
              }`}
            >
              {tab === 'response' ? 'Response' : tab.charAt(0).toUpperCase() + tab.slice(1)}
            </button>
          ))}
        </div>

        {activeTab === 'response' && (
          <div className="rounded-lg bg-[#1a1a2e] border border-white/10">
            {response ? (
              <div>
                <div className="flex items-center justify-between px-4 py-2 border-b border-white/10">
                  <span className={`text-sm font-medium ${response.status < 400 ? 'text-emerald-400' : 'text-red-400'}`}>
                    {response.status} {response.statusText}
                  </span>
                  <span className="text-xs text-gray-500">{response.time}ms</span>
                </div>
                <pre className="p-4 text-sm text-gray-300 overflow-x-auto">
                  {JSON.stringify(response.data, null, 2)}
                </pre>
              </div>
            ) : (
              <div className="p-8 text-center text-gray-500 text-sm">
                Click "Try it" to see the response
              </div>
            )}
          </div>
        )}

        {activeTab === 'curl' && <CodeBlock code={generateCurl()} language="bash" />}
        {activeTab === 'python' && <CodeBlock code={generatePython()} language="python" />}
        {activeTab === 'kotlin' && <CodeBlock code={generateKotlin()} language="kotlin" />}
      </div>
    </div>
  );
}
