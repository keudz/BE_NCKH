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
    
    IMAGE_PROVIDER: str = "pollinations"
    IMAGE_API_KEY: str = "mock_key"
    BACKEND_URL: str = "http://localhost:8080"

    class Config:
        env_file = ".env"

settings = Settings()
