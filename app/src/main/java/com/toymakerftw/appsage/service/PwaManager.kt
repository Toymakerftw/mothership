package com.toymakerftw.appsage.service

import android.content.Context
import android.content.Intent
import com.toymakerftw.appsage.PwaInstaller
import java.io.File

class PwaManager(private val context: Context) {
    
    data class PwaInfo(
        val uuid: String,
        val name: String,
        val description: String = "",
        val creationDate: Long = System.currentTimeMillis()
    )
    
    fun getGeneratedPwas(): List<PwaInfo> {
        val pwasDir = context.getExternalFilesDir(null)
        if (pwasDir == null || !pwasDir.exists()) {
            return emptyList()
        }
        
        return pwasDir.listFiles { file -> 
            file.isDirectory && 
            File(file, "app_info.json").exists() &&
            File(file, "index.html").exists()
        }?.map { dir ->
            val appInfoFile = File(dir, "app_info.json")
            var name = "PWA (${dir.name})"
            var description = ""
            
            if (appInfoFile.exists()) {
                try {
                    val appInfo = appInfoFile.readText()
                    val json = org.json.JSONObject(appInfo)
                    name = json.optString("name", "PWA (${dir.name})")
                    description = json.optString("description", "")
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            
            PwaInfo(
                uuid = dir.name,
                name = name,
                description = description,
                creationDate = dir.lastModified()
            )
        } ?: emptyList()
    }
    
    fun deletePwa(uuid: String) {
        val pwaDir = File(context.getExternalFilesDir(null), uuid)
        if (pwaDir.exists()) {
            // Uninstall the shortcut first
            val installer = PwaInstaller(context)
            installer.uninstall(uuid)
            
            // Then delete the directory and all its contents
            pwaDir.deleteRecursively()
        }
    }
    
    fun startPwaServer(uuid: String, port: Int = 8080): Boolean {
        val intent = Intent(context, PwaHttpServerService::class.java).apply {
            action = PwaHttpServerService.ACTION_START_SERVER
            putExtra(PwaHttpServerService.EXTRA_PWA_UUID, uuid)
            putExtra(PwaHttpServerService.EXTRA_PORT, port)
        }
        return try {
            context.startService(intent)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    fun stopPwaServer(port: Int = 8080): Boolean {
        val intent = Intent(context, PwaHttpServerService::class.java).apply {
            action = PwaHttpServerService.ACTION_STOP_SERVER
            putExtra(PwaHttpServerService.EXTRA_PORT, port)
        }
        return try {
            context.startService(intent)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}