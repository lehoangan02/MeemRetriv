curl -s "http://127.0.0.1:8080/v1/objects?class=MemeImage&limit=1000" \
| jq -r '.objects[] | "\(.id) | \(.properties.name)"'