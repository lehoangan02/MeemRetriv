#!/bin/bash

TARGET_NAME="c2_3_agOs_original-D24_1620642651293.npy"

echo "Deleting all objects with name: $TARGET_NAME"

curl -s -X DELETE "http://127.0.0.1:8080/v1/batch/objects" \
  -H "Content-Type: application/json" \
  -d "{
    \"match\": {
      \"class\": \"MemeImage\",
      \"where\": {
        \"path\": [\"name\"],
        \"operator\": \"Equal\",
        \"valueText\": \"$TARGET_NAME\"
      }
    }
}" | jq