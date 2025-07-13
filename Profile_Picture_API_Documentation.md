# Profile Picture API Documentation

## Overview
This API provides endpoints for managing user profile pictures (avatars) in the banking platform. All endpoints require authentication and users can only manage their own profile pictures.

## Base URL
```
/api/user/profile/avatar
```

## Authentication
All endpoints require a valid JWT token in the Authorization header:
```
Authorization: Bearer <jwt-token>
```

## Endpoints

### 1. Upload Profile Picture

**Endpoint:** `POST /api/user/profile/avatar`

**Description:** Upload a new profile picture for the authenticated user. This will replace any existing profile picture.

**Content-Type:** `multipart/form-data`

**Request Parameters:**
- `avatar` (file, required): The image file to upload
  - Supported formats: JPG, JPEG, PNG
  - Maximum file size: Depends on server configuration
  - Recommended size: 512x512 pixels or smaller

**Request Example (JavaScript):**
```javascript
const formData = new FormData();
formData.append('avatar', fileInput.files[0]);

fetch('/api/user/profile/avatar', {
  method: 'POST',
  headers: {
    'Authorization': `Bearer ${token}`
  },
  body: formData
})
.then(response => response.json())
.then(data => console.log(data));
```

**Request Example (cURL):**
```bash
curl -X POST \
  -H "Authorization: Bearer <jwt-token>" \
  -F "avatar=@/path/to/image.jpg" \
  http://localhost:8080/api/user/profile/avatar
```

**Success Response (200 OK):**
```json
{
  "success": true,
  "message": "Profile picture uploaded successfully",
  "avatarUrl": "/avatars/username_12345678-1234-5678-9012-123456789012.jpg",
  "timestamp": 1721721600000
}
```

**Error Responses:**

**400 Bad Request - Missing file:**
```json
{
  "success": false,
  "error": "Avatar image is required",
  "timestamp": 1721721600000
}
```

**400 Bad Request - Invalid file type:**
```json
{
  "success": false,
  "error": "Only JPG, JPEG, and PNG files are allowed",
  "timestamp": 1721721600000
}
```

**500 Internal Server Error:**
```json
{
  "success": false,
  "error": "Failed to upload profile picture: <error details>",
  "timestamp": 1721721600000
}
```

---

### 2. Get Profile Picture

**Endpoint:** `GET /api/user/profile/avatar`

**Description:** Retrieve the URL of the current user's profile picture.

**Request Example (JavaScript):**
```javascript
fetch('/api/user/profile/avatar', {
  method: 'GET',
  headers: {
    'Authorization': `Bearer ${token}`,
    'Content-Type': 'application/json'
  }
})
.then(response => response.json())
.then(data => {
  if (data.success) {
    console.log('Avatar URL:', data.avatarUrl);
  }
});
```

**Request Example (cURL):**
```bash
curl -X GET \
  -H "Authorization: Bearer <jwt-token>" \
  -H "Content-Type: application/json" \
  http://localhost:8080/api/user/profile/avatar
```

**Success Response (200 OK):**
```json
{
  "success": true,
  "avatarUrl": "/avatars/username_12345678-1234-5678-9012-123456789012.jpg",
  "timestamp": 1721721600000
}
```

**Error Responses:**

**404 Not Found - No profile picture:**
```json
{
  "success": false,
  "error": "No profile picture found",
  "timestamp": 1721721600000
}
```

**500 Internal Server Error:**
```json
{
  "success": false,
  "error": "Failed to retrieve profile picture: <error details>",
  "timestamp": 1721721600000
}
```

---

### 3. Delete Profile Picture

**Endpoint:** `DELETE /api/user/profile/avatar`

**Description:** Delete the current user's profile picture from both the database and file system.

**Request Example (JavaScript):**
```javascript
fetch('/api/user/profile/avatar', {
  method: 'DELETE',
  headers: {
    'Authorization': `Bearer ${token}`,
    'Content-Type': 'application/json'
  }
})
.then(response => response.json())
.then(data => {
  if (data.success) {
    console.log('Profile picture deleted successfully');
  }
});
```

**Request Example (cURL):**
```bash
curl -X DELETE \
  -H "Authorization: Bearer <jwt-token>" \
  -H "Content-Type: application/json" \
  http://localhost:8080/api/user/profile/avatar
```

**Success Response (200 OK):**
```json
{
  "success": true,
  "message": "Profile picture deleted successfully",
  "timestamp": 1721721600000
}
```

**Error Response:**

**500 Internal Server Error:**
```json
{
  "success": false,
  "error": "Failed to delete profile picture: <error details>",
  "timestamp": 1721721600000
}
```

---

## Frontend Integration Examples

### React Component Example

```jsx
import React, { useState, useEffect } from 'react';

const ProfilePicture = () => {
  const [avatarUrl, setAvatarUrl] = useState(null);
  const [uploading, setUploading] = useState(false);
  const [error, setError] = useState(null);

  const token = localStorage.getItem('authToken'); // Adjust based on your auth implementation

  // Load current avatar on component mount
  useEffect(() => {
    loadAvatar();
  }, []);

  const loadAvatar = async () => {
    try {
      const response = await fetch('/api/user/profile/avatar', {
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json'
        }
      });
      
      const data = await response.json();
      if (data.success) {
        setAvatarUrl(data.avatarUrl);
      }
    } catch (err) {
      console.error('Failed to load avatar:', err);
    }
  };

  const uploadAvatar = async (file) => {
    if (!file) return;

    // Validate file type
    const allowedTypes = ['image/jpeg', 'image/jpg', 'image/png'];
    if (!allowedTypes.includes(file.type)) {
      setError('Only JPG, JPEG, and PNG files are allowed');
      return;
    }

    setUploading(true);
    setError(null);

    try {
      const formData = new FormData();
      formData.append('avatar', file);

      const response = await fetch('/api/user/profile/avatar', {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${token}`
        },
        body: formData
      });

      const data = await response.json();
      
      if (data.success) {
        setAvatarUrl(data.avatarUrl);
        setError(null);
      } else {
        setError(data.error);
      }
    } catch (err) {
      setError('Failed to upload avatar');
      console.error('Upload error:', err);
    } finally {
      setUploading(false);
    }
  };

  const deleteAvatar = async () => {
    if (!confirm('Are you sure you want to delete your profile picture?')) {
      return;
    }

    try {
      const response = await fetch('/api/user/profile/avatar', {
        method: 'DELETE',
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json'
        }
      });

      const data = await response.json();
      
      if (data.success) {
        setAvatarUrl(null);
        setError(null);
      } else {
        setError(data.error);
      }
    } catch (err) {
      setError('Failed to delete avatar');
      console.error('Delete error:', err);
    }
  };

  const handleFileChange = (event) => {
    const file = event.target.files[0];
    if (file) {
      uploadAvatar(file);
    }
  };

  return (
    <div className="profile-picture">
      <div className="avatar-container">
        {avatarUrl ? (
          <img 
            src={avatarUrl} 
            alt="Profile" 
            className="avatar-image"
            style={{ width: '100px', height: '100px', borderRadius: '50%' }}
          />
        ) : (
          <div className="avatar-placeholder">
            <span>No Image</span>
          </div>
        )}
      </div>
      
      <div className="avatar-controls">
        <label htmlFor="avatar-upload" className="upload-btn">
          {uploading ? 'Uploading...' : 'Upload New'}
        </label>
        <input
          id="avatar-upload"
          type="file"
          accept="image/jpeg,image/jpg,image/png"
          onChange={handleFileChange}
          disabled={uploading}
          style={{ display: 'none' }}
        />
        
        {avatarUrl && (
          <button onClick={deleteAvatar} className="delete-btn">
            Delete
          </button>
        )}
      </div>
      
      {error && (
        <div className="error-message" style={{ color: 'red' }}>
          {error}
        </div>
      )}
    </div>
  );
};

export default ProfilePicture;
```

### Vue.js Component Example

```vue
<template>
  <div class="profile-picture">
    <div class="avatar-container">
      <img 
        v-if="avatarUrl" 
        :src="avatarUrl" 
        alt="Profile" 
        class="avatar-image"
        style="width: 100px; height: 100px; border-radius: 50%;"
      />
      <div v-else class="avatar-placeholder">
        <span>No Image</span>
      </div>
    </div>
    
    <div class="avatar-controls">
      <label for="avatar-upload" class="upload-btn">
        {{ uploading ? 'Uploading...' : 'Upload New' }}
      </label>
      <input
        id="avatar-upload"
        type="file"
        accept="image/jpeg,image/jpg,image/png"
        @change="handleFileChange"
        :disabled="uploading"
        style="display: none;"
      />
      
      <button v-if="avatarUrl" @click="deleteAvatar" class="delete-btn">
        Delete
      </button>
    </div>
    
    <div v-if="error" class="error-message" style="color: red;">
      {{ error }}
    </div>
  </div>
</template>

<script>
export default {
  name: 'ProfilePicture',
  data() {
    return {
      avatarUrl: null,
      uploading: false,
      error: null
    }
  },
  mounted() {
    this.loadAvatar();
  },
  methods: {
    async loadAvatar() {
      try {
        const token = localStorage.getItem('authToken');
        const response = await fetch('/api/user/profile/avatar', {
          headers: {
            'Authorization': `Bearer ${token}`,
            'Content-Type': 'application/json'
          }
        });
        
        const data = await response.json();
        if (data.success) {
          this.avatarUrl = data.avatarUrl;
        }
      } catch (err) {
        console.error('Failed to load avatar:', err);
      }
    },
    
    async uploadAvatar(file) {
      if (!file) return;

      const allowedTypes = ['image/jpeg', 'image/jpg', 'image/png'];
      if (!allowedTypes.includes(file.type)) {
        this.error = 'Only JPG, JPEG, and PNG files are allowed';
        return;
      }

      this.uploading = true;
      this.error = null;

      try {
        const formData = new FormData();
        formData.append('avatar', file);
        
        const token = localStorage.getItem('authToken');
        const response = await fetch('/api/user/profile/avatar', {
          method: 'POST',
          headers: {
            'Authorization': `Bearer ${token}`
          },
          body: formData
        });

        const data = await response.json();
        
        if (data.success) {
          this.avatarUrl = data.avatarUrl;
          this.error = null;
        } else {
          this.error = data.error;
        }
      } catch (err) {
        this.error = 'Failed to upload avatar';
        console.error('Upload error:', err);
      } finally {
        this.uploading = false;
      }
    },
    
    async deleteAvatar() {
      if (!confirm('Are you sure you want to delete your profile picture?')) {
        return;
      }

      try {
        const token = localStorage.getItem('authToken');
        const response = await fetch('/api/user/profile/avatar', {
          method: 'DELETE',
          headers: {
            'Authorization': `Bearer ${token}`,
            'Content-Type': 'application/json'
          }
        });

        const data = await response.json();
        
        if (data.success) {
          this.avatarUrl = null;
          this.error = null;
        } else {
          this.error = data.error;
        }
      } catch (err) {
        this.error = 'Failed to delete avatar';
        console.error('Delete error:', err);
      }
    },
    
    handleFileChange(event) {
      const file = event.target.files[0];
      if (file) {
        this.uploadAvatar(file);
      }
    }
  }
}
</script>
```

## Error Handling

### Common HTTP Status Codes
- **200 OK**: Request successful
- **400 Bad Request**: Invalid request (missing file, wrong file type, etc.)
- **401 Unauthorized**: Invalid or missing authentication token
- **403 Forbidden**: User doesn't have permission
- **404 Not Found**: Profile picture not found
- **500 Internal Server Error**: Server-side error

### Best Practices for Frontend Implementation

1. **File Validation**: Always validate file type and size on the frontend before uploading
2. **Loading States**: Show loading indicators during upload/delete operations
3. **Error Handling**: Display user-friendly error messages
4. **Preview**: Show image preview before uploading
5. **Confirmation**: Ask for confirmation before deleting profile pictures
6. **Caching**: Consider implementing proper caching for avatar URLs
7. **Responsive Design**: Ensure avatar displays work on different screen sizes

## File Storage Details

- **Storage Location**: `{user.home}/banking-uploads/avatars/`
- **File Naming**: `{username}_{UUID}.{extension}`
- **Supported Formats**: JPG, JPEG, PNG
- **Auto Cleanup**: Old avatars are automatically deleted when new ones are uploaded

## Security Considerations

- Users can only access their own profile pictures
- File type validation prevents malicious file uploads
- Unique filenames prevent file conflicts
- JWT authentication required for all operations
