package com.toymakerftw.mothership.demo

import com.toymakerftw.mothership.BuildConfig
import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.security.MessageDigest
import java.util.*
import java.util.concurrent.TimeUnit
import javax.crypto.Cipher
import javax.crypto.Mac
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import kotlin.coroutines.suspendCoroutine

class DemoKeyManager(private val context: Context) {
    companion object {
        private const val TAG = "DemoKeyManager"
        private const val SHARED_PREFS_NAME = "demo_key_prefs"
        private const val DEVICE_ID_KEY = "device_id"
        private const val DEMO_API_KEY = "demo_api_key"
        private const val LAST_RESET_TIME = "last_reset_time"
        private const val USAGE_COUNT = "usage_count"
        
        // Constants from BuildConfig
        private val PSK = BuildConfig.DEMO_PSK
        private val APP_SECRET = BuildConfig.DEMO_APP_SECRET
        private val API_URL = BuildConfig.DEMO_API_URL
        
        // Max 5 calls per day
        private const val MAX_DAILY_USES = 5
        private const val MILLIS_IN_DAY = 24 * 60 * 60 * 1000L
        
        init {
            // Validate PSK
            try {
                val pskBytes = Base64.getDecoder().decode(PSK)
                Log.d(TAG, "PSK validation successful, decoded length: ${pskBytes.size}")
            } catch (e: Exception) {
                Log.e(TAG, "PSK validation failed", e)
            }
        }
    }
    
    private val gson = Gson()
    private val sharedPreferences = context.getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE)
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
    
    /**
     * Registers the device with the backend and saves the device ID
     */
    suspend fun registerDevice(): Boolean = suspendCoroutine { continuation ->
        try {
            // Check if already registered
            val existingDeviceId = getDeviceId()
            if (existingDeviceId != null) {
                Log.d(TAG, "Device already registered with ID: $existingDeviceId")
                continuation.resumeWith(Result.success(true))
                return@suspendCoroutine
            }
            
            // Get device info (using model as example)
            val deviceModel = android.os.Build.MODEL
            Log.d(TAG, "Registering device with model: $deviceModel")
            
            val registerRequest = RegisterDeviceRequest(
                appSecret = APP_SECRET,
                deviceInfo = mapOf("model" to deviceModel)
            )
            
            val json = gson.toJson(registerRequest)
            Log.d(TAG, "Register request JSON: $json")
            
            val body = json.toRequestBody("application/json".toMediaType())
            
            val request = Request.Builder()
                .url("$API_URL/api/register-device")
                .post(body)
                .build()
            
            httpClient.newCall(request).enqueue(object : okhttp3.Callback {
                override fun onFailure(call: okhttp3.Call, e: IOException) {
                    Log.e(TAG, "Failed to register device", e)
                    continuation.resumeWith(Result.success(false))
                }
                
                override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                    try {
                        Log.d(TAG, "Register device response code: ${response.code}")
                        if (!response.isSuccessful) {
                            Log.e(TAG, "Failed to register device: ${response.code}")
                            continuation.resumeWith(Result.success(false))
                            return
                        }
                        
                        val responseBody = response.body?.string()
                        Log.d(TAG, "Register device response body: $responseBody")
                        if (responseBody.isNullOrEmpty()) {
                            Log.e(TAG, "Empty response from register device")
                            continuation.resumeWith(Result.success(false))
                            return
                        }
                        
                        val registerResponse = gson.fromJson(responseBody, RegisterDeviceResponse::class.java)
                        if (registerResponse.deviceId.isNullOrEmpty()) {
                            Log.e(TAG, "Invalid device ID in response")
                            continuation.resumeWith(Result.success(false))
                            return
                        }
                        
                        // Save device ID
                        sharedPreferences.edit()
                            .putString(DEVICE_ID_KEY, registerResponse.deviceId)
                            .apply()
                        
                        Log.d(TAG, "Device registered successfully with ID: ${registerResponse.deviceId}")
                        continuation.resumeWith(Result.success(true))
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing register device response", e)
                        continuation.resumeWith(Result.success(false))
                    } finally {
                        response.close()
                    }
                }
            })
        } catch (e: Exception) {
            Log.e(TAG, "Error registering device", e)
            continuation.resumeWith(Result.success(false))
        }
    }
    
    /**
     * Generates HMAC signature for the device ID using PSK
     */
    private fun generateHmac(deviceId: String): String {
        Log.d(TAG, "Generating HMAC for device ID: $deviceId")
        
        try {
            // Use PSK directly as string (like bash script does), not decoded
            val pskBytes = PSK.toByteArray(Charsets.UTF_8)
            Log.d(TAG, "PSK bytes length: ${pskBytes.size}")
            Log.d(TAG, "PSK bytes: ${pskBytes.contentToString()}")
            
            // Create HMAC-SHA256
            val mac = Mac.getInstance("HmacSHA256")
            val secretKeySpec = SecretKeySpec(pskBytes, "HmacSHA256")
            mac.init(secretKeySpec)
            
            // Process device ID exactly as echo -n would (no trailing newline)
            val deviceIdBytes = deviceId.toByteArray(Charsets.UTF_8)
            Log.d(TAG, "Device ID bytes length: ${deviceIdBytes.size}")
            Log.d(TAG, "Device ID bytes: ${deviceIdBytes.contentToString()}")
            
            // Compute HMAC
            val hmacBytes = mac.doFinal(deviceIdBytes)
            Log.d(TAG, "Raw HMAC bytes: ${hmacBytes.contentToString()}")
            
            // Encode as base64 (standard encoding with padding)
            val hmac = Base64.getEncoder().encodeToString(hmacBytes)
            Log.d(TAG, "Base64 encoded HMAC: $hmac")
            
            return hmac
        } catch (e: Exception) {
            Log.e(TAG, "Error generating HMAC", e)
            throw e
        }
    }
    
    /**
     * Fetches a demo API key from the backend
     */
    suspend fun fetchDemoApiKey(): String? = suspendCoroutine { continuation ->
        try {
            val deviceId = getDeviceId()
            if (deviceId.isNullOrEmpty()) {
                Log.e(TAG, "Device not registered")
                continuation.resumeWith(Result.success(null))
                return@suspendCoroutine
            }
            
            val hmac = generateHmac(deviceId)
            
            val getKeyRequest = GetApiKeyRequest(
                deviceId = deviceId,
                hmac = hmac
            )
            
            val json = gson.toJson(getKeyRequest)
            Log.d(TAG, "Get API key request JSON: $json")
            
            val body = json.toRequestBody("application/json".toMediaType())
            
            val request = Request.Builder()
                .url("$API_URL/api/get-api-key")
                .post(body)
                .build()
            
            httpClient.newCall(request).enqueue(object : okhttp3.Callback {
                override fun onFailure(call: okhttp3.Call, e: IOException) {
                    Log.e(TAG, "Failed to fetch demo API key", e)
                    continuation.resumeWith(Result.success(null))
                }
                
                override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                    try {
                        Log.d(TAG, "Get API key response code: ${response.code}")
                        if (!response.isSuccessful) {
                            Log.e(TAG, "Failed to fetch demo API key: ${response.code}")
                            val errorBody = response.body?.string()
                            Log.e(TAG, "Error response body: $errorBody")
                            continuation.resumeWith(Result.success(null))
                            return
                        }
                        
                        val responseBody = response.body?.string()
                        Log.d(TAG, "Get API key response body: $responseBody")
                        if (responseBody.isNullOrEmpty()) {
                            Log.e(TAG, "Empty response from get API key")
                            continuation.resumeWith(Result.success(null))
                            return
                        }
                        
                        val getKeyResponse = gson.fromJson(responseBody, GetApiKeyResponse::class.java)
                        if (getKeyResponse.encryptedKey.isNullOrEmpty()) {
                            Log.e(TAG, "Invalid encrypted key in response")
                            continuation.resumeWith(Result.success(null))
                            return
                        }
                        
                        // Decrypt the key
                        val decryptedKey = decryptApiKey(getKeyResponse.encryptedKey)
                        if (decryptedKey.isNullOrEmpty()) {
                            Log.e(TAG, "Failed to decrypt API key")
                            continuation.resumeWith(Result.success(null))
                            return
                        }
                        
                        // Save the demo API key
                        sharedPreferences.edit()
                            .putString(DEMO_API_KEY, decryptedKey)
                            .apply()
                        
                        Log.d(TAG, "Demo API key fetched and decrypted successfully")
                        continuation.resumeWith(Result.success(decryptedKey))
                    } catch (e: Exception) {
                        Log.e(TAG, "Error fetching demo API key", e)
                        continuation.resumeWith(Result.success(null))
                    } finally {
                        response.close()
                    }
                }
            })
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching demo API key", e)
            continuation.resumeWith(Result.success(null))
        }
    }
    
    /**
     * Decrypts the API key using AES-256-CBC with PSK
     */
    private fun decryptApiKey(encryptedKey: String): String? {
        return try {
            // Split the encrypted key into IV and encrypted data
            val parts = encryptedKey.split(":")
            if (parts.size != 2) {
                Log.e(TAG, "Invalid encrypted key format")
                return null
            }
            
            val ivHex = parts[0]
            val encryptedDataHex = parts[1]
            
            // Convert hex strings to bytes
            val iv = hexStringToByteArray(ivHex)
            val encryptedData = hexStringToByteArray(encryptedDataHex)
            
            // Derive key from PSK using SHA-256 (use PSK directly, not decoded)
            val md = MessageDigest.getInstance("SHA-256")
            val keyBytes = md.digest(PSK.toByteArray(Charsets.UTF_8))
            
            val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
            val keySpec = SecretKeySpec(keyBytes, "AES")
            val ivSpec = IvParameterSpec(iv)
            cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec)
            
            val decryptedBytes = cipher.doFinal(encryptedData)
            String(decryptedBytes, Charsets.UTF_8)
        } catch (e: Exception) {
            Log.e(TAG, "Error decrypting API key", e)
            null
        }
    }
    
    /**
     * Converts a hex string to byte array
     */
    private fun hexStringToByteArray(hexString: String): ByteArray {
        val len = hexString.length
        val data = ByteArray(len / 2)
        for (i in 0 until len step 2) {
            data[i / 2] = ((Character.digit(hexString[i], 16) shl 4)
                    + Character.digit(hexString[i+1], 16)).toByte()
        }
        return data
    }
    
    /**
     * Checks if we can use a demo API call (under daily limit)
     */
    fun canUseDemoKey(): Boolean {
        resetUsageIfNeeded()
        val currentUsage = sharedPreferences.getInt(USAGE_COUNT, 0)
        return currentUsage < MAX_DAILY_USES
    }
    
    /**
     * Increments the demo key usage counter
     */
    fun incrementUsage() {
        resetUsageIfNeeded()
        val currentUsage = sharedPreferences.getInt(USAGE_COUNT, 0)
        sharedPreferences.edit()
            .putInt(USAGE_COUNT, currentUsage + 1)
            .apply()
    }
    
    /**
     * Resets usage counter if a day has passed
     */
    private fun resetUsageIfNeeded() {
        val lastResetTime = sharedPreferences.getLong(LAST_RESET_TIME, 0)
        val currentTime = System.currentTimeMillis()
        
        if (currentTime - lastResetTime >= MILLIS_IN_DAY) {
            sharedPreferences.edit()
                .putLong(LAST_RESET_TIME, currentTime)
                .putInt(USAGE_COUNT, 0)
                .apply()
        }
    }
    
    /**
     * Gets the current demo API key
     */
    fun getDemoApiKey(): String? {
        return sharedPreferences.getString(DEMO_API_KEY, null)
    }
    
    /**
     * Gets the device ID from shared preferences
     */
    fun getDeviceId(): String? {
        val deviceId = sharedPreferences.getString(DEVICE_ID_KEY, null)
        Log.d(TAG, "Retrieved device ID: $deviceId")
        return deviceId
    }
    
    /**
     * Clears the stored device ID for testing purposes
     */
    fun clearDeviceId() {
        Log.d(TAG, "Clearing stored device ID")
        sharedPreferences.edit()
            .remove(DEVICE_ID_KEY)
            .apply()
    }
    
    // Data classes for API requests/responses
    private data class RegisterDeviceRequest(
        @SerializedName("appSecret")
        val appSecret: String,
        @SerializedName("deviceInfo")
        val deviceInfo: Map<String, String>
    )
    
    private data class RegisterDeviceResponse(
        @SerializedName("deviceId")
        val deviceId: String?
    )
    
    private data class GetApiKeyRequest(
        @SerializedName("deviceId")
        val deviceId: String,
        @SerializedName("hmac")
        val hmac: String
    )
    
    private data class GetApiKeyResponse(
        @SerializedName("encryptedKey")
        val encryptedKey: String?
    )
}