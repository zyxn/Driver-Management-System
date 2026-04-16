import { writable } from 'svelte/store';
import type { User } from '$lib/api/auth';
import { getCurrentUser, logout as apiLogout } from '$lib/api/auth';
import { goto } from '$app/navigation';

interface AuthState {
	user: User | null;
	loading: boolean;
	initialized: boolean;
}

function createAuthStore() {
	const { subscribe, set, update } = writable<AuthState>({
		user: null,
		loading: false,
		initialized: false
	});

	return {
		subscribe,
		setUser: (user: User | null) => {
			update((state) => ({ ...state, user, initialized: true }));
		},
		setLoading: (loading: boolean) => {
			update((state) => ({ ...state, loading }));
		},
		logout: async () => {
			try {
				await apiLogout();
				set({ user: null, loading: false, initialized: true });
				goto('/login');
			} catch (error) {
				console.error('Logout error:', error);
			}
		},
		checkAuth: async () => {
			update((state) => ({ ...state, loading: true }));
			try {
				const user = await getCurrentUser();
				set({ user, loading: false, initialized: true });
				return user;
			} catch (error) {
				set({ user: null, loading: false, initialized: true });
				return null;
			}
		}
	};
}

export const authStore = createAuthStore();
