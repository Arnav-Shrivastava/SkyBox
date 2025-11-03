# 🎨 SkyBox Frontend Architecture

## Overview

SkyBox's frontend is a modern React application built with Vite, providing a responsive and intuitive interface for cloud file storage management. The application follows component-based architecture with centralized state management and secure authentication.

---

## 📁 Project Structure

```
skyboxwebapp/
├── public/                          # Static assets
├── src/
│   ├── assets/
│   │   └── data.js                 # Static data (features, pricing, testimonials)
│   ├── components/
│   │   ├── landing/                # Landing page components
│   │   │   ├── HeroSection.jsx
│   │   │   ├── FeaturesSection.jsx
│   │   │   ├── PricingSection.jsx
│   │   │   ├── TestimonialsSection.jsx
│   │   │   ├── CTASection.jsx
│   │   │   └── Footer.jsx
│   │   ├── ConfirmationDialog.jsx  # Reusable confirmation modal
│   │   ├── CreditsDisplay.jsx      # User credits widget
│   │   └── Modal.jsx               # Base modal component
│   ├── context/
│   │   └── UserCreditsContext.jsx  # Global state for user credits/storage
│   ├── layout/
│   │   └── DashboardLayout.jsx     # Dashboard wrapper with sidebar
│   ├── pages/
│   │   ├── Landing.jsx             # Public landing page
│   │   ├── Dashboard.jsx           # Main dashboard (protected)
│   │   └── Subscription.jsx        # Subscription management (protected)
│   ├── util/
│   │   └── apiEndpoints.js         # Centralized API endpoint definitions
│   ├── App.jsx                     # Root component with routing
│   ├── App.css                     # Global styles
│   └── main.jsx                    # Application entry point
├── .env                            # Environment variables (not committed)
├── .env.example                    # Environment variables template
├── package.json                    # Dependencies and scripts
├── tailwind.config.js              # Tailwind CSS configuration
├── postcss.config.js               # PostCSS configuration
├── vite.config.js                  # Vite configuration
└── index.html                      # HTML entry point
```

---

## 🛠️ Technology Stack

### Core Dependencies

```json
{
  "dependencies": {
    "react": "^18.3.1",
    "react-dom": "^18.3.1",
    "react-router-dom": "^6.26.2",
    "@clerk/clerk-react": "^5.14.2",
    "axios": "^1.7.7",
    "lucide-react": "^0.447.0"
  },
  "devDependencies": {
    "vite": "^5.4.2",
    "tailwindcss": "^3.4.14",
    "@vitejs/plugin-react": "^4.3.1"
  }
}
```

### Key Libraries

| Library | Purpose | Documentation |
|---------|---------|---------------|
| **React** | UI library | https://react.dev/ |
| **Vite** | Build tool & dev server | https://vitejs.dev/ |
| **React Router** | Client-side routing | https://reactrouter.com/ |
| **Clerk** | Authentication provider | https://clerk.com/docs |
| **Axios** | HTTP client | https://axios-http.com/ |
| **Tailwind CSS** | Utility-first CSS | https://tailwindcss.com/ |
| **Lucide React** | Icon library | https://lucide.dev/ |

---

## 🚀 Application Initialization

### Entry Point (main.jsx)

```javascript
import React from 'react'
import ReactDOM from 'react-dom/client'
import App from './App.jsx'
import './App.css'

ReactDOM.createRoot(document.getElementById('root')).render(
  <React.StrictMode>
    <App />
  </React.StrictMode>,
)
```

**What happens:**
1. React renders into DOM element with id="root"
2. StrictMode enables development warnings
3. App component becomes the root

---

## 🔐 Authentication Architecture

### Clerk Integration

SkyBox uses **Clerk** for complete authentication management:

```javascript
import { ClerkProvider } from "@clerk/clerk-react";

<ClerkProvider 
    publishableKey={import.meta.env.VITE_CLERK_PUBLISHABLE_KEY}
    navigate={(to) => navigate(to)}
>
    {/* App content */}
</ClerkProvider>
```

#### Authentication Flow

```
┌─────────────┐
│   User      │
└──────┬──────┘
       │
       ▼
┌─────────────────────────────┐
│  Click Sign In/Sign Up      │
└──────┬──────────────────────┘
       │
       ▼
┌─────────────────────────────┐
│  Clerk Modal Opens          │
│  (Email/Social Login)       │
└──────┬──────────────────────┘
       │
       ▼
┌─────────────────────────────┐
│  User Authenticates         │
└──────┬──────────────────────┘
       │
       ▼
┌─────────────────────────────┐
│  Clerk Issues JWT Token     │
└──────┬──────────────────────┘
       │
       ▼
┌─────────────────────────────┐
│  Redirect to Dashboard      │
└──────┬──────────────────────┘
       │
       ▼
┌─────────────────────────────┐
│  Auto-Register with Backend │
└──────┬──────────────────────┘
       │
       ▼
┌─────────────────────────────┐
│  Dashboard Loads            │
└─────────────────────────────┘
```

### Protected Routes

```javascript
// Public route - redirects if signed in
<Route path="/" element={
    <>
        <SignedOut>
            <Landing />
        </SignedOut>
        <SignedIn>
            <Navigate to="/dashboard" replace />
        </SignedIn>
    </>
} />

// Protected route - requires authentication
<Route path="/dashboard" element={
    <SignedIn>
        <Dashboard />
    </SignedIn>
} />
```

### JWT Token Management

Clerk automatically manages JWT tokens:

```javascript
const { getToken } = useAuth();

// Get fresh token for API calls
const token = await getToken();

// Use in API requests
axios.get('/api/endpoint', {
    headers: {
        'Authorization': `Bearer ${token}`
    }
});
```

---

## 🌐 Routing Architecture

### Route Structure

```javascript
<Router>
  <ClerkProvider>
    <UserCreditsProvider>
      <Routes>
        {/* Public Routes */}
        <Route path="/" element={<Landing />} />
        
        {/* Protected Routes */}
        <Route path="/dashboard" element={<Dashboard />} />
        <Route path="/subscription" element={<Subscription />} />
        
        {/* Catch-all Redirect */}
        <Route path="*" element={<Navigate />} />
      </Routes>
    </UserCreditsProvider>
  </ClerkProvider>
</Router>
```

### Navigation Flow

```
Landing Page (/)
    ├── Sign In → Dashboard
    ├── Sign Up → Dashboard
    └── Already Signed In → Auto-redirect to Dashboard

Dashboard (/dashboard)
    ├── My Files
    ├── Storage Usage
    ├── Quick Actions
    └── Navigate to Subscription

Subscription (/subscription)
    ├── View Plans
    ├── Purchase Plan
    └── Back to Dashboard
```

---

## 🎨 Component Architecture

### Landing Page Components

#### 1. HeroSection.jsx
```javascript
const HeroSection = ({ openSignIn, openSignUp }) => {
    return (
        <section className="hero-section">
            <h1>Secure Cloud Storage</h1>
            <p>Store, manage, and share files effortlessly</p>
            <button onClick={openSignUp}>Get Started</button>
            <button onClick={openSignIn}>Sign In</button>
        </section>
    );
};
```

**Features:**
- Main headline and CTA
- Sign In/Sign Up buttons
- Responsive design

#### 2. FeaturesSection.jsx
```javascript
const FeaturesSection = ({ features }) => {
    return (
        <section className="features-section">
            {features.map(feature => (
                <FeatureCard 
                    key={feature.id}
                    icon={feature.icon}
                    title={feature.title}
                    description={feature.description}
                />
            ))}
        </section>
    );
};
```

**Features:**
- Grid layout of feature cards
- Icons from Lucide React
- Data from assets/data.js

#### 3. PricingSection.jsx
```javascript
const PricingSection = ({ pricingPlans, openSignUp }) => {
    return (
        <section className="pricing-section">
            {pricingPlans.map(plan => (
                <PricingCard 
                    key={plan.id}
                    plan={plan}
                    onSelect={() => openSignUp()}
                />
            ))}
        </section>
    );
};
```

**Features:**
- Display subscription plans
- Highlight recommended plan
- CTA buttons for sign up

---

### Dashboard Components

#### DashboardLayout.jsx

```javascript
const DashboardLayout = ({ activeMenu, children }) => {
    const { user } = useUser();
    const { credits } = useContext(UserCreditsContext);
    
    return (
        <div className="dashboard-layout">
            <Sidebar activeMenu={activeMenu} />
            <main className="dashboard-content">
                <Header user={user} credits={credits} />
                {children}
            </main>
        </div>
    );
};
```

**Structure:**
```
┌─────────────────────────────────────┐
│  Header (User info, Credits)        │
├──────────┬──────────────────────────┤
│          │                          │
│ Sidebar  │   Main Content Area      │
│          │   (Dashboard/Sub pages)  │
│          │                          │
│          │                          │
└──────────┴──────────────────────────┘
```

**Features:**
- Persistent sidebar navigation
- User profile in header
- Credits display
- Responsive mobile menu

---

## 📦 State Management

### UserCreditsContext

Global state for user credits and storage:

```javascript
// Context creation
export const UserCreditsContext = createContext({
    credits: 0,
    setCredits: () => {},
    fetchUserCredits: () => {}
});

// Provider implementation
export const UserCreditsProvider = ({ children }) => {
    const [credits, setCredits] = useState(0);
    const { getToken, isSignedIn } = useAuth();

    const fetchUserCredits = useCallback(async () => {
        if (!isSignedIn) return;
        
        try {
            const token = await getToken();
            const response = await axios.get(API_ENDPOINTS.GET_USER_CREDITS, {
                headers: { 'Authorization': `Bearer ${token}` }
            });
            setCredits(response.data);
        } catch (error) {
            console.error("Error fetching credits:", error);
        }
    }, [getToken, isSignedIn]);

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

**Usage in components:**
```javascript
const { credits, fetchUserCredits } = useContext(UserCreditsContext);

// Display credits
<span>{credits} credits</span>

// Refresh after upload
await uploadFile();
fetchUserCredits();
```

---

## 🔌 API Communication

### API Endpoints Configuration

```javascript
// util/apiEndpoints.js
const BASE_URL = import.meta.env.VITE_API_BASE_URL;

export const apiEndpoints = {
    // User endpoints
    REGISTER: `${BASE_URL}/register`,
    GET_USER_CREDITS: `${BASE_URL}/users/credits`,
    GET_STORAGE_INFO: `${BASE_URL}/users/storage`,
    
    // File endpoints
    UPLOAD_FILE: `${BASE_URL}/files/upload`,
    DOWNLOAD_FILE: `${BASE_URL}/files/download`,
    DELETE_FILE: `${BASE_URL}/files`,
    LIST_FILES: `${BASE_URL}/files/list`,
    
    // Payment endpoints
    CREATE_ORDER: `${BASE_URL}/payments/create-order`,
    VERIFY_PAYMENT: `${BASE_URL}/payments/verify`,
};
```

### API Request Pattern

```javascript
// Standard GET request
const fetchData = async () => {
    try {
        const token = await getToken();
        const response = await axios.get(apiEndpoints.ENDPOINT, {
            headers: {
                'Authorization': `Bearer ${token}`
            }
        });
        return response.data;
    } catch (error) {
        console.error('API Error:', error);
        throw error;
    }
};

// POST request with data
const sendData = async (data) => {
    try {
        const token = await getToken();
        const response = await axios.post(apiEndpoints.ENDPOINT, data, {
            headers: {
                'Authorization': `Bearer ${token}`,
                'Content-Type': 'application/json'
            }
        });
        return response.data;
    } catch (error) {
        console.error('API Error:', error);
        throw error;
    }
};
```

---

## 📤 File Upload Flow

### Complete Upload Process

```javascript
const handleFileUpload = async (file) => {
    // 1. Validate file size
    const MAX_FILE_SIZE = 25 * 1024 * 1024; // 25MB
    if (file.size > MAX_FILE_SIZE) {
        setError('File size exceeds 25MB limit');
        return;
    }

    // 2. Prepare form data
    const formData = new FormData();
    formData.append('file', file);
    formData.append('isPublic', false);

    // 3. Get authentication token
    const token = await getToken();

    // 4. Upload with progress tracking
    try {
        setUploading(true);
        setProgress(0);

        const response = await axios.post(
            apiEndpoints.UPLOAD_FILE,
            formData,
            {
                headers: {
                    'Authorization': `Bearer ${token}`,
                    'Content-Type': 'multipart/form-data'
                },
                timeout: 300000, // 5 minutes
                onUploadProgress: (progressEvent) => {
                    const percentCompleted = Math.round(
                        (progressEvent.loaded * 100) / progressEvent.total
                    );
                    setProgress(percentCompleted);
                }
            }
        );

        // 5. Handle success
        setSuccess('File uploaded successfully!');
        fetchUserCredits(); // Refresh credits
        fetchFiles(); // Refresh file list

    } catch (error) {
        // 6. Handle errors
        if (error.code === 'ECONNRESET') {
            setError('Connection lost. Please try again.');
        } else if (error.response?.status === 413) {
            setError('File too large.');
        } else {
            setError('Upload failed. Please try again.');
        }
    } finally {
        setUploading(false);
        setProgress(0);
    }
};
```

### Upload UI Component

```javascript
// Progress bar component
{uploading && (
    <div className="upload-progress">
        <div className="progress-info">
            <span>Uploading...</span>
            <span>{progress}%</span>
        </div>
        <div className="progress-bar">
            <div 
                className="progress-fill" 
                style={{ width: `${progress}%` }}
            />
        </div>
    </div>
)}
```

---

## 📥 File Download Flow

```javascript
const handleFileDownload = async (fileId, fileName) => {
    try {
        const token = await getToken();
        
        const response = await axios.get(
            `${apiEndpoints.DOWNLOAD_FILE}/${fileId}`,
            {
                headers: {
                    'Authorization': `Bearer ${token}`
                },
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
        console.error('Download error:', error);
        setError('Download failed. Please try again.');
    }
};
```

---

## 💳 Payment Integration

### Razorpay Setup

```javascript
// Load Razorpay script
useEffect(() => {
    const script = document.createElement('script');
    script.src = 'https://checkout.razorpay.com/v1/checkout.js';
    script.async = true;
    script.onload = () => setRazorpayLoaded(true);
    document.body.appendChild(script);
    
    return () => document.body.removeChild(script);
}, []);
```

### Payment Flow

```javascript
const handlePurchase = async (plan) => {
    try {
        // 1. Create order on backend
        const token = await getToken();
        const orderResponse = await axios.post(
            apiEndpoints.CREATE_ORDER,
            {
                planId: plan.id,
                amount: plan.price * 100, // Convert to paise
                currency: "INR",
                storageInMB: plan.storageInMB
            },
            {
                headers: { 'Authorization': `Bearer ${token}` }
            }
        );

        // 2. Configure Razorpay options
        const options = {
            key: import.meta.env.VITE_RAZORPAY_KEY,
            amount: plan.price * 100,
            currency: "INR",
            name: "SkyBox",
            description: `${plan.name} Plan - ${plan.storage}GB`,
            order_id: orderResponse.data.orderId,
            
            // 3. Handle successful payment
            handler: async function (response) {
                try {
                    const verifyResponse = await axios.post(
                        apiEndpoints.VERIFY_PAYMENT,
                        {
                            razorpay_order_id: response.razorpay_order_id,
                            razorpay_payment_id: response.razorpay_payment_id,
                            razorpay_signature: response.razorpay_signature,
                            planId: plan.id
                        },
                        {
                            headers: { 'Authorization': `Bearer ${token}` }
                        }
                    );

                    if (verifyResponse.data.success) {
                        setSuccess('Payment successful! Plan activated.');
                        fetchUserCredits(); // Refresh credits
                    }
                } catch (error) {
                    setError('Payment verification failed.');
                }
            },
            
            prefill: {
                name: user.fullName,
                email: user.primaryEmailAddress
            },
            
            theme: {
                color: "#3B82F6"
            }
        };

        // 4. Open Razorpay checkout
        const razorpay = new window.Razorpay(options);
        razorpay.open();

    } catch (error) {
        console.error('Payment initiation error:', error);
        setError('Failed to initiate payment.');
    }
};
```

---

## 🎭 User Experience Features

### Loading States

```javascript
// Dashboard loading
if (loading) {
    return (
        <div className="loading-container">
            <div className="spinner" />
            <p>Loading your dashboard...</p>
        </div>
    );
}
```

### Error Handling

```javascript
// Error message display
{error && (
    <div className="error-message">
        <AlertCircle size={20} />
        <span>{error}</span>
    </div>
)}

// Success message display
{success && (
    <div className="success-message">
        <Check size={20} />
        <span>{success}</span>
    </div>
)}
```

### Responsive Design

```javascript
// Tailwind breakpoints
<div className="
    w-full 
    md:w-1/2 
    lg:w-1/3 
    p-4 
    md:p-6 
    lg:p-8
">
    {/* Content adapts to screen size */}
</div>
```

---

## 🔧 Configuration Files

### Vite Configuration (vite.config.js)

```javascript
import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    open: true
  },
  build: {
    outDir: 'dist',
    sourcemap: true
  }
})
```

### Tailwind Configuration (tailwind.config.js)

```javascript
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        primary: '#3B82F6',
        secondary: '#8B5CF6'
      }
    },
  },
  plugins: [],
}
```

### Environment Variables (.env.example)

```env
# Clerk Authentication
VITE_CLERK_PUBLISHABLE_KEY=pk_test_your_key_here

# Razorpay Payment Gateway
VITE_RAZORPAY_KEY=rzp_test_your_key_here

# Backend API URL
VITE_API_BASE_URL=http://localhost:8080/api/v1.0
```

---

## 🚀 Build & Deployment

### Development Server

```bash
npm run dev
```
- Starts Vite dev server on http://localhost:5173
- Hot module replacement (HMR) enabled
- Fast refresh for React components

### Production Build

```bash
npm run build
```
- Creates optimized production build
- Output to `dist/` folder
- Minifies and bundles all assets

### Preview Production Build

```bash
npm run preview
```
- Serves the production build locally
- Test before deployment

### Deployment to Vercel

```bash
# Install Vercel CLI
npm i -g vercel

# Deploy
cd skyboxwebapp
vercel
```

**Vercel Configuration:**
- Build Command: `npm run build`
- Output Directory: `dist`
- Install Command: `npm install`

**Environment Variables in Vercel:**
1. Go to Project Settings
2. Add environment variables:
   - `VITE_CLERK_PUBLISHABLE_KEY`
   - `VITE_RAZORPAY_KEY`
   - `VITE_API_BASE_URL` (production backend URL)

### Deployment to Netlify

```bash
# Build locally
npm run build

# Deploy dist/ folder via Netlify CLI or drag-and-drop
```

**Netlify Configuration (netlify.toml):**
```toml
[build]
  command = "npm run build"
  publish = "dist"

[[redirects]]
  from = "/*"
  to = "/index.html"
  status = 200
```

---

## 🐛 Debugging & Development

### Common Issues

#### 1. CORS Errors
**Problem:** API requests blocked by CORS policy
**Solution:** 
- Ensure backend CORS is configured correctly
- Check `Authorization` header is sent
- Verify API URL in `.env`

#### 2. Authentication Failures
**Problem:** 401 Unauthorized errors
**Solution:**
- Verify Clerk publishable key is correct
- Check JWT token is being sent
- Ensure user is signed in

#### 3. Payment Gateway Not Loading
**Problem:** Razorpay script not loading
**Solution:**
- Check internet connection
- Verify Razorpay key in `.env`
- Check browser console for script errors

### Development Tools

```javascript
// Enable React DevTools
// Install: https://react.dev/learn/react-developer-tools

// Enable Clerk DevTools
<ClerkProvider publishableKey={key} devMode={true}>

// Axios request/response logging
axios.interceptors.request.use(request => {
    console.log('Starting Request', request)
    return request
})

axios.interceptors.response.use(response => {
    console.log('Response:', response)
    return response
})
```

---

## 📊 Performance Optimization

### Code Splitting

```javascript
import { lazy, Suspense } from 'react';

// Lazy load components
const Dashboard = lazy(() => import('./pages/Dashboard'));
const Subscription = lazy(() => import('./pages/Subscription'));

// Wrap with Suspense
<Suspense fallback={<Loading />}>
    <Dashboard />
</Suspense>
```

### Image Optimization

```javascript
// Use WebP format
<img src="image.webp" alt="..." loading="lazy" />

// Responsive images
<picture>
    <source srcset="image-large.webp" media="(min-width: 768px)" />
    <source srcset="image-small.webp" />
    <img src="image.jpg" alt="..." />
</picture>
```

### Memoization

```javascript
import { useMemo, useCallback } from 'react';

// Memoize expensive calculations
const expensiveValue = useMemo(() => {
    return computeExpensiveValue(data);
}, [data]);

// Memoize callback functions
const handleClick = useCallback(() => {
    doSomething(value);
}, [value]);
```

---

## 🔒 Security Best Practices

### 1. Environment Variables
- Never commit `.env` files
- Use `.env.example` for documentation
- Rotate keys regularly

### 2. XSS Protection
```javascript
// Sanitize user input
import DOMPurify from 'dompurify';

const sanitizedInput = DOMPurify.sanitize(userInput);
```

### 3. HTTPS Only
```javascript
// Force HTTPS in production
if (import.meta.env.PROD && window.location.protocol !== 'https:') {
    window.location.protocol = 'https:';
}
```

### 4. Token Security
```javascript
// Never store tokens in localStorage
// Clerk handles token storage securely
// Always use getToken() to fetch fresh tokens
```

---

## 📚 Additional Resources

### Documentation Links
- [React Documentation](https://react.dev/)
- [Vite Documentation](https://vitejs.dev/)
- [Clerk Documentation](https://clerk.com/docs)
- [Tailwind CSS Documentation](https://tailwindcss.com/docs)
- [Axios Documentation](https://axios-http.com/docs/intro)
- [Razorpay Documentation](https://razorpay.com/docs/)

### Useful Commands

```bash
# Install dependencies
npm install

# Run development server
npm run dev

# Build for production
npm run build

# Preview production build
npm run preview

# Lint code
npm run lint

# Format code
npm run format
```
---

**Frontend Architecture Documentation - Version 1.0**
*Last Updated: October 2025*