<script lang="ts">
	import { onMount } from 'svelte';
	import { Button } from '$lib/components/ui/button';
	import { AlertTriangle, RefreshCw } from 'lucide-svelte';

	export let fallback: ((error: Error) => any) | undefined = undefined;
	export let onError: ((error: Error) => void) | undefined = undefined;

	let error: Error | null = null;
	let hasError = false;

	function handleError(err: Error) {
		error = err;
		hasError = true;
		console.error('Error caught by boundary:', err);
		
		if (onError) {
			onError(err);
		}
	}

	function reset() {
		error = null;
		hasError = false;
	}

	onMount(() => {
		const errorHandler = (event: ErrorEvent) => {
			event.preventDefault();
			handleError(event.error);
		};

		const rejectionHandler = (event: PromiseRejectionEvent) => {
			event.preventDefault();
			handleError(new Error(event.reason));
		};

		window.addEventListener('error', errorHandler);
		window.addEventListener('unhandledrejection', rejectionHandler);

		return () => {
			window.removeEventListener('error', errorHandler);
			window.removeEventListener('unhandledrejection', rejectionHandler);
		};
	});
</script>

{#if hasError && error}
	{#if fallback}
		{@html fallback(error)}
	{:else}
		<div class="flex items-center justify-center p-8">
			<div class="max-w-md w-full text-center space-y-4">
				<div class="inline-flex items-center justify-center w-16 h-16 rounded-full bg-red-100 dark:bg-red-900/20">
					<AlertTriangle class="w-8 h-8 text-red-600 dark:text-red-400" />
				</div>
				
				<h3 class="text-xl font-semibold text-gray-900 dark:text-white">
					Something went wrong
				</h3>
				
				<p class="text-sm text-gray-600 dark:text-gray-400">
					{error.message}
				</p>
				
				<Button on:click={reset} variant="outline" class="gap-2">
					<RefreshCw class="w-4 h-4" />
					Try Again
				</Button>
			</div>
		</div>
	{/if}
{:else}
	<slot />
{/if}
