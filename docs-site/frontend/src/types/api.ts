export type HttpMethod = 'GET' | 'POST' | 'PUT' | 'DELETE' | 'PATCH';

export interface Parameter {
  name: string;
  in: 'path' | 'query' | 'header' | 'body';
  type: string;
  required: boolean;
  description: string;
  default?: string;
  enum?: string[];
}

export interface Endpoint {
  id: string;
  method: HttpMethod;
  path: string;
  title: string;
  description: string;
  category: string;
  parameters: Parameter[];
  requestBody?: {
    type: string;
    description: string;
    example: object;
  };
  responseSchema: object;
  exampleResponse: object;
}

export interface ApiResponse {
  status: number;
  statusText: string;
  headers: Record<string, string>;
  data: unknown;
  time: number;
}

export interface ApiConfig {
  baseUrl: string;
  merchantId: string;
  apiKey: string;
}

export interface NavItem {
  title: string;
  href: string;
  icon?: string;
  children?: NavItem[];
}

export interface CodeLanguage {
  id: string;
  label: string;
  prismLanguage: string;
}
