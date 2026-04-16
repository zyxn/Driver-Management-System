<script lang="ts">
	import {
		FieldGroup,
		Field,
		FieldLabel
	} from '$lib/components/ui/field/index.js';
	import { Input } from '$lib/components/ui/input/index.js';
	import { Button } from '$lib/components/ui/button/index.js';
	import { cn, type WithElementRef } from '$lib/utils.js';
	import type { HTMLFormAttributes } from 'svelte/elements';
	import { goto } from '$app/navigation';
	import { login } from '$lib/api/auth';
	import { authStore } from '$lib/stores/auth';

	let {
		ref = $bindable(null),
		class: className,
		...restProps
	}: WithElementRef<HTMLFormAttributes> = $props();

	const id = $props.id();
	
	let email = $state('');
	let password = $state('');
	let error = $state('');
	let loading = $state(false);

	async function handleSubmit(e: Event) {
		e.preventDefault();
		error = '';
		loading = true;

		try {
			const user = await login({ email, password });
			authStore.setUser(user);
			await goto('/dashboard');
		} catch (err: any) {
			error = err.message || 'Login failed. Please try again.';
		} finally {
			loading = false;
		}
	}
</script>

<form class={cn('flex flex-col gap-6', className)} bind:this={ref} {...restProps} onsubmit={handleSubmit}>
	<FieldGroup>
		<div class="flex flex-col items-center gap-1 text-center">
			<h1 class="text-2xl font-bold">Login to your account</h1>
			<p class="text-balance text-sm text-muted-foreground">
				Enter your email below to login to your account
			</p>
		</div>
		
		{#if error}
			<div class="text-sm text-red-500 text-center">{error}</div>
		{/if}
		
		<Field>
			<FieldLabel for="email-{id}">Email</FieldLabel>
			<Input id="email-{id}" type="email" placeholder="admin@example.com" required bind:value={email} />
		</Field>
		<Field>
			<div class="flex items-center">
				<FieldLabel for="password-{id}">Password</FieldLabel>
				<a href="##" class="ms-auto text-sm underline-offset-4 hover:underline">
					Forgot your password?
				</a>
			</div>
			<Input id="password-{id}" type="password" required bind:value={password} />
		</Field>
		<Field>
			<Button type="submit" disabled={loading}>
				{loading ? 'Loading...' : 'Login'}
			</Button>
		</Field>
	</FieldGroup>
</form>
