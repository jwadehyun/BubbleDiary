### Notes

curl command for posting a diary bubble:
curl -X POST http://localhost:8080/api/messages \
-H "Content-Type: application/json" \
-d '{"text": "my first diary bubble!"}'

#### Shutdown
sudo lsof -i :PORT_NUMBER
kill PID