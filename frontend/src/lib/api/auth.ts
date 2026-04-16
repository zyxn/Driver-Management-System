import { handleApiError } from '$lib/utils/error-handler';

const API_BASE_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080/api/v1';

export interface LoginCredentials {
	email: string;
	password: string;
}

export interface User {
	id: number;
	email: string;
	username: string;
	full_name: string;
	phone: string;
	role: string;
}

export interface LoginResponse {
	success: boolean;
	data: {
		user: User;
	};
}

export interface RegisterData {
	email: string;
	password: string;
	username: string;
	full_name: string;
	phone?: string;
}

export async function login(credentials: LoginCredentials): Promise<User> {
	try {
		const response = await fetch(`${API_BASE_URL}/auth/login`, {
			method: 'POST',
			headers: {
				'Content-Type': 'application/json'
			},
			credentials: 'include', // Important: send cookies
			body: JSON.stringify(credentials)
		});

		if (!response.ok) {
			const error = await response.json();
			throw new Error(error.error || 'Login failed');
		}

		const result: LoginResponse = await response.json();
		return result.data.user;
	} catch (err) {
		handleApiError(err);
	}
}

export async function register(data: RegisterData): Promise<User> {
	try {
		const response = await fetch(`${API_BASE_URL}/auth/register`, {
			method: 'POST',
			headers: {
				'Content-Type': 'application/json'
			},
			credentials: 'include',
			body: JSON.stringify(data)
		});

		if (!response.ok) {
			const error = await response.json();
			throw new Error(error.error || 'Registration failed');
		}

		const result = await response.json();
		return result.data;
	} catch (err) {
		handleApiError(err);
	}
}

export async function logout(): Promise<void> {
	await fetch(`${API_BASE_URL}/auth/logout`, {
		method: 'POST',
		credentials: 'include' // Important: send cookies
	});
}

export async function getCurrentUser(): Promise<User | null> {
	try {
		const response = await fetch(`${API_BASE_URL}/auth/me`, {
			method: 'GET',
			credentials: 'include' // Important: send cookies
		});

		if (!response.ok) {
			return null;
		}

		const result = await response.json();
		return result.data;
	} catch (error) {
		return null;
	}
}

// Helper function for authenticated API calls
export async function fetchWithAuth(url: string, options: RequestInit = {}): Promise<Response> {
	const defaultOptions: RequestInit = {
		credentials: 'include', // Always include cookies
		headers: {
			'Content-Type': 'application/json',
			...options.headers
		}
	};

	return fetch(url, { ...defaultOptions, ...options });
}
