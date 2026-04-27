/**
 * Firebase API Client for Migration Scripts
 * 
 * Reads and writes widget configuration to Firebase Realtime Database.
 * 
 * Required environment variables:
 * - FIREBASE_DATABASE_URL: Firebase Realtime Database URL
 * - FIREBASE_SERVICE_ACCOUNT: Path to service account JSON file OR JSON string
 * 
 * Usage:
 *   export FIREBASE_DATABASE_URL="https://your-project.firebaseio.com"
 *   export FIREBASE_SERVICE_ACCOUNT="./service-account.json"
 *   npm run step2
 */

import * as admin from 'firebase-admin';
import { WidgetConfig } from './types';
import { widgetToFirebaseFormat } from './utils';

let firebaseApp: admin.app.App | null = null;

/**
 * Get Firebase configuration from environment
 */
export function getFirebaseConfig(): { databaseURL: string; serviceAccount: admin.ServiceAccount } | null {
  const databaseURL = process.env.FIREBASE_DATABASE_URL;
  const serviceAccountPath = process.env.FIREBASE_SERVICE_ACCOUNT;

  if (!databaseURL || !serviceAccountPath) {
    return null;
  }

  try {
    let serviceAccount: admin.ServiceAccount;
    
    // Check if it's a JSON string or a file path
    if (serviceAccountPath.trim().startsWith('{')) {
      // It's a JSON string
      serviceAccount = JSON.parse(serviceAccountPath);
    } else {
      // It's a file path
      serviceAccount = require(serviceAccountPath);
    }

    return { databaseURL, serviceAccount };
  } catch (error) {
    console.error('Error loading Firebase service account:', error);
    return null;
  }
}

/**
 * Check if Firebase is configured
 */
export function isFirebaseConfigured(): boolean {
  return getFirebaseConfig() !== null;
}

/**
 * Initialize Firebase Admin SDK
 */
export function initializeFirebase(): admin.app.App {
  if (firebaseApp) {
    return firebaseApp;
  }

  const config = getFirebaseConfig();
  if (!config) {
    throw new Error(
      'Firebase not configured. Set environment variables:\n' +
      '  FIREBASE_DATABASE_URL=https://your-project.firebaseio.com\n' +
      '  FIREBASE_SERVICE_ACCOUNT=./path/to/service-account.json'
    );
  }

  firebaseApp = admin.initializeApp({
    credential: admin.credential.cert(config.serviceAccount),
    databaseURL: config.databaseURL
  });

  console.log('Firebase initialized successfully');
  return firebaseApp;
}

/**
 * Get Firebase Realtime Database reference
 */
export function getDatabase(): admin.database.Database {
  const app = initializeFirebase();
  return admin.database(app);
}

/**
 * Save widgets to Firebase for a merchant
 * Path: merchants/{merchantId}/widgets/{widgetId}
 */
export async function saveWidgetsToFirebase(
  merchantId: string,
  widgets: WidgetConfig[]
): Promise<void> {
  const db = getDatabase();
  const widgetsRef = db.ref(`merchants/${merchantId}/widgets`);

  console.log(`  Saving ${widgets.length} widgets to Firebase...`);
  console.log(`  Path: merchants/${merchantId}/widgets`);

  // Convert widgets to Firebase format
  const widgetsData: Record<string, unknown> = {};
  for (const widget of widgets) {
    widgetsData[widget.id] = widgetToFirebaseFormat(widget);
  }

  // Save to Firebase
  await widgetsRef.set(widgetsData);

  console.log(`  ✅ Saved ${widgets.length} widgets to Firebase`);
}

/**
 * Get existing widgets from Firebase for a merchant
 */
export async function getWidgetsFromFirebase(
  merchantId: string
): Promise<Record<string, unknown> | null> {
  const db = getDatabase();
  const widgetsRef = db.ref(`merchants/${merchantId}/widgets`);

  const snapshot = await widgetsRef.once('value');
  return snapshot.val();
}

/**
 * Delete all widgets for a merchant (use with caution)
 */
export async function deleteWidgetsFromFirebase(
  merchantId: string
): Promise<void> {
  const db = getDatabase();
  const widgetsRef = db.ref(`merchants/${merchantId}/widgets`);

  await widgetsRef.remove();
  console.log(`  Deleted all widgets for merchant ${merchantId}`);
}

/**
 * Test Firebase connection
 */
export async function testFirebaseConnection(): Promise<boolean> {
  try {
    const db = getDatabase();
    const testRef = db.ref('.info/connected');
    const snapshot = await testRef.once('value');
    const connected = snapshot.val();
    
    if (connected) {
      console.log('Firebase connection successful');
      return true;
    } else {
      console.log('Firebase not connected');
      return false;
    }
  } catch (error) {
    console.error('Firebase connection failed:', error);
    return false;
  }
}
