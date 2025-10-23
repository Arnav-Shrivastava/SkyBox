import {CreditCard, Files, LayoutDashboard, Receipt, Upload} from "lucide-react";

export const features = [
    {
        iconName: "ArrowUpCircle",
        iconColor: "text-blue-600",
        title: "Easy File Upload",
        description: "Quickly upload your files with our intuitive drag-and-drop interface."
    },
    {
        iconName: "Shield",
        iconColor: "text-blue-600",
        title: "Secure Storage",
        description: "Your files are encrypted and stored securely in our cloud infrastructure."
    },
    {
        iconName: "Share2",
        iconColor: "text-blue-600",
        title: "Simple Sharing",
        description: "Share files with anyone using secure links that you control."
    },
    {
        iconName: "CreditCard",
        iconColor: "text-blue-600",
        title: "Flexible Credits",
        description: "Pay only for what you use with our credit-based system."
    },
    {
        iconName: "FileText",
        iconColor: "text-blue-600",
        title: "File Management",
        description: "Organize, preview, and manage your files from any device."
    },
    {
        iconName: "Clock",
        iconColor: "text-blue-600",
        title: "Transaction History",
        description: "Keep track of all your credit purchases and usage."
    }
];

export const pricingPlans = [
    {
        name: "Free",
        price: "0",
        description: "Perfect for getting started",
        features: [
            "5 file uploads",
            "Upto 125MB total upload size at once",
        ],
        cta: "Get Started",
        highlighted: false
    },
    {
        name: "Premium",
        price: "80",
        description: "For individuals with larger needs",
        features: [
            "Get credits for 210 file uploads",
            "Upto 1GB total upload size",
        ],
        cta: "Go Premium",
        highlighted: true
    },
    {
        name: "Ultimate",
        price: "140",
        description: "For teams and businesses",
        features: [
           "Get credits for 1024 file uploads",
            "Upto 5GB total upload size",
        ],
        cta: "Go Ultimate",
        highlighted: false
    }
];

export const testimonials = [
    {
        name: "Arnav Shrivastava",
        role: "Full Stack Developer",
        company: "Student at SRM IST KTR",
        image: "https://media.licdn.com/dms/image/v2/D5603AQFG-fh-6YqM2g/profile-displayphoto-scale_400_400/B56ZnSc.PKKIAg-/0/1760172434910?e=1762387200&v=beta&t=sO06sDW_gVWVcfToXOVCCb20tqzFUdVlfNDWErYsfto",
        quote: "As a passionate full-stack developer, I thrive on building robust and scalable applications. SkyBox was an exciting challenge, pushing the boundaries of file management with seamless user experience and powerful backend logic. We focused on crafting a secure and efficient platform for creative collaboration.",
    },
    {
        name: "Purusharth Singh",
        role: "Frontend Architect",
        company: "Student at SRM IST KTR",
        image: "https://media.licdn.com/dms/image/v2/D4E03AQHK3BGKWbUXVg/profile-displayphoto-scale_400_400/B4EZlOU4G2IwAg-/0/1757955721842?e=1762387200&v=beta&t=w0mzSyB7DHSg5_mrKx-phCNK7XjUNGUOsC1nohs1CY0",
        quote: "My goal for SkyBox was to create an intuitive and responsive user interface that makes complex tasks feel effortless. Leveraging the latest frontend technologies, I ensured that every drag-and-drop, every click, and every interaction felt natural and performant. Building an engaging experience was key.",
    },
    {
        name: "Abhinav Singh",
        role: "Backend & DevOps Engineer",
        company: "Student at SRM IST KTR",
        image: "https://thumbs.dreamstime.com/b/default-profile-picture-avatar-photo-placeholder-vector-illustration-default-profile-picture-avatar-photo-placeholder-vector-189495158.jpg",
        quote: "From designing the secure API endpoints to optimizing database performance, my focus was on the reliability and scalability of SkyBox. Implementing robust authentication and ensuring seamless data flow were critical. We aimed to deliver a rock-solid foundation that handles user data with integrity and speed.",
    },
    {
        name: "Mehul Chaurasia",
        role: "Frontend Architect",
        company: "Student at SRM IST KTR",
        image: "https://media.licdn.com/dms/image/v2/D5603AQE6nfOhy3jeiA/profile-displayphoto-shrink_100_100/B56ZR1HZAsGsAU-/0/1737131659576?e=1762992000&v=beta&t=GT4J69zniWqoozFvQSzmXge8rdotT_AOf7rGfWsjM2w",
        quote: "My focus was on refining the Graphical User Interface (GUI) to ensure it was visually appealing and highly consistent. Leveraging Tailwind CSS was key to rapidly implementing a modern, polished design. I contributed to establishing a robust and scalable UI component library, ensuring every button, card, and modal was pixel-perfect and responsive. The goal was to make the front end look good and perform flawlessly, providing the user with an aesthetically pleasing and intuitive experience",
    }
];
//side menu bar options
export const SIDE_MENU_DATA = [
    {
        id: "01",
        label: "Dashboard",
        icon: LayoutDashboard,
        path: "/dashboard",
    },
    {
        id: "02",
        label: "Upload",
        icon: Upload,
        path: "/upload",
    },
    {
        id: "03",
        label: "My Files",
        icon: Files,
        path: "/my-files",
    },
    {
        id: "04",
        label: "Subscription",
        icon: CreditCard,
        path: "/subscriptions",
    },
    {
        id: "05",
        label: "Transactions",
        icon: Receipt,
        path: "/transactions",
    }
];
