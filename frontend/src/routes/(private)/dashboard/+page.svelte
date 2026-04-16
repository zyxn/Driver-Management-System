<script lang="ts">
	import * as Breadcrumb from "$lib/components/ui/breadcrumb/index.js";
	import { Separator } from "$lib/components/ui/separator/index.js";
	import * as Sidebar from "$lib/components/ui/sidebar/index.js";
	import * as Card from '$lib/components/ui/card/index.js';
	import Map from '$lib/components/map.svelte';
	import { onMount, onDestroy } from 'svelte';
	import { createLocationWebSocket, type Location } from '$lib/api/locations';

	interface MarkerData {
		lat: number;
		lng: number;
		label: string;
		driverName: string;
		status: string;
		speed: number;
		lastUpdate: string;
		vehicleType: string;
		plateNumber: string;
		recordedAt: string;
	}

	let markers = $state<MarkerData[]>([]);
	let wsConnection: ReturnType<typeof createLocationWebSocket> | null = null;
	let connectionStatus = $state<'connecting' | 'connected' | 'disconnected'>('disconnected');
	let totalDrivers = $state(0);
	let activeTrips = $state(0);
	let totalReports = $state(0);

	function formatLastUpdate(recordedAtUtc: string) {
		const now = new Date();
		const recordedAt = new Date(recordedAtUtc);
		const diffMs = now.getTime() - recordedAt.getTime();
		const diffSecs = Math.floor(diffMs / 1000);
		const diffMins = Math.floor(diffSecs / 60);
		const diffHours = Math.floor(diffMins / 60);
		
		if (diffHours > 0) {
			return `${diffHours} hr${diffHours > 1 ? 's' : ''} ago`;
		} else if (diffMins > 0) {
			return `${diffMins} min${diffMins > 1 ? 's' : ''} ago`;
		} else if (diffSecs > 5) {
			return `${diffSecs} sec${diffSecs > 1 ? 's' : ''} ago`;
		}
		return 'Just now';
	}

	function handleLocationUpdate(location: Location) {
		const lastUpdate = formatLastUpdate(location.recorded_at_utc);

		const newMarker: MarkerData = {
			lat: location.latitude,
			lng: location.longitude,
			label: `D${location.user_id}`,
			driverName: location.user?.full_name || location.user?.username || `Driver ${location.user_id}`,
			status: location.speed > 5 ? 'On Trip' : 'Available',
			speed: Math.round(location.speed),
			lastUpdate,
			vehicleType: 'N/A',
			plateNumber: 'N/A',
			recordedAt: location.recorded_at_utc
		};

		// Update or add marker
		const existingIndex = markers.findIndex(m => m.label === newMarker.label);
		if (existingIndex >= 0) {
			markers[existingIndex] = newMarker;
		} else {
			markers = [...markers, newMarker];
		}

		// Update stats
		totalDrivers = markers.length;
		activeTrips = markers.filter(m => m.status === 'On Trip').length;
	}

	let timeUpdateInterval: ReturnType<typeof setInterval>;

	onMount(async () => {
		// Load initial locations first
		await loadInitialLocations();
		
		// Then connect to WebSocket for real-time updates
		wsConnection = createLocationWebSocket(
			(message) => {
				if (message.type === 'location_update') {
					handleLocationUpdate(message.data);
				} else if (message.type === 'connected') {
					console.log('Connected to location tracking:', message.data?.message);
				}
			},
			(status) => {
				connectionStatus = status;
			}
		);

		// Periodically update the "lastUpdate" text so it shows seconds dynamically
		timeUpdateInterval = setInterval(() => {
			if (markers.length === 0) return;
			markers = markers.map(m => {
				const lastUpdate = formatLastUpdate(m.recordedAt);
				
				// Only return a new object if there is a change, else keep original (better performance)
				if (m.lastUpdate === lastUpdate) return m;
				return { ...m, lastUpdate };
			});
		}, 5000);
	});

	onDestroy(() => {
		if (wsConnection) {
			wsConnection.close();
		}
		if (timeUpdateInterval) {
			clearInterval(timeUpdateInterval);
		}
	});

	async function loadInitialLocations() {
		try {
			const { getAllDriversLatestLocations } = await import('$lib/api/locations');
			const locations = await getAllDriversLatestLocations();
			
			console.log('Loaded initial locations:', locations.length);
			
			// Convert locations to markers
			locations.forEach(location => {
				handleLocationUpdate(location);
			});
		} catch (error) {
			console.error('Error loading initial locations:', error);
		}
	}
</script>

<header
	class="flex h-16 shrink-0 items-center gap-2 transition-[width,height] ease-linear group-has-data-[collapsible=icon]/sidebar-wrapper:h-12"
>
	<div class="flex items-center gap-2 px-4">
		<Sidebar.Trigger class="-ms-1" />
		<Separator orientation="vertical" class="me-2 data-[orientation=vertical]:h-4" />
		<Breadcrumb.Root>
			<Breadcrumb.List>
				<Breadcrumb.Item class="hidden md:block">
					<Breadcrumb.Link href="/dashboard">Dashboard</Breadcrumb.Link>
				</Breadcrumb.Item>
				<Breadcrumb.Separator class="hidden md:block" />
				<Breadcrumb.Item>
					<Breadcrumb.Page>Overview</Breadcrumb.Page>
				</Breadcrumb.Item>
			</Breadcrumb.List>
		</Breadcrumb.Root>
	</div>
</header>

<div class="flex flex-1 flex-col gap-4 p-4 pt-0">
	<div class="flex gap-4 items-start">
		<Card.Root class="w-fit">
			<Card.Content class="p-4">
				<div class="flex gap-6">
					<div class="text-center">
						<div class="text-2xl font-bold mb-1">{totalDrivers}</div>
						<div class="text-xs text-muted-foreground">Total Drivers</div>
					</div>
					<Separator orientation="vertical" class="h-auto" />
					<div class="text-center">
						<div class="text-2xl font-bold mb-1">{activeTrips}</div>
						<div class="text-xs text-muted-foreground">Active Trips</div>
					</div>
					<Separator orientation="vertical" class="h-auto" />
					<div class="text-center">
						<div class="text-2xl font-bold mb-1">{totalReports}</div>
						<div class="text-xs text-muted-foreground">Reports</div>
					</div>
				</div>
			</Card.Content>
		</Card.Root>

		<Card.Root class="w-fit">
			<Card.Content class="p-4">
				<div class="flex items-center gap-2">
					<div class="flex items-center gap-2">
						<div class="w-2 h-2 rounded-full {connectionStatus === 'connected' ? 'bg-green-500 animate-pulse' : connectionStatus === 'connecting' ? 'bg-yellow-500 animate-pulse' : 'bg-red-500'}"></div>
						<span class="text-xs text-muted-foreground">
							{connectionStatus === 'connected' ? 'Live Tracking Active' : connectionStatus === 'connecting' ? 'Connecting...' : 'Disconnected'}
						</span>
					</div>
				</div>
			</Card.Content>
		</Card.Root>
	</div>
	
	<div class="bg-muted/50 min-h-[calc(100vh-220px)] flex-1 rounded-xl p-6">
		<div class="flex justify-between items-center mb-4">
			<h3 class="text-xl font-semibold">Driver Locations (Real-time)</h3>
			{#if markers.length === 0}
				<span class="text-sm text-muted-foreground">Waiting for location updates...</span>
			{:else}
				<span class="text-sm text-muted-foreground">{markers.length} driver{markers.length !== 1 ? 's' : ''} tracked</span>
			{/if}
		</div>
		<div class="h-[calc(100vh-300px)]">
			<Map center={[-6.2088, 106.8456]} zoom={13} {markers} />
		</div>
	</div>
</div>
