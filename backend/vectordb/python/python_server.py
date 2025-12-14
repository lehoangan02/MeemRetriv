import uvicorn
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
from contextlib import asynccontextmanager
import json
import sys
import os

# Import your classes
from MemeLLMProcessor import MemeLLMProcessor
from MobileClipEmbedder import MobileCLIPEmbedder 

# Global storage
ml_models = {}

@asynccontextmanager
async def lifespan(app: FastAPI):
    # --- STARTUP LOGIC ---
    print("-----------------------------------------")
    print("🚀 Initializing models...")
    
    try:
        # 1. Load LLM
        ml_models["processor"] = MemeLLMProcessor()
        print("✅ MemeLLMProcessor loaded.")

        # 2. Load MobileCLIP
        ml_models["embedder"] = MobileCLIPEmbedder(
            model_name="mobileclip_s0",
            pretrained_path="./../models/mobileclip_s0.pt" 
        )
        print("✅ MobileCLIP Embedder loaded.")
        
    except Exception as e:
        print(f"❌ Critical Error loading models: {e}")
        # Stop server if models fail to load
        sys.exit(1)
        
    print("-----------------------------------------")
    
    yield # Server runs here
    
    # --- SHUTDOWN LOGIC (Optional) ---
    print("Cleaning up resources...")
    ml_models.clear()

app = FastAPI(lifespan=lifespan)

# --- DTOs ---
class QueryRequest(BaseModel):
    query: str

class EmbedImageRequest(BaseModel):
    image_path: str

class EmbedTextRequest(BaseModel):
    text: str

# --- ENDPOINTS ---

@app.post("/analyze")
def analyze_meme(request: QueryRequest):
    processor = ml_models.get("processor")
    if not processor:
        raise HTTPException(status_code=503, detail="LLM Processor not active")
    
    try:
        result = processor.process_query(request.query)
        
        # Clean markdown if present (Common issue with LLM outputs)
        clean_result = result
        if "```json" in clean_result:
            clean_result = clean_result.split("```json")[1].split("```")[0].strip()
        elif "```" in clean_result: # Generic code block
            clean_result = clean_result.split("```")[1].split("```")[0].strip()
            
        return json.loads(clean_result)
        
    except json.JSONDecodeError:
        # Fallback if LLM didn't return valid JSON
        return {"raw_response": result, "error": "Failed to parse JSON"}
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

@app.post("/embed_image")
def embed_image(request: EmbedImageRequest):
    embedder = ml_models.get("embedder")
    if not embedder:
        raise HTTPException(status_code=503, detail="Embedder not active")
    
    if not os.path.exists(request.image_path):
        raise HTTPException(status_code=404, detail=f"Image not found at path: {request.image_path}")

    try:
        # normalized=True is usually better for search/comparison
        embedding = embedder.get_embedding(request.image_path, normalize=True)
        
        if embedding is None:
             raise HTTPException(status_code=400, detail="Failed to generate embedding (image corrupt?)")

        return {"embedding": embedding.flatten().tolist()}
        
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

@app.post("/embed_text")
def embed_text(request: EmbedTextRequest):
    embedder = ml_models.get("embedder")
    if not embedder:
        raise HTTPException(status_code=503, detail="Embedder not active")

    if not request.text or not request.text.strip():
        raise HTTPException(status_code=400, detail="Text query cannot be empty")

    try:
        embedding = embedder.get_text_embedding(request.text, normalize=True)
        
        if embedding is None:
             raise HTTPException(status_code=500, detail="Failed to generate text embedding")

        # FIX: Flatten explicitly if the class returns 2D
        # If embedding is shape (1, 512), flatten it to (512,)
        return {"embedding": embedding.flatten().tolist()}
        
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

if __name__ == "__main__":
    uvicorn.run(app, host="127.0.0.1", port=8000)