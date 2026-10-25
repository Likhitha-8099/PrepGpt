import requests
import json
import time

BASE_URL = "http://localhost:8080/api/v1"

def run_phase4_tests():
    print("=== Phase 4 Interview Engine Backend Test Suite ===")

    # Wait for backend readiness
    for _ in range(30):
        try:
            r = requests.get(f"{BASE_URL}/health")
            if r.status_code == 200:
                print("Backend health check PASSED")
                break
        except Exception:
            pass
        time.sleep(1)
    else:
        print("Backend failed to start within timeout.")
        return

    # 1. Register and Login Student A
    student_a_email = f"student_a_{int(time.time())}@example.com"
    r_reg_a = requests.post(f"{BASE_URL}/auth/register", json={
        "fullName": "Student Alpha",
        "email": student_a_email,
        "password": "Password123!"
    })
    assert r_reg_a.status_code == 201, f"Student A registration failed: {r_reg_a.text}"
    r_log_a = requests.post(f"{BASE_URL}/auth/login", json={
        "email": student_a_email,
        "password": "Password123!"
    })
    assert r_log_a.status_code == 200, f"Student A login failed: {r_log_a.text}"
    token_a = r_log_a.json()["token"]
    headers_a = {"Authorization": f"Bearer {token_a}"}
    print("Student A registered and logged in successfully.")

    # 2. Register and Login Student B
    student_b_email = f"student_b_{int(time.time())}@example.com"
    r_reg_b = requests.post(f"{BASE_URL}/auth/register", json={
        "fullName": "Student Beta",
        "email": student_b_email,
        "password": "Password123!"
    })
    assert r_reg_b.status_code == 201, f"Student B registration failed: {r_reg_b.text}"
    r_log_b = requests.post(f"{BASE_URL}/auth/login", json={
        "email": student_b_email,
        "password": "Password123!"
    })
    assert r_log_b.status_code == 200, f"Student B login failed: {r_log_b.text}"
    token_b = r_log_b.json()["token"]
    headers_b = {"Authorization": f"Bearer {token_b}"}
    print("Student B registered and logged in successfully.")

    # 3. Fetch categories
    r_cats = requests.get(f"{BASE_URL}/interview-categories", headers=headers_a)
    assert r_cats.status_code == 200
    categories = r_cats.json()
    tech_cat = next(c for c in categories if c["name"] == "Technical Interview")
    tech_cat_id = tech_cat["id"]

    # 4. Student A configures session (5 questions, MEDIUM)
    r_create = requests.post(f"{BASE_URL}/interview-sessions", headers=headers_a, json={
        "categoryId": tech_cat_id,
        "difficulty": "MEDIUM",
        "questionCount": 5,
        "durationMinutes": 15
    })
    assert r_create.status_code == 201, f"Create session failed: {r_create.text}"
    session = r_create.json()
    session_id = session["id"]
    assert session["status"] == "CONFIGURED"
    print(f"Student A created CONFIGURED session #{session_id}.")

    # 5. Security Check: Student B cannot access Student A's session details
    r_sec = requests.get(f"{BASE_URL}/interview-sessions/{session_id}", headers=headers_b)
    assert r_sec.status_code in (403, 404), f"Security breach: Student B accessed Student A session! Status={r_sec.status_code}"
    print("Security Check PASSED: Student B cannot access Student A's session.")

    # 6. Start session for Student A
    r_start = requests.post(f"{BASE_URL}/interview-sessions/{session_id}/start", headers=headers_a)
    assert r_start.status_code == 200, f"Start session failed: {r_start.text}"
    session_started = r_start.json()
    assert session_started["status"] == "IN_PROGRESS"
    assert session_started["startedAt"] is not None
    print(f"Session #{session_id} started successfully (IN_PROGRESS).")

    # 7. Idempotency test: Starting an already IN_PROGRESS session returns existing session without error
    r_start_again = requests.post(f"{BASE_URL}/interview-sessions/{session_id}/start", headers=headers_a)
    assert r_start_again.status_code == 200
    assert r_start_again.json()["status"] == "IN_PROGRESS"
    print("Idempotency Check PASSED: Re-starting session returns existing IN_PROGRESS state.")

    # 8. Get current question
    r_curr = requests.get(f"{BASE_URL}/interview-sessions/{session_id}/current-question", headers=headers_a)
    assert r_curr.status_code == 200, f"Get current question failed: {r_curr.text}"
    q1 = r_curr.json()
    assert q1["questionOrder"] == 1
    assert q1["totalQuestions"] == 5
    assert q1["answered"] == False
    q1_id = q1["id"]
    print(f"Question 1 loaded: '{q1['questionText'][:40]}...' (ID: {q1_id})")

    # 9. Empty answer validation
    r_empty = requests.post(f"{BASE_URL}/interview-sessions/{session_id}/questions/{q1_id}/answer", headers=headers_a, json={"answerText": "   "})
    assert r_empty.status_code == 400, f"Empty answer should be rejected! Status={r_empty.status_code}"
    print("Validation PASSED: Empty answer rejected.")

    # 10. Submit valid answer for Q1
    r_ans1 = requests.post(f"{BASE_URL}/interview-sessions/{session_id}/questions/{q1_id}/answer", headers=headers_a, json={
        "answerText": "Object-Oriented Programming (OOP) relies on inheritance, encapsulation, polymorphism, and abstraction."
    })
    assert r_ans1.status_code == 200, f"Submit answer failed: {r_ans1.text}"
    ans1_resp = r_ans1.json()
    assert ans1_resp["success"] == True
    assert ans1_resp["answered"] == True
    assert ans1_resp["completed"] == False
    print("Question 1 answer submitted successfully.")

    # 11. Prevent duplicate answer submission for Q1
    r_dup = requests.post(f"{BASE_URL}/interview-sessions/{session_id}/questions/{q1_id}/answer", headers=headers_a, json={"answerText": "Duplicate submission"})
    assert r_dup.status_code == 400, f"Duplicate answer should be rejected! Status={r_dup.status_code}"
    print("Duplicate Check PASSED: Second submission for Q1 rejected.")

    # 12. Check status after Q1
    r_stat1 = requests.get(f"{BASE_URL}/interview-sessions/{session_id}/status", headers=headers_a)
    assert r_stat1.status_code == 200
    stat1 = r_stat1.json()
    assert stat1["answeredQuestions"] == 1
    assert stat1["progress"] == 20
    print(f"Session status after Q1: Progress = {stat1['progress']}%, Answered = {stat1['answeredQuestions']}/5")

    # 13. Answer questions 2 through 5 sequentially
    for expected_order in range(2, 6):
        r_next = requests.get(f"{BASE_URL}/interview-sessions/{session_id}/next-question", headers=headers_a)
        assert r_next.status_code == 200
        curr_q = r_next.json()
        assert curr_q["questionOrder"] == expected_order, f"Expected order {expected_order}, got {curr_q['questionOrder']}"
        q_id = curr_q["id"]

        r_sub = requests.post(f"{BASE_URL}/interview-sessions/{session_id}/questions/{q_id}/answer", headers=headers_a, json={
            "answerText": f"This is candidate Student A's answer for interview question #{expected_order}."
        })
        assert r_sub.status_code == 200
        if expected_order == 5:
            assert r_sub.json()["completed"] == True
            print("Final Question (Q5) answered. Session completed!")

    # 14. Verify session transitioned to COMPLETED
    r_final_stat = requests.get(f"{BASE_URL}/interview-sessions/{session_id}/status", headers=headers_a)
    assert r_final_stat.status_code == 200
    final_stat = r_final_stat.json()
    assert final_stat["status"] == "COMPLETED"
    assert final_stat["progress"] == 100
    assert final_stat["answeredQuestions"] == 5
    print("Completion Check PASSED: Status = COMPLETED, Progress = 100%.")

    # 15. Verify Session Detail returns full question & answer transcript
    r_details = requests.get(f"{BASE_URL}/interview-sessions/{session_id}", headers=headers_a)
    assert r_details.status_code == 200
    det = r_details.json()
    assert det["status"] == "COMPLETED"
    assert len(det["questions"]) == 5
    for q_item in det["questions"]:
        assert q_item["answerText"] is not None and len(q_item["answerText"]) > 0
        assert q_item["answeredAt"] is not None
    print(f"Session Details transcript verify PASSED: {len(det['questions'])} answers persisted.")

    print("\nALL PHASE 4 BACKEND E2E TESTS PASSED PERFECTLY!")

if __name__ == "__main__":
    run_phase4_tests()
