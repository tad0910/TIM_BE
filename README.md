# AppTim Backend 🚀

![Java](https://img.shields.io/badge/Java-21-orange.svg)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.6-brightgreen.svg)
![MariaDB](https://img.shields.io/badge/MariaDB-Supported-blue.svg)

AppTim is a robust backend system designed to handle Social Media features and Learning Management System (LMS) operations. Built with **Spring Boot 3** and **Java 21**, the system provides high performance, security, and scalability.

## 🌟 Key Features

*   **Identity & Access Management**: Secure authentication and authorization using **Keycloak** & **JWT**. Includes OTP verification via email and password recovery.
*   **Education & Schedule Management**: Comprehensive management of Academic Programs, Modules, Classes, and Instructor assignments (Lecturer, Supporter, Observer roles).
*   **Gamification & Ranking**: Behavior point tracking and dynamic monthly ranking system for users.
*   **Real-time Interactions**: Live notifications powered by **Server-Sent Events (SSE)**.
*   **Media Management**: Seamless image uploads and management integrated with **Cloudinary**.
*   **Automated Services**: RSS News Feed processing, automatic tuition payment reminders via Email, and PDF report generation.

## 🛠️ Tech Stack

*   **Core**: Java 21, Spring Boot 3.3.6
*   **Database**: MariaDB (Production), H2 (Testing)
*   **Security**: Spring Security, Keycloak Admin Client, JJWT
*   **Caching & Optimization**: Caffeine Cache
*   **Utilities**: Lombok, Jsoup (HTML Parser), Rome (RSS), Flying Saucer (PDF generation)
*   **Code Quality**: SonarQube, Jacoco

## 🚀 Getting Started

### Prerequisites
*   JDK 21 or higher
*   Maven 3.8+
*   MariaDB Server
*   Keycloak Server (for authentication)

### Installation & Setup

1. **Clone the repository**
   ```bash
   git clone https://github.com/tad0910/TIM_BE.git
   cd TIM_BE
   ```

2. **Configure Environment Variables**
   Update the `.env` file or `application.properties` with your database credentials, Keycloak server details, and Cloudinary API keys.

3. **Database Initialization**
   The database schema, gamification structure, and sample data can be found in the `DB/` directory. Execute these scripts on your MariaDB instance to initialize the database.

4. **Run the Application**
   ```bash
   mvn spring-boot:run
   ```
   The server will start on port `8081` (default).

## 📚 API Documentation

A complete Postman collection is included in the project for easy API testing and exploration.
*   Import `postman_collection.json` (located in the project root) into your Postman workspace to see all available endpoints and payloads.

---
*Developed by ALM-Team-Tim*
