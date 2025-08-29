let currentAppName = "";
let currentResponse = "";
let originalPrompt = ""; // Store the original prompt

// Add event listener for Enter key
document.getElementById("userInput").addEventListener("keypress", function(e) {
    if (e.key === "Enter") {
        sendPrompt();
    }
});

// Format message for display
function formatMessage(content, isUser = false) {
    const icon = isUser ? 
        '<div class="p-2 rounded-full bg-blue-500 text-white mr-3"><i class="fas fa-user"></i></div>' : 
        '<div class="p-2 rounded-full bg-blue-500 text-white mr-3"><i class="fas fa-robot"></i></div>';
    
    const sender = isUser ? "You" : "Assistant";
    
    return `
        <div class="message ${isUser ? 'user-message' : 'bot-message'} rounded-lg p-4 mb-4">
            <div class="flex items-start">
                ${icon}
                <div>
                    <strong>${sender}:</strong>
                    <p class="mt-1">${content}</p>
                </div>
            </div>
        </div>
    `;
}

async function sendPrompt(files = null) {
    const userInput = document.getElementById("userInput").value.trim();
    if (!userInput) return;

    // Store the original prompt
    originalPrompt = userInput;

    // Display user message
    const chatBox = document.getElementById("chatBox");
    chatBox.innerHTML += formatMessage(userInput, true);
    
    // Clear input
    document.getElementById("userInput").value = "";
    
    // Show loading indicator
    document.getElementById("loadingIndicator").style.display = "flex";
    document.getElementById("sendButton").disabled = true;

    try {
        const payload = { prompt: userInput };
        if (files) {
            payload.files = files;
        }

        // Call Flask backend
        const response = await fetch("/chat", {
            method: "POST",
            headers: {
                "Content-Type": "application/json",
            },
            body: JSON.stringify(payload),
        });
        
        const result = await response.json();
        
        if (result.error) {
            chatBox.innerHTML += formatMessage(`Error: ${result.error}`, false);
        } else {
            currentResponse = result.response;
            let message = `I've created a plan for your "${userInput}" PWA. Click "Compile App" to create the application.`;
            if (result.cached) {
                message += " (Used cached response to save costs)";
            }
            chatBox.innerHTML += formatMessage(message, false);
            
            // Show action buttons
            document.getElementById("actionButtons").style.display = "flex";
            
            // Generate a unique app name based on the original prompt
            if (!currentAppName) {
                currentAppName = generateUniqueAppName(userInput);
                localStorage.setItem("currentAppName", currentAppName);
            }
        }
    } catch (error) {
        console.error("Error:", error);
        chatBox.innerHTML += formatMessage("Failed to communicate with the server.", false);
    } finally {
        // Hide loading indicator
        document.getElementById("loadingIndicator").style.display = "none";
        document.getElementById("sendButton").disabled = false;
        
        // Scroll to bottom of chat
        chatBox.scrollTop = chatBox.scrollHeight;
    }
}

// Helper function to generate unique app names
function generateUniqueAppName(prompt) {
    const cleanInput = prompt.replace(/[^a-z0-9\s]/gi, '').toLowerCase();
    const words = cleanInput.split(/\s+/).filter(word => word.length > 0);
    // Take first few words and add timestamp
    const nameWords = words.slice(0, 4).join('-');
    const timestamp = Date.now().toString(36); // Base-36 timestamp for shorter string
    return `${nameWords}-${timestamp}`.replace(/[^a-z0-9\-]/g, '');
}

async function generateApp() {
    if (!currentResponse) return;

    const chatBox = document.getElementById("chatBox");
    chatBox.innerHTML += formatMessage("Generating files...", false);
    
    // Show loading indicator
    document.getElementById("loadingIndicator").style.display = "flex";
    document.getElementById("actionButtons").style.display = "none";

    try {
        // Generate improved app name if not already set
        if (!currentAppName) {
            currentAppName = generateUniqueAppName(originalPrompt || "pwa-app");
        }
        
        const response = await fetch("/generate", {
            method: "POST",
            headers: {
                "Content-Type": "application/json",
            },
            body: JSON.stringify({ 
                prompt: originalPrompt || "pwa-app",
                app_name: currentAppName,
                response_text: currentResponse 
            }),
        });
        
        const result = await response.json();
        
        if (result.status === "success") {
            chatBox.innerHTML += formatMessage(`Success! Created files: ${result.files.join(", ")}`, false);
            chatBox.innerHTML += formatMessage("You can now preview your app or get deployment instructions.", false);
            
            // Update current app name
            currentAppName = result.app_name;
            
            // Show action buttons again
            document.getElementById("actionButtons").style.display = "flex";
        } else {
            chatBox.innerHTML += formatMessage(`Error: ${result.message}`, false);
        }
    } catch (error) {
        console.error("Error:", error);
        chatBox.innerHTML += formatMessage("Failed to generate files.", false);
    } finally {
        // Hide loading indicator
        document.getElementById("loadingIndicator").style.display = "none";
        
        // Scroll to bottom of chat
        chatBox.scrollTop = chatBox.scrollHeight;
    }
}

async function previewApp() {
    if (!currentAppName) {
        alert("Please generate an app first.");
        return;
    }
    
    // Check if the app was actually created
    try {
        const response = await fetch(`/generated/${currentAppName}/index.html`);
        if (response.status === 200) {
            window.open(`/generated/${currentAppName}/index.html`, "_blank");
        } else {
            alert("App files not found. Please try generating again.");
        }
    } catch (error) {
        alert("App files not found. Please try generating again.");
    }
}

async function deployApp() {
    if (!currentAppName) return;
    
    const chatBox = document.getElementById("chatBox");
    chatBox.innerHTML += formatMessage(`
        Deployment Instructions:<br>
        1. Your app files are in the "generated/${currentAppName}" folder<br>
        2. Upload this folder to any web hosting service (Netlify, Vercel, GitHub Pages, etc.)<br>
        3. For full PWA functionality, ensure your server serves the manifest.json and service worker files correctly
    `, false);
    
    // Scroll to bottom of chat
    chatBox.scrollTop = chatBox.scrollHeight;
}

async function reworkApp(rework_prompt_from_url = null) {
    if (!currentAppName) {
        alert("Please generate an app first.");
        return;
    }

    const rework_prompt = rework_prompt_from_url || prompt("How would you like to revise your app?");
    if (!rework_prompt) return;

    const chatBox = document.getElementById("chatBox");
    chatBox.innerHTML += formatMessage(`Reworking app based on your request: "${rework_prompt}"`, false);
    
    // Show loading indicator
    document.getElementById("loadingIndicator").style.display = "flex";
    document.getElementById("actionButtons").style.display = "none";

    try {
        const response = await fetch("/rework", {
            method: "POST",
            headers: {
                "Content-Type": "application/json",
            },
            body: JSON.stringify({ 
                app_name: currentAppName,
                rework_prompt: rework_prompt 
            }),
        });
        
        const result = await response.json();
        
        if (result.status === "success") {
            let message = "Success! Your app has been reworked.";
            if (result.cached) {
                message += " (Used cached response to save costs)";
            }
            chatBox.innerHTML += formatMessage(message, false);
            
            // Show action buttons again
            document.getElementById("actionButtons").style.display = "flex";
        } else {
            chatBox.innerHTML += formatMessage(`Error: ${result.error}`, false);
        }
    } catch (error) {
        console.error("Error:", error);
        chatBox.innerHTML += formatMessage("Failed to rework the app.", false);
    } finally {
        // Hide loading indicator
        document.getElementById("loadingIndicator").style.display = "none";
        
        // Scroll to bottom of chat
        chatBox.scrollTop = chatBox.scrollHeight;
    }
}

// Initialize the app
document.addEventListener("DOMContentLoaded", function() {
    console.log("PWA Generator initialized");
    currentAppName = localStorage.getItem("currentAppName") || "";

    const urlParams = new URLSearchParams(window.location.search);
    const reworkAppParam = urlParams.get('rework_app');
    const reworkPromptParam = urlParams.get('rework_prompt');

    if (reworkAppParam && reworkPromptParam) {
        currentAppName = reworkAppParam;
        localStorage.setItem("currentAppName", currentAppName);
        // Call reworkApp with the prompt, but we need to modify reworkApp to accept a prompt
        reworkApp(reworkPromptParam);
    }
});