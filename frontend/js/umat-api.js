/**
 * UMAT AI Quiz System — Shared API Client
 * Used by login.html, quiz-select.html, exam.html, lecturer-dashboard.html
 */

const UMAT = (() => {
  const API_BASE = 'http://localhost:8080/api';
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

  function onElectronCheatEvent(callback) {
    if (isElectron) {
      window.electronAPI.onCheatEvent(callback);
    }
  }

  function notifyElectronSubmitted() {
    if (isElectron) {
      window.electronAPI.examSubmitted();
    }
  }

  // ── Utilities ────────────────────────────────────────────
  function formatTime(seconds) {
    const m = Math.floor(seconds / 60).toString().padStart(2, '0');
    const s = (seconds % 60).toString().padStart(2, '0');
    return `${m}:${s}`;
  }

  function formatRefNumber(raw) {
    return raw.toUpperCase().trim();
  }

  function validateRefNumber(ref) {
    return /^UMAT\/[A-Z]{2,4}\/\d{2}\/\d{4}$/i.test(ref);
  }

  // ── Public API ───────────────────────────────────────────
  return {
    API_BASE,
    getToken,
    getStudent,
    getLecturer,
    requireStudentAuth,
    requireLecturerAuth,
    logout,
    post,
    get,
    analyzeFrame,
    getProctoringStatus,
    isElectron,
    onElectronCheatEvent,
    notifyElectronSubmitted,
    formatTime,
    formatRefNumber,
    validateRefNumber
  };
})();
