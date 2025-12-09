curl -X GET "http://localhost:9200/celebrities/_search?pretty" -H "Content-Type: application/json" -d '{
  "query": {
    "match": {
      "name": {
        "query": "Obama",
        "fuzziness": "AUTO"
      }
    }
  }
}'
