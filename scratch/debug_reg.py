import json
import urllib.request
import uuid

base_url = "http://localhost:8080/api/v1"
email = f"stt_debug_{uuid.uuid4().hex[:6]}@example.com"
pwd = "Password123!"

reg_payload = json.dumps({"fullName": "STT Tester", "email": email, "password": pwd}).encode("utf-8")
req = urllib.request.Request(f"{base_url}/auth/register", data=reg_payload, headers={"Content-Type": "application/json"}, method="POST")
try:
    with urllib.request.urlopen(req) as resp:
        data = json.loads(resp.read().decode("utf-8"))
        print("KEYS:", data.keys())
        print("DATA:", data)
except Exception as e:
    print("ERROR:", e)
