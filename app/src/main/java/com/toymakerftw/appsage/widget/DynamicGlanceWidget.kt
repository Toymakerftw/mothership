package com.toymakerftw.appsage.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.Button
import androidx.glance.ButtonDefaults
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.appwidget.GlanceAppWidget
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
import androidx.glance.layout.padding
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
        val KEY_PROMPT = stringPreferencesKey("prompt")
    }

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val prefs = currentState<Preferences>()
            val layoutJson = prefs[KEY_LAYOUT_JSON]
            
            GlanceTheme {
                if (layoutJson.isNullOrEmpty()) {
                    Column(
                        modifier = GlanceModifier.fillMaxSize().background(Color.DarkGray).padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Awaiting AI Generation...",
                            style = TextStyle(color = ColorProvider(Color.White))
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
                        Column(modifier = GlanceModifier.fillMaxSize().background(bgColor)) {
                            parsedLayout.root?.let { RenderNode(it) }
                        }
                    } else {
                        Text(
                            text = "Error rendering widget layout",
                            style = TextStyle(color = ColorProvider(Color.Red))
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun RenderNode(node: WidgetNode) {
        var modifier = GlanceModifier.padding((node.padding ?: 0).dp)
        
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
        
        // Define standard alignments based on JSON property
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
                        RenderNode(child)
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
                        RenderNode(child)
                    }
                }
            }
            "text" -> {
                val weight = if (node.fontWeight == "bold") FontWeight.Bold else FontWeight.Normal
                val textColor = parseColor(node.color) ?: Color.Black
                Text(
                    text = node.text ?: "",
                    modifier = modifier,
                    style = TextStyle(
                        color = ColorProvider(textColor),
                        fontSize = (node.fontSize ?: 14).sp,
                        fontWeight = weight
                    )
                )
            }
            "button" -> {
                val buttonColor = parseColor(node.backgroundColor) ?: Color.DarkGray
                val textColor = parseColor(node.color) ?: Color.White
                
                val promptParamKey = ActionParameters.Key<String>("actionPrompt")
                val params = actionParametersOf(promptParamKey to (node.actionPrompt ?: ""))
                
                Button(
                    text = node.text ?: "Button",
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

    private fun parseColor(colorString: String?): Color? {
        if (colorString == null) return null
        return try {
            Color(android.graphics.Color.parseColor(colorString))
        } catch (e: Exception) {
            null
        }
    }
}
