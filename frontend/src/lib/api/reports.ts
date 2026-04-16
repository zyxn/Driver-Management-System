import { fetchWithAuth } from './auth';

const API_BASE_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080/api/v1';

export interface Report {
	id: number;
	user_id: number;
	title: string;
	description: string;
	status: string;
	created_at: string;
	updated_at: string;
}

export interface CreateReportRequest {
	title: string;
	description: string;
	status?: string;
}

export async function getReports(dateStr?: string): Promise<Report[]> {
	const url = dateStr ? `${API_BASE_URL}/reports?date=${dateStr}` : `${API_BASE_URL}/reports`;
	const response = await fetchWithAuth(url);
	
	if (!response.ok) {
		throw new Error('Failed to fetch reports');
	}
	
	const result = await response.json();
	return result.data || [];
}

export async function getReportById(id: number): Promise<Report> {
	const response = await fetchWithAuth(`${API_BASE_URL}/reports/${id}`);
	
	if (!response.ok) {
		throw new Error('Failed to fetch report');
	}
	
	const result = await response.json();
	return result.data;
}

export async function createReport(data: CreateReportRequest): Promise<Report> {
	const response = await fetchWithAuth(`${API_BASE_URL}/reports`, {
		method: 'POST',
		body: JSON.stringify(data)
	});
	
	if (!response.ok) {
		const error = await response.json();
		throw new Error(error.error || 'Failed to create report');
	}
	
	const result = await response.json();
	return result.data;
}

export async function getUserReports(userId: number, dateStr?: string): Promise<Report[]> {
	const url = dateStr ? `${API_BASE_URL}/reports/user/${userId}?date=${dateStr}` : `${API_BASE_URL}/reports/user/${userId}`;
	const response = await fetchWithAuth(url);
	
	if (!response.ok) {
		throw new Error('Failed to fetch user reports');
	}
	
	const result = await response.json();
	return result.data || [];
}
