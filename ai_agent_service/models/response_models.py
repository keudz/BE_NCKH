from pydantic import BaseModel
from typing import List, Optional

class PipelineResult(BaseModel):
    agents_used: Optional[List[str]] = None
    agent_type: Optional[str] = "pipeline"
    full_response: Optional[str] = None
    strategy_agent: Optional[str] = None
    content_agent: Optional[str] = None
    design_agent: Optional[str] = None
    report_agent: Optional[str] = None
    report_url: Optional[str] = None
    generated_image_url: Optional[str] = None
