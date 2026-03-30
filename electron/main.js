/**
 * UMAT AI Quiz System — Electron Secure Browser
 * ===============================================
 * Kiosk mode, disable all shortcuts, screenshot detection,
 * prevent window close, DevTools disabled in production.
 */

const {
  app, BrowserWindow, globalShortcut, ipcMain,
  dialog, screen, powerSaveBlocker
} = require('electron');
const path = require('path');

// ── CONFIG ────────────────────────────────────────────────────
const FRONTEND_URL = process.env.UMAT_FRONTEND || 'http://localhost:8080/frontend/login.html';
const USE_LOCAL_FILE = !process.env.UMAT_FRONTEND; // use local HTML if no URL set
const IS_DEV = process.argv.includes('--dev');
const PREVENT_CLOSE = !IS_DEV;

let mainWindow = null;
let powerBlockerId = null;

// ── CREATE WINDOW ─────────────────────────────────────────────
function createWindow() {
  const { width, height } = screen.getPrimaryDisplay().workAreaSize;

  mainWindow = new BrowserWindow({
    width: width,
    height: height,
    fullscreen: !IS_DEV,         // True fullscreen kiosk
    kiosk: !IS_DEV,              // Kiosk mode — locks to one app
    resizable: IS_DEV,
    closable: IS_DEV,            // Disable close button in production
    minimizable: false,
    maximizable: false,
    alwaysOnTop: !IS_DEV,
    frame: IS_DEV,               // No window frame in production
    titleBarStyle: 'hidden',
    backgroundColor: '#0c1e4a',
    webPreferences: {
      nodeIntegration: false,
      contextIsolation: true,
      devTools: IS_DEV,           // DevTools only in dev mode
      sandbox: true,
      preload: path.join(__dirname, 'preload.js'),
      webSecurity: true,
      allowRunningInsecureContent: false,
    }
  });

  // ── Load the frontend ─────────────────────────────────────
  if (USE_LOCAL_FILE) {
    mainWindow.loadFile(path.join(__dirname, '..', 'frontend', 'login.html'));
  } else {
    mainWindow.loadURL(FRONTEND_URL);
  }

  // ── Prevent page navigation away from allowed domains ─────
  mainWindow.webContents.on('will-navigate', (event, url) => {
    const allowed = ['localhost', '127.0.0.1', 'file://'];
    const isAllowed = allowed.some(domain => url.includes(domain));
    if (!isAllowed) {
      event.preventDefault();
      notifyRenderer('cheat-event', { type: 'TAB_SWITCH', description: 'Navigation attempt blocked: ' + url });
    }
  });

  // ── Block new windows / popups ────────────────────────────
  mainWindow.webContents.setWindowOpenHandler(() => {
    notifyRenderer('cheat-event', { type: 'TAB_SWITCH', description: 'Popup window attempt blocked' });
    return { action: 'deny' };
  });

  // ── Prevent close unless allowed ─────────────────────────
  mainWindow.on('close', (e) => {
    if (PREVENT_CLOSE) {
      e.preventDefault();
      notifyRenderer('cheat-event', { type: 'ESC_PRESSED', description: 'Attempted to close exam window' });
      dialog.showMessageBox(mainWindow, {
        type: 'warning',
        title: 'UMAT Exam in Progress',
        message: 'You cannot close this window during an exam.\nPlease submit your exam first.',
        buttons: ['Return to Exam']
      });
    }
  });

  // ── Re-enter fullscreen if exited ────────────────────────
  mainWindow.on('leave-full-screen', () => {
    if (!IS_DEV) {
      mainWindow.setFullScreen(true);
      notifyRenderer('cheat-event', { type: 'FULLSCREEN_EXIT', description: 'Fullscreen exit detected and blocked' });
    }
  });

  // ── Block DevTools ────────────────────────────────────────
  mainWindow.webContents.on('devtools-opened', () => {
    if (!IS_DEV) {
      mainWindow.webContents.closeDevTools();
      notifyRenderer('cheat-event', { type: 'DEVTOOLS_OPEN', description: 'DevTools opened and blocked' });
    }
  });

  // Open DevTools in dev mode
  if (IS_DEV) mainWindow.webContents.openDevTools();

  // Prevent sleep during exam
  powerBlockerId = powerSaveBlocker.start('prevent-display-sleep');
}

// ── REGISTER GLOBAL SHORTCUTS ─────────────────────────────────
function registerShortcuts() {
  // Block common screenshot shortcuts
  const blockedShortcuts = [
    'PrintScreen',
    'Ctrl+PrintScreen',
    'Alt+PrintScreen',
    'Meta+PrintScreen',
    'Ctrl+Shift+S',    // Screenshot on some systems
    'Meta+Shift+3',    // macOS screenshot
    'Meta+Shift+4',    // macOS screenshot selection
    'Meta+Shift+5',    // macOS screenshot toolbar
    'Ctrl+W',          // Close tab
    'Ctrl+T',          // New tab
    'Ctrl+N',          // New window
    'Ctrl+R',          // Refresh
    'F5',              // Refresh
    'F11',             // Toggle fullscreen
    'Escape',          // ESC
    'Alt+F4',          // Close window (Windows)
    'Meta+Q',          // Quit (macOS)
    'Ctrl+Alt+Delete', // Task manager attempt
  ];

  blockedShortcuts.forEach(shortcut => {
    try {
      globalShortcut.register(shortcut, () => {
        const isScreenshot = shortcut.toLowerCase().includes('print') ||
                             shortcut.includes('Shift+3') ||
                             shortcut.includes('Shift+4');
        const eventType = isScreenshot ? 'SCREENSHOT_DETECTED' : 'ESC_PRESSED';
        notifyRenderer('cheat-event', {
          type: eventType,
          description: `Blocked shortcut: ${shortcut}`
        });
        if (isScreenshot) {
          showScreenshotAlert();
        }
      });
    } catch (e) {
      // Some shortcuts may fail on certain platforms
    }
  });

  // Alt+Tab is handled by OS; we detect window focus loss instead
  mainWindow?.webContents.on('did-finish-load', () => {
    mainWindow?.focus();
  });
}

// ── SCREENSHOT ALERT ──────────────────────────────────────────
function showScreenshotAlert() {
  notifyRenderer('screenshot-detected', {});
  dialog.showMessageBox(mainWindow, {
    type: 'error',
    title: '⚠️ Screenshot Detected',
    message: 'Taking screenshots during the exam is prohibited.\n\nThis violation has been recorded and reported to your lecturer.',
    buttons: ['I Understand']
  });
}

// ── IPC HANDLERS ──────────────────────────────────────────────
function setupIPC() {
  // Renderer → Main: exam submitted
  ipcMain.on('exam-submitted', () => {
    PREVENT_CLOSE = false; // Allow close after submission
    mainWindow?.setKiosk(false);
    mainWindow?.setAlwaysOnTop(false);
  });

  // Renderer → Main: request fullscreen
  ipcMain.on('request-fullscreen', () => {
    mainWindow?.setFullScreen(true);
  });

  // Renderer → Main: get proctor status
  ipcMain.handle('get-proctor-status', () => {
    return { kiosk: !IS_DEV, fullscreen: mainWindow?.isFullScreen() };
  });
}

// ── NOTIFY RENDERER ───────────────────────────────────────────
function notifyRenderer(channel, data) {
  if (mainWindow && !mainWindow.isDestroyed()) {
    mainWindow.webContents.send(channel, data);
  }
}

// ── APP LIFECYCLE ─────────────────────────────────────────────
app.whenReady().then(() => {
  createWindow();
  registerShortcuts();
  setupIPC();

  app.on('activate', () => {
    if (BrowserWindow.getAllWindows().length === 0) createWindow();
  });
});

app.on('window-all-closed', () => {
  if (powerBlockerId !== null) powerSaveBlocker.stop(powerBlockerId);
  globalShortcut.unregisterAll();
  if (process.platform !== 'darwin') app.quit();
});

app.on('before-quit', () => {
  globalShortcut.unregisterAll();
});

// ── SECOND INSTANCE PREVENTION ────────────────────────────────
const gotLock = app.requestSingleInstanceLock();
if (!gotLock) {
  app.quit();
} else {
  app.on('second-instance', () => {
    if (mainWindow) {
      if (mainWindow.isMinimized()) mainWindow.restore();
      mainWindow.focus();
    }
  });
}
