# Mothership

Mothership is a web application project. This repository contains the backend application, static assets, and a generated Progressive Web App (PWA).

## Project Structure

```
app.py                # Main backend application (likely Flask or similar)
requirements.txt      # Python dependencies

static/               # Static files for the web app
  manifest.json
  script.js
  service-worker.js
  styles.css

templates/            # HTML templates
  index.html

generated/pwa-app/    # Generated PWA assets
  index.html
  manifest.json
  script.js
  styles.css
  sw.js
```


## Environment Variables

Create a `.env` file in the project root and add your OpenRouter API key:

```env
OPENROUTER_API_KEY=your_openrouter_api_key_here
```

Replace `your_openrouter_api_key_here` with your actual OpenRouter API key.

## Getting Started

1. **Install dependencies:**
   ```bash
   pip install -r requirements.txt
   ```
2. **Run the application:**
   ```bash
   python app.py
   ```
3. **Access the app:**
   Open your browser and go to `http://localhost:5000` (or the port specified in `app.py`).

## Features
- Backend Python application
- Static assets for web frontend
- Progressive Web App (PWA) support
