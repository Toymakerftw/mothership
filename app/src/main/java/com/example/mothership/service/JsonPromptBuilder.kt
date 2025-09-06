package com.example.mothership.service

import org.json.JSONObject

class JsonPromptBuilder {
    
    companion object {
        private const val PWA_GENERATION_TASK = "create_pwa"
        private const val PWA_REWORK_TASK = "rework_pwa"
        private const val PROMPT_REWRITE_TASK = "rewrite_prompt"
    }
    
    /**
     * Builds a JSON prompt for PWA generation
     */
    fun buildPwaGenerationPrompt(prompt: String): String {
        val jsonObject = JSONObject()
        jsonObject.put("task", PWA_GENERATION_TASK)
        
        val pwaGenerationPrompt = """
            You are an expert UI/UX and Front-End Developer. You create web app in a way a designer would, using ONLY HTML, CSS, and Javascript. Try to create the best UI possible. Important: Make the app responsive by using TailwindCSS. Use it as much as you can, if you can't use it, use custom css (make sure to import tailwind with <script src='https://cdn.tailwindcss.com'></script> in the head). Also try to elaborate as much as you can, to create something unique, with a great design. Make sure to include a favicon link in the HTML head section with <link rel="icon" href="favicon.ico" type="image/x-icon">. If you want to use ICONS import Feather Icons (Make sure to add <script src='https://unpkg.com/feather-icons'></script> and <script src='https://cdn.jsdelivr.net/npm/feather-icons/dist/feather.min.js'></script> in the head., and <script>feather.replace();</script> in the body. Ex : <i data-feather='user'></i>). For scroll animations you can use: AOS.com (Make sure to add <link href='https://unpkg.com/aos@2.3.1/dist/aos.css' rel='stylesheet'> and <script src='https://unpkg.com/aos@2.3.1/dist/aos.js'></script> and <script>AOS.init();</script>). For interactive animations you can use: Vanta.js (Make sure to add <script src='https://cdn.jsdelivr.net/npm/vanta@latest/dist/vanta.globe.min.js'></script> and <script>VANTA.GLOBE({...</script> in the body.). You can create multiple pages web app at once (following the format rules below) or a Single Page Application. If the user doesn't ask for a specific version, you have to determine the best version for the user, depending on the request. (Try to avoid the Single Page Application if the user asks for multiple pages.) No need to explain what you did. Just return the expected result. AVOID Chinese characters in the code if not asked by the user.
        """.trimIndent()
        
        jsonObject.put("pwa_generation_prompt", pwaGenerationPrompt)
        jsonObject.put("output_format_instructions", buildPwaOutputFormat())
        jsonObject.put("user_input", buildUserInput(prompt))
        
        return jsonObject.toString(2)
    }
    
    /**
     * Builds a JSON prompt for PWA rework
     */
    fun buildPwaReworkPrompt(prompt: String): String {
        val jsonObject = JSONObject()
        jsonObject.put("task", PWA_REWORK_TASK)
        
        val reworkPrompt = """
            You are an expert UI/UX and Front-End Developer. Your task is to modify an existing Progressive Web App (PWA) based on the user's request. You will be provided with the user's request and the content of the existing PWA files. Make sure to include a favicon link in the HTML head section with <link rel="icon" href="favicon.ico" type="image/x-icon">.
        """.trimIndent()
        
        jsonObject.put("rework_prompt", reworkPrompt)
        jsonObject.put("output_format_instructions", buildPwaOutputFormat())
        jsonObject.put("user_input", buildUserInput(prompt))
        
        return jsonObject.toString(2)
    }
    
    /**
     * Builds a JSON prompt for prompt rewriting
     */
    fun buildPromptRewritePrompt(): String {
        val jsonObject = JSONObject()
        jsonObject.put("task", PROMPT_REWRITE_TASK)
        
        val instructions = JSONObject()
        instructions.put("role", "You are a helpful assistant that rewrites prompts to make them better. All the prompts will be about creating an app.")
        instructions.put("goal", "Try to make the prompt more detailed and specific to create a good UI/UX Design and good code.")
        
        val outputFormat = JSONObject()
        outputFormat.put("format", "text")
        outputFormat.put("start_marker", ">>>>>>> START PROMPT >>>>>>")
        outputFormat.put("end_marker", ">>>>>>> END PROMPT >>>>>>")
        outputFormat.put("language", "same as input")
        outputFormat.put("intent", "keep original intent, improve if needed")
        
        instructions.put("output_format", outputFormat)
        
        val rules = JSONObject()
        rules.put("if_no_rewrite", "return original prompt")
        instructions.put("rules", rules)
        
        jsonObject.put("instructions", instructions)
        
        return jsonObject.toString(2)
    }
    
    /**
     * Builds the output format instructions for PWA files
     */
    private fun buildPwaOutputFormat(): JSONObject {
        val outputFormat = JSONObject()
        outputFormat.put("type", "json")
        
        val schema = JSONObject()
        val files = JSONObject()
        val properties = JSONObject()
        
        // Define each file property
        val indexHtml = JSONObject()
        indexHtml.put("type", "string")
        indexHtml.put("description", "Complete HTML file with proper structure. It must reference manifest.json, app.js, and styles.css. It must also register sw.js as a service worker.")
        properties.put("index.html", indexHtml)
        
        val manifestJson = JSONObject()
        manifestJson.put("type", "string")
        manifestJson.put("description", "Valid PWA manifest file with proper start_url.")
        properties.put("manifest.json", manifestJson)
        
        val swJs = JSONObject()
        swJs.put("type", "string")
        swJs.put("description", "Basic but functional service worker.")
        properties.put("sw.js", swJs)
        
        val appJs = JSONObject()
        appJs.put("type", "string")
        appJs.put("description", "JavaScript file for functionality.")
        properties.put("app.js", appJs)
        
        val stylesCss = JSONObject()
        stylesCss.put("type", "string")
        stylesCss.put("description", "CSS styling file.")
        properties.put("styles.css", stylesCss)
        
        files.put("type", "object")
        files.put("properties", properties)
        
        val required = listOf("index.html", "manifest.json", "sw.js", "app.js", "styles.css")
        files.put("required", required)
        
        schema.put("files", files)
        outputFormat.put("schema", schema)
        
        // Add example
        val example = JSONObject()
        val exampleFiles = JSONObject()
        exampleFiles.put("index.html", "<!DOCTYPE html>...")
        exampleFiles.put("manifest.json", "{...}")
        exampleFiles.put("sw.js", "...")
        exampleFiles.put("app.js", "...")
        exampleFiles.put("styles.css", "...")
        example.put("files", exampleFiles)
        outputFormat.put("example", example)
        
        return outputFormat
    }
    
    /**
     * Builds the user input object
     */
    private fun buildUserInput(prompt: String): JSONObject {
        val userInput = JSONObject()
        userInput.put("prompt", prompt)
        return userInput
    }
}
