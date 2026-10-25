import urllib.request
import urllib.error
import json

login_url = 'http://localhost:8080/api/v1/auth/login'
login_data = json.dumps({'email': 'admin@prepgpt.com', 'password': 'AdminPassword123!'}).encode('utf-8')
req = urllib.request.Request(login_url, data=login_data, headers={'Content-Type': 'application/json'}, method='POST')

with urllib.request.urlopen(req) as resp:
    token = json.loads(resp.read().decode('utf-8'))['token']

print('Logged in as admin successfully')

cat_req = urllib.request.Request('http://localhost:8080/api/v1/interview-categories', headers={'Authorization': f'Bearer {token}'})
with urllib.request.urlopen(cat_req) as resp:
    cats = json.loads(resp.read().decode('utf-8'))
cat_id = cats[0]['id']

sess_payload = json.dumps({
    'categoryId': cat_id,
    'difficulty': 'MEDIUM',
    'questionCount': 3,
    'durationMinutes': 15,
    'mode': 'QUESTION_BANK',
    'interactionMode': 'VOICE'
}).encode('utf-8')

sess_req = urllib.request.Request('http://localhost:8080/api/v1/interview-sessions', data=sess_payload, headers={
    'Content-Type': 'application/json',
    'Authorization': f'Bearer {token}'
}, method='POST')

with urllib.request.urlopen(sess_req) as resp:
    session = json.loads(resp.read().decode('utf-8'))
sess_id = session['id']
print('Created session', sess_id, 'status:', session.get('status'))

# 1. Try TTS before starting (status is CONFIGURED)
tts_payload = json.dumps({'text': 'Hello world'}).encode('utf-8')
tts_req = urllib.request.Request(f'http://localhost:8080/api/v1/interview-sessions/{sess_id}/voice/synthesize', data=tts_payload, headers={
    'Content-Type': 'application/json',
    'Authorization': f'Bearer {token}'
}, method='POST')

try:
    with urllib.request.urlopen(tts_req) as resp:
        print('1. TTS in CONFIGURED status:', resp.getcode())
except urllib.error.HTTPError as e:
    print('1. TTS in CONFIGURED status error:', e.code, e.read().decode('utf-8'))

# 2. Now start session
start_req = urllib.request.Request(f'http://localhost:8080/api/v1/interview-sessions/{sess_id}/start', headers={'Authorization': f'Bearer {token}'}, method='POST')
with urllib.request.urlopen(start_req) as resp:
    started = json.loads(resp.read().decode('utf-8'))
print('2. Started session status:', started.get('status'))

# 3. Try TTS now with short text
tts_req2 = urllib.request.Request(f'http://localhost:8080/api/v1/interview-sessions/{sess_id}/voice/synthesize', data=tts_payload, headers={
    'Content-Type': 'application/json',
    'Authorization': f'Bearer {token}'
}, method='POST')
try:
    with urllib.request.urlopen(tts_req2) as resp:
        content_type = resp.headers.get('Content-Type')
        data = resp.read()
        print('3. TTS IN_PROGRESS status:', resp.getcode(), 'ContentType:', content_type, 'Bytes:', len(data))
except urllib.error.HTTPError as e:
    print('3. TTS IN_PROGRESS error:', e.code, e.read().decode('utf-8'))

# 4. Try TTS with empty text
empty_payload = json.dumps({'text': ''}).encode('utf-8')
tts_req3 = urllib.request.Request(f'http://localhost:8080/api/v1/interview-sessions/{sess_id}/voice/synthesize', data=empty_payload, headers={
    'Content-Type': 'application/json',
    'Authorization': f'Bearer {token}'
}, method='POST')
try:
    with urllib.request.urlopen(tts_req3) as resp:
        print('4. TTS empty text:', resp.getcode())
except urllib.error.HTTPError as e:
    print('4. TTS empty text error:', e.code, e.read().decode('utf-8'))

# 5. Try TTS with typical interview question from DB (349 chars)
q_text = 'Design and implement an LRU (Least Recently Used) cache class with capacity N that supports get(key) and put(key,value) operations, both in O(1) time. The get operation should return the value if the key exists, otherwise -1. The put operation should insert or update the value and, if the cache exceeds capacity, evict the least recently used item.'
q_payload = json.dumps({'text': q_text}).encode('utf-8')
tts_req4 = urllib.request.Request(f'http://localhost:8080/api/v1/interview-sessions/{sess_id}/voice/synthesize', data=q_payload, headers={
    'Content-Type': 'application/json',
    'Authorization': f'Bearer {token}'
}, method='POST')
try:
    with urllib.request.urlopen(tts_req4) as resp:
        print('5. TTS question text (349 chars):', resp.getcode(), 'Bytes:', len(resp.read()))
except urllib.error.HTTPError as e:
    print('5. TTS question text error:', e.code, e.read().decode('utf-8'))

# 6. Try TTS with very long question (e.g. 1200 chars)
long_q = 'Explain the following in great detail. ' * 30
long_payload = json.dumps({'text': long_q}).encode('utf-8')
tts_req5 = urllib.request.Request(f'http://localhost:8080/api/v1/interview-sessions/{sess_id}/voice/synthesize', data=long_payload, headers={
    'Content-Type': 'application/json',
    'Authorization': f'Bearer {token}'
}, method='POST')
try:
    with urllib.request.urlopen(tts_req5) as resp:
        print('6. TTS long question text (1170 chars):', resp.getcode(), 'Bytes:', len(resp.read()))
except urllib.error.HTTPError as e:
    print('6. TTS long question error:', e.code, e.read().decode('utf-8'))
