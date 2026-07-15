import contextvars
from services.image_generator import ImageGenerator
from core.config import settings

# Context variable to capture generated image url
generated_image_url_var = contextvars.ContextVar("generated_image_url", default=None)

image_gen = ImageGenerator(provider=settings.IMAGE_PROVIDER, api_key=settings.IMAGE_API_KEY)

async def generate_image(prompt: str) -> dict:
    """Tạo hình ảnh quảng cáo hoặc poster dựa trên mô tả chi tiết.
    
    Args:
        prompt: Mô tả chi tiết hình ảnh cần tạo (bằng tiếng Anh hoặc tiếng Việt).
    """
    try:
        url = await image_gen.generate(prompt)
        generated_image_url_var.set(url)
        return {"status": "success", "image_url": url, "message": f"Đã tạo ảnh thành công. URL: {url}"}
    except Exception as e:
        return {"status": "error", "message": f"Lỗi khi tạo ảnh: {str(e)}"}
