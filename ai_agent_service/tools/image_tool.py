from tools.base import BaseTool, ToolDefinition
from services.image_generator import ImageGenerator
from core.config import settings

class ImageTool(BaseTool):
    def __init__(self, image_gen: ImageGenerator):
        super().__init__()
        self.image_gen = image_gen

    def get_definition(self) -> ToolDefinition:
        return ToolDefinition(
            name="generate_image",
            description="Tạo hình ảnh quảng cáo hoặc poster dựa trên mô tả chi tiết.",
            parameters={
                "type": "object",
                "properties": {
                    "prompt": {
                        "type": "string",
                        "description": "Mô tả chi tiết hình ảnh cần tạo (tiếng Anh hoặc tiếng Việt)."
                    }
                },
                "required": ["prompt"]
            }
        )

    async def run(self, prompt: str) -> str:
        url = await self.image_gen.generate(prompt)
        return f"Đã tạo ảnh thành công. URL: {url}"
