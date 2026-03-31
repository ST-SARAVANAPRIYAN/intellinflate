package com.example.intellinflate.database

import com.google.gson.JsonObject
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

/**
 * Retrofit interface wrapping the MongoDB Atlas Data API v1.
 * Each method corresponds to one REST endpoint.
 *
 * Request body must always include:
 *   "dataSource" → AtlasConfig.DATA_SOURCE
 *   "database"   → AtlasConfig.DATABASE
 *   "collection" → one of the AtlasConfig.COLLECTION_* constants
 */
interface AtlasApiService {

    // ── Insert one document ────────────────────────────────────────────────
    @POST("action/insertOne")
    @Headers("Content-Type: application/json")
    suspend fun insertOne(@Body body: JsonObject): Response<JsonObject>

    // ── Insert many documents ──────────────────────────────────────────────
    @POST("action/insertMany")
    @Headers("Content-Type: application/json")
    suspend fun insertMany(@Body body: JsonObject): Response<JsonObject>

    // ── Find one document ──────────────────────────────────────────────────
    @POST("action/findOne")
    @Headers("Content-Type: application/json")
    suspend fun findOne(@Body body: JsonObject): Response<JsonObject>

    // ── Find many documents ────────────────────────────────────────────────
    @POST("action/find")
    @Headers("Content-Type: application/json")
    suspend fun find(@Body body: JsonObject): Response<JsonObject>

    // ── Update one document ────────────────────────────────────────────────
    @POST("action/updateOne")
    @Headers("Content-Type: application/json")
    suspend fun updateOne(@Body body: JsonObject): Response<JsonObject>

    // ── Delete one document ────────────────────────────────────────────────
    @POST("action/deleteOne")
    @Headers("Content-Type: application/json")
    suspend fun deleteOne(@Body body: JsonObject): Response<JsonObject>

    // ── Delete many documents ──────────────────────────────────────────────
    @POST("action/deleteMany")
    @Headers("Content-Type: application/json")
    suspend fun deleteMany(@Body body: JsonObject): Response<JsonObject>
}
