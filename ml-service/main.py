from fastapi import FastAPI

app = FastAPI(title="DepotIQ ML Service")


@app.get("/health")
def health():
    return {"status": "ok"}

