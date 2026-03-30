/* PRODUCTION RAILWAY API URL */
const API_BASE = window.location.hostname === 'localhost' 
  ? 'http://localhost:8080/api'  /* Local development */
  : `${window.location.protocol}//${window.location.hostname}/api`;  /* Railway production */

console.log('🚀 UMAT API Base:', API_BASE);

const UMAT = (() => {
  const PROCTOR_BASE = 'http://localhost:5555';

  // ── Auth helpers ─────────────────────────────────────────
  function getToken() {
    return localStorage.getItem('umat_token') || '';
  }

  function getStudent() {
    const s = localStorage.getItem('umat_student');
    return s ? JSON.parse(s) : null;
  }

  function getLecturer() {
    const l = localStorage.getItem('umat_lecturer');
    return l ? JSON.parse(l) : null;
  }

  function authHeaders() {
    return {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${getToken()}`
    };
  }

  function requireStudentAuth() {
    if (!getToken() || !getStudent()) {
      window.location.href = 'login.html';
      return false;
    }
    return true;
  }

  function requireLecturerAuth() {
    if (!getToken() || !getLecturer()) {
      window.location.href = 'login.html';
      return false;
    }
    return true;
  }

  function logout() {
    localStorage.clear();
    window.location.href = 'login.html';
  }

  // ── API calls ────────────────────────────────────────────
  async function post(path, body) {
    const res = await fetch(`${API_BASE}${path}`, {
      method: 'POST',
      headers: authHeaders(),
      body: JSON.stringify(body)
    });
    const data = await res.json();
    if (!res.ok) throw new Error(data.error || `HTTP ${res.status}`);
    return data;
  }

  async function get(path) {
    const res = await fetch(`${API_BASE}${path}`, {
      headers: authHeaders()
    });
    const data = await res.json();
    if (!res.ok) throw new Error(data.error || `HTTP ${res.status}`);
    return data;
  }

  async function apiFetch(path, opts = {}) {
    const res = await fetch(`${API_BASE}${path}`, {
      headers: authHeaders(),
      ...opts
    });
    const data = await res.json();
    if (!res.ok) throw new Error(data.error || data.message || `HTTP ${res.status}`);
    return data;
  }

  // ── Proctor bridge ───────────────────────────────────────
  async function analyzeFrame(canvas) {
    try {
      const b64 = canvas.toDataURL('image/jpeg', 0.5).split(',')[1];
      const res = await fetch(`${PROCTOR_BASE}/analyze`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ frame: b64 })
      });
      return await res.json();
    } catch (e) {
      return null; // Bridge not running — use client-side only
    }
  }

  async function getProctoringStatus() {
    try {
      const res = await fetch(`${PROCTOR_BASE}/status`);
      return await res.json();
    } catch (e) {
      return null;
    }
  }

  // ── Electron integration ─────────────────────────────────
  const isElectron = typeof window.electronAPI !== 'undefined';

  return {
    API_BASE, isElectron, getToken, getStudent, getLecturer, 
    authHeaders, requireStudentAuth, requireLecturerAuth, logout,
    post, get, apiFetch, analyzeFrame, getProctoringStatus
  };
})();

// Legacy alias for compatibility
function apiFetch(path, opts) { return UMAT.apiFetch(path, opts); }
