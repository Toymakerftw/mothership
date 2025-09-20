package com.toymakerftw.mothership.data

import android.content.Context
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import java.util.zip.ZipInputStream

class VersionManager(private val context: Context) {
    
    companion object {
        private const val VERSIONS_DIR = "versions"
        private const val MAX_VERSIONS = 5 // Keep only the last 5 versions to save space
    }
    
    /**
     * Creates a backup of the current app state before reworking
     */
    fun createVersionBackup(uuid: String, versionName: String = "Before Rework"): Boolean {
        return try {
            val pwaDir = File(context.getExternalFilesDir(null), uuid)
            if (!pwaDir.exists() || !pwaDir.isDirectory) {
                return false
            }
            
            val versionsDir = File(context.getExternalFilesDir(null), "$VERSIONS_DIR/$uuid")
            if (!versionsDir.exists()) {
                versionsDir.mkdirs()
            }
            
            // Clean up old versions if we have too many
            cleanupOldVersions(versionsDir)
            
            // Create version file name with timestamp
            val timestamp = System.currentTimeMillis()
            val versionFileName = "${versionName.replace(" ", "_")}_$timestamp.zip"
            val versionFile = File(versionsDir, versionFileName)
            
            // Create zip of current app state
            createZipFromDirectory(pwaDir, versionFile)
            
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    /**
     * Reverts the app to a specific version
     */
    fun revertToVersion(uuid: String, versionFileName: String): Boolean {
        return try {
            val versionsDir = File(context.getExternalFilesDir(null), "$VERSIONS_DIR/$uuid")
            val versionFile = File(versionsDir, versionFileName)
            
            if (!versionFile.exists()) {
                return false
            }
            
            val pwaDir = File(context.getExternalFilesDir(null), uuid)
            
            // Delete current app files
            pwaDir.deleteRecursively()
            
            // Extract version files
            extractZipToDirectory(versionFile, pwaDir)
            
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    /**
     * Gets a list of available versions for an app
     */
    fun getVersions(uuid: String): List<VersionInfo> {
        return try {
            val versionsDir = File(context.getExternalFilesDir(null), "$VERSIONS_DIR/$uuid")
            if (!versionsDir.exists()) {
                return emptyList()
            }
            
            versionsDir.listFiles()
                ?.filter { it.extension == "zip" }
                ?.map { file ->
                    val parts = file.nameWithoutExtension.split("_")
                    val name = if (parts.size >= 2) {
                        parts.dropLast(1).joinToString(" ")
                    } else {
                        "Version"
                    }
                    val timestamp = parts.lastOrNull()?.toLongOrNull() ?: 0L
                    
                    VersionInfo(
                        fileName = file.name,
                        name = name,
                        timestamp = timestamp,
                        size = file.length()
                    )
                }
                ?.sortedByDescending { it.timestamp }
                ?: emptyList()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
    
    /**
     * Deletes a specific version
     */
    fun deleteVersion(uuid: String, versionFileName: String): Boolean {
        return try {
            val versionsDir = File(context.getExternalFilesDir(null), "$VERSIONS_DIR/$uuid")
            val versionFile = File(versionsDir, versionFileName)
            versionFile.delete()
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    private fun cleanupOldVersions(versionsDir: File) {
        try {
            val versionFiles = versionsDir.listFiles()
                ?.filter { it.extension == "zip" }
                ?.sortedByDescending { file ->
                    file.nameWithoutExtension.split("_").lastOrNull()?.toLongOrNull() ?: 0L
                }
            
            // Delete oldest versions if we have more than MAX_VERSIONS
            if (versionFiles != null && versionFiles.size > MAX_VERSIONS) {
                versionFiles.drop(MAX_VERSIONS).forEach { file ->
                    file.delete()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    private fun createZipFromDirectory(sourceDir: File, zipFile: File) {
        ZipOutputStream(FileOutputStream(zipFile)).use { zos ->
            sourceDir.walkTopDown().forEach { file ->
                val zipFileName = "${sourceDir.name}/${file.toRelativeString(sourceDir)}"
                if (file.isDirectory) {
                    zos.putNextEntry(ZipEntry("$zipFileName/"))
                    zos.closeEntry()
                } else {
                    FileInputStream(file).use { fis ->
                        zos.putNextEntry(ZipEntry(zipFileName))
                        fis.copyTo(zos)
                        zos.closeEntry()
                    }
                }
            }
        }
    }
    
    private fun extractZipToDirectory(zipFile: File, targetDir: File) {
        ZipInputStream(FileInputStream(zipFile)).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                val fileName = entry.name
                // Skip the first directory level (app UUID)
                val relativePath = fileName.substringAfter("/", fileName)
                if (relativePath.isNotEmpty()) {
                    val newFile = File(targetDir, relativePath)
                    if (entry.isDirectory) {
                        newFile.mkdirs()
                    } else {
                        newFile.parentFile?.mkdirs()
                        FileOutputStream(newFile).use { fos ->
                            zis.copyTo(fos)
                        }
                    }
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }
    }
}

data class VersionInfo(
    val fileName: String,
    val name: String,
    val timestamp: Long,
    val size: Long
) {
    val formattedTimestamp: String
        get() = java.text.SimpleDateFormat("MMM dd, yyyy HH:mm", java.util.Locale.getDefault())
            .format(java.util.Date(timestamp))
            
    val formattedSize: String
        get() = when {
            size < 1024 -> "$size B"
            size < 1024 * 1024 -> "${String.format("%.1f", size / 1024.0)} KB"
            else -> "${String.format("%.1f", size / (1024.0 * 1024.0))} MB"
        }
}