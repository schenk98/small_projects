import requests
import json
from time import time

base_url = 'http://localhost:8081'
session = requests.Session()

# Create unique email
email = f"histtest_{int(time())}@example.com"
password = "Pass123!"

print(f"Testing /history endpoint with email: {email}")

# Register
r = session.post(f"{base_url}/register", data={'email': email, 'password': password})
print(f"1. Register: {r.status_code}")

# Login
r = session.post(f"{base_url}/login", data={'email': email, 'password': password}, allow_redirects=True)
print(f"2. Login: {r.status_code}")

# Create book
r = session.post(f"{base_url}/books/new", data={'title': 'Test Book', 'author': 'Test Author', 'totalPages': 100})
print(f"3. Create book: {r.status_code}")

# Create session
r = session.post(f"{base_url}/new-session",
    data={'bookId': '1', 'date': '2026-09-20', 'minutes': '30', 'pagesFrom': '10', 'pagesTo': '35', 'note': 'Test session'},
    allow_redirects=True)
print(f"4. Create session: {r.status_code}")

# Test history endpoint
r = session.get(f"{base_url}/history")
print(f"5. GET /history: {r.status_code}")

if r.status_code == 200:
    # Check if history content is present
    if 'Historie' in r.text or 'historii' in r.text or 'ČTENÍ' in r.text:
        print("✓ History page loaded successfully!")
        print(f"  - Page size: {len(r.text)} bytes")
        if 'Test Book' in r.text:
            print("✓ Reading session appears in history!")
        if 'Čtení' in r.text or 'ČTENÍ' in r.text or 'čtení' in r.text.lower():
            print("✓ Session type label found!")
    else:
        print("✗ History page loaded but content seems missing")
        print(f"  First 500 chars: {r.text[:500]}")
else:
    print(f"✗ History endpoint returned {r.status_code}")
    print(f"  Response: {r.text[:200]}")

