export const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';

export const API_ENDPOINTS = {
	auth: {
		login: `${API_BASE_URL}/api/auth/login`,
		register: `${API_BASE_URL}/api/auth/register`,
		logout: `${API_BASE_URL}/api/auth/logout`
	},
	locations: {
		list: `${API_BASE_URL}/api/locations`,
		create: `${API_BASE_URL}/api/locations`,
		update: (id: string) => `${API_BASE_URL}/api/locations/${id}`,
		delete: (id: string) => `${API_BASE_URL}/api/locations/${id}`
	},
	reports: {
		list: `${API_BASE_URL}/api/reports`,
		create: `${API_BASE_URL}/api/reports`,
		update: (id: string) => `${API_BASE_URL}/api/reports/${id}`,
		delete: (id: string) => `${API_BASE_URL}/api/reports/${id}`
	}
};
