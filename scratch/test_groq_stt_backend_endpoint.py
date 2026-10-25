import os
import json
import urllib.request
import io
import wave
import math
import struct
import uuid

def generate_sample_wav():
    sample_rate = 16000
    duration = 1.0
    num_samples = int(sample_rate * duration)
    
    buf = io.BytesIO()
    with wave.open(buf, 'wb') as wav_file:
        wav_file.setnchannels(1)
        wav_file.setsampwidth(2)
        wav_file.setframerate(sample_rate)
        
        for i in range(num_samples):
            value = int(8000 * math.sin(2 * math.pi * 440 * i / sample_rate))
            wav_file.writeframesraw(struct.pack('<h', value))
            
    return buf.getvalue()

def build_multipart(files, boundary):
    body = []
    for (key, filename, content_type, data) in files:
        body.append(f'--{boundary}'.encode('ascii'))
        body.append(f'Content-Disposition: form-data; name="{key}"; filename="{filename}"'.encode('ascii'))
        body.append(f'Content-Type: {content_type}'.encode('ascii'))
        body.append(b'')
        body.append(data)
        
    body.append(f'--{boundary}--'.encode('ascii'))
    body.append(b'')
    return b'\r\n'.join(body)

def test_backend_stt():
    base_url = "http://localhost:8080/api/v1"
    
    # 1. Register
    email = f"stt_tester_{uuid.uuid4().hex[:6]}@example.com"
    pwd = "Password123!"
    reg_payload = json.dumps({"fullName": "STT Tester", "email": email, "password": pwd}).encode("utf-8")
    req = urllib.request.Request(f"{base_url}/auth/register", data=reg_payload, headers={"Content-Type": "application/json"}, method="POST")
    try:
        urllib.request.urlopen(req)
    except Exception as e:
        print("Register failed:", e)
        return

    # 2. Login
    login_payload = json.dumps({"email": email, "password": pwd}).encode("utf-8")
    req = urllib.request.Request(f"{base_url}/auth/login", data=login_payload, headers={"Content-Type": "application/json"}, method="POST")
    try:
        with urllib.request.urlopen(req) as resp:
            data = json.loads(resp.read().decode("utf-8"))
            token = data["token"]
    except Exception as e:
        print("Login failed:", e)
        return

    # 3. Call /voice/transcribe
    wav_bytes = generate_sample_wav()
    boundary = f"----WebKitFormBoundary{uuid.uuid4().hex}"
    files = [("file", "sample_test.wav", "audio/wav", wav_bytes)]
    body = build_multipart(files, boundary)
    
    headers = {
        "Authorization": f"Bearer {token}",
        "Content-Type": f"multipart/form-data; boundary={boundary}"
    }
    
    req = urllib.request.Request(f"{base_url}/voice/transcribe", data=body, headers=headers, method="POST")
    try:
        with urllib.request.urlopen(req) as resp:
            data = json.loads(resp.read().decode("utf-8"))
            print("BACKEND_ENDPOINT_STATUS:", resp.getcode())
            print("BACKEND_TRANSCRIPTION_TEXT:", data["transcription"])
            print("BACKEND_SUCCESS: True")
    except Exception as e:
        print("BACKEND_ENDPOINT_ERROR:", e)

if __name__ == "__main__":
    test_backend_stt()
