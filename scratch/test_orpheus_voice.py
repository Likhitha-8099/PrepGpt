import os
import json
import urllib.request
import urllib.error

def test_orpheus_voice(voice_name):
    env_file = r"c:\Users\likhi\OneDrive\Desktop\PrepGpt\backend\.env"
    key = None
    if os.path.exists(env_file):
        with open(env_file, "r") as f:
            for line in f:
                if line.startswith("GROQ_API_KEY="):
                    key = line.split("=", 1)[1].strip()

    url = "https://api.groq.com/openai/v1/audio/speech"
    payload = {
        "model": "canopylabs/orpheus-v1-english",
        "input": "Hello, this is a PrepGPT voice test.",
        "voice": voice_name,
        "response_format": "wav"
    }
    
    headers = {
        "Content-Type": "application/json",
        "Authorization": f"Bearer {key}",
        "User-Agent": "PrepGPT-Backend/1.0"
    }
    
    req = urllib.request.Request(url, data=json.dumps(payload).encode("utf-8"), headers=headers, method="POST")
    try:
        with urllib.request.urlopen(req) as resp:
            print(f"VOICE:{voice_name} -> STATUS:{resp.getcode()} CONTENT_TYPE:{resp.headers.get('Content-Type')} SIZE:{len(resp.read())}")
    except urllib.error.HTTPError as e:
        body = e.read().decode("utf-8")
        if key in body:
            body = body.replace(key, "[REDACTED]")
        print(f"VOICE:{voice_name} -> HTTP ERROR {e.code}: {body}")

if __name__ == "__main__":
    test_orpheus_voice("daniel")
