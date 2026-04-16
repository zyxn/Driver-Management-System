interface User {
	id: string;
	email: string;
	name: string;
	role: string;
}

interface AuthState {
	user: User | null;
	token: string | null;
	isAuthenticated: boolean;
}

function createAuthStore() {
	let state = $state<AuthState>({
		user: null,
		token: null,
		isAuthenticated: false
	});

	// Initialize from localStorage
	if (typeof window !== 'undefined') {
		const token = localStorage.getItem('token');
		const userStr = localStorage.getItem('user');

		if (token && userStr) {
			try {
				state.token = token;
				state.user = JSON.parse(userStr);
				state.isAuthenticated = true;
			} catch (e) {
				console.error('Failed to parse user data:', e);
			}
		}
	}

	return {
		get user() {
			return state.user;
		},
		get token() {
			return state.token;
		},
		get isAuthenticated() {
			return state.isAuthenticated;
		},
		setAuth(token: string, user: User) {
			state.token = token;
			state.user = user;
			state.isAuthenticated = true;

			if (typeof window !== 'undefined') {
				localStorage.setItem('token', token);
				localStorage.setItem('user', JSON.stringify(user));
			}
		},
		clearAuth() {
			state.token = null;
			state.user = null;
			state.isAuthenticated = false;

			if (typeof window !== 'undefined') {
				localStorage.removeItem('token');
				localStorage.removeItem('user');
			}
		}
	};
}

export const authStore = createAuthStore();
