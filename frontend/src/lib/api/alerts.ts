import { API_BASE_URL } from './config';

export interface Alert {
	id: number;
	driver_id: number;
	driver_name: string;
	title: string;
	message: string;
	priority: 'low' | 'medium' | 'high' | 'critical';
	status: 'sent' | 'delivered' | 'read';
	alert_type: 'overspeed' | 'teguran' | 'hati_hati' | 'announcement';
	created_at: string;
}

export interface CreateAlertRequest {
	driver_id: number;
	title: string;
	message: string;
	priority: 'low' | 'medium' | 'high' | 'critical';
	alert_type: 'overspeed' | 'teguran' | 'hati_hati' | 'announcement';
}

export interface ApiResponse<T> {
	success: boolean;
	data: T;
	error?: string;
}

export async function sendAlert(alert: CreateAlertRequest): Promise<Alert> {
	const response = await fetch(`${API_BASE_URL}/alerts`, {
		method: 'POST',
		headers: {
			'Content-Type': 'application/json'
		},
		credentials: 'include',
		body: JSON.stringify(alert)
	});

	if (!response.ok) {
		const errorData = await response.json().catch(() => ({ error: 'Failed to send alert' }));
		throw new Error(errorData.error || `HTTP error! status: ${response.status}`);
	}

	const result: ApiResponse<Alert> = await response.json();
	return result.data;
}

export async function getAlerts(): Promise<Alert[]> {
	const response = await fetch(`${API_BASE_URL}/alerts`, {
		credentials: 'include'
	});

	if (!response.ok) {
		throw new Error('Failed to fetch alerts');
	}

	const result: ApiResponse<Alert[]> = await response.json();
	return result.data;
}

export async function getAlertsByDriver(driverId: number): Promise<Alert[]> {
	const response = await fetch(`${API_BASE_URL}/alerts/driver/${driverId}`, {
		credentials: 'include'
	});

	if (!response.ok) {
		throw new Error('Failed to fetch driver alerts');
	}

	const result: ApiResponse<Alert[]> = await response.json();
	return result.data;
}
