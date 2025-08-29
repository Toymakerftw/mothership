let currentAppName = "";
let currentResponse = "";

// Add event listener for Enter key
document.getElementById("userInput").addEventListener("keypress", function(e) {
    if (e.key === "Enter") {
        sendPrompt();
    }
});

async function sendPrompt() {
    const userInput = document.getElementById("userInput").value.trim();
    if (!userInput) return;

    // Display user message
    const chatBox = document.getElementById("chatBox");
    chatBox.innerHTML += `<div class="message user-message"><strong>You:</strong> ${userInput}</div>`;
    
    // Clear input
    document.getElementById("userInput").value = "";
    
    // Show loading indicator
    document.getElementById("loadingIndicator").style.display = "flex";
    document.getElementById("sendButton").disabled = true;

    try {
        // Call Flask backend
        const response = await fetch("/chat", {
            method: "POST",
            headers: {
                "Content-Type": "application/json",
            },
            body: JSON.stringify({ prompt: userInput }),
        });
        
        const result = await response.json();
        
        if (result.error) {
            chatBox.innerHTML += `<div class="message bot-message"><strong>Error:</strong> ${result.error}</div>`;
        } else {
            currentResponse = result.response;
            chatBox.innerHTML += `<div class="message bot-message"><strong>PWA Generator:</strong> I've created a plan for your "${userInput}" PWA. Click "Generate Files" to create the application.</div>`;
            
            // Show action buttons
            document.getElementById("actionButtons").style.display = "flex";
            currentAppName = userInput.replace(/[^a-z0-9\-]/g, '').toLowerCase();
        }
    } catch (error) {
        console.error("Error:", error);
        chatBox.innerHTML += `<div class="message bot-message"><strong>Error:</strong> Failed to communicate with the server.</div>`;
    } finally {
        // Hide loading indicator
        document.getElementById("loadingIndicator").style.display = "none";
        document.getElementById("sendButton").disabled = false;
        
        // Scroll to bottom of chat
        chatBox.scrollTop = chatBox.scrollHeight;
    }
}

async function generateApp() {
    if (!currentResponse) return;

    const chatBox = document.getElementById("chatBox");
    chatBox.innerHTML += `<div class="message bot-message"><strong>PWA Generator:</strong> Generating files...</div>`;
    
    // Show loading indicator
    document.getElementById("loadingIndicator").style.display = "flex";
    document.getElementById("actionButtons").style.display = "none";

    try {
        const response = await fetch("/generate", {
            method: "POST",
            headers: {
                "Content-Type": "application/json",
            },
            body: JSON.stringify({ 
                prompt: document.getElementById("userInput").dataset.originalPrompt || document.getElementById("userInput").value || "pwa-app",
                app_name: currentAppName || (document.getElementById("userInput").value || "pwa-app").replace(/[^a-z0-9\-]/g, '').toLowerCase(),
                response_text: currentResponse 
            }),
        });
        
        const result = await response.json();
        
        if (result.status === "success") {
            chatBox.innerHTML += `<div class="message bot-message"><strong>Success!</strong> Created files: ${result.files.join(", ")}</div>`;
            chatBox.innerHTML += `<div class="message bot-message">You can now preview your app or get deployment instructions.</div>`;
            
            // Update current app name
            currentAppName = result.app_name;
            
            // Show action buttons again
            document.getElementById("actionButtons").style.display = "flex";
        } else {
            chatBox.innerHTML += `<div class="message bot-message"><strong>Error:</strong> ${result.message}</div>`;
        }
    } catch (error) {
        console.error("Error:", error);
        chatBox.innerHTML += `<div class="message bot-message"><strong>Error:</strong> Failed to generate files.</div>`;
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
    chatBox.innerHTML += `
        <div class="message bot-message">
            <strong>Deployment Instructions:</strong><br>
            1. Your app files are in the "generated/${currentAppName}" folder<br>
            2. Upload this folder to any web hosting service (Netlify, Vercel, GitHub Pages, etc.)<br>
            3. For full PWA functionality, ensure your server serves the manifest.json and service worker files correctly
        </div>`;
    
    // Scroll to bottom of chat
    chatBox.scrollTop = chatBox.scrollHeight;
}

async function reworkApp() {
    const userInput = prompt("How would you like to revise your app?");
    if (userInput) {
        document.getElementById("userInput").value = userInput;
        sendPrompt();
    }
}

// Initialize the app
document.addEventListener("DOMContentLoaded", function() {
    console.log("PWA Generator initialized");
});