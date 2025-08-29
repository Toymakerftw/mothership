from flask import Flask, render_template, request, jsonify, send_from_directory
import os, requests, re, json
from dotenv import load_dotenv

load_dotenv()

app = Flask(__name__)

# Environment variables
OPENROUTER_API_KEY = os.getenv("OPENROUTER_API_KEY")
OPENROUTER_API_URL = "https://openrouter.ai/api/v1/chat/completions"

@app.route("/")
def index():
    return render_template("index.html")

@app.route("/apps")
def list_apps():
    base_dir = "generated"
    os.makedirs(base_dir, exist_ok=True)
    try:
        entries = []
        for name in sorted(os.listdir(base_dir)):
            path = os.path.join(base_dir, name)
            if os.path.isdir(path):
                index_path = os.path.join(path, "index.html")
                has_index = os.path.isfile(index_path)
                entries.append({
                    "name": name,
                    "has_index": has_index,
                    "preview_url": f"/generated/{name}/index.html" if has_index else None
                })
        return render_template("apps.html", apps=entries)
    except Exception as e:
        return render_template("apps.html", apps=[], error=str(e))

@app.route("/generated/<app_name>/<path:filename>")
def generated_files(app_name, filename):
    return send_from_directory(f"generated/{app_name}", filename)

@app.route("/chat", methods=["POST"])
def chat():
    prompt = request.json.get("prompt")
    if not prompt:
        return jsonify({"error": "No prompt provided"}), 400
    
    headers = {
        "Authorization": f"Bearer {OPENROUTER_API_KEY}",
        "Content-Type": "application/json"
    }
    
    # System prompt to steer high-quality generation
    system_prompt = """
ONLY USE HTML, CSS AND JAVASCRIPT. Create the best, modern, responsive UI possible.
MAKE IT RESPONSIVE USING TAILWINDCSS. Import Tailwind via CDN in <head> using <script src="https://cdn.tailwindcss.com"></script>.
Prefer Tailwind utility classes heavily for layout and components (CSS Grid for dashboards/cards, Flexbox for toolbars/forms). If Tailwind cannot cover a case, add minimal custom CSS in styles.css.
Use semantic HTML5 (<main>, <header>, <nav>, <section>, <footer>), accessible patterns (ARIA where needed), and a dark theme by default with good contrast.
If you want to use ICONS, import the icon library first. Use Feather Icons (https://unpkg.com/feather-icons) and/or Font Awesome (CDN) wherever icons help. If using Feather, add data-feather attributes and call feather.replace() in script.js after DOM load. If using Font Awesome, use <i> with appropriate classes.
Avoid Chinese characters unless explicitly requested by the user. Be creative and elaborate to produce something unique and polished.
IMPORTANT: For this API, ALWAYS output exactly three files: index.html (with Tailwind CDN + any icon CDN), styles.css (only minimal custom CSS), and script.js (modern JS, no frameworks). Do NOT inline CSS (except the Tailwind CDN include) and prefer Tailwind classes in markup.
Ensure offline compatibility with an external service worker (do not generate it inside these files).
Separate each file with the following exact tags: [HTML]...[/HTML] [CSS]...[/CSS] [JS]...[/JS]
"""
    
    data = {
        "model": "qwen/qwen-2.5-coder-32b-instruct:free",
        "messages": [
            {"role": "system", "content": system_prompt},
            {"role": "user", "content": f"Create a PWA for: {prompt}"}
        ]
    }

    try:
        response = requests.post(OPENROUTER_API_URL, headers=headers, json=data, timeout=30)
        response.raise_for_status()
        result = response.json()
        return jsonify({"response": result["choices"][0]["message"]["content"]})
    except requests.exceptions.Timeout:
        return jsonify({"error": "Request timeout"}), 408
    except requests.exceptions.RequestException as e:
        return jsonify({"error": f"API request failed: {str(e)}"}), 500
    except (KeyError, IndexError) as e:
        return jsonify({"error": f"Unexpected response format: {str(e)}"}), 500

def extract_code(response_text, file_type):
    """Extract code for a specific file type using multiple patterns"""
    patterns = [
        # Pattern 1: [HTML]...[/HTML]
        rf"\[{file_type}\](.*?)\[\/{file_type}\]",
        # Pattern 2: ```html...```
        rf"```(?:html|{file_type.lower()})\s*(.*?)```",
        # Pattern 3: <file_type> code: ... (fallback)
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
        return jsonify({"status": "error", "message": "Missing response text"}), 400
    
    # Ensure we always have a human-readable prompt for metadata (title/manifest)
    if not prompt:
        prompt = provided_app_name or "pwa-app"
    
    # Create a safe directory name, allowing explicit app_name override
    if provided_app_name:
        app_name = re.sub(r'[^a-z0-9\-]', '', str(provided_app_name).replace(" ", "-").lower())
    else:
        app_name = re.sub(r'[^a-z0-9\-]', '', prompt.replace(" ", "-").lower())
    
    app_dir = f"generated/{app_name}"
    os.makedirs(app_dir, exist_ok=True)

    # Extract code for each file type
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
            # Create a basic HTML template if extraction failed
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
            # Create basic CSS if extraction failed
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
            # Create basic JS if extraction failed
            basic_js = "console.log('PWA loaded successfully');"
            with open(f"{app_dir}/script.js", "w", encoding="utf-8") as f:
                f.write(basic_js)
            files_created.append("script.js")
        
        # Generate a basic manifest for the PWA
        manifest_data = {
            "name": prompt,
            "short_name": app_name[:12],
            "start_url": "index.html",
            "display": "standalone",
            "background_color": "#ffffff",
            "theme_color": "#007BFF",
            "icons": [
                {
                    "src": "icon-192.png",
                    "sizes": "192x192",
                    "type": "image/png"
                },
                {
                    "src": "icon-512.png",
                    "sizes": "512x512",
                    "type": "image/png"
                }
            ]
        }
        
        with open(f"{app_dir}/manifest.json", "w", encoding="utf-8") as f:
            json.dump(manifest_data, f, indent=2)
        files_created.append("manifest.json")
        
        # Create a basic service worker
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
        
        return jsonify({
            "status": "success", 
            "files": files_created, 
            "app_name": app_name,
            "preview_url": f"/generated/{app_name}/index.html"
        })
        
    except Exception as e:
        return jsonify({"status": "error", "message": f"File creation failed: {str(e)}"}), 500

if __name__ == "__main__":
    os.makedirs("generated", exist_ok=True)
    app.run(debug=True, port=5000)