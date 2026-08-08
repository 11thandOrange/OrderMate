/**
 * Clover OAuth install callback.
 *
 * Configure this URL as your app's "App URL" in the Clover Developer
 * Dashboard. When a merchant installs the app, Clover redirects their
 * browser here with ?merchant_id=...&code=...&client_id=... - this
 * exchanges that one-time code for a merchant-specific access token so no
 * merchant ever needs to be given or paste a hardcoded API key.
 */

import * as functions from "firebase-functions";
import {exchangeCodeForToken} from "./cloverAuth";

export const cloverOAuthCallback = functions.https.onRequest(async (req, res) => {
  const merchantId = req.query.merchant_id as string | undefined;
  const code = req.query.code as string | undefined;

  if (!merchantId || !code) {
    res.status(400).send("Missing merchant_id or code");
    return;
  }

  try {
    await exchangeCodeForToken(merchantId, code);
    console.log(`Stored Clover OAuth token for merchant ${merchantId}`);
    res.status(200).send(
      "OrderMate is connected. You can close this window and return to Clover."
    );
  } catch (error) {
    console.error(`OAuth code exchange failed for merchant ${merchantId}:`, error);
    res.status(500).send("Failed to complete Clover authorization");
  }
});
