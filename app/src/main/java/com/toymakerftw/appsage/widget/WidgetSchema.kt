package com.toymakerftw.appsage.widget

import androidx.annotation.Keep

@Keep
data class WidgetLayout(
    val root: WidgetNode?,
    val themeColor: String? = null,
    val backgroundColor: String? = null
)

@Keep
data class WidgetNode(
    val type: String, // "column", "row", "text", "button", "spacer"
    val text: String? = null,
    val color: String? = null,
    val backgroundColor: String? = null,
    val fontSize: Int? = null,
    val fontWeight: String? = null, // "bold", "normal"
    val align: String? = null, // "start", "center", "end"
    val cornerRadius: Int? = null,
    val weight: Float? = null,
    val fillMaxWidth: Boolean? = null,
    val fillMaxHeight: Boolean? = null,
    val padding: Int? = null,
    val actionPrompt: String? = null, // Used for buttons
    val children: List<WidgetNode>? = null
)
