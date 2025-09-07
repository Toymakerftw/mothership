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
            You are an expert UI/UX and Front-End Developer specializing in Progressive Web Apps (PWAs).
            Your task is to create a fully functional, installable PWA using **only HTML, CSS, and JavaScript**. No server-side code, frameworks (React/Vue/Angular), or external backends—everything must run client-side.
            Focus on delivering a responsive, native-like experience for both mobile and desktop.

            **Core Requirements:**
            - **Responsive Design:**
              Use TailwindCSS LOCALLY via `<script src='tailwind.min.js'></script>` included in the generated PWA folder (do NOT use an external CDN).
              Use Tailwind classes for layout, styling, and responsiveness (e.g., `sm:`, `md:`, `lg:` breakpoints).
              Fall back to custom CSS in `styles.css` only if Tailwind is insufficient.

            - **Offline Functionality:**
              Implement a service worker (`sw.js`) to cache all essential assets (HTML, CSS, JS, images).
              Ensure `tailwind.min.js` is included in the cache list for offline use.
              Handle network errors gracefully with fallback messages.

            - **Installability:**
              `manifest.json` must include:
              - `name`, `short_name`, `description`
              - `start_url: '/'`
              - `display: 'standalone'`
              - `theme_color`, `background_color`
              - `icons` array (use placeholders like `icon-192.png`, `icon-512.png`; provide base64 if possible).
              Include a favicon in HTML: `<link rel="icon" href="favicon.ico" type="image/x-icon">`.

            - **File Structure:**
              Generate:
              - `index.html` (entry point, meta viewport, includes `<script src='tailwind.min.js'></script>` in `<head>`, manifest link, and service worker registration)
              - `manifest.json`
              - `sw.js` (install, activate, fetch events for caching)
              - `app.js` (core logic, PWA registration via `if ('serviceWorker' in navigator)`)
              - `styles.css` (only if custom CSS is needed beyond Tailwind)

            - **Functionality:**
              The app must be interactive and fully functional based on the user prompt.
              Use vanilla JS for all logic (DOM manipulation, localStorage, event listeners).
              If the prompt suggests multiple pages, create a multi-page app with navigation.
              Otherwise, prefer a Single Page Application (SPA) with client-side routing.

            - **Enhancements:**
              - **Icons:** Use Feather Icons LOCALLY via `<script src='feather.min.js'></script>` in `<head>`, then initialize with `<script>feather.replace();</script>` in `<body>` (do NOT use an external CDN). Ensure `feather.min.js` is cached in `sw.js` for offline usage.
              - **Animations:** Use AOS LOCALLY via `<link href='aos.css' rel='stylesheet'>` and `<script src='aos.js'></script>` in `<head>`, then initialize with `<script>AOS.init();</script>` in `<body>` (do NOT use an external CDN). Ensure both files are cached in `sw.js`.
              - **Background/Interactive Animations:** Use Vanta Globe LOCALLY via `<script src='vanta.globe.min.js'></script>` (do NOT use an external CDN).
              Ensure Vanta Globe is cached in `sw.js` for offline usage.

            - **Best Practices:**
              Ensure accessibility (ARIA, semantic HTML), performance (lazy loading), security (HTTPS assumed), and PWA compliance (aim for Lighthouse score 100).
              Avoid Chinese characters unless specified.
              Output only the files in JSON format—no explanations.
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
            You are a UI/UX and Front-End Developer specializing in Progressive Web Apps (PWAs).
            Your task is to update an existing PWA based on the user's request, using only the provided files: index.html, manifest.json, sw.js, app.js, and styles.css.
            Output only the updated files, ensuring the PWA remains functional, installable, and compliant with modern standards.

            **Strict Requirements:**
            - **PWA Core Features:**
              - Offline support via sw.js (update caching and fetch strategies as needed).
              - Valid manifest.json (include icons, start_url: '/', display: 'standalone', theme_color, and background_color).
              - Register the service worker in index.html or app.js.
            - **Design & Styling:**
              - Use TailwindCSS LOCALLY via `<script src='tailwind.min.js'></script>` included with the app (do NOT use external CDN). Add custom CSS in styles.css only when Tailwind is insufficient.
            - **Functionality:**
              - Implement changes in app.js using vanilla JavaScript (e.g., DOM manipulation, localStorage, client-side routing for new pages).
              - Ensure all features work offline.
            - **Assets:**
              - Add a favicon if missing: `<link rel=\"icon\" href=\"favicon.ico\" type=\"image/x-icon\">`.
              - Ensure `tailwind.min.js` is referenced in index.html and cached in sw.js for offline usage.
              - Use Feather Icons LOCALLY via `<script src='feather.min.js'></script>` and call `feather.replace();`; cache in sw.js.
              - Use AOS LOCALLY via `<link href='aos.css' rel='stylesheet'>` and `<script src='aos.js'></script>`, and cache both in sw.js (no CDN). Initialize AOS in the page.
              - If using Vanta background animations, reference Vanta Globe LOCALLY via `<script src='vanta.globe.min.js'></script>` and cache it in sw.js (no CDN).
              - Optionally use Feather Icons, AOS, or Vanta.js if they enhance the design or functionality.
            - **Standards:**
              - Preserve existing features unless explicitly requested for removal.
              - Ensure accessibility, performance, and compliance with PWA best practices.
              - Avoid Chinese characters and external frameworks/backends.
            - **Output:**
              - Return updated files in JSON format, with each file as a key and its content as the value.
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
        indexHtml.put("description", "Complete, valid HTML file (index.html) with <!DOCTYPE html>, <html lang='en'>, <head> including meta viewport='width=device-width, initial-scale=1', title, favicon link, local Tailwind script <script src='tailwind.min.js'></script>, manifest link <link rel='manifest' href='manifest.json'>, and service worker registration in <body> or via app.js. Reference styles.css and app.js. Ensure semantic structure and responsiveness.")
        properties.put("index.html", indexHtml)

        val manifestJson = JSONObject()
        manifestJson.put("type", "string")
        manifestJson.put("description", "Valid JSON manifest with name, short_name, description, start_url: '/', display: 'standalone', theme_color, background_color, icons array (at least 192x192 and 512x512 with src placeholders like 'icon-192.png'), and scope: '/'.")
        properties.put("manifest.json", manifestJson)

        val swJs = JSONObject()
        swJs.put("type", "string")
        swJs.put("description", "Fully functional service worker (sw.js) with self.addEventListener for 'install' (caches static assets like index.html, styles.css, app.js, manifest.json, favicon.ico, and tailwind.min.js), 'activate' (cleans old caches), and 'fetch' (serves from cache or fetches/network fallback with error handling for offline). Use Cache API.")
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
        exampleFiles.put("index.html", "<!DOCTYPE html><html lang='en'><head><meta charset='UTF-8'><meta name='viewport' content='width=device-width, initial-scale=1.0'><title>Example PWA</title><link rel='manifest' href='manifest.json'><link rel='icon' href='favicon.ico' type='image/x-icon'><script src='tailwind.min.js'></script><link rel='stylesheet' href='styles.css'></head><body><script src='app.js'></script></body></html>")
        exampleFiles.put("manifest.json", "{\"name\":\"Example PWA\",\"short_name\":\"Example\",\"start_url\":\"/\",\"display\":\"standalone\",\"theme_color\":\"#000000\",\"background_color\":\"#ffffff\",\"icons\":[{\"src\":\"icon-192.png\",\"sizes\":\"192x192\",\"type\":\"image/png\"},{\"src\":\"icon-512.png\",\"sizes\":\"512x512\",\"type\":\"image/png\"}]}")
        exampleFiles.put("sw.js", "const CACHE_NAME = 'pwa-cache-v1'; self.addEventListener('install', e => { e.waitUntil(caches.open(CACHE_NAME).then(cache => cache.addAll(['/', '/index.html', '/styles.css', '/app.js', '/manifest.json', '/favicon.ico', '/tailwind.min.js']))); }); self.addEventListener('fetch', e => { e.respondWith(caches.match(e.request).then(response => response || fetch(e.request).catch(() => caches.match('/index.html')))); });")
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