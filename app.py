from flask import Flask, render_template, request, jsonify, send_from_directory
import os
import requests
import re
import json
import logging
import base64
import hashlib
import time
import sqlite3
import uuid
from io import BytesIO
from cachetools import TTLCache
from flask_compress import Compress
from circuitbreaker import circuit
from tenacity import retry, stop_after_attempt, wait_exponential, retry_if_exception_type

from config import (
    OPENROUTER_API_KEY, OPENROUTER_API_URL,
    OPTIMIZER_MODEL, GENERATOR_MODEL, VERIFIER_MODEL,
    MAX_VERIFICATION_ITERATIONS, REQUEST_TIMEOUT
)

# Set up logging
logging.basicConfig(level=logging.DEBUG, format='%(asctime)s - %(levelname)s - %(message)s')
logger = logging.getLogger(__name__)

app = Flask(__name__)
# Enable compression
Compress(app)

# Initialize caches with TTL (time-to-live)
# Cache for optimized prompts (5 minutes TTL, max 100 entries)
prompt_cache = TTLCache(maxsize=100, ttl=300)
# Cache for generated code responses (10 minutes TTL, max 50 entries)
code_cache = TTLCache(maxsize=50, ttl=600)

# Circuit breaker configuration
# Trip after 3 failures, reset after 60 seconds
OPENROUTER_CIRCUIT_BREAKER = circuit(
    failure_threshold=3,
    recovery_timeout=60,
    expected_exception=(requests.exceptions.RequestException, ValueError)
)

# Retry configuration with exponential backoff
# Retry up to 3 times with exponential backoff (1s, 2s, 4s)
OPENROUTER_RETRY = retry(
    stop=stop_after_attempt(3),
    wait=wait_exponential(multiplier=1, min=1, max=10),
    retry=retry_if_exception_type((requests.exceptions.Timeout, requests.exceptions.ConnectionError)),
    reraise=True
)

# Database setup
def init_db():
    conn = sqlite3.connect('pwa_generator.db')
    c = conn.cursor()
    c.execute('''CREATE TABLE IF NOT EXISTS apps
                 (id INTEGER PRIMARY KEY AUTOINCREMENT,
                  name TEXT UNIQUE NOT NULL,
                  prompt TEXT,
                  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)''')
    c.execute('''CREATE TABLE IF NOT EXISTS app_stats
                 (id INTEGER PRIMARY KEY AUTOINCREMENT,
                  app_id INTEGER,
                  view_count INTEGER DEFAULT 0,
                  generate_count INTEGER DEFAULT 0,
                  FOREIGN KEY (app_id) REFERENCES apps (id))''')
    conn.commit()
    conn.close()

def generate_unique_app_name(prompt):
    """Generate a unique app name using a UUID"""
    return str(uuid.uuid4())

# Initialize database on startup
init_db()

@app.route("/")
def index():
    return render_template("index.html")

@app.route("/apps")
def list_apps():
    base_dir = "generated"
    os.makedirs(base_dir, exist_ok=True)
    
    # Pagination parameters
    page = request.args.get('page', 1, type=int)
    per_page = request.args.get('per_page', 10, type=int)
    
    # Ensure per_page is within reasonable limits
    per_page = min(per_page, 50)
    
    try:
        # Get entries from database
        conn = sqlite3.connect('pwa_generator.db')
        c = conn.cursor()
        
        # Get total count
        c.execute("SELECT COUNT(*) FROM apps")
        total_entries = c.fetchone()[0]
        
        # Calculate pagination
        total_pages = (total_entries + per_page - 1) // per_page  # Ceiling division
        page = max(1, min(page, total_pages))  # Ensure page is within valid range
        offset = (page - 1) * per_page
        
        # Get apps with pagination
        c.execute("""
            SELECT a.name, a.prompt, a.created_at, a.updated_at, s.view_count, s.generate_count
            FROM apps a
            LEFT JOIN app_stats s ON a.id = s.app_id
            ORDER BY a.updated_at DESC
            LIMIT ? OFFSET ?
        """, (per_page, offset))
        
        db_entries = c.fetchall()
        conn.close()
        
        # Enhance with file system information
        entries = []
        for row in db_entries:
            name, prompt, created_at, updated_at, view_count, generate_count = row
            path = os.path.join(base_dir, name)
            if os.path.isdir(path):
                index_path = os.path.join(path, "index.html")
                has_index = os.path.isfile(index_path)
                entries.append({
                    "name": name,
                    "prompt": prompt,
                    "created_at": created_at,
                    "updated_at": updated_at,
                    "view_count": view_count or 0,
                    "generate_count": generate_count or 0,
                    "has_index": has_index,
                    "preview_url": f"/generated/{name}/index.html" if has_index else None
                })
        
        # If no database entries, fall back to file system scanning
        if not entries:
            # Get all entries from file system
            all_entries = []
            for name in sorted(os.listdir(base_dir)):
                path = os.path.join(base_dir, name)
                if os.path.isdir(path):
                    index_path = os.path.join(path, "index.html")
                    has_index = os.path.isfile(index_path)
                    all_entries.append({
                        "name": name,
                        "has_index": has_index,
                        "preview_url": f"/generated/{name}/index.html" if has_index else None
                    })
            
            total_entries = len(all_entries)
            total_pages = (total_entries + per_page - 1) // per_page  # Ceiling division
            page = max(1, min(page, total_pages))  # Ensure page is within valid range
            start_idx = (page - 1) * per_page
            end_idx = start_idx + per_page
            entries = all_entries[start_idx:end_idx]
        
        return render_template("apps.html", 
                             apps=entries,
                             current_page=page,
                             total_pages=total_pages,
                             per_page=per_page,
                             total_entries=total_entries)
    except Exception as e:
        logger.error(f"Error listing apps: {str(e)}")
        return render_template("apps.html", apps=[], error=str(e))

@app.route("/generated/<app_name>/<path:filename>")
def generated_files(app_name, filename):
    # Basic path normalization to prevent traversal
    safe_path = os.path.normpath(filename)
    if '..' in safe_path or safe_path.startswith('/'):
        return jsonify({"error": "Invalid file path"}), 400
    return send_from_directory(f"generated/{app_name}", safe_path)

@OPENROUTER_CIRCUIT_BREAKER
@OPENROUTER_RETRY
def call_openrouter(model, system_prompt, user_content):
    headers = {
        "Authorization": f"Bearer {OPENROUTER_API_KEY}",
        "Content-Type": "application/json",
        "HTTP-Referer": request.host_url.rstrip('/'),
        "X-Title": "PWA Generator"
    }
    data = {
        "model": model,
        "messages": [
            {"role": "system", "content": system_prompt},
            {"role": "user", "content": user_content}
        ]
    }
    try:
        response = requests.post(OPENROUTER_API_URL, headers=headers, json=data, timeout=REQUEST_TIMEOUT)
        try:
            response.raise_for_status()
        except requests.exceptions.HTTPError as http_err:
            error_body = None
            try:
                error_body = response.json()
            except Exception:
                error_body = response.text
            logger.error(f"API request failed ({response.status_code}): {error_body}")
            raise ValueError(f"OpenRouter error {response.status_code}: {error_body}")

        result = response.json()
        content = result["choices"][0]["message"]["content"]
        logger.debug(f"API call to {model} successful. Response length: {len(content)}")
        return content
    except requests.exceptions.Timeout:
        logger.error("API request timeout")
        raise ValueError("Request timeout")
    except requests.exceptions.RequestException as e:
        logger.error(f"API request failed: {str(e)}")
        raise ValueError(f"API request failed: {str(e)}")
    except (KeyError, IndexError) as e:
        logger.error(f"Unexpected response format: {str(e)}")
        raise ValueError(f"Unexpected response format: {str(e)}")

@app.route("/static/icons/<path:icon_filename>")
def serve_icons(icon_filename):
    """Serve icons from static/icons or fall back to a 1x1 PNG to avoid 404s."""
    static_icons_dir = os.path.join(app.root_path, "static", "icons")
    requested_path = os.path.join(static_icons_dir, icon_filename)

    if os.path.isfile(requested_path):
        return send_from_directory(static_icons_dir, icon_filename)

    one_px_png_base64 = (
        "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAukB9Wl0C3sAAAAASUVORK5CYII="
    )
    png_bytes = base64.b64decode(one_px_png_base64)
    return app.response_class(png_bytes, mimetype="image/png")

@app.route("/get_app_files/<app_name>")
def get_app_files(app_name):
    try:
        app_dir = os.path.join("generated", app_name)
        if not os.path.isdir(app_dir):
            return jsonify({"error": "App not found"}), 404

        files = {}
        for filename in ["index.html", "styles.css", "script.js"]:
            filepath = os.path.join(app_dir, filename)
            if os.path.isfile(filepath):
                with open(filepath, "r", encoding="utf-8") as f:
                    files[filename] = f.read()
            else:
                files[filename] = ""
        
        return jsonify(files)
    except Exception as e:
        logger.error(f"Error getting app files for {app_name}: {str(e)}")
        return jsonify({"error": str(e)}), 500

@app.route("/chat", methods=["POST"])
def chat():
    user_prompt = request.json.get("prompt")
    files = request.json.get("files")

    if not user_prompt:
        logger.warning("No prompt provided in chat request")
        return jsonify({"error": "No prompt provided"}), 400

    if files:
        user_prompt += "\n\nHere are the current files:\n"
        for filename, content in files.items():
            user_prompt += f"--- {filename} ---\n{content}\n"

    try:
        # Create cache keys
        prompt_cache_key = hashlib.md5(user_prompt.encode()).hexdigest()
        
        # Track if we used cached responses
        prompt_cached = False
        code_cached = False
        
        # Step 1: Optimize Prompt (with caching)
        optimizer_system = """
You are a prompt optimizer for PWA generation. Refine the user's prompt to be more detailed using this structure:

1. Core Functionality:
   - Primary Purpose: [Identify main app purpose]
   - Essential Features: [List core features]

2. UI/UX Enhancements:
   - Layout Suggestions: [Recommend layouts]
   - Animations/Transitions: [Suggest effects]
   - Dark/Light Mode: [Mandatory support]
   - Empty States: [Include placeholders]
   - Confirmation Dialogs: [For critical actions]

3. Responsiveness:
   - Tailwind CSS: [For adaptive design]
   - Mobile Interactions: [Touch-friendly elements]

4. Accessibility:
   - WCAG 2.1 AA Compliance: [Mandatory]
   - Keyboard Navigation: [Required]
   - ARIA Labels: [For interactive elements]
   - High Contrast: [For readability]

5. PWA Features:
   - Web App Manifest: [With icons/colors]
   - Service Worker: [For offline/cache]
   - Push Notifications: [For updates]

6. Data & Performance:
   - Storage: [Local/IndexedDB]
   - Lazy Loading: [For assets]
   - Optimization: [Fast load times]

7. Security & Extensibility:
   - Authentication: [If needed]
   - Backend: [Firebase/etc. options]

Output only the optimized prompt following this structure.
"""
        
        # Check cache first
        if prompt_cache_key in prompt_cache:
            logger.info("Using cached optimized prompt")
            optimized_prompt = prompt_cache[prompt_cache_key]
            prompt_cached = True
        else:
            logger.info("Optimizing prompt...")
            optimized_prompt = call_openrouter(OPTIMIZER_MODEL, optimizer_system, user_prompt)
            # Cache the optimized prompt
            prompt_cache[prompt_cache_key] = optimized_prompt
        
        # Create cache key for code generation
        code_cache_key = hashlib.md5(optimized_prompt.encode()).hexdigest()
        
        # Step 2: Generate Code (with caching)
        generator_system = """
ONLY USE HTML, CSS AND JAVASCRIPT. Create the best, modern, responsive UI possible.
MAKE IT RESPONSIVE USING TAILWINDCSS. Import Tailwind via CDN in <head> using <script src="https://cdn.tailwindcss.com"></script>.
Prefer Tailwind utility classes heavily for layout and components (CSS Grid for dashboards/cards, Flexbox for toolbars/forms). If Tailwind cannot cover a case, add minimal custom CSS in styles.css.
Use semantic HTML5 (<main>, <header>, <nav>, <section>, <footer>), accessible patterns (ARIA where needed), and a dark theme by default with good contrast.
If you want to use ICONS, import the icon library first. Use Feather Icons (https://unpkg.com/feather-icons) and/or Font Awesome (CDN) wherever icons help. If using Feather, add data-feather attributes and call feather.replace() in script.js after DOM load. If using Font Awesome, use <i> with appropriate classes.
Avoid Chinese characters unless explicitly requested by the user. Be creative and elaborate to produce something unique and polished.
IMPORTANT: For this API, ALWAYS output exactly three files: index.html (with Tailwind CDN + any icon CDN), styles.css (only minimal custom CSS), and script.js (modern JS, no frameworks). Do NOT inline CSS (except the Tailwind CDN include) and prefer Tailwind classes in markup.
Ensure offline compatibility with an external service worker (do not generate it inside these files). Include <link rel="manifest" href="manifest.json"> in index.html and service worker registration in script.js: if ('serviceWorker' in navigator) { navigator.serviceWorker.register('/sw.js'); }
Separate each file with the following exact tags: [HTML]...[/HTML] [CSS]...[/CSS] [JS]...[/JS]
"""
        
        # Check cache first
        if code_cache_key in code_cache:
            logger.info("Using cached generated code")
            generated_response = code_cache[code_cache_key]
            code_cached = True
        else:
            logger.info("Generating code...")
            generated_response = call_openrouter(GENERATOR_MODEL, generator_system, f"Create a PWA for: {optimized_prompt}")
            # Cache the generated code
            code_cache[code_cache_key] = generated_response

        # Step 3: Verify and Iterate
        current_response = generated_response
        for i in range(MAX_VERIFICATION_ITERATIONS):
            logger.info(f"Verification iteration {i+1}/{MAX_VERIFICATION_ITERATIONS}")
            verifier_system = """
You are a code reviewer for HTML/CSS/JS PWAs. Check the generated code for:
- Syntax errors, bugs, or inefficiencies.
- Compliance with instructions (Tailwind, semantic HTML, accessibility, dark theme, icons).
- Responsiveness and modern best practices.
- PWA features (manifest link, SW registration in JS).
Output: If good, say 'VALID' and return the code unchanged. If issues, say 'INVALID', list fixes, and provide the full revised code in [HTML]...[/HTML] [CSS]...[/CSS] [JS]...[/JS] format.
"""
            verification = call_openrouter(VERIFIER_MODEL, verifier_system, current_response)
            
            if "VALID" in verification.upper():
                logger.info("Code verified as valid.")
                break
            elif "INVALID" in verification.upper():
                logger.info("Code invalid; extracting revisions.")
                current_response = extract_revised_code(verification)
            else:
                raise ValueError("Verification response unclear")

        # Step 4: JS-Specific Bug Check (Browser Console Simulation)
        js_code = extract_code(current_response, "JS")
        js_bug_report = []
        if js_code:
            js_verifier_system = """
You are a JavaScript debugger simulating a browser console (e.g., Chrome DevTools). Analyze this JS code as if executed in a browser environment with DOM, assuming it runs alongside the generated HTML/CSS. Check for:
- Syntax errors (e.g., missing semicolons, unbalanced brackets).
- Runtime errors (e.g., ReferenceError for undefined variables, TypeError for null/undefined operations).
- DOM-related issues (e.g., querying non-existent elements, missing event listeners).
- Common console warnings (e.g., deprecated APIs, improper async handling).
- Logical bugs (e.g., infinite loops, incorrect event handling).
- PWA-specific issues (e.g., missing service worker registration).
Simulate execution step-by-step, assuming a modern browser context (window, document, etc.). For each issue:
- Specify the error type (e.g., SyntaxError, ReferenceError).
- Provide the line number (or approximate if unclear).
- Explain the issue and suggest a fix.
Output format:
- If no issues: 'VALID_JS
[JS]{code}[/JS]'
- If issues: 'INVALID_JS
Errors:
- [Error Type]: [Description, line ~X, fix suggestion]
[JS]{revised code}[/JS]'
"""
            logger.info("Performing JS-specific bug check...")
            js_verification = call_openrouter(VERIFIER_MODEL, js_verifier_system, js_code)
            
            if "INVALID_JS" in js_verification.upper():
                logger.info("JS code has issues; processing revisions.")
                # Extract errors for reporting
                error_section = re.search(r'Errors:\n(.*?)\n\[JS\]', js_verification, re.DOTALL | re.IGNORECASE)
                if error_section:
                    js_bug_report = error_section.group(1).strip().split('\n')
                
                # Extract revised JS
                revised_js_match = re.search(r'\[JS\](.*?)\[/JS\]', js_verification, re.DOTALL | re.IGNORECASE)
                if revised_js_match:
                    revised_js = revised_js_match.group(1).strip()
                    # Replace JS section in current_response
                    current_response = re.sub(r'\[JS\].*?\[/JS\]', f'[JS]{revised_js}[/JS]', current_response, flags=re.DOTALL)
                else:
                    logger.warning("No revised JS found in verification; keeping original JS.")
            else:
                logger.info("JS code verified as valid.")

        return jsonify({
            "response": current_response,
            "js_bug_report": js_bug_report if js_bug_report else ["No JavaScript errors detected"],
            "cached": prompt_cached and code_cached  # True only if both prompt and code were cached
        })
    except ValueError as e:
        logger.error(f"Value error in chat endpoint: {str(e)}")
        return jsonify({"error": str(e), "js_bug_report": []}), 500
    except requests.exceptions.RequestException as e:
        logger.error(f"API request error in chat endpoint: {str(e)}")
        return jsonify({"error": "API service temporarily unavailable", "js_bug_report": []}), 503
    except Exception as e:
        logger.error(f"Unexpected error in chat endpoint: {str(e)}", exc_info=True)
        return jsonify({"error": "An unexpected error occurred", "js_bug_report": []}), 500

def extract_revised_code(response_text):
    """Extract revised code from verifier response, similar to extract_code but combined."""
    patterns = [
        r"\[HTML\](.*?)\[/HTML\].*?\[CSS\](.*?)\[/CSS\].*?\[JS\](.*?)\[/JS\]",
        r"```html\s*(.*?)\s*```.*```css\s*(.*?)\s*```.*```javascript\s*(.*?)\s*```",
    ]
    for pattern in patterns:
        match = re.search(pattern, response_text, re.DOTALL | re.IGNORECASE)
        if match:
            return f"[HTML]{match.group(1).strip()}[/HTML] [CSS]{match.group(2).strip()}[/CSS] [JS]{match.group(3).strip()}[/JS]"
    return response_text

def extract_code(response_text, file_type):
    """Extract code for a specific file type using multiple patterns"""
    patterns = [
        rf"\[{file_type}\](.*?)\[/{file_type}\]",
        rf"```(?:{file_type.lower()})\s*(.*?)```",
        rf"{file_type} code:(.*?)(?=\n\w+ code:|\Z)",
    ]
    
    for pattern in patterns:
        match = re.search(pattern, response_text, re.DOTALL | re.IGNORECASE)
        if match:
            return match.group(1).strip()
    
    return None

@app.route("/generate", methods=["POST"])
def generate_files():
    prompt = request.json.get("prompt")
    response_text = request.json.get("response_text")
    provided_app_name = request.json.get("app_name")
    
    if not response_text:
        logger.warning("Missing response text in generate request")
        return jsonify({"status": "error", "message": "Missing response text"}), 400
    
    # Ensure we have a valid prompt
    if not prompt or prompt.strip() == "":
        prompt = "pwa-app"
    
    app_name = generate_unique_app_name(prompt)
    app_dir = f"generated/{app_name}"
    
    try:
        os.makedirs(app_dir, exist_ok=True)
    except OSError as e:
        logger.error(f"Failed to create directory {app_dir}: {str(e)}")
        return jsonify({"status": "error", "message": f"Failed to create app directory: {str(e)}"}), 500

    html_code = extract_code(response_text, "HTML")
    css_code = extract_code(response_text, "CSS")
    js_code = extract_code(response_text, "JS")
    
    files_created = []
    
    try:
        if html_code:
            with open(f"{app_dir}/index.html", "w", encoding="utf-8") as f:
                f.write(html_code)
            files_created.append("index.html")
        else:
            basic_html = f"""<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>{prompt}</title>
    <link rel="stylesheet" href="styles.css">
    <link rel="manifest" href="manifest.json">
</head>
<body>
    <h1>{prompt}</h1>
    <p>This is a PWA generated by the PWA Generator.</p>
    <script src="script.js"></script>
</body>
</html>"""
            with open(f"{app_dir}/index.html", "w", encoding="utf-8") as f:
                f.write(basic_html)
            files_created.append("index.html")
        
        if css_code:
            with open(f"{app_dir}/styles.css", "w", encoding="utf-8") as f:
                f.write(css_code)
            files_created.append("styles.css")
        else:
            basic_css = """body {
    font-family: Arial, sans-serif;
    max-width: 800px;
    margin: 0 auto;
    padding: 20px;
    line-height: 1.6;
}"""
            with open(f"{app_dir}/styles.css", "w", encoding="utf-8") as f:
                f.write(basic_css)
            files_created.append("styles.css")
        
        if js_code:
            with open(f"{app_dir}/script.js", "w", encoding="utf-8") as f:
                f.write(js_code)
            files_created.append("script.js")
        else:
            basic_js = "console.log('PWA loaded successfully');"
            with open(f"{app_dir}/script.js", "w", encoding="utf-8") as f:
                f.write(basic_js)
            files_created.append("script.js")
        
        manifest_data = {
            "name": prompt,
            "short_name": app_name[:12],
            "start_url": "index.html",
            "display": "standalone",
            "background_color": "#ffffff",
            "theme_color": "#007BFF",
            "icons": [
                {"src": "icon-192.png", "sizes": "192x192", "type": "image/png"},
                {"src": "icon-512.png", "sizes": "512x512", "type": "image/png"}
            ]
        }
        
        with open(f"{app_dir}/manifest.json", "w", encoding="utf-8") as f:
            json.dump(manifest_data, f, indent=2)
        files_created.append("manifest.json")
        
        sw_content = """self.addEventListener('install', e => {
  e.waitUntil(
    caches.open('pwa-store').then(cache => {
      return cache.addAll([
        './',
        './index.html',
        './styles.css',
        './script.js',
        './manifest.json'
      ]);
    })
  );
});

self.addEventListener('fetch', e => {
  e.respondWith(
    caches.match(e.request).then(response => {
      return response || fetch(e.request);
    })
  );
});
"""
        with open(f"{app_dir}/sw.js", "w", encoding="utf-8") as f:
            f.write(sw_content)
        files_created.append("sw.js")
        
        # Store app information in database
        try:
            conn = sqlite3.connect('pwa_generator.db')
            c = conn.cursor()
            c.execute("INSERT OR REPLACE INTO apps (name, prompt) VALUES (?, ?)", (app_name, prompt))
            c.execute("INSERT OR IGNORE INTO app_stats (app_id, view_count, generate_count) VALUES ((SELECT id FROM apps WHERE name = ?), 0, 1)", (app_name,))
            c.execute("UPDATE app_stats SET generate_count = generate_count + 1 WHERE app_id = (SELECT id FROM apps WHERE name = ?)", (app_name,))
            conn.commit()
            conn.close()
        except sqlite3.Error as db_error:
            logger.error(f"Database error: {str(db_error)}")
            # Don't fail the whole request if database update fails
        except Exception as e:
            logger.error(f"Unexpected error updating database: {str(e)}")
        
        logger.info(f"Successfully generated app: {app_name}")
        return jsonify({
            "status": "success", 
            "files": files_created, 
            "app_name": app_name,
            "preview_url": f"/generated/{app_name}/index.html"
        })
        
    except PermissionError as e:
        logger.error(f"Permission error creating files for {app_name}: {str(e)}")
        return jsonify({"status": "error", "message": "Permission denied when creating app files"}), 500
    except OSError as e:
        logger.error(f"OS error creating files for {app_name}: {str(e)}")
        return jsonify({"status": "error", "message": f"Failed to create app files: {str(e)}"}), 500
    except Exception as e:
        logger.error(f"Unexpected error generating files for {app_name}: {str(e)}", exc_info=True)
        return jsonify({"status": "error", "message": f"An unexpected error occurred: {str(e)}"}), 500

@app.route("/delete_app/<app_name>", methods=["DELETE"])
def delete_app(app_name):
    try:
        app_dir = os.path.join("generated", app_name)
        if os.path.isdir(app_dir):
            import shutil
            shutil.rmtree(app_dir)
            
            # Also remove from database
            try:
                conn = sqlite3.connect('pwa_generator.db')
                c = conn.cursor()
                c.execute("DELETE FROM app_stats WHERE app_id = (SELECT id FROM apps WHERE name = ?)", (app_name,))
                c.execute("DELETE FROM apps WHERE name = ?", (app_name,))
                conn.commit()
                conn.close()
            except Exception as db_error:
                logger.error(f"Database error when deleting app {app_name}: {str(db_error)}")
            
            return jsonify({"success": True})
        else:
            return jsonify({"success": False, "error": "App not found"}), 404
    except Exception as e:
        logger.error(f"Error deleting app {app_name}: {str(e)}")
        return jsonify({"success": False, "error": str(e)}), 500

@app.route("/rework", methods=["POST"])
def rework_app():
    app_name = request.json.get("app_name")
    rework_prompt = request.json.get("rework_prompt")

    if not app_name or not rework_prompt:
        return jsonify({"error": "Missing app_name or rework_prompt"}), 400

    # Create cache key for rework
    rework_cache_key = hashlib.md5(f"{app_name}:{rework_prompt}".encode()).hexdigest()
    
    # Track if we used cached response
    rework_cached = False
    
    # Check cache first
    if rework_cache_key in code_cache:
        logger.info("Using cached rework response")
        reworked_response = code_cache[rework_cache_key]
        # Skip to verification step with cached response
        current_response = reworked_response
        rework_cached = True
    else:
        try:
            # 1. Read existing files
            app_dir = os.path.join("generated", app_name)
            if not os.path.isdir(app_dir):
                return jsonify({"error": "App not found"}), 404

            files = {}
            for filename in ["index.html", "styles.css", "script.js"]:
                filepath = os.path.join(app_dir, filename)
                if os.path.isfile(filepath):
                    with open(filepath, "r", encoding="utf-8") as f:
                        files[filename] = f.read()
                else:
                    files[filename] = ""

            # 2. Construct a detailed prompt for the rework
            rework_user_prompt = f"""
Rework the following PWA application based on the user's request.

User Request: {rework_prompt}

Current Files:
--- index.html ---
{files["index.html"]}

--- styles.css ---
{files["styles.css"]}

--- script.js ---
{files["script.js"]}
"""

            # 3. Call the generator model with the rework prompt
            generator_system = """
You are an expert PWA developer. Your task is to rework an existing application based on the user's request.
- ONLY USE HTML, CSS AND JAVASCRIPT.
- Make sure to only make changes to the section mentioned in the rework prompt.
- Do not let other functionalities be affected.
- MAKE IT RESPONSIVE USING TAILWINDCSS.
- Use semantic HTML5, accessible patterns, and a dark theme by default.
- Use Feather Icons or Font Awesome for icons.
- ALWAYS output exactly three files: index.html, styles.css, and script.js.
- Separate each file with the following exact tags: [HTML]...[/HTML] [CSS]...[/CSS] [JS]...[/JS]
"""
            logger.info(f"Reworking app: {app_name}")
            reworked_response = call_openrouter(GENERATOR_MODEL, generator_system, rework_user_prompt)
            # Cache the rework response
            code_cache[rework_cache_key] = reworked_response

            # Set current_response for verification step
            current_response = reworked_response
        except Exception as e:
            logger.error(f"Error reworking app {app_name}: {str(e)}")
            return jsonify({"error": str(e)}), 500

    # 4. Verify and Iterate (similar to the /chat endpoint)
    for i in range(MAX_VERIFICATION_ITERATIONS):
        logger.info(f"Rework verification iteration {i+1}/{MAX_VERIFICATION_ITERATIONS}")
        verifier_system = """
You are a code reviewer for HTML/CSS/JS PWAs. Check the generated code for:
- Syntax errors, bugs, or inefficiencies.
- Compliance with the rework request.
- That only the requested section was changed.
Output: If good, say 'VALID' and return the code unchanged. If issues, say 'INVALID', list fixes, and provide the full revised code in [HTML]...[/HTML] [CSS]...[/CSS] [JS]...[/JS] format.
"""
        verification = call_openrouter(VERIFIER_MODEL, verifier_system, current_response)
        
        if "VALID" in verification.upper():
            logger.info("Reworked code verified as valid.")
            break
        elif "INVALID" in verification.upper():
            logger.info("Reworked code invalid; extracting revisions.")
            current_response = extract_revised_code(verification)
            # Update cache with revised code
            code_cache[rework_cache_key] = current_response
        else:
            raise ValueError("Verification response unclear")

    # 5. Save the reworked files
    html_code = extract_code(current_response, "HTML")
    css_code = extract_code(current_response, "CSS")
    js_code = extract_code(current_response, "JS")

    if html_code:
        with open(os.path.join(app_dir, "index.html"), "w", encoding="utf-8") as f:
            f.write(html_code)
    if css_code:
        with open(os.path.join(app_dir, "styles.css"), "w", encoding="utf-8") as f:
            f.write(css_code)
    if js_code:
        with open(os.path.join(app_dir, "script.js"), "w", encoding="utf-8") as f:
            f.write(js_code)

    return jsonify({
        "status": "success",
        "app_name": app_name,
        "preview_url": f"/generated/{app_name}/index.html",
        "cached": rework_cached
    })

@app.route("/health")
def health_check():
    """Health check endpoint for monitoring"""
    try:
        # Check database connectivity
        conn = sqlite3.connect('pwa_generator.db')
        c = conn.cursor()
        c.execute("SELECT 1")
        conn.close()
        
        # Check if generated directory exists
        if not os.path.exists("generated"):
            return jsonify({
                "status": "unhealthy",
                "details": "Generated directory does not exist"
            }), 503
        
        return jsonify({
            "status": "healthy",
            "timestamp": time.time()
        })
    except Exception as e:
        logger.error(f"Health check failed: {str(e)}")
        return jsonify({
            "status": "unhealthy",
            "error": str(e)
        }), 503

@app.route("/health/db")
def db_health_check():
    """Database-specific health check"""
    try:
        conn = sqlite3.connect('pwa_generator.db', timeout=5)
        c = conn.cursor()
        c.execute("PRAGMA schema_version")
        version = c.fetchone()
        conn.close()
        
        return jsonify({
            "status": "healthy",
            "database_version": version[0] if version else None
        })
    except Exception as e:
        logger.error(f"Database health check failed: {str(e)}")
        return jsonify({
            "status": "unhealthy",
            "error": str(e)
        }), 503

@app.route("/health/api")
def api_health_check():
    """API-specific health check"""
    # Check if we can access the OpenRouter API (circuit breaker status)
    try:
        # Just check if the circuit breaker is closed
        if OPENROUTER_CIRCUIT_BREAKER.closed:
            return jsonify({
                "status": "healthy",
                "circuit_breaker": "closed"
            })
        else:
            return jsonify({
                "status": "degraded",
                "circuit_breaker": "open"
            }), 503
    except Exception as e:
        logger.error(f"API health check failed: {str(e)}")
        return jsonify({
            "status": "unhealthy",
            "error": str(e)
        }), 503

if __name__ == "__main__":
    os.makedirs("generated", exist_ok=True)
    app.run(debug=True, port=5000)