import httpx
import json

class LLMClient:
    def __init__(self, provider: str, base_url: str, model: str, api_key: str = ""):
        self.provider = provider.lower()
        self.base_url = base_url
        self.model_name = model
        self.api_key = api_key
        
        if self.provider == "gemini":
            import google.generativeai as genai
            genai.configure(api_key=self.api_key)
            self.model = genai.GenerativeModel(self.model_name)

    async def chat(self, system_prompt: str, user_message: str) -> str:
        if self.provider == "ollama":
            url = f"{self.base_url}/api/chat"
            payload = {
                "model": self.model_name,
                "messages": [
                    {"role": "system", "content": system_prompt},
                    {"role": "user", "content": user_message}
                ],
                "stream": False
            }
            
            async with httpx.AsyncClient(timeout=120.0) as client:
                response = await client.post(url, json=payload)
                response.raise_for_status()
                data = response.json()
                return data.get("message", {}).get("content", "")
        
        elif self.provider == "gemini":
            # Gemini system instruction is handled via GenerativeModel(system_instruction=...) 
            # or by prefixing the message. For simplicity here:
            chat = self.model.start_chat()
            full_prompt = f"SYSTEM: {system_prompt}\n\nUSER: {user_message}"
            response = await chat.send_message_async(full_prompt)
            return response.text
            
        else:
            raise NotImplementedError(f"Provider {self.provider} not supported.")

    async def chat_with_tools(self, system_prompt: str, user_message: str, tools: list) -> any:
        """Thực hiện chat với khả năng gọi hàm (Function Calling)."""
        if self.provider == "gemini":
            import google.generativeai as genai
            # ... (giữ nguyên logic gemini cũ)
            gemini_tools = [
                genai.types.FunctionDeclaration(
                    name=t.definition.name,
                    description=t.definition.description,
                    parameters=t.definition.parameters
                ) for t in tools
            ]
            model = genai.GenerativeModel(
                model_name=self.model_name,
                tools=[genai.types.Tool(function_declarations=gemini_tools)],
                system_instruction=system_prompt
            )
            chat = model.start_chat(enable_automatic_function_calling=True)
            response = await chat.send_message_async(user_message)
            return response
            
        elif self.provider == "ollama":
            # Ollama Tool Calling (Thực hiện qua định dạng Chat API)
            url = f"{self.base_url}/api/chat"
            
            # Chuyển đổi tools sang định dạng Ollama/OpenAI
            ollama_tools = [
                {
                    "type": "function",
                    "function": {
                        "name": t.definition.name,
                        "description": t.definition.description,
                        "parameters": t.definition.parameters
                    }
                } for t in tools
            ]
            
            payload = {
                "model": self.model_name,
                "messages": [
                    {"role": "system", "content": system_prompt},
                    {"role": "user", "content": user_message}
                ],
                "tools": ollama_tools,
                "stream": False
            }
            
            async with httpx.AsyncClient(timeout=150.0) as client:
                response = await client.post(url, json=payload)
                response.raise_for_status()
                data = response.json()
                
                # Tạo một object giả lập response để AutonomousAgent dễ xử lý
                class MockResponse:
                    def __init__(self, content, tool_calls=None):
                        self.text = content
                        self.tool_calls = tool_calls
                
                message = data.get("message", {})
                return MockResponse(message.get("content", ""), message.get("tool_calls"))
        
        else:
            return await self.chat(system_prompt, user_message)
