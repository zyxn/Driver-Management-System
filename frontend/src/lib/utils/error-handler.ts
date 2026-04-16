import { goto } from '$app/navigation';

export class AppError extends Error {
	constructor(
		message: string,
		public statusCode: number = 500,
		public code?: string
	) {
		super(message);
		this.name = 'AppError';
	}
}

export function handleApiError(err: any): never {
	console.error('API Error:', err);

	if (err.response) {
		const status = err.response.status;
		const message = err.response.data?.message || err.message || 'An error occurred';

		// Handle specific status codes
		if (status === 401) {
			// Unauthorized - redirect to login
			goto('/login');
			throw new Error('Session expired. Please login again.');
		}

		if (status === 403) {
			throw new Error('You do not have permission to access this resource.');
		}

		if (status === 404) {
			throw new Error('Resource not found.');
		}

		if (status === 422) {
			throw new Error(message || 'Validation error.');
		}

		if (status >= 500) {
			throw new Error('Server error. Please try again later.');
		}

		throw new Error(message);
	}

	// Network error or other errors
	if (err.request) {
		throw new Error('Unable to connect to server. Please check your connection.');
	}

	throw new Error(err.message || 'An unexpected error occurred.');
}

export function showErrorToast(message: string) {
	// You can integrate with a toast library here
	console.error('Error:', message);
	// Example: toast.error(message);
}

export function logError(error: Error, context?: Record<string, any>) {
	console.error('Error:', {
		message: error.message,
		stack: error.stack,
		context
	});

	// You can send to error tracking service here
	// Example: Sentry.captureException(error, { extra: context });
}
