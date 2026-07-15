import os
import json
import contextvars
from google.adk import Agent, Runner
from google.adk.sessions.in_memory_session_service import InMemorySessionService
from google.genai import types

from core.config import settings
from core.llm_client import LLMClient
from services.image_generator import ImageGenerator

# Import tools and context vars
from tools.database_tool import query_database, tenant_id_var, access_token_var
from tools.image_tool import generate_image, generated_image_url_var
from tools.report_tool import generate_report, tenant_name_var, generated_report_url_var

# Configure Gemini API Key for ADK
if settings.GEMINI_API_KEY:
    os.environ["GEMINI_API_KEY"] = settings.GEMINI_API_KEY

class AutonomousAgent:
    def __init__(self, llm: LLMClient = None, image_gen: ImageGenerator = None):
        self.model_name = settings.GEMINI_MODEL or "gemini-1.5-flash"
        self.session_service = InMemorySessionService()

    async def _init_adk_runner(self, user_input: str, tenant_name: str, tenant_description: str, tenant_id: str, access_token: str):
        # Reset and set ContextVars
        tenant_id_var.set(str(tenant_id))
        access_token_var.set(access_token)
        tenant_name_var.set(tenant_name)
        generated_image_url_var.set(None)
        generated_report_url_var.set(None)

        system_prompt = f"""Bạn là một Trợ lý AI cao cấp cho doanh nghiệp '{tenant_name}'. 
Mô tả doanh nghiệp: {tenant_description}

Nhiệm vụ: Hỗ trợ các yêu cầu về chiến lược, nội dung, báo cáo và TRUY VẤN DỮ LIỆU THỰC TẾ. 

Quy tắc:
1. Phản hồi bằng tiếng Việt Markdown chuyên nghiệp.
2. Nếu người dùng hỏi về dữ liệu thực tế, hãy ưu tiên dùng 'query_database' để lấy thông tin chính xác trước khi trả lời.
3. Sử dụng lịch sử hội thoại để hiểu ngữ cảnh.
"""

        agent = Agent(
            name="autonomous_agent",
            model=self.model_name,
            instruction=system_prompt,
            tools=[query_database, generate_image, generate_report]
        )

        runner = Runner(
            app_name="ai_agent_service",
            agent=agent,
            session_service=self.session_service
        )

        session = await self.session_service.create_session(app_name="ai_agent_service", user_id="default_user")
        user_message = types.Content(role="user", parts=[types.Part.from_text(text=user_input)])

        return runner, session, user_message

    async def stream(self, user_input: str, tenant_name: str, tenant_description: str, tenant_id: str = "1", access_token: str = None, history: list = None):
        runner, session, user_message = await self._init_adk_runner(
            user_input, tenant_name, tenant_description, tenant_id, access_token
        )

        # In ADK 2.0, run_async drives the execution graph
        async for event in runner.run_async(user_id=session.user_id, session_id=session.id, new_message=user_message):
            if event.content and event.content.parts:
                for part in event.content.parts:
                    if part.text:
                        yield part.text

        # Yield any generated URLs at the end
        img_url = generated_image_url_var.get()
        rep_url = generated_report_url_var.get()
        if img_url or rep_url:
            yield {
                "text": "",
                "image_url": img_url,
                "report_url": rep_url
            }

    async def run(self, user_input: str, tenant_name: str, tenant_description: str, access_token: str = None, history: list = None) -> dict:
        tenant_id = "1"
        runner, session, user_message = await self._init_adk_runner(
            user_input, tenant_name, tenant_description, tenant_id, access_token
        )

        full_text = ""
        async for event in runner.run_async(user_id=session.user_id, session_id=session.id, new_message=user_message):
            if event.content and event.content.parts:
                for part in event.content.parts:
                    if part.text:
                        full_text += part.text

        img_url = generated_image_url_var.get()
        rep_url = generated_report_url_var.get()

        return {
            "agent_type": "autonomous",
            "full_response": full_text,
            "generated_image_url": img_url,
            "report_url": rep_url
        }
