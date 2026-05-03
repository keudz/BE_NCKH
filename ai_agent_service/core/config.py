from pydantic_settings import BaseSettings

class Settings(BaseSettings):
    # Primary Provider (Default)
    LLM_PROVIDER: str = "ollama"
    
    # Ollama / Gemma Config
    LLM_OLLAMA_BASE_URL: str = "http://localhost:11434"
    LLM_OLLAMA_MODEL: str = "gemma4:31b-cloud"
    
    # Gemini Config
    GEMINI_API_KEY: str = ""
    GEMINI_MODEL: str = "gemini-1.5-flash"

    # Nvidia Config (OpenAI Compatible)
    NVIDIA_API_KEY: str = ""
    NVIDIA_BASE_URL: str = "https://integrate.api.nvidia.com/v1"
    NVIDIA_MODEL: str = "meta/llama-3.1-8b-instruct"
    
    IMAGE_PROVIDER: str = "pollinations"
    IMAGE_API_KEY: str = ""
    BACKEND_URL: str = "http://localhost:8080"

    class Config:
        env_file = ".env"

settings = Settings()
