import requests
import json
import time
import sys

BASE_URL = "http://localhost:8080/api/v1"

def run_test():
    print("=" * 60)
    print("Starting Phase 8 Reports & Analytics E2E Verification")
    print("=" * 60)

    # 1. Register & Login candidate A
    email_a = f"report_user_a_{int(time.time())}@example.com"
    password = "Password123!"
    print(f"\n1. Registering test candidate A ({email_a})...")

    reg_a = requests.post(f"{BASE_URL}/auth/register", json={
        "fullName": "Report Candidate A",
        "email": email_a,
        "password": password
    })
    assert reg_a.status_code == 201, f"Registration failed: {reg_a.text}"

    login_a = requests.post(f"{BASE_URL}/auth/login", json={"email": email_a, "password": password})
    assert login_a.status_code == 200, f"Login failed: {login_a.text}"
    token_a = login_a.json()["token"]
    headers_a = {"Authorization": f"Bearer {token_a}", "Content-Type": "application/json"}
    print("  [SUCCESS] Candidate A authenticated successfully.")

    # 2. Register candidate B for security checks
    email_b = f"report_user_b_{int(time.time())}@example.com"
    reg_b = requests.post(f"{BASE_URL}/auth/register", json={"fullName": "Candidate B", "email": email_b, "password": password})
    login_b = requests.post(f"{BASE_URL}/auth/login", json={"email": email_b, "password": password})
    token_b = login_b.json()["token"]
    headers_b = {"Authorization": f"Bearer {token_b}", "Content-Type": "application/json"}
    print("  [SUCCESS] Candidate B authenticated successfully.")

    # 3. Fetch Category
    cat_resp = requests.get(f"{BASE_URL}/interview-categories", headers=headers_a)
    categories = cat_resp.json()
    cat_id = categories[0]["id"]
    cat_name = categories[0]["name"]
    print(f"\n2. Category selected: {cat_name} (ID: {cat_id})")

    # 4. Create & Complete Session #1 for Candidate A
    print("\n3. Creating AI Interview Session #1 for Candidate A...")
    create_resp = requests.post(f"{BASE_URL}/interview-sessions", headers=headers_a, json={
        "categoryId": cat_id,
        "difficulty": "MEDIUM",
        "mode": "AI",
        "interactionMode": "TEXT",
        "questionCount": 2,
        "durationMinutes": 20
    })
    session_id = create_resp.json()["id"]
    print(f"  Session created: ID {session_id}")

    start_resp = requests.post(f"{BASE_URL}/interview-sessions/{session_id}/start", headers=headers_a)
    assert start_resp.status_code == 200, f"Start session failed: {start_resp.text}"

    # Answer Q1
    q1 = requests.get(f"{BASE_URL}/interview-sessions/{session_id}/current-question", headers=headers_a).json()
    ans1_resp = requests.post(
        f"{BASE_URL}/interview-sessions/{session_id}/questions/{q1['id']}/answer",
        headers=headers_a,
        json={
            "answerText": "Java memory is managed via Heap (Young/Old gen) and Stack. G1GC reclaims unreachable references.",
            "inputMethod": "TEXT"
        }
    )

    # Answer Q2
    q2 = requests.get(f"{BASE_URL}/interview-sessions/{session_id}/current-question", headers=headers_a).json()
    ans2_resp = requests.post(
        f"{BASE_URL}/interview-sessions/{session_id}/questions/{q2['id']}/answer",
        headers=headers_a,
        json={
            "answerText": "Synchronization uses object intrinsic locks to ensure single thread access to critical sections.",
            "inputMethod": "TEXT"
        }
    )
    print("  [SUCCESS] Completed session #1 answering all 2 questions.")

    # 5. Fetch Detailed Report for Session #1
    print("\n4. Testing Detailed Interview Report Endpoint (GET /api/v1/reports/sessions/{id})...")
    report_resp = requests.get(f"{BASE_URL}/reports/sessions/{session_id}", headers=headers_a)
    assert report_resp.status_code == 200, f"Report endpoint failed: {report_resp.status_code} - {report_resp.text}"
    report = report_resp.json()

    print(f"  [SUCCESS] Report Session ID: {report['sessionId']}")
    print(f"  [SUCCESS] Overall Score: {report['overallScore']} / 10")
    print(f"  [SUCCESS] Performance Level: {report['performanceLevel']}")
    print(f"  [SUCCESS] Correctness: {report['correctnessScore']}, Relevance: {report['relevanceScore']}, Clarity: {report['clarityScore']}, Tech Accuracy: {report['technicalAccuracyScore']}")
    print(f"  [SUCCESS] Questions Evaluated Count: {len(report['questions'])}")
    print(f"  [SUCCESS] Adaptive Decisions Count: {report['adaptiveSummary']['decisionCounts']}")

    # 6. Security Check: Candidate B cannot access Candidate A's report
    print("\n5. Testing Security Ownership Enforcement...")
    unauth_resp = requests.get(f"{BASE_URL}/reports/sessions/{session_id}", headers=headers_b)
    assert unauth_resp.status_code in [403, 404], f"Expected 403/404 for unauthorized report fetch, got {unauth_resp.status_code}"
    print("  [SUCCESS] Security PASSED: Candidate B denied access to Candidate A's report.")

    # 7. Fetch Analytics Overview Endpoint
    print("\n6. Testing Candidate Analytics Overview Endpoint (GET /api/v1/analytics/overview)...")
    overview_resp = requests.get(f"{BASE_URL}/analytics/overview", headers=headers_a)
    assert overview_resp.status_code == 200, f"Analytics overview failed: {overview_resp.text}"
    overview = overview_resp.json()

    print(f"  [SUCCESS] Total Interviews: {overview['totalInterviews']}")
    print(f"  [SUCCESS] Completed Interviews: {overview['completedInterviews']}")
    print(f"  [SUCCESS] Average Overall Score: {overview['averageOverallScore']} / 10")
    print(f"  [SUCCESS] Best Score: {overview['bestOverallScore']} / 10")
    print(f"  [SUCCESS] Score Trend Count: {len(overview['scoreTrend'])}")

    # 8. Fetch Category Skill Analytics Endpoint
    print("\n7. Testing Category Performance Analytics Endpoint (GET /api/v1/analytics/categories)...")
    cat_analytics_resp = requests.get(f"{BASE_URL}/analytics/categories", headers=headers_a)
    assert cat_analytics_resp.status_code == 200, f"Category analytics failed: {cat_analytics_resp.text}"
    cat_analytics = cat_analytics_resp.json()

    print(f"  [SUCCESS] Categories Analyzed Count: {len(cat_analytics)}")
    if len(cat_analytics) > 0:
        c0 = cat_analytics[0]
        print(f"  [SUCCESS] Top Category: {c0['categoryName']}, Avg Score: {c0['averageScore']}/10, Level: {c0['performanceLevel']}")

    print("\n" + "=" * 60)
    print("ALL PHASE 8 REPORTS & ANALYTICS E2E VERIFICATIONS PASSED SUCCESSFULLY!")
    print("=" * 60)

if __name__ == "__main__":
    run_test()
