<script lang="ts">
	import { onMount } from 'svelte';
	import { getAllDrivers, createDriver, updateDriver, deleteDriver, updateUserStatus, type User, type CreateDriverRequest, type UpdateDriverRequest } from '$lib/api/users';
	import * as Table from '$lib/components/ui/table';
	import * as Dialog from '$lib/components/ui/dialog';
	import * as AlertDialog from '$lib/components/ui/alert-dialog';
	import { Badge } from '$lib/components/ui/badge';
	import { Button } from '$lib/components/ui/button';
	import { Input } from '$lib/components/ui/input';
	import { Label } from '$lib/components/ui/label';
	import { toast } from 'svelte-sonner';
	import { Plus, Pencil, Trash2 } from 'lucide-svelte';

	let drivers = $state<User[]>([]);
	let loading = $state(true);
	let error = $state<string | null>(null);
	let showAddDialog = $state(false);
	let showEditDialog = $state(false);
	let showDeleteDialog = $state(false);
	let selectedDriver = $state<User | null>(null);
	let submitting = $state(false);

	// Form state
	let formData = $state({
		username: '',
		email: '',
		password: '',
		full_name: '',
		phone: '',
		license_no: '',
		status: 'inactive' as 'active' | 'inactive'
	});

	async function loadDrivers() {
		try {
			loading = true;
			error = null;
			drivers = await getAllDrivers();
		} catch (err) {
			error = err instanceof Error ? err.message : 'Failed to load drivers';
			toast.error(error);
		} finally {
			loading = false;
		}
	}

	function openAddDialog() {
		formData = {
			username: '',
			email: '',
			password: '',
			full_name: '',
			phone: '',
			license_no: '',
			status: 'inactive'
		};
		showAddDialog = true;
	}

	function openEditDialog(driver: User) {
		selectedDriver = driver;
		formData = {
			username: driver.username,
			email: driver.email,
			password: '',
			full_name: driver.full_name,
			phone: driver.phone,
			license_no: driver.license_no || '',
			status: driver.status
		};
		showEditDialog = true;
	}

	function openDeleteDialog(driver: User) {
		selectedDriver = driver;
		showDeleteDialog = true;
	}

	async function handleAddDriver() {
		try {
			submitting = true;
			const driverData: CreateDriverRequest = {
				username: formData.username,
				email: formData.email,
				password: formData.password,
				full_name: formData.full_name,
				phone: formData.phone,
				license_no: formData.license_no || undefined,
				status: formData.status
			};
			await createDriver(driverData);
			toast.success('Driver added successfully');
			showAddDialog = false;
			await loadDrivers();
		} catch (err) {
			const errorMessage = err instanceof Error ? err.message : 'Failed to add driver';
			toast.error(errorMessage);
		} finally {
			submitting = false;
		}
	}

	async function handleUpdateDriver() {
		if (!selectedDriver) return;
		
		try {
			submitting = true;
			const updates: UpdateDriverRequest = {
				username: formData.username,
				email: formData.email,
				full_name: formData.full_name,
				phone: formData.phone,
				license_no: formData.license_no || undefined,
				status: formData.status
			};
			
			// Only include password if it's been changed
			if (formData.password) {
				updates.password = formData.password;
			}
			
			await updateDriver(selectedDriver.id, updates);
			toast.success('Driver updated successfully');
			showEditDialog = false;
			await loadDrivers();
		} catch (err) {
			const errorMessage = err instanceof Error ? err.message : 'Failed to update driver';
			toast.error(errorMessage);
		} finally {
			submitting = false;
		}
	}

	async function handleDeleteDriver() {
		if (!selectedDriver) return;
		
		try {
			submitting = true;
			await deleteDriver(selectedDriver.id);
			toast.success('Driver deleted successfully');
			showDeleteDialog = false;
			await loadDrivers();
		} catch (err) {
			const errorMessage = err instanceof Error ? err.message : 'Failed to delete driver';
			toast.error(errorMessage);
		} finally {
			submitting = false;
		}
	}

	async function toggleStatus(driver: User) {
		const newStatus = driver.status === 'active' ? 'inactive' : 'active';
		try {
			await updateUserStatus(driver.id, newStatus);
			toast.success(`Driver status updated to ${newStatus}`);
			await loadDrivers();
		} catch (err) {
			const errorMessage = err instanceof Error ? err.message : 'Failed to update driver status';
			toast.error(errorMessage);
		}
	}

	onMount(() => {
		loadDrivers();
	});

	function formatDate(dateString: string) {
		return new Date(dateString).toLocaleDateString('id-ID', {
			year: 'numeric',
			month: 'short',
			day: 'numeric'
		});
	}
</script>

<div class="container mx-auto py-8 px-4">
	<div class="mb-8 flex items-center justify-between">
		<div>
			<h1 class="text-3xl font-bold tracking-tight">Drivers</h1>
			<p class="text-muted-foreground mt-2">Manage and monitor all drivers in the system</p>
		</div>
		<Button onclick={openAddDialog}>
			<Plus class="mr-2 h-4 w-4" />
			Add Driver
		</Button>
	</div>

	{#if loading}
		<div class="flex items-center justify-center py-12">
			<div class="text-center">
				<div class="animate-spin rounded-full h-12 w-12 border-b-2 border-primary mx-auto"></div>
				<p class="mt-4 text-muted-foreground">Loading drivers...</p>
			</div>
		</div>
	{:else if error}
		<div class="rounded-lg border border-destructive/50 bg-destructive/10 p-4">
			<p class="text-destructive">{error}</p>
			<Button onclick={loadDrivers} class="mt-4" variant="outline">Try Again</Button>
		</div>
	{:else if drivers.length === 0}
		<div class="rounded-lg border border-dashed p-12 text-center">
			<p class="text-muted-foreground">No drivers found</p>
			<Button onclick={openAddDialog} class="mt-4" variant="outline">
				<Plus class="mr-2 h-4 w-4" />
				Add Your First Driver
			</Button>
		</div>
	{:else}
		<div class="rounded-md border">
			<Table.Root>
				<Table.Header>
					<Table.Row>
						<Table.Head class="w-[80px]">ID</Table.Head>
						<Table.Head>Full Name</Table.Head>
						<Table.Head>Username</Table.Head>
						<Table.Head>Email</Table.Head>
						<Table.Head>Phone</Table.Head>
						<Table.Head>License No</Table.Head>
						<Table.Head>Status</Table.Head>
						<Table.Head>Joined</Table.Head>
						<Table.Head class="text-right">Actions</Table.Head>
					</Table.Row>
				</Table.Header>
				<Table.Body>
					{#each drivers as driver (driver.id)}
						<Table.Row>
							<Table.Cell class="font-medium">{driver.id}</Table.Cell>
							<Table.Cell class="font-medium">{driver.full_name}</Table.Cell>
							<Table.Cell>{driver.username}</Table.Cell>
							<Table.Cell>{driver.email}</Table.Cell>
							<Table.Cell>{driver.phone}</Table.Cell>
							<Table.Cell>{driver.license_no || '-'}</Table.Cell>
							<Table.Cell>
								{#if driver.status === 'active'}
									<Badge variant="default" class="bg-green-500 hover:bg-green-600">Active</Badge>
								{:else}
									<Badge variant="secondary">Inactive</Badge>
								{/if}
							</Table.Cell>
							<Table.Cell>{formatDate(driver.created_at)}</Table.Cell>
							<Table.Cell class="text-right">
								<div class="flex justify-end gap-2">
									<Button
										onclick={() => toggleStatus(driver)}
										variant="outline"
										size="sm"
									>
										{driver.status === 'active' ? 'Deactivate' : 'Activate'}
									</Button>
									<Button
										onclick={() => openEditDialog(driver)}
										variant="outline"
										size="sm"
									>
										<Pencil class="h-4 w-4" />
									</Button>
									<Button
										onclick={() => openDeleteDialog(driver)}
										variant="destructive"
										size="sm"
									>
										<Trash2 class="h-4 w-4" />
									</Button>
								</div>
							</Table.Cell>
						</Table.Row>
					{/each}
				</Table.Body>
			</Table.Root>
		</div>

		<div class="mt-4 text-sm text-muted-foreground">
			Showing {drivers.length} driver{drivers.length !== 1 ? 's' : ''}
		</div>
	{/if}
</div>

<!-- Add Driver Dialog -->
<Dialog.Root bind:open={showAddDialog}>
	<Dialog.Content class="max-w-[95vw] sm:max-w-[500px]">
		<Dialog.Header>
			<Dialog.Title>Add New Driver</Dialog.Title>
			<Dialog.Description>
				Enter the driver's information below. All fields are required except license number.
			</Dialog.Description>
		</Dialog.Header>
		<div class="grid gap-4 py-4 max-h-[60vh] overflow-y-auto px-1">
			<div class="grid gap-2">
				<Label for="add-username">Username</Label>
				<Input id="add-username" bind:value={formData.username} placeholder="johndoe" required />
			</div>
			<div class="grid gap-2">
				<Label for="add-email">Email</Label>
				<Input id="add-email" type="email" bind:value={formData.email} placeholder="john@example.com" required />
			</div>
			<div class="grid gap-2">
				<Label for="add-password">Password</Label>
				<Input id="add-password" type="password" bind:value={formData.password} placeholder="••••••••" required />
			</div>
			<div class="grid gap-2">
				<Label for="add-fullname">Full Name</Label>
				<Input id="add-fullname" bind:value={formData.full_name} placeholder="John Doe" required />
			</div>
			<div class="grid gap-2">
				<Label for="add-phone">Phone</Label>
				<Input id="add-phone" bind:value={formData.phone} placeholder="+62812345678" required />
			</div>
			<div class="grid gap-2">
				<Label for="add-license">License Number (Optional)</Label>
				<Input id="add-license" bind:value={formData.license_no} placeholder="ABC123456" />
			</div>
			<div class="grid gap-2">
				<Label for="add-status">Status</Label>
				<select id="add-status" bind:value={formData.status} class="flex h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring">
					<option value="inactive">Inactive</option>
					<option value="active">Active</option>
				</select>
			</div>
		</div>
		<Dialog.Footer class="flex flex-col-reverse sm:flex-row sm:justify-end gap-2">
			<Button variant="outline" onclick={() => showAddDialog = false} disabled={submitting} class="w-full sm:w-auto">
				Cancel
			</Button>
			<Button onclick={handleAddDriver} disabled={submitting} class="w-full sm:w-auto">
				{submitting ? 'Adding...' : 'Add Driver'}
			</Button>
		</Dialog.Footer>
	</Dialog.Content>
</Dialog.Root>

<!-- Edit Driver Dialog -->
<Dialog.Root bind:open={showEditDialog}>
	<Dialog.Content class="max-w-[95vw] sm:max-w-[500px]">
		<Dialog.Header>
			<Dialog.Title>Edit Driver</Dialog.Title>
			<Dialog.Description>
				Update the driver's information. Leave password blank to keep it unchanged.
			</Dialog.Description>
		</Dialog.Header>
		<div class="grid gap-4 py-4 max-h-[60vh] overflow-y-auto px-1">
			<div class="grid gap-2">
				<Label for="edit-username">Username</Label>
				<Input id="edit-username" bind:value={formData.username} required />
			</div>
			<div class="grid gap-2">
				<Label for="edit-email">Email</Label>
				<Input id="edit-email" type="email" bind:value={formData.email} required />
			</div>
			<div class="grid gap-2">
				<Label for="edit-password">Password (leave blank to keep unchanged)</Label>
				<Input id="edit-password" type="password" bind:value={formData.password} placeholder="••••••••" />
			</div>
			<div class="grid gap-2">
				<Label for="edit-fullname">Full Name</Label>
				<Input id="edit-fullname" bind:value={formData.full_name} required />
			</div>
			<div class="grid gap-2">
				<Label for="edit-phone">Phone</Label>
				<Input id="edit-phone" bind:value={formData.phone} required />
			</div>
			<div class="grid gap-2">
				<Label for="edit-license">License Number</Label>
				<Input id="edit-license" bind:value={formData.license_no} />
			</div>
			<div class="grid gap-2">
				<Label for="edit-status">Status</Label>
				<select id="edit-status" bind:value={formData.status} class="flex h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring">
					<option value="inactive">Inactive</option>
					<option value="active">Active</option>
				</select>
			</div>
		</div>
		<Dialog.Footer class="flex flex-col-reverse sm:flex-row sm:justify-end gap-2">
			<Button variant="outline" onclick={() => showEditDialog = false} disabled={submitting} class="w-full sm:w-auto">
				Cancel
			</Button>
			<Button onclick={handleUpdateDriver} disabled={submitting} class="w-full sm:w-auto">
				{submitting ? 'Updating...' : 'Update Driver'}
			</Button>
		</Dialog.Footer>
	</Dialog.Content>
</Dialog.Root>

<!-- Delete Confirmation Dialog -->
<AlertDialog.Root bind:open={showDeleteDialog}>
	<AlertDialog.Content>
		<AlertDialog.Header>
			<AlertDialog.Title>Are you sure?</AlertDialog.Title>
			<AlertDialog.Description>
				This will permanently delete the driver <strong>{selectedDriver?.full_name}</strong>. This action cannot be undone.
			</AlertDialog.Description>
		</AlertDialog.Header>
		<AlertDialog.Footer>
			<AlertDialog.Cancel disabled={submitting}>Cancel</AlertDialog.Cancel>
			<AlertDialog.Action onclick={handleDeleteDriver} disabled={submitting}>
				{submitting ? 'Deleting...' : 'Delete'}
			</AlertDialog.Action>
		</AlertDialog.Footer>
	</AlertDialog.Content>
</AlertDialog.Root>
