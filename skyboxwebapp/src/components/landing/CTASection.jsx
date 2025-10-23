const CTASection = ({openSignUp}) => {
    return (
        <div className='bg-blue-600'>
            <div className="max-w-7xl mx-auto py-12 px-4 sm:px-6 lg:py-16 lg:px-8 lg:flex lg:items-center lg:justify-between">
                <h2 className="text-3xl font-extrabold tracking-tight text-white sm:text-4xl">
                    <span className="block">
                        Open your SkyBox
                    </span>
                    <span className="block text-blue-100">
                        and get started
                    </span>
                </h2>
                <div className="mt-8 flex lg:mt-0 lg:flex-shrink-0">
                    <div className="inline-flex rounded-md shadow">
                        <button
                            onClick={() => openSignUp()}
                            className='inline-flex items-center justify-center px-5 py-3 border border-transparent text-base font-semibold rounded-md text-blue-600 bg-white hover:bg-blue-50 transition-colors duration-200'>
                            Claim your SkyBox for free
                        </button>
                    </div>
                </div>
            </div>
        </div>
    )
}

export default CTASection;