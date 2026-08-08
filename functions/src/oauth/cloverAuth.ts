/**
 * Per-merchant Clover OAuth token storage and refresh.
 *
 * Replaces the old single, hardcoded CLOVER_API_TOKEN (which only ever
 * worked for one merchant) with a token obtained per merchant through
 * Clover's real OAuth flow (see cloverOAuthCallback.ts) and persisted at
 * merchants/{merchantId}/cloverAuth.
 */

import * as admin from "firebase-admin";
import axios from "axios";

const db = admin.database();

// access_token is short-lived (Clover: ~30 min); refresh a bit early to
// avoid a request racing the exact expiry instant.
const EXPIRY_SAFETY_MARGIN_MS = 60 * 1000;

export interface CloverAuthRecord {
  accessToken: string;
  refreshToken: string;
  expiresAt: number;
  merchantId: string;
  updatedAt: number | object;
}

interface TokenResponse {
  access_token: string;
  refresh_token: string;
  // Seconds until the access_token expires.
  expires_in: number;
}

/**
 * Base URL for Clover's OAuth token endpoint. Distinct from CLOVER_BASE_URL
 * (the REST API host) - production OAuth lives on www.clover.com, sandbox on
 * apisandbox.dev.clover.com, while the REST API itself is api.clover.com /
 * sandbox.dev.clover.com.
 * @return {string} The OAuth base URL
 */
function oauthBaseUrl(): string {
  return process.env.CLOVER_OAUTH_BASE_URL || "https://www.clover.com";
}

/**
 * Exchanges a one-time authorization code (received on the install redirect)
 * for a merchant-specific access/refresh token pair, and persists it.
 * @param {string} merchantId - The Clover merchant ID
 * @param {string} code - The authorization code from the install redirect
 * @return {Promise<void>}
 */
export async function exchangeCodeForToken(
  merchantId: string,
  code: string
): Promise<void> {
  const clientId = process.env.CLOVER_CLIENT_ID;
  const clientSecret = process.env.CLOVER_CLIENT_SECRET;

  if (!clientId || !clientSecret) {
    throw new Error(
      "CLOVER_CLIENT_ID / CLOVER_CLIENT_SECRET not configured - cannot " +
      "exchange authorization code for an access token"
    );
  }

  const response = await axios.post<TokenResponse>(
    `${oauthBaseUrl()}/oauth/v2/token`,
    {client_id: clientId, client_secret: clientSecret, code},
    {headers: {"Content-Type": "application/json"}}
  );

  await storeToken(merchantId, response.data);
}

/**
 * Uses a stored refresh_token to obtain a new access_token for a merchant
 * whose current one has expired.
 * @param {string} merchantId - The Clover merchant ID
 * @param {string} refreshToken - The merchant's stored refresh token
 * @return {Promise<string>} The new access token
 */
async function refreshAccessToken(
  merchantId: string,
  refreshToken: string
): Promise<string> {
  const clientId = process.env.CLOVER_CLIENT_ID;
  const clientSecret = process.env.CLOVER_CLIENT_SECRET;

  if (!clientId || !clientSecret) {
    throw new Error(
      "CLOVER_CLIENT_ID / CLOVER_CLIENT_SECRET not configured - cannot " +
      "refresh access token"
    );
  }

  const response = await axios.post<TokenResponse>(
    `${oauthBaseUrl()}/oauth/v2/refresh`,
    {
      client_id: clientId,
      client_secret: clientSecret,
      refresh_token: refreshToken,
    },
    {headers: {"Content-Type": "application/json"}}
  );

  await storeToken(merchantId, response.data);
  return response.data.access_token;
}

/**
 * Persists a token response for a merchant.
 * @param {string} merchantId - The Clover merchant ID
 * @param {TokenResponse} token - The token response from Clover
 * @return {Promise<void>}
 */
async function storeToken(merchantId: string, token: TokenResponse): Promise<void> {
  const record: CloverAuthRecord = {
    accessToken: token.access_token,
    refreshToken: token.refresh_token,
    expiresAt: Date.now() + token.expires_in * 1000,
    merchantId,
    updatedAt: admin.database.ServerValue.TIMESTAMP,
  };
  await db.ref(`merchants/${merchantId}/cloverAuth`).set(record);
}

/**
 * Returns a currently-valid access token for the given merchant, refreshing
 * it first if it has expired. Returns null if the merchant has never
 * completed the OAuth flow, or if refreshing fails (e.g. the merchant
 * uninstalled and revoked access) - callers should treat that as "no
 * enrichment available," the same degraded behavior as before this token
 * was per-merchant.
 * @param {string} merchantId - The Clover merchant ID
 * @return {Promise<string | null>} A valid access token, or null
 */
export async function getValidAccessToken(merchantId: string): Promise<string | null> {
  const snapshot = await db.ref(`merchants/${merchantId}/cloverAuth`).once("value");
  const record = snapshot.val() as CloverAuthRecord | null;

  if (!record || !record.accessToken) {
    console.warn(`No stored Clover OAuth token for merchant ${merchantId}`);
    return null;
  }

  if (Date.now() < record.expiresAt - EXPIRY_SAFETY_MARGIN_MS) {
    return record.accessToken;
  }

  try {
    return await refreshAccessToken(merchantId, record.refreshToken);
  } catch (error) {
    console.error(`Failed to refresh Clover token for merchant ${merchantId}:`, error);
    return null;
  }
}
