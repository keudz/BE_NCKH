import httpx
import os
import uuid
import urllib.parse

class ImageGenerator:
    def __init__(self, provider: str, api_key: str):
        self.provider = provider
        self.api_key = api_key
        # Thư mục lưu ảnh local
        self.output_dir = "static/generated_images"
        # Địa chỉ của AI Service (Để Frontend gọi được ảnh)
        self.base_url = "http://localhost:8001" 
        
        if not os.path.exists(self.output_dir):
            os.makedirs(self.output_dir, exist_ok=True)

    async def generate(self, prompt: str) -> str:
        """Tạo ảnh sử dụng model Flux (Miễn phí & Chất lượng cao)"""
        # Encode prompt
        encoded_prompt = urllib.parse.quote(prompt)
        # Sử dụng model Flux của Pollinations
        image_url = f"https://image.pollinations.ai/prompt/{encoded_prompt}?width=1024&height=1024&nologo=true&model=flux"
        
        print(f"[AI-Image] Generating with Flux: {prompt[:50]}...")
        
        try:
            # Tải ảnh về lưu local để tránh lỗi cross-domain trên Zalo Mini App
            async with httpx.AsyncClient(timeout=40.0) as client:
                response = await client.get(image_url)
                if response.status_code == 200:
                    filename = f"gen_{uuid.uuid4().hex[:10]}.png"
                    filepath = os.path.join(self.output_dir, filename)
                    with open(filepath, "wb") as f:
                        f.write(response.content)
                    
                    final_url = f"{self.base_url}/static/generated_images/{filename}"
                    print(f"[AI-Image] Success: {final_url}")
                    return final_url
        except Exception as e:
            print(f"[AI-Image] Error downloading: {str(e)}")
            
        # Fallback trả về link gốc nếu không tải về được
        return image_url
