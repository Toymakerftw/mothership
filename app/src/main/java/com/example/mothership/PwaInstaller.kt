package com.example.mothership

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Build
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import org.json.JSONObject
import java.io.File

class PwaInstaller(private val context: Context) {

    fun install(uuid: String) {
        val pwaDir = File(context.getExternalFilesDir(null), uuid)
        if (!pwaDir.exists()) return

        val appInfoFile = File(pwaDir, "app_info.json")
        val manifestFile = File(pwaDir, "manifest.json")
        
        if (!appInfoFile.exists() || !manifestFile.exists()) return

        try {
            // Read app info
            val appInfo = appInfoFile.readText()
            val appInfoJson = JSONObject(appInfo)
            val appName = appInfoJson.optString("name", "PWA App")
            
            // Read manifest for additional info
            val manifest = manifestFile.readText()
            val manifestJson = JSONObject(manifest)
            val shortName = manifestJson.optString("short_name", appName)
            
            // Try to get start URL from manifest, fallback to index.html
            val startUrl = manifestJson.optString("start_url", "index.html")
            
            // Ensure start URL is properly formatted
            val normalizedStartUrl = if (startUrl.startsWith("/")) {
                startUrl.substring(1)
            } else {
                startUrl
            }
            
            // Check if the start file exists, fallback to index.html if not
            val startFile = File(pwaDir, normalizedStartUrl)
            val actualStartUrl = if (startFile.exists()) {
                normalizedStartUrl
            } else {
                "index.html"
            }
            
            // Create intent for the shortcut
            val intent = Intent(context, PwaViewerActivity::class.java).apply {
                action = Intent.ACTION_VIEW
                putExtra("pwaUrl", "file://${pwaDir.absolutePath}/$actualStartUrl")
                putExtra("pwaName", appName)
                putExtra("pwaId", uuid)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            
            // Generate or extract icon
            val icon = generateAppIcon(appName)
            
            // Create shortcut
            val shortcut = ShortcutInfoCompat.Builder(context, uuid)
                .setShortLabel(shortName)
                .setLongLabel(appName)
                .setIcon(icon)
                .setIntent(intent)
                .build()
            
            // Add shortcut to launcher
            ShortcutManagerCompat.pushDynamicShortcut(context, shortcut)
            ShortcutManagerCompat.requestPinShortcut(context, shortcut, null)
            
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    fun uninstall(uuid: String) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                ShortcutManagerCompat.disableShortcuts(
                    context,
                    listOf(uuid),
                    "Shortcut disabled"
                )
            }
            // Remove shortcut from launcher
            ShortcutManagerCompat.removeDynamicShortcuts(context, listOf(uuid))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    private fun generateAppIcon(appName: String): IconCompat {
        // Create a simple colored icon with the first letter of the app name
        val bitmap = Bitmap.createBitmap(256, 256, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint()
        
        // Use a color based on the app name
        val color = getColorFromString(appName)
        paint.color = color
        paint.isAntiAlias = true
        canvas.drawPaint(paint)
        
        // Draw the first letter
        paint.color = Color.WHITE
        paint.textSize = 128f
        paint.textAlign = Paint.Align.CENTER
        val xPos = canvas.width / 2f
        val yPos = (canvas.height / 2 - (paint.descent() + paint.ascent()) / 2)
        canvas.drawText(if (appName.isNotEmpty()) appName[0].toString() else "A", xPos, yPos, paint)
        
        return IconCompat.createWithBitmap(bitmap)
    }
    
    private fun getColorFromString(str: String): Int {
        val hash = str.hashCode()
        val r = (hash and 0xFF0000) shr 16
        val g = (hash and 0x00FF00) shr 8
        val b = hash and 0x0000FF
        return Color.rgb(r, g, b)
    }
}