# 🔗 SkyBox Integration Architecture

## Overview

This document explains how the frontend (React) and backend (Spring Boot) work together to provide a seamless cloud storage experience.

---

## 🌐 System Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│                         User's Browser                          │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │                 React Frontend (Port 5173)               │  │
│  │  • Landing Page  • Dashboard  • Subscription             │  │
│  └────────────────┬─────────────────────────────────────────┘  │
└─────────────────┬─┴────────────────────────────────────────────┘
                  │
                  │ HTTPS/REST API
                  │ JWT Bearer Token
                  │
┌─────────────────▼──────────────────────────────────────────────┐
│           Spring Boot Backend (Port 8080/api/v1.0)             │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │  Controllers → Services → Repositories → Database/Storage│  │
│  └──────────────────────────────────────────────────────────┘  │
└─────────┬───────────────┬────────────────┬─────────────────────┘
          │               │                │
          ▼               ▼                ▼
    ┌─────────┐    ┌──────────┐    ┌────────────┐
    │  Clerk  │    │ MongoDB  │    │ Cloudflare │
    │  Auth   │    │  Atlas   │    │     R2     │
    └─────────┘    └──────────┘    └────────────┘
          │
          ▼
    ┌──────────┐
    │ Razorpay │
    └──────────┘
```

---

## 🔐 Authentication Flow

### How Frontend and Backend Handle Authentication

```
1. User Sign-In/Sign-Up
   ├─→ Frontend: User clicks "Sign In" button
   ├─→ Frontend: Opens Clerk authentication modal
   ├─→ Clerk: User authenticates with email/password
   ├─→ Clerk: Issues JWT token
   └─→ Frontend: Stores token (managed by Clerk SDK)

2. Protected API Requests
   ├─→ Frontend: Gets token using getToken()
   ├─→ Frontend: Sends request with Authorization header
   ├─→ Backend: ClerkJwtAuthFilter intercepts request
   ├─→ Backend: Validates JWT against Clerk's JWKS
   ├─→ Backend: Extracts clerkId from token
   ├─→ Backend: Sets SecurityContext
   └─→ Backend: Processes request

3. User Registration/Sync
   ├─→ Frontend: After authentication, calls POST /register
   ├─→ Backend: Creates Profile document in MongoDB
   ├─→ Backend: Creates UserCredits with 1GB storage
   └─→ Frontend: Redirects to dashboard
```

**Code Flow:**

**Frontend (Dashboard.jsx):**
```javascript
// 1. Get authenticated user from Clerk
const { getToken } = useAuth();
const { user } = useUser();

// 2. Register user with backend
useEffect(() => {
    const registerUser = async () => {
        const token = await getToken(); // Get JWT from Clerk
        
        await axios.post(apiEndpoints.REGISTER, {
            clerkId: user.id,
            email: user.primaryEmailAddress?.emailAddress,
            name: user.fullName
        }, {
            headers: { 'Authorization': `Bearer ${token}` }
        });
    };
    
    registerUser();
}, [user, getToken]);
```

**Backend (ClerkJwtAuthFilter.java):**
```java
// 1. Extract token from Authorization header
String token = request.getHeader("Authorization").substring(7);

// 2. Validate JWT using Clerk's JWKS
Claims claims = validateToken(token);

// 3. Extract clerkId
String clerkId = claims.getSubject();

// 4. Set authentication in SecurityContext
UsernamePasswordAuthenticationToken auth = 
    new UsernamePasswordAuthenticationToken(clerkId, null, Collections.emptyList());
SecurityContextHolder.getContext().setAuthentication(auth);

// 5. Continue with request
filterChain.doFilter(request, response);
```

**Backend (ProfileController.java):**
```java
@PostMapping("/register")
public ResponseEntity<ProfileDTO> registerUser(@RequestBody ProfileDTO profileDTO) {
    // Create or update user profile
    ProfileDTO created = profileService.createProfile(profileDTO);
    
    // Create initial user credits (1GB storage)
    userCreditsService.createInitialCredits(profileDTO.getClerkId());
    
    return ResponseEntity.ok(created);
}
```

---

## 📤 File Upload Flow

### Complete Request-Response Cycle

```
┌──────────────┐
│   Frontend   │
│  Dashboard   │
└──────┬───────┘
       │
       │ 1. User selects file
       ▼
┌────────────────────────┐
│  handleFileUpload()    │
│  • Validate file size  │
│  • Create FormData     │
│  • Get JWT token       │
└──────┬─────────────────┘
       │
       │ 2. POST /files/upload
       │    Content-Type: multipart/form-data
       │    Authorization: Bearer <token>
       ▼
┌────────────────────────────────┐
│  Backend: FileController       │
│  @PostMapping("/files/upload") │
└──────┬─────────────────────────┘
       │
       │ 3. Extract clerkId from SecurityContext
       ▼
┌────────────────────────────┐
│  Check storage limit       │
│  UserCreditsService        │
└──────┬─────────────────────┘
       │
       │ 4. Storage available?
       ├─── No ──→ Return 403 Forbidden
       │
       └─── Yes
            │
            │ 5. Upload to Cloudflare R2
            ▼
       ┌────────────────────────┐
       │ CloudflareR2Service    │
       │ • Generate unique key  │
       │ • Upload to R2 bucket  │
       │ • Return fileKey       │
       └──────┬─────────────────┘
              │
              │ 6. Save metadata to MongoDB
              ▼
       ┌────────────────────────────┐
       │  FileMetadata document     │
       │  • fileName, fileKey       │
       │  • fileSize, contentType   │
       │  • clerkId, uploadedAt     │
       └──────┬─────────────────────┘
              │
              │ 7. Update storage usage
              ▼
       ┌────────────────────────────┐
       │  UserCredits.storageUsed   │
       │  += fileSize (in MB)       │
       └──────┬─────────────────────┘
              │
              │ 8. Return success response
              ▼
       ┌────────────────────────────┐
       │  Frontend receives         │
       │  • fileId, fileName        │
       │  • uploadedAt              │
       └──────┬─────────────────────┘
              │
              │ 9. Refresh UI
              ▼
       ┌────────────────────────────┐
       │  • Refresh file list       │
       │  • Update storage display  │
       │  • Show success message    │
       └────────────────────────────┘
```

**Frontend Code (Dashboard.jsx):**
```javascript
const handleFileUpload = async (file) => {
    // 1. Validate file
    if (file.size > 25 * 1024 * 1024) {
        setError('File too large');
        return;
    }
    
    // 2. Prepare FormData
    const formData = new FormData();
    formData.append('file', file);
    formData.append('isPublic', false);
    
    // 3. Get JWT token
    const token = await getToken();
    
    // 4. Upload file
    try {
        const response = await axios.post(
            apiEndpoints.UPLOAD_FILE,
            formData,
            {
                headers: {
                    'Authorization': `Bearer ${token}`,
                    'Content-Type': 'multipart/form-data'
                },
                onUploadProgress: (progressEvent) => {
                    const percent = Math.round(
                        (progressEvent.loaded * 100) / progressEvent.total
                    );
                    setProgress(percent);
                }
            }
        );
        
        // 5. Refresh data
        fetchUserCredits(); // Update storage usage
        fetchFiles();       // Refresh file list
        setSuccess('File uploaded successfully!');
        
    } catch (error) {
        setError('Upload failed: ' + error.message);
    }
};
```

**Backend Code (FileController.java):**
```java
@PostMapping("/files/upload")
public ResponseEntity<FileMetadataDTO> uploadFile(
        @RequestParam("file") MultipartFile file,
        @RequestParam(defaultValue = "false") Boolean isPublic) {
    
    // 1. Get authenticated user
    String clerkId = SecurityContextHolder.getContext()
        .getAuthentication().getName();
    
    // 2. Check storage limit
    UserCredits credits = userCreditsService.getUserCredits();
    long requiredSpace = file.getSize() / (1024 * 1024); // Convert to MB
    
    if (credits.getStorageUsed() + requiredSpace > credits.getStorageLimit()) {
        throw new StorageLimitExceededException("Storage limit exceeded");
    }
    
    // 3. Upload to R2
    String fileKey = cloudflareR2Service.uploadFile(
        file, 
        "private/" + clerkId
    );
    
    // 4. Save metadata
    FileMetadata metadata = FileMetadata.builder()
        .fileName(file.getOriginalFilename())
        .fileKey(fileKey)
        .fileSize(file.getSize())
        .contentType(file.getContentType())
        .clerkId(clerkId)
        .isPublic(isPublic)
        .uploadedAt(LocalDateTime.now())
        .build();
    
    FileMetadata saved = fileMetadataRepository.save(metadata);
    
    // 5. Update storage usage
    userCreditsService.updateStorageUsage(clerkId, requiredSpace);
    
    // 6. Return response
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(convertToDTO(saved));
}
```

---

## 📥 File Download Flow

```
Frontend                    Backend                     Cloudflare R2
   │                           │                              │
   │ 1. Click download         │                              │
   │──────────────────────────>│                              │
   │   GET /files/download/:id │                              │
   │   Authorization: Bearer   │                              │
   │                           │                              │
   │                           │ 2. Verify ownership          │
   │                           │    (check clerkId matches)   │
   │                           │                              │
   │                           │ 3. Get file metadata         │
   │                           │    from MongoDB              │
   │                           │                              │
   │                           │ 4. Request file from R2      │
   │                           │─────────────────────────────>│
   │                           │                              │
   │                           │ 5. Return file bytes         │
   │                           │<─────────────────────────────│
   │                           │                              │
   │ 6. Receive file bytes     │                              │
   │<──────────────────────────│                              │
   │   Content-Type: file type │                              │
   │   Content-Disposition:    │                              │
   │   attachment; filename    │                              │
   │                           │                              │
   │ 7. Browser downloads file │                              │
   │                           │                              │
```

**Frontend Code:**
```javascript
const handleDownload = async (fileId, fileName) => {
    try {
        const token = await getToken();
        
        const response = await axios.get(
            `${apiEndpoints.DOWNLOAD_FILE}/${fileId}`,
            {
                headers: { 'Authorization': `Bearer ${token}` },
                responseType: 'blob' // Important for binary data
            }
        );
        
        // Create download link
        const url = window.URL.createObjectURL(new Blob([response.data]));
        const link = document.createElement('a');
        link.href = url;
        link.setAttribute('download', fileName);
        document.body.appendChild(link);
        link.click();
        link.remove();
        window.URL.revokeObjectURL(url);
        
    } catch (error) {
        setError('Download failed');
    }
};
```

**Backend Code:**
```java
@GetMapping("/files/download/{fileId}")
public ResponseEntity<byte[]> downloadFile(@PathVariable String fileId) {
    
    // 1. Get file metadata
    FileMetadata metadata = fileMetadataRepository.findById(fileId)
        .orElseThrow(() -> new FileNotFoundException("File not found"));
    
    // 2. Verify ownership
    String clerkId = SecurityContextHolder.getContext()
        .getAuthentication().getName();
    
    if (!metadata.getClerkId().equals(clerkId) && !metadata.getIsPublic()) {
        throw new UnauthorizedException("Access denied");
    }
    
    // 3. Download from R2
    byte[] fileData = cloudflareR2Service.downloadFile(metadata.getFileKey());
    
    // 4. Return file
    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType(metadata.getContentType()))
        .header(HttpHeaders.CONTENT_DISPOSITION, 
            "attachment; filename=\"" + metadata.getFileName() + "\"")
        .body(fileData);
}
```

---

## 💳 Payment Processing Flow

```
┌──────────────┐
│  Frontend    │
│ Subscription │
└──────┬───────┘
       │
       │ 1. User clicks "Upgrade to Premium"
       ▼
┌────────────────────────────┐
│  POST /payments/create-order
│  {                         │
│    planId: "premium",      │
│    amount: 29900,          │
│    currency: "INR"         │
│  }                         │
└──────┬─────────────────────┘
       │
       │ 2. Backend creates Razorpay order
       ▼
┌────────────────────────────┐
│  Razorpay API              │
│  order_xxxxx created       │
└──────┬─────────────────────┘
       │
       │ 3. Save PaymentTransaction (status: created)
       │    in MongoDB
       ▼
┌────────────────────────────┐
│  Backend returns           │
│  { orderId: "order_xxx" }  │
└──────┬─────────────────────┘
       │
       │ 4. Open Razorpay checkout modal
       ▼
┌────────────────────────────┐
│  Razorpay.open({           │
│    key: razorpay_key,      │
│    order_id: order_xxx,    │
│    handler: onSuccess      │
│  })                        │
└──────┬─────────────────────┘
       │
       │ 5. User completes payment
       ▼
┌────────────────────────────┐
│  Razorpay callback         │
│  {                         │
│    razorpay_order_id,      │
│    razorpay_payment_id,    │
│    razorpay_signature      │
│  }                         │
└──────┬─────────────────────┘
       │
       │ 6. Verify payment
       ▼
┌────────────────────────────┐
│  POST /payments/verify     │
│  Send payment details      │
└──────┬─────────────────────┘
       │
       │ 7. Backend verifies signature
       │    HMAC(order_id|payment_id, secret)
       ▼
┌────────────────────────────┐
│  Signature valid?          │
├─── Yes ──→ Update payment  │
│            Update storage  │
│            Return success  │
│                            │
└─── No ───→ Return failure  │
            Log fraud attempt│
```

**Frontend Code (Subscription.jsx):**
```javascript
const handlePurchase = async (plan) => {
    try {
        // 1. Create order on backend
        const token = await getToken();
        const orderResponse = await axios.post(
            apiEndpoints.CREATE_ORDER,
            {
                planId: plan.id,
                amount: plan.price * 100, // Paise
                currency: "INR",
                storageInMB: plan.storageInMB
            },
            { headers: { 'Authorization': `Bearer ${token}` } }
        );

        // 2. Open Razorpay checkout
        const options = {
            key: import.meta.env.VITE_RAZORPAY_KEY,
            amount: plan.price * 100,
            currency: "INR",
            order_id: orderResponse.data.orderId,
            
            handler: async function (response) {
                // 3. Verify payment on backend
                try {
                    await axios.post(
                        apiEndpoints.VERIFY_PAYMENT,
                        {
                            razorpay_order_id: response.razorpay_order_id,
                            razorpay_payment_id: response.razorpay_payment_id,
                            razorpay_signature: response.razorpay_signature,
                            planId: plan.id
                        },
                        { headers: { 'Authorization': `Bearer ${token}` } }
                    );

                    // 4. Refresh credits
                    fetchUserCredits();
                    setSuccess('Payment successful!');
                } catch (error) {
                    setError('Payment verification failed');
                }
            },
            
            prefill: {
                name: user.fullName,
                email: user.primaryEmailAddress
            }
        };

        const razorpay = new window.Razorpay(options);
        razorpay.open();

    } catch (error) {
        setError('Payment initiation failed');
    }
};
```

**Backend Code (PaymentController.java):**
```java
// Step 1: Create Order
@PostMapping("/payments/create-order")
public ResponseEntity<PaymentOrderDTO> createOrder(
        @RequestBody PaymentOrderDTO orderRequest) {
    
    String clerkId = SecurityContextHolder.getContext()
        .getAuthentication().getName();
    
    // Create Razorpay order
    JSONObject orderRequest = new JSONObject();
    orderRequest.put("amount", orderRequest.getAmount());
    orderRequest.put("currency", "INR");
    
    Order order = razorpayClient.orders.create(orderRequest);
    
    // Save transaction
    PaymentTransaction transaction = PaymentTransaction.builder()
        .clerkId(clerkId)
        .razorpayOrderId(order.get("id"))
        .planId(orderRequest.getPlanId())
        .amount(orderRequest.getAmount())
        .status("created")
        .createdAt(LocalDateTime.now())
        .build();
    
    paymentTransactionRepository.save(transaction);
    
    return ResponseEntity.ok(PaymentOrderDTO.builder()
        .orderId(order.get("id"))
        .amount(orderRequest.getAmount())
        .currency("INR")
        .build());
}

// Step 2: Verify Payment
@PostMapping("/payments/verify")
public ResponseEntity<Map<String, Object>> verifyPayment(
        @RequestBody PaymentVerificationDTO verificationDTO) {
    
    String clerkId = SecurityContextHolder.getContext()
        .getAuthentication().getName();
    
    try {
        // Verify signature
        String payload = verificationDTO.getRazorpayOrderId() + "|" + 
                        verificationDTO.getRazorpayPaymentId();
        String expectedSignature = HmacUtils.hmacSha256Hex(
            razorpaySecret, payload
        );
        
        if (!expectedSignature.equals(verificationDTO.getRazorpaySignature())) {
            throw new SignatureException("Invalid signature");
        }
        
        // Update transaction
        PaymentTransaction transaction = paymentTransactionRepository
            .findByRazorpayOrderId(verificationDTO.getRazorpayOrderId())
            .orElseThrow();
        
        transaction.setRazorpayPaymentId(verificationDTO.getRazorpayPaymentId());
        transaction.setRazorpaySignature(verificationDTO.getRazorpaySignature());
        transaction.setStatus("paid");
        transaction.setPaidAt(LocalDateTime.now());
        paymentTransactionRepository.save(transaction);
        
        // Update user storage
        UserCredits credits = userCreditsService.getUserCredits();
        credits.setCurrentPlan(verificationDTO.getPlanId());
        credits.setStorageLimit(getStorageLimitForPlan(verificationDTO.getPlanId()));
        userCreditsRepository.save(credits);
        
        return ResponseEntity.ok(Map.of("success", true));
        
    } catch (Exception e) {
        return ResponseEntity.ok(Map.of("success", false));
    }
}
```

---

## 🔄 Real-Time Data Synchronization

### UserCreditsContext Integration

**How Frontend Keeps Storage Data in Sync:**

```
┌─────────────────────────────────────────────────────┐
│  UserCreditsContext (Global State)                  │
│  • credits                                          │
│  • setCredits()                                     │
│  • fetchUserCredits()                               │
└───────────────┬─────────────────────────────────────┘
                │
                │ Wrapped around entire app
                │
    ┌───────────┴───────────┬─────────────────┐
    │                       │                 │
    ▼                       ▼                 ▼
┌────────┐            ┌──────────┐      ┌──────────┐
│Landing │            │Dashboard │      │Subscription│
└────────┘            └─────┬────┘      └─────┬────┘
                            │                  │
                            │ After actions:   │
                            │ • Upload file    │
                            │ • Delete file    │
                            │ • Pay subscription│
                            │                  │
                            ▼                  ▼
                    ┌───────────────────────────┐
                    │  fetchUserCredits()       │
                    │  GET /users/credits       │
                    └──────────┬────────────────┘
                               │
                               │ Update context
                               ▼
                    ┌───────────────────────────┐
                    │  setCredits(newCredits)   │
                    │  All components re-render │
                    └───────────────────────────┘
```

**Frontend Code (UserCreditsContext.jsx):**
```javascript
export const UserCreditsProvider = ({ children }) => {
    const [credits, setCredits] = useState(null);
    const { getToken, isSignedIn } = useAuth();

    const fetchUserCredits = useCallback(async () => {
        if (!isSignedIn) return;
        
        try {
            const token = await getToken();
            const response = await axios.get(
                apiEndpoints.GET_USER_CREDITS,
                { headers: { 'Authorization': `Bearer ${token}` } }
            );
            setCredits(response.data);
        } catch (error) {
            console.error("Error fetching credits:", error);
        }
    }, [getToken, isSignedIn]);

    // Auto-fetch on mount
    useEffect(() => {
        if (isSignedIn) {
            fetchUserCredits();
        }
    }, [isSignedIn, fetchUserCredits]);

    return (
        <UserCreditsContext.Provider value={{ 
            credits, 
            setCredits, 
            fetchUserCredits 
        }}>
            {children}
        </UserCreditsContext.Provider>
    );
};
```

**Usage in Components:**
```javascript
// Dashboard.jsx
const { credits, fetchUserCredits } = useContext(UserCreditsContext);

// After file upload
const handleUpload = async () => {
    await uploadFile();
    fetchUserCredits(); // Refresh storage data
};

// Display storage
<div>Storage Used: {credits.storageUsed} MB / {credits.storageLimit} MB</div>
```

---

## 🔒 Security Integration Points

### 1. JWT Token Flow

```
Frontend                     Backend                      Clerk
   │                           │                            │
   │ 1. User authenticates     │                            │
   │──────────────────────────────────────────────────────>│
   │                           │                            │
   │ 2. Receive JWT            │                            │
   │<──────────────────────────────────────────────────────│
   │                           │                            │
   │ 3. Store token (Clerk SDK)│                            │
   │                           │                            │
   │ 4. API request with token │                            │
   │──────────────────────────>│                            │
   │   Authorization: Bearer   │                            │
   │                           │                            │
   │                           │ 5. Fetch JWKS              │
   │                           │───────────────────────────>│
   │                           │                            │
   │                           │ 6. Return public keys      │
   │                           │<───────────────────────────│
   │                           │                            │
   │                           │ 7. Validate JWT signature  │
   │                           │    with public key         │
   │                           │                            │
   │ 8. Return response        │                            │
   │<──────────────────────────│                            │
```

### 2. File Access Control

**Frontend ensures user only accesses their files:**
```javascript
// List files - backend filters by clerkId automatically
const files = await axios.get(apiEndpoints.LIST_FILES, {
    headers: { 'Authorization': `Bearer ${token}` }
});
// Backend: WHERE clerkId = authenticated_user_id
```

**Backend verifies ownership:**
```java
// Before download/delete, verify file belongs to user
FileMetadata file = fileMetadataRepository.findById(fileId);
String authenticatedUser = SecurityContextHolder.getContext()
    .getAuthentication().getName();

if (!file.getClerkId().equals(authenticatedUser)) {
    throw new UnauthorizedException();
}
```

---

## 📊 Data Flow Summary

### User Registration Flow
```
React → Clerk Auth → JWT Token → POST /register → MongoDB (Profile + UserCredits)
```

### File Upload Flow
```
React → FormData → POST /files/upload → Check Storage → Cloudflare R2 → MongoDB (FileMetadata) → Update UserCredits.storageUsed
```

### File Download Flow
```
React → GET /files/download/:id → Verify Ownership → Cloudflare R2 → Return Bytes → Browser Download
```

### Payment Flow
```
React → POST /payments/create-order → Razorpay → Save Transaction → 
Razorpay Checkout → User Pays → POST /payments/verify → Verify Signature → 
Update Transaction + UserCredits → Success
```

### Credits Sync Flow
```
React (Context) → GET /users/credits → MongoDB (UserCredits) → Return Data → Update Context → Re-render Components
```

---

## 🔗 API Contract Examples

### Example 1: Upload File

**Request:**
```http
POST http://localhost:8080/api/v1.0/files/upload
Authorization: Bearer eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...
Content-Type: multipart/form-data

file: [binary data]
isPublic: false
```

**Response:**
```json
{
    "id": "64f1a2b3c4d5e6f7g8h9i0j1",
    "fileName": "document.pdf",
    "fileSize": 1048576,
    "contentType": "application/pdf",
    "uploadedAt": "2024-10-26T10:30:00Z"
}
```

### Example 2: Get User Credits

**Request:**
```http
GET http://localhost:8080/api/v1.0/users/credits
Authorization: Bearer eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...
```

**Response:**
```json
{
    "credits": 750,
    "storageUsed": 256,
    "storageLimit": 1024,
    "currentPlan": "basic",
    "storagePercentage": 25.0
}
```

### Example 3: Create Payment Order

**Request:**
```http
POST http://localhost:8080/api/v1.0/payments/create-order
Authorization: Bearer eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...
Content-Type: application/json

{
    "planId": "premium",
    "amount": 29900,
    "currency": "INR",
    "storageInMB": 5120
}
```

**Response:**
```json
{
    "orderId": "order_MgZWN1xKLpHKmf",
    "amount": 29900,
    "currency": "INR"
}
```

---

## 🎯 Key Integration Points Summary

| Feature | Frontend Technology | Backend Technology | External Service |
|---------|-------------------|-------------------|------------------|
| **Authentication** | Clerk React SDK | ClerkJwtAuthFilter | Clerk API |
| **File Storage** | Axios (multipart) | S3 SDK | Cloudflare R2 |
| **Database** | REST API calls | Spring Data MongoDB | MongoDB Atlas |
| **Payments** | Razorpay SDK | Razorpay Java SDK | Razorpay API |
| **State Management** | React Context | SecurityContext | - |
| **Routing** | React Router | Spring MVC | - |

---

## 🚀 Deployment Integration

### Environment Variables Mapping

**Frontend (.env):**
```env
VITE_CLERK_PUBLISHABLE_KEY=pk_test_xxxxx
VITE_RAZORPAY_KEY=rzp_live_xxxxx
VITE_API_BASE_URL=https://api.skybox.com/api/v1.0
```

**Backend (application.properties):**
```properties
spring.data.mongodb.uri=${MONGODB_URI}
clerk.jwks.url=https://your-clerk-domain/.well-known/jwks.json
cloudflare.r2.account-id=${CLOUDFLARE_ACCOUNT_ID}
cloudflare.r2.access-key-id=${CLOUDFLARE_ACCESS_KEY_ID}
cloudflare.r2.secret-access-key=${CLOUDFLARE_SECRET_ACCESS_KEY}
razorpay.key.id=${RAZORPAY_KEY_ID}
razorpay.key.secret=${RAZORPAY_KEY_SECRET}
```

### CORS Configuration for Production

**Backend must allow frontend domain:**
```java
config.setAllowedOriginPatterns(List.of(
    "https://skybox-frontend.vercel.app",  // Production frontend
    "http://localhost:5173"                 // Development
));
```

---

## 📚 Common Integration Patterns

### Pattern 1: Authenticated Request
```javascript
// Frontend pattern for ALL authenticated requests
const token = await getToken();
const response = await axios.{method}(endpoint, data, {
    headers: { 'Authorization': `Bearer ${token}` }
});
```

### Pattern 2: Error Handling
```javascript
// Frontend
try {
    const response = await axios.post(endpoint, data, config);
    setSuccess('Operation successful');
} catch (error) {
    if (error.response?.status === 401) {
        setError('Session expired, please sign in again');
    } else if (error.response?.status === 403) {
        setError('You do not have permission');
    } else {
        setError('Operation failed');
    }
}
```

```java
// Backend
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ErrorResponse> handleUnauthorized(ex) {
        return ResponseEntity.status(401).body(errorResponse);
    }
}
```

### Pattern 3: Data Refresh After Mutation
```javascript
// After any operation that changes data
await uploadFile();        // Mutation
await fetchUserCredits();  // Refresh credits
await fetchFiles();        // Refresh file list
```

---

**Integration Architecture Documentation - Version 1.0**
*Last Updated: October 2025*

**For detailed implementation, see:**
- [Frontend Architecture](./FRONTEND_ARCHITECTURE.md)
- [Backend Architecture](./BACKEND_ARCHITECTURE.md)