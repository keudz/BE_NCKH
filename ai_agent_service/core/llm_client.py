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

    async def chat(self, system_prompt: str, user_message: str, history: list = None) -> str:
        """Chat thông thường (Non-streaming) với hỗ trợ history"""
        messages = [{"role": "system", "content": system_prompt}]
        if history:
            messages.extend(history)
        messages.append({"role": "user", "content": user_message})

        if self.provider == "ollama":
            url = f"{self.base_url}/api/chat"
            payload = {
                "model": self.model_name,
                "messages": messages,
                "stream": False
            }
            async with httpx.AsyncClient(timeout=120.0) as client:
                response = await client.post(url, json=payload)
                response.raise_for_status()
                data = response.json()
                return data.get("message", {}).get("content", "")
        
        elif self.provider == "gemini":
            chat = self.model.start_chat(history=[]) # Could convert history list here
            # For simplicity, concatenate history for now if not using SDK history
            full_context = f"SYSTEM: {system_prompt}\n\n"
            if history:
                for m in history:
                    role = "USER" if m['role'] == 'user' else "AI"
                    full_context += f"{role}: {m['content']}\n"
            full_context += f"USER: {user_message}"
            response = await chat.send_message_async(full_context)
            return response.text

        elif self.provider == "nvidia" or self.provider == "openai":
            url = f"{self.base_url}/chat/completions"
            headers = {
                "Authorization": f"Bearer {self.api_key}",
                "Content-Type": "application/json"
            }
            payload = {
                "model": self.model_name,
                "messages": messages,
                "stream": False
            }
            async with httpx.AsyncClient(timeout=120.0) as client:
                response = await client.post(url, json=payload, headers=headers)
                response.raise_for_status()
                data = response.json()
                return data["choices"][0]["message"]["content"]
            
        else:
            raise NotImplementedError(f"Provider {self.provider} not supported.")

    async def stream_chat(self, system_prompt: str, user_message: str, history: list = None):
        """Chat dưới dạng Stream với hỗ trợ history"""
        messages = [{"role": "system", "content": system_prompt}]
        if history:
            messages.extend(history)
        messages.append({"role": "user", "content": user_message})

        if self.provider == "gemini":
            full_context = f"SYSTEM: {system_prompt}\n\n"
            if history:
                for m in history:
                    role = "USER" if m['role'] == 'user' else "AI"
                    full_context += f"{role}: {m['content']}\n"
            full_context += f"USER: {user_message}"
            response = await self.model.generate_content_async(full_context, stream=True)
            async for chunk in response:
                if chunk.text:
                    yield chunk.text

        elif self.provider == "ollama":
            url = f"{self.base_url}/api/chat"
            payload = {
                "model": self.model_name,
                "messages": messages,
                "stream": True
            }
            async with httpx.AsyncClient(timeout=120.0) as client:
                async with client.stream("POST", url, json=payload) as response:
                    async for line in response.aiter_lines():
                        if line:
                            try:
                                chunk = json.loads(line)
                                if "message" in chunk:
                                    yield chunk["message"].get("content", "")
                            except: continue

        elif self.provider == "nvidia" or self.provider == "openai":
            url = f"{self.base_url}/chat/completions"
            headers = {"Authorization": f"Bearer {self.api_key}"}
            payload = {
                "model": self.model_name,
                "messages": messages,
                "stream": True
            }
            async with httpx.AsyncClient(timeout=120.0) as client:
                async with client.stream("POST", url, json=payload, headers=headers) as response:
                    async for line in response.aiter_lines():
                        if line.startswith("data: "):
                            data_str = line[6:].strip()
                            if data_str == "[DONE]": break
                            try:
                                chunk = json.loads(data_str)
                                if "choices" in chunk:
                                    content = chunk["choices"][0].get("delta", {}).get("content", "")
                                    if content: yield content
                            except: continue
        else:
            # Fallback về chat thường nếu không hỗ trợ stream
            content = await self.chat(system_prompt, user_message)
            yield content

    async def chat_with_tools(self, system_prompt: str, user_message: str, tools: list, history: list = None) -> any:
        """Thực hiện chat với khả năng gọi hàm (Function Calling) và history."""
        if self.provider == "gemini":
            import google.generativeai as genai
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
            
            # Convert history to Gemini format
            gemini_history = []
            if history:
                for m in history:
                    role = "user" if m['role'] == 'user' else "model"
                    gemini_history.append({"role": role, "parts": [m['content']]})
            
            chat = model.start_chat(history=gemini_history, enable_automatic_function_calling=True)
            response = await chat.send_message_async(user_message)
            return response
            
        elif self.provider == "ollama":
            url = f"{self.base_url}/api/chat"
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
            
            messages = [{"role": "system", "content": system_prompt}]
            if history:
                messages.extend(history)
            messages.append({"role": "user", "content": user_message})

            payload = {
                "model": self.model_name,
                "messages": messages,
                "tools": ollama_tools,
                "stream": False
            }
            async with httpx.AsyncClient(timeout=150.0) as client:
                response = await client.post(url, json=payload)
                response.raise_for_status()
                data = response.json()
                
                class MockResponse:
                    def __init__(self, content, tool_calls=None):
                        self.text = content
                        self.tool_calls = tool_calls
                
                message = data.get("message", {})
                return MockResponse(message.get("content", ""), message.get("tool_calls"))

        elif self.provider == "nvidia" or self.provider == "openai":
            url = f"{self.base_url}/chat/completions"
            headers = {
                "Authorization": f"Bearer {self.api_key}",
                "Content-Type": "application/json"
            }
            openai_tools = [
                {
                    "type": "function",
                    "function": {
                        "name": t.definition.name,
                        "description": t.definition.description,
                        "parameters": t.definition.parameters
                    }
                } for t in tools
            ]
            
            messages = [{"role": "system", "content": system_prompt}]
            if history:
                messages.extend(history)
            messages.append({"role": "user", "content": user_message})

            payload = {
                "model": self.model_name,
                "messages": messages,
                "tools": openai_tools,
                "tool_choice": "auto",
                "stream": False
            }
            async with httpx.AsyncClient(timeout=150.0) as client:
                response = await client.post(url, json=payload, headers=headers)
                response.raise_for_status()
                data = response.json()
                
                class MockResponse:
                    def __init__(self, content, tool_calls=None):
                        self.text = content
                        self.tool_calls = tool_calls
                
                msg_obj = data.get("choices", [{}])[0].get("message", {})
                return MockResponse(msg_obj.get("content", ""), msg_obj.get("tool_calls"))
        
        else:
            return await self.chat(system_prompt, user_message, history=history)
