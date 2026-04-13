from fastapi import FastAPI
from routers.agent_router import router as agent_router

from fastapi.staticfiles import StaticFiles

app = FastAPI(title="AI Agent Service", version="1.0.0")

app.mount("/static", StaticFiles(directory="static"), name="static")
app.include_router(agent_router, prefix="/api/v1/agent", tags=["Agent"])

@app.get("/")
async def health():
    return {"status": "AI Agent Service is running", "agents": 4}

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8001)
