import time
from tools.base import BaseTool, ToolDefinition
from services.document_service import DocumentService

class ReportTool(BaseTool):
    def __init__(self):
        super().__init__()
        self.doc_service = DocumentService()

    def get_definition(self) -> ToolDefinition:
        return ToolDefinition(
            name="generate_report",
            description="Tạo báo cáo doanh nghiệp chuyên nghiệp dưới dạng file Word (.docx).",
            parameters={
                "type": "object",
                "properties": {
                    "content": {
                        "type": "string",
                        "description": "Nội dung báo cáo chi tiết (Markdown format)."
                    },
                    "title": {
                        "type": "string",
                        "description": "Tiêu đề của báo cáo."
                    }
                },
                "required": ["content"]
            }
        )

    async def run(self, content: str, title: str = "Báo cáo doanh nghiệp", tenant_name: str = "Doanh nghiệp") -> str:
        import time
        filename = f"BAO_CAO_{tenant_name}_{int(time.time())}.docx"
        filepath = self.doc_service.generate_docx(content, filename, title)
        url = f"http://localhost:8001/static/{filename}"
        return f"Đã tạo báo cáo thành công. Người dùng có thể tải về tại: {url}"
