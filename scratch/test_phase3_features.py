import urllib.request
import json
import time

BASE_URL = "http://localhost:8080/api/v1"

def make_request(url, method="GET", data=None, token=None):
    req = urllib.request.Request(url, method=method)
    req.add_header("Content-Type", "application/json")
    if token:
        req.add_header("Authorization", f"Bearer {token}")
    
    body = json.dumps(data).encode("utf-8") if data else None
    try:
        with urllib.request.urlopen(req, data=body) as resp:
            return resp.status, json.loads(resp.read().decode("utf-8"))
    except urllib.error.HTTPError as e:
        error_body = e.read().decode("utf-8")
        try:
            return e.code, json.loads(error_body)
        except:
            return e.code, error_body

print("--- STARTING PHASE 3 DASHBOARD & INTERVIEW CONFIGURATION TESTS ---")

# 1. Register and Login Candidate 1
ts = int(time.time())
user1_email = f"student1_{ts}@university.edu"
make_request(f"{BASE_URL}/auth/register", method="POST", data={"fullName": "Candidate One", "email": user1_email, "password": "Password123"})
code, res1 = make_request(f"{BASE_URL}/auth/login", method="POST", data={"email": user1_email, "password": "Password123"})
token1 = res1.get("token")
print(f"[TEST 1] Candidate 1 Authenticated | Token: {bool(token1)}")

# 2. Register and Login Candidate 2 (Security Boundary Verification)
user2_email = f"student2_{ts}@university.edu"
make_request(f"{BASE_URL}/auth/register", method="POST", data={"fullName": "Candidate Two", "email": user2_email, "password": "Password123"})
code, res2 = make_request(f"{BASE_URL}/auth/login", method="POST", data={"email": user2_email, "password": "Password123"})
token2 = res2.get("token")
print(f"[TEST 2] Candidate 2 Authenticated | Token: {bool(token2)}")

# 3. GET /interview-categories (Unauthenticated vs Authenticated)
code, _ = make_request(f"{BASE_URL}/interview-categories")
print(f"[TEST 3A] Unauthenticated GET /interview-categories: Status {code}")
assert code == 401, "Categories endpoint must require authentication!"

code, categories = make_request(f"{BASE_URL}/interview-categories", token=token1)
print(f"[TEST 3B] Authenticated GET /interview-categories: Status {code} | Categories Found: {len(categories)}")
assert code == 200, "Failed to load categories!"
assert len(categories) == 8, "Expected 8 default active categories!"

category_id = categories[0]["id"]
category_name = categories[0]["name"]
print(f"Selected Category: '{category_name}' (ID: {category_id})")

# 4. Input Validation Tests on Session Creation
# 4A. Invalid Question Count (0)
bad_payload = {"categoryId": category_id, "difficulty": "MEDIUM", "questionCount": 0, "durationMinutes": 30}
code, res = make_request(f"{BASE_URL}/interview-sessions", method="POST", data=bad_payload, token=token1)
print(f"[TEST 4A] Validation QuestionCount 0: Status {code}")
assert code == 400, "Validation failed to reject questionCount 0!"

# 4B. Invalid Duration (2 mins)
bad_payload = {"categoryId": category_id, "difficulty": "MEDIUM", "questionCount": 10, "durationMinutes": 2}
code, res = make_request(f"{BASE_URL}/interview-sessions", method="POST", data=bad_payload, token=token1)
print(f"[TEST 4B] Validation Duration 2 mins: Status {code}")
assert code == 400, "Validation failed to reject duration 2 mins!"

# 5. Create Valid Interview Session for Candidate 1
session_payload = {
    "categoryId": category_id,
    "difficulty": "MEDIUM",
    "questionCount": 10,
    "durationMinutes": 30
}
code, session1 = make_request(f"{BASE_URL}/interview-sessions", method="POST", data=session_payload, token=token1)
print(f"[TEST 5] Create Session: Status {code} | Session ID: {session1.get('id')} | Status: {session1.get('status')}")
assert code == 201, "Session creation failed!"
assert session1.get("status") == "CONFIGURED", "Initial status must be CONFIGURED!"
session_id = session1.get("id")

# 6. Session Ownership Security Boundary Test
code, res = make_request(f"{BASE_URL}/interview-sessions/{session_id}", token=token2)
print(f"[TEST 6] Candidate 2 accessing Candidate 1 Session: Status {code}")
assert code == 404, "Security violation: Candidate 2 accessed Candidate 1 session!"

# 7. Candidate 1 Starts Session (CONFIGURED -> IN_PROGRESS)
code, started_session = make_request(f"{BASE_URL}/interview-sessions/{session_id}/start", method="POST", token=token1)
print(f"[TEST 7] Start Session: Status {code} | New Status: {started_session.get('status')} | StartedAt: {bool(started_session.get('startedAt'))}")
assert code == 200, "Start session failed!"
assert started_session.get("status") == "IN_PROGRESS", "Status must transition to IN_PROGRESS!"

# 8. GET Candidate 1 Dashboard
code, dashboard = make_request(f"{BASE_URL}/dashboard", token=token1)
print(f"[TEST 8] GET /dashboard: Status {code} | Total: {dashboard['statistics']['totalInterviews']} | InProgress: {dashboard['statistics']['inProgressInterviews']}")
assert code == 200, "GET /dashboard failed!"
assert dashboard['statistics']['totalInterviews'] == 1, "Expected 1 total interview in dashboard!"
assert dashboard['statistics']['inProgressInterviews'] == 1, "Expected 1 in-progress interview!"
assert len(dashboard['recentInterviews']) == 1, "Expected 1 recent interview session!"

# 9. Candidate 1 Abandons Session (IN_PROGRESS -> ABANDONED)
code, abandoned_session = make_request(f"{BASE_URL}/interview-sessions/{session_id}/abandon", method="POST", token=token1)
print(f"[TEST 9] Abandon Session: Status {code} | Status: {abandoned_session.get('status')}")
assert code == 200, "Abandon session failed!"
assert abandoned_session.get("status") == "ABANDONED", "Status must transition to ABANDONED!"

# 10. Attempting to Restart ABANDONED session (Invalid Transition)
code, res = make_request(f"{BASE_URL}/interview-sessions/{session_id}/start", method="POST", token=token1)
print(f"[TEST 10] Restart ABANDONED Session: Status {code} | Error: {res.get('message')}")
assert code == 400, "Invalid transition allowed!"

print("\nALL PHASE 3 DASHBOARD & INTERVIEW CONFIGURATION TESTS PASSED SUCCESSFULLY!")
