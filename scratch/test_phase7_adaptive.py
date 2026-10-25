import requests
import json
import time
import sys

sys.stdout.reconfigure(encoding='utf-8')

BASE_URL = "http://localhost:8080/api/v1"

def run_test():
    print("=" * 60)
    print("Starting Phase 7 Adaptive Interview & Evaluation E2E Verification")
    print("=" * 60)

    # 1. Register & Login fresh student
    email = f"adaptive_user_{int(time.time())}@example.com"
    password = "Password123!"
    print(f"\n1. Registering new test candidate ({email})...")

    reg_resp = requests.post(f"{BASE_URL}/auth/register", json={
        "fullName": "Adaptive Candidate",
        "email": email,
        "password": password
    })
    if reg_resp.status_code != 201:
        print(f"FAILED: Registration failed with status {reg_resp.status_code}: {reg_resp.text}")
        sys.exit(1)
    print("  [SUCCESS] Registered successfully.")

    login_resp = requests.post(f"{BASE_URL}/auth/login", json={
        "email": email,
        "password": password
    })
    if login_resp.status_code != 200:
        print(f"FAILED: Login failed: {login_resp.status_code} {login_resp.text}")
        sys.exit(1)

    token = login_resp.json().get("token")
    headers = {"Authorization": f"Bearer {token}", "Content-Type": "application/json"}
    print("  [SUCCESS] Authenticated successfully.")

    # 2. Get category ID
    print("\n2. Fetching categories...")
    cat_resp = requests.get(f"{BASE_URL}/interview-categories", headers=headers)
    categories = cat_resp.json()
    cat_id = categories[0]["id"]
    cat_name = categories[0]["name"]
    print(f"  [SUCCESS] Selected category: {cat_name} (ID: {cat_id})")

    # 3. Create AI Interview Session (3 Questions, MEDIUM)
    print("\n3. Creating AI Interview Session (3 Questions, MEDIUM)...")
    create_resp = requests.post(f"{BASE_URL}/interview-sessions", headers=headers, json={
        "categoryId": cat_id,
        "difficulty": "MEDIUM",
        "mode": "AI",
        "interactionMode": "TEXT",
        "questionCount": 3,
        "durationMinutes": 30
    })
    session_id = create_resp.json()["id"]
    print(f"  [SUCCESS] Session created: ID {session_id}")

    # 4. Start Session
    start_resp = requests.post(f"{BASE_URL}/interview-sessions/{session_id}/start", headers=headers)
    print(f"  [SUCCESS] Session started: Status {start_resp.json()['status']}")

    assigned_question_texts = []

    # 5. Q1: Get Question & Submit Strong Answer
    print("\n4. Fetching Question 1...")
    q1_resp = requests.get(f"{BASE_URL}/interview-sessions/{session_id}/current-question", headers=headers)
    q1 = q1_resp.json()
    print(f"  Q1 Text: {q1['questionText']}")
    assigned_question_texts.append(q1['questionText'])

    print("  Submitting Strong Answer to Q1...")
    q1_answer = (
        "Java memory management relies on Heap (Young and Old Generation) and Stack spaces. "
        "Garbage collection reclaims unreachable object references using Mark-Sweep-Compact and "
        "G1 region-based collectors to optimize throughput and pause times."
    )
    ans1_resp = requests.post(
        f"{BASE_URL}/interview-sessions/{session_id}/questions/{q1['id']}/answer",
        headers=headers,
        json={"answerText": q1_answer, "inputMethod": "TEXT"}
    )
    eval1 = ans1_resp.json()["evaluation"]
    print(f"  [SUCCESS] Q1 Score: {eval1['score']}/10")
    print(f"  [SUCCESS] Adaptive Decision: {eval1.get('adaptiveDecision')}")
    print(f"  [SUCCESS] User Facing Message: {eval1.get('userFacingMessage')}")
    print(f"  [SUCCESS] Next Difficulty: {eval1.get('nextDifficulty')}")

    # 6. Q2: Get Question 2 & Submit Partial/Weak Answer
    print("\n5. Fetching Question 2...")
    q2_resp = requests.get(f"{BASE_URL}/interview-sessions/{session_id}/current-question", headers=headers)
    q2 = q2_resp.json()
    print(f"  Q2 Text: {q2['questionText']}")
    print(f"  Q2 Difficulty: {q2['difficulty']}")
    assigned_question_texts.append(q2['questionText'])

    print("  Submitting Partial Answer to Q2...")
    q2_answer = "Java handles threads using the Thread class and Runnable interface."
    ans2_resp = requests.post(
        f"{BASE_URL}/interview-sessions/{session_id}/questions/{q2['id']}/answer",
        headers=headers,
        json={"answerText": q2_answer, "inputMethod": "TEXT"}
    )
    eval2 = ans2_resp.json()["evaluation"]
    print(f"  [SUCCESS] Q2 Score: {eval2['score']}/10")
    print(f"  [SUCCESS] Adaptive Decision: {eval2.get('adaptiveDecision')}")
    print(f"  [SUCCESS] User Facing Message: {eval2.get('userFacingMessage')}")
    print(f"  [SUCCESS] Target Concept: {eval2.get('targetConcept')}")

    # 7. Q3: Get Question 3 (Final Question) & Submit Answer
    print("\n6. Fetching Question 3 (Final Question)...")
    q3_resp = requests.get(f"{BASE_URL}/interview-sessions/{session_id}/current-question", headers=headers)
    q3 = q3_resp.json()
    print(f"  Q3 Text: {q3['questionText']}")
    assigned_question_texts.append(q3['questionText'])

    print("  Submitting Answer to Q3 (Final Question)...")
    q3_answer = "Multithreading synchronized blocks use intrinsic object locks to prevent data races."
    ans3_resp = requests.post(
        f"{BASE_URL}/interview-sessions/{session_id}/questions/{q3['id']}/answer",
        headers=headers,
        json={"answerText": q3_answer, "inputMethod": "TEXT"}
    )
    res3 = ans3_resp.json()
    eval3 = res3["evaluation"]
    print(f"  [SUCCESS] Q3 Completed Flag: {res3['completed']}")
    print(f"  [SUCCESS] Final Adaptive Decision: {eval3.get('adaptiveDecision')}")
    assert eval3.get('adaptiveDecision') == 'COMPLETE', f"Expected COMPLETE but got {eval3.get('adaptiveDecision')}"

    # 8. Verify Session Completion & No Duplicate Questions
    print("\n7. Verifying Session Transcript Details...")
    session_details_resp = requests.get(f"{BASE_URL}/interview-sessions/{session_id}", headers=headers)
    details = session_details_resp.json()
    print(f"  [SUCCESS] Session Status: {details['status']}")
    assert details['status'] == 'COMPLETED', f"Expected COMPLETED status but got {details['status']}"

    unique_questions = set(assigned_question_texts)
    print(f"  [SUCCESS] Total Questions Assigned: {len(assigned_question_texts)}")
    print(f"  [SUCCESS] Unique Questions: {len(unique_questions)}")
    assert len(assigned_question_texts) == len(unique_questions), "Duplicate question detected!"

    # 9. Verify Question Bank mode remains unaffected
    print("\n8. Verifying Question Bank Mode is unaffected...")
    qb_create_resp = requests.post(f"{BASE_URL}/interview-sessions", headers=headers, json={
        "categoryId": cat_id,
        "difficulty": "EASY",
        "mode": "QUESTION_BANK",
        "interactionMode": "TEXT",
        "questionCount": 3,
        "durationMinutes": 30
    })
    qb_session_id = qb_create_resp.json()["id"]
    print(f"  [SUCCESS] Created Question Bank session ID: {qb_session_id}")

    start_qb = requests.post(f"{BASE_URL}/interview-sessions/{qb_session_id}/start", headers=headers)
    assert start_qb.status_code == 200, f"Failed to start QB session: {start_qb.text}"
    print("  [SUCCESS] Started Question Bank session successfully.")

    print("\n" + "=" * 60)
    print("ALL PHASE 7 ADAPTIVE E2E VERIFICATIONS PASSED SUCCESSFULLY!")
    print("=" * 60)

if __name__ == "__main__":
    run_test()
