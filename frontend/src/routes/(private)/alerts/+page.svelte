<script lang="ts">
	import { onMount } from 'svelte';
	import { getAllDrivers, type User } from '$lib/api/users';
	import { sendAlert, type CreateAlertRequest } from '$lib/api/alerts';
	import * as Card from '$lib/components/ui/card';
	import { Button } from '$lib/components/ui/button';
	import { Input } from '$lib/components/ui/input';
	import { Label } from '$lib/components/ui/label';
	import { Textarea } from '$lib/components/ui/textarea';
	import { toast } from 'svelte-sonner';
	import BellIcon from 'lucide-svelte/icons/bell';
	import UserIcon from 'lucide-svelte/icons/user';
	import AlertTriangleIcon from 'lucide-svelte/icons/alert-triangle';

	let drivers: User[] = $state([]);
	let selectedDriver: User | null = $state(null);
	let loading = $state(false);
	let sending = $state(false);

	// Form state
	let title = $state('');
	let message = $state('');
	let priority = $state<'low' | 'medium' | 'high' | 'critical'>('medium');
	let alertType = $state<'overspeed' | 'teguran' | 'hati_hati' | 'announcement'>('announcement');

	onMount(async () => {
		await loadDrivers();
	});

	async function loadDrivers() {
		try {
			loading = true;
			drivers = await getAllDrivers();
		} catch (error) {
			toast.error('Failed to load drivers');
			console.error(error);
		} finally {
			loading = false;
		}
	}

	async function handleSendAlert() {
		if (!selectedDriver) {
			toast.error('Please select a driver');
			return;
		}

		if (!title.trim() || !message.trim()) {
			toast.error('Please fill in all fields');
			return;
		}

		try {
			sending = true;
			const alertData: CreateAlertRequest = {
				driver_id: selectedDriver.id,
				title: title.trim(),
				message: message.trim(),
				priority,
				alert_type: alertType
			};

			await sendAlert(alertData);
			toast.success('Alert sent successfully to ' + selectedDriver.full_name);
			
			// Reset form
			title = '';
			message = '';
			priority = 'medium';
			alertType = 'announcement';
		} catch (error) {
			toast.error('Failed to send alert');
			console.error(error);
		} finally {
			sending = false;
		}
	}

	function getStatusColor(status: string) {
		return status === 'active' ? 'bg-green-500' : 'bg-gray-400';
	}
</script>

<div class="flex h-full gap-6 p-6">
	<!-- Driver List -->
	<div class="w-1/3">
		<Card.Root class="h-full">
			<Card.Header>
				<Card.Title class="flex items-center gap-2">
					<UserIcon class="h-5 w-5" />
					Select Driver
				</Card.Title>
				<Card.Description>Choose a driver to send alert</Card.Description>
			</Card.Header>
			<Card.Content class="space-y-2 overflow-y-auto max-h-[calc(100vh-250px)]">
				{#if loading}
					<div class="flex items-center justify-center py-8">
						<div class="animate-spin rounded-full h-8 w-8 border-b-2 border-primary"></div>
					</div>
				{:else if drivers.length === 0}
					<div class="text-center py-8 text-muted-foreground">
						<UserIcon class="h-12 w-12 mx-auto mb-2 opacity-50" />
						<p>No drivers found</p>
					</div>
				{:else}
					{#each drivers as driver}
						<button
							class="w-full text-left p-4 rounded-lg border transition-all hover:border-primary {selectedDriver?.id === driver.id ? 'border-primary bg-primary/5' : 'border-border'}"
							onclick={() => selectedDriver = driver}
						>
							<div class="flex items-start justify-between">
								<div class="flex-1">
									<div class="flex items-center gap-2 mb-1">
										<h3 class="font-semibold">{driver.full_name}</h3>
										<div class="h-2 w-2 rounded-full {getStatusColor(driver.status)}"></div>
									</div>
									<p class="text-sm text-muted-foreground">{driver.email}</p>
									{#if driver.phone}
										<p class="text-sm text-muted-foreground">{driver.phone}</p>
									{/if}
									{#if driver.license_no}
										<p class="text-xs text-muted-foreground mt-1">License: {driver.license_no}</p>
									{/if}
								</div>
								{#if selectedDriver?.id === driver.id}
									<BellIcon class="h-5 w-5 text-primary" />
								{/if}
							</div>
						</button>
					{/each}
				{/if}
			</Card.Content>
		</Card.Root>
	</div>

	<!-- Alert Form -->
	<div class="flex-1">
		<Card.Root class="h-full">
			<Card.Header>
				<Card.Title class="flex items-center gap-2">
					<AlertTriangleIcon class="h-5 w-5" />
					Send Alert
				</Card.Title>
				<Card.Description>
					{#if selectedDriver}
						Sending alert to <strong>{selectedDriver.full_name}</strong>
					{:else}
						Select a driver from the list to send an alert
					{/if}
				</Card.Description>
			</Card.Header>
			<Card.Content class="space-y-6">
				{#if selectedDriver}
					<!-- Selected Driver Info -->
					<div class="p-4 rounded-lg bg-muted">
						<div class="flex items-center gap-3">
							<div class="h-12 w-12 rounded-full bg-primary/10 flex items-center justify-center">
								<UserIcon class="h-6 w-6 text-primary" />
							</div>
							<div>
								<h4 class="font-semibold">{selectedDriver.full_name}</h4>
								<p class="text-sm text-muted-foreground">{selectedDriver.email}</p>
							</div>
						</div>
					</div>

					<!-- Alert Type -->
					<div class="space-y-2">
						<Label for="alertType">Alert Type</Label>
						<select
							id="alertType"
							bind:value={alertType}
							class="flex h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2"
						>
							<option value="overspeed">Overspeed</option>
							<option value="teguran">Teguran</option>
							<option value="hati_hati">Hati-hati</option>
							<option value="announcement">Announcement</option>
						</select>
					</div>

					<!-- Priority -->
					<div class="space-y-2">
						<Label for="priority">Priority</Label>
						<select
							id="priority"
							bind:value={priority}
							class="flex h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2"
						>
							<option value="low">Low</option>
							<option value="medium">Medium</option>
							<option value="high">High</option>
							<option value="critical">Critical</option>
						</select>
					</div>

					<!-- Title -->
					<div class="space-y-2">
						<Label for="title">Alert Title</Label>
						<Input
							id="title"
							bind:value={title}
							placeholder="Enter alert title"
							maxlength={100}
						/>
					</div>

					<!-- Message -->
					<div class="space-y-2">
						<Label for="message">Message</Label>
						<Textarea
							id="message"
							bind:value={message}
							placeholder="Enter alert message"
							rows={6}
							class="resize-none"
						/>
						<p class="text-xs text-muted-foreground text-right">
							{message.length} characters
						</p>
					</div>

					<!-- Send Button -->
					<Button
						class="w-full"
						onclick={handleSendAlert}
						disabled={sending || !title.trim() || !message.trim()}
					>
						{#if sending}
							<div class="animate-spin rounded-full h-4 w-4 border-b-2 border-white mr-2"></div>
							Sending...
						{:else}
							<BellIcon class="h-4 w-4 mr-2" />
							Send Alert
						{/if}
					</Button>
				{:else}
					<div class="flex flex-col items-center justify-center py-12 text-center">
						<AlertTriangleIcon class="h-16 w-16 text-muted-foreground/50 mb-4" />
						<h3 class="text-lg font-semibold mb-2">No Driver Selected</h3>
						<p class="text-muted-foreground">
							Please select a driver from the list on the left to send an alert
						</p>
					</div>
				{/if}
			</Card.Content>
		</Card.Root>
	</div>
</div>
