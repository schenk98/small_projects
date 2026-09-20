#!/bin/bash
# Test history endpoint
email="historytest_$(date +%s)@example.com"
pass="Pass123!"

echo "Testing history endpoint..."
echo "1. Register user: $email"
curl -sS -c /tmp/cookies.txt -d "email=$email&password=$pass" http://localhost:8081/register -o /dev/null

echo "2. Login..."
curl -sS -b /tmp/cookies.txt -L -d "email=$email&password=$pass" http://localhost:8081/login -o /dev/null

echo "3. Create book..."
curl -sS -b /tmp/cookies.txt -L -d "title=Test%20Book&author=Test%20Author&totalPages=100" http://localhost:8081/books/new -o /dev/null

echo "4. Create session..."
curl -sS -b /tmp/cookies.txt -L -d "bookId=1&date=2026-09-20&minutes=30&pagesFrom=10&pagesTo=35&note=Test%20Note" http://localhost:8081/new-session -o /dev/null

echo "5. Access history..."
curl -sS -b /tmp/cookies.txt -w "\nHTTP Status: %{http_code}\n" http://localhost:8081/history | head -n 20

echo "Done."

