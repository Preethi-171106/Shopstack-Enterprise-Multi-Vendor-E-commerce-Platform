import api from './api';

let wakeUpStarted = false;
let keepAliveInterval = null;

/**
 * Pings the backend /health endpoint in the background to wake up
 * sleeping free-tier containers (e.g. on Render) as soon as the app loads.
 */
export const initServerWakeup = () => {
  if (wakeUpStarted) return;
  wakeUpStarted = true;

  const ping = () => {
    // Non-blocking background health ping
    api.get('/health', { timeout: 60000, _skipRetry: true }).catch(() => {
      // Background ping errors are silently caught;
      // the HTTP request itself triggers the cloud host to wake the container.
    });
  };

  // Immediate ping on app boot
  ping();

  // Periodic keep-alive ping every 10 minutes while document is active
  if (!keepAliveInterval && typeof window !== 'undefined') {
    keepAliveInterval = setInterval(() => {
      if (typeof document !== 'undefined' && document.visibilityState === 'visible') {
        ping();
      }
    }, 10 * 60 * 1000);
  }
};
