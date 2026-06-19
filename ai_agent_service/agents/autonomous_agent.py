import json
from core.llm_client import LLMClient
from tools.image_tool import ImageTool
from tools.report_tool import ReportTool
from tools.database_tool import DatabaseQueryTool
from services.image_generator import ImageGenerator

class AutonomousAgent:
    def __init__(self, llm: LLMClient, image_gen: ImageGenerator):
        self.llm = llm
        self.image_tool = ImageTool(image_gen)
        self.report_tool = ReportTool()
        self.db_tool = DatabaseQueryTool()
        self.tools = [self.image_tool, self.report_tool, self.db_tool]
        
    async def stream(self, user_input: str, tenant_name: str, tenant_description: str, tenant_id: str = "1", access_token: str = None, history: list = None):
        system_prompt = f"""Bạn là một Trợ lý AI cao cấp cho doanh nghiệp '{tenant_name}'. 
Mô tả doanh nghiệp: {tenant_description}

Nhiệm vụ: Hỗ trợ các yêu cầu về chiến lược, nội dung, báo cáo và TRUY VẤN DỮ LIỆU THỰC TẾ. 

Công cụ có sẵn:
1. 'generate_image': Tạo hình ảnh, poster quảng cáo.
2. 'generate_report': Tạo báo cáo Word chuyên nghiệp.
3. 'query_database': Truy vấn dữ liệu từ hệ thống. Các loại dữ liệu (data_category) gồm: 
   - 'tasks': Danh sách công việc, nhiệm vụ.
   - 'attendance': Dữ liệu chấm công, đi muộn/về sớm.
   - 'products': Danh sách sản phẩm, hàng hóa.
   - 'invoices': Danh sách hóa đơn, chứng từ.
   - 'employees': Danh sách nhân sự, nhân viên.
   - 'customers': Danh sách khách hàng.
   - 'summary': Thống kê tổng quan dashboard.

Quy tắc:
1. Phản hồi bằng tiếng Việt Markdown chuyên nghiệp.
2. Nếu người dùng hỏi về dữ liệu thực tế, hãy ưu tiên dùng 'query_database' để lấy thông tin chính xác trước khi trả lời.
3. Sử dụng lịch sử hội thoại để hiểu ngữ cảnh.
"""
        # Kiểm tra xem có cần dùng Tool không (Lượt 1 - Non-streaming)
        response = await self.llm.chat_with_tools(system_prompt, user_input, self.tools, history=history)
        
        tool_outputs = []
        is_tool_called = False

        if hasattr(response, 'tool_calls') and response.tool_calls:
            is_tool_called = True
            for call in response.tool_calls:
                func_name = call['function']['name']
                raw_args = call['function']['arguments']
                
                try:
                    args = json.loads(raw_args) if isinstance(raw_args, str) else raw_args
                except:
                    args = {}

                if func_name == "query_database":
                    yield "🔍 *Đang truy vấn dữ liệu từ hệ thống...*\n\n"
                    try:
                        data_res = await self.db_tool.run(**args, tenant_id=tenant_id, access_token=access_token)
                        tool_outputs.append(data_res)
                    except Exception as e:
                        print(f"DB TOOL ERROR: {str(e)}")
                        tool_outputs.append(f"Lỗi khi truy vấn: {str(e)}")
                
                elif func_name == "generate_report":
                    try:
                        if 'tenant_name' not in args: args['tenant_name'] = tenant_name
                        res = await self.report_tool.run(**args)
                        url = res.split("tải về tại: ")[1] if "tải về tại: " in res else None
                        yield {"text": f"✅ {res}\n\n", "report_url": url}
                    except Exception as e:
                        yield {"text": f"❌ Lỗi khi tạo báo cáo: {str(e)}\n\n"}

                elif func_name == "generate_image":
                    try:
                        res = await self.image_tool.run(**args)
                        url = res.split("URL: ")[1] if "URL: " in res else None
                        yield {"text": f"✅ {res}\n\n", "image_url": url}
                    except Exception as e:
                        yield {"text": f"❌ Lỗi khi tạo ảnh: {str(e)}\n\n"}
            
            if tool_outputs:
                yield "📝 *Đang phân tích dữ liệu và soạn câu trả lời...*\n\n"
                combined_context = "\n".join(tool_outputs)
                final_prompt = f"Dựa trên dữ liệu sau đây từ hệ thống, hãy trả lời câu hỏi: '{user_input}'\n\nDữ liệu: {combined_context}"
                async for chunk in self.llm.stream_chat(system_prompt, final_prompt, history=history):
                    if chunk:
                        yield chunk
                return

        # Nếu không có tool nào được gọi hoặc tool sinh file đã xong, nhưng vẫn cần phản hồi văn bản
        if is_tool_called and not tool_outputs:
             # Nếu đã gọi tool sinh file/ảnh thì có thể kết thúc hoặc tiếp tục tùy LLM. 
             # Thường LLM sẽ có text phản hồi đi kèm.
             if getattr(response, 'text', ""):
                 yield response.text
             return

        # Nếu không dùng tool hoặc LLM trả về text ngay
        if getattr(response, 'text', ""):
            yield response.text
        else:
            # Nếu lượt 1 trống, gọi stream_chat trực tiếp
            async for chunk in self.llm.stream_chat(system_prompt, user_input, history=history):
                if chunk:
                    yield chunk

    async def run(self, user_input: str, tenant_name: str, tenant_description: str, access_token: str = None, history: list = None) -> dict:
        tenant_id = "1"
        system_prompt = f"""Bạn là một Trợ lý AI cao cấp cho doanh nghiệp '{tenant_name}'. 
Mô tả doanh nghiệp: {tenant_description}

Nhiệm vụ: Hỗ trợ các yêu cầu về chiến lược, nội dung, báo cáo và TRUY VẤN DỮ LIỆU THỰC TẾ. 

Công cụ có sẵn:
1. 'generate_image': Tạo hình ảnh, poster quảng cáo.
2. 'generate_report': Tạo báo cáo Word chuyên nghiệp.
3. 'query_database': Truy vấn dữ liệu thực tế. Các loại dữ liệu (data_category) gồm: 
   - 'tasks': Công việc.
   - 'attendance': Chấm công.
   - 'products': Sản phẩm.
   - 'invoices': Hóa đơn.
   - 'employees': Nhân sự.
   - 'customers': Khách hàng.
   - 'summary': Thống kê tổng quan.

Quy tắc:
1. Luôn dùng 'query_database' khi người dùng hỏi về thông tin thực tế trong hệ thống.
2. Phản hồi bằng tiếng Việt Markdown.
3. Sử dụng lịch sử hội thoại để giữ ngữ cảnh.
"""
        # Execute Chat (Lượt 1)
        response = await self.llm.chat_with_tools(system_prompt, user_input, self.tools, history=history)
        
        results = {
            "agent_type": "autonomous",
            "full_response": getattr(response, 'text', ""),
            "generated_image_url": None,
            "report_url": None
        }

        # Xử lý Tool Calling
        if hasattr(response, 'tool_calls') and response.tool_calls:
            tool_outputs = []
            for call in response.tool_calls:
                func_name = call['function']['name']
                raw_args = call['function']['arguments']
                
                # Parse arguments if it's a string
                try:
                    args = json.loads(raw_args) if isinstance(raw_args, str) else raw_args
                except:
                    args = {}

                if func_name == "generate_image":
                    url = await self.image_tool.run(**args)
                    results["generated_image_url"] = url.split("URL: ")[1] if "URL: " in url else None
                    results["full_response"] += f"\n\n**[Hệ thống]** Đã tạo ảnh thành công: {results['generated_image_url']}"
                
                elif func_name == "generate_report":
                    if 'tenant_name' not in args: args['tenant_name'] = tenant_name
                    url = await self.report_tool.run(**args)
                    results["report_url"] = url.split("tải về tại: ")[1] if "tải về tại: " in url else None
                    results["full_response"] += f"\n\n**[Hệ thống]** Đã xuất báo cáo thành công: [Tải về]({results['report_url']})"
                
                elif func_name == "query_database":
                    try:
                        data_res = await self.db_tool.run(**args, tenant_id=tenant_id, access_token=access_token)
                        tool_outputs.append(data_res)
                    except Exception as e:
                        tool_outputs.append(f"Lỗi khi truy vấn: {str(e)}")
            
            # Nếu có dữ liệu từ DB, gọi LLM lượt 2 để tổng hợp câu trả lời
            if tool_outputs:
                combined_context = "\n".join(tool_outputs)
                final_prompt = f"Dựa trên dữ liệu sau đây từ hệ thống, hãy trả lời câu hỏi của người dùng: '{user_input}'\n\nDữ liệu: {combined_context}"
                final_response = await self.llm.chat(system_prompt, final_prompt, history=history)
                results["full_response"] = final_response

        elif "URL: " in results["full_response"]:
            # Logic cũ cho Regex (phòng hờ)
            import re
            img_match = re.search(r"URL: (\S+)", results["full_response"])
            if img_match: results["generated_image_url"] = img_match.group(1)
            report_match = re.search(r"tải về tại: (\S+)", results["full_response"])
            if report_match: results["report_url"] = report_match.group(1)
        
        return results
