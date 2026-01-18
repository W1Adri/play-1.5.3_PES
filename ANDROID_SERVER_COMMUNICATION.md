# Android App - Server Communication Guide

## Overview
This document explains how the native Android application communicates with the Play Framework backend server.

## Architecture

```
┌─────────────────────┐         JSON/HTTP          ┌──────────────────────┐
│   Android App       │ ◄────────────────────────► │   Play Framework     │
│   (MyApplication)   │                            │   Server (tt/)       │
└─────────────────────┘                            └──────────────────────┘
      │                                                      │
      │                                                      │
      ├─ MainActivity.java                                  ├─ Application.java
      ├─ ApiClient.java                                     ├─ Routes (conf/routes)
      ├─ RegisterActivity.java                              └─ Models (Usuario, etc.)
      └─ Other Activities...
```

## API Client Configuration

### Base URL
The `ApiClient.java` class defines the server URL:

```java
public static final String BASE_URL = "http://10.0.2.2:9000";
```

**Important Notes:**
- `10.0.2.2` is a special IP that Android Emulator uses to access the host machine's localhost
- For **real Android devices**, change this to your computer's IP address on the local network
- For **production**, use your actual server URL (e.g., `https://yourserver.com`)

## Authentication Flow

### 1. Login Process

**Android Side (MainActivity.java):**
```java
JSONObject payload = new JSONObject();
payload.put("username", user);
payload.put("password", pass);
ApiClient.ApiResponse response = ApiClient.postJson("/api/login", payload);
```

**Server Side (Application.java - apiLogin):**
- Receives JSON with `Content-Type: application/json`
- Extracts `username` and `password` from JSON body
- Validates credentials against database
- Returns JSON response with user data

**Response Format:**
```json
{
  "status": "ok",
  "msg": "Login OK",
  "user": {
    "id": 1,
    "username": "testuser",
    "fullName": "Test User",
    "rol": "ALUMNO"
  }
}
```

### 2. Session Management

The Play Framework uses **cookies** for session management:
- After successful login, server sends a session cookie
- `ApiClient` uses `CookieManager` to automatically store and send cookies
- All subsequent API calls include the session cookie
- Session persists until logout or expiration

## API Endpoints Used by Android App

### Authentication
- **POST /api/login** - User login
- **POST /api/register** - New user registration
- **POST /api/logout** - User logout
- **GET /api/me** - Get current logged-in user info

### Student Features (ALUMNO)
- **GET /api/materias** - List all subjects/courses
- **GET /api/materia/{id}** - Get subject details
- **POST /api/inscribirse** - Enroll in a course
- **GET /api/inscripciones** - Get my enrollments
- **GET /api/reservas** - Get my class reservations
- **POST /api/reservas** - Create new class reservation

### Teacher Features (PROFESOR)
- **GET /api/inscripciones** - Get my students
- **GET /api/gestion/usuarios** - Manage users
- **GET /api/gestion/materias** - Manage courses

### Messaging
- **POST /api/chat/enviar** - Send message
- **GET /api/chat/mensajes** - Get messages

### Statistics
- **GET /api/consultas** - Get various statistics

## Request/Response Format

### Sending JSON Requests

The `ApiClient.postJson()` method handles JSON serialization:

```java
// Android code
JSONObject payload = new JSONObject();
payload.put("username", "testuser");
payload.put("password", "testpass");

ApiClient.ApiResponse response = ApiClient.postJson("/api/login", payload);
```

This sends:
```
POST /api/login HTTP/1.1
Content-Type: application/json
Content-Length: 47

{"username":"testuser","password":"testpass"}
```

### Receiving JSON Responses

```java
ApiClient.ApiResponse response = ApiClient.postJson("/api/login", payload);
JSONObject json = ApiClient.parseJson(response.body);

if (response.code >= 200 && response.code < 300) {
    String status = json.optString("status", "error");
    if ("ok".equals(status)) {
        JSONObject userObj = json.getJSONObject("user");
        String userId = userObj.optString("id");
        // ... process user data
    }
}
```

## Error Handling

### HTTP Status Codes
- **200 OK** - Request successful (but check JSON `status` field)
- **400 Bad Request** - Invalid request data
- **401 Unauthorized** - Not logged in or session expired
- **403 Forbidden** - Logged in but not authorized for this action
- **404 Not Found** - Endpoint or resource not found
- **500 Internal Server Error** - Server error

### Application-Level Errors

Even with HTTP 200, check the JSON `status` field:

```json
{
  "status": "error",
  "msg": "Usuario o contraseña incorrectos"
}
```

Common error messages:
- `"Faltan credenciales"` - Missing username or password
- `"Usuario o contraseña incorrectos"` - Invalid credentials
- `"No logueado"` - Not logged in (session expired)
- `"El usuario ya existe"` - Username already taken
- `"Acceso no autorizado"` - Not authorized for this action

## Testing

### Test Server Connection
```java
btnPing.setOnClickListener(v -> {
    new Thread(() -> {
        try {
            ApiClient.ApiResponse response = ApiClient.get("/api/ping");
            // Should return: {"status":"ok","msg":"Play funcionando"}
        } catch (Exception e) {
            // Handle connection error
        }
    }).start();
});
```

### Test Login
```java
JSONObject payload = new JSONObject();
payload.put("username", "testuser");
payload.put("password", "testpass");

ApiClient.ApiResponse response = ApiClient.postJson("/api/login", payload);
System.out.println("Response: " + response.body);
```

## Troubleshooting

### "Faltan credenciales" Error
**Fixed in this PR!** 
- **Problem:** Server was trying to read JSON body from already-consumed InputStream
- **Solution:** Modified `readRequestBody()` to read from `params.get("body")`
- **Status:** Should work now with JSON requests

### Connection Refused
- **Cause:** Server not running or wrong URL
- **Solution:** 
  1. Check server is running: `http://localhost:9000`
  2. Verify `BASE_URL` in ApiClient.java
  3. For emulator, use `10.0.2.2:9000`
  4. For real device, use computer's IP

### Session Expired
- **Cause:** Server session timeout or server restart
- **Solution:** Login again, session cookie will be renewed

### Slow Response
- **Cause:** Network latency or database query
- **Solution:** 
  1. Show loading indicator in UI
  2. Increase timeouts in ApiClient if needed
  3. Check server logs for slow queries

## Network Security

### Android Manifest Permissions
```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
```

### Cleartext Traffic (for development)
For Android 9+ to allow HTTP (not HTTPS):
```xml
<application
    android:usesCleartextTraffic="true"
    ...>
```

**Important:** For production, use HTTPS only!

## Best Practices

1. **Always run network calls in background threads**
   ```java
   new Thread(() -> {
       // API call here
       runOnUiThread(() -> {
           // Update UI here
       });
   }).start();
   ```

2. **Handle all exceptions**
   ```java
   try {
       ApiClient.ApiResponse response = ApiClient.postJson("/api/login", payload);
   } catch (IOException e) {
       // Network error
   } catch (JSONException e) {
       // JSON parsing error
   }
   ```

3. **Check both HTTP status and JSON status**
   ```java
   if (response.code == 200) {
       JSONObject json = ApiClient.parseJson(response.body);
       if ("ok".equals(json.optString("status"))) {
           // Success
       }
   }
   ```

4. **Don't log passwords**
   ```java
   // Bad:
   Log.d("API", "Login with: " + username + "/" + password);
   
   // Good:
   Log.d("API", "Login attempt for user: " + username);
   ```

## Future Improvements

1. **Add retry logic** for failed requests
2. **Implement request queue** for offline support
3. **Add request/response caching** for better performance
4. **Use Retrofit** or similar library for cleaner API client code
5. **Add token-based authentication** instead of session cookies
6. **Implement push notifications** for real-time updates
