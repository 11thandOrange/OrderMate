#!/bin/bash
# OrderMate Migration Scripts - Environment Setup
#
# This script sets the environment variables needed for production mode.
# 
# REQUIRED: You must download a Firebase service account JSON file from:
#   1. Go to Firebase Console: https://console.firebase.google.com/project/ordermate-53077
#   2. Project Settings > Service Accounts
#   3. Click "Generate new private key"
#   4. Save the JSON file to this directory as "service-account.json"
#
# Usage:
#   source setup-env.sh

# Firebase Configuration (from google-services.json)
export FIREBASE_DATABASE_URL="https://ordermate-53077-default-rtdb.firebaseio.com"

# Path to service account JSON (download from Firebase Console)
# Update this path if you save the file elsewhere
export FIREBASE_SERVICE_ACCOUNT="$(pwd)/service-account.json"

# Clover Configuration (optional - for reading real orders in Step 1)
# Get these from Clover Developer Dashboard
# export CLOVER_API_TOKEN="your-clover-oauth-token"
# export CLOVER_MERCHANT_ID="your-merchant-id"
# export CLOVER_ENVIRONMENT="sandbox"  # or "production"

echo "Environment variables set:"
echo "  FIREBASE_DATABASE_URL=$FIREBASE_DATABASE_URL"
echo "  FIREBASE_SERVICE_ACCOUNT=$FIREBASE_SERVICE_ACCOUNT"

if [ -f "$FIREBASE_SERVICE_ACCOUNT" ]; then
  echo "  ✅ Service account file found"
else
  echo "  ⚠️  Service account file NOT found"
  echo "     Download from: https://console.firebase.google.com/project/ordermate-53077/settings/serviceaccounts/adminsdk"
fi

if [ -n "$CLOVER_API_TOKEN" ]; then
  echo "  CLOVER_API_TOKEN=****"
  echo "  CLOVER_MERCHANT_ID=$CLOVER_MERCHANT_ID"
  echo "  CLOVER_ENVIRONMENT=$CLOVER_ENVIRONMENT"
else
  echo "  CLOVER_API_TOKEN=(not set - will use mock data for Step 1)"
fi
