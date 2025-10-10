package com.toymakerftw.appsage.data

import android.content.Context
import android.util.Log
import com.toymakerftw.appsage.service.PwaManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File

class PwaRepository(private val context: Context) {
    
    private val pwaManager = PwaManager(context)
    
    suspend fun getGeneratedPwas(): List<Pair<String, String>> {
        return withContext(Dispatchers.IO) {
            val pwaDir = context.getExternalFilesDir(null)
            if (pwaDir != null && pwaDir.exists()) {
                pwaDir.listFiles()?.mapNotNull { dir ->
                    if (dir.isDirectory) {
                        val appInfoFile = File(dir, "app_info.json")
                        val manifestFile = File(dir, "manifest.json")
                        
                        if (appInfoFile.exists() || manifestFile.exists()) {
                            try {
                                var pwaName = "Untitled App"
                                
                                if (manifestFile.exists()) {
                                    try {
                                        val manifestContent = manifestFile.readText()
                                        val manifestJson = JSONObject(manifestContent)
                                        
                                        val shortName = manifestJson.optString("short_name")
                                        val manifestName = manifestJson.optString("name")
                                        
                                        val betterName = shortName.ifEmpty { manifestName }
                                        if (betterName.isNotEmpty()) {
                                            pwaName = betterName
                                        }
                                    } catch (manifestException: Exception) {
                                        Log.w("PwaRepository", "Could not parse manifest.json for ${dir.name}", manifestException)
                                    }
                                }
                                
                                if (pwaName == "Untitled App" && appInfoFile.exists()) {
                                    val appInfo = appInfoFile.readText()
                                    try {
                                        val jsonObject = JSONObject(appInfo)
                                        val appInfoName = jsonObject.optString("name", "Untitled App")
                                        if (appInfoName != "Untitled App") {
                                            pwaName = appInfoName
                                        }
                                    } catch (e: Exception) {
                                        Log.w("PwaRepository", "Could not parse app_info.json for ${dir.name}", e)
                                    }
                                }
                                
                                dir.name to pwaName
                            } catch (e: Exception) {
                                Log.w("PwaRepository", "Error getting PWA name for ${dir.name}", e)
                                dir.name to "Untitled App (${dir.name})"
                            }
                        } else {
                            null
                        }
                    } else {
                        null
                    }
                } ?: emptyList()
            } else {
                emptyList()
            }
        }
    }
    
    suspend fun deletePwa(uuid: String): Boolean {
        return withContext(Dispatchers.IO) {
            pwaManager.deletePwa(uuid)
        }
    }
}