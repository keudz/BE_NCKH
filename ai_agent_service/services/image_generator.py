class ImageGenerator:
    def __init__(self, provider: str, api_key: str):
        self.provider = provider
        self.api_key = api_key

    async def generate(self, prompt: str) -> str:
        if self.provider == "pollinations":
            import urllib.parse
            encoded_prompt = urllib.parse.quote(prompt)
            # Dịch vụ Pollinations.ai cho phép tạo ảnh qua URL cực kỳ nhanh và miễn phí
            image_url = f"https://image.pollinations.ai/prompt/{encoded_prompt}?width=1024&height=1024&nologo=true"
            print(f"[PollinationsAI] Image URL: {image_url}")
            return image_url
            
        elif self.provider == "mock":
            return "https://via.placeholder.com/600x400?text=Generated+Image"
        else:
            raise NotImplementedError(f"Image provider {self.provider} không hỗ trợ.")
