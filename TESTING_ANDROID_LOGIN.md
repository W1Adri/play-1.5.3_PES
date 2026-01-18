# Testing Android Login Fix

## Problem Fixed
The Android application was receiving "Faltan credenciales" (Missing credentials) when trying to login, even though it was sending valid JSON with username and password.

## Root Cause
In Play Framework 1.x, when a request with `Content-Type: application/json` arrives:
1. The `TextParser` reads the entire `request.body` InputStream
2. It stores the content in `params.get("body")` 
3. The InputStream is consumed and cannot be read again
4. The old code tried to read from the already-consumed `request.body`

## Solution
Modified `readRequestBody()` method in `Application.java` to:
1. **First** try reading from `params.get("body")` (where TextParser stores JSON content)
2. **As fallback** try reading from `request.body` (in case it hasn't been consumed yet)

## How to Test

### 1. Start the Play Framework Server

```bash
cd "play-1.5.3_PES-master (1)/play-1.5.3_PES-master"
# If you have Python 2:
./play run tt

# If you only have Python 3, you'll need to convert the script or use Java directly
```

### 2. Test with curl (JSON login)

```bash
# Test API ping (should work without authentication)
curl -X GET http://localhost:9000/api/ping

# Test login with JSON body (this is what the Android app sends)
curl -X POST http://localhost:9000/api/login \
  -H "Content-Type: application/json" \
  -d '{"username":"test","password":"test"}'

# Expected response if credentials are wrong:
# {"status":"error","msg":"Usuario o contraseña incorrectos"}

# Expected response if credentials are correct:
# {"status":"ok","msg":"Login OK","user":{"id":1,"username":"test","fullName":"Test User","rol":"ALUMNO"}}
```

### 3. Test with curl (form-encoded login)

```bash
# The fix also supports traditional form-encoded requests
curl -X POST http://localhost:9000/api/login \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "username=test&password=test"
```

### 4. Test Registration

```bash
# Register a new user
curl -X POST http://localhost:9000/api/register \
  -H "Content-Type: application/json" \
  -d '{
    "username":"testuser",
    "password":"testpass123",
    "email":"test@example.com",
    "fullName":"Test User",
    "rol":"alumno"
  }'

# Then try to login with the new credentials
curl -X POST http://localhost:9000/api/login \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser","password":"testpass123"}'
```

### 5. Test from Android Application

1. Make sure the Play server is running on port 9000
2. Update `ApiClient.java` if needed:
   ```java
   // For Android Emulator accessing localhost:
   public static final String BASE_URL = "http://10.0.2.2:9000";
   
   // For real device on same network:
   // public static final String BASE_URL = "http://YOUR_COMPUTER_IP:9000";
   ```
3. Build and run the Android app
4. Try to login with valid credentials
5. Check the Play server logs for the debug messages

## Expected Server Logs

With the fix and debug logging, you should see:
```
[apiLogin] Parametros recibidos: username=null, password=***
[apiLogin] Content-Type: application/json
[apiLogin] Method: POST
[readRequestBody] Body leido desde params.get('body'): {"username":"testuser","password":"testpass123"}
[getCachedJsonBody] JSON parseado correctamente: {"username":"testuser","password":"testpass123"}
[getJsonParam] Extraido key=username, value=testuser
[getJsonParam] Extraido key=password, value=***
[apiLogin] Parametros resueltos: username=testuser, password=***
```

## Files Changed

1. **Application.java** - Fixed `readRequestBody()` method to read from `params.get("body")` first
2. Added extensive logging for debugging
3. Added JavaDoc documentation for key methods

## Benefits

This fix enables all JSON-based API endpoints to work correctly:
- `/api/login` - User login
- `/api/register` - User registration  
- `/api/inscribirse` - Course enrollment
- `/api/reservas` - Class reservations
- `/api/chat/enviar` - Send messages
- `/api/gestion/*` - User and course management
- And all other API endpoints that accept JSON

## Rollback Instructions

If for any reason this fix causes issues, you can revert by changing line 1481 in Application.java back to:
```java
return play.libs.IO.readContentAsString(request.body);
```

However, this would break JSON requests from the Android app again.
