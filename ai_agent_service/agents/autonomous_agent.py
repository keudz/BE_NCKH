from core.llm_client import LLMClient
from tools.image_tool import ImageTool
from tools.report_tool import ReportTool
from services.image_generator import ImageGenerator

class AutonomousAgent:
    def __init__(self, llm: LLMClient, image_gen: ImageGenerator):
        self.llm = llm
        self.image_tool = ImageTool(image_gen)
        self.report_tool = ReportTool()
        self.tools = [self.image_tool, self.report_tool]
        
    async def run(self, user_input: str, tenant_name: str, tenant_description: str) -> dict:
        system_prompt = f"""Bạn là một Trợ lý AI cao cấp cho doanh nghiệp '{tenant_name}'. 
Mô tả doanh nghiệp: {tenant_description}

Nhiệm vụ: Hỗ trợ các yêu cầu về chiến lược, nội dung và báo cáo. 
Quyền hạn: Gọi giải pháp 'generate_image' để tạo ảnh/poster và 'generate_report' để tạo báo cáo Word.

Quy tắc:
1. Luôn ưu tiên dùng Tool nếu yêu cầu liên quan đến tạo ảnh hoặc xuất file.
2. Phản hồi bằng tiếng Việt Markdown chuyên nghiệp.
3. Nếu dùng Tool, hãy đưa link kết quả vào cuối câu trả lời.
"""
        # Execute Chat
        response = await self.llm.chat_with_tools(system_prompt, user_input, self.tools)
        
        results = {
            "agent_type": "autonomous",
            "full_response": getattr(response, 'text', ""),
            "generated_image_url": None,
            "report_url": None
        }

        # Xử lý Tool Calling cho Ollama (Manual Execution)
        if hasattr(response, 'tool_calls') and response.tool_calls:
            for call in response.tool_calls:
                func_name = call['function']['name']
                args = call['function']['arguments']
                
                if func_name == "generate_image":
                    url = await self.image_tool.run(**args)
                    results["generated_image_url"] = url.split("URL: ")[1] if "URL: " in url else None
                    results["full_response"] += f"\n\n**[Hệ thống]** Đã tạo ảnh thành công: {results['generated_image_url']}"
                
                elif func_name == "generate_report":
                    url = await self.report_tool.run(**args)
                    results["report_url"] = url.split("tải về tại: ")[1] if "tải về tại: " in url else None
                    results["full_response"] += f"\n\n**[Hệ thống]** Đã xuất báo cáo thành công: [Tải về]({results['report_url']})"


        elif "URL: " in results["full_response"]:
            import re
            img_match = re.search(r"URL: (\S+)", results["full_response"])
            if img_match: results["generated_image_url"] = img_match.group(1)
            
            report_match = re.search(r"tải về tại: (\S+)", results["full_response"])
            if report_match: results["report_url"] = report_match.group(1)
        
        return results
