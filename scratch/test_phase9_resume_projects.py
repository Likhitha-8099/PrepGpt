import requests
import time
import os
import sys

sys.stdout.reconfigure(encoding='utf-8')

BASE_URL = "http://localhost:8080/api/v1"
TIMESTAMP = int(time.time())

def main():
    print("=" * 60)
    print("Starting Phase 9 Resume & Project Analysis E2E Verification")
    print("=" * 60)

    # 1. Register candidate User A
    user_a_email = f"phase9_user_a_{TIMESTAMP}@example.com"
    reg_payload = {
        "fullName": "Alice Resume Candidate",
        "email": user_a_email,
        "password": "Password123!"
    }
    r = requests.post(f"{BASE_URL}/auth/register", json=reg_payload)
    if r.status_code != 201:
        print(f"[FAIL] User A registration failed: {r.status_code} {r.text}")
        sys.exit(1)
    
    r_login_a = requests.post(f"{BASE_URL}/auth/login", json={"email": user_a_email, "password": "Password123!"})
    token_a = r_login_a.json()["token"]
    headers_a = {"Authorization": f"Bearer {token_a}"}
    print("  [SUCCESS] Candidate User A registered and authenticated.")

    # Register User B for Security Isolation Testing
    user_b_email = f"phase9_user_b_{TIMESTAMP}@example.com"
    r_b = requests.post(f"{BASE_URL}/auth/register", json={"fullName": "Bob Candidate", "email": user_b_email, "password": "Password123!"})
    r_login_b = requests.post(f"{BASE_URL}/auth/login", json={"email": user_b_email, "password": "Password123!"})
    token_b = r_login_b.json()["token"]
    headers_b = {"Authorization": f"Bearer {token_b}"}

    # 2. Upload Candidate Resume Document (.txt / .pdf)
    sample_resume_content = """
    Alice Candidate
    Email: alice@example.com | Phone: +1 555-0199
    
    EXECUTIVE SUMMARY:
    Dedicated Backend Software Engineer with 2 years of experience building high-throughput microservices in Java, Spring Boot, React, and MySQL.
    
    TECHNICAL SKILLS:
    - Programming Languages: Java, Python, JavaScript, SQL
    - Frameworks & Libraries: Spring Boot, Spring Security, Hibernate, React.js
    - Databases & Storage: MySQL, Redis, PostgreSQL
    - Tools & DevOps: Docker, Maven, Git, REST APIs
    
    WORK EXPERIENCE:
    Software Engineering Intern | Tech Solutions Inc. | Jun 2023 - Dec 2023
    - Designed and implemented RESTful microservices for processing high-volume candidate evaluation data.
    - Optimized SQL queries in MySQL decreasing latency by 35%.
    
    EDUCATION:
    Bachelor of Technology in Computer Science | State University | 2020 - 2024
    
    PROJECTS:
    PrepGPT Mock Interview Engine
    - Developed an adaptive AI interview platform using Spring Boot, React, and Groq LLM API.
    - Implemented voice speech-to-text (STT) and text-to-speech (TTS) integration.
    
    CERTIFICATIONS:
    - AWS Certified Cloud Practitioner
    """

    files = {
        'file': ('alice_resume.txt', sample_resume_content, 'text/plain')
    }
    
    print("\n2. Uploading Candidate Resume Document...")
    r_up = requests.post(f"{BASE_URL}/resumes/upload", headers=headers_a, files=files)
    if r_up.status_code != 201:
        print(f"[FAIL] Resume upload failed: {r_up.status_code} {r_up.text}")
        sys.exit(1)
    
    resume_data = r_up.json()
    resume_id = resume_data["id"]
    print(f"  [SUCCESS] Resume uploaded: ID {resume_id}, File: {resume_data['fileName']}")
    print(f"  [SUCCESS] Primary Flag: {resume_data['isPrimary']}")
    print(f"  [SUCCESS] Extracted Skills JSON: {resume_data['skillsJson']}")

    # 3. Verify Candidate Resumes Listing
    print("\n3. Fetching User A Resumes...")
    r_resumes = requests.get(f"{BASE_URL}/resumes", headers=headers_a)
    if r_resumes.status_code != 200 or len(r_resumes.json()) != 1:
        print(f"[FAIL] User A resumes listing failed: {r_resumes.text}")
        sys.exit(1)
    print("  [SUCCESS] Resume list verified successfully.")

    # 4. Verify Auto-Extracted Candidate Projects
    print("\n4. Checking Auto-Extracted Projects from Resume...")
    r_proj = requests.get(f"{BASE_URL}/projects", headers=headers_a)
    if r_proj.status_code != 200:
        print(f"[FAIL] Projects listing failed: {r_proj.text}")
        sys.exit(1)
    
    user_a_projects = r_proj.json()
    print(f"  [SUCCESS] Total Projects Found: {len(user_a_projects)}")
    
    # 5. Create a Manual Software Project
    print("\n5. Creating Manual Candidate Project...")
    manual_proj_payload = {
        "title": "High Throughput Distributed LRU Cache",
        "description": "Custom thread-safe distributed cache with O(1) eviction and Redis persistence.",
        "technologies": "Java 17, Netty, Redis, ConcurrentHashMap, Docker",
        "roleContribution": "Lead Architect & Implementation Engineer",
        "keyFeatures": "O(1) get/put, Lock-free read operations, TTL expiration",
        "architectureDetails": "Doubly linked list combined with hashmap and asynchronous snapshotting.",
        "difficulty": "HARD"
    }
    r_create_p = requests.post(f"{BASE_URL}/projects", headers=headers_a, json=manual_proj_payload)
    if r_create_p.status_code != 201:
        print(f"[FAIL] Manual project creation failed: {r_create_p.status_code} {r_create_p.text}")
        sys.exit(1)
    
    project_data = r_create_p.json()
    project_id = project_data["id"]
    print(f"  [SUCCESS] Manual Project created: ID {project_id}, Title: '{project_data['title']}'")

    # 6. Trigger Groq AI Technical Project Analysis
    print("\n6. Triggering Groq AI Technical Project Analysis...")
    r_analyze_p = requests.post(f"{BASE_URL}/projects/{project_id}/analyze", headers=headers_a)
    if r_analyze_p.status_code != 200:
        print(f"[FAIL] AI Project analysis failed: {r_analyze_p.status_code} {r_analyze_p.text}")
        sys.exit(1)
    
    analyzed_project = r_analyze_p.json()
    print("  [SUCCESS] Groq AI Project Analysis completed.")
    print(f"  [SUCCESS] AI Analysis JSON present: {bool(analyzed_project['aiAnalysisJson'])}")

    # 7. Security Isolation Verification
    print("\n7. Testing Security & Data Isolation (User B accessing User A assets)...")
    r_sec_res = requests.get(f"{BASE_URL}/resumes/{resume_id}", headers=headers_b)
    if r_sec_res.status_code not in (403, 404):
        print(f"[FAIL] Security breach: User B accessed User A resume! Status: {r_sec_res.status_code}")
        sys.exit(1)
    print("  [SUCCESS] Resume security isolation verified (404/403 returned).")

    r_sec_proj = requests.get(f"{BASE_URL}/projects/{project_id}", headers=headers_b)
    if r_sec_proj.status_code not in (403, 404):
        print(f"[FAIL] Security breach: User B accessed User A project! Status: {r_sec_proj.status_code}")
        sys.exit(1)
    print("  [SUCCESS] Project security isolation verified (404/403 returned).")

    # 8. Start RESUME-Based Mock Interview
    print("\n8. Starting RESUME-Based Mock Interview Session...")
    cats = requests.get(f"{BASE_URL}/interview-categories", headers=headers_a).json()
    category_id = cats[0]["id"]

    resume_session_req = {
        "categoryId": category_id,
        "difficulty": "MEDIUM",
        "questionCount": 3,
        "durationMinutes": 30,
        "mode": "RESUME",
        "interactionMode": "TEXT",
        "resumeId": resume_id
    }
    r_sess_res = requests.post(f"{BASE_URL}/interview-sessions", headers=headers_a, json=resume_session_req)
    if r_sess_res.status_code != 201:
        print(f"[FAIL] Resume session creation failed: {r_sess_res.status_code} {r_sess_res.text}")
        sys.exit(1)
    
    res_session_id = r_sess_res.json()["id"]
    requests.post(f"{BASE_URL}/interview-sessions/{res_session_id}/start", headers=headers_a)
    
    r_q1_res = requests.get(f"{BASE_URL}/interview-sessions/{res_session_id}/current-question", headers=headers_a)
    if r_q1_res.status_code != 200:
        print(f"[FAIL] Failed to fetch Question 1 for Resume mode: {r_q1_res.text}")
        sys.exit(1)
    
    q1_res_data = r_q1_res.json()
    print(f"  [SUCCESS] Resume Session Started: ID {res_session_id}")
    print(f"  [SUCCESS] Q1 Text: {q1_res_data['questionText']}")

    # 9. Start PROJECT-Based Mock Interview
    print("\n9. Starting PROJECT-Based Mock Interview Session...")
    time.sleep(2)
    project_session_req = {
        "categoryId": category_id,
        "difficulty": "HARD",
        "questionCount": 3,
        "durationMinutes": 30,
        "mode": "PROJECT",
        "interactionMode": "TEXT",
        "projectId": project_id
    }
    r_sess_proj = requests.post(f"{BASE_URL}/interview-sessions", headers=headers_a, json=project_session_req)
    if r_sess_proj.status_code != 201:
        print(f"[FAIL] Project session creation failed: {r_sess_proj.status_code} {r_sess_proj.text}")
        sys.exit(1)
    
    proj_session_id = r_sess_proj.json()["id"]
    requests.post(f"{BASE_URL}/interview-sessions/{proj_session_id}/start", headers=headers_a)
    time.sleep(1)
    
    r_q1_proj = requests.get(f"{BASE_URL}/interview-sessions/{proj_session_id}/current-question", headers=headers_a)
    if r_q1_proj.status_code != 200:
        print(f"[FAIL] Failed to fetch Question 1 for Project mode: {r_q1_proj.text}")
        sys.exit(1)
    
    q1_proj_data = r_q1_proj.json()
    print(f"  [SUCCESS] Project Session Started: ID {proj_session_id}")
    print(f"  [SUCCESS] Q1 Text: {q1_proj_data['questionText']}")

    # 10. Submit Answer & Verify Phase 7 Adaptive & Phase 8 Report Integration
    print("\n10. Submitting Answer to Project Question & Verifying Evaluation & Report...")
    ans_req = {
        "answerText": "In my LRU cache, I used a Doubly Linked List for O(1) removal/insertion and a ConcurrentHashMap for O(1) key lookup. To maintain thread safety under high concurrency, I used segment locking and lock-free atomic pointers for cache hit updates.",
        "inputMethod": "TEXT"
    }
    r_ans = requests.post(
        f"{BASE_URL}/interview-sessions/{proj_session_id}/questions/{q1_proj_data['id']}/answer",
        headers=headers_a,
        json=ans_req
    )
    if r_ans.status_code != 200:
        print(f"[FAIL] Answer submission failed: {r_ans.status_code} {r_ans.text}")
        sys.exit(1)
    
    ans_data = r_ans.json()
    print("  [SUCCESS] Answer evaluated successfully.")
    print(f"  [SUCCESS] Score: {ans_data['evaluation']['score']}/10")
    print(f"  [SUCCESS] Adaptive Decision: {ans_data['evaluation']['adaptiveDecision']}")

    # Phase 8 Report Compatibility Test
    r_report = requests.get(f"{BASE_URL}/reports/sessions/{proj_session_id}", headers=headers_a)
    if r_report.status_code != 200:
        print(f"[FAIL] Report fetch failed for Phase 9 session: {r_report.status_code} {r_report.text}")
        sys.exit(1)
    
    report_data = r_report.json()
    print(f"  [SUCCESS] Phase 8 Report generated for Project Session #{proj_session_id}:")
    print(f"  [SUCCESS] Overall Score: {report_data['overallScore']}/100, Level: {report_data['performanceLevel']}")

    print("\n" + "=" * 60)
    print("ALL PHASE 9 E2E VERIFICATIONS PASSED SUCCESSFULLY!")
    print("=" * 60)

if __name__ == "__main__":
    main()
