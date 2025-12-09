curl -X GET "http://localhost:9200/celebrities/_search?pretty" -H "Content-Type: application/json" -d '{
  "query": {
    "match": {
      "name": {
        "query": "Messi",
        "fuzziness": "AUTO"
      }
    }
  }
}'

curl -X GET "http://localhost:9200/captions/_search" -H "Content-Type: application/json" -d '{
  "query": {
    "match": {
      "caption": "funny cat"
    }
  }
}'

