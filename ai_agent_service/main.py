from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from routers.agent_router import router as agent_router
from routers.invoice_router import router as invoice_router
from fastapi.staticfiles import StaticFiles

app = FastAPI(title="AI Agent Service", version="1.0.0")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

app.mount("/static", StaticFiles(directory="static"), name="static")
app.include_router(agent_router, prefix="/api/v1/agent", tags=["Agent"])
app.include_router(invoice_router, prefix="/api/v1/ai", tags=["Invoice AI"])

@app.get("/")
async def health():
    return {"status": "AI Agent Service is running", "agents": 4}

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8001)
