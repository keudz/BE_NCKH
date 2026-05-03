from pydantic import BaseModel
from typing import Optional, List

class AgentRequest(BaseModel):
    prompt: str
    tenant_id: Optional[int] = None
    user_id: Optional[int] = None
    tenant_name: Optional[str] = "Doanh nghiệp"
    tenant_description: Optional[str] = ""
    access_token: Optional[str] = None
    history: Optional[List[dict]] = []
