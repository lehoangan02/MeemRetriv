curl -X PUT "http://localhost:9200/celebrities" -H "Content-Type: application/json" -d '{
  "mappings": {
    "properties": {
      "name": {
        "type": "text"
      }
    }
  }
}'
