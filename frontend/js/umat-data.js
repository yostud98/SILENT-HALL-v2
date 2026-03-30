/**
 * UMAT Programs, Courses and Levels
 * All official UMAT programs as of 2026
 */
const UMAT_PROGRAMS = {
  "Computer Science": {
    label: "BSc Computer Science and Engineering",
    department: "Computer Science",
    code: "CS",
    courses: ["Data Structures", "Algorithms", "Operating Systems", "Database Systems",
              "Computer Networks", "Software Engineering", "Artificial Intelligence",
              "Computer Architecture", "Web Technologies", "Discrete Mathematics"]
  },
  "Mining Engineering": {
    label: "BSc Mining Engineering",
    department: "Mining Engineering",
    code: "ME",
    courses: ["Rock Mechanics", "Mine Planning", "Mineral Processing", "Blasting Engineering",
              "Mining Geology", "Environmental Mining", "Mine Safety", "Hydrogeology"]
  },
  "Electrical Engineering": {
    label: "BSc Electrical/Electronic Engineering",
    department: "Electrical Engineering",
    code: "EE",
    courses: ["Circuit Theory", "Electromagnetics", "Power Systems", "Control Systems",
              "Electronics", "Signal Processing", "Telecommunications", "Instrumentation"]
  },
  "Civil Engineering": {
    label: "BSc Civil Engineering",
    department: "Civil Engineering",
    code: "CE",
    courses: ["Structural Analysis", "Soil Mechanics", "Hydraulics", "Highway Engineering",
              "Concrete Technology", "Surveying", "Construction Management", "Foundation Engineering"]
  },
  "Metallurgical Engineering": {
    label: "BSc Metallurgical Engineering",
    department: "Metallurgical Engineering",
    code: "MT",
    courses: ["Extractive Metallurgy", "Physical Metallurgy", "Corrosion Engineering",
              "Heat Treatment", "Metallurgical Thermodynamics", "Casting & Forming"]
  },
  "Geomatic Engineering": {
    label: "BSc Geomatic Engineering",
    department: "Geomatic Engineering",
    code: "GE",
    courses: ["Geodesy", "Remote Sensing", "GIS", "Photogrammetry",
              "Land Surveying", "Cartography", "GPS Technology", "Land Law"]
  },
  "Environmental Science": {
    label: "BSc Environmental Science",
    department: "Environmental Science",
    code: "ES",
    courses: ["Environmental Chemistry", "Ecology", "Pollution Control", "EIA",
              "Waste Management", "Climate Change", "Environmental Law"]
  },
  "Petroleum Engineering": {
    label: "BSc Petroleum Engineering",
    department: "Petroleum Engineering",
    code: "PE",
    courses: ["Reservoir Engineering", "Drilling Engineering", "Production Engineering",
              "Petroleum Geology", "Well Logging", "Enhanced Oil Recovery"]
  }
};

const UMAT_LEVELS = [100, 200, 300, 400];

const API_BASE = 'http://localhost:8080/api';

// ── Auth helpers ─────────────────────────────────────────────
function getToken() { return localStorage.getItem('umat_token') || ''; }
function getStudent() { const s = localStorage.getItem('umat_student'); return s ? JSON.parse(s) : null; }
function getLecturer() { const l = localStorage.getItem('umat_lecturer'); return l ? JSON.parse(l) : null; }
function authHeaders() { return { 'Content-Type': 'application/json', 'Authorization': `Bearer ${getToken()}` }; }

function requireStudentAuth() {
  if (!getToken() || !getStudent()) { window.location.href = 'login.html'; return false; }
  return true;
}
function requireLecturerAuth() {
  if (!getToken() || !getLecturer()) { window.location.href = 'login.html'; return false; }
  return true;
}
function logout() { localStorage.clear(); window.location.href = 'login.html'; }

async function apiFetch(path, options = {}) {
  const res = await fetch(`${API_BASE}${path}`, {
    headers: authHeaders(),
    ...options
  });
  const data = await res.json().catch(() => ({}));
  if (!res.ok) throw new Error(data.error || `HTTP ${res.status}`);
  return data;
}

function formatDate(dt) {
  if (!dt) return '—';
  return new Date(dt).toLocaleString('en-GB', { day:'2-digit', month:'short', year:'numeric', hour:'2-digit', minute:'2-digit' });
}
function formatDateShort(dt) {
  if (!dt) return '—';
  return new Date(dt).toLocaleDateString('en-GB', { day:'2-digit', month:'short', year:'numeric' });
}
