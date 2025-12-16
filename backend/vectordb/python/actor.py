import requests

import requests

class CharacterActorResolver:
    def __init__(self):
        self.search_url = "https://www.wikidata.org/w/api.php"
        self.sparql_url = "https://query.wikidata.org/sparql"
        self.headers = {
            "Accept": "application/sparql-results+json",
            "User-Agent": "CharacterActorResolver/1.0 (contact: youremail@example.com)"
        }

    def _get_character_qid(self, name):
        params = {
            "action": "wbsearchentities",
            "search": name,
            "language": "en",
            "format": "json",
            "limit": 1
        }
        r = requests.get(self.search_url, params=params, headers=self.headers)
        r.raise_for_status()
        data = r.json()
        if not data["search"]:
            return None
        return data["search"][0]["id"]

    def _get_actor_from_qid(self, qid):
        query = f"""
        SELECT ?actorLabel WHERE {{
          wd:{qid} wdt:P175 ?actor .
          SERVICE wikibase:label {{ bd:serviceParam wikibase:language "en". }}
        }}
        """
        r = requests.get(self.sparql_url, params={"query": query}, headers=self.headers)
        r.raise_for_status()
        data = r.json()
        results = data["results"]["bindings"]
        if not results:
            return None
        return [x["actorLabel"]["value"] for x in results]

    def resolve(self, character_name):
        qid = self._get_character_qid(character_name)
        if not qid:
            return None
        actors = self._get_actor_from_qid(qid)
        if not actors:
            return None
        return actors[0]

if __name__ == "__main__":
    resolver = CharacterActorResolver()
    character_name = "Sansa Stark"
    actors = resolver.resolve(character_name)
    # check if actors is a string or list
    if actors:
        if isinstance(actors, str):
            print(f"Actor is a string: {actors}")
            print(f"Actor who played {character_name}: {actors}")
        elif isinstance(actors, list):
            print(f"Actors is a list with {len(actors)} items: {actors}")
            print(f"Actors who played {character_name}: {', '.join(actors)}")
        else:
            print(f"Actors is neither a string nor a list: {type(actors)}")
    else:
        print(f"No actors found for character: {character_name}")

    if actors:
        print(f"Actors who played {character_name}: {', '.join(actors)}")
    else:
        print(f"No actors found for character: {character_name}")