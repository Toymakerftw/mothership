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
            You are an expert UI/UX and Front-End Developer specializing in Progressive Web Apps (PWAs). Your task is to create a fully functional, installable PWA using ONLY HTML, CSS, and JavaScript. No server-side code, frameworks like React/Vue/Angular, or external backends—everything must run client-side. Focus on delivering an exceptional, responsive UI/UX that feels native on mobile and desktop.

            Key PWA Requirements:
            - Make the app fully responsive using TailwindCSS (import via CDN: <script src='https://cdn.tailwindcss.com'></script> in the <head>). Use Tailwind classes extensively for layout, styling, and responsiveness (e.g., sm:, md:, lg: breakpoints). Fall back to custom CSS only if Tailwind can't handle it.
            - Ensure offline functionality: Implement a service worker (sw.js) that caches all essential assets (HTML, CSS, JS, images) for offline access. Handle network errors gracefully with fallback messages.
            - App installability: The manifest.json must include name, short_name, description, start_url (set to '/'), display: 'standalone', theme_color, background_color, and icons array (use placeholders like 'icon-192.png' and 'icon-512.png' if no real icons are generated; assume they exist or provide base64 if possible).
            - Include a favicon in HTML: <link rel="icon" href="favicon.ico" type="image/x-icon">.
            - Structure: Generate a complete set of files—index.html (entry point with proper doctype, meta viewport for responsiveness, links to manifest and sw registration), manifest.json, sw.js (with install, activate, fetch events for caching), app.js (core functionality, PWA registration via if('serviceWorker' in navigator)), styles.css (if custom CSS needed beyond Tailwind).
            - Functionality: The app must be interactive and fully working based on the user prompt. Use vanilla JS for all logic (e.g., DOM manipulation, localStorage for data persistence, event listeners). If the prompt suggests multiple pages, create a multi-page app with navigation (e.g., links between HTML files); otherwise, prefer a Single Page Application (SPA) with client-side routing if it fits better. Determine the best approach based on the request—avoid SPA if multi-page is explicitly or implicitly needed.
            - Enhancements: Elaborate creatively for unique, polished design. For icons, import Feather Icons (add <script src='https://unpkg.com/feather-icons'></script> and <script src='https://cdn.jsdelivr.net/npm/feather-icons/dist/feather.min.js'></script> in <head>, then <script>feather.replace();</script> in <body>; use like <i data-feather='user'></i>). For scroll animations, use AOS (add <link href='https://unpkg.com/aos@2.3.1/dist/aos.css' rel='stylesheet'> and <script src='https://unpkg.com/aos@2.3.1/dist/aos.js'></script> in <head>, then <script>AOS.init();</script> in <body>). For background/interactive animations, use Vanta.js (e.g., <script src='https://cdn.jsdelivr.net/npm/vanta@latest/dist/vanta.globe.min.js'></script> and initialize in <body>).
            - Best Practices: Ensure accessibility (ARIA attributes, semantic HTML), performance (lazy loading if applicable), security (HTTPS assumed), and PWA compliance (lighthouse score 100 ideal). Avoid any Chinese characters unless specified. No explanations—just output the files in the specified JSON format.
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
            You are an expert UI/UX and Front-End Developer specializing in Progressive Web Apps (PWAs). Your task is to rework and improve an existing PWA based on the user's specific request. You will receive the user's request and the full content of the current PWA files (index.html, manifest.json, sw.js, app.js, styles.css). Output only the updated versions of these files, ensuring the PWA remains fully functional, installable, and compliant using ONLY HTML, CSS, and JavaScript—no external frameworks or backends.

            Key Guidelines:
            - Preserve core PWA features: Maintain offline support via an updated sw.js (caching strategy, fetch handling). Ensure manifest.json has valid icons (update paths if needed), start_url: '/', display: 'standalone', and colors. Register the service worker in index.html or app.js.
            - Responsiveness: Use or enhance TailwindCSS (via CDN if not already) for all updates. Add custom CSS in styles.css only as needed.
            - Functionality: Implement the requested changes in app.js with vanilla JS (e.g., update DOM, localStorage). If adding pages or routing, keep it client-side. Ensure the app works offline post-changes.
            - Enhancements: Include favicon if missing: <link rel="icon" href="favicon.ico" type="image/x-icon">. Optionally integrate Feather Icons, AOS, or Vanta.js as before if they fit the rework.
            - Integrity: Do not break existing features unless the request specifies removal. Make changes precise and minimal while improving UI/UX. Ensure accessibility, performance, and PWA standards. Avoid Chinese characters. No explanations—just output the updated files in JSON format.
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
        instructions.put("role", "You are a helpful assistant that rewrites user prompts to make them optimal for generating fully functional Progressive Web Apps (PWAs) using only HTML, CSS, and JavaScript.")
        instructions.put("goal", "Enhance the original prompt to be more detailed, specific, and focused on creating exceptional UI/UX design, responsive layouts (emphasize TailwindCSS), PWA features (offline support, installability via manifest and service worker), and robust client-side functionality. Suggest multi-page if it suits, or SPA otherwise. Include details on animations (Feather Icons, AOS, Vanta.js) and accessibility if relevant.")

        val outputFormat = JSONObject()
        outputFormat.put("format", "text")
        outputFormat.put("start_marker", ">>>>>>> START PROMPT >>>>>>")
        outputFormat.put("end_marker", ">>>>>>> END PROMPT >>>>>>")
        outputFormat.put("language", "same as input")
        outputFormat.put("intent", "keep original intent, but improve clarity, add PWA specifics, and elaborate on design/functionality for better results")

        instructions.put("output_format", outputFormat)

        val rules = JSONObject()
        rules.put("if_no_rewrite_needed", "return original prompt unchanged")
        rules.put("add_pwa_focus", "Always incorporate PWA requirements like service worker, manifest, offline capability, and responsiveness")
        rules.put("avoid_overcomplication", "Keep it concise yet detailed; don't add unrelated features")
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
        indexHtml.put("description", "Complete, valid HTML file (index.html) with <!DOCTYPE html>, <html lang='en'>, <head> including meta viewport='width=device-width, initial-scale=1', title, favicon link, Tailwind CDN script, manifest link <link rel='manifest' href='manifest.json'>, and service worker registration in <body> or via app.js. Reference styles.css and app.js. Ensure semantic structure and responsiveness.")
        properties.put("index.html", indexHtml)

        val manifestJson = JSONObject()
        manifestJson.put("type", "string")
        manifestJson.put("description", "Valid JSON manifest with name, short_name, description, start_url: '/', display: 'standalone', theme_color, background_color, icons array (at least 192x192 and 512x512 with src placeholders like 'icon-192.png'), and scope: '/'.")
        properties.put("manifest.json", manifestJson)

        val swJs = JSONObject()
        swJs.put("type", "string")
        swJs.put("description", "Fully functional service worker (sw.js) with self.addEventListener for 'install' (caches static assets like index.html, styles.css, app.js, manifest.json), 'activate' (cleans old caches), and 'fetch' (serves from cache or fetches/network fallback with error handling for offline). Use Cache API.")
        properties.put("sw.js", swJs)

        val appJs = JSONObject()
        appJs.put("type", "string")
        appJs.put("description", "Vanilla JavaScript file (app.js) implementing all app logic, event handling, data persistence (localStorage), and PWA registration: if ('serviceWorker' in navigator) { navigator.serviceWorker.register('sw.js'); }. Make it modular and efficient.")
        properties.put("app.js", appJs)

        val stylesCss = JSONObject()
        stylesCss.put("type", "string")
        stylesCss.put("description", "Custom CSS file (styles.css) for any non-Tailwind styles, global resets, or overrides. Linked in index.html <head> via <link rel='stylesheet' href='styles.css'>.")
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
        exampleFiles.put("index.html", "<!DOCTYPE html><html lang='en'><head><meta charset='UTF-8'><meta name='viewport' content='width=device-width, initial-scale=1.0'><title>Example PWA</title><link rel='manifest' href='manifest.json'><link rel='icon' href='favicon.ico' type='image/x-icon'><script src='https://cdn.tailwindcss.com'></script><link rel='stylesheet' href='styles.css'></head><body><script src='app.js'></script></body></html>")
        exampleFiles.put("manifest.json", "{\"name\":\"Example PWA\",\"short_name\":\"Example\",\"start_url\":\"/\",\"display\":\"standalone\",\"theme_color\":\"#000000\",\"background_color\":\"#ffffff\",\"icons\":[{\"src\":\"icon-192.png\",\"sizes\":\"192x192\",\"type\":\"image/png\"},{\"src\":\"icon-512.png\",\"sizes\":\"512x512\",\"type\":\"image/png\"}]}")
        exampleFiles.put("sw.js", "const CACHE_NAME = 'pwa-cache-v1'; self.addEventListener('install', e => { e.waitUntil(caches.open(CACHE_NAME).then(cache => cache.addAll(['/index.html', '/styles.css', '/app.js', '/manifest.json']))); }); self.addEventListener('fetch', e => { e.respondWith(caches.match(e.request).then(response => response || fetch(e.request).catch(() => caches.match('/index.html')))); });")
        exampleFiles.put("app.js", "if ('serviceWorker' in navigator) { navigator.serviceWorker.register('/sw.js'); } // App logic here")
        exampleFiles.put("styles.css", "/* Custom styles */ body { font-family: sans-serif; }")
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