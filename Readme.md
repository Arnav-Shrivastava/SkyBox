# 🌤️ SkyBox - Cloud File Storage Platform

<div align="center">

![SkyBox Logo](https://img.shields.io/badge/SkyBox-Cloud_Storage-blue?style=for-the-badge)
[![React](https://img.shields.io/badge/React-18.3.1-61DAFB?style=flat-square&logo=react)](https://reactjs.org/)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.5.6-6DB33F?style=flat-square&logo=springboot)](https://spring.io/projects/spring-boot)
[![MongoDB](https://img.shields.io/badge/MongoDB-Cloud-47A248?style=flat-square&logo=mongodb)](https://www.mongodb.com/)
[![Cloudflare R2](https://img.shields.io/badge/Cloudflare-R2-F38020?style=flat-square&logo=cloudflare)](https://www.cloudflare.com/products/r2/)

**A modern, secure cloud file storage solution with subscription-based plans and seamless file management.**

[Live Demo](#) • [Documentation](./INTEGRATION_ARCHITECTURE.md) • [Report Bug](#) • [Request Feature](#)

</div>

---

## 📋 Table of Contents

- [About The Project](#about-the-project)
- [Features](#features)
- [Tech Stack](#tech-stack)
- [Architecture](#architecture)
- [Getting Started](#getting-started)
  - [Prerequisites](#prerequisites)
  - [Installation](#installation)
  - [Environment Variables](#environment-variables)
- [Usage](#usage)
- [API Documentation](#api-documentation)
- [Deployment](#deployment)
- [Subscription Plans](#subscription-plans)
- [Contributing](#contributing)
- [License](#license)
- [Contact](#contact)
- [Acknowledgments](#acknowledgments)

---

## 🎯 About The Project

SkyBox is a full-stack cloud file storage platform that enables users to securely upload, manage, and download files with a beautiful, intuitive interface. Built with modern technologies, it offers subscription-based storage plans with integrated payment processing.

### Why SkyBox?

- 🔒 **Secure Authentication** - Powered by Clerk for robust user management
- ☁️ **Cloud Storage** - Leverages Cloudflare R2 for reliable, fast file storage
- 💳 **Seamless Payments** - Integrated Razorpay for Indian market (₹ INR)
- 📊 **Real-time Tracking** - Monitor storage usage and manage files effortlessly
- 🎨 **Modern UI** - Clean, responsive design with Tailwind CSS
- 🚀 **High Performance** - Optimized for speed and reliability

---

## ✨ Features

### Core Features
- ✅ **User Authentication & Authorization** via Clerk
- ✅ **File Upload/Download** with progress tracking
- ✅ **Storage Management** with visual usage indicators
- ✅ **Subscription Plans** (Basic, Premium, Ultimate)
- ✅ **Payment Processing** with Razorpay
- ✅ **File Metadata Management**
- ✅ **Secure File Access** with JWT tokens
- ✅ **Multipart Upload** for large files (>5MB)

### Advanced Features
- 🔐 JWT-based API authentication
- 📦 Cloudflare R2 object storage integration
- 🎫 Presigned URLs for secure file access
- 💾 MongoDB for metadata and user data
- 🔄 Real-time credit synchronization
- 📱 Responsive design for all devices
- 🌍 CORS configured for cross-origin requests

---

## 🛠️ Tech Stack

### Frontend
- **React 18.3.1** - UI Library
- **Vite 5.4.2** - Build Tool
- **React Router 6.26.2** - Routing
- **Clerk React 5.14.2** - Authentication
- **Axios 1.7.7** - HTTP Client
- **Tailwind CSS 3.4.14** - Styling
- **Lucide React** - Icons

### Backend
- **Spring Boot 3.5.6** - Framework
- **Java 17** - Programming Language
- **Spring Security** - Security Layer
- **MongoDB** - Database
- **AWS S3 SDK 2.20.26** - R2 Integration
- **JJWT 0.11.5** - JWT Processing
- **Razorpay Java 1.4.3** - Payment Gateway

### Cloud Services
- **Cloudflare R2** - Object Storage
- **MongoDB Atlas** - Cloud Database
- **Clerk** - Authentication Service
- **Razorpay** - Payment Processing
- **Render** - Backend Hosting (Optional)

---

## 🏗️ Architecture

```
┌─────────────────┐         ┌──────────────────┐         ┌─────────────────┐
│                 │         │                  │         │                 │
│  React Frontend │◄───────►│  Spring Boot API │◄───────►│  MongoDB Atlas  │
│                 │   JWT   │                  │  Data   │                 │
└─────────────────┘         └──────────────────┘         └─────────────────┘
        │                            │
        │                            │
        ▼                            ▼
┌─────────────────┐         ┌──────────────────┐
│                 │         │                  │
│  Clerk Auth     │         │  Cloudflare R2   │
│                 │         │  (File Storage)  │
└─────────────────┘         └──────────────────┘
        │                            
        │                            
        ▼                            
┌─────────────────┐         
│                 │         
│  Razorpay       │         
│  (Payments)     │         
└─────────────────┘         
```

### Request Flow
1. User authenticates via Clerk
2. Frontend receives JWT token
3. API requests include JWT in Authorization header
4. Backend validates JWT against Clerk's JWKS
5. Authorized requests access MongoDB and R2
6. Files stored in Cloudflare R2, metadata in MongoDB

📚 **Detailed Documentation:**
- [Frontend Architecture](./FRONTEND_ARCHITECTURE.md)
- [Backend Architecture](./BACKEND_ARCHITECTURE.md)
- [Integration Guide](./INTEGRATION_ARCHITECTURE.md)

---

## 🚀 Getting Started

### Prerequisites

- **Node.js** 18+ and npm/yarn
- **Java 17+** and Maven
- **MongoDB Atlas** account
- **Cloudflare R2** account
- **Clerk** account
- **Razorpay** account

### Installation

#### 1. Clone the repository
```bash
git clone https://github.com/yourusername/skybox.git
cd skybox
```

#### 2. Backend Setup

```bash
cd skyboxapi

# Install dependencies (Maven will download automatically)
./mvnw clean install

# Run the application
./mvnw spring-boot:run
```

The backend will start on `http://localhost:8080`

#### 3. Frontend Setup

```bash
cd ../skyboxwebapp

# Install dependencies
npm install

# Start development server
npm run dev
```

The frontend will start on `http://localhost:5173`

---

## 🔐 Environment Variables

### Backend (`skyboxapi/src/main/resources/application.properties`)

```properties
# MongoDB
spring.data.mongodb.uri=mongodb+srv://username:password@cluster.mongodb.net/skybox

# Cloudflare R2
cloudflare.r2.account-id=your_account_id
cloudflare.r2.access-key-id=your_access_key
cloudflare.r2.secret-access-key=your_secret_key
cloudflare.r2.bucket-name=skybox-files

# Razorpay
razorpay.key.id=your_razorpay_key
razorpay.key.secret=your_razorpay_secret

# Clerk
clerk.jwks.url=https://your-clerk-domain/.well-known/jwks.json
```

### Frontend (`skyboxwebapp/.env`)

```env
VITE_CLERK_PUBLISHABLE_KEY=pk_test_your_clerk_key
VITE_RAZORPAY_KEY=rzp_live_your_razorpay_key
VITE_API_BASE_URL=http://localhost:8080/api/v1.0
```

---

## 📖 Usage

### 1. User Registration
- Visit the landing page
- Click "Get Started" or "Sign Up"
- Complete Clerk authentication
- Automatically redirected to dashboard

### 2. Upload Files
```javascript
// Files up to 25MB per file
// Total request size up to 1GB
// Supported: All file types
```

- Click "Upload File" button
- Select file from your device
- Monitor upload progress
- File appears in "My Files"

### 3. Download Files
- Navigate to "My Files"
- Click download icon on any file
- File downloads immediately

### 4. Manage Storage
- View storage usage on dashboard
- Upgrade plan in "Subscription" page
- Choose from Basic (1GB), Premium (5GB), or Ultimate (10GB)

---

## 📡 API Documentation

### Base URL
```
http://localhost:8080/api/v1.0
```

### Authentication
All protected endpoints require JWT token:
```
Authorization: Bearer <clerk_jwt_token>
```

### Key Endpoints

#### User Management
```http
POST /register
GET /users/credits
GET /users/storage
```

#### File Operations
```http
POST /files/upload
GET /files/download/{fileId}
GET /files/list
DELETE /files/{fileId}
```

#### Payments
```http
POST /payments/create-order
POST /payments/verify
```

#### Health Check
```http
GET /health
```

For complete API documentation, see [Backend Architecture](./BACKEND_ARCHITECTURE.md).

---

## 🚢 Deployment

### Backend on Render

1. Push code to GitHub
2. Create new Web Service on Render
3. Configure:
   - **Build Command**: `cd skyboxapi && ./mvnw clean package -DskipTests`
   - **Start Command**: `cd skyboxapi && java -jar target/*.jar`
4. Add environment variables
5. Deploy

### Frontend on Vercel/Netlify

```bash
cd skyboxwebapp

# Build
npm run build

# Deploy dist/ folder
```

Update `.env` with production API URL:
```env
VITE_API_BASE_URL=https://your-backend.onrender.com/api/v1.0
```

---

## 💳 Subscription Plans

| Plan | Storage | Price | Features |
|------|---------|-------|----------|
| **Basic** | 1GB | Free | • 25MB max file size<br>• Unlimited uploads<br>• Basic support |
| **Premium** | 5GB | ₹299/month | • 25MB max file size<br>• Unlimited uploads<br>• Priority support<br>• Advanced features |
| **Ultimate** | 10GB | ₹499/month | • 25MB max file size<br>• Unlimited uploads<br>• Premium support<br>• Advanced analytics |

---

## 🤝 Contributing

Contributions make the open-source community amazing! Any contributions are **greatly appreciated**.

1. Fork the Project
2. Create your Feature Branch (`git checkout -b feature/AmazingFeature`)
3. Commit your Changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the Branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

---

## 📝 License

Distributed under the MIT License. See `LICENSE` for more information.

---

## 📧 Contact

Your Name - [@yourtwitter](https://twitter.com/yourtwitter) - email@example.com

Project Link: [https://github.com/yourusername/skybox](https://github.com/yourusername/skybox)

---

## 🙏 Acknowledgments

- [React](https://reactjs.org/)
- [Spring Boot](https://spring.io/projects/spring-boot)
- [Clerk](https://clerk.com/)
- [Cloudflare R2](https://www.cloudflare.com/products/r2/)
- [MongoDB](https://www.mongodb.com/)
- [Razorpay](https://razorpay.com/)
- [Tailwind CSS](https://tailwindcss.com/)
- [Lucide Icons](https://lucide.dev/)

---

<div align="center">

**Made with ❤️ by Your Team**

⭐ Star this repo if you find it helpful!

</div>