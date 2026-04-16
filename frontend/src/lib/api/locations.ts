import { fetchWithAuth } from './auth';

const API_BASE_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080/api/v1';

export interface Location {
	id: number;
	user_id: number;
	user?: {
		id: number;
		username: string;
		full_name: string;
		role: string;
	};
	latitude: number;
	longitude: number;
	speed: number;
	accuracy: number;
	heading?: number;
	altitude?: number;
	recorded_at_utc: string;
	timezone: string;
	recorded_at_local: string;
	created_at: string;
}

export interface TrackLocationRequest {
	user_id?: number;
	latitude: number;
	longitude: number;
	speed?: number;
	accuracy?: number;
	heading?: number;
	altitude?: number;
	timezone: string;
}

export async function trackLocation(data: TrackLocationRequest): Promise<Location> {
	const response = await fetchWithAuth(`${API_BASE_URL}/locations/track`, {
		method: 'POST',
		body: JSON.stringify(data)
	});
	
	if (!response.ok) {
		const error = await response.json();
		throw new Error(error.error || 'Failed to track location');
	}
	
	const result = await response.json();
	return result.data;
}

export async function getLocationHistory(userId: number, limit: number = 100, dateStr?: string): Promise<Location[]> {
	const params = new URLSearchParams();
	if (limit) params.append('limit', limit.toString());
	if (dateStr) params.append('date', dateStr);

	const response = await fetchWithAuth(`${API_BASE_URL}/locations/user/${userId}?${params.toString()}`);
	
	if (!response.ok) {
		throw new Error('Failed to fetch location history');
	}
	
	const result = await response.json();
	return result.data || [];
}

export async function getLatestLocation(userId: number): Promise<Location | null> {
	const response = await fetchWithAuth(`${API_BASE_URL}/locations/user/${userId}/latest`);
	
	if (!response.ok) {
		return null;
	}
	
	const result = await response.json();
	return result.data;
}

export async function getAllDriversLatestLocations(): Promise<Location[]> {
	try {
		const response = await fetchWithAuth(`${API_BASE_URL}/locations/latest`);
		
		if (!response.ok) {
			throw new Error('Failed to fetch latest locations');
		}
		
		const result = await response.json();
		return result.data || [];
	} catch (error) {
		console.error('Error fetching latest locations:', error);
		return [];
	}
}

// WebSocket helper
export function createLocationWebSocket(onMessage: (data: any) => void, onStatusChange?: (status: 'connecting' | 'connected' | 'disconnected') => void) {
	const wsUrl = API_BASE_URL.replace('http', 'ws').replace('/api/v1', '') + '/ws/locations';
	
	let ws: WebSocket | null = null;
	let reconnectTimeout: ReturnType<typeof setTimeout> | null = null;
	let pingInterval: ReturnType<typeof setInterval> | null = null;

	function connect() {
		try {
			ws = new WebSocket(wsUrl);
			onStatusChange?.('connecting');

			ws.onopen = () => {
				console.log('WebSocket connected to:', wsUrl);
				onStatusChange?.('connected');
				
				// Send ping every 30 seconds to keep connection alive
				pingInterval = setInterval(() => {
					if (ws?.readyState === WebSocket.OPEN) {
						ws.send(JSON.stringify({ type: 'ping' }));
					}
				}, 30000);
			};

			ws.onmessage = (event) => {
				try {
					const message = JSON.parse(event.data);
					onMessage(message);
				} catch (error) {
					console.error('Error parsing WebSocket message:', error);
				}
			};

			ws.onerror = (error) => {
				console.error('WebSocket error:', error);
				onStatusChange?.('disconnected');
			};

			ws.onclose = () => {
				console.log('WebSocket disconnected');
				onStatusChange?.('disconnected');
				
				if (pingInterval) {
					clearInterval(pingInterval);
					pingInterval = null;
				}
				
				// Attempt to reconnect after 5 seconds
				reconnectTimeout = setTimeout(() => {
					console.log('Attempting to reconnect...');
					connect();
				}, 5000);
			};
		} catch (error) {
			console.error('Error creating WebSocket:', error);
			onStatusChange?.('disconnected');
		}
	}

	connect();

	return {
		send: (data: any) => {
			if (ws?.readyState === WebSocket.OPEN) {
				ws.send(JSON.stringify(data));
			}
		},
		close: () => {
			if (reconnectTimeout) {
				clearTimeout(reconnectTimeout);
			}
			if (pingInterval) {
				clearInterval(pingInterval);
			}
			if (ws) {
				ws.close();
			}
		}
	};
}
