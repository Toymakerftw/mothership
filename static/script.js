let currentAppName = "";
let currentResponse = "";

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

async function sendPrompt() {
    const userInput = document.getElementById("userInput").value.trim();
    if (!userInput) return;

    // Display user message
    const chatBox = document.getElementById("chatBox");
    chatBox.innerHTML += formatMessage(userInput, true);
    
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
            chatBox.innerHTML += formatMessage(`Error: ${result.error}`, false);
        } else {
            currentResponse = result.response;
            chatBox.innerHTML += formatMessage(`I've created a plan for your "${userInput}" PWA. Click "Compile App" to create the application.`, false);
            
            // Show action buttons
            document.getElementById("actionButtons").style.display = "flex";
            currentAppName = userInput.replace(/[^a-z0-9\-]/g, '').toLowerCase();
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

async function generateApp() {
    if (!currentResponse) return;

    const chatBox = document.getElementById("chatBox");
    chatBox.innerHTML += formatMessage("Generating files...", false);
    
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