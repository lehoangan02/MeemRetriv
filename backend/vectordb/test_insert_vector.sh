#!/bin/bash

# Path to your embeddings
EMBEDDINGS_DIR="./../../DATA/embeddings"
VECTOR_FILE="c2_3_agOs_original-D24_1620642651293.npy"

# 1. Prepare Data & Generate Deterministic UUID
# We output a string like "UUID|JSON_VECTOR" to capture both in one go
READ_DATA=$(python3 - <<END
import numpy as np, json, uuid

# Load vector and flatten
vector = np.load("$EMBEDDINGS_DIR/$VECTOR_FILE")
vec_json = json.dumps(vector.flatten().tolist())

# Generate UUID based on the filename (namespace DNS is standard for strings)
unique_id = str(uuid.uuid5(uuid.NAMESPACE_DNS, "$VECTOR_FILE"))

print(f"{unique_id}|{vec_json}")
END
)

# 2. Split the output into shell variables
UUID=${READ_DATA%|*}
VECTOR_JSON=${READ_DATA#*|}

echo "Target UUID for this file: $UUID"

# 3. First Insert
echo "Attempting 1st Insert..."
curl -s -X POST "http://127.0.0.1:8080/v1/objects" \
  -H "Content-Type: application/json" \
  -d "{
    \"id\": \"$UUID\",
    \"class\": \"LegoPiece\",
    \"vector\": $VECTOR_JSON,
    \"properties\": {
        \"name\": \"$VECTOR_FILE\"
    }
}" | jq 'del(.vector)'

# 4. Check Database
echo "Querying Weaviate (should see 1 entry for this ID)..."
curl -s "http://127.0.0.1:8080/v1/objects" | jq 'del(.objects[].vector)'

# 5. Duplicate Insert (This should FAIL)
echo "Attempting Duplicate Insert (should fail)..."
curl -s -X POST "http://127.0.0.1:8080/v1/objects" \
  -H "Content-Type: application/json" \
  -d "{
    \"id\": \"$UUID\",
    \"class\": \"MemeImage\",
    \"vector\": $VECTOR_JSON,
    \"properties\": {
        \"name\": \"$VECTOR_FILE\"
    }
}" | jq 'del(.vector)'