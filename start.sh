#!/bin/bash
# UMAT AI Quiz System — Quick Launcher (Linux/macOS)

echo "============================================"
echo "  UMAT AI Quiz System — Quick Launcher"
echo "============================================"
echo ""

show_menu() {
  echo "Select what to start:"
  echo "  1. Backend (Spring Boot)"
  echo "  2. Frontend (HTTP Server on :3000)"
  echo "  3. Electron Browser (Dev)"
  echo "  4. Electron Browser (Production)"
  echo "  5. AI Proctoring Setup"
  echo "  6. Start Backend + Frontend together"
  echo "  0. Exit"
  echo ""
  read -p "Enter choice: " choice
  handle_choice "$choice"
}

handle_choice() {
  case $1 in
    1)
      echo "[Starting Backend...]"
      cd backend && mvn spring-boot:run &
      cd ..
      echo "Backend started. API at http://localhost:8080/api"
      ;;
    2)
      echo "[Starting Frontend server on port 3000...]"
      cd frontend && python3 -m http.server 3000 &
      cd ..
      echo "Frontend at http://localhost:3000/login.html"
      ;;
    3)
      echo "[Starting Electron in dev mode...]"
      cd electron && npm run dev &
      cd ..
      ;;
    4)
      echo "[Starting Electron in production kiosk mode...]"
      cd electron && npm start &
      cd ..
      ;;
    5)
      echo "[Setting up AI proctoring...]"
      cd ai
      pip3 install -r requirements.txt
      python3 download_model.py
      cd ..
      ;;
    6)
      echo "[Starting Backend + Frontend...]"
      cd backend && mvn spring-boot:run &
      sleep 3
      cd ../frontend && python3 -m http.server 3000 &
      cd ..
      echo ""
      echo "Both started!"
      echo "  Backend:  http://localhost:8080/api"
      echo "  Frontend: http://localhost:3000/login.html"
      ;;
    0)
      echo "Goodbye!"
      exit 0
      ;;
    *)
      echo "Invalid choice."
      ;;
  esac
  echo ""
  show_menu
}

show_menu
