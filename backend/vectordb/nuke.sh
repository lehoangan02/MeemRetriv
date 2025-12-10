#!/bin/bash

# ==============================================================================
# CONFIGURATION
# Update these to match your DatabaseConfig.java settings
# ==============================================================================

# Elasticsearch Config
ES_URL="http://localhost:9200"

# Weaviate Config
WEAVIATE_URL="http://localhost:8080"

# Postgres Config
PG_HOST="localhost"
PG_PORT="5432"
PG_USER="postgres"
PG_DB="postgres"      # Or whatever your specific DB name is
PG_PASSWORD="password" # WARNING: Ideally, use .pgpass, but hardcoded here for convenience

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# ==============================================================================
# SAFETY CHECK
# ==============================================================================
echo -e "${RED}!!! WARNING !!!${NC}"
echo "This script will PERMANENTLY DELETE schemas and data from:"
echo "1. Elasticsearch (Indices: celebrities, captions)"
echo "2. Weaviate (Classes: MemeImage, MemeImageCleaned, CelebFaceEmbeddings, ExtractedFaceEmbeddings)"
echo "3. PostgreSQL (Tables: label, celeb | Type: sentiment_level)"
echo ""
read -p "Are you sure you want to proceed? (y/N): " confirmation

if [[ "$confirmation" != "y" && "$confirmation" != "Y" ]]; then
    echo "Aborting."
    exit 1
fi

echo ""

# ==============================================================================
# 1. ELASTICSEARCH WIPE
# Defined in: ElasticSearchDBManager.java
# ==============================================================================
echo -e "${YELLOW}--- Nuking Elasticsearch Indices ---${NC}"

# Nuke 'celebrities' index
# Java: createCelebIndex(), importCelebNames()
echo -n "Deleting 'celebrities' index... "
curl -s -X DELETE "$ES_URL/celebrities" -o /dev/null
echo -e "${GREEN}Done${NC}"

# Nuke 'captions' index
# Java: addCaptionIndex(), importCaptions()
echo -n "Deleting 'captions' index...    "
curl -s -X DELETE "$ES_URL/captions" -o /dev/null
echo -e "${GREEN}Done${NC}"


# ==============================================================================
# 2. WEAVIATE WIPE
# Defined in: WeviateSchema.java
# ==============================================================================
echo -e "\n${YELLOW}--- Nuking Weaviate Classes ---${NC}"

# Nuke 'MemeImage' class
# Java: createWeviateClass("MemeImage", ...)
echo -n "Deleting 'MemeImage'...               "
curl -s -X DELETE "$WEAVIATE_URL/v1/schema/MemeImage" -o /dev/null
echo -e "${GREEN}Done${NC}"

# Nuke 'MemeImageCleaned' class
# Java: createWeviateClass("MemeImageCleaned", ...)
echo -n "Deleting 'MemeImageCleaned'...        "
curl -s -X DELETE "$WEAVIATE_URL/v1/schema/MemeImageCleaned" -o /dev/null
echo -e "${GREEN}Done${NC}"

# Nuke 'CelebFaceEmbeddings' class
# Java: createCelebFacePickleSchema("CelebFaceEmbeddings", ...)
echo -n "Deleting 'CelebFaceEmbeddings'...     "
curl -s -X DELETE "$WEAVIATE_URL/v1/schema/CelebFaceEmbeddings" -o /dev/null
echo -e "${GREEN}Done${NC}"

# Nuke 'ExtractedFaceEmbeddings' class
# Java: createExtractedFaceSchema("ExtractedFaceEmbeddings", ...)
echo -n "Deleting 'ExtractedFaceEmbeddings'... "
curl -s -X DELETE "$WEAVIATE_URL/v1/schema/ExtractedFaceEmbeddings" -o /dev/null
echo -e "${GREEN}Done${NC}"


# ==============================================================================
# 3. POSTGRESQL WIPE
# Defined in: PostgresSchemaCreator.java
# ==============================================================================
echo -e "\n${YELLOW}--- Nuking PostgreSQL Schema ---${NC}"

# Export password so psql doesn't ask interactively
export PGPASSWORD=$PG_PASSWORD

# Define the SQL commands to drop tables and types
# CASCADE is used to ensure dependent objects (like views or foreign keys) are also removed
SQL_COMMANDS="
DROP TABLE IF EXISTS label CASCADE;
DROP TABLE IF EXISTS celeb CASCADE;
DROP TYPE IF EXISTS sentiment_level CASCADE;
"

echo "Executing Drop Commands on Postgres..."

# Run psql
if command -v psql &> /dev/null; then
    psql -h "$PG_HOST" -p "$PG_PORT" -U "$PG_USER" -d "$PG_DB" -c "$SQL_COMMANDS"
    
    if [ $? -eq 0 ]; then
        echo -e "${GREEN}Postgres tables and types dropped successfully.${NC}"
    else
        echo -e "${RED}Failed to drop Postgres tables. Check credentials or connection.${NC}"
    fi
else
    echo -e "${RED}Error: 'psql' command not found. Cannot wipe Postgres.${NC}"
    echo "Please install postgresql-client or run the SQL manually."
fi

# Unset password for security
unset PGPASSWORD

echo -e "\n${GREEN}=== System Cleaned ===${NC}"