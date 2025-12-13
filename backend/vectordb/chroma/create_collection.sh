curl -X POST http://localhost:8001/api/v1/collections \
  -H "Content-Type: application/json" \
  -d '{
    "name": "face_vectors",
    "metadata": {
        "hnsw:space": "cosine",
        "hnsw:construction_ef": 100,
        "hnsw:M": 16,
        "hnsw:search_ef": 10
    }
  }' | jq