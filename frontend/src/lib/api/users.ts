const API_BASE_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080/api/v1';

export interface User {
	id: number;
	username: string;
	email: string;
	full_name: string;
	phone: string;
	license_no?: string;
	role: 'driver' | 'operator';
	status: 'active' | 'inactive';
	created_at: string;
	updated_at: string;
}

export interface CreateDriverRequest {
	username: string;
	email: string;
	password: string;
	full_name: string;
	phone: string;
	license_no?: string;
	status?: 'active' | 'inactive';
}

export interface UpdateDriverRequest {
	username?: string;
	email?: string;
	password?: string;
	full_name?: string;
	phone?: string;
	license_no?: string;
	status?: 'active' | 'inactive';
}

export interface ApiResponse<T> {
	success: boolean;
	data: T;
	error?: string;
}

export async function getAllDrivers(): Promise<User[]> {
	const response = await fetch(`${API_BASE_URL}/users/drivers`, {
		credentials: 'include'
	});

	if (!response.ok) {
		throw new Error('Failed to fetch drivers');
	}

	const result: ApiResponse<User[]> = await response.json();
	return result.data;
}

export async function getAllUsers(): Promise<User[]> {
	const response = await fetch(`${API_BASE_URL}/users`, {
		credentials: 'include'
	});

	if (!response.ok) {
		throw new Error('Failed to fetch users');
	}

	const result: ApiResponse<User[]> = await response.json();
	return result.data;
}

export async function createDriver(driver: CreateDriverRequest): Promise<User> {
	const response = await fetch(`${API_BASE_URL}/users/drivers`, {
		method: 'POST',
		headers: {
			'Content-Type': 'application/json'
		},
		credentials: 'include',
		body: JSON.stringify(driver)
	});

	if (!response.ok) {
		const errorData = await response.json().catch(() => ({ error: 'Failed to create driver' }));
		throw new Error(errorData.error || `HTTP error! status: ${response.status}`);
	}

	const result: ApiResponse<User> = await response.json();
	return result.data;
}

export async function updateDriver(id: number, updates: UpdateDriverRequest): Promise<void> {
	const response = await fetch(`${API_BASE_URL}/users/drivers/${id}`, {
		method: 'PUT',
		headers: {
			'Content-Type': 'application/json'
		},
		credentials: 'include',
		body: JSON.stringify(updates)
	});

	if (!response.ok) {
		const errorData = await response.json().catch(() => ({ error: 'Failed to update driver' }));
		throw new Error(errorData.error || `HTTP error! status: ${response.status}`);
	}
}

export async function deleteDriver(id: number): Promise<void> {
	const response = await fetch(`${API_BASE_URL}/users/drivers/${id}`, {
		method: 'DELETE',
		credentials: 'include'
	});

	if (!response.ok) {
		const errorData = await response.json().catch(() => ({ error: 'Failed to delete driver' }));
		throw new Error(errorData.error || `HTTP error! status: ${response.status}`);
	}
}

export async function updateUserStatus(id: number, status: 'active' | 'inactive'): Promise<void> {
	const response = await fetch(`${API_BASE_URL}/users/${id}/status`, {
		method: 'PATCH',
		headers: {
			'Content-Type': 'application/json'
		},
		credentials: 'include',
		body: JSON.stringify({ status })
	});

	if (!response.ok) {
		const errorData = await response.json().catch(() => ({ error: 'Failed to update user status' }));
		throw new Error(errorData.error || `HTTP error! status: ${response.status}`);
	}
}
