/**
 * UMAT Electron Preload — Secure IPC Bridge
 * Exposes a safe API to the renderer (frontend) process.
 */

const { contextBridge, ipcRenderer } = require('electron');

contextBridge.exposeInMainWorld('electronAPI', {
  // Notify main process that exam was submitted
  examSubmitted: () => ipcRenderer.send('exam-submitted'),

  // Request fullscreen
  requestFullscreen: () => ipcRenderer.send('request-fullscreen'),

  // Get proctor status
  getProctorStatus: () => ipcRenderer.invoke('get-proctor-status'),

  // Listen for cheating events from main process (screenshot, etc.)
  onCheatEvent: (callback) => {
    ipcRenderer.on('cheat-event', (_, data) => callback(data));
  },

  // Listen for screenshot detected
  onScreenshotDetected: (callback) => {
    ipcRenderer.on('screenshot-detected', (_, data) => callback(data));
  },

  // Platform info
  platform: process.platform,
  isElectron: true
});
