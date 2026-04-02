package com.toymakerftw.appsage.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.Button
import androidx.glance.ButtonDefaults
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalSize
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.google.gson.Gson

class DynamicGlanceWidget : GlanceAppWidget() {
    override val stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition

    companion object {
        val KEY_LAYOUT_JSON = stringPreferencesKey("layout_json")
        val KEY_STATE_JSON = stringPreferencesKey("state_json")
        val KEY_PROMPT = stringPreferencesKey("prompt")

        // Responsive breakpoints
        private val SMALL = DpSize(57.dp, 57.dp)
        private val MEDIUM = DpSize(130.dp, 130.dp)
        private val LARGE = DpSize(250.dp, 130.dp)
        private val EXTRA_LARGE = DpSize(250.dp, 250.dp)
    }

    override val sizeMode = SizeMode.Responsive(
        setOf(SMALL, MEDIUM, LARGE, EXTRA_LARGE)
    )

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val prefs = currentState<Preferences>()
            val layoutJson = prefs[KEY_LAYOUT_JSON]
            val stateJson = prefs[KEY_STATE_JSON] ?: "{}"
            val size = LocalSize.current
            
            val state = try {
                Gson().fromJson(stateJson, Map::class.java) as Map<String, Any>
            } catch (e: Exception) {
                emptyMap<String, Any>()
            }

            GlanceTheme {
                if (layoutJson.isNullOrEmpty()) {
                    Column(
                        modifier = GlanceModifier.fillMaxSize().background(Color.DarkGray).padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (size.width < 130.dp) "..." else "Awaiting AI...",
                            style = TextStyle(
                                color = ColorProvider(Color.White),
                                fontSize = if (size.width < 130.dp) 10.sp else 14.sp
                            )
                        )
                    }
                } else {
                    val parsedLayout = try {
                        Gson().fromJson(layoutJson, WidgetLayout::class.java)
                    } catch (e: Exception) {
                        null
                    }

                    if (parsedLayout != null) {
                        val bgColor = parseColor(parsedLayout.backgroundColor) ?: Color.White
                        val scaleFactor = when {
                            size.width < 130.dp -> 0.6f
                            size.width < 250.dp -> 0.85f
                            else -> 1.0f
                        }
                        Column(
                            modifier = GlanceModifier
                                .fillMaxSize()
                                .background(bgColor)
                                .cornerRadius(16.dp)
                        ) {
                            parsedLayout.root?.let { RenderNode(it, state, scaleFactor) }
                        }
                    } else {
                        Text(
                            text = "Error rendering",
                            style = TextStyle(color = ColorProvider(Color.Red), fontSize = 12.sp)
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun RenderNode(node: WidgetNode, state: Map<String, Any>, scaleFactor: Float = 1.0f) {
        // Conditional Visibility Check
        if (!evaluateVisibility(node.visibleIf, state)) return

        val scaledPadding = ((node.padding ?: 0) * scaleFactor).toInt()
        var modifier = GlanceModifier.padding(scaledPadding.dp)
        
        if (node.fillMaxWidth == true) modifier = modifier.fillMaxWidth()
        if (node.fillMaxHeight == true) modifier = modifier.fillMaxHeight()
        
        node.backgroundColor?.let { bgColor ->
            parseColor(bgColor)?.let {
                modifier = modifier.background(it)
            }
        }
        
        node.cornerRadius?.let {
            modifier = modifier.cornerRadius(it.dp)
        }
        
        val hAlign = when(node.align) {
            "center" -> Alignment.CenterHorizontally
            "end" -> Alignment.End
            else -> Alignment.Start
        }
        val vAlign = when(node.align) {
            "center" -> Alignment.CenterVertically
            "end" -> Alignment.Bottom
            else -> Alignment.Top
        }

        when (node.type.lowercase()) {
            "column" -> {
                Column(
                    modifier = modifier,
                    horizontalAlignment = hAlign,
                    verticalAlignment = Alignment.Top
                ) {
                    node.children?.forEach { child ->
                        RenderNode(child, state, scaleFactor)
                    }
                }
            }
            "row" -> {
                Row(
                    modifier = modifier,
                    verticalAlignment = vAlign,
                    horizontalAlignment = Alignment.Start
                ) {
                    node.children?.forEach { child ->
                        RenderNode(child, state, scaleFactor)
                    }
                }
            }
            "text" -> {
                val weight = if (node.fontWeight == "bold") FontWeight.Bold else FontWeight.Normal
                val textColor = parseColor(node.color) ?: Color.Black
                val scaledFontSize = ((node.fontSize ?: 14) * scaleFactor).toInt().coerceAtLeast(8)
                val processedText = substituteState(node.text ?: "", state)
                Text(
                    text = processedText,
                    modifier = modifier,
                    style = TextStyle(
                        color = ColorProvider(textColor),
                        fontSize = scaledFontSize.sp,
                        fontWeight = weight
                    ),
                    maxLines = if (scaleFactor < 0.7f) 1 else 10
                )
            }
            "button" -> {
                val buttonColor = parseColor(node.backgroundColor) ?: Color.DarkGray
                val textColor = parseColor(node.color) ?: Color.White
                
                val promptParamKey = ActionParameters.Key<String>("actionPrompt")
                val params = actionParametersOf(promptParamKey to (node.actionPrompt ?: ""))
                
                val processedText = substituteState(node.text ?: "Button", state)
                Button(
                    text = processedText,
                    onClick = actionRunCallback<ActionReceiver>(parameters = params),
                    modifier = modifier,
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = ColorProvider(buttonColor),
                        contentColor = ColorProvider(textColor)
                    )
                )
            }
            "spacer" -> {
                Spacer(modifier = modifier)
            }
        }
    }

    private fun substituteState(text: String, state: Map<String, Any>): String {
        var result = text
        state.forEach { (key, value) ->
            result = result.replace("%$key%", value.toString())
        }
        return result
    }

    private fun evaluateVisibility(condition: String?, state: Map<String, Any>): Boolean {
        if (condition == null) return true
        
        return try {
            val trimmed = condition.trim()
            if (trimmed.startsWith("!")) {
                val key = trimmed.substring(1).replace("%", "")
                val value = state[key]
                value == null || value == false || value == 0 || value == "" || value == "false"
            } else if (trimmed.contains("==")) {
                val parts = trimmed.split("==")
                val key = parts[0].trim().replace("%", "")
                val target = parts[1].trim().replace("\"", "").replace("'", "")
                state[key]?.toString() == target
            } else if (trimmed.contains("!=")) {
                val parts = trimmed.split("!=")
                val key = parts[0].trim().replace("%", "")
                val target = parts[1].trim().replace("\"", "").replace("'", "")
                state[key]?.toString() != target
            } else {
                val key = trimmed.replace("%", "")
                val value = state[key]
                value == true || value == "true" || (value is Number && value.toInt() > 0) || (value is String && value.isNotEmpty() && value != "false" && value != "0")
            }
        } catch (e: Exception) {
            true // Default to visible on error
        }
    }

    private fun parseColor(colorString: String?): Color? {
        if (colorString == null) return null
        return try {
            Color(android.graphics.Color.parseColor(colorString))
        } catch (e: Exception) {
            null
        }
    }
}
