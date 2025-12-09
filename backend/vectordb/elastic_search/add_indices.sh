curl -X PUT "http://localhost:9200/celebrities" -H "Content-Type: application/json" -d '{
  "mappings": {
    "properties": {
      "name": {
        "type": "text"
      }
    }
  }
}'
curl -X PUT "http://localhost:9200/captions" -H "Content-Type: application/json" -d '{
  "mappings": {
    "properties": {
      "caption": { "type": "text" },
      "ref_id": { "type": "integer" }
    }
  }
}'