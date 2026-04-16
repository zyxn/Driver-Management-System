<script lang="ts">
	import { onMount } from 'svelte';
	import { goto } from '$app/navigation';
	import { authStore } from '$lib/stores/auth';
	import AppSidebar from "$lib/components/app-sidebar.svelte";
	import * as Sidebar from "$lib/components/ui/sidebar/index.js";
	import { Toaster } from '$lib/components/ui/sonner';

	let { children } = $props();

	let authState = $state({ user: null, loading: true, initialized: false });

	authStore.subscribe((state) => {
		authState = state;
	});

	onMount(async () => {
		// Wait for auth check if not initialized
		if (!authState.initialized) {
			await authStore.checkAuth();
		}
		
		// Redirect to login if not authenticated
		if (authState.initialized && !authState.user) {
			goto('/login');
		}
	});
</script>

{#if authState.loading}
	<div class="flex items-center justify-center h-screen">
		<div class="text-lg">Loading...</div>
	</div>
{:else if authState.user}
	<Sidebar.Provider>
		<AppSidebar />
		<Sidebar.Inset>
			{@render children()}
		</Sidebar.Inset>
	</Sidebar.Provider>
	<Toaster />
{/if}
