package com.toymakerftw.appsage.versioncontrol

import android.content.Context
import android.util.Log
import com.toymakerftw.appsage.service.PwaManager
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*

data class VersionInfo(
    val versionId: String,
    val timestamp: Long,
    val commitMessage: String = "",
    val fileCount: Int = 0
)

data class VersionHistory(
    val versions: List<VersionInfo> = emptyList()
)

class VersionControl(private val context: Context) {
    private val dateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss").withZone(ZoneId.systemDefault())

    fun createBackup(uuid: String, commitMessage: String = "Auto-backup before rework"): Boolean {
        val pwaDir = File(context.getExternalFilesDir(null), uuid)
        if (!pwaDir.exists() || !pwaDir.isDirectory) {
            Log.e("VersionControl", "PWA directory not found: $uuid")
            return false
        }

        val timestamp = System.currentTimeMillis()
        val versionId = dateFormat.format(Instant.ofEpochMilli(timestamp))
        val backupDir = File(context.getExternalFilesDir(null), "$uuid/versions/$versionId")
        
        if (!backupDir.exists()) {
            backupDir.mkdirs()
        }

        // Copy all relevant files to backup directory
        val filesToBackup = listOf("index.html", "style.css", "script.js", "manifest.json", "app_info.json")
        var copiedFiles = 0

        for (fileName in filesToBackup) {
            val sourceFile = File(pwaDir, fileName)
            if (sourceFile.exists()) {
                val destFile = File(backupDir, fileName)
                try {
                    sourceFile.copyTo(destFile, overwrite = true)
                    copiedFiles++
                } catch (e: Exception) {
                    Log.e("VersionControl", "Failed to backup file: $fileName", e)
                }
            }
        }

        // Create a metadata file with commit message and timestamp
        val metadataFile = File(backupDir, ".metadata.json")
        val metadata = """{
            "versionId": "$versionId",
            "timestamp": $timestamp,
            "commitMessage": "$commitMessage",
            "fileCount": $copiedFiles
        }"""
        metadataFile.writeText(metadata)

        Log.d("VersionControl", "Created backup with $copiedFiles files for version: $versionId")
        return true
    }

    fun getHistory(uuid: String): VersionHistory {
        val versionsDir = File(context.getExternalFilesDir(null), "$uuid/versions")
        if (!versionsDir.exists() || !versionsDir.isDirectory) {
            return VersionHistory(emptyList())
        }

        val versions = versionsDir.listFiles { file -> 
            file.isDirectory && File(file, ".metadata.json").exists()
        }?.mapNotNull { versionDir ->
            try {
                val metadataFile = File(versionDir, ".metadata.json")
                val metadataJson = org.json.JSONObject(metadataFile.readText())
                
                VersionInfo(
                    versionId = metadataJson.getString("versionId"),
                    timestamp = metadataJson.getLong("timestamp"),
                    commitMessage = metadataJson.optString("commitMessage", ""),
                    fileCount = metadataJson.optInt("fileCount", 0)
                )
            } catch (e: Exception) {
                Log.e("VersionControl", "Failed to parse version metadata", e)
                null
            }
        }?.sortedByDescending { it.timestamp } // Sort by newest first

        return VersionHistory(versions ?: emptyList())
    }

    fun revertToVersion(uuid: String, versionId: String): Boolean {
        val pwaDir = File(context.getExternalFilesDir(null), uuid)
        val versionsDir = File(pwaDir, "versions")
        val versionDir = File(versionsDir, versionId)

        if (!versionDir.exists() || !versionDir.isDirectory) {
            Log.e("VersionControl", "Version directory not found: $versionId")
            return false
        }

        // Copy files from backup to main PWA directory
        val filesToRestore = listOf("index.html", "style.css", "script.js", "manifest.json", "app_info.json")
        var restoredFiles = 0

        for (fileName in filesToRestore) {
            val sourceFile = File(versionDir, fileName)
            if (sourceFile.exists()) {
                val destFile = File(pwaDir, fileName)
                try {
                    sourceFile.copyTo(destFile, overwrite = true)
                    restoredFiles++
                } catch (e: Exception) {
                    Log.e("VersionControl", "Failed to restore file: $fileName", e)
                }
            }
        }

        Log.d("VersionControl", "Reverted to version: $versionId, restored $restoredFiles files")
        return restoredFiles > 0
    }

    fun deleteVersion(uuid: String, versionId: String): Boolean {
        val versionsDir = File(context.getExternalFilesDir(null), "$uuid/versions")
        val versionDir = File(versionsDir, versionId)

        if (!versionDir.exists()) {
            return false
        }

        return versionDir.deleteRecursively()
    }

    fun getMaxVersions(uuid: String): Int {
        val versionsDir = File(context.getExternalFilesDir(null), "$uuid/versions")
        if (!versionsDir.exists() || !versionsDir.isDirectory) {
            return 0
        }

        return versionsDir.listFiles { file -> 
            file.isDirectory && File(file, ".metadata.json").exists()
        }?.size ?: 0
    }

    fun clearOldVersions(uuid: String, maxVersions: Int = 10): Boolean {
        val versions = getHistory(uuid).versions
        if (versions.size <= maxVersions) {
            return true
        }

        // Keep only the most recent versions
        val versionsToDelete = versions.drop(maxVersions)
        var deletedCount = 0

        for (versionInfo in versionsToDelete) {
            if (deleteVersion(uuid, versionInfo.versionId)) {
                deletedCount++
            }
        }

        Log.d("VersionControl", "Cleared $deletedCount old versions for PWA: $uuid")
        return true
    }
}