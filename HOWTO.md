# HOW TO INSTALL AND RUN

## Prerequisites

Before starting, make sure you have the following installed:

- **Docker** (and `docker-compose`) — for running the Weviate database  
- **Maven** — for building any Java components  
- **PostgreSQL** — if required by the project  
- **Python 3.11+** — for running data processing scripts  
- **Git** — for cloning the repository  
- Optional (recommended) utilities: `wget`, `unzip`, `jq`

Clone the repository.
```
git clone https://github.com/lehoangan02/MeemRetriv
cd MeemRetriv
```

### Running the database

Download data.

```
cd DATA 
# Download the archive.zip file from https://www.kaggle.com/datasets/hammadjavaid/6992-labeled-meme-images-dataset?resource=download
# Extract the file
unzip archive.zip

# Download the cleaned.zip file from https://huggingface.co/datasets/Anov129/celeb/resolve/main/cleaned.zip?download=true

wget https://huggingface.co/datasets/Anov129/celeb/resolve/main/cleaned.zip?download=true

unzip cleaned.zip
cd ..
```

Process the data.

```
# first download the CLIP model
cd backend/vectordb/models
chmod +x download_mobileclip_s0.sh
cd ..
cd ..
cd ..

cd DataProcess
python -m venv myenv
source myenv/bin/activate (for MacOS and Linux)
pip install -r requirements.txt
python mobileClipEmbedder.py --input_dir ./../DATA/archive/images/images --output_dir ./../DATA/embeddings
python mobileClipEmbedder.py --input_dir ./../DATA/cleaned --output_dir ./../DATA/embeddings_cleaned
deactivate
cd ..
```

Setup Weviate database.

```
cd backend/vectordb/weviate/
docker-compose up -d

# check if it has composed successfully
docker ps

# check if it is running properly
curl -X GET "http://127.0.0.1:8080/v1/meta" | jq
cd ..
```

Running the database

```
python -m venv .venv
source .venv/bin/activate (for MacOS and Linux)
pip install -r requirements.txt
deactivate
mvn clean compile && mvn exec:java -Dexec.mainClass="cat.dog.App"
```

