import requests
import json
import sys

sys.stdout.reconfigure(encoding='utf-8')

BASE_URL = "http://localhost:8080/api/v1"

email = "project_user@example.com"
password = "Password@123"

print("--- Step 1: Register or Login ---")
try:
    reg = requests.post(f"{BASE_URL}/auth/register", json={
        "fullName": "Project Candidate",
        "email": email,
        "password": password
    })
    print("Register status:", reg.status_code)
except Exception as e:
    print("Register exception:", e)

login = requests.post(f"{BASE_URL}/auth/login", json={
    "email": email,
    "password": password
})
print("Login status:", login.status_code)
tokens = login.json()
token = tokens.get("token")
print("Access token received:", bool(token))

headers = {
    "Authorization": f"Bearer {token}",
    "Content-Type": "application/json"
}

# Fetch categories to get valid category ID
cat_res = requests.get(f"{BASE_URL}/interview-categories", headers=headers)
categories = cat_res.json()
category_id = categories[0]["id"]
print(f"Using category ID: {category_id} ({categories[0]['name']})")

# 2. Check or Create Project
print("\n--- Step 2: Create Project ---")
proj_data = {
    "title": "Distributed LRU Cache",
    "description": "In-memory caching system with eviction policies and distributed synchronization",
    "technologies": "Java, Spring Boot, Redis",
    "roleContribution": "Lead Backend Developer responsible for cache eviction algorithms and redis pub/sub invalidation",
    "keyFeatures": "O(1) lookup and eviction, TTL expiration, distributed cache invalidation",
    "architectureDetails": "Master-replica Redis structure with client-side near-cache in Java ConcurrentHashMap",
    "difficulty": "MEDIUM"
}

proj_res = requests.post(f"{BASE_URL}/projects", headers=headers, json=proj_data)
print("Create Project status:", proj_res.status_code)
project = proj_res.json()
project_id = project.get("id")
print(f"Project created with ID: {project_id}, Title: {project.get('title')}")

# 3. Start Project Mock Interview
print("\n--- Step 3: Create & Start Project Mock Interview ---")
session_data = {
    "categoryId": category_id,
    "projectId": project_id,
    "mode": "PROJECT",
    "difficulty": "MEDIUM",
    "questionCount": 3,
    "durationMinutes": 15,
    "interactionMode": "TEXT"
}
session_res = requests.post(f"{BASE_URL}/interview-sessions", headers=headers, json=session_data)
print("Create Session status:", session_res.status_code)
session = session_res.json()
session_id = session.get("id")
mode = session.get("mode")
print(f"Session created: ID={session_id}, Mode={mode}, Title={session.get('projectTitleSnapshot')}")

start_res = requests.post(f"{BASE_URL}/interview-sessions/{session_id}/start", headers=headers)
print("Start Session status:", start_res.status_code)

# 4. Fetch initial question
print("\n--- Step 4: Fetch First Question ---")
curr_q = requests.get(f"{BASE_URL}/interview-sessions/{session_id}/current-question", headers=headers)
print("Current Q status:", curr_q.status_code)
q1_data = curr_q.json()
print("Q1:", q1_data.get("questionText"))
iq_id = q1_data.get("id")
print("First InterviewQuestion ID:", iq_id)

import time

first_q_id = iq_id

def submit_answer_with_retry(session_id, q_id, answer_text, token):
    sub_res = None
    for attempt in range(4):
        sub_res = requests.post(
            f"{BASE_URL}/interview-sessions/{session_id}/questions/{q_id}/answer",
            headers=headers,
            json={
                "answerText": answer_text
            }
        )
        if sub_res.status_code == 200:
            break
        elif sub_res.status_code == 503:
            print(f"Groq TPM rate limit hit (503). Waiting 8s before retry (attempt {attempt+1}/4)...")
            time.sleep(8)
        else:
            break
    return sub_res

def print_eval(sub_res):
    print(f"Answer submit status:", sub_res.status_code)
    eval_data = sub_res.json()
    eval_obj = eval_data.get("evaluation", {})
    print(f"Score: {eval_obj.get('score')}, Correctness: {eval_obj.get('correctness')}, Relevance: {eval_obj.get('relevance')}, Clarity: {eval_obj.get('clarity')}, TechAccuracy: {eval_obj.get('technicalAccuracy')}")
    print(f"Feedback: {eval_obj.get('feedback', '')[:100]}...")

# Submit Answer 1
print(f"\n--- Submitting Answer 1 for Question ID {first_q_id} ---")
ans1_resp = submit_answer_with_retry(
    session_id,
    first_q_id,
    "In the near-cache, I combined a ConcurrentHashMap with a custom doubly linked list to achieve O(1) access and eviction. When a get request hits the cache, the node is moved to the head of the list under a fine-grained ReentrantLock. For evictions, the tail node is removed. When an eviction occurs or an update is received from Redis pub/sub, the local entry is invalidated atomically.",
    token
)
print_eval(ans1_resp)

print("Waiting 16s for Groq TPM quota...")
time.sleep(16)

# Next question
q2_res = requests.get(f"{BASE_URL}/interview-sessions/{session_id}/current-question", headers=headers)
q2_data = q2_res.json()
q2_id = q2_data.get("id")
print(f"Q2 (ID {q2_id}): {q2_data.get('questionText')}")

# Submit Answer 2
print(f"\n--- Submitting Answer 2 for Question ID {q2_id} ---")
ans2_resp = submit_answer_with_retry(
    session_id,
    q2_id,
    "To ensure thread-safety, we utilized ConcurrentHashMap for segment-level concurrency and protected the doubly linked list pointers using Striped Locks or StampedLock for optimistic reads. For Redis pub/sub invalidation, each microservice instance subscribes to a 'cache-invalidation' topic; upon message receipt, it evicts the specified key from its local near-cache using computeIfPresent.",
    token
)
print_eval(ans2_resp)

print("Waiting 16s for Groq TPM quota...")
time.sleep(16)

# Next question
q3_res = requests.get(f"{BASE_URL}/interview-sessions/{session_id}/current-question", headers=headers)
q3_data = q3_res.json()
q3_id = q3_data.get("id")
print(f"Q3 (ID {q3_id}): {q3_data.get('questionText')}")

# Submit Answer 3
print(f"\n--- Submitting Answer 3 for Question ID {q3_id} ---")
ans3_resp = submit_answer_with_retry(
    session_id,
    q3_id,
    "We followed a cache-aside with write-through invalidation strategy. When a write occurs, the master Redis instance is updated first, which publishes an invalidation event. To prevent stale reads between Redis master replication to replicas, reads are directed to the primary or we use Redis WAIT for synchronization. Client near-cache has a short TTL of 5 seconds as a safety net.",
    token
)
print_eval(ans3_resp)

# 6. Complete interview by fetching current question after all answered
print("\n--- Step 6: Complete Interview ---")
comp_res = requests.get(f"{BASE_URL}/interview-sessions/{session_id}/current-question", headers=headers)
print("Complete status:", comp_res.status_code)
print("Progress:", comp_res.json().get("progress"))

# 7. Get Report
print("\n--- Step 7: Get Report from Backend ---")
rep_res = requests.get(f"{BASE_URL}/reports/sessions/{session_id}", headers=headers)
print("Report status:", rep_res.status_code)
report = rep_res.json()
print("Report Overall Score:", report.get("overallScore"))
print("Report Mode:", report.get("mode"))
print("Report Project Title:", report.get("projectTitle"))
print("Report Project ID:", report.get("projectId"))
print("Report Performance Level:", report.get("performanceLevel"))
print("Report Correctness Score:", report.get("correctnessScore"))
print("Report Relevance Score:", report.get("relevanceScore"))
print("Report Clarity Score:", report.get("clarityScore"))
print("Report Technical Accuracy Score:", report.get("technicalAccuracyScore"))
print("Question count in report:", len(report.get("questions", [])))
print("Key Strengths:", report.get("keyStrengths"))
print("Key Weaknesses:", report.get("keyWeaknesses"))

# 8. Refresh verification - second HTTP request
print("\n--- Step 8: Second HTTP Request (Simulate Refresh) ---")
rep_res2 = requests.get(f"{BASE_URL}/reports/sessions/{session_id}", headers=headers)
report2 = rep_res2.json()
print("Refreshed Report Overall Score:", report2.get("overallScore"))
assert report.get("overallScore") == report2.get("overallScore"), "Score did not persist across requests!"
print(">>> SUCCESS: SCORE PERSISTS EXACTLY ACROSS SEPARATE REQUESTS! <<<")
