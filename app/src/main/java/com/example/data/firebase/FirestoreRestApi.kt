package com.example.data.firebase

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class FirestoreRestApi {

    private val tag = "FirestoreRestApi"
    private val projectId = "kinza-digital-hub"
    private val apiKey = "AIzaSyDvCAx1EU-o0XztFDbt7isO44vh-jSqI1Q"
    private val baseUrl = "https://firestore.googleapis.com/v1/projects/$projectId/databases/(default)/documents"

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    /**
     * Converts a standard Map<String, Any?> to Firestore REST JSON fields object
     */
    private fun mapToFirestoreFields(map: Map<String, Any?>): JSONObject {
        val fieldsObj = JSONObject()
        for ((key, value) in map) {
            val valueObj = JSONObject()
            when (value) {
                null -> valueObj.put("nullValue", JSONObject.NULL)
                is String -> valueObj.put("stringValue", value)
                is Boolean -> valueObj.put("booleanValue", value)
                is Int -> valueObj.put("integerValue", value.toString())
                is Long -> valueObj.put("integerValue", value.toString())
                is Double -> valueObj.put("doubleValue", value)
                is Float -> valueObj.put("doubleValue", value.toDouble())
                else -> valueObj.put("stringValue", value.toString())
            }
            fieldsObj.put(key, valueObj)
        }
        return fieldsObj
    }

    /**
     * Push or update a document directly via REST API
     */
    suspend fun putDocument(
        collection: String,
        documentId: String,
        data: Map<String, Any?>,
        idToken: String? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val url = "$baseUrl/$collection/$documentId?key=$apiKey"
            val fieldsJson = mapToFirestoreFields(data)
            val rootJson = JSONObject().apply {
                put("fields", fieldsJson)
            }

            val requestBody = rootJson.toString().toRequestBody(jsonMediaType)
            val requestBuilder = Request.Builder()
                .url(url)
                .patch(requestBody)

            if (!idToken.isNullOrBlank()) {
                requestBuilder.addHeader("Authorization", "Bearer $idToken")
            }

            val request = requestBuilder.build()
            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string() ?: ""
                if (response.isSuccessful) {
                    Log.d(tag, "REST Firestore write SUCCESS for $collection/$documentId (HTTP ${response.code})")
                    Result.success(responseBody)
                } else {
                    Log.w(tag, "REST Firestore write error for $collection/$documentId: HTTP ${response.code} -> $responseBody")
                    Result.failure(Exception("HTTP ${response.code}: $responseBody"))
                }
            }
        } catch (e: Exception) {
            Log.w(tag, "REST Firestore exception for $collection/$documentId: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Delete a document via REST API
     */
    suspend fun deleteDocument(
        collection: String,
        documentId: String,
        idToken: String? = null
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val url = "$baseUrl/$collection/$documentId?key=$apiKey"
            val requestBuilder = Request.Builder()
                .url(url)
                .delete()

            if (!idToken.isNullOrBlank()) {
                requestBuilder.addHeader("Authorization", "Bearer $idToken")
            }

            val request = requestBuilder.build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful || response.code == 404) {
                    Log.d(tag, "REST Firestore delete SUCCESS for $collection/$documentId")
                    Result.success(Unit)
                } else {
                    val err = response.body?.string() ?: ""
                    Log.w(tag, "REST Firestore delete error: HTTP ${response.code} -> $err")
                    Result.failure(Exception("HTTP ${response.code}: $err"))
                }
            }
        } catch (e: Exception) {
            Log.w(tag, "REST Firestore delete exception: ${e.message}")
            Result.failure(e)
        }
    }
}
