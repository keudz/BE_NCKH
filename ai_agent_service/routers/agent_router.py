from fastapi import APIRouter
from fastapi.responses import StreamingResponse
import json
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

llm_nvidia = LLMClient(
    provider="nvidia",
    base_url=settings.NVIDIA_BASE_URL,
    model=settings.NVIDIA_MODEL,
    api_key=settings.NVIDIA_API_KEY
)

image_gen = ImageGenerator(provider=settings.IMAGE_PROVIDER, api_key=settings.IMAGE_API_KEY)

# Unified Autonomous Agents for all providers
agent_ollama = AutonomousAgent(llm_ollama, image_gen)
agent_gemini = AutonomousAgent(llm_gemini, image_gen)
agent_nvidia = AutonomousAgent(llm_nvidia, image_gen)

@router.post("/execute", response_model=PipelineResult)
async def execute_agent(request: AgentRequest):
    """Chạy AI Agent tự chủ (Autonomous)."""
    provider = settings.LLM_PROVIDER.lower()
    if provider == "gemini":
        target_agent = agent_gemini
    elif provider == "nvidia":
        target_agent = agent_nvidia
    else:
        target_agent = agent_ollama
    
    result = await target_agent.run(
        request.prompt,
        tenant_name=request.tenant_name,
        tenant_description=request.tenant_description,
        access_token=request.access_token,
        history=request.history
    )
    return PipelineResult(**result)

@router.post("/stream")
async def stream_agent(request: AgentRequest):
    """Chạy AI Agent với phản hồi trực tiếp (Streaming)."""
    provider = settings.LLM_PROVIDER.lower()
    if provider == "gemini":
        target_agent = agent_gemini
    elif provider == "nvidia":
        target_agent = agent_nvidia
    else:
        target_agent = agent_ollama
    
    async def event_generator():
        try:
            async for chunk in target_agent.stream(
                request.prompt,
                tenant_name=request.tenant_name,
                tenant_description=request.tenant_description,
                tenant_id=str(request.tenant_id or "1"),
                access_token=request.access_token,
                history=request.history
            ):
                if chunk:
                    if isinstance(chunk, dict):
                        yield f"data: {json.dumps(chunk, ensure_ascii=False)}\n\n"
                    else:
                        yield f"data: {json.dumps({'text': chunk}, ensure_ascii=False)}\n\n"
            
            yield "data: [DONE]\n\n"
        except Exception as e:
            print(f"STREAM ERROR: {str(e)}")
            import traceback
            traceback.print_exc()
            error_msg = json.dumps({'text': f"\n\n**[Lỗi hệ thống]**: {str(e)}"}, ensure_ascii=False)
            yield f"data: {error_msg}\n\n"
            yield "data: [DONE]\n\n"

    return StreamingResponse(
        event_generator(), 
        media_type="text/event-stream",
        headers={
            "Cache-Control": "no-cache",
            "Connection": "keep-alive",
            "X-Accel-Buffering": "no"
        }
    )

