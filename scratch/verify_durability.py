import subprocess

def run_query(sql):
    cmd = ['mysql', '-u', 'root', '--password=1234567890', '-D', 'prepgpt', '-e', sql]
    res = subprocess.run(cmd, capture_output=True, text=True)
    return res.stdout.strip()

print("--- 1. VERIFY CANDIDATE ANSWER DURABILITY AFTER GROQ 503 ---")
print(run_query("SELECT id, interview_question_id, SUBSTRING(answer_text, 1, 50) as answer_snippet, input_method, answered_at FROM interview_responses WHERE interview_question_id=250;"))

print("\n--- 2. VERIFY NO FAKE EVALUATION CREATED ---")
print(run_query("SELECT count(*) as eval_count FROM interview_evaluations WHERE interview_response_id IN (SELECT id FROM interview_responses WHERE interview_question_id=250);"))

print("\n--- 3. VERIFY OPTIMISTIC LOCKING VERSION COLUMN ---")
print(run_query("SHOW COLUMNS FROM interview_sessions LIKE 'version';"))

print("\n--- 4. VERIFY DATABASE INDEX: idx_session_user_created ON interview_sessions ---")
print(run_query("SHOW INDEX FROM interview_sessions WHERE Key_name='idx_session_user_created';"))

print("\n--- 5. VERIFY DATABASE INDEX: idx_resume_user ON resumes ---")
print(run_query("SHOW INDEX FROM resumes WHERE Key_name='idx_resume_user';"))

print("\n--- 6. VERIFY DATABASE INDEX: idx_project_user ON projects ---")
print(run_query("SHOW INDEX FROM projects WHERE Key_name='idx_project_user';"))

print("\n--- 7. VERIFY NO REDUNDANT DUPLICATE INDEX ON interview_evaluations ---")
print(run_query("SHOW INDEX FROM interview_evaluations;"))
