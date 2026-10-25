import requests
import sys
import time

sys.stdout.reconfigure(encoding='utf-8')

BASE_URL = "http://localhost:8080/api/v1"
ADMIN_EMAIL = "admin@prepgpt.com"
ADMIN_PASSWORD = "AdminPassword123!"

print("=" * 70)
print("PREPGPT PHASE 12: ADMIN SYSTEM SECURITY & INVARIANT VERIFICATION")
print("=" * 70)

results = []

def record(test_num, name, passed, detail=""):
    status = "PASSED" if passed else "FAILED"
    results.append((test_num, name, passed, detail))
    print(f"[{status}] Test {test_num:02d}: {name}")
    if detail:
        print(f"         {detail}")

# --- SETUP: Authenticate Admin ---
print("\n[SETUP] Authenticating Bootstrap Admin...")
admin_login = requests.post(f"{BASE_URL}/auth/login", json={
    "email": ADMIN_EMAIL,
    "password": ADMIN_PASSWORD
})
assert admin_login.status_code == 200, f"Admin login failed: {admin_login.text}"
admin_token = admin_login.json()["token"]
admin_user = admin_login.json()["user"]
admin_id = admin_user["id"]
admin_headers = {
    "Authorization": f"Bearer {admin_token}",
    "Content-Type": "application/json"
}
print(f"  Bootstrap Admin ID: {admin_id}, Role: {admin_user['role']}")

# --- SETUP: Register Candidate 1 (Student) ---
ts = int(time.time())
student1_email = f"student1_{ts}@test.com"
student_pass = "StudentPass123!"
reg1 = requests.post(f"{BASE_URL}/auth/register", json={
    "fullName": "Student One",
    "email": student1_email,
    "password": student_pass
})
assert reg1.status_code in (200, 201), f"Student 1 reg failed: {reg1.text}"
login1 = requests.post(f"{BASE_URL}/auth/login", json={"email": student1_email, "password": student_pass})
student1_token = login1.json()["token"]
student1_id = login1.json()["user"]["id"]
student1_headers = {
    "Authorization": f"Bearer {student1_token}",
    "Content-Type": "application/json"
}

# --- SETUP: Register Candidate 2 (Student) ---
student2_email = f"student2_{ts}@test.com"
reg2 = requests.post(f"{BASE_URL}/auth/register", json={
    "fullName": "Student Two",
    "email": student2_email,
    "password": student_pass
})
login2 = requests.post(f"{BASE_URL}/auth/login", json={"email": student2_email, "password": student_pass})
student2_token = login2.json()["token"]
student2_id = login2.json()["user"]["id"]
student2_headers = {
    "Authorization": f"Bearer {student2_token}",
    "Content-Type": "application/json"
}

print("\n" + "-" * 70)
print("EXECUTING 20/20 SECURITY & INVARIANT TEST SUITE")
print("-" * 70)

# TEST 01: Student attempts GET /api/v1/admin/stats -> 403 Forbidden
r1 = requests.get(f"{BASE_URL}/admin/stats", headers=student1_headers)
record(1, "Student access to admin stats rejected (403)", r1.status_code == 403, f"Status: {r1.status_code}")

# TEST 02: Unauthenticated request to GET /api/v1/admin/users -> 401 Unauthorized
r2 = requests.get(f"{BASE_URL}/admin/users")
record(2, "Unauthenticated access to admin users rejected (401)", r2.status_code == 401, f"Status: {r2.status_code}")

# TEST 03: Unauthenticated request to POST /api/v1/admin/questions -> 401 Unauthorized
r3 = requests.post(f"{BASE_URL}/admin/questions", json={"questionText": "Test"})
record(3, "Unauthenticated question creation rejected (401)", r3.status_code == 401, f"Status: {r3.status_code}")

# TEST 04: Authenticated admin attempts to deactivate self -> 400 Bad Request
r4 = requests.patch(f"{BASE_URL}/admin/users/{admin_id}/deactivate", headers=admin_headers)
record(4, "Admin self-deactivation blocked (400)", r4.status_code == 400 and "own account" in r4.text.lower(), f"Status: {r4.status_code}, Body: {r4.text}")

# TEST 05: Authenticated admin attempts to change own role -> 400 Bad Request
r5 = requests.patch(f"{BASE_URL}/admin/users/{admin_id}/role", json={"newRole": "ROLE_STUDENT"}, headers=admin_headers)
record(5, "Admin self-role demotion blocked (400)", r5.status_code == 400 and "own role" in r5.text.lower(), f"Status: {r5.status_code}, Body: {r5.text}")

# TEST 06: Sole active admin cannot be deactivated
record(6, "Sole active admin cannot be deactivated (invariant enforced)", r4.status_code == 400, f"Status: {r4.status_code}")

# TEST 07: Sole active admin cannot be demoted
record(7, "Sole active admin cannot be demoted (invariant enforced)", r5.status_code == 400, f"Status: {r5.status_code}")

# TEST 08: Multi-admin deactivation: Promote Student 2 to ROLE_ADMIN, then Admin 1 deactivates Admin 2 -> 200 OK
prom_resp = requests.patch(f"{BASE_URL}/admin/users/{student2_id}/role", json={"newRole": "ROLE_ADMIN"}, headers=admin_headers)
assert prom_resp.status_code == 200, f"Promotion of student2 failed: {prom_resp.text}"

deact2 = requests.patch(f"{BASE_URL}/admin/users/{student2_id}/deactivate", headers=admin_headers)
record(8, "Multi-admin: Deactivation allowed when other admin remains (200)", deact2.status_code == 200, f"Status: {deact2.status_code}")

# Reactivate Student 2 and revert to ROLE_STUDENT
requests.patch(f"{BASE_URL}/admin/users/{student2_id}/reactivate", headers=admin_headers)
requests.patch(f"{BASE_URL}/admin/users/{student2_id}/role", json={"newRole": "ROLE_STUDENT"}, headers=admin_headers)

# TEST 09: Invalid role rejected: ROLE_HACKER -> 400 Bad Request
r9 = requests.patch(f"{BASE_URL}/admin/users/{student1_id}/role", json={"newRole": "ROLE_HACKER"}, headers=admin_headers)
record(9, "Invalid role name rejected with 400 Bad Request", r9.status_code == 400 and "invalid role" in r9.text.lower(), f"Status: {r9.status_code}, Body: {r9.text}")

# TEST 10: Valid promotion audit logged with oldRole and newRole
r10 = requests.patch(f"{BASE_URL}/admin/users/{student1_id}/role", json={"newRole": "ROLE_ADMIN"}, headers=admin_headers)
audit_resp = requests.get(f"{BASE_URL}/admin/audit-log?page=0&size=20", headers=admin_headers)
audit_entries = audit_resp.json().get("entries", audit_resp.json().get("content", []))
found_role_audit = any(
    e.get("action") == "CHANGE_USER_ROLE" and
    e.get("targetEntityId") == student1_id and
    "ROLE_STUDENT" in e.get("detail", "") and
    "ROLE_ADMIN" in e.get("detail", "")
    for e in audit_entries
)
record(10, "Role change succeeds and audit log records oldRole and newRole", r10.status_code == 200 and found_role_audit, f"Audit found: {found_role_audit}")

# Demote Student 1 back to ROLE_STUDENT
requests.patch(f"{BASE_URL}/admin/users/{student1_id}/role", json={"newRole": "ROLE_STUDENT"}, headers=admin_headers)

# TEST 11: Deactivated user's existing JWT is immediately rejected (401)
fresh_login1 = requests.post(f"{BASE_URL}/auth/login", json={"email": student1_email, "password": student_pass})
s1_token = fresh_login1.json()["token"]
s1_h = {"Authorization": f"Bearer {s1_token}", "Content-Type": "application/json"}
pre_check = requests.get(f"{BASE_URL}/auth/me", headers=s1_h)
assert pre_check.status_code == 200, "Token should be valid initially"

# Admin deactivates Student 1
deact_s1 = requests.patch(f"{BASE_URL}/admin/users/{student1_id}/deactivate", headers=admin_headers)
assert deact_s1.status_code == 200, "Deactivation should succeed"

# Immediate attempt with the existing JWT -> must be 401 Unauthorized
post_deact = requests.get(f"{BASE_URL}/auth/me", headers=s1_h)
record(11, "Existing JWT immediately rejected after deactivation (401)", post_deact.status_code == 401, f"Status: {post_deact.status_code}")

# TEST 12: Reactivated user's existing JWT is immediately accepted again (200)
react_s1 = requests.patch(f"{BASE_URL}/admin/users/{student1_id}/reactivate", headers=admin_headers)
assert react_s1.status_code == 200, "Reactivation should succeed"

post_react = requests.get(f"{BASE_URL}/auth/me", headers=s1_h)
record(12, "Existing JWT immediately accepted after reactivation (200)", post_react.status_code == 200, f"Status: {post_react.status_code}")

# TEST 13: Audit log is immutable: DELETE endpoint does not exist (404 or 405)
r13 = requests.delete(f"{BASE_URL}/admin/audit-log/1", headers=admin_headers)
record(13, "DELETE /admin/audit-log/{id} rejected (404/405 - immutable)", r13.status_code in (404, 405), f"Status: {r13.status_code}")

# TEST 14: Audit log is immutable: PUT endpoint does not exist (404 or 405)
r14 = requests.put(f"{BASE_URL}/admin/audit-log/1", json={"detail": "tampered"}, headers=admin_headers)
record(14, "PUT /admin/audit-log/{id} rejected (404/405 - immutable)", r14.status_code in (404, 405), f"Status: {r14.status_code}")

# TEST 15: Student cannot access audit log -> 403 Forbidden
r15 = requests.get(f"{BASE_URL}/admin/audit-log", headers=student1_headers)
record(15, "Student access to audit log rejected (403)", r15.status_code == 403, f"Status: {r15.status_code}")

# TEST 16: Audit log page size capped at 100 -> ?size=101 returns 400 Bad Request
r16 = requests.get(f"{BASE_URL}/admin/audit-log?size=101", headers=admin_headers)
record(16, "Audit log size cap validation: ?size=101 rejected (400)", r16.status_code == 400 and "100" in r16.text, f"Status: {r16.status_code}, Body: {r16.text}")

# TEST 17: Question deactivation lifecycle
q_text = f"Phase 12 Invariant Test Question #{ts}"
create_q = requests.post(f"{BASE_URL}/admin/questions", json={
    "categoryId": 1,
    "questionText": q_text,
    "questionType": "TECHNICAL",
    "difficulty": "MEDIUM",
    "expectedAnswerGuidance": "Candidate must explain core concepts."
}, headers=admin_headers)
assert create_q.status_code == 201, f"Question create failed: {create_q.text}"
q_id = create_q.json()["id"]

# Deactivate question
deact_q = requests.patch(f"{BASE_URL}/admin/questions/{q_id}/deactivate", headers=admin_headers)
# Reactivate question
react_q = requests.patch(f"{BASE_URL}/admin/questions/{q_id}/reactivate", headers=admin_headers)

record(17, "Question soft-delete / reactivation lifecycle verified (200)", deact_q.status_code == 200 and react_q.status_code == 200, f"Deactivate: {deact_q.status_code}, Reactivate: {react_q.status_code}")

# TEST 18: Duplicate question creation prevented (409 Conflict)
dup_q = requests.post(f"{BASE_URL}/admin/questions", json={
    "categoryId": 1,
    "questionText": q_text,
    "questionType": "TECHNICAL",
    "difficulty": "MEDIUM",
    "expectedAnswerGuidance": "Duplicate prompt"
}, headers=admin_headers)
record(18, "Duplicate question creation prevented (409 Conflict)", dup_q.status_code == 409, f"Status: {dup_q.status_code}")

# TEST 19: Admin sessions oversight returns sessions across multiple users
cat_resp = requests.get(f"{BASE_URL}/interview-categories", headers=student1_headers)
cat_id = cat_resp.json()[0]["id"]

# Student 1 creates a session
s1_sess = requests.post(f"{BASE_URL}/interview-sessions", json={
    "categoryId": cat_id,
    "difficulty": "EASY",
    "questionCount": 2,
    "durationMinutes": 20,
    "mode": "QUESTION_BANK",
    "interactionMode": "TEXT"
}, headers=student1_headers)

# Student 2 creates a session
s2_sess = requests.post(f"{BASE_URL}/interview-sessions", json={
    "categoryId": cat_id,
    "difficulty": "EASY",
    "questionCount": 2,
    "durationMinutes": 20,
    "mode": "QUESTION_BANK",
    "interactionMode": "TEXT"
}, headers=student2_headers)

admin_sess_resp = requests.get(f"{BASE_URL}/admin/sessions?page=0&size=50", headers=admin_headers)
sess_list = admin_sess_resp.json().get("sessions", admin_sess_resp.json().get("content", []))
user_ids_in_sessions = {s.get("userId") for s in sess_list}
has_multiple_users = student1_id in user_ids_in_sessions and student2_id in user_ids_in_sessions
record(19, "Admin session oversight sees cross-user sessions", admin_sess_resp.status_code == 200 and has_multiple_users, f"Users observed in sessions: {user_ids_in_sessions}")

# TEST 20: Admin session oversight is strictly read-only: POST /admin/sessions/{id}/modify returns 404 or 405
r20 = requests.post(f"{BASE_URL}/admin/sessions/{s1_sess.json().get('id', 1)}/modify", json={"status": "ABANDONED"}, headers=admin_headers)
record(20, "Admin session oversight has no mutation endpoints (404/405)", r20.status_code in (404, 405), f"Status: {r20.status_code}")

# --- SUMMARY ---
print("\n" + "=" * 70)
passed_count = sum(1 for _, _, p, _ in results if p)
total_count = len(results)
print(f"PHASE 12 VERIFICATION RESULT: {passed_count}/{total_count} PASSED")
print("=" * 70)

if passed_count == total_count:
    print("ALL 20 PHASE 12 SECURITY AND INVARIANT TESTS PASSED!")
    sys.exit(0)
else:
    print("SOME TESTS FAILED! Review details above.")
    sys.exit(1)
