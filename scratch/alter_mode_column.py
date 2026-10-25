import mysql.connector

try:
    conn = mysql.connector.connect(
        host="localhost",
        user="root",
        password="",
        database="prepgpt"
    )
    cursor = conn.cursor()
    cursor.execute("ALTER TABLE interview_sessions MODIFY COLUMN mode VARCHAR(50) NOT NULL;")
    conn.commit()
    print("Successfully altered interview_sessions.mode to VARCHAR(50)")
    cursor.close()
    conn.close()
except Exception as e:
    print(f"Error: {e}")
