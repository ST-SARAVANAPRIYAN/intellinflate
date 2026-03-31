package com.example.intellinflate.network

/**
 * Global Network Configuration for Data Communication
 */
object NetworkConfig {
    /**
     * The Node.js Backend API URL.
     */
    const val BASE_URL = "http://10.247.133.200:3000"

    
    // API Endpoints
    const val LOGIN_URL = "$BASE_URL/api/login"
    const val REGISTER_URL = "$BASE_URL/api/register"
    const val DETECT_URL = "$BASE_URL/api/detect"
    const val HEALTH_URL = "$BASE_URL/health"
}
