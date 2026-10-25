import os
import json
import urllib.request
import urllib.error
import io
import wave
import math
import struct
import uuid

def generate_sample_wav():
    sample_rate = 16000
    duration = 1.0 # 1 second tone
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

def build_multipart(fields, files, boundary):
    body = []
    for (key, value) in fields:
        body.append(f'--{boundary}'.encode('ascii'))
        body.append(f'Content-Disposition: form-data; name="{key}"'.encode('ascii'))
        body.append(b'')
        body.append(str(value).encode('utf-8'))
        
    for (key, filename, content_type, data) in files:
        body.append(f'--{boundary}'.encode('ascii'))
        body.append(f'Content-Disposition: form-data; name="{key}"; filename="{filename}"'.encode('ascii'))
        body.append(f'Content-Type: {content_type}'.encode('ascii'))
        body.append(b'')
        body.append(data)
        
    body.append(f'--{boundary}--'.encode('ascii'))
    body.append(b'')
    return b'\r\n'.join(body)

def test_stt():
    env_file = r"c:\Users\likhi\OneDrive\Desktop\PrepGpt\backend\.env"
    key = None
    model = "whisper-large-v3-turbo"
    
    if os.path.exists(env_file):
        with open(env_file, "r") as f:
            for line in f:
                line = line.strip()
                if line.startswith("GROQ_API_KEY="):
                    key = line.split("=", 1)[1].strip()
                elif line.startswith("GROQ_STT_MODEL="):
                    model = line.split("=", 1)[1].strip()

    detected = "yes" if (key and key != "your_groq_api_key_here" and len(key) > 10) else "no"
    print(f"ENV_DETECTED:{detected}")
    
    if detected != "yes":
        print("PROVIDER:Groq STT")
        print(f"MODEL:{model}")
        print("HTTP_STATUS:N/A")
        print("TRANSCRIPTION_SUCCESS:False")
        print("TRANSCRIPTION_TEXT:N/A")
        print("MOCK_SERVICE_BYPASSED:False")
        print("ERROR:GROQ_API_KEY not configured in backend/.env")
        return

    wav_bytes = generate_sample_wav()
    boundary = f"----WebKitFormBoundary{uuid.uuid4().hex}"
    
    fields = [("model", model)]
    files = [("file", "test_audio.wav", "audio/wav", wav_bytes)]
    multipart_data = build_multipart(fields, files, boundary)
    
    url = "https://api.groq.com/openai/v1/audio/transcriptions"
    headers = {
        "Content-Type": f"multipart/form-data; boundary={boundary}",
        "Authorization": f"Bearer {key}",
        "User-Agent": "PrepGPT-Backend/1.0"
    }
    
    req = urllib.request.Request(url, data=multipart_data, headers=headers, method="POST")
    try:
        with urllib.request.urlopen(req) as resp:
            status_code = resp.getcode()
            body_bytes = resp.read()
            resp_json = json.loads(body_bytes.decode("utf-8"))
            text = resp_json.get("text", "")
            
            print("PROVIDER:Groq STT (RestVoiceService)")
            print(f"MODEL:{model}")
            print(f"HTTP_STATUS:{status_code}")
            print("TRANSCRIPTION_SUCCESS:True")
            print(f"TRANSCRIPTION_TEXT:'{text}'")
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
        print("PROVIDER:Groq STT")
        print(f"MODEL:{model}")
        print(f"HTTP_STATUS:{e.code}")
        print("TRANSCRIPTION_SUCCESS:False")
        print("TRANSCRIPTION_TEXT:N/A")
        print("MOCK_SERVICE_BYPASSED:False")
        print(f"ERROR:{safe_msg}")
    except Exception as ex:
        print("PROVIDER:Groq STT")
        print(f"MODEL:{model}")
        print("HTTP_STATUS:500")
        print("TRANSCRIPTION_SUCCESS:False")
        print("TRANSCRIPTION_TEXT:N/A")
        print("MOCK_SERVICE_BYPASSED:False")
        print(f"ERROR:{type(ex).__name__}: {str(ex)}")

if __name__ == "__main__":
    test_stt()
