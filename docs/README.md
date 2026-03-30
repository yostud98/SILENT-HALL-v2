## What's New in Silent Hall

### Dashboard-Based Result System
- **No more popups**: Quiz submission now redirects you directly to your dashboard
- **My Results section**: All past quiz attempts are stored and accessible anytime
- **Result Detail Page** (`result-detail.html`): Full per-question breakdown with answer indicators
- **On-Demand AI Explanations**: Click "Explain with AI" on any question to get an instant AI explanation — explanations are generated only when you ask, not automatically

### New Backend Endpoints
| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET`  | `/api/results/{studentId}` | List all quiz attempts for a student |
| `GET`  | `/api/results/details/{attemptId}` | Full result with all answers |
| `POST` | `/api/ai/explain` | Generate on-demand AI explanation for one question |

### Environment Variables
Add to your `application.properties` or environment:
```
ANTHROPIC_API_KEY=your_key_here
```

---

# 🎓 Silent Hall — UMAT AI Quiz System

> AI-powered, fully proctored examination system for the University of Mines and Technology, Tarkwa.

---

## 📁 Project Structure

```
UMAT-AI-QUIZ/
├── backend/              ← Java Spring Boot REST API
│   ├── src/main/java/com/umat/quiz/
│   │   ├── controller/   ← AuthController, QuizController, LecturerController, RecordingController
│   │   ├── model/        ← Student, Quiz, Question, QuizAttempt, CheatingEvent, Lecturer
│   │   ├── repository/   ← JPA repositories
│   │   ├── security/     ← JwtUtil
│   │   └── config/       ← SecurityConfig, DataSeeder
│   ├── src/main/resources/application.properties
│   ├── pom.xml
│   └── setup.sql         ← MySQL schema
│
├── frontend/             ← HTML/CSS/JS exam UI
│   ├── login.html        ← Student & Lecturer login
│   ├── quiz-select.html  ← Available exams page
│   ├── exam.html         ← Live exam with camera, timer, anti-cheat
│   └── lecturer-dashboard.html ← Full lecturer portal
│
├── ai/                   ← Python AI proctoring engine
│   ├── proctor.py        ← Main AI loop (face, eyes, phone)
│   ├── download_model.py ← YOLOv8 model setup
│   └── requirements.txt
│
├── electron/             ← Secure exam browser
│   ├── main.js           ← Kiosk mode, shortcut blocking
│   ├── preload.js        ← Secure IPC bridge
│   └── package.json
│
├── recordings/           ← Local exam recordings saved here
│
└── docs/
    └── README.md         ← This file
```

---

## 🚀 Quick Start

### Prerequisites

| Tool | Version | Download |
|------|---------|----------|
| Java JDK | 17+ | https://adoptium.net |
| Maven | 3.8+ | https://maven.apache.org |
| MySQL | 8.0+ | https://dev.mysql.com |
| Python | 3.9+ | https://python.org |
| Node.js | 18+ | https://nodejs.org |

---

## 1️⃣ Database Setup

```bash
# Start MySQL and create the database
mysql -u root -p

# In MySQL shell:
CREATE DATABASE umat_quiz CHARACTER SET utf8mb4;
EXIT;

# Run setup script
mysql -u root -p umat_quiz < backend/setup.sql
```

---

## 2️⃣ Backend (Spring Boot)

```bash
cd backend

# Edit database credentials
nano src/main/resources/application.properties
# Set: spring.datasource.username=root
# Set: spring.datasource.password=yourpassword

# Build and run
mvn clean install -DskipTests
mvn spring-boot:run

# Backend runs at: http://localhost:8080/api
```

**On first run**, the `DataSeeder` automatically creates:
- 6 demo students
- 1 demo lecturer (username: `lecturer`, password: `umat2026`)
- 1 demo quiz (Data Structures, CS Level 300)

---

## 3️⃣ Frontend

The frontend is plain HTML — no build step required.

**Option A — Open directly in browser (demo mode):**
```bash
# Just open login.html in your browser
open frontend/login.html
# or: double-click frontend/login.html
```

**Option B — Serve via any HTTP server:**
```bash
# Python quick server
cd frontend
python3 -m http.server 3000
# Visit: http://localhost:3000/login.html
```

**Demo Credentials:**
| Role | Login |
|------|-------|
| Student | `UMAT/CS/21/0042` (any demo reference) |
| Lecturer | `lecturer` / `umat2026` |

---

## 4️⃣ AI Proctoring (Python)

```bash
cd ai

# Install dependencies
pip install -r requirements.txt

# Download YOLOv8 model (~6MB)
python download_model.py

# Run the proctoring engine
python proctor.py \
  --attempt-id 1 \
  --student-ref "UMAT/CS/21/0042" \
  --token "your_jwt_token"

# Headless mode (no GUI window)
python proctor.py --attempt-id 1 --headless
```

**What the AI detects:**
| Detection | Severity | Implementation |
|-----------|---------|----------------|
| No face | +5 pts | OpenCV Haar Cascade |
| Multiple faces | +20 pts | OpenCV Haar Cascade |
| Looking away | +3 pts | Face position analysis |
| Phone detected | +15 pts | YOLOv8 COCO class 67 |

---

## 5️⃣ Electron Secure Browser

```bash
cd electron

# Install dependencies
npm install

# Run in development mode (no kiosk)
npm run dev

# Run in production mode (full kiosk)
npm start

# Build installer
npm run build:win    # Windows .exe
npm run build:mac    # macOS .dmg
npm run build:linux  # Linux .AppImage
```

**Electron Security Features:**
- ✅ True fullscreen / kiosk mode
- ✅ Blocks: Alt+Tab, Ctrl+W, ESC, F12, PrintScreen, Alt+F4
- ✅ Screenshot detection via `globalShortcut`
- ✅ Prevents window close during exam
- ✅ Blocks DevTools in production
- ✅ Prevents navigation to external sites
- ✅ Single instance only

---

## 📡 API Reference

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/api/auth/student/login` | Student login with reference number |
| `POST` | `/api/auth/lecturer/login` | Lecturer login |
| `GET` | `/api/quiz/available?course=&level=` | Get available quizzes |
| `POST` | `/api/quiz/start` | Start a quiz attempt |
| `POST` | `/api/quiz/submit` | Submit answers |
| `POST` | `/api/quiz/cheat-event` | Record a cheating event |
| `POST` | `/api/recording/upload` | Upload exam recording |
| `GET` | `/api/lecturer/dashboard/stats` | Dashboard statistics |
| `GET` | `/api/lecturer/students` | All students (filterable) |
| `GET` | `/api/lecturer/scores/{quizId}` | Quiz results |
| `GET` | `/api/lecturer/flagged` | Flagged students |
| `GET` | `/api/lecturer/chart/distribution/{quizId}` | Score distribution |

---

## ☁️ Cloud Configuration (Optional)

### AWS S3 (Recording Storage)
Edit `application.properties`:
```properties
aws.s3.enabled=true
aws.s3.bucket-name=umat-quiz-recordings
aws.s3.region=us-east-1
aws.access-key=YOUR_AWS_ACCESS_KEY
aws.secret-key=YOUR_AWS_SECRET_KEY
```

### Firebase (Alternative)
```properties
firebase.enabled=true
firebase.storage-bucket=umat-quiz.appspot.com
firebase.credentials-path=./firebase-credentials.json
```

---

## 🔐 Security Notes

- Reference numbers validated server-side with regex: `^UMaT/[A-Z]{2,4}/\d{2}/\d{4}$`
- JWT tokens expire after 24 hours
- Passwords are BCrypt-hashed (strength 10)
- CORS configured for localhost only (update for production)
- Recordings are stored locally; S3 option available
- All cheating events are timestamped and persisted

---

## 🧪 Demo Workflow

1. Start MySQL and run `setup.sql`
2. Start backend: `mvn spring-boot:run`
3. Open `frontend/login.html`
4. Login as student: `UMAT/CS/21/0042`
5. Select "Data Structures Mid-Semester Exam"
6. Allow camera → exam begins
7. Answer questions → submit
8. Login as lecturer: `lecturer` / `umat2026`
9. View results, violations, recordings

---

## 📞 Support

- System: Silent Hall — UMAT AI Quiz System v1.0
- University: University of Mines and Technology, Tarkwa, Ghana
- Year: 2026

---

*This system is designed for educational examination use only.*
