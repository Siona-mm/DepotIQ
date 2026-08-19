from fastapi import FastAPI

from app.routes.recommendations import router as recommendations_router
from app.routes.data_sync import router as data_sync_router

app = FastAPI(title="DepotIQ ML Service")
app.include_router(recommendations_router)
app.include_router(data_sync_router)


@app.get("/health")
def health():
    return {"status": "ok"}
