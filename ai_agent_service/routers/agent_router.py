from fastapi import APIRouter
from models.request_models import AgentRequest
from models.response_models import PipelineResult
from agents.autonomous_agent import AutonomousAgent
from core.llm_client import LLMClient
from core.config import settings
from services.image_generator import ImageGenerator

router = APIRouter()

# Init Clients
llm_ollama = LLMClient(
    provider="ollama", 
    base_url=settings.LLM_OLLAMA_BASE_URL, 
    model=settings.LLM_OLLAMA_MODEL
)

llm_gemini = LLMClient(
    provider="gemini", 
    base_url="", 
    model=settings.GEMINI_MODEL,
    api_key=settings.GEMINI_API_KEY
)

image_gen = ImageGenerator(provider=settings.IMAGE_PROVIDER, api_key=settings.IMAGE_API_KEY)

# Unified Autonomous Agents for both providers
agent_ollama = AutonomousAgent(llm_ollama, image_gen)
agent_gemini = AutonomousAgent(llm_gemini, image_gen)

@router.post("/execute", response_model=PipelineResult)
async def execute_agent(request: AgentRequest):
    """Chạy AI Agent tự chủ (Autonomous)."""
    target_agent = agent_gemini if settings.LLM_PROVIDER.lower() == "gemini" else agent_ollama
    
    result = await target_agent.run(
        request.prompt,
        tenant_name=request.tenant_name,
        tenant_description=request.tenant_description
    )
    return PipelineResult(**result)

