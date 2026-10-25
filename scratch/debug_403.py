import os
import json
import urllib.request
import urllib.error

def debug_test():
    env_file = r"c:\Users\likhi\OneDrive\Desktop\PrepGpt\backend\.env"
    key = None
    model = "openai/gpt-oss-120b"
    
    if os.path.exists(env_file):
        with open(env_file, "r") as f:
            for line in f:
                line = line.strip()
                if line.startswith("GROQ_API_KEY="):
                    key = line.split("=", 1)[1].strip()
                elif line.startswith("GROQ_AI_MODEL="):
                    model = line.split("=", 1)[1].strip()

    url = "https://api.groq.com/openai/v1/chat/completions"
    payload = {
        "model": model,
        "messages": [
            {"role": "user", "content": "hello"}
        ]
    }
    
    headers = {
        "Content-Type": "application/json",
        "Authorization": f"Bearer {key}",
        "User-Agent": "PrepGPT-Backend/1.0"
    }
    
    req = urllib.request.Request(url, data=json.dumps(payload).encode("utf-8"), headers=headers, method="POST")
    try:
        with urllib.request.urlopen(req) as resp:
            print("STATUS:", resp.getcode())
            print("BODY:", resp.read().decode("utf-8")[:200])
    except urllib.error.HTTPError as e:
        print("HTTP STATUS:", e.code)
        body = e.read().decode("utf-8")
        if key in body:
            body = body.replace(key, "[REDACTED_KEY]")
        print("ERROR BODY:", body)

if __name__ == "__main__":
    debug_test()
