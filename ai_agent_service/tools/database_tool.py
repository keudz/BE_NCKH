import httpx
from tools.base import BaseTool, ToolDefinition
from core.config import settings

class DatabaseQueryTool(BaseTool):
    def __init__(self):
        super().__init__()
        self.backend_url = settings.BACKEND_URL

    def get_definition(self) -> ToolDefinition:
        return ToolDefinition(
            name="query_database",
            description="Truy vấn dữ liệu thực tế từ hệ thống doanh nghiệp (nhân viên, công việc, chấm công, sản phẩm, hóa đơn).",
            parameters={
                "type": "object",
                "properties": {
                    "endpoint": {
                        "type": "string",
                        "description": "Đường dẫn API cần gọi (ví dụ: /api/v1/tasks, /api/v1/attendance/admin/today, /api/v1/products, /api/v1/invoices, /api/v1/customers)."
                    },
                    "method": {
                        "type": "string",
                        "enum": ["GET", "POST"],
                        "default": "GET"
                    },
                    "params": {
                        "type": "object",
                        "description": "Các tham số query (ví dụ: { 'tenantId': 1 })"
                    }
                },
                "required": ["endpoint"]
            }
        )

    async def run(self, endpoint: str, method: str = "GET", params: dict = None, tenant_id: str = "1", access_token: str = None) -> str:
        url = f"{self.backend_url}{endpoint}"
        
        if params is None:
            params = {}
        
        headers = {
            "X-Tenant-Id": str(tenant_id),
            "Content-Type": "application/json"
        }
        if access_token:
            headers["Authorization"] = f"Bearer {access_token}"
        
        print(f"DEBUG: Calling {url} with Tenant-Id: {tenant_id}")
        if not access_token:
            print("DEBUG: WARNING - No access token provided!")
        
        try:
            async with httpx.AsyncClient(timeout=30.0) as client:
                if method == "GET":
                    response = await client.get(url, params=params, headers=headers)
                else:
                    response = await client.post(url, json=params, headers=headers)
                
                print(f"DEBUG: Response Status: {response.status_code}")
                
                if response.status_code == 401:
                    return "Lỗi xác thực (401): Token không hợp lệ hoặc đã hết hạn. Vui lòng đăng nhập lại."
                
                response.raise_for_status()
                data = response.json()
                
                import json
                return f"Dữ liệu thực tế: {json.dumps(data, ensure_ascii=False)}"
        except Exception as e:
            return f"Lỗi truy vấn ({type(e).__name__}): {str(e)}"
