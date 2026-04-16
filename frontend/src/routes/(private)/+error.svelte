<script lang="ts">
	import { page } from '$app/stores';
	import { Home, RefreshCw, ArrowLeft, LogOut } from 'lucide-svelte';
	import { goto } from '$app/navigation';
	import { browser } from '$app/environment';

	let status = $derived($page.status);
	let message = $derived($page.error?.message || 'An unexpected error occurred');

	function reload() {
		if (browser) {
			window.location.reload();
		}
	}

	function goBack() {
		if (browser) {
			// Check if there's history to go back to
			if (window.history.length > 1) {
				window.history.back();
			} else {
				// If no history, go to dashboard
				goto('/dashboard');
			}
		}
	}

	function goHome() {
		goto('/dashboard');
	}

	function logout() {
		goto('/login');
	}
</script>

<div class="min-h-screen flex items-center justify-center bg-gradient-to-br from-gray-50 to-gray-100 dark:from-gray-900 dark:to-gray-800 px-4">
	<div class="max-w-md w-full text-center">
		<!-- Error Icon -->
		<div class="mb-8">
			<div class="inline-flex items-center justify-center w-24 h-24 rounded-full bg-red-100 dark:bg-red-900/20">
				<svg class="w-12 h-12 text-red-600 dark:text-red-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
					<path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
				</svg>
			</div>
		</div>

		<!-- Error Status -->
		<h1 class="text-6xl font-bold text-gray-900 dark:text-white mb-4">
			{status}
		</h1>

		<!-- Error Title -->
		<h2 class="text-2xl font-semibold text-gray-800 dark:text-gray-200 mb-4">
			{#if status === 404}
				Page Not Found
			{:else if status === 500}
				Internal Server Error
			{:else if status === 403}
				Access Denied
			{:else if status === 401}
				Unauthorized
			{:else}
				Something Went Wrong
			{/if}
		</h2>

		<!-- Error Message -->
		<p class="text-gray-600 dark:text-gray-400 mb-8">
			{message}
		</p>

		<!-- Action Buttons -->
		<div class="flex flex-col sm:flex-row gap-3 justify-center">
			<button 
				onclick={goHome}
				class="inline-flex items-center justify-center gap-2 rounded-md bg-primary px-4 py-2 text-sm font-medium text-primary-foreground hover:bg-primary/90"
			>
				<Home class="w-4 h-4" />
				Dashboard
			</button>
			
			<button 
				onclick={goBack}
				class="inline-flex items-center justify-center gap-2 rounded-md border border-input bg-background px-4 py-2 text-sm font-medium hover:bg-accent hover:text-accent-foreground"
			>
				<ArrowLeft class="w-4 h-4" />
				Go Back
			</button>
			
			{#if status === 401 || status === 403}
				<button 
					onclick={logout}
					class="inline-flex items-center justify-center gap-2 rounded-md border border-input bg-background px-4 py-2 text-sm font-medium hover:bg-accent hover:text-accent-foreground"
				>
					<LogOut class="w-4 h-4" />
					Logout
				</button>
			{:else}
				<button 
					onclick={reload}
					class="inline-flex items-center justify-center gap-2 rounded-md border border-input bg-background px-4 py-2 text-sm font-medium hover:bg-accent hover:text-accent-foreground"
				>
					<RefreshCw class="w-4 h-4" />
					Reload
				</button>
			{/if}
		</div>

		<!-- Additional Help -->
		{#if status === 404}
			<div class="mt-8 text-sm text-gray-500 dark:text-gray-400">
				The page you're looking for doesn't exist or has been moved.
			</div>
		{:else if status === 401 || status === 403}
			<div class="mt-8 text-sm text-gray-500 dark:text-gray-400">
				You don't have permission to access this resource. Please login again.
			</div>
		{:else if status === 500}
			<div class="mt-8 text-sm text-gray-500 dark:text-gray-400">
				We're working on fixing this. Please try again later.
			</div>
		{/if}
	</div>
</div>
