"""Opens a pull request for the current branch.

Uses the credential Git already has for github.com, so nothing is written to
disk and the token is never printed.
"""
import json
import subprocess
import sys
import urllib.error
import urllib.request

OWNER = "Chaosxenpai96"
REPO = "DuGO"
BASE = "main"
HEAD = "feature/profile-settings"


def github_token():
    """Asks git for the stored credential and returns the password."""
    proc = subprocess.run(
        ["git", "credential", "fill"],
        input="protocol=https\nhost=github.com\n\n",
        capture_output=True,
        text=True,
    )
    for line in proc.stdout.splitlines():
        if line.startswith("password="):
            return line.split("=", 1)[1].strip()
    return None


def api(path, token, method="GET", body=None):
    url = f"https://api.github.com/repos/{OWNER}/{REPO}{path}"
    data = json.dumps(body).encode() if body is not None else None
    req = urllib.request.Request(url, data=data, method=method)
    req.add_header("Authorization", f"Bearer {token}")
    req.add_header("Accept", "application/vnd.github+json")
    req.add_header("X-GitHub-Api-Version", "2022-11-28")
    if data:
        req.add_header("Content-Type", "application/json")
    try:
        with urllib.request.urlopen(req, timeout=45) as r:
            return r.status, json.loads(r.read().decode() or "{}")
    except urllib.error.HTTPError as e:
        return e.code, e.read().decode()[:2000]


token = github_token()
if not token:
    print("No GitHub credential found.")
    sys.exit(1)

body = open(sys.argv[1], encoding="utf-8").read()

status, existing = api(f"/pulls?head={OWNER}:{HEAD}&base={BASE}&state=open", token)
if status == 200 and existing:
    print("A pull request is already open:", existing[0]["html_url"])
    sys.exit(0)

status, result = api(
    "/pulls",
    token,
    method="POST",
    body={
        "title": "Location-driven donors, real messaging, and auto sign-up location",
        "head": HEAD,
        "base": BASE,
        "body": body,
    },
)
if status == 201:
    print("Pull request opened:", result["html_url"])
    print("Number:", result["number"])
else:
    print("HTTP", status)
    print(result)
