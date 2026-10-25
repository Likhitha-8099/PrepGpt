# PrepGPT - AI-Powered Mock Interview Platform

PrepGPT is an AI-powered mock interview platform designed specifically for students preparing for campus placements, technical interviews, coding challenges, DSA, HR, behavioral, project defense, and resume-based interviews.

---

## 🛠️ Technology Stack

- **Frontend**: React 18, JavaScript (ES6+), React Router DOM (v6), Vanilla CSS (Design System Tokens), Axios, Vite
- **Backend**: Java 17, Spring Boot 3.2.5, Spring Web, Spring Data JPA, Hibernate, Bean Validation, Maven, Springdoc OpenAPI (Swagger UI)
- **Database**: MySQL 8.0+

---

## 🏗️ Architecture Overview

PrepGPT uses a clean layered architecture with a decoupled React SPA frontend and a Spring Boot REST API backend.

```
PrepGPT/
├── backend/            # Spring Boot REST API application (Port 8080)
│   ├── src/main/java/com/prepgpt
│   │   ├── config/      # Web MVC CORS & Swagger OpenAPI configurations
│   │   ├── controller/  # REST API Controllers (Health, Auth, Interview, etc.)
│   │   ├── service/     # Business logic & abstraction interfaces
│   │   ├── repository/  # Spring Data JPA repositories
│   │   ├── entity/      # JPA Hibernate entities
│   │   ├── dto/         # Request & response data transfer objects
│   │   ├── exception/   # Global Exception Handler & error DTOs
│   │   ├── security/    # Security abstractions
│   │   ├── ai/          # AI Service abstraction & providers
│   │   ├── voice/       # Voice STT/TTS service abstraction
│   │   └── util/        # Utility classes
│   └── pom.xml
├── frontend/           # React SPA application built with Vite (Port 5173)
│   ├── src/
│   │   ├── assets/      # Global CSS design tokens & styles
│   │   ├── components/  # Reusable UI components (Navbar, Footer, Buttons, etc.)
│   │   ├── pages/       # Router Pages (Landing, Login, Register, Dashboard, NotFound)
│   │   ├── layouts/     # Layout wrappers
│   │   ├── services/    # Axios HTTP client & Health API services
│   │   ├── hooks/       # Custom React hooks
│   │   ├── context/     # State management contexts
│   │   └── utils/       # Formatting & helper utilities
│   └── package.json
├── database/           # MySQL schema scripts
│   └── schema.sql
├── .gitignore          # Root gitignore rules
├── .env.example        # Environment variables template
└── README.md
```

---

## 📋 Prerequisites

- **Java Development Kit (JDK)**: Java 17+ installed (`java -version`)
- **Apache Maven**: Maven 3.8+ installed (`mvn -version`)
- **Node.js & npm**: Node 18+ installed (`node -version`, `npm -version`)
- **MySQL Database Server**: MySQL 8.0+ running on port 3306 (`mysql --version`)

---

## 🗄️ MySQL Database Setup

1. Start your MySQL Server service.
2. Run the initialization script in `database/schema.sql` or create the database manually:
   ```sql
   CREATE DATABASE IF NOT EXISTS prepgpt;
   ```

---

## ⚙️ Environment Variables Setup

### 1. Backend Environment Configuration (`backend/.env`)
Copy `backend/.env.example` to `backend/.env` and update your MySQL password:
```env
DB_HOST=localhost
DB_PORT=3306
DB_NAME=prepgpt
DB_USERNAME=root
DB_PASSWORD=your_mysql_password
SERVER_PORT=8080
```

### 2. Frontend Environment Configuration (`frontend/.env`)
Copy `frontend/.env.example` to `frontend/.env`:
```env
VITE_API_BASE_URL=http://localhost:8080/api/v1
```

---

## 🚀 How to Run the Application

### 1. Start the Spring Boot Backend

Navigate to the `backend` folder and run:
```bash
cd backend
mvn clean compile
mvn spring-boot:run
```
The backend will start at: `http://localhost:8080`

### 2. Start the React Frontend

Navigate to the `frontend` folder and run:
```bash
cd frontend
npm install
npm run dev
```
The frontend will start at: `http://localhost:5173`

---

## 🔍 Swagger & Health APIs

- **Backend Health Check Endpoint**:  
  `GET http://localhost:8080/api/v1/health`  
  Response:
  ```json
  {
    "status": "UP",
    "application": "PrepGPT"
  }
  ```

- **Swagger / OpenAPI Documentation**:  
  Interactive API docs: `http://localhost:8080/swagger-ui.html`  
  OpenAPI JSON spec: `http://localhost:8080/v3/api-docs`

---

## 🧪 Phase 1 Verification Status

- [x] Spring Boot Maven application compiled successfully (`mvn clean compile`).
- [x] Backend started & connected to MySQL database `prepgpt`.
- [x] REST Health endpoint `GET /api/v1/health` returning `UP`.
- [x] Swagger UI loaded at `http://localhost:8080/swagger-ui.html`.
- [x] Frontend React application built & running on Vite (`http://localhost:5173`).
- [x] React Router routes verified (`/`, `/login`, `/register`, `/dashboard`, `*`).
- [x] Dashboard page communicating with backend `/api/v1/health` displaying `Backend Status: Connected`.
