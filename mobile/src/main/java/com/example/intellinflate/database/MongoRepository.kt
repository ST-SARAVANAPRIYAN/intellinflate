package com.example.intellinflate.database

import android.util.Log
import com.example.intellinflate.models.TireScanResult
import com.example.intellinflate.models.VehicleProfile
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * MongoDB Atlas Data API repository for IntelliInflate.
 *
 * Manages three collections:
 *   tire_scans      - every tyre health scan result
 *   vehicles        - registered vehicle profiles
 *   service_history - log of service events
 *
 * All I/O runs on Dispatchers.IO.
 * Fill in AtlasConfig.APP_ID and AtlasConfig.API_KEY before going live.
 */
class MongoRepository private constructor(private val api: AtlasApiService) {

    private val gson = Gson()
    private val TAG = "MongoRepository"

    // Helpers
    private fun baseBody(collection: String): JsonObject = JsonObject().apply {
        addProperty("dataSource", AtlasConfig.DATA_SOURCE)
        addProperty("database",   AtlasConfig.DATABASE)
        addProperty("collection", collection)
    }

    private fun <T> toJsonObject(obj: T): JsonObject =
        JsonParser.parseString(gson.toJson(obj)).asJsonObject

    // Collection: tire_scans

    suspend fun saveTireScan(
        scan: TireScanResult,
        licensePlate: String = "",
        vehicleId: String = ""
    ): String? = withContext(Dispatchers.IO) {
        try {
            val doc = TireScanDocument(
                scanId                   = scan.scanId,
                timestampMillis          = scan.timestamp,
                tirePosition             = scan.tirePosition.name,
                overallScore             = scan.overallCondition.overallScore,
                healthStatus             = scan.overallCondition.overallStatus.name,
                hasCracks                = scan.crackDetection.hasCracks,
                crackSeverity            = scan.crackDetection.crackSeverity.name,
                hasForeignObjects        = scan.treadAnalysis.hasForeignObjects,
                hasSidewallDamage        = scan.sidewallAnalysis.hasDamage,
                averageTreadDepth        = scan.wearAnalysis.averageTreadDepth,
                estimatedRemainingLifeKm = scan.wearAnalysis.estimatedRemainingLife.remainingKilometers ?: 0,
                aiModelVersion           = scan.aiModelVersion,
                licensePlate             = licensePlate,
                vehicleId                = vehicleId
            )
            val body = baseBody(AtlasConfig.COLLECTION_TIRE_SCANS).apply {
                add("document", toJsonObject(doc))
            }
            val resp = api.insertOne(body)
            if (resp.isSuccessful) {
                val insertedId = resp.body()?.get("insertedId")?.asString
                Log.d(TAG, "tire_scans insert ok: $insertedId")
                insertedId
            } else {
                Log.e(TAG, "tire_scans insert failed: ${resp.code()} ${resp.errorBody()?.string()}")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "saveTireScan error", e)
            null
        }
    }

    suspend fun getTireScans(licensePlate: String): List<TireScanDocument> =
        withContext(Dispatchers.IO) {
            try {
                val body = baseBody(AtlasConfig.COLLECTION_TIRE_SCANS).apply {
                    add("filter", JsonObject().apply { addProperty("licensePlate", licensePlate) })
                    add("sort",   JsonObject().apply { addProperty("timestampMillis", -1) })
                }
                val resp = api.find(body)
                if (resp.isSuccessful) {
                    val docs = resp.body()?.getAsJsonArray("documents") ?: return@withContext emptyList()
                    docs.map { gson.fromJson(it, TireScanDocument::class.java) }
                } else {
                    Log.e(TAG, "getTireScans failed: ${resp.code()}")
                    emptyList()
                }
            } catch (e: Exception) {
                Log.e(TAG, "getTireScans error", e)
                emptyList()
            }
        }

    suspend fun getLatestScansPerPosition(licensePlate: String): Map<String, TireScanDocument> =
        withContext(Dispatchers.IO) {
            val all = getTireScans(licensePlate)
            val map = mutableMapOf<String, TireScanDocument>()
            for (doc in all) {
                if (!map.containsKey(doc.tirePosition)) map[doc.tirePosition] = doc
            }
            map
        }

    // Collection: vehicles

    suspend fun saveVehicle(vehicle: VehicleProfile): Boolean = withContext(Dispatchers.IO) {
        try {
            val doc = VehicleDocument(
                vehicleId    = vehicle.vehicleId,
                licensePlate = vehicle.licensePlate,
                vehicleType  = vehicle.vehicleType.name,
                make         = vehicle.make ?: "",
                model        = vehicle.model ?: "",
                year         = vehicle.year ?: 0
            )
            val body = baseBody(AtlasConfig.COLLECTION_VEHICLES).apply {
                add("filter", JsonObject().apply { addProperty("licensePlate", vehicle.licensePlate) })
                add("update", JsonObject().apply {
                    add("\$set", toJsonObject(doc))
                })
                addProperty("upsert", true)
            }
            val resp = api.updateOne(body)
            if (resp.isSuccessful) {
                Log.d(TAG, "vehicles upsert ok: ${vehicle.licensePlate}")
                true
            } else {
                Log.e(TAG, "vehicles upsert failed: ${resp.code()}")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "saveVehicle error", e)
            false
        }
    }

    suspend fun getVehicle(licensePlate: String): VehicleDocument? =
        withContext(Dispatchers.IO) {
            try {
                val body = baseBody(AtlasConfig.COLLECTION_VEHICLES).apply {
                    add("filter", JsonObject().apply { addProperty("licensePlate", licensePlate) })
                }
                val resp = api.findOne(body)
                if (resp.isSuccessful) {
                    val docEl = resp.body()?.get("document")
                    if (docEl != null && !docEl.isJsonNull)
                        gson.fromJson(docEl, VehicleDocument::class.java)
                    else null
                } else {
                    Log.e(TAG, "getVehicle failed: ${resp.code()}")
                    null
                }
            } catch (e: Exception) {
                Log.e(TAG, "getVehicle error", e)
                null
            }
        }

    // Collection: service_history

    suspend fun addServiceEvent(
        vehicleId: String,
        licensePlate: String,
        serviceType: String,
        description: String,
        technicianNote: String = "",
        costEstimate: Float = 0f
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val doc = ServiceHistoryDocument(
                vehicleId      = vehicleId,
                licensePlate   = licensePlate,
                serviceType    = serviceType,
                description    = description,
                technicianNote = technicianNote,
                costEstimate   = costEstimate
            )
            val body = baseBody(AtlasConfig.COLLECTION_SERVICE_HISTORY).apply {
                add("document", toJsonObject(doc))
            }
            val resp = api.insertOne(body)
            if (resp.isSuccessful) {
                Log.d(TAG, "service_history insert ok")
                true
            } else {
                Log.e(TAG, "service_history insert failed: ${resp.code()}")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "addServiceEvent error", e)
            false
        }
    }

    suspend fun getServiceHistory(licensePlate: String): List<ServiceHistoryDocument> =
        withContext(Dispatchers.IO) {
            try {
                val body = baseBody(AtlasConfig.COLLECTION_SERVICE_HISTORY).apply {
                    add("filter", JsonObject().apply { addProperty("licensePlate", licensePlate) })
                    add("sort",   JsonObject().apply { addProperty("performedAtMillis", -1) })
                }
                val resp = api.find(body)
                if (resp.isSuccessful) {
                    val docs = resp.body()?.getAsJsonArray("documents") ?: return@withContext emptyList()
                    docs.map { gson.fromJson(it, ServiceHistoryDocument::class.java) }
                } else {
                    Log.e(TAG, "getServiceHistory failed: ${resp.code()}")
                    emptyList()
                }
            } catch (e: Exception) {
                Log.e(TAG, "getServiceHistory error", e)
                emptyList()
            }
        }

    // Collection: users

    /** Create a new user. Returns the inserted document id or null on failure. */
    suspend fun createUser(user: UserDocument): String? = withContext(Dispatchers.IO) {
        try {
            val body = baseBody(AtlasConfig.COLLECTION_USERS).apply {
                add("document", toJsonObject(user))
            }
            val resp = api.insertOne(body)
            if (resp.isSuccessful) {
                val insertedId = resp.body()?.get("insertedId")?.asString
                Log.d(TAG, "users insert ok: $insertedId")
                insertedId
            } else {
                Log.e(TAG, "users insert failed: ${resp.code()} ${resp.errorBody()?.string()}")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "createUser error", e)
            null
        }
    }

    /** Find a user by email. Returns null if not found. */
    suspend fun getUserByEmail(email: String): UserDocument? = withContext(Dispatchers.IO) {
        try {
            val body = baseBody(AtlasConfig.COLLECTION_USERS).apply {
                add("filter", JsonObject().apply { addProperty("email", email) })
            }
            val resp = api.findOne(body)
            if (resp.isSuccessful) {
                val docEl = resp.body()?.get("document")
                if (docEl != null && !docEl.isJsonNull)
                    gson.fromJson(docEl, UserDocument::class.java)
                else null
            } else {
                Log.e(TAG, "getUserByEmail failed: ${resp.code()}")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "getUserByEmail error", e)
            null
        }
    }

    /** Get all active users, optionally filter by role (e.g. "ADMIN", "TECHNICIAN"). */
    suspend fun getUsers(role: String? = null): List<UserDocument> = withContext(Dispatchers.IO) {
        try {
            val filter = JsonObject().apply {
                addProperty("isActive", true)
                if (role != null) addProperty("role", role)
            }
            val body = baseBody(AtlasConfig.COLLECTION_USERS).apply {
                add("filter", filter)
                add("sort", JsonObject().apply { addProperty("name", 1) })
            }
            val resp = api.find(body)
            if (resp.isSuccessful) {
                val docs = resp.body()?.getAsJsonArray("documents") ?: return@withContext emptyList()
                docs.map { gson.fromJson(it, UserDocument::class.java) }
            } else {
                Log.e(TAG, "getUsers failed: ${resp.code()}")
                emptyList()
            }
        } catch (e: Exception) {
            Log.e(TAG, "getUsers error", e)
            emptyList()
        }
    }

    /** Update user fields (name, phone, role, assignedStationId). */
    suspend fun updateUser(userId: String, name: String, phone: String, role: String, stationId: String): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val body = baseBody(AtlasConfig.COLLECTION_USERS).apply {
                    add("filter", JsonObject().apply { addProperty("userId", userId) })
                    add("update", JsonObject().apply {
                        add("\$set", JsonObject().apply {
                            addProperty("name", name)
                            addProperty("phone", phone)
                            addProperty("role", role)
                            addProperty("assignedStationId", stationId)
                        })
                    })
                }
                val resp = api.updateOne(body)
                resp.isSuccessful.also {
                    if (!it) Log.e(TAG, "updateUser failed: ${resp.code()}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "updateUser error", e)
                false
            }
        }

    /** Soft-delete a user by setting isActive = false. */
    suspend fun deactivateUser(userId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val body = baseBody(AtlasConfig.COLLECTION_USERS).apply {
                add("filter", JsonObject().apply { addProperty("userId", userId) })
                add("update", JsonObject().apply {
                    add("\$set", JsonObject().apply { addProperty("isActive", false) })
                })
            }
            val resp = api.updateOne(body)
            resp.isSuccessful.also {
                if (!it) Log.e(TAG, "deactivateUser failed: ${resp.code()}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "deactivateUser error", e)
            false
        }
    }

    /** Update the lastLoginMillis timestamp for a user. */
    suspend fun recordLogin(userId: String) = withContext(Dispatchers.IO) {
        try {
            val body = baseBody(AtlasConfig.COLLECTION_USERS).apply {
                add("filter", JsonObject().apply { addProperty("userId", userId) })
                add("update", JsonObject().apply {
                    add("\$set", JsonObject().apply {
                        addProperty("lastLoginMillis", System.currentTimeMillis())
                    })
                })
            }
            api.updateOne(body)
        } catch (e: Exception) {
            Log.e(TAG, "recordLogin error", e)
        }
    }

    // Singleton

    companion object {
        @Volatile private var INSTANCE: MongoRepository? = null

        fun getInstance(): MongoRepository =
            INSTANCE ?: synchronized(this) { INSTANCE ?: create().also { INSTANCE = it } }

        private fun create(): MongoRepository {
            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }
            val client = OkHttpClient.Builder()
                .addInterceptor(logging)
                .addInterceptor { chain ->
                    val req = chain.request().newBuilder()
                        .addHeader("api-key", AtlasConfig.API_KEY)
                        .build()
                    chain.proceed(req)
                }
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .build()

            val retrofit = Retrofit.Builder()
                .baseUrl(AtlasConfig.BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            return MongoRepository(retrofit.create(AtlasApiService::class.java))
        }
    }
}
