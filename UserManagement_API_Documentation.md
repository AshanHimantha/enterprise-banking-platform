# User Management API Documentation

## Overview
The User Management API provides endpoints for managing user accounts in the enterprise banking platform. This API is restricted to users with ADMIN or EMPLOYEE roles and includes functionality for listing, searching, suspending, and reactivating user accounts.

**Base URL:** `/admin/manage/users`

**Authentication:** Required (ADMIN or EMPLOYEE roles)

**Content-Type:** `application/json`

---

## Table of Contents
1. [List All Users](#1-list-all-users)
2. [Get User by Username](#2-get-user-by-username)
3. [Search Users](#3-search-users)
4. [Suspend User Account](#4-suspend-user-account)
5. [Reactivate User Account](#5-reactivate-user-account)
6. [Common Error Responses](#common-error-responses)
7. [Data Models](#data-models)
8. [Authentication & Authorization](#authentication--authorization)

---

## 1. List All Users

Retrieves a list of all users in the system.

### Endpoint
```
GET /admin/manage/users
```

### Request Headers
| Header | Type | Required | Description |
|--------|------|----------|-------------|
| `Authorization` | string | Yes | Bearer token or session authentication |

### Response

#### Success Response (200 OK)
```json
[
  {
    "id": 1,
    "username": "johndoe",
    "email": "john.doe@example.com",
    "firstName": "John",
    "lastName": "Doe",
    "accountLevel": "GOLD",
    "status": "ACTIVE",
    "kycStatus": "VERIFIED",
    "createdAt": "2025-01-15T10:30:00",
    "updatedAt": "2025-01-15T10:30:00"
  },
  {
    "id": 2,
    "username": "janedoe",
    "email": "jane.doe@example.com",
    "firstName": "Jane",
    "lastName": "Doe",
    "accountLevel": "PLATINUM",
    "status": "SUSPENDED",
    "kycStatus": "VERIFIED",
    "createdAt": "2025-01-10T09:15:00",
    "updatedAt": "2025-01-16T14:20:00"
  }
]
```

#### Error Response (500 Internal Server Error)
```json
{
  "error": "INTERNAL_ERROR",
  "message": "Unable to retrieve user list. Please try again later.",
  "timestamp": "2025-07-13T10:30:00"
}
```

### Example Request
```bash
curl -X GET \
  'https://localhost:8080/admin/admin/manage/users' \
  -H 'Authorization: Bearer your-jwt-token'
```

---

## 2. Get User by Username

Retrieves detailed information about a specific user by their username.

### Endpoint
```
GET /admin/manage/users/{username}
```

### Path Parameters
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `username` | string | Yes | The username of the user to retrieve |

### Request Headers
| Header | Type | Required | Description |
|--------|------|----------|-------------|
| `Authorization` | string | Yes | Bearer token or session authentication |

### Response

#### Success Response (200 OK)
```json
{
  "id": 1,
  "username": "johndoe",
  "email": "john.doe@example.com",
  "firstName": "John",
  "lastName": "Doe",
  "accountLevel": "GOLD",
  "status": "ACTIVE",
  "kycStatus": "VERIFIED",
  "createdAt": "2025-01-15T10:30:00",
  "updatedAt": "2025-01-15T10:30:00"
}
```

#### Error Response (400 Bad Request)
```json
{
  "error": "INVALID_INPUT",
  "message": "Username cannot be empty.",
  "timestamp": "2025-07-13T10:30:00"
}
```

#### Error Response (404 Not Found)
```json
{
  "error": "USER_NOT_FOUND",
  "message": "User 'johndoe' was not found in the system.",
  "timestamp": "2025-07-13T10:30:00"
}
```

### Example Request
```bash
curl -X GET \
  'https://localhost:8080/admin/admin/manage/users/johndoe' \
  -H 'Authorization: Bearer your-jwt-token'
```

---

## 3. Search Users

Search for users with pagination and filtering capabilities.

### Endpoint
```
GET /admin/manage/users/search
```

### Query Parameters
| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `page` | integer | No | 1 | Page number (1-based) |
| `limit` | integer | No | 20 | Number of results per page (1-1000) |
| `accountLevel` | string | No | - | Filter by account level (BRONZE, GOLD, PLATINUM, DIAMOND) |
| `status` | string | No | - | Filter by user status (ACTIVE, INACTIVE, SUSPENDED, DEACTIVATED) |
| `kycStatus` | string | No | - | Filter by KYC status (PENDING, VERIFIED, REJECTED) |
| `username` | string | No | - | Search by username (partial match) |
| `email` | string | No | - | Search by email (partial match) |

### Request Headers
| Header | Type | Required | Description |
|--------|------|----------|-------------|
| `Authorization` | string | Yes | Bearer token or session authentication |

### Response

#### Success Response (200 OK)
```json
{
  "users": [
    {
      "id": 1,
      "username": "johndoe",
      "email": "john.doe@example.com",
      "firstName": "John",
      "lastName": "Doe",
      "accountLevel": "GOLD",
      "status": "ACTIVE",
      "kycStatus": "VERIFIED",
      "createdAt": "2025-01-15T10:30:00",
      "updatedAt": "2025-01-15T10:30:00"
    }
  ],
  "totalCount": 150,
  "page": 1,
  "limit": 20,
  "hasMore": true
}
```

#### Error Responses

**400 Bad Request - Invalid Pagination:**
```json
{
  "error": "INVALID_INPUT",
  "message": "Page number must be greater than 0.",
  "timestamp": "2025-07-13T10:30:00"
}
```

**400 Bad Request - Invalid Limit:**
```json
{
  "error": "INVALID_INPUT",
  "message": "Limit must be between 1 and 1000.",
  "timestamp": "2025-07-13T10:30:00"
}
```

**400 Bad Request - Invalid Account Level:**
```json
{
  "error": "INVALID_INPUT",
  "message": "Invalid account level. Valid values are: BRONZE, GOLD, PLATINUM, DIAMOND.",
  "timestamp": "2025-07-13T10:30:00"
}
```

**400 Bad Request - Invalid Status:**
```json
{
  "error": "INVALID_INPUT",
  "message": "Invalid status. Valid values are: ACTIVE, INACTIVE, SUSPENDED, DEACTIVATED.",
  "timestamp": "2025-07-13T10:30:00"
}
```

**400 Bad Request - Invalid KYC Status:**
```json
{
  "error": "INVALID_INPUT",
  "message": "Invalid KYC status. Valid values are: PENDING, VERIFIED, REJECTED.",
  "timestamp": "2025-07-13T10:30:00"
}
```

### Example Requests

**Basic search (default pagination):**
```bash
curl -X GET \
  'https://localhost:8080/admin/admin/manage/users/search' \
  -H 'Authorization: Bearer your-jwt-token'
```

**Search with filters:**
```bash
curl -X GET \
  'https://localhost:8080/admin/admin/manage/users/search?status=ACTIVE&accountLevel=GOLD&limit=50' \
  -H 'Authorization: Bearer your-jwt-token'
```

**Search by username:**
```bash
curl -X GET \
  'https://localhost:8080/admin/admin/manage/users/search?username=john&limit=10' \
  -H 'Authorization: Bearer your-jwt-token'
```

**Pagination example:**
```bash
curl -X GET \
  'https://localhost:8080/admin/admin/manage/users/search?page=2&limit=100' \
  -H 'Authorization: Bearer your-jwt-token'
```

---

## 4. Suspend User Account

Suspends a user account with a specified reason.

### Endpoint
```
POST /admin/manage/users/{username}/suspend
```

### Path Parameters
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `username` | string | Yes | The username of the user to suspend |

### Request Headers
| Header | Type | Required | Description |
|--------|------|----------|-------------|
| `Content-Type` | string | Yes | Must be `application/json` |
| `Authorization` | string | Yes | Bearer token or session authentication |

### Request Body
```json
{
  "reason": "string"
}
```

| Field | Type | Required | Constraints | Description |
|-------|------|----------|-------------|-------------|
| `reason` | string | Yes | 1-500 characters | The reason for suspending the user account |

### Response

#### Success Response (200 OK)
```json
{
  "message": "User 'johndoe' has been suspended successfully."
}
```

#### Error Responses

**400 Bad Request - Invalid Input:**
```json
{
  "error": "INVALID_INPUT",
  "message": "Username cannot be empty.",
  "timestamp": "2025-07-13T10:30:00"
}
```

**400 Bad Request - Missing Reason:**
```json
{
  "error": "INVALID_INPUT",
  "message": "A reason for suspension is required and cannot be empty.",
  "timestamp": "2025-07-13T10:30:00"
}
```

**400 Bad Request - Reason Too Long:**
```json
{
  "error": "INVALID_INPUT",
  "message": "Suspension reason cannot exceed 500 characters.",
  "timestamp": "2025-07-13T10:30:00"
}
```

**400 Bad Request - Invalid Request:**
```json
{
  "error": "INVALID_REQUEST",
  "message": "User not found.",
  "timestamp": "2025-07-13T10:30:00"
}
```

**409 Conflict - Operation Not Allowed:**
```json
{
  "error": "OPERATION_NOT_ALLOWED",
  "message": "User is already suspended.",
  "timestamp": "2025-07-13T10:30:00"
}
```

### Example Request
```bash
curl -X POST \
  'https://localhost:8080/admin/admin/manage/users/johndoe/suspend' \
  -H 'Content-Type: application/json' \
  -H 'Authorization: Bearer your-jwt-token' \
  -d '{
    "reason": "Suspicious account activity detected"
  }'
```

---

## 5. Reactivate User Account

Reactivates a previously suspended user account.

### Endpoint
```
POST /admin/manage/users/{username}/reactivate
```

### Path Parameters
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `username` | string | Yes | The username of the user to reactivate |

### Request Headers
| Header | Type | Required | Description |
|--------|------|----------|-------------|
| `Authorization` | string | Yes | Bearer token or session authentication |

### Response

#### Success Response (200 OK)
```json
{
  "message": "User 'johndoe' has been reactivated successfully."
}
```

#### Error Responses

**400 Bad Request - Invalid Input:**
```json
{
  "error": "INVALID_INPUT",
  "message": "Username cannot be empty.",
  "timestamp": "2025-07-13T10:30:00"
}
```

**400 Bad Request - Invalid Request:**
```json
{
  "error": "INVALID_REQUEST",
  "message": "User not found.",
  "timestamp": "2025-07-13T10:30:00"
}
```

**409 Conflict - Operation Not Allowed:**
```json
{
  "error": "OPERATION_NOT_ALLOWED",
  "message": "User account is not currently suspended.",
  "timestamp": "2025-07-13T10:30:00"
}
```

### Example Request
```bash
curl -X POST \
  'https://localhost:8080/admin/admin/manage/users/johndoe/reactivate' \
  -H 'Authorization: Bearer your-jwt-token'
```

---

## Common Error Responses

### HTTP Status Codes
| Status Code | Description |
|-------------|-------------|
| 200 | Success |
| 400 | Bad Request - Invalid input or request parameters |
| 401 | Unauthorized - Authentication required |
| 403 | Forbidden - Insufficient permissions |
| 404 | Not Found - Resource does not exist |
| 409 | Conflict - Operation not allowed in current state |
| 500 | Internal Server Error - System error |

### Error Response Format
All error responses follow this structure:
```json
{
  "error": "ERROR_CODE",
  "message": "Human-readable error message",
  "timestamp": "2025-07-13T10:30:00"
}
```

### Error Codes
| Error Code | Description |
|------------|-------------|
| `INVALID_INPUT` | Invalid input parameters |
| `INVALID_REQUEST` | Invalid request (e.g., user not found) |
| `OPERATION_NOT_ALLOWED` | Operation cannot be performed in current state |
| `USER_NOT_FOUND` | Specified user does not exist |
| `INTERNAL_ERROR` | System error occurred |

---

## Data Models

### UserDTO
```json
{
  "id": 1,
  "username": "johndoe",
  "email": "john.doe@example.com",
  "firstName": "John",
  "lastName": "Doe",
  "accountLevel": "GOLD",
  "status": "ACTIVE",
  "kycStatus": "VERIFIED",
  "createdAt": "2025-01-15T10:30:00",
  "updatedAt": "2025-01-15T10:30:00"
}
```

### UserSearchResponse
```json
{
  "users": [UserDTO],
  "totalCount": 150,
  "page": 1,
  "limit": 20,
  "hasMore": true
}
```

### Enums

#### AccountLevel
- `BRONZE`
- `GOLD`
- `PLATINUM`
- `DIAMOND`

#### UserStatus
- `ACTIVE`
- `INACTIVE`
- `SUSPENDED`
- `DEACTIVATED`

#### KycStatus
- `PENDING`
- `VERIFIED`
- `REJECTED`

---

## Authentication & Authorization

### Required Roles
All endpoints require authentication with one of the following roles:
- `ADMIN`
- `EMPLOYEE`

### Authentication Methods
The API supports the following authentication methods:
- **Bearer Token**: Include `Authorization: Bearer <token>` header
- **Session-based**: Cookie-based session authentication

### Authorization Headers
```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

---

## Rate Limiting

The API may implement rate limiting based on:
- User role
- IP address
- Number of requests per minute

Rate limit headers will be included in responses:
```
X-RateLimit-Limit: 100
X-RateLimit-Remaining: 95
X-RateLimit-Reset: 1642099200
```

---

## Troubleshooting

### Common Issues

1. **401 Unauthorized**
   - Ensure you're authenticated with a valid token/session
   - Check token expiration

2. **403 Forbidden**
   - Verify your user has ADMIN or EMPLOYEE role
   - Check role assignments

3. **400 Bad Request**
   - Validate request body format
   - Check parameter constraints
   - Verify enum values are correct

4. **404 Not Found**
   - Confirm the username exists
   - Check URL path

5. **409 Conflict**
   - User may already be in the target state
   - Check current user status before operations

### Debug Information
- Error messages are available in response body `message` field
- Fallback error messages are in `X-Error-Message` header
- Check server logs for detailed error information

---

## Examples

### JavaScript Integration
```javascript
class UserManagementAPI {
  constructor(baseUrl, token) {
    this.baseUrl = baseUrl;
    this.token = token;
  }

  async searchUsers(filters = {}) {
    const params = new URLSearchParams();
    Object.entries(filters).forEach(([key, value]) => {
      if (value !== undefined && value !== null) {
        params.append(key, value);
      }
    });

    const response = await fetch(`${this.baseUrl}/admin/manage/users/search?${params}`, {
      headers: {
        'Authorization': `Bearer ${this.token}`
      }
    });

    if (!response.ok) {
      const error = await response.json();
      throw new Error(error.message || 'Search failed');
    }

    return response.json();
  }

  async suspendUser(username, reason) {
    const response = await fetch(`${this.baseUrl}/admin/manage/users/${username}/suspend`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${this.token}`
      },
      body: JSON.stringify({ reason })
    });

    if (!response.ok) {
      const error = await response.json();
      throw new Error(error.message || 'Suspension failed');
    }

    return response.json();
  }
}
```

### Usage Example
```javascript
const api = new UserManagementAPI('https://localhost:8080/admin', 'your-jwt-token');

// Search for active gold users
const result = await api.searchUsers({
  status: 'ACTIVE',
  accountLevel: 'GOLD',
  limit: 50
});

// Suspend a user
await api.suspendUser('johndoe', 'Suspicious activity detected');
```

---

## Changelog

### Version 1.0
- Initial API implementation
- Basic CRUD operations
- User search with pagination
- Account suspension/reactivation
- Comprehensive error handling
- Role-based authorization
