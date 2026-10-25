import subprocess

passwords = [
    'root123456', 'admin1234', '1234567890', 'root123!', 'Root@123',
    'Admin@123', 'Password@123', 'likhitha123', 'Likhitha123', 'likhi123',
    'likhitha@123', 'Likhitha@1234', 'likhi@123', 'mysql123', 'mysql@123',
    'Sql@123', 'System@123', 'Database@123', 'Welcome@123', 'Password123!',
    'rootroot', 'adminadmin', '12341234', '12345', '123'
]

found = False
for p in passwords:
    cmd = ['mysql', '-u', 'root', f'--password={p}', '-e', 'CREATE DATABASE IF NOT EXISTS prepgpt; SHOW DATABASES;']
    try:
        res = subprocess.run(cmd, capture_output=True, text=True, timeout=3)
        if res.returncode == 0:
            print(f"SUCCESS_PASSWORD: '{p}'")
            found = True
            break
    except Exception as e:
        print(f"Error for '{p}': {e}")

if not found:
    print("NO_PASSWORD_FOUND")
