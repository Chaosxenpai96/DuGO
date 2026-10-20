"""Checks the Firestore rules against the cases the app actually performs.

Uses the API key from app/google-services.json to call the Firebase Rules
test endpoint. The key is read from the file and never printed.
"""
import json
import urllib.error
import urllib.request

d = json.load(open('app/google-services.json', encoding='utf-8'))
key = None
for p, v in d.items():
    if isinstance(v, list):
        for c in v:
            api = c.get('api_key')
            if api:
                key = api[0]['current_key'] if isinstance(api, list) else api['current_key']
project = d['project_info']['project_id']
rules = open('firestore.rules', encoding='utf-8').read()

DB = f"/projects/{project}/databases/(default)/documents"

# (label, method, path, resource or None, signed-in uid, expected result)
CASES = [
    ("any signed-in user can read another profile",
     "list", f"{DB}/users", None, "user1", "ALLOW"),
    ("a user can write their own profile",
     "create", f"{DB}/users/user1",
     {"fullName": "A", "role": "Blood Donor", "latitude": 10.0, "longitude": 123.0},
     "user1", "ALLOW"),
    ("a user cannot write someone else's profile",
     "create", f"{DB}/users/user2", {"fullName": "B"}, "user1", "DENY"),
    ("any signed-in user can read the request feed",
     "list", f"{DB}/requests", None, "user1", "ALLOW"),
    ("a user can post a request as themselves",
     "create", f"{DB}/requests/req1",
     {"posterUid": "user1", "bloodType": "O+", "posterName": "A"}, "user1", "ALLOW"),
    ("a user cannot post a request as someone else",
     "create", f"{DB}/requests/req2",
     {"posterUid": "user2", "bloodType": "O+", "posterName": "B"}, "user1", "DENY"),
    ("a user can read a conversation they are part of",
     "get", f"{DB}/conversations/user1_user2",
     {"participantIds": ["user1", "user2"]}, "user1", "ALLOW"),
    ("a user cannot read a conversation they are not part of",
     "get", f"{DB}/conversations/user2_user3",
     {"participantIds": ["user2", "user3"]}, "user1", "DENY"),
    ("a participant can create a conversation",
     "create", f"{DB}/conversations/user1_user2",
     {"participantIds": ["user1", "user2"]}, "user1", "ALLOW"),
    ("a participant can send a message",
     "create", f"{DB}/conversations/user1_user2/messages/m1",
     {"senderUid": "user1", "text": "hi"}, "user1", "ALLOW"),
    ("a non participant cannot send a message",
     "create", f"{DB}/conversations/user2_user3/messages/m1",
     {"senderUid": "user1", "text": "hi"}, "user1", "DENY"),
]

test_cases = []
labels = []
for label, method, path, resource, uid, expect in CASES:
    req = {
        "auth": {"uid": uid},
        "method": method,
        "path": path,
    }
    if resource is not None:
        req["resource"] = {"data": resource}
    test_cases.append({"expectation": expect, "request": req})
    labels.append(label)

body = {
    "testSuite": {
        "testCases": test_cases,
    },
    "source": {
        "files": [{"name": "firestore.rules", "content": rules}],
        "language": "FIREBASE_RULES",
    },
}

url = f"https://firebaserules.googleapis.com/v1/projects/{project}:test"
req = urllib.request.Request(url, data=json.dumps(body).encode(), method='POST')
req.add_header('Content-Type', 'application/json')
req.add_header('x-goog-api-key', key)

try:
    with urllib.request.urlopen(req, timeout=60) as r:
        status = r.status
        payload = r.read().decode()
except urllib.error.HTTPError as e:
    status = e.code
    payload = e.read().decode()
except Exception as e:
    status = 0
    payload = str(e)

print("HTTP", status)
try:
    parsed = json.loads(payload)
except Exception:
    print(payload[:3000])
    raise SystemExit(1)

if "error" in parsed:
    print(json.dumps(parsed["error"], indent=2)[:3000])
    raise SystemExit(1)

failed = 0
for i, r in enumerate(parsed.get("testResults", [])):
    exp = r.get("expectation", "?")
    ok = r.get("result") == "PASSED"
    if not ok:
        failed += 1
    label = labels[i] if i < len(labels) else r.get("testCase", {}).get("name", "?")
    print(f"  {'PASS' if ok else 'FAIL'}  {exp:6s} {label}")
    if not ok:
        print("        ->", r.get("error", {}).get("message", ""))

print(f"\n{len(parsed.get('testResults', [])) - failed}/{len(parsed.get('testResults', []))} passed")
raise SystemExit(1 if failed else 0)
