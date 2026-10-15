# KnowledgeHub AI - Deployment & Local Setup Guide

This guide explains how to run KnowledgeHub AI locally using Docker Compose or standalone developer mode on Windows PowerShell and Linux/macOS.

---

## 1. Prerequisites

1. **Java 17+** (OpenJDK 17 LTS recommended)
2. **Node.js 18+** & **npm**
3. **Maven 3.9+**
4. **Ollama** (for local AI generation):
   Download and install Ollama from [https://ollama.ai](https://ollama.ai).
   Pull the recommended models:
   ```bash
   ollama pull llama3.2
   ollama pull nomic-embed-text
   ```

---

## 2. Option A: Docker Compose (Production Environment)

To launch the full stack (PostgreSQL with pgvector, Backend, Frontend) with a single command:

1. Clone or navigate to the project directory:
   ```bash
   cd "AI chatbot"
   ```
2. Copy environment file:
   ```bash
   cp .env.example .env
   ```
3. Start the containers:
   ```bash
   docker compose up -d --build
   ```
4. Access the applications:
   - **Frontend UI**: [http://localhost:3000](http://localhost:3000)
   - **Backend API**: [http://localhost:8080](http://localhost:8080)
   - **OpenAPI / Swagger**: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)

---

## 3. Option B: Local Developer Mode (Windows PowerShell)

For rapid local iteration without Docker:

### Step 1: Start Backend (Spring Boot with embedded H2 or local Postgres)
```powershell
cd backend
mvn spring-boot:run
```
The backend will initialize storage directories under `backend/data/` and seed the default administrator user.

### Step 2: Start Frontend (Vite Dev Server)
In a second terminal:
```powershell
cd frontend
npm install
npm run dev
```
Open [http://localhost:5173](http://localhost:5173).

---

## 4. Option C: Local Developer Mode (Linux / macOS)

### Step 1: Start Backend
```bash
cd backend
mvn spring-boot:run
```

### Step 2: Start Frontend
```bash
cd frontend
npm install
npm run dev
```

---

## 5. Default Credentials

The system seeds default credentials on startup:
- **Administrator**:
  - Username: `admin`
  - Password: `Admin123!`
- **Demo User**:
  - Username: `demo`
  - Password: `Demo123!`
