
docker-compose up -d

# check if it has composed successfully
docker ps

# check if it is running properly
curl -X GET "http://127.0.0.1:8080/v1/meta"