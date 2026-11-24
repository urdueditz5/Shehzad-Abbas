package com.example.core.constants

object PakistanConstants {
    const val ADMIN_BOOTSTRAP_EMAIL = "shehzadawrites5@gmail.com"
    const val MAX_VIDEO_UPLOAD_BYTES = 15L * 1024L * 1024L // 15 MB

    val cities = listOf(
        "All Cities",
        "Lahore",
        "Karachi",
        "Islamabad",
        "Rawalpindi",
        "Faisalabad",
        "Multan",
        "Peshawar",
        "Quetta",
        "Sialkot",
        "Gujranwala",
        "Hyderabad",
        "Bahawalpur",
        "Abbottabad",
        "Remote"
    )

    val selectableCities = cities.filter { it != "All Cities" }

    val socialCategories = listOf(
        "All",
        "Community",
        "Tech & AI",
        "Education",
        "Campus Life",
        "Business",
        "Culture"
    )

    val jobCategories = listOf("All", "Government", "Private", "Internship")

    val jobQualifications = listOf("All", "Matric", "Intermediate", "Bachelor", "Master")

    val marketplaceCategories = listOf(
        "All",
        "Electronics",
        "Mobile Phones",
        "Computers",
        "Clothing",
        "Books",
        "Home Products",
        "Furniture",
        "Other"
    )

    val marketplaceConditions = listOf("New", "Used", "Refurbished")

    val serviceCategories = listOf(
        "All",
        "Education and tutoring",
        "Computer services",
        "Mobile repair",
        "Home maintenance",
        "Graphic design",
        "Photography",
        "Freelancing services",
        "Printing services",
        "Other local services"
    )

    val educationCategories = listOf(
        "Matric",
        "Intermediate",
        "FA",
        "FSc",
        "ICS",
        "ICom",
        "University",
        "General Knowledge",
        "English Learning",
        "Computer Skills"
    )

    val newsCategories = listOf(
        "Pakistan",
        "Education",
        "Technology",
        "Business",
        "Sports",
        "Entertainment"
    )

    val notificationCategories = listOf(
        "all",
        "likes",
        "comments",
        "followers",
        "messages",
        "listings",
        "announcements"
    )
}
