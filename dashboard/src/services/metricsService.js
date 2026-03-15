import axios from 'axios';

const API_BASE = process.env.REACT_APP_API_BASE_URL || 'http://localhost:8080/api';

/**
 * Fetch all dashboard metrics from the backend API.
 *
 * Expected endpoint: GET /api/metrics
 * Response shape:
 * {
 *   poolSize, maxPoolSize, keysFromQuantum, keysFromClassical,
 *   tokensGenerated, tokensValidated, tokensFailed,
 *   alerts: [{ id, level, message, time }],
 *   minerUptime, lastMineTime
 * }
 */
export async function fetchMetrics() {
    const response = await axios.get(`${API_BASE}/metrics`, { timeout: 5000 });
    return response.data;
}

/**
 * Fetch just the entropy pool status.
 */
export async function fetchPoolStatus() {
    const response = await axios.get(`${API_BASE}/pool`, { timeout: 5000 });
    return response.data;
}

/**
 * Fetch recent security alerts.
 */
export async function fetchAlerts() {
    const response = await axios.get(`${API_BASE}/alerts`, { timeout: 5000 });
    return response.data;
}
