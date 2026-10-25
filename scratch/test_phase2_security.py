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

print("--- STARTING PHASE 2 SECURITY & API VERIFICATION ---")

# 1. Health API (Public)
code, res = make_request(f"{BASE_URL}/health")
print(f"[TEST 1] GET /health: Status {code} | App: {res.get('application')}")
assert code == 200, "Health API failed!"

# 2. Register New Student
timestamp = int(time.time())
student_email = f"student_{timestamp}@university.edu"
reg_payload = {
    "fullName": "Student Candidate",
    "email": student_email,
    "password": "SecurePassword123"
}
code, res = make_request(f"{BASE_URL}/auth/register", method="POST", data=reg_payload)
print(f"[TEST 2] POST /auth/register: Status {code} | User ID: {res.get('id')} | Role: {res.get('role')}")
assert code == 201, "Registration failed!"
assert res.get("role") == "ROLE_STUDENT", "Role must be ROLE_STUDENT!"
assert "password" not in res and "passwordHash" not in res, "Password leaked in register response!"

# 3. Duplicate Email Registration (Conflict)
code, res = make_request(f"{BASE_URL}/auth/register", method="POST", data=reg_payload)
print(f"[TEST 3] Duplicate Register (Conflict): Status {code} | Error: {res.get('message')}")
assert code == 409, "Duplicate email check failed!"

# 4. Login with Invalid Credentials
code, res = make_request(f"{BASE_URL}/auth/login", method="POST", data={"email": student_email, "password": "WrongPassword"})
print(f"[TEST 4] Invalid Login: Status {code} | Error: {res.get('message')}")
assert code == 401, "Invalid login allowed!"

# 5. Login with Valid Credentials
code, res = make_request(f"{BASE_URL}/auth/login", method="POST", data={"email": student_email, "password": "SecurePassword123"})
print(f"[TEST 5] Valid Login: Status {code} | Token Received: {bool(res.get('token'))}")
assert code == 200, "Login failed!"
token = res.get("token")
assert token is not None, "JWT token missing!"

# 6. Unauthenticated Protected Endpoint Access
code, res = make_request(f"{BASE_URL}/auth/me")
print(f"[TEST 6] Unauthenticated /auth/me: Status {code}")
assert code == 401, "Unauthenticated access allowed!"

# 7. Authenticated GET /auth/me
code, res = make_request(f"{BASE_URL}/auth/me", token=token)
print(f"[TEST 7] Authenticated /auth/me: Status {code} | User Email: {res.get('email')}")
assert code == 200, "Authenticated /auth/me failed!"

# 8. Authenticated GET /profile
code, res = make_request(f"{BASE_URL}/profile", token=token)
print(f"[TEST 8] GET /profile: Status {code} | Target Role: {res.get('targetRole')}")
assert code == 200, "GET /profile failed!"

# 9. Authenticated PUT /profile
update_payload = {
    "bio": "Passionate Java candidate seeking software engineer placement.",
    "targetRole": "Senior Java Developer",
    "experienceLevel": "FRESHER",
    "phone": "+91 9876543210",
    "githubUrl": "https://github.com/studentcandidate",
    "linkedinUrl": "https://linkedin.com/in/studentcandidate"
}
code, res = make_request(f"{BASE_URL}/profile", method="PUT", data=update_payload, token=token)
print(f"[TEST 9] PUT /profile: Status {code} | Updated Bio: {res.get('bio')}")
assert code == 200, "PUT /profile failed!"
assert res.get("targetRole") == "Senior Java Developer", "Profile update failed!"

print("\nALL PHASE 2 SECURITY & API VERIFICATION TESTS PASSED SUCCESSFULLY!")
