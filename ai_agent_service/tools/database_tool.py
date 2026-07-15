import httpx
import json
import datetime
import contextvars
from core.config import settings

# Context variables for ADK
tenant_id_var = contextvars.ContextVar("tenant_id", default="1")
access_token_var = contextvars.ContextVar("access_token", default=None)

async def query_database(data_category: str, search_query: str = None) -> str:
    """Truy vấn dữ liệu thực tế từ hệ thống doanh nghiệp (nhân viên, công việc, chấm công, sản phẩm, hóa đơn, thống kê).
    
    Args:
        data_category: Loại dữ liệu cần lấy: tasks (công việc), attendance (chấm công), products (sản phẩm), invoices (hóa đơn), employees (nhân sự), customers (khách hàng), summary (thống kê tổng quan).
        search_query: Từ khóa tìm kiếm hoặc tham số bổ sung (ví dụ: tên nhân viên, mã sản phẩm). Để trống nếu muốn lấy toàn bộ danh sách.
    """
    tenant_id = tenant_id_var.get()
    access_token = access_token_var.get()
    
    backend_url = settings.BACKEND_URL
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

    url = f"{backend_url}{endpoint}"
    params = {}

    if data_category == "attendance":
        now = datetime.datetime.now()
        params["month"] = now.month
        params["year"] = now.year
    
    if search_query:
        params["search"] = search_query 

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
            
            actual_data = data.get("data", data)
            
            if isinstance(actual_data, list) and len(actual_data) > 10:
                actual_data = actual_data[:10]
                message_suffix = "\n(Lưu ý: Chỉ hiển thị 10 bản ghi đầu tiên)"
            else:
                message_suffix = ""

            return f"Dữ liệu thực tế ({data_category}): {json.dumps(actual_data, ensure_ascii=False)}{message_suffix}"
            
    except Exception as e:
        return f"Lỗi khi truy vấn {data_category}: {str(e)}"
