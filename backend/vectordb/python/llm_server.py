import uvicorn
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
import sys
import os

# Import your existing class
# Assuming your original script is named 'MemeLLMProcessor.py'
from MemeLLMProcessor import MemeLLMProcessor

app = FastAPI()

# Global variable to hold the model (Singleton)
processor = None

class QueryRequest(BaseModel):
    query: str

@app.on_event("startup")
def load_model():
    global processor
    print("Loading models into memory... this happens only once.")
    # Initialize the heavy model here
    processor = MemeLLMProcessor()
    print("Models loaded successfully!")

@app.post("/analyze")
def analyze_meme(request: QueryRequest):
    if not processor:
        raise HTTPException(status_code=503, detail="Model not loaded yet")
    
    try:
        # Call your existing processing logic
        result = processor.process_query(request.query)
        # Your process_query returns a JSON string, we might want to return it directly
        # or let FastAPI handle JSON serialization if process_query returned a dict.
        # Since your process_query returns a JSON string, we parse it to return a proper JSON object.
        import json
        
        # Clean the response if it contains markdown code blocks (from your existing logic)
        clean_result = result
        if "```json" in clean_result:
            clean_result = clean_result.replace("```json", "").replace("```", "").strip()
            
        return json.loads(clean_result)
        
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

if __name__ == "__main__":
    # Run the server on localhost port 8000
    uvicorn.run(app, host="127.0.0.1", port=8000)