package com.toymakerftw.appsage.widget

import android.content.Context
import com.google.gson.Gson
import java.io.File
import java.util.UUID

class WidgetManager(private val context: Context) {

    data class StoredWidget(
        val uuid: String,
        val title: String,
        val prompt: String,
        val layoutJson: String,
        val creationDate: Long = System.currentTimeMillis()
    )

    private val widgetsDir by lazy {
        File(context.getExternalFilesDir(null), "widgets").apply {
            if (!exists()) mkdirs()
        }
    }

    private val gson = Gson()

    fun getGeneratedWidgets(): List<StoredWidget> {
        return widgetsDir.listFiles()?.mapNotNull { file ->
            if (file.extension == "json") {
                try {
                    gson.fromJson(file.readText(), StoredWidget::class.java)
                } catch (e: Exception) {
                    null
                }
            } else null
        }?.sortedByDescending { it.creationDate } ?: emptyList()
    }

    fun saveWidget(prompt: String, layoutJson: String, title: String? = null): StoredWidget {
        val uuid = UUID.randomUUID().toString()
        val finalTitle = title ?: prompt.take(30).let { if (it.length == 30) "$it..." else it }
        val storedWidget = StoredWidget(uuid, finalTitle, prompt, layoutJson)
        val file = File(widgetsDir, "$uuid.json")
        file.writeText(gson.toJson(storedWidget))
        return storedWidget
    }

    fun deleteWidget(uuid: String): Boolean {
        val file = File(widgetsDir, "$uuid.json")
        return if (file.exists()) file.delete() else true
    }
}
