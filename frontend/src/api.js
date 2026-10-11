
const API_BASE_URL = "http://localhost:8080/api";

async function apiRequest(path) {
    const response = await fetch(`${API_BASE_URL}${path}`);

    if (!response.ok) {
        throw new Error(`API request failed: ${response.status}`);
    }

    return response.json();
}

export async function getMonitors(page = 0, size = 100) {
    const data = await apiRequest(
        `/monitors?page=${page}&size=${size}`
    );

    return data.content ?? data;
}

export async function getMonitorStats(monitorId) {
    return apiRequest(`/monitors/${monitorId}/stats`);
}

export async function getMonitorChecks(monitorId, page = 0, size = 10) {
    return apiRequest(
        `/monitors/${monitorId}/checks?page=${page}&size=${size}`
    );
}

export async function getActiveMonitors(page = 0, size = 100) {
    const data = await apiRequest(
        `/monitors/active?page=${page}&size=${size}`
    );

    return data.content ?? data;
}
