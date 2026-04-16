<script lang="ts">
	import LoginForm from '$lib/components/login-form.svelte';
	import { nanoid } from 'nanoid';
	import { onMount } from 'svelte';
	import { goto } from '$app/navigation';
	import { authStore } from '$lib/stores/auth';

	const id = nanoid();

	onMount(() => {
		const unsubscribe = authStore.subscribe((auth) => {
			if (auth.initialized && auth.user) {
				goto('/dashboard');
			}
		});

		return unsubscribe;
	});
</script>

<div class="grid min-h-svh lg:grid-cols-2">
	<div class="flex flex-col gap-4 p-6 md:p-10">
		<div class="flex justify-center gap-2 md:justify-start">
			<a href="/" class="flex items-center gap-2 font-medium">
				<img src="/logo/logo_square.png" alt="Logo" class="size-6 rounded-md" />
				Driver Management System
			</a>
		</div>
		<div class="flex flex-1 items-center justify-center">
			<div class="w-full max-w-xs">
				<LoginForm {id} />
			</div>
		</div>
	</div>
	<div class="relative hidden bg-muted lg:block">
		<img
			src="https://images.unsplash.com/photo-1449965408869-eaa3f722e40d?q=80&w=2070"
			alt="Driver Management"
			class="absolute inset-0 h-full w-full object-cover dark:brightness-[0.2] dark:grayscale"
		/>
	</div>
</div>
