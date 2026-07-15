import time
import contextvars
from services.document_service import DocumentService

# Request-scoped context
tenant_name_var = contextvars.ContextVar("tenant_name", default="Doanh nghiệp")
generated_report_url_var = contextvars.ContextVar("generated_report_url", default=None)

doc_service = DocumentService()

async def generate_report(content: str, title: str = "Báo cáo doanh nghiệp") -> dict:
    """Tạo báo cáo doanh nghiệp chuyên nghiệp dưới dạng file Word (.docx).
    
    Args:
        content: Nội dung báo cáo chi tiết dưới định dạng Markdown.
        title: Tiêu đề của báo cáo.
    """
    tenant_name = tenant_name_var.get()
    try:
        filename = f"BAO_CAO_{tenant_name}_{int(time.time())}.docx"
        filepath = doc_service.generate_docx(content, filename, title)
        url = f"http://localhost:8001/static/{filename}"
        generated_report_url_var.set(url)
        return {"status": "success", "report_url": url, "message": f"Đã tạo báo cáo thành công. Người dùng có thể tải về tại: {url}"}
    except Exception as e:
        return {"status": "error", "message": f"Lỗi khi tạo báo cáo: {str(e)}"}
