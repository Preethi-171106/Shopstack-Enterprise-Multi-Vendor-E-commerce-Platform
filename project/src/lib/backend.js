import axios from 'axios';

// Spring Boot backend base URL for report/analytics APIs.
// In this sandbox the Java backend is not running, so report pages fall back
// to Supabase-computed analytics. When the backend is available, set
// VITE_BACKEND_API_URL to point at it (e.g. http://localhost:8080).
const baseURL = import.meta.env.VITE_BACKEND_API_URL || 'http://localhost:8080';

export const backend = axios.create({
  baseURL,
  timeout: 15000,
  headers: { 'Content-Type': 'application/json' },
});

// Attach JWT from localStorage (Spring backend) if present.
backend.interceptors.request.use((config) => {
  const token = localStorage.getItem('shopstack_backend_token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

backend.interceptors.response.use(
  (res) => res,
  (error) => {
    const message =
      error.response?.data?.message ||
      error.response?.data?.error ||
      error.message ||
      'Backend request failed';
    return Promise.reject(new Error(message));
  }
);
