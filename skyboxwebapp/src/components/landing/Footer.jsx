import React from "react";
const Footer = () => {

    const currentYear = new Date().getFullYear();
    return (
        <footer className="bg-gray-800">
            <div className="max-w-7xl mx-auto py-12 px-4 sm:px-6 lg:px-8">
                <div className="text-center">
                    <p className="text-base font-medium text-gray-400">
                        &copy; {currentYear} SkyBox
                    </p>
                </div>
            </div>
        </footer>
    )
}

export default Footer;