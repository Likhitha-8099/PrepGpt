import os
import json
import urllib.request
import urllib.error

def run_test():
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
                    
    detected = "yes" if (key and key != "your_groq_api_key_here" and len(key) > 10) else "no"
    print(f"ENV_DETECTED:{detected}")
    
    if detected != "yes":
        print("HTTP_STATUS:N/A")
        print("REQUEST_SUCCESS:False")
        print("MODEL_ACCEPTED:False")
        print("ERROR:Environment variable GROQ_API_KEY was not detected.")
        return

    url = "https://api.groq.com/openai/v1/chat/completions"
    payload = {
        "model": model,
        "messages": [
            {"role": "system", "content": "You are a professional AI interviewer. You MUST respond with valid JSON."},
            {"role": "user", "content": "Return a JSON object with key 'status' equal to 'ok'."}
        ],
        "response_format": {"type": "json_object"},
        "max_tokens": 50
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
            body_bytes = resp.read()
            resp_json = json.loads(body_bytes.decode("utf-8"))
            returned_model = resp_json.get("model", "")
            model_accepted = (model in returned_model or returned_model in model)
            
            print(f"HTTP_STATUS:{status_code}")
            print(f"REQUEST_SUCCESS:True")
            print(f"MODEL_ACCEPTED:{model_accepted}")
            print("ERROR:None")
    except urllib.error.HTTPError as e:
        error_body = e.read().decode("utf-8")
        print(f"HTTP_STATUS:{e.code}")
        print("REQUEST_SUCCESS:False")
        print("MODEL_ACCEPTED:False")
        safe_msg = f"HTTP Error {e.code}: {e.reason}"
        try:
            err_json = json.loads(error_body)
            if "error" in err_json and "message" in err_json["error"]:
                safe_msg = f"HTTP Error {e.code}: {err_json['error']['message']}"
        except Exception:
            pass
        print(f"ERROR:{safe_msg}")
    except Exception as ex:
        print("HTTP_STATUS:500")
        print("REQUEST_SUCCESS:False")
        print("MODEL_ACCEPTED:False")
        print(f"ERROR:{type(ex).__name__}: {str(ex)}")

if __name__ == "__main__":
    run_test()
