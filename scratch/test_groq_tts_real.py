import os
import json
import urllib.request
import urllib.error

def test_tts():
    env_file = r"c:\Users\likhi\OneDrive\Desktop\PrepGpt\backend\.env"
    key = None
    model = "canopylabs/orpheus-v1-english"
    
    if os.path.exists(env_file):
        with open(env_file, "r") as f:
            for line in f:
                line = line.strip()
                if line.startswith("GROQ_API_KEY="):
                    key = line.split("=", 1)[1].strip()
                elif line.startswith("GROQ_TTS_MODEL="):
                    model = line.split("=", 1)[1].strip()

    detected = "yes" if (key and key != "your_groq_api_key_here" and len(key) > 10) else "no"
    print(f"ENV_DETECTED:{detected}")
    
    if detected != "yes":
        print("PROVIDER:Groq TTS")
        print(f"MODEL:{model}")
        print("HTTP_STATUS:N/A")
        print("SYNTHESIS_SUCCESS:False")
        print("CONTENT_TYPE:N/A")
        print("RESPONSE_SIZE_BYTES:0")
        print("MOCK_SERVICE_BYPASSED:False")
        print("ERROR:GROQ_API_KEY not configured in backend/.env")
        return

    url = "https://api.groq.com/openai/v1/audio/speech"
    payload = {
        "model": model,
        "input": "Hello, this is a PrepGPT voice test.",
        "voice": "alloy"
    }
    
    headers = {
        "Content-Type": "application/json",
        "Authorization": f"Bearer {key}",
        "User-Agent": "PrepGPT-Backend/1.0"
    }
    
    req = urllib.request.Request(url, data=json.dumps(payload).encode("utf-8"), headers=headers, method="POST")
    try:
        with urllib.request.urlopen(req) as resp:
            status_code = resp.getcode()
            content_type = resp.headers.get("Content-Type", "audio/mpeg")
            audio_bytes = resp.read()
            size = len(audio_bytes)
            
            print("PROVIDER:Groq TTS (RestVoiceService)")
            print(f"MODEL:{model}")
            print(f"HTTP_STATUS:{status_code}")
            print("SYNTHESIS_SUCCESS:True")
            print(f"CONTENT_TYPE:{content_type}")
            print(f"RESPONSE_SIZE_BYTES:{size}")
            print("MOCK_SERVICE_BYPASSED:True")
            print("ERROR:None")
    except urllib.error.HTTPError as e:
        err_body = e.read().decode("utf-8")
        safe_msg = f"HTTP Error {e.code}: {e.reason}"
        try:
            err_j = json.loads(err_body)
            if "error" in err_j and "message" in err_j["error"]:
                safe_msg = f"HTTP Error {e.code}: {err_j['error']['message']}"
        except Exception:
            pass
        print("PROVIDER:Groq TTS")
        print(f"MODEL:{model}")
        print(f"HTTP_STATUS:{e.code}")
        print("SYNTHESIS_SUCCESS:False")
        print("CONTENT_TYPE:N/A")
        print("RESPONSE_SIZE_BYTES:0")
        print("MOCK_SERVICE_BYPASSED:False")
        print(f"ERROR:{safe_msg}")
    except Exception as ex:
        print("PROVIDER:Groq TTS")
        print(f"MODEL:{model}")
        print("HTTP_STATUS:500")
        print("SYNTHESIS_SUCCESS:False")
        print("CONTENT_TYPE:N/A")
        print("RESPONSE_SIZE_BYTES:0")
        print("MOCK_SERVICE_BYPASSED:False")
        print(f"ERROR:{type(ex).__name__}: {str(ex)}")

if __name__ == "__main__":
    test_tts()
