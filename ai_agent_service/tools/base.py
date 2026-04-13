from typing import Any, Dict, List
from pydantic import BaseModel

class ToolDefinition(BaseModel):
    name: str
    description: str
    parameters: Dict[str, Any]

class BaseTool:
    def __init__(self):
        self.definition = self.get_definition()

    def get_definition(self) -> ToolDefinition:
        raise NotImplementedError

    async def run(self, **kwargs) -> Any:
        raise NotImplementedError
