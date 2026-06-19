import httpx
import json
import datetime
from tools.base import BaseTool, ToolDefinition
from core.config import settings

class DatabaseQueryTool(BaseTool):
    def __init__(self):
        super().__init__()
        self.backend_url = settings.BACKEND_URL

    def get_definition(self) -> ToolDefinition:
        return ToolDefinition(
            name="query_database",
            description="Truy vấn dữ liệu thực tế từ hệ thống doanh nghiệp (nhân viên, công việc, chấm công, sản phẩm, hóa đơn, thống kê).",
            parameters={
                "type": "object",
                "properties": {
                    "data_category": {
                        "type": "string",
                        "enum": ["tasks", "attendance", "products", "invoices", "employees", "customers", "summary"],
                        "description": "Loại dữ liệu cần lấy: tasks (công việc), attendance (chấm công), products (sản phẩm), invoices (hóa đơn), employees (nhân sự), customers (khách hàng), summary (thống kê tổng quan)."
                    },
                    "search_query": {
                        "type": "string",
                        "description": "Từ khóa tìm kiếm hoặc tham số bổ sung (ví dụ: tên nhân viên, mã sản phẩm). Để trống nếu muốn lấy toàn bộ danh sách."
                    }
                },
                "required": ["data_category"]
            }
        )

    async def run(self, data_category: str, search_query: str = None, tenant_id: str = "1", access_token: str = None) -> str:
        # Mapping từ category sang API endpoint thực tế
        mapping = {
            "tasks": f"/api/v1/tasks/tenant/{tenant_id}",
            "attendance": "/api/v1/attendance/tenant-history",
            "products": "/api/v1/products",
            "invoices": "/api/v1/invoices",
            "employees": "/api/v1/users",
            "customers": "/api/v1/customers",
            "summary": "/api/v1/dashboard/summary"
        }

        endpoint = mapping.get(data_category)
        if not endpoint:
            return f"Lỗi: Loại dữ liệu '{data_category}' không hợp lệ."

        url = f"{self.backend_url}{endpoint}"
        params = {}

        # Xử lý tham số đặc biệt cho từng loại
        if data_category == "attendance":
            # Mặc định lấy tháng và năm hiện tại
            now = datetime.datetime.now()
            params["month"] = now.month
            params["year"] = now.year
        
        if search_query:
            params["search"] = search_query # Giả định backend hỗ trợ search query

        headers = {
            "X-Tenant-Id": str(tenant_id),
            "Content-Type": "application/json"
        }
        if access_token:
            headers["Authorization"] = f"Bearer {access_token}"
        
        print(f"DEBUG: AI Agent calling {data_category} -> {url}")
        
        try:
            async with httpx.AsyncClient(timeout=30.0) as client:
                response = await client.get(url, params=params, headers=headers)
                
                if response.status_code == 401:
                    return "Lỗi xác thực (401): Token không hợp lệ hoặc đã hết hạn."
                
                response.raise_for_status()
                data = response.json()
                
                # Trích xuất phần data nếu backend trả về theo cấu trúc ApiResponse
                actual_data = data.get("data", data)
                
                # Nếu dữ liệu quá lớn, chỉ lấy 10 bản ghi đầu tiên để tránh tràn context
                if isinstance(actual_data, list) and len(actual_data) > 10:
                    actual_data = actual_data[:10]
                    message_suffix = "\n(Lưu ý: Chỉ hiển thị 10 bản ghi đầu tiên)"
                else:
                    message_suffix = ""

                return f"Dữ liệu thực tế ({data_category}): {json.dumps(actual_data, ensure_ascii=False)}{message_suffix}"
                
        except Exception as e:
            return f"Lỗi khi truy vấn {data_category}: {str(e)}"
