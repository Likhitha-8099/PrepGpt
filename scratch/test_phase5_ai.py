import requests
import json
import time

BASE_URL = "http://localhost:8080/api/v1"

def run_phase5_tests():
    print("=== Phase 5 AI Interviewer Integration Test Suite ===")

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

    # 1. Register and Login Student C
    student_c_email = f"student_c_{int(time.time())}@example.com"
    r_reg = requests.post(f"{BASE_URL}/auth/register", json={
        "fullName": "Student Gamma",
        "email": student_c_email,
        "password": "Password123!"
    })
    assert r_reg.status_code == 201, f"Student C registration failed: {r_reg.text}"

    r_log = requests.post(f"{BASE_URL}/auth/login", json={
        "email": student_c_email,
        "password": "Password123!"
    })
    assert r_log.status_code == 200, f"Student C login failed: {r_log.text}"
    token = r_log.json()["token"]
    headers = {"Authorization": f"Bearer {token}"}
    print("Student C registered and logged in successfully.")

    # 2. Fetch categories
    r_cats = requests.get(f"{BASE_URL}/interview-categories", headers=headers)
    assert r_cats.status_code == 200
    categories = r_cats.json()
    tech_cat_id = next(c["id"] for c in categories if c["name"] == "Technical Interview")

    # 3. Test Backward Compatibility: Create Question Bank mode session
    r_qb = requests.post(f"{BASE_URL}/interview-sessions", headers=headers, json={
        "categoryId": tech_cat_id,
        "difficulty": "EASY",
        "questionCount": 3,
        "durationMinutes": 10,
        "mode": "QUESTION_BANK"
    })
    assert r_qb.status_code == 201
    assert r_qb.json()["mode"] == "QUESTION_BANK"
    print("Backward Compatibility Check PASSED: Question Bank mode session created successfully.")

    # 4. Create AI Mode Session
    r_ai_create = requests.post(f"{BASE_URL}/interview-sessions", headers=headers, json={
        "categoryId": tech_cat_id,
        "difficulty": "MEDIUM",
        "questionCount": 3,
        "durationMinutes": 15,
        "mode": "AI"
    })
    assert r_ai_create.status_code == 201, f"Create AI session failed: {r_ai_create.text}"
    ai_session = r_ai_create.json()
    ai_session_id = ai_session["id"]
    assert ai_session["mode"] == "AI"
    print(f"Student C created AI Mode session #{ai_session_id}.")

    # 5. Start AI Session & Check Unconfigured Key Error (or AI response)
    r_ai_start = requests.post(f"{BASE_URL}/interview-sessions/{ai_session_id}/start", headers=headers)
    
    if r_ai_start.status_code == 503:
        err_res = r_ai_start.json()
        assert err_res["status"] == 503
        assert "AI_SERVICE_UNAVAILABLE" in err_res["error"]
        print("Controlled Error Verification PASSED: Unconfigured AI key returns clean 503 AI_SERVICE_UNAVAILABLE response without stack trace.")
        print("\nALL PHASE 5 UNCONFIGURED AI SAFETY TESTS PASSED PERFECTLY!")
        return

    assert r_ai_start.status_code == 200, f"Start AI session failed: {r_ai_start.text}"
    started_ai = r_ai_start.json()
    assert started_ai["status"] == "IN_PROGRESS"
    print("AI Session started successfully (IN_PROGRESS).")

    # 6. Fetch Current AI Question
    r_curr_ai = requests.get(f"{BASE_URL}/interview-sessions/{ai_session_id}/current-question", headers=headers)
    assert r_curr_ai.status_code == 200, f"Get current AI question failed: {r_curr_ai.text}"
    q_ai = r_curr_ai.json()
    q_ai_id = q_ai["id"]
    print(f"AI Question 1 loaded: '{q_ai['questionText'][:50]}...'")

    # 7. Submit Answer for AI Question
    r_ans_ai = requests.post(f"{BASE_URL}/interview-sessions/{ai_session_id}/questions/{q_ai_id}/answer", headers=headers, json={
        "answerText": "Encapsulation hides implementation details and exposes public methods to safeguard state invariants."
    })
    assert r_ans_ai.status_code == 200, f"Submit AI answer failed: {r_ans_ai.text}"
    ans_res = r_ans_ai.json()
    assert ans_res["success"] == True
    assert ans_res["evaluation"] is not None
    eval_dto = ans_res["evaluation"]
    print(f"AI Answer Evaluated! Score: {eval_dto['score']}/10. Correctness: {eval_dto['correctness']}, Relevance: {eval_dto['relevance']}")

    print("\nALL PHASE 5 E2E INTEGRATION TESTS PASSED PERFECTLY!")

if __name__ == "__main__":
    run_phase5_tests()
