import requests
import sys
import time
import io
import os

sys.stdout.reconfigure(encoding='utf-8')

BASE_URL = "http://localhost:8080/api/v1"
ADMIN_EMAIL = "admin@prepgpt.com"
ADMIN_PASSWORD = "AdminPassword123!"

print("=" * 75)
print("PREPGPT PHASE 13: MASTER LOCALHOST E2E & SECURITY HARDENING SUITE")
print("=" * 75)

results = []

def record(test_id, name, passed, detail=""):
    status = "PASSED" if passed else "FAILED"
    results.append((test_id, name, passed, detail))
    print(f"[{status}] {test_id}: {name}")
    if detail:
        print(f"         {detail}")

ts = int(time.time())

# ==============================================================================
# SECTION A: STARTUP & HEALTH DB PROBE
# ==============================================================================
print("\n--- SECTION A: STARTUP & HEALTH DB PROBE ---")

r_health = requests.get(f"{BASE_URL}/health")
record("SEC-A1", "GET /api/v1/health returns 200 UP with active DB connectivity",
       r_health.status_code == 200 and r_health.json().get("status") == "UP",
       f"Status: {r_health.status_code}, Body: {r_health.text}")

# ==============================================================================
# SECTION B: AUTHENTICATION & SECURITY
# ==============================================================================
print("\n--- SECTION B: AUTHENTICATION & SECURITY ---")

student_a_email = f"student_a_{ts}@preptest.com"
student_b_email = f"student_b_{ts}@preptest.com"
password = "Password123!"

# B1: Register Student A
r_reg_a = requests.post(f"{BASE_URL}/auth/register", json={
    "fullName": "Student Alpha",
    "email": student_a_email,
    "password": password
})
record("SEC-B1", "Register Student A returns 201 Created",
       r_reg_a.status_code in (200, 201),
       f"Status: {r_reg_a.status_code}")

# B2: Login Student A & acquire JWT
r_login_a = requests.post(f"{BASE_URL}/auth/login", json={
    "email": student_a_email,
    "password": password
})
token_a = r_login_a.json().get("token") if r_login_a.status_code == 200 else None
student_a_id = r_login_a.json().get("user", {}).get("id") if token_a else None
headers_a = {"Authorization": f"Bearer {token_a}", "Content-Type": "application/json"}
record("SEC-B2", "Login Student A returns 200 with JWT bearer token",
       r_login_a.status_code == 200 and token_a is not None,
       f"Student ID: {student_a_id}")

# B3: Register and Login Student B
r_reg_b = requests.post(f"{BASE_URL}/auth/register", json={
    "fullName": "Student Beta",
    "email": student_b_email,
    "password": password
})
r_login_b = requests.post(f"{BASE_URL}/auth/login", json={
    "email": student_b_email,
    "password": password
})
token_b = r_login_b.json().get("token") if r_login_b.status_code == 200 else None
student_b_id = r_login_b.json().get("user", {}).get("id") if token_b else None
headers_b = {"Authorization": f"Bearer {token_b}", "Content-Type": "application/json"}
record("SEC-B3", "Register & Login Student B returns 200 with JWT bearer token",
       r_login_b.status_code == 200 and token_b is not None,
       f"Student B ID: {student_b_id}")

# B4: Bad password rejected with 401
r_bad_auth = requests.post(f"{BASE_URL}/auth/login", json={
    "email": student_a_email,
    "password": "WrongPassword123!"
})
record("SEC-B4", "Login with incorrect password rejected with 401 Unauthorized",
       r_bad_auth.status_code == 401,
       f"Status: {r_bad_auth.status_code}")

# ==============================================================================
# SECTION C: STUDENT DASHBOARD, PROFILE & INTERVIEW WORKFLOW
# ==============================================================================
print("\n--- SECTION C: STUDENT DASHBOARD, PROFILE & INTERVIEW WORKFLOW ---")

# C1: Dashboard summary
r_dash = requests.get(f"{BASE_URL}/dashboard", headers=headers_a)
record("SEC-C1", "GET /api/v1/dashboard returns 200 with student metrics summary",
       r_dash.status_code == 200 and "profile" in r_dash.json(),
       f"Status: {r_dash.status_code}")

# C2: Profile fetch
r_prof = requests.get(f"{BASE_URL}/profile", headers=headers_a)
record("SEC-C2", "GET /api/v1/profile returns 200 with user profile",
       r_prof.status_code == 200 and r_prof.json().get("email") == student_a_email,
       f"Profile email: {r_prof.json().get('email')}")

# C3: Profile update
r_prof_up = requests.put(f"{BASE_URL}/profile", headers=headers_a, json={
    "targetRole": "Backend Engineer",
    "experienceLevel": "Entry",
    "bio": "Passionate backend engineer building resilient distributed systems."
})
record("SEC-C3", "PUT /api/v1/profile updates student profile details",
       r_prof_up.status_code == 200 and r_prof_up.json().get("targetRole") == "Backend Engineer",
       f"Updated targetRole: {r_prof_up.json().get('targetRole') if r_prof_up.status_code == 200 else 'N/A'}")

# C4: Fetch categories and create interview session for Student A
r_cats = requests.get(f"{BASE_URL}/interview-categories", headers=headers_a)
cat_id = r_cats.json()[0]["id"] if (r_cats.status_code == 200 and len(r_cats.json()) > 0) else 1

r_session_a = requests.post(f"{BASE_URL}/interview-sessions", headers=headers_a, json={
    "categoryId": cat_id,
    "difficulty": "EASY",
    "mode": "QUESTION_BANK",
    "questionCount": 3,
    "durationMinutes": 30
})
session_a_id = r_session_a.json().get("id") if r_session_a.status_code in (200, 201) else None
record("SEC-C4", "Create Interview Session for Student A returns 200/201",
       session_a_id is not None,
       f"Session A ID: {session_a_id}")

# ==============================================================================
# SECTION D: RESUME & PROJECT WORKFLOW
# ==============================================================================
print("\n--- SECTION D: RESUME & PROJECT WORKFLOW ---")

# D1: Upload valid resume text file
resume_content = (
    "John Doe\n"
    "Education: B.Tech Computer Science\n"
    "Skills: Java, Spring Boot, MySQL, React, Docker\n"
    "Experience: Software Engineer Intern at Tech Corp\n"
    "Projects: PrepGPT Mock Interview Platform with automated scoring and voice feedback."
)
files = {
    'file': ('resume_test.txt', io.BytesIO(resume_content.encode('utf-8')), 'text/plain')
}
r_res_up = requests.post(f"{BASE_URL}/resumes/upload", headers={"Authorization": f"Bearer {token_a}"}, files=files)
resume_id = r_res_up.json().get("id") if r_res_up.status_code in (200, 201) else None
record("SEC-D1", "POST /api/v1/resumes/upload parses and stores valid resume",
       resume_id is not None and "PrepGPT" in r_res_up.json().get("extractedText", ""),
       f"Resume ID: {resume_id}, Extracted length: {len(r_res_up.json().get('extractedText', '')) if resume_id else 0}")

# D2: Create candidate project
r_proj = requests.post(f"{BASE_URL}/projects", headers=headers_a, json={
    "title": "Cloud Sentinel Monitoring System",
    "description": "Distributed metric collection pipeline built with Spring Boot and Kafka.",
    "technologies": "Java, Spring Boot, Kafka, MySQL, React",
    "roleContribution": "Engineered event ingestion consumers and REST API services.",
    "architectureDetails": "Microservice architecture with decoupled pub/sub consumers."
})
proj_id = r_proj.json().get("id") if r_proj.status_code in (200, 201) else None
record("SEC-D2", "POST /api/v1/projects creates candidate portfolio project",
       proj_id is not None,
       f"Project ID: {proj_id}")

# ==============================================================================
# SECTION E: JOB DESCRIPTION WORKFLOW
# ==============================================================================
print("\n--- SECTION E: JOB DESCRIPTION WORKFLOW ---")

r_jd = requests.post(f"{BASE_URL}/job-descriptions", headers=headers_a, json={
    "targetRole": "Associate Software Engineer - Java / Backend",
    "companyName": "Acme Software Labs",
    "rawText": (
        "Seeking an Associate Backend Engineer with strong fundamentals in Java 17, Spring Boot, "
        "REST APIs, relational databases (MySQL/PostgreSQL), and Docker containerization. "
        "The engineer will collaborate on high-scale microservices and campus placement platforms."
    )
})
jd_id = r_jd.json().get("id") if r_jd.status_code in (200, 201) else None
record("SEC-E1", "POST /api/v1/job-descriptions parses and creates target JD",
       jd_id is not None,
       f"JD ID: {jd_id}, Extracted Skills: {r_jd.json().get('extractedSkills', []) if jd_id else None}")

# ==============================================================================
# SECTION F: CODING SANDBOX / PROBLEMS
# ==============================================================================
print("\n--- SECTION F: CODING SANDBOX / PROBLEMS ---")

r_problems = requests.get(f"{BASE_URL}/coding/problems", headers=headers_a)
record("SEC-F1", "GET /api/v1/coding/problems returns catalog of coding challenges",
       r_problems.status_code == 200,
       f"Status: {r_problems.status_code}, Problems count: {len(r_problems.json()) if isinstance(r_problems.json(), list) else 'N/A'}")

# ==============================================================================
# SECTION G: ADMIN SYSTEM & GOVERNANCE
# ==============================================================================
print("\n--- SECTION G: ADMIN SYSTEM & GOVERNANCE ---")

# G1: Authenticate Admin
r_admin_login = requests.post(f"{BASE_URL}/auth/login", json={
    "email": ADMIN_EMAIL,
    "password": ADMIN_PASSWORD
})
admin_token = r_admin_login.json().get("token") if r_admin_login.status_code == 200 else None
admin_id = r_admin_login.json().get("user", {}).get("id") if admin_token else None
admin_headers = {"Authorization": f"Bearer {admin_token}", "Content-Type": "application/json"}
record("SEC-G1", "Admin authentication succeeds with ROLE_ADMIN credentials",
       admin_token is not None and r_admin_login.json().get("user", {}).get("role") == "ROLE_ADMIN",
       f"Admin ID: {admin_id}")

# G2: Admin Platform Stats
r_admin_stats = requests.get(f"{BASE_URL}/admin/stats", headers=admin_headers)
record("SEC-G2", "GET /api/v1/admin/stats returns aggregate platform statistics",
       r_admin_stats.status_code == 200 and "totalUsers" in r_admin_stats.json(),
       f"Total Users: {r_admin_stats.json().get('totalUsers') if r_admin_stats.status_code == 200 else 'N/A'}")

# G3: Admin Paginated User List
r_admin_users = requests.get(f"{BASE_URL}/admin/users?page=0&size=10", headers=admin_headers)
record("SEC-G3", "GET /api/v1/admin/users?page=0&size=10 returns paginated user list",
       r_admin_users.status_code == 200 and "users" in r_admin_users.json(),
       f"User count in page: {len(r_admin_users.json().get('users', [])) if r_admin_users.status_code == 200 else 0}")

# G4: Deactivate Student B -> 200 OK
r_deact = requests.patch(f"{BASE_URL}/admin/users/{student_b_id}/deactivate", headers=admin_headers)
record("SEC-G4", "PATCH /api/v1/admin/users/{id}/deactivate soft-deactivates candidate account",
       r_deact.status_code == 200,
       f"Status: {r_deact.status_code}")

# G5: Deactivated Student B token rejected immediately -> 401 Unauthorized
r_deact_access = requests.get(f"{BASE_URL}/profile", headers=headers_b)
record("SEC-G5", "Deactivated user token rejected immediately with 401 Unauthorized",
       r_deact_access.status_code == 401,
       f"Status: {r_deact_access.status_code}")

# G6: Reactivate Student B -> 200 OK
r_react = requests.patch(f"{BASE_URL}/admin/users/{student_b_id}/reactivate", headers=admin_headers)
record("SEC-G6", "PATCH /api/v1/admin/users/{id}/reactivate reactivates candidate account",
       r_react.status_code == 200,
       f"Status: {r_react.status_code}")

# G7: Self-Deactivation Blocked for Admin
r_self_deact = requests.patch(f"{BASE_URL}/admin/users/{admin_id}/deactivate", headers=admin_headers)
record("SEC-G7", "Admin self-deactivation blocked with 400 Bad Request",
       r_self_deact.status_code == 400,
       f"Status: {r_self_deact.status_code}")

# G8: Sole Active Admin Demotion Blocked
r_self_demote = requests.patch(f"{BASE_URL}/admin/users/{admin_id}/role", headers=admin_headers, json={"newRole": "ROLE_STUDENT"})
record("SEC-G8", "Sole active admin demotion blocked (invariant enforced)",
       r_self_demote.status_code == 400,
       f"Status: {r_self_demote.status_code}")

# G9: Immutable Audit Log Inspection
r_audit = requests.get(f"{BASE_URL}/admin/audit-log?page=0&size=10", headers=admin_headers)
record("SEC-G9", "GET /api/v1/admin/audit-log returns append-only compliance trail",
       r_audit.status_code == 200 and len(r_audit.json().get("entries", [])) > 0,
       f"Audit entries count: {len(r_audit.json().get('entries', [])) if r_audit.status_code == 200 else 0}")

# G10: Audit Log Immutability (DELETE endpoint is 404 or 405)
r_audit_del = requests.delete(f"{BASE_URL}/admin/audit-log/1", headers=admin_headers)
record("SEC-G10", "DELETE /admin/audit-log/{id} rejected (immutable audit log)",
       r_audit_del.status_code in (404, 405),
       f"Status: {r_audit_del.status_code}")

# ==============================================================================
# SECTION H: PHASE 13 SECURITY HARDENING & DEFENSE IN DEPTH
# ==============================================================================
print("\n--- SECTION H: PHASE 13 SECURITY HARDENING & DEFENSE IN DEPTH ---")

# H1: CORS Preflight includes PATCH method
r_cors = requests.options(f"{BASE_URL}/admin/users/{student_a_id}/deactivate", headers={
    "Origin": "http://localhost:5173",
    "Access-Control-Request-Method": "PATCH",
    "Access-Control-Request-Headers": "Authorization,Content-Type"
})
allow_methods = r_cors.headers.get("Access-Control-Allow-Methods", "")
record("SEC-H1", "CORS Preflight OPTIONS permits PATCH method",
       r_cors.status_code == 200 and "PATCH" in allow_methods,
       f"Status: {r_cors.status_code}, Allow-Methods: {allow_methods}")

# H2: Security Headers Present
r_headers = requests.get(f"{BASE_URL}/health")
has_nosniff = r_headers.headers.get("X-Content-Type-Options") == "nosniff"
has_frame_deny = r_headers.headers.get("X-Frame-Options") in ("DENY", "SAMEORIGIN")
record("SEC-H2", "HTTP Security Headers configured (nosniff, frame-options)",
       has_nosniff and has_frame_deny,
       f"nosniff: {has_nosniff}, frame-options: {r_headers.headers.get('X-Frame-Options')}")

# H3: Pagination page < 0 rejected with 400
r_pag_neg = requests.get(f"{BASE_URL}/admin/users?page=-1&size=10", headers=admin_headers)
record("SEC-H3", "Admin Pagination: page < 0 rejected with 400 Bad Request",
       r_pag_neg.status_code == 400 and "negative" in r_pag_neg.text.lower(),
       f"Status: {r_pag_neg.status_code}, Body: {r_pag_neg.text}")

# H4: Pagination size < 1 rejected with 400
r_pag_zero = requests.get(f"{BASE_URL}/admin/users?page=0&size=0", headers=admin_headers)
record("SEC-H4", "Admin Pagination: size < 1 rejected with 400 Bad Request",
       r_pag_zero.status_code == 400 and "at least 1" in r_pag_zero.text.lower(),
       f"Status: {r_pag_zero.status_code}, Body: {r_pag_zero.text}")

# H5: Pagination size > 100 rejected with 400
r_pag_large = requests.get(f"{BASE_URL}/admin/users?page=0&size=101", headers=admin_headers)
record("SEC-H5", "Admin Pagination: size > 100 rejected with 400 Bad Request",
       r_pag_large.status_code == 400 and "100" in r_pag_large.text.lower(),
       f"Status: {r_pag_large.status_code}, Body: {r_pag_large.text}")

# H6: Valid boundary page=0 & size=100 succeeds with 200 OK
r_pag_valid = requests.get(f"{BASE_URL}/admin/users?page=0&size=100", headers=admin_headers)
record("SEC-H6", "Admin Pagination: valid boundary page=0 & size=100 returns 200 OK",
       r_pag_valid.status_code == 200,
       f"Status: {r_pag_valid.status_code}")

# H7: Cross-User Data Isolation: Student B cannot access Student A's interview session
if session_a_id:
    r_cross = requests.get(f"{BASE_URL}/interview-sessions/{session_a_id}", headers=headers_b)
    record("SEC-H7", "Cross-User Isolation: Student B cannot view Student A's session (403/404)",
           r_cross.status_code in (403, 404),
           f"Status: {r_cross.status_code}")
else:
    record("SEC-H7", "Cross-User Isolation (Skipped - No session A)", False)

# H8: Rate Limiting: Burst authentication attempts trigger 429 and Retry-After header
rapid_blocked = False
retry_after_found = False
for attempt in range(12):
    r_burst = requests.post(f"{BASE_URL}/auth/login", json={
        "email": f"burst_test_{ts}@preptest.com",
        "password": "WrongPassword123!"
    })
    if r_burst.status_code == 429:
        rapid_blocked = True
        retry_after_found = "Retry-After" in r_burst.headers
        break

record("SEC-H8", "Rate Limiting: Rapid Auth bursts trigger HTTP 429 with Retry-After header",
       rapid_blocked and retry_after_found,
       f"Rate limited: {rapid_blocked}, Retry-After present: {retry_after_found}")

# ==============================================================================
# SECTION I: REAL GROQ PROVIDER VERIFICATION (Master Plan Requirement)
# ==============================================================================
print("\n--- SECTION I: REAL GROQ PROVIDER VERIFICATION ---")

# I0: Verify existing Groq configuration without exposing actual API key
env_path = r"c:\Users\likhi\OneDrive\Desktop\PrepGpt\backend\.env"
groq_models = {}
if os.path.exists(env_path):
    with open(env_path, "r", encoding="utf-8") as f:
        for line in f:
            line = line.strip()
            if line.startswith("GROQ_AI_MODEL="):
                groq_models["AI"] = line.split("=", 1)[1].strip()
            elif line.startswith("GROQ_STT_MODEL="):
                groq_models["STT"] = line.split("=", 1)[1].strip()
            elif line.startswith("GROQ_TTS_MODEL="):
                groq_models["TTS"] = line.split("=", 1)[1].strip()

record("SEC-I0", "Groq Configuration: Model names detected and verified",
       "AI" in groq_models and "STT" in groq_models and "TTS" in groq_models,
       f"AI: {groq_models.get('AI')}, STT: {groq_models.get('STT')}, TTS: {groq_models.get('TTS')}")

# I1: Real Groq AI Question Generation (Confirm MockAIService is bypassed)
r_session_ai = requests.post(f"{BASE_URL}/interview-sessions", headers=headers_a, json={
    "categoryId": cat_id,
    "difficulty": "MEDIUM",
    "mode": "AI",
    "interactionMode": "VOICE",
    "questionCount": 2,
    "durationMinutes": 20
})
session_ai_id = r_session_ai.json().get("id") if r_session_ai.status_code in (200, 201) else None

# Start session -> triggers Groq AI question generation
r_start_ai = requests.post(f"{BASE_URL}/interview-sessions/{session_ai_id}/start", headers=headers_a)
r_curr_q = requests.get(f"{BASE_URL}/interview-sessions/{session_ai_id}/current-question", headers=headers_a)
ai_q_data = r_curr_q.json() if r_curr_q.status_code == 200 else {}
ai_q_text = ai_q_data.get("questionText", "")
ai_q_id = ai_q_data.get("id")

# MockAIService always generates "AI Mock Question #1 for ... Explain key principles and trade-offs."
is_mock_ai_q = ai_q_text.startswith("AI Mock Question") or "Mock Question" in ai_q_text
record("SEC-I1", "Groq AI: Real Question Generation (MockAIService bypassed)",
       len(ai_q_text) > 15 and not is_mock_ai_q,
       f"Question ID: {ai_q_id}, Preview: '{ai_q_text[:70]}...', Mock Bypassed: {not is_mock_ai_q}")

# I2: Real Groq Orpheus TTS Voice Synthesis (Confirm MockVoiceService is bypassed)
speech_test_text = "Hello, can you hear me? Testing 1, 2, 3."
r_tts = requests.post(f"{BASE_URL}/interview-sessions/{session_ai_id}/voice/synthesize",
                      headers=headers_a,
                      json={"text": speech_test_text})
tts_bytes = r_tts.content if r_tts.status_code == 200 else b""
tts_len = len(tts_bytes)
tts_content_type = r_tts.headers.get("Content-Type", "")

# MockVoiceService returns exactly 44 bytes header; Groq Orpheus returns > 10,000 bytes
is_mock_tts = (tts_len <= 44)
record("SEC-I2", "Groq TTS: Real speech synthesized via Orpheus (MockVoiceService bypassed)",
       r_tts.status_code == 200 and tts_len > 1000 and not is_mock_tts,
       f"Status: {r_tts.status_code}, Bytes: {tts_len}, Content-Type: {tts_content_type}, Mock Bypassed: {not is_mock_tts}")

# I3: Real Groq Whisper STT Voice Transcription (Confirm MockVoiceService is bypassed)
# Send the genuine speech WAV audio synthesized by Orpheus to Whisper STT via localhost endpoint
files_stt = {
    'file': ('speech.wav', io.BytesIO(tts_bytes), 'audio/wav')
}
r_stt = requests.post(f"{BASE_URL}/interview-sessions/{session_ai_id}/voice/transcribe",
                      headers={"Authorization": f"Bearer {token_a}"},
                      files=files_stt)
stt_json = r_stt.json() if r_stt.status_code == 200 else {}
stt_text = stt_json.get("text") or stt_json.get("transcription", "")
# MockVoiceService always returns: "I believe the core tradeoff involves balancing memory overhead with time complexity."
is_mock_stt = ("tradeoff involves balancing memory overhead" in stt_text)
has_spoken_words = any(w in stt_text.lower() for w in ["hello", "hear", "testing", "1", "one", "two", "three"])
record("SEC-I3", "Groq STT: Real speech transcribed via Whisper (MockVoiceService bypassed)",
       r_stt.status_code == 200 and has_spoken_words and not is_mock_stt,
       f"Status: {r_stt.status_code}, Transcribed: '{stt_text.strip()}', Mock Bypassed: {not is_mock_stt}")

# I4: Real Groq AI Answer Evaluation (Confirm MockAIService is bypassed)
candidate_ans = (
    "An interface in Java defines a contract with abstract and default methods where a class can implement multiple interfaces. "
    "An abstract class can hold instance state with fields, constructors, and template methods, but Java single inheritance restricts classes to extending only one abstract class."
)
r_eval = requests.post(f"{BASE_URL}/interview-sessions/{session_ai_id}/questions/{ai_q_id}/answer",
                       headers=headers_a,
                       json={"answerText": candidate_ans, "inputMethod": "VOICE"})
eval_resp = r_eval.json() if r_eval.status_code == 200 else {}
eval_data = eval_resp.get("evaluation", {})
eval_score = eval_data.get("score")
eval_feedback = eval_data.get("feedback", "")
eval_strengths = eval_data.get("strengths", [])

# MockAIService always returns feedback: "Good explanation of the fundamental concepts. Expanding on real-world edge cases will strengthen your response."
is_mock_eval = ("Expanding on real-world edge cases will strengthen your response" in eval_feedback)
record("SEC-I4", "Groq AI: Real Answer Evaluation & Scoring (MockAIService bypassed)",
       r_eval.status_code == 200 and eval_score is not None and not is_mock_eval,
       f"Score: {eval_score}/10, Strengths: {len(eval_strengths)}, Mock Bypassed: {not is_mock_eval}")

# ==============================================================================
# FINAL RESULTS SUMMARY
# ==============================================================================
print("\n" + "=" * 75)
passed_count = sum(1 for _, _, p, _ in results if p)
total_count = len(results)
print(f"PHASE 13 LOCALHOST VERIFICATION SUMMARY: {passed_count}/{total_count} PASSED")
print("=" * 75)

if passed_count == total_count:
    print(f"ALL {total_count}/{total_count} PHASE 13 SECURITY HARDENING AND END-TO-END TESTS (SECTIONS A-I) PASSED!")
    sys.exit(0)
else:
    print(f"FAILED: {total_count - passed_count} tests failed.")
    for tid, name, p, d in results:
        if not p:
            print(f"  [FAIL] {tid}: {name} -> {d}")
    sys.exit(1)

