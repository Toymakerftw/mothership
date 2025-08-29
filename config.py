# config.py
import os
from dotenv import load_dotenv

load_dotenv()

# API Configuration
OPENROUTER_API_KEY = os.getenv("OPENROUTER_API_KEY")
OPENROUTER_API_URL = "https://openrouter.ai/api/v1/chat/completions"

# Model Selection
OPTIMIZER_MODEL = os.getenv("OPTIMIZER_MODEL", "mistralai/mistral-small-3.2-24b-instruct:free")
GENERATOR_MODEL = os.getenv("GENERATOR_MODEL", "qwen/qwen-2.5-coder-32b-instruct:free")
VERIFIER_MODEL = os.getenv("VERIFIER_MODEL", "deepseek/deepseek-chat-v3.1:free")

# Other Settings
MAX_VERIFICATION_ITERATIONS = 3
REQUEST_TIMEOUT = 30