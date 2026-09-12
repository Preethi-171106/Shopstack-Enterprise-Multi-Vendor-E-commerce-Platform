import axios from "axios";

/**
 * Axios instance for all ShopStack backend API calls.
 *
 * Base URL resolution order:
 *   1. VITE_API_BASE_URL environment variable (set in .env.development or .env.production)
 *   2. http://localhost:8080/api  — Vite dev server fallback
 *   3. https://shopstack-enterprise-multi-vendor-e.onrender.com/api — Cloud production fallback
 */
const apiBaseUrl =
    import.meta.env.VITE_API_BASE_URL ||
    (import.meta.env.DEV ? "http://localhost:8080/api" : "https://shopstack-enterprise-multi-vendor-e.onrender.com/api");

const api = axios.create({
    baseURL: apiBaseUrl,
    headers: {
        "Content-Type": "application/json",
    },
    // 90-second timeout to accommodate free-tier cloud hosting cold starts (Render/Railway)
    timeout: 90000,
});

// ── Request Interceptor: Attach JWT Bearer token if present ──────────────────
api.interceptors.request.use(
    (config) => {
        const token = localStorage.getItem("shopstack_token");
        if (token) {
            config.headers.Authorization = `Bearer ${token}`;
        }
        return config;
    },
    (error) => Promise.reject(error)
);

// ── Response Interceptor: Auto-Retry on Cold-Start / Network Glitches & Normalize error messages
api.interceptors.response.use(
    (response) => response,
    async (error) => {
        const config = error.config;

        // Auto-retry on cold-start timeouts (ECONNABORTED), Network errors, or 502/503/504 Gateway errors
        const isNetworkOrTimeout = !error.response && (error.code === "ECONNABORTED" || error.message?.toLowerCase().includes("network") || error.code === "ERR_NETWORK");
        const isServerError = error.response && [502, 503, 504].includes(error.response.status);

        if (config && !config._skipRetry && (isNetworkOrTimeout || isServerError)) {
            config._retryCount = config._retryCount || 0;
            const maxRetries = 2;

            if (config._retryCount < maxRetries) {
                config._retryCount += 1;
                const delayMs = config._retryCount * 2000;
                await new Promise((resolve) => setTimeout(resolve, delayMs));
                return api(config);
            }
        }

        // 401: clear stale token so the user is redirected to login
        if (error.response?.status === 401) {
            const token = localStorage.getItem("shopstack_token");
            if (token) {
                localStorage.removeItem("shopstack_token");
                localStorage.removeItem("shopstack_user");
            }
        }

        // Build a user-friendly message from the most specific source available
        let userMessage;

        if (!error.response) {
            // No response at all — backend is unreachable or timed out
            if (error.code === "ECONNABORTED") {
                userMessage = "The cloud server is taking longer than expected to respond (waking up from sleep mode). Please try again in a moment.";
            } else if (error.message?.toLowerCase().includes("network") || error.code === "ERR_NETWORK") {
                userMessage =
                    "Unable to connect to the backend server. " +
                    "The cloud instance may be waking up. Please try again in a few seconds.";
            } else {
                userMessage = "A network error occurred. Please check your connection and try again.";
            }
        } else {
            const status = error.response.status;
            const data = error.response.data;

            // Try to get the most specific message from the backend response
            const backendMessage =
                data?.message ||
                data?.error ||
                (typeof data === "string" ? data : null);

            if (status === 400) {
                // Validation errors include fieldErrors map
                if (data?.fieldErrors && Object.keys(data.fieldErrors).length > 0) {
                    const firstField = Object.keys(data.fieldErrors)[0];
                    userMessage = data.fieldErrors[firstField];
                } else {
                    userMessage = backendMessage || "Invalid input. Please check the form and try again.";
                }
            } else if (status === 401) {
                userMessage = backendMessage || "Invalid email or password.";
            } else if (status === 403) {
                userMessage = backendMessage || "You do not have permission to perform this action.";
            } else if (status === 404) {
                userMessage = backendMessage || "The requested resource was not found.";
            } else if (status === 409) {
                // Duplicate resource (e.g., email already registered)
                userMessage = backendMessage || "This resource already exists. Please use a different value.";
            } else if (status === 502 || status === 503 || status === 504) {
                userMessage = "Cloud server is currently starting up. Please try again in a few moments.";
            } else if (status >= 500) {
                userMessage =
                    backendMessage ||
                    "A server error occurred. Please try again later. " +
                    "If the problem persists, contact support.";
            } else {
                userMessage = backendMessage || error.message || "An unexpected error occurred.";
            }
        }

        error.userMessage = userMessage;
        return Promise.reject(error);
    }
);

export default api;
