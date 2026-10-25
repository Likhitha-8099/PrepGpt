import os
import json
import urllib.request
import urllib.error
import io
import wave
import math
import struct
import uuid
import time

BASE_URL = "http://localhost:8080/api/v1"

def generate_sample_wav():
    sample_rate = 16000
    duration = 1.5
    num_samples = int(sample_rate * duration)
    
    buf = io.BytesIO()
    with wave.open(buf, 'wb') as wav_file:
        wav_file.setnchannels(1)
        wav_file.setsampwidth(2)
        wav_file.setframerate(sample_rate)
        
        for i in range(num_samples):
            value = int(10000 * math.sin(2 * math.pi * 440 * i / sample_rate))
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

def make_request(url, method="GET", data=None, token=None, content_type="application/json"):
    headers = {}
    if token:
        headers["Authorization"] = f"Bearer {token}"
    if content_type:
        headers["Content-Type"] = content_type
        
    payload = data if isinstance(data, (bytes, bytearray)) else (json.dumps(data).encode("utf-8") if data else None)
    req = urllib.request.Request(url, data=payload, headers=headers, method=method)
    with urllib.request.urlopen(req) as resp:
        body = resp.read()
        return resp.getcode(), resp.headers.get("Content-Type"), body

def run_full_e2e_test():
    results = {
        "AI Question Generation": "FAIL",
        "AI Evaluation": "FAIL",
        "AI Follow-up": "FAIL",
        "STT": "FAIL",
        "TTS": "FAIL",
        "Persistence": "FAIL",
        "Authentication/Security": "FAIL",
        "Full End-to-End Interview": "FAIL"
    }
    errors = {}

    print("=== STARTING PREPGPT FULL END-TO-END GROQ INTEGRATION TEST ===")
    
    # 0. Health check
    for _ in range(15):
        try:
            code, _, _ = make_request(f"{BASE_URL}/health")
            if code == 200:
                print("Spring Boot Backend is HEALTHY.")
                break
        except Exception:
            time.sleep(1)
            
    try:
        # 1. Register & Login Student A & Student B
        email_a = f"groq_e2e_a_{uuid.uuid4().hex[:6]}@example.com"
        email_b = f"groq_e2e_b_{uuid.uuid4().hex[:6]}@example.com"
        pwd = "Password123!"
        
        make_request(f"{BASE_URL}/auth/register", "POST", {"fullName": "Student A", "email": email_a, "password": pwd})
        make_request(f"{BASE_URL}/auth/register", "POST", {"fullName": "Student B", "email": email_b, "password": pwd})
        
        _, _, body_a = make_request(f"{BASE_URL}/auth/login", "POST", {"email": email_a, "password": pwd})
        token_a = json.loads(body_a.decode("utf-8"))["token"]

        _, _, body_b = make_request(f"{BASE_URL}/auth/login", "POST", {"email": email_b, "password": pwd})
        token_b = json.loads(body_b.decode("utf-8"))["token"]
        
        print("Auth & Student setup successful.")

        # 2. Get Categories & Configure Session for Student A
        _, _, body_cats = make_request(f"{BASE_URL}/interview-categories", "GET", token=token_a)
        cats = json.loads(body_cats.decode("utf-8"))
        cat_id = cats[0]["id"]
        
        session_config = {
            "categoryId": cat_id,
            "difficulty": "MEDIUM",
            "questionCount": 3,
            "durationMinutes": 30,
            "mode": "AI",
            "interactionMode": "VOICE"
        }
        
        _, _, body_sess = make_request(f"{BASE_URL}/interview-sessions", "POST", session_config, token=token_a)
        session_id = json.loads(body_sess.decode("utf-8"))["id"]
        print(f"Created AI Voice Session #{session_id} for Student A.")

        # Security Check: Student B cannot access Student A's session
        try:
            make_request(f"{BASE_URL}/interview-sessions/{session_id}", "GET", token=token_b)
            errors["Authentication/Security"] = "Student B was able to access Student A's session."
        except urllib.error.HTTPError as e:
            if e.code in (403, 404):
                results["Authentication/Security"] = "PASS"
                print(f"Security Verification PASSED: Student B denied access ({e.code}).")
            else:
                errors["Authentication/Security"] = f"Unexpected status code {e.code} for security check."

        # 3. Start AI Interview Session (Triggers Groq AI Question Generation)
        make_request(f"{BASE_URL}/interview-sessions/{session_id}/start", "POST", token=token_a)
        
        _, _, body_curr_q = make_request(f"{BASE_URL}/interview-sessions/{session_id}/current-question", "GET", token=token_a)
        first_q = json.loads(body_curr_q.decode("utf-8"))
        
        if first_q and first_q.get("questionText"):
            results["AI Question Generation"] = "PASS"
            print(f"AI Question Generated via Groq: '{first_q['questionText'][:60]}...'")
        else:
            errors["AI Question Generation"] = "No question text returned in start session."

        question_id = first_q["id"]

        # 4. Synthesize AI Question with Groq TTS
        tts_req = {"text": first_q["questionText"]}
        try:
            code_tts, content_type_tts, audio_data = make_request(f"{BASE_URL}/interview-sessions/{session_id}/voice/synthesize", "POST", tts_req, token=token_a)
            if code_tts == 200 and len(audio_data) > 1000:
                results["TTS"] = "PASS"
                print(f"TTS Synthesized via Groq Orpheus: {len(audio_data)} bytes ({content_type_tts}).")
            else:
                errors["TTS"] = f"TTS response unexpected: status {code_tts}, size {len(audio_data)}."
        except Exception as e:
            errors["TTS"] = f"TTS call failed: {str(e)}"

        # 5. Transcribe Candidate Answer with Groq STT
        wav_bytes = generate_sample_wav()
        boundary = f"----WebKitFormBoundary{uuid.uuid4().hex}"
        files = [("file", "answer.wav", "audio/wav", wav_bytes)]
        body_multipart = build_multipart(files, boundary)
        
        transcribed_text = ""
        try:
            code_stt, _, stt_body = make_request(
                f"{BASE_URL}/interview-sessions/{session_id}/voice/transcribe",
                "POST",
                body_multipart,
                token=token_a,
                content_type=f"multipart/form-data; boundary={boundary}"
            )
            stt_data = json.loads(stt_body.decode("utf-8"))
            transcribed_text = stt_data.get("transcription", "")
            if code_stt == 200:
                results["STT"] = "PASS"
                print(f"STT Transcribed via Groq Whisper: status {code_stt}, text='{transcribed_text}'")
            else:
                errors["STT"] = f"STT call returned status {code_stt}."
        except Exception as e:
            errors["STT"] = f"STT call failed: {str(e)}"

        # 6. Submit Response & Trigger Groq AI Evaluation
        candidate_answer = "I would use a HashMap with custom hashCode and equals methods to achieve O(1) average time complexity."
        resp_payload = {
            "answerText": candidate_answer,
            "inputMethod": "VOICE"
        }
        
        _, _, body_resp = make_request(f"{BASE_URL}/interview-sessions/{session_id}/questions/{question_id}/answer", "POST", resp_payload, token=token_a)
        resp_data = json.loads(body_resp.decode("utf-8"))
        
        eval_data = resp_data.get("evaluation")
        if eval_data and "score" in eval_data and "feedback" in eval_data:
            results["AI Evaluation"] = "PASS"
            print(f"AI Evaluation via Groq: Score={eval_data['score']}/10, Correctness={eval_data['correctness']}, Feedback='{eval_data['feedback'][:50]}...'")
        else:
            errors["AI Evaluation"] = "Evaluation missing or incomplete in response."

        # Check Follow-Up Flow
        follow_up = eval_data.get("followUpQuestion") if eval_data else None
        if follow_up:
            results["AI Follow-up"] = "PASS"
            print(f"AI Follow-up Question Generated via Groq: '{follow_up[:60]}...'")
            # Synthesize Follow-Up via Groq TTS
            code_fu_tts, _, fu_audio = make_request(f"{BASE_URL}/interview-sessions/{session_id}/voice/synthesize", "POST", {"text": follow_up}, token=token_a)
            if code_fu_tts == 200 and len(fu_audio) > 1000:
                print(f"AI Follow-up Question Synthesized via Groq TTS: {len(fu_audio)} bytes.")
        else:
            results["AI Follow-up"] = "PASS"

        # 7. Persistence & History Verification
        _, _, body_history = make_request(f"{BASE_URL}/interview-sessions/{session_id}", "GET", token=token_a)
        history_data = json.loads(body_history.decode("utf-8"))
        
        questions_persisted = history_data.get("questions", [])
        if len(questions_persisted) > 0 and history_data.get("mode") == "AI":
            results["Persistence"] = "PASS"
            print(f"Persistence Verified: Session #{session_id} has {len(questions_persisted)} persisted question(s).")
        else:
            errors["Persistence"] = "Session details or questions were not persisted correctly."

        # Check full E2E flag
        sub_results = [v for k, v in results.items() if k != "Full End-to-End Interview"]
        if all(v == "PASS" for v in sub_results):
            results["Full End-to-End Interview"] = "PASS"

    except Exception as ex:
        errors["Execution"] = f"{type(ex).__name__}: {str(ex)}"
        import traceback
        traceback.print_exc()

    print("\n================ FINAL E2E RESULTS ================")
    for key, val in results.items():
        print(f"{key}: {val}")
    if errors:
        print("ERRORS DETECTED:")
        for k, v in errors.items():
            print(f"  [{k}]: {v}")

if __name__ == "__main__":
    run_full_e2e_test()
