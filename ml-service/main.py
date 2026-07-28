from fastapi import FastAPI

from app.routes.recommendations import router as recommendations_router

app = FastAPI(title="DepotIQ ML Service")
app.include_router(recommendations_router)


@app.get("/health")
def health():
    return {"status": "ok"}
