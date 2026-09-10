import axios from "axios";

/**
 * Axios instance for all ShopStack backend API calls.
 *
 * Base URL resolution order:
 *   1. VITE_API_BASE_URL environment variable (set in .env.development or .env.production)
 *   2. http://localhost:8080/api  — Vite dev server fallback
 *   3. /api                      — Production (served via Nginx reverse proxy)
 */
const apiBaseUrl =
    import.meta.env.VITE_API_BASE_URL ||
    (import.meta.env.DEV ? "http://localhost:8080/api" : "/api");

const api = axios.create({
    baseURL: apiBaseUrl,
    headers: {
        "Content-Type": "application/json",
    },
    // Timeout after 15 seconds so users see a clear error instead of spinning forever
    timeout: 15000,
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

// ── Response Interceptor: Normalize error messages ───────────────────────────
api.interceptors.response.use(
    (response) => response,
    (error) => {
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
            // No response at all — backend is unreachable
            if (error.code === "ECONNABORTED") {
                userMessage = "Request timed out. The server is taking too long to respond. Please try again.";
            } else if (error.message?.toLowerCase().includes("network")) {
                userMessage =
                    "Unable to connect to the server. " +
                    "Please make sure the backend is running on port 8080 (http://localhost:8080).";
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
