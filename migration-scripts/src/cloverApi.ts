/**
 * Clover API Client for Migration Scripts
 * 
 * Fetches orders and line items from Clover REST API.
 * 
 * Required environment variables:
 * - CLOVER_API_TOKEN: Clover API OAuth token
 * - CLOVER_MERCHANT_ID: Merchant ID to fetch orders for
 * - CLOVER_ENVIRONMENT: 'sandbox' or 'production' (default: 'sandbox')
 * 
 * Usage:
 *   export CLOVER_API_TOKEN="your-token"
 *   export CLOVER_MERCHANT_ID="your-merchant-id"
 *   npm run step1
 */

import { CloverOrder, CloverLineItem } from './types';

// Clover API base URLs
const CLOVER_SANDBOX_URL = 'https://sandbox.dev.clover.com';
const CLOVER_PRODUCTION_URL = 'https://api.clover.com';

// Configuration from environment
export function getCloverConfig(): { baseUrl: string; token: string; merchantId: string } | null {
  const token = process.env.CLOVER_API_TOKEN;
  const merchantId = process.env.CLOVER_MERCHANT_ID;
  const environment = process.env.CLOVER_ENVIRONMENT || 'sandbox';

  if (!token || !merchantId) {
    return null;
  }

  const baseUrl = environment === 'production' ? CLOVER_PRODUCTION_URL : CLOVER_SANDBOX_URL;

  return { baseUrl, token, merchantId };
}

/**
 * Check if Clover API credentials are configured
 */
export function isCloverConfigured(): boolean {
  return getCloverConfig() !== null;
}

/**
 * Fetch orders from Clover API
 * Includes line items with their notes
 */
export async function fetchCloverOrders(
  merchantId?: string,
  limit: number = 1000
): Promise<CloverOrder[]> {
  const config = getCloverConfig();
  
  if (!config) {
    throw new Error(
      'Clover API not configured. Set environment variables:\n' +
      '  CLOVER_API_TOKEN=your-token\n' +
      '  CLOVER_MERCHANT_ID=your-merchant-id\n' +
      '  CLOVER_ENVIRONMENT=sandbox|production (optional, default: sandbox)'
    );
  }

  const targetMerchantId = merchantId || config.merchantId;
  const url = `${config.baseUrl}/v3/merchants/${targetMerchantId}/orders?expand=lineItems&limit=${limit}`;

  console.log(`  Fetching orders from Clover API...`);
  console.log(`  URL: ${url.replace(config.token, '***')}`);

  const response = await fetch(url, {
    method: 'GET',
    headers: {
      'Authorization': `Bearer ${config.token}`,
      'Content-Type': 'application/json'
    }
  });

  if (!response.ok) {
    const errorText = await response.text();
    throw new Error(`Clover API error ${response.status}: ${errorText}`);
  }

  const data = await response.json() as { elements?: any[] };
  const elements = data.elements || [];

  console.log(`  Fetched ${elements.length} orders from Clover`);

  // Transform to our CloverOrder format
  const orders: CloverOrder[] = elements.map((order: any) => ({
    id: order.id,
    note: order.note || undefined,
    lineItems: order.lineItems ? {
      elements: (order.lineItems.elements || []).map((item: any) => ({
        id: item.id,
        name: item.name,
        note: item.note || undefined
      }))
    } : undefined
  }));

  return orders;
}

/**
 * Fetch a single order with line items
 */
export async function fetchCloverOrder(orderId: string, merchantId?: string): Promise<CloverOrder | null> {
  const config = getCloverConfig();
  
  if (!config) {
    throw new Error('Clover API not configured');
  }

  const targetMerchantId = merchantId || config.merchantId;
  const url = `${config.baseUrl}/v3/merchants/${targetMerchantId}/orders/${orderId}?expand=lineItems`;

  const response = await fetch(url, {
    method: 'GET',
    headers: {
      'Authorization': `Bearer ${config.token}`,
      'Content-Type': 'application/json'
    }
  });

  if (!response.ok) {
    if (response.status === 404) {
      return null;
    }
    const errorText = await response.text();
    throw new Error(`Clover API error ${response.status}: ${errorText}`);
  }

  const order = await response.json() as any;

  return {
    id: order.id,
    note: order.note || undefined,
    lineItems: order.lineItems ? {
      elements: (order.lineItems.elements || []).map((item: any) => ({
        id: item.id,
        name: item.name,
        note: item.note || undefined
      }))
    } : undefined
  };
}

/**
 * Fetch line items for an order
 */
export async function fetchCloverLineItems(orderId: string, merchantId?: string): Promise<CloverLineItem[]> {
  const config = getCloverConfig();
  
  if (!config) {
    throw new Error('Clover API not configured');
  }

  const targetMerchantId = merchantId || config.merchantId;
  const url = `${config.baseUrl}/v3/merchants/${targetMerchantId}/orders/${orderId}/line_items`;

  const response = await fetch(url, {
    method: 'GET',
    headers: {
      'Authorization': `Bearer ${config.token}`,
      'Content-Type': 'application/json'
    }
  });

  if (!response.ok) {
    const errorText = await response.text();
    throw new Error(`Clover API error ${response.status}: ${errorText}`);
  }

  const data = await response.json() as { elements?: any[] };
  const elements = data.elements || [];

  return elements.map((item: any) => ({
    id: item.id,
    name: item.name,
    note: item.note || undefined
  }));
}

/**
 * Test Clover API connection
 */
export async function testCloverConnection(): Promise<boolean> {
  const config = getCloverConfig();
  
  if (!config) {
    console.log('Clover API not configured');
    return false;
  }

  try {
    const url = `${config.baseUrl}/v3/merchants/${config.merchantId}`;
    
    const response = await fetch(url, {
      method: 'GET',
      headers: {
        'Authorization': `Bearer ${config.token}`,
        'Content-Type': 'application/json'
      }
    });

    if (response.ok) {
      const merchant = await response.json() as { name?: string; id?: string };
      console.log(`Connected to Clover merchant: ${merchant.name} (${merchant.id})`);
      return true;
    } else {
      console.log(`Clover API error: ${response.status}`);
      return false;
    }
  } catch (error) {
    console.log(`Clover API connection failed: ${error}`);
    return false;
  }
}
