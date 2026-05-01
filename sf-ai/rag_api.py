import json

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse, StreamingResponse
from pydantic import BaseModel, Field

from rag_service import RagService

app = FastAPI(title="StudyFlow RAG API")
service = RagService()

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


class ChatReq(BaseModel):
    query: str = Field(..., min_length=1)
    top_k: int = Field(default=5, ge=1, le=10)


@app.get("/health")
def health():
    return {"ok": True}


@app.post("/rag/chat")
def rag_chat(req: ChatReq):
    data = service.chat(req.query, top_k=req.top_k)
    return JSONResponse(content=data, media_type="application/json; charset=utf-8")


@app.post("/rag/chat/stream")
def rag_chat_stream(req: ChatReq):
    def event_stream():
        try:
            for item in service.chat_stream(req.query, top_k=req.top_k):
                yield f"data: {json.dumps(item, ensure_ascii=False)}\n\n"
            yield "event: done\ndata: {}\n\n"
        except Exception as exc:
            payload = json.dumps({"message": str(exc)}, ensure_ascii=False)
            yield f"event: error\ndata: {payload}\n\n"

    return StreamingResponse(
        event_stream(),
        media_type="text/event-stream; charset=utf-8",
        headers={
            "Cache-Control": "no-cache",
            "Connection": "keep-alive",
            "X-Accel-Buffering": "no",
        },
    )
