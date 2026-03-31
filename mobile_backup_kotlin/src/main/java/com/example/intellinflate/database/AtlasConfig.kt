package com.example.intellinflate.database

/**
 * MongoDB Atlas Data API configuration.
 *
 * How to set up (one-time):
 * 1. Go to https://cloud.mongodb.com → your Project → App Services
 * 2. Create an App (or use existing) → Data API tab → Enable Data API
 * 3. Copy your App ID and generate an API Key
 * 4. Replace the placeholders below with your values
 * 5. Your cluster and database names are shown in the Atlas cluster view
 *
 * Collections created automatically on first insert:
 *   • tire_scans
 *   • vehicles
 *   • service_history
 */
object AtlasConfig {
    // ── Replace with your Atlas App ID ─────────────────────────────────────
    const val APP_ID = "YOUR_ATLAS_APP_ID"          // e.g. "intellinflate-abcde"

    // ── Replace with your Atlas Data API key ───────────────────────────────
    const val API_KEY = "YOUR_ATLAS_DATA_API_KEY"   // from Atlas App Services → API Keys

    // ── Atlas cluster & database ────────────────────────────────────────────
    const val DATA_SOURCE = "Cluster0"               // your Atlas cluster name
    const val DATABASE    = "intellinflate_db"

    // ── Collection names ────────────────────────────────────────────────────
    const val COLLECTION_TIRE_SCANS      = "tire_scans"
    const val COLLECTION_VEHICLES        = "vehicles"
    const val COLLECTION_SERVICE_HISTORY = "service_history"
    const val COLLECTION_USERS           = "users"

    // ── Base URL (do NOT change) ────────────────────────────────────────────
    const val BASE_URL = "https://data.mongodb-api.com/app/${APP_ID}/endpoint/data/v1/"
}
