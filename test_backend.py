import requests
import time

BASE_URL = "http://localhost:8080/api"

print("========================================")
print("  QUANTUM JWT FRAMEWORK - FULL TEST     ")
print("========================================")

def test_endpoint(name, method, url, data=None, headers=None, expected_status_list=[200]):
    print(f"\n[TEST] {name}")
    try:
        if method == "POST":
            response = requests.post(url, data=data, headers=headers)
        elif method == "GET":
            response = requests.get(url, headers=headers)
        
        status = response.status_code
        if status in expected_status_list:
            print(f"  ✅ SUCCESS (Status {status})")
            return True, response.json()
        else:
            print(f"  ❌ FAILED (Status {status}) - Expected {expected_status_list}")
            print(f"     Response: {response.text}")
            return False, None
    except Exception as e:
        print(f"  ❌ ERROR: {e}")
        return False, None

# 1. Test Metrics
success, metrics = test_endpoint(
    "Check System Metrics", 
    "GET", 
    f"{BASE_URL}/metrics"
)
if success:
    print(f"     Pool Size: {metrics.get('poolSize', 'N/A')}")
    print(f"     Historical Total Keys: {metrics.get('historicalTotalKeys', 'N/A')}")

# 2. Signup User
username = f"test_{int(time.time())}"
password = "supersecretpassword123"

test_endpoint(
    "Signup a new user",
    "POST",
    f"{BASE_URL}/auth/signup",
    data={"username": username, "password": password}
)

# 3. Traditional JWT Login
success, std_data = test_endpoint(
    "Traditional JWT Login",
    "POST",
    f"{BASE_URL}/auth/standard/token",
    data={"username": username, "password": password}
)
std_token = std_data['token'] if success and std_data else None

# 4. Traditional JWT Validate
if std_token:
    test_endpoint(
        "Validate Traditional JWT",
        "GET",
        f"{BASE_URL}/auth/standard/validate",
        headers={"Authorization": f"Bearer {std_token}"}
    )

# 5. Quantum JWT Login
success, q_data = test_endpoint(
    "Quantum JWT Login",
    "POST",
    f"{BASE_URL}/auth/token",
    data={"username": username, "password": password}
)
q_token = q_data['token'] if success and q_data else None

if q_token:
    print("\n  >> Quantum Token Acquired:")
    print(f"     {q_token[:30]}...{q_token[-30:]}")

# 6. Quantum JWT Validate
if q_token:
    test_endpoint(
        "Validate Quantum JWT",
        "GET",
        f"{BASE_URL}/auth/validate",
        headers={"Authorization": f"Bearer {q_token}"}
    )

    # 7. Quantum Protected Resource
    test_endpoint(
        "Access Quantum Protected Resource",
        "GET",
        f"{BASE_URL}/example/protected/quantum",
        headers={"Authorization": f"Bearer {q_token}"}
    )

# 8. Test IP Binding Rejection (if we change the X-Forwarded-For header)
if q_token:
    test_endpoint(
        "Test IP Binding Security (Should Fail)",
        "GET",
        f"{BASE_URL}/auth/validate",
        headers={
            "Authorization": f"Bearer {q_token}",
            "X-Forwarded-For": "192.168.99.99"
        },
        expected_status_list=[401, 403, 500] 
    )

print("\n========================================")
print("  TESTING COMPLETE                      ")
print("========================================")
