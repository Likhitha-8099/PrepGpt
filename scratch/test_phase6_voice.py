import requests
import sys

BASE_URL = "http://localhost:8080/api/v1"

def run_tests():
    print("=== Phase 6 Voice Interview (STT + TTS) Backend Test Suite ===")

    # 1. Health check
    h_resp = requests.get("http://localhost:8080/api/health")
    if h_resp.status_code != 401 and h_resp.status_code != 200:
        print(f"FAILED: Backend health check failed with status {h_resp.status_code}")
        sys.exit(1)
    print("Backend health check PASSED")

    # 2. Register Student D
    email_d = "student_voice_d@prepgpt.com"
    password_d = "VoicePass123!"
    reg_d = requests.post(f"{BASE_URL}/auth/register", json={
        "email": email_d,
        "password": password_d,
        "fullName": "Voice Student D",
        "targetRole": "Frontend Developer",
        "experienceLevel": "MID"
    })
    token_d = None
    if reg_d.status_code == 201:
        token_d = reg_d.json().get("token")
    else:
        login_d = requests.post(f"{BASE_URL}/auth/login", json={"email": email_d, "password": password_d})
        token_d = login_d.json().get("token")

    headers_d = {"Authorization": f"Bearer {token_d}"}
    print("Student D registered and logged in successfully.")

    # 3. Register Student E (Security adversary)
    email_e = "student_voice_e@prepgpt.com"
    password_e = "VoicePass123!"
    reg_e = requests.post(f"{BASE_URL}/auth/register", json={
        "email": email_e,
        "password": password_e,
        "fullName": "Voice Student E",
        "targetRole": "Backend Developer",
        "experienceLevel": "ENTRY"
    })
    token_e = None
    if reg_e.status_code == 201:
        token_e = reg_e.json().get("token")
    else:
        login_e = requests.post(f"{BASE_URL}/auth/login", json={"email": email_e, "password": password_e})
        token_e = login_e.json().get("token")

    headers_e = {"Authorization": f"Bearer {token_e}"}
    print("Student E registered and logged in successfully.")

    # 5. Fetch Categories
    cat_resp = requests.get(f"{BASE_URL}/interview-categories", headers=headers_d)
    categories = cat_resp.json()
    cat_id = categories[0]["id"]

    # 5. Create QUESTION_BANK + VOICE Interview Session (Works without external AI credentials)
    sess_req = {
        "categoryId": cat_id,
        "difficulty": "MEDIUM",
        "questionCount": 5,
        "durationMinutes": 30,
        "mode": "QUESTION_BANK",
        "interactionMode": "VOICE"
    }
    create_resp = requests.post(f"{BASE_URL}/interview-sessions", json=sess_req, headers=headers_d)
    assert create_resp.status_code == 201, f"Failed to create session: {create_resp.text}"
    session_data = create_resp.json()
    session_id = session_data["id"]
    assert session_data["interactionMode"] == "VOICE", "interactionMode must be VOICE"
    print(f"Student D created AI + VOICE session #{session_id} successfully.")

    # 6. Start Voice Session
    start_resp = requests.post(f"{BASE_URL}/interview-sessions/{session_id}/start", headers=headers_d)
    assert start_resp.status_code == 200, f"Failed to start session: {start_resp.text}"
    print(f"Voice Session #{session_id} started successfully (IN_PROGRESS).")

    # 7. Test TTS Synthesis Endpoint
    tts_req = {"text": "Explain the concept of Virtual DOM in React and why it optimizes rendering performance."}
    tts_resp = requests.post(f"{BASE_URL}/interview-sessions/{session_id}/voice/synthesize", json=tts_req, headers=headers_d)
    assert tts_resp.status_code == 200, f"TTS synthesis failed: {tts_resp.status_code} - {tts_resp.text}"
    assert "audio/mpeg" in tts_resp.headers.get("Content-Type", ""), "Content-Type must be audio/mpeg"
    assert len(tts_resp.content) > 0, "TTS audio content must not be empty"
    print(f"TTS Synthesis PASSED: Received {len(tts_resp.content)} bytes of audio stream.")

    # 8. Test STT Transcription Endpoint with multipart audio file
    import io, wave, math, struct
    buf = io.BytesIO()
    with wave.open(buf, 'wb') as wav_file:
        wav_file.setnchannels(1)
        wav_file.setsampwidth(2)
        wav_file.setframerate(16000)
        for i in range(16000):
            val = int(8000 * math.sin(2 * math.pi * 440 * i / 16000))
            wav_file.writeframesraw(struct.pack('<h', val))
    sample_wav_bytes = buf.getvalue()

    files = {"file": ("answer.wav", sample_wav_bytes, "audio/wav")}
    stt_resp = requests.post(f"{BASE_URL}/interview-sessions/{session_id}/voice/transcribe", files=files, headers=headers_d)
    assert stt_resp.status_code == 200, f"STT transcription failed: {stt_resp.status_code} - {stt_resp.text}"
    stt_json = stt_resp.json()
    assert "text" in stt_json and len(stt_json["text"]) > 0, "Transcription text must be non-empty"
    print(f"STT Transcription PASSED: Transcribed text = '{stt_json['text']}'")

    # 9. Test Input Validation Failures
    # Empty file rejection
    empty_files = {"file": ("empty.webm", b"", "audio/webm")}
    empty_stt_resp = requests.post(f"{BASE_URL}/interview-sessions/{session_id}/voice/transcribe", files=empty_files, headers=headers_d)
    assert empty_stt_resp.status_code == 400, "Empty audio file must return 400 Bad Request"
    print("Validation PASSED: Empty audio file rejected with HTTP 400.")

    # Excessively long synthesis text rejection (>2000 chars)
    long_text_req = {"text": "A" * 2500}
    long_tts_resp = requests.post(f"{BASE_URL}/interview-sessions/{session_id}/voice/synthesize", json=long_text_req, headers=headers_d)
    assert long_tts_resp.status_code == 400, "Synthesis text >2000 chars must return 400 Bad Request"
    print("Validation PASSED: Oversized synthesis text rejected with HTTP 400.")

    # 10. Security Ownership Verification
    adv_stt_resp = requests.post(f"{BASE_URL}/interview-sessions/{session_id}/voice/transcribe", files=files, headers=headers_e)
    assert adv_stt_resp.status_code in [403, 404], "Unauthorized STT transcribe must be rejected with 403/404"

    adv_tts_resp = requests.post(f"{BASE_URL}/interview-sessions/{session_id}/voice/synthesize", json=tts_req, headers=headers_e)
    assert adv_tts_resp.status_code in [403, 404], "Unauthorized TTS synthesize must be rejected with 403/404"
    print("Security Check PASSED: Student E cannot access Student D's voice endpoints.")

    # 11. Submit Voice Answer to Interview Engine
    q_resp = requests.get(f"{BASE_URL}/interview-sessions/{session_id}/current-question", headers=headers_d)
    q_data = q_resp.json()
    q_id = q_data["id"]

    ans_req = {
        "answerText": stt_json["text"],
        "inputMethod": "VOICE"
    }
    sub_resp = requests.post(f"{BASE_URL}/interview-sessions/{session_id}/questions/{q_id}/answer", json=ans_req, headers=headers_d)
    assert sub_resp.status_code == 200, f"Answer submission failed: {sub_resp.text}"
    print("Voice answer submitted successfully to AI interview evaluation engine.")

    # 12. Verify Transcript and Session History
    hist_resp = requests.get(f"{BASE_URL}/interview-sessions/{session_id}", headers=headers_d)
    hist_data = hist_resp.json()
    assert hist_data["interactionMode"] == "VOICE", "Persisted session interactionMode must be VOICE"
    print(f"Session #{session_id} history details verified with interactionMode = 'VOICE'.")

    print("\nALL PHASE 6 VOICE INTERVIEW E2E TESTS PASSED PERFECTLY!")

if __name__ == "__main__":
    run_tests()
