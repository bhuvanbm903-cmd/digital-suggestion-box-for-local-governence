# SuggestBox — Firestore Data Model

## Collections

### 1. `/users/{uid}`
Stores private citizen/official metadata (accessible **only** by the owning user via security rules).
```json
{
  "displayName": "Aarav Kulkarni",
  "wardId": "ward-14",
  "wardName": "Ward 14 • Indiranagar",
  "role": "CITIZEN",
  "department": null,
  "isAnonymous": false,
  "fcmToken": "cXa9..."
}
```

### 2. `/suggestions/{suggestionId}`
Stores public and official suggestion data. Anonymous submissions store `"authorId": "anonymous"` and `"authorDisplayName": "Anonymous Citizen"` so identity is never leaked.
```json
{
  "trackingId": "SBX-W14-2041",
  "kind": "ISSUE",
  "title": "Repair sunken stormwater drain grate on 100ft Road",
  "description": "The iron stormwater cover near the pedestrian crossing...",
  "category": "ROADS",
  "status": "IN_PROGRESS",
  "priority": "URGENT",
  "wardId": "ward-14",
  "wardName": "Ward 14 • Indiranagar",
  "latitude": 12.9789,
  "longitude": 77.6412,
  "addressLabel": "100ft Road, near Metro Pillar 42",
  "mediaUrls": ["https://firebasestorage.../media_0.jpg"],
  "afterPhotoUrl": null,
  "isAnonymous": false,
  "authorId": "uid_12345",
  "authorDisplayName": "Aarav Kulkarni",
  "assignedDepartment": "Public Works & Roads",
  "assignedOfficerName": "Meera Deshmukh (AEE)",
  "upvoteCount": 47,
  "upvoterIds": ["uid_12345"],
  "officialResponse": "Safety cones placed; precast grate scheduled.",
  "duplicateOfTrackingId": null,
  "createdAtMillis": 1762900000000,
  "slaDeadlineMillis": 1762986400000
}
```

### 3. `/announcements/{announcementId}`
Ward-targeted notices published by local governance officials.
```json
{
  "wardId": "ward-14",
  "title": "Scheduled Water Pipeline Maintenance",
  "body": "Potable water supply will resume by 5:00 PM today.",
  "department": "Water Supply & Sewerage Board",
  "publishedAtMillis": 1762940000000,
  "isPinned": true
}
```

### 4. `/wards/{wardId}`
Local body metadata and centroid coordinates.
```json
{
  "name": "Ward 14 • Indiranagar",
  "municipalityName": "Bengaluru City Municipal Council",
  "centerLat": 12.9784,
  "centerLng": 77.6408
}
```
