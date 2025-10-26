# ⚙️ SkyBox Backend Architecture

## Overview

SkyBox's backend is a RESTful API built with Spring Boot 3, providing secure file storage management, user authentication, and payment processing. The system integrates with Cloudflare R2 for object storage, MongoDB for data persistence, and Razorpay for payment processing.

---

## 📁 Project Structure

```
skyboxapi/
├── src/
│   ├── main/
│   │   ├── java/com/skybox/skyboxapi/
│   │   │   ├── config/                      # Configuration classes
│   │   │   │   ├── CloudflareR2Config.java
│   │   │   │   ├── RazorpayConfig.java
│   │   │   │   └── SecurityConfig.java
│   │   │   ├── controller/                  # REST controllers
│   │   │   │   ├── FileController.java
│   │   │   │   ├── PaymentController.java
│   │   │   │   ├── ProfileController.java
│   │   │   │   └── UserCreditsController.java
│   │   │   ├── document/                    # MongoDB documents
│   │   │   │   ├── FileMetadata.java
│   │   │   │   ├── PaymentTransaction.java
│   │   │   │   ├── Profile.java
│   │   │   │   └── UserCredits.java
│   │   │   ├── dto/                         # Data Transfer Objects
│   │   │   │   ├── ProfileDTO.java
│   │   │   │   ├── StorageInfoDTO.java
│   │   │   │   └── PaymentOrderDTO.java
│   │   │   ├── repository/                  # MongoDB repositories
│   │   │   │   ├── FileMetadataRepository.java
│   │   │   │   ├── PaymentTransactionRepository.java
│   │   │   │   ├── ProfileRepository.java
│   │   │   │   └── UserCreditsRepository.java
│   │   │   ├── security/                    # Security components
│   │   │   │   ├── ClerkJwtAuthFilter.java
│   │   │   │   └── JwksProvider.java
│   │   │   ├── service/                     # Business logic
│   │   │   │   ├── CloudflareR2Service.java
│   │   │   │   ├── FileMetadataService.java
│   │   │   │   ├── PaymentService.java
│   │   │   │   ├── ProfileService.java
│   │   │   │   └── UserCreditsService.java
│   │   │   └── SkyboxapiApplication.java    # Main application class
│   │   └── resources/
│   │       ├── application.properties        # Application configuration
│   │       └── static/                       # Static resources
│   └── test/                                # Unit and integration tests
├── pom.xml                                  # Maven dependencies
├── mvnw                                     # Maven wrapper (Unix)
├── mvnw.cmd                                 # Maven wrapper (Windows)
└── .mvn/                                    # Maven wrapper files
```

---

## 🛠️ Technology Stack

### Core Dependencies

```xml
<dependencies>
    <!-- Spring Boot Starters -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-security</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-mongodb</artifactId>
    </dependency>
    
    <!-- AWS S3 SDK for Cloudflare R2 -->
    <dependency>
        <groupId>software.amazon.awssdk</groupId>
        <artifactId>s3</artifactId>
        <version>2.20.26</version>
    </dependency>
    
    <!-- JWT Processing -->
    <dependency>
        <groupId>io.jsonwebtoken</groupId>
        <artifactId>jjwt-api</artifactId>
        <version>0.11.5</version>
    </dependency>
    
    <!-- Razorpay Payment Gateway -->
    <dependency>
        <groupId>com.razorpay</groupId>
        <artifactId>razorpay-java</artifactId>
        <version>1.4.3</version>
    </dependency>
    
    <!-- Lombok -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
    </dependency>
</dependencies>
```

### Key Technologies

| Technology | Purpose | Version |
|------------|---------|---------|
| **Spring Boot** | Framework | 3.5.6 |
| **Spring Security** | Authentication & Authorization | 6.x |
| **Spring Data MongoDB** | Database access | 4.x |
| **AWS S3 SDK** | R2 object storage | 2.20.26 |
| **JJWT** | JWT processing | 0.11.5 |
| **Razorpay SDK** | Payment processing | 1.4.3 |
| **Lombok** | Code generation | Latest |

---

## 🔐 Security Architecture

### Authentication Flow

```
┌─────────────────┐
│   Frontend      │
│   (React App)   │
└────────┬────────┘
         │
         │ 1. User signs in with Clerk
         ▼
┌─────────────────┐
│   Clerk Auth    │
│   Service       │
└────────┬────────┘
         │
         │ 2. Returns JWT token
         ▼
┌─────────────────┐
│   Frontend      │
│   Stores Token  │
└────────┬────────┘
         │
         │ 3. API Request with JWT
         │    Authorization: Bearer <token>
         ▼
┌─────────────────────────────────┐
│   Spring Boot API               │
│   ClerkJwtAuthFilter            │
└────────┬────────────────────────┘
         │
         │ 4. Fetch JWKS from Clerk
         ▼
┌─────────────────┐
│   JwksProvider  │
│   Caches JWKS   │
└────────┬────────┘
         │
         │ 5. Validate JWT signature
         ▼
┌─────────────────┐
│   JWT Valid?    │
└────────┬────────┘
         │
         ├─── Yes ──→ 6. Extract clerkId
         │              Set SecurityContext
         │              Continue request
         │
         └─── No ───→ Return 401 Unauthorized
```

### ClerkJwtAuthFilter Implementation

```java
@Component
@RequiredArgsConstructor
public class ClerkJwtAuthFilter extends OncePerRequestFilter {

    private final JwksProvider jwksProvider;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        // 1. Extract JWT from Authorization header
        String token = extractToken(request);
        
        if (token != null) {
            try {
                // 2. Validate and parse JWT
                Claims claims = validateToken(token);
                
                // 3. Extract user identifier (clerkId)
                String clerkId = claims.getSubject();
                
                // 4. Create authentication object
                UsernamePasswordAuthenticationToken auth = 
                    new UsernamePasswordAuthenticationToken(
                        clerkId, null, Collections.emptyList()
                    );
                
                // 5. Set security context
                SecurityContextHolder.getContext().setAuthentication(auth);
                
            } catch (Exception e) {
                log.error("JWT validation failed", e);
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }
        }
        
        filterChain.doFilter(request, response);
    }

    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }

    private Claims validateToken(String token) throws Exception {
        // Get JWKS (JSON Web Key Set) from Clerk
        JsonNode jwks = jwksProvider.getJwks();
        
        // Parse and validate JWT
        // Implementation uses JJWT library
        return Jwts.parserBuilder()
            .setSigningKeyResolver(/* resolver using JWKS */)
            .build()
            .parseClaimsJws(token)
            .getBody();
    }
}
```

### JwksProvider (JWKS Caching)

```java
@Component
public class JwksProvider {

    @Value("${clerk.jwks.url}")
    private String jwksUrl;
    
    private JsonNode cachedJwks;
    private LocalDateTime lastFetchTime;
    private static final Duration CACHE_DURATION = Duration.ofHours(1);

    public JsonNode getJwks() throws Exception {
        // Check if cache is valid
        if (cachedJwks != null && 
            lastFetchTime != null && 
            Duration.between(lastFetchTime, LocalDateTime.now())
                .compareTo(CACHE_DURATION) < 0) {
            return cachedJwks;
        }
        
        // Fetch fresh JWKS from Clerk
        RestTemplate restTemplate = new RestTemplate();
        String response = restTemplate.getForObject(jwksUrl, String.class);
        
        ObjectMapper mapper = new ObjectMapper();
        cachedJwks = mapper.readTree(response);
        lastFetchTime = LocalDateTime.now();
        
        return cachedJwks;
    }
}
```

### SecurityConfig

```java
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final ClerkJwtAuthFilter clerkJwtAuthFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) 
            throws Exception {
        
        http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/v1.0/health").permitAll()
                .requestMatchers("/api/v1.0/register").permitAll()
                .anyRequest().authenticated()
            )
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .addFilterBefore(
                clerkJwtAuthFilter, 
                UsernamePasswordAuthenticationFilter.class
            );

        return http.build();
    }

    private UrlBasedCorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of(
            "http://localhost:5173",
            "http://localhost:3000",
            "https://your-production-domain.com"
        ));
        config.setAllowedMethods(List.of(
            "GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"
        ));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        config.setExposedHeaders(List.of("*"));

        UrlBasedCorsConfigurationSource source = 
            new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
```

---

## 🗄️ Database Architecture

### MongoDB Documents

#### 1. Profile Document

```java
@Document(collection = "profiles")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Profile {
    
    @Id
    private String id;
    
    @Indexed(unique = true)
    private String clerkId;
    
    private String email;
    private String name;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

#### 2. UserCredits Document

```java
@Document(collection = "user_credits")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserCredits {
    
    @Id
    private String id;
    
    @Indexed(unique = true)
    private String clerkId;
    
    private Long credits;
    private Long storageUsed;        // in MB
    private Long storageLimit;       // in MB
    private String currentPlan;      // "basic", "premium", "ultimate"
    private LocalDateTime lastUpdated;
}
```

#### 3. FileMetadata Document

```java
@Document(collection = "file_metadata")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileMetadata {
    
    @Id
    private String id;
    
    private String fileName;
    private String fileKey;          // R2 object key
    private Long fileSize;           // in bytes
    private String contentType;
    
    @Indexed
    private String clerkId;
    
    private Boolean isPublic;
    private Boolean isWelcomeFile;
    private LocalDateTime uploadedAt;
    private LocalDateTime lastAccessedAt;
}
```

#### 4. PaymentTransaction Document

```java
@Document(collection = "payment_transactions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentTransaction {
    
    @Id
    private String id;
    
    @Indexed
    private String clerkId;
    
    private String razorpayOrderId;
    private String razorpayPaymentId;
    private String razorpaySignature;
    
    private String planId;
    private Long amount;             // in paise
    private String currency;
    private String status;           // "created", "paid", "failed"
    
    private LocalDateTime createdAt;
    private LocalDateTime paidAt;
}
```

### Database Relationships

```
Profile (1) ──────── (1) UserCredits
    │
    │
    └──── (1) ──────── (N) FileMetadata
    │
    │
    └──── (1) ──────── (N) PaymentTransaction
```

---

## 🌐 API Endpoints

### Base URL Structure

```
Production:  https://your-api.onrender.com/api/v1.0
Development: http://localhost:8080/api/v1.0
```

### 1. Profile Management

#### Register/Sync User

```http
POST /api/v1.0/register
Authorization: Bearer <jwt_token>
Content-Type: application/json

{
    "clerkId": "user_xxxxxxxxxxxxx",
    "email": "user@example.com",
    "name": "John Doe"
}

Response: 200 OK
{
    "clerkId": "user_xxxxxxxxxxxxx",
    "email": "user@example.com",
    "name": "John Doe"
}
```

**Implementation:**
```java
@PostMapping("/register")
public ResponseEntity<ProfileDTO> registerUser(
        @RequestBody ProfileDTO profileDTO) {
    
    ProfileDTO createdProfile = profileService.createProfile(profileDTO);
    return ResponseEntity.ok(createdProfile);
}
```

---

### 2. User Credits & Storage

#### Get User Credits

```http
GET /api/v1.0/users/credits
Authorization: Bearer <jwt_token>

Response: 200 OK
{
    "credits": 500,
    "storageUsed": 256,
    "storageLimit": 1024,
    "currentPlan": "basic"
}
```

#### Get Storage Info

```http
GET /api/v1.0/users/storage
Authorization: Bearer <jwt_token>

Response: 200 OK
{
    "storageUsed": 256,
    "storageLimit": 1024,
    "currentPlan": "basic",
    "storagePercentage": 25.0
}
```

**Implementation:**
```java
@GetMapping("/users/storage")
public ResponseEntity<StorageInfoDTO> getStorageInfo() {
    UserCredits userCredits = userCreditsService.getUserCredits();
    
    StorageInfoDTO response = StorageInfoDTO.builder()
        .storageUsed(userCredits.getStorageUsed())
        .storageLimit(userCredits.getStorageLimit())
        .currentPlan(userCredits.getCurrentPlan())
        .storagePercentage(calculateStoragePercentage(userCredits))
        .build();
    
    return ResponseEntity.ok(response);
}
```

---

### 3. File Operations

#### Upload File

```http
POST /api/v1.0/files/upload
Authorization: Bearer <jwt_token>
Content-Type: multipart/form-data

file: <binary_file_data>
isPublic: false

Response: 201 Created
{
    "id": "file_id_123",
    "fileName": "document.pdf",
    "fileSize": 1048576,
    "uploadedAt": "2024-10-26T10:30:00Z"
}
```

**Implementation:**
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
    String fileKey = cloudflareR2Service.uploadFile(file, "private/" + clerkId);
    
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
    
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(convertToDTO(saved));
}
```

#### Download File

```http
GET /api/v1.0/files/download/{fileId}
Authorization: Bearer <jwt_token>

Response: 200 OK
Content-Type: application/octet-stream
Content-Disposition: attachment; filename="document.pdf"

<binary_file_data>
```

**Implementation:**
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

#### List User Files

```http
GET /api/v1.0/files/list?page=0&size=10
Authorization: Bearer <jwt_token>

Response: 200 OK
{
    "content": [
        {
            "id": "file_id_123",
            "fileName": "document.pdf",
            "fileSize": 1048576,
            "contentType": "application/pdf",
            "uploadedAt": "2024-10-26T10:30:00Z"
        }
    ],
    "totalElements": 25,
    "totalPages": 3,
    "currentPage": 0
}
```

#### Delete File

```http
DELETE /api/v1.0/files/{fileId}
Authorization: Bearer <jwt_token>

Response: 204 No Content
```

**Implementation:**
```java
@DeleteMapping("/files/{fileId}")
public ResponseEntity<Void> deleteFile(@PathVariable String fileId) {
    
    // 1. Get file metadata
    FileMetadata metadata = fileMetadataRepository.findById(fileId)
        .orElseThrow(() -> new FileNotFoundException("File not found"));
    
    // 2. Verify ownership
    String clerkId = SecurityContextHolder.getContext()
        .getAuthentication().getName();
    
    if (!metadata.getClerkId().equals(clerkId)) {
        throw new UnauthorizedException("Access denied");
    }
    
    // 3. Delete from R2
    cloudflareR2Service.deleteFile(metadata.getFileKey());
    
    // 4. Delete metadata
    fileMetadataRepository.deleteById(fileId);
    
    // 5. Update storage usage
    long freedSpace = metadata.getFileSize() / (1024 * 1024); // Convert to MB
    userCreditsService.updateStorageUsage(clerkId, -freedSpace);
    
    return ResponseEntity.noContent().build();
}
```

---

### 4. Payment Operations

#### Create Payment Order

```http
POST /api/v1.0/payments/create-order
Authorization: Bearer <jwt_token>
Content-Type: application/json

{
    "planId": "premium",
    "amount": 29900,
    "currency": "INR",
    "storageInMB": 5120
}

Response: 200 OK
{
    "orderId": "order_xxxxxxxxxxxxx",
    "amount": 29900,
    "currency": "INR"
}
```

**Implementation:**
```java
@PostMapping("/payments/create-order")
public ResponseEntity<PaymentOrderDTO> createOrder(
        @RequestBody PaymentOrderDTO orderRequest) {
    
    String clerkId = SecurityContextHolder.getContext()
        .getAuthentication().getName();
    
    // 1. Create Razorpay order
    JSONObject orderRequest = new JSONObject();
    orderRequest.put("amount", orderRequest.getAmount());
    orderRequest.put("currency", orderRequest.getCurrency());
    orderRequest.put("receipt", "receipt_" + System.currentTimeMillis());
    
    Order order = razorpayClient.orders.create(orderRequest);
    
    // 2. Save transaction record
    PaymentTransaction transaction = PaymentTransaction.builder()
        .clerkId(clerkId)
        .razorpayOrderId(order.get("id"))
        .planId(orderRequest.getPlanId())
        .amount(orderRequest.getAmount())
        .currency(orderRequest.getCurrency())
        .status("created")
        .createdAt(LocalDateTime.now())
        .build();
    
    paymentTransactionRepository.save(transaction);
    
    // 3. Return order details
    return ResponseEntity.ok(PaymentOrderDTO.builder()
        .orderId(order.get("id"))
        .amount(orderRequest.getAmount())
        .currency(orderRequest.getCurrency())
        .build());
}
```

#### Verify Payment

```http
POST /api/v1.0/payments/verify
Authorization: Bearer <jwt_token>
Content-Type: application/json

{
    "razorpay_order_id": "order_xxxxxxxxxxxxx",
    "razorpay_payment_id": "pay_xxxxxxxxxxxxx",
    "razorpay_signature": "signature_xxxxxxxxxxxxx",
    "planId": "premium"
}

Response: 200 OK
{
    "success": true,
    "message": "Payment verified successfully"
}
```

**Implementation:**
```java
@PostMapping("/payments/verify")
public ResponseEntity<Map<String, Object>> verifyPayment(
        @RequestBody PaymentVerificationDTO verificationDTO) {
    
    String clerkId = SecurityContextHolder.getContext()
        .getAuthentication().getName();
    
    try {
        // 1. Verify signature
        String signature = calculateSignature(
            verificationDTO.getRazorpayOrderId(),
            verificationDTO.getRazorpayPaymentId()
        );
        
        if (!signature.equals(verificationDTO.getRazorpaySignature())) {
            throw new SignatureException("Invalid signature");
        }
        
        // 2. Update transaction
        PaymentTransaction transaction = paymentTransactionRepository
            .findByRazorpayOrderId(verificationDTO.getRazorpayOrderId())
            .orElseThrow(() -> new NotFoundException("Transaction not found"));
        
        transaction.setRazorpayPaymentId(verificationDTO.getRazorpayPaymentId());
        transaction.setRazorpaySignature(verificationDTO.getRazorpaySignature());
        transaction.setStatus("paid");
        transaction.setPaidAt(LocalDateTime.now());
        paymentTransactionRepository.save(transaction);
        
        // 3. Update user storage
        UserCredits credits = userCreditsService.getUserCredits();
        credits.setCurrentPlan(verificationDTO.getPlanId());
        credits.setStorageLimit(getStorageLimitForPlan(verificationDTO.getPlanId()));
        userCreditsRepository.save(credits);
        
        return ResponseEntity.ok(Map.of(
            "success", true,
            "message", "Payment verified successfully"
        ));
        
    } catch (Exception e) {
        return ResponseEntity.ok(Map.of(
            "success", false,
            "message", "Payment verification failed"
        ));
    }
}

private String calculateSignature(String orderId, String paymentId) {
    String payload = orderId + "|" + paymentId;
    return HmacUtils.hmacSha256Hex(razorpaySecret, payload);
}
```

---

## ☁️ Cloudflare R2 Integration

### R2 Configuration

```java
@Configuration
public class CloudflareR2Config {

    @Value("${cloudflare.r2.account-id}")
    private String accountId;

    @Value("${cloudflare.r2.access-key-id}")
    private String accessKeyId;

    @Value("${cloudflare.r2.secret-access-key}")
    private String secretAccessKey;

    @Value("${cloudflare.r2.bucket-name}")
    private String bucketName;

    @Bean
    public S3Client s3Client() {
        AwsBasicCredentials credentials = AwsBasicCredentials.create(
            accessKeyId, 
            secretAccessKey
        );

        return S3Client.builder()
            .region(Region.of("auto"))
            .credentialsProvider(StaticCredentialsProvider.create(credentials))
            .endpointOverride(URI.create(
                "https://" + accountId + ".r2.cloudflarestorage.com"
            ))
            .build();
    }
}
```

### CloudflareR2Service

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class CloudflareR2Service {

    private final S3Client s3Client;
    private final CloudflareR2Config r2Config;

    /**
     * Upload file to R2
     */
    public String uploadFile(MultipartFile file, String folder) 
            throws IOException {
        
        String fileName = generateFileName(file.getOriginalFilename());
        String key = folder + "/" + fileName;

        try {
            // For large files (>5MB), use multipart upload
            if (file.getSize() > 5 * 1024 * 1024) {
                return uploadLargeFile(file, key);
            }

            // Standard upload for smaller files
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(r2Config.getBucketName())
                .key(key)
                .contentType(file.getContentType())
                .contentLength(file.getSize())
                .build();

            s3Client.putObject(
                putObjectRequest, 
                RequestBody.fromBytes(file.getBytes())
            );

            log.info("File uploaded successfully to R2: {}", key);
            return key;

        } catch (Exception e) {
            log.error("Error uploading file to R2: {}", key, e);
            throw new RuntimeException("Failed to upload file to R2", e);
        }
    }

    /**
     * Upload large file using multipart upload
     */
    private String uploadLargeFile(MultipartFile file, String key) 
            throws IOException {
        
        // 1. Initiate multipart upload
        CreateMultipartUploadRequest createRequest = 
            CreateMultipartUploadRequest.builder()
                .bucket(r2Config.getBucketName())
                .key(key)
                .contentType(file.getContentType())
                .build();

        CreateMultipartUploadResponse createResponse = 
            s3Client.createMultipartUpload(createRequest);
        String uploadId = createResponse.uploadId();

        try {
            List<CompletedPart> completedParts = new ArrayList<>();
            byte[] fileBytes = file.getBytes();
            int partSize = 5 * 1024 * 1024; // 5MB parts
            int partCount = (int) Math.ceil((double) fileBytes.length / partSize);

            // 2. Upload each part
            for (int i = 0; i < partCount; i++) {
                int start = i * partSize;
                int end = Math.min(start + partSize, fileBytes.length);
                byte[] partBytes = Arrays.copyOfRange(fileBytes, start, end);

                UploadPartRequest uploadPartRequest = UploadPartRequest.builder()
                    .bucket(r2Config.getBucketName())
                    .key(key)
                    .uploadId(uploadId)
                    .partNumber(i + 1)
                    .build();

                UploadPartResponse uploadPartResponse = s3Client.uploadPart(
                    uploadPartRequest,
                    RequestBody.fromBytes(partBytes)
                );

                completedParts.add(CompletedPart.builder()
                    .partNumber(i + 1)
                    .eTag(uploadPartResponse.eTag())
                    .build());
            }

            // 3. Complete multipart upload
            CompleteMultipartUploadRequest completeRequest = 
                CompleteMultipartUploadRequest.builder()
                    .bucket(r2Config.getBucketName())
                    .key(key)
                    .uploadId(uploadId)
                    .multipartUpload(CompletedMultipartUpload.builder()
                        .parts(completedParts)
                        .build())
                    .build();

            s3Client.completeMultipartUpload(completeRequest);
            log.info("Large file uploaded successfully to R2: {}", key);
            return key;

        } catch (Exception e) {
            // Abort multipart upload on failure
            AbortMultipartUploadRequest abortRequest = 
                AbortMultipartUploadRequest.builder()
                    .bucket(r2Config.getBucketName())
                    .key(key)
                    .uploadId(uploadId)
                    .build();
            s3Client.abortMultipartUpload(abortRequest);
            
            throw new RuntimeException("Failed to upload large file", e);
        }
    }

    /**
     * Download file from R2
     */
    public byte[] downloadFile(String fileKey) {
        try {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(r2Config.getBucketName())
                .key(fileKey)
                .build();

            ResponseBytes<GetObjectResponse> objectBytes = 
                s3Client.getObjectAsBytes(getObjectRequest);

            log.info("File downloaded successfully from R2: {}", fileKey);
            return objectBytes.asByteArray();

        } catch (Exception e) {
            log.error("Error downloading file from R2: {}", fileKey, e);
            throw new RuntimeException("Failed to download file from R2", e);
        }
    }

    /**
     * Delete file from R2
     */
    public void deleteFile(String fileKey) {
        try {
            DeleteObjectRequest deleteObjectRequest = 
                DeleteObjectRequest.builder()
                    .bucket(r2Config.getBucketName())
                    .key(fileKey)
                    .build();

            s3Client.deleteObject(deleteObjectRequest);
            log.info("File deleted successfully from R2: {}", fileKey);

        } catch (Exception e) {
            log.error("Error deleting file from R2: {}", fileKey, e);
            throw new RuntimeException("Failed to delete file from R2", e);
        }
    }

    /**
     * Generate unique filename
     */
    private String generateFileName(String originalFilename) {
        String extension = "";
        int dotIndex = originalFilename.lastIndexOf('.');
        if (dotIndex > 0) {
            extension = originalFilename.substring(dotIndex);
        }
        return UUID.randomUUID().toString() + extension;
    }

    /**
     * Generate presigned URL for file access
     */
    public String generatePresignedUrl(String fileKey, Duration duration) {
        try {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(r2Config.getBucketName())
                .key(fileKey)
                .build();

            GetObjectPresignRequest presignRequest = 
                GetObjectPresignRequest.builder()
                    .signatureDuration(duration)
                    .getObjectRequest(getObjectRequest)
                    .build();

            S3Presigner presigner = S3Presigner.builder()
                .region(Region.of("auto"))
                .credentialsProvider(StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(
                        r2Config.getAccessKeyId(),
                        r2Config.getSecretAccessKey()
                    )
                ))
                .endpointOverride(URI.create(
                    "https://" + r2Config.getAccountId() + 
                    ".r2.cloudflarestorage.com"
                ))
                .build();

            PresignedGetObjectRequest presignedRequest = 
                presigner.presignGetObject(presignRequest);

            return presignedRequest.url().toString();

        } catch (Exception e) {
            log.error("Error generating presigned URL: {}", fileKey, e);
            throw new RuntimeException("Failed to generate presigned URL", e);
        }
    }
}
```

---

## 💳 Razorpay Payment Integration

### RazorpayConfig

```java
@Configuration
public class RazorpayConfig {

    @Value("${razorpay.key.id}")
    private String keyId;

    @Value("${razorpay.key.secret}")
    private String keySecret;

    @Bean
    public RazorpayClient razorpayClient() throws RazorpayException {
        return new RazorpayClient(keyId, keySecret);
    }
}
```

### Payment Flow Diagram

```
┌──────────────┐
│   Frontend   │
└──────┬───────┘
       │
       │ 1. User clicks "Purchase Plan"
       ▼
┌────────────────────────────┐
│   POST /payments/create-order
│   planId, amount, currency
└──────┬─────────────────────┘
       │
       │ 2. Create order in Razorpay
       ▼
┌────────────────────────────┐
│   Razorpay API             │
│   Creates order            │
└──────┬─────────────────────┘
       │
       │ 3. Return order_id
       ▼
┌────────────────────────────┐
│   Save PaymentTransaction  │
│   status: "created"        │
└──────┬─────────────────────┘
       │
       │ 4. Return order_id to frontend
       ▼
┌────────────────────────────┐
│   Frontend opens Razorpay  │
│   checkout modal           │
└──────┬─────────────────────┘
       │
       │ 5. User completes payment
       ▼
┌────────────────────────────┐
│   Razorpay processes payment
│   Returns payment_id,      │
│   order_id, signature      │
└──────┬─────────────────────┘
       │
       │ 6. Frontend sends verification
       ▼
┌────────────────────────────┐
│   POST /payments/verify    │
│   payment_id, signature    │
└──────┬─────────────────────┘
       │
       │ 7. Verify signature
       ▼
┌────────────────────────────┐
│   Calculate HMAC signature │
│   Compare with received    │
└──────┬─────────────────────┘
       │
       │ 8. If valid
       ▼
┌────────────────────────────┐
│   Update PaymentTransaction│
│   status: "paid"           │
│   Update UserCredits       │
│   Upgrade storage plan     │
└────────────────────────────┘
```

---

## ⚙️ Configuration

### application.properties

```properties
# Application
spring.application.name=skyboxapi
server.port=${PORT:8080}
server.servlet.context-path=/api/v1.0

# MongoDB (use environment variable)
spring.data.mongodb.uri=${MONGODB_URI}

# File Upload Limits
spring.servlet.multipart.max-file-size=25MB
spring.servlet.multipart.max-request-size=1024MB

# Tomcat Configuration
server.tomcat.max-swallow-size=1024MB
server.tomcat.max-http-form-post-size=1024MB
server.tomcat.connection-timeout=300000
server.tomcat.keep-alive-timeout=300000
spring.mvc.async.request-timeout=300000

# Clerk Authentication
clerk.jwks.url=https://YOUR_CLERK_DOMAIN/.well-known/jwks.json

# Cloudflare R2 (use environment variables)
cloudflare.r2.account-id=${CLOUDFLARE_ACCOUNT_ID}
cloudflare.r2.access-key-id=${CLOUDFLARE_ACCESS_KEY_ID}
cloudflare.r2.secret-access-key=${CLOUDFLARE_SECRET_ACCESS_KEY}
cloudflare.r2.bucket-name=${CLOUDFLARE_BUCKET_NAME:skybox-files}
cloudflare.r2.endpoint=https://${cloudflare.r2.account-id}.r2.cloudflarestorage.com

# Razorpay (use environment variables)
razorpay.key.id=${RAZORPAY_KEY_ID}
razorpay.key.secret=${RAZORPAY_KEY_SECRET}

# Logging
logging.level.root=INFO
logging.level.com.skybox.skyboxapi=DEBUG
```

### Environment Variables

Required environment variables (never commit these):

```bash
# MongoDB
MONGODB_URI=mongodb+srv://user:pass@cluster.mongodb.net/skybox

# Cloudflare R2
CLOUDFLARE_ACCOUNT_ID=your_account_id
CLOUDFLARE_ACCESS_KEY_ID=your_access_key
CLOUDFLARE_SECRET_ACCESS_KEY=your_secret_key
CLOUDFLARE_BUCKET_NAME=skybox-files

# Razorpay
RAZORPAY_KEY_ID=rzp_live_xxxxx
RAZORPAY_KEY_SECRET=your_secret_key

# For Render deployment
PORT=8080
```

---

## 🚀 Running the Application

### Local Development

```bash
# Navigate to backend directory
cd skyboxapi

# Run with Maven wrapper
./mvnw spring-boot:run

# Or build and run JAR
./mvnw clean package
java -jar target/skyboxapi-0.0.1-SNAPSHOT.jar
```

### Docker Deployment

```dockerfile
FROM eclipse-temurin:17-jdk-alpine
WORKDIR /app
COPY target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

```bash
# Build image
docker build -t skybox-api .

# Run container
docker run -p 8080:8080 \
  -e MONGODB_URI="mongodb+srv://..." \
  -e CLOUDFLARE_ACCOUNT_ID="..." \
  skybox-api
```

---

## 🧪 Testing

### Unit Tests

```java
@SpringBootTest
class FileMetadataServiceTest {

    @MockBean
    private FileMetadataRepository repository;

    @MockBean
    private CloudflareR2Service r2Service;

    @Autowired
    private FileMetadataService service;

    @Test
    void testUploadFile() {
        // Arrange
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "test.pdf",
            "application/pdf",
            "test content".getBytes()
        );

        when(r2Service.uploadFile(any(), any()))
            .thenReturn("test-key");

        // Act
        FileMetadata result = service.uploadFile(file, "user_123", false);

        // Assert
        assertNotNull(result);
        assertEquals("test.pdf", result.getFileName());
        verify(r2Service).uploadFile(any(), any());
    }
}
```

### Integration Tests

```java
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class FileControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithMockUser(username = "user_123")
    void testFileUpload() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "test.pdf",
            "application/pdf",
            "test content".getBytes()
        );

        mockMvc.perform(multipart("/api/v1.0/files/upload")
                .file(file)
                .header("Authorization", "Bearer mock-token"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.fileName").value("test.pdf"));
    }
}
```

---

## 📊 Monitoring & Logging

### Logging Configuration

```java
@Slf4j
@RestController
public class FileController {

    @PostMapping("/upload")
    public ResponseEntity<?> uploadFile(@RequestParam("file") MultipartFile file) {
        log.info("Upload request received: filename={}, size={}", 
            file.getOriginalFilename(), file.getSize());
        
        try {
            // Upload logic
            log.debug("File uploaded successfully: {}", fileKey);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("File upload failed", e);
            return ResponseEntity.status(500).body("Upload failed");
        }
    }
}
```

### Health Check Endpoint

```java
@RestController
@RequestMapping("/health")
public class HealthController {

    @GetMapping
    public ResponseEntity<Map<String, String>> healthCheck() {
        return ResponseEntity.ok(Map.of(
            "status", "UP",
            "timestamp", LocalDateTime.now().toString()
        ));
    }
}
```

---

## 🔧 Performance Optimization

### Database Indexing

```java
// Create indexes for frequently queried fields
@Document(collection = "file_metadata")
@CompoundIndex(name = "clerkId_uploadedAt_idx", 
    def = "{'clerkId': 1, 'uploadedAt': -1}")
public class FileMetadata {
    @Indexed
    private String clerkId;
    
    @Indexed
    private LocalDateTime uploadedAt;
}
```

### Caching

```java
@Service
@Slf4j
public class FileMetadataService {

    @Cacheable(value = "userFiles", key = "#clerkId")
    public List<FileMetadata> getUserFiles(String clerkId) {
        return fileMetadataRepository.findByClerkId(clerkId);
    }

    @CacheEvict(value = "userFiles", key = "#clerkId")
    public void invalidateCache(String clerkId) {
        log.info("Cache invalidated for user: {}", clerkId);
    }
}
```

### Async Processing

```java
@Service
public class FileProcessingService {

    @Async
    public CompletableFuture<String> processFileAsync(MultipartFile file) {
        // Long-running file processing
        String result = processFile(file);
        return CompletableFuture.completedFuture(result);
    }
}
```

---

## 🐛 Error Handling

### Global Exception Handler

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(FileNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleFileNotFound(
            FileNotFoundException ex) {
        ErrorResponse error = ErrorResponse.builder()
            .status(HttpStatus.NOT_FOUND.value())
            .message(ex.getMessage())
            .timestamp(LocalDateTime.now())
            .build();
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(StorageLimitExceededException.class)
    public ResponseEntity<ErrorResponse> handleStorageLimitExceeded(
            StorageLimitExceededException ex) {
        ErrorResponse error = ErrorResponse.builder()
            .status(HttpStatus.FORBIDDEN.value())
            .message(ex.getMessage())
            .timestamp(LocalDateTime.now())
            .build();
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex) {
        log.error("Unexpected error occurred", ex);
        ErrorResponse error = ErrorResponse.builder()
            .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
            .message("An unexpected error occurred")
            .timestamp(LocalDateTime.now())
            .build();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(error);
    }
}
```

---

## 📚 Additional Resources

- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [Spring Security Documentation](https://spring.io/projects/spring-security)
- [MongoDB Documentation](https://www.mongodb.com/docs/)
- [AWS S3 SDK Documentation](https://docs.aws.amazon.com/sdk-for-java/)
- [Razorpay API Documentation](https://razorpay.com/docs/api/)

---

**Backend Architecture Documentation - Version 1.0**
*Last Updated: October 2025*