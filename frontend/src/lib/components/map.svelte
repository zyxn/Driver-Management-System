<script lang="ts">
	import { onMount, onDestroy } from 'svelte';
	import type { Map as LeafletMap, Marker } from 'leaflet';

	interface Props {
		center?: [number, number];
		zoom?: number;
		markers?: Array<{ 
			lat: number; 
			lng: number; 
			label?: string;
			driverName?: string;
			status?: string;
			speed?: number;
			lastUpdate?: string;
			vehicleType?: string;
			plateNumber?: string;
		}>;
		routes?: Array<{
			driverId: number;
			driverName: string;
			coordinates: [number, number][];
			color?: string;
		}>;
		pickupPoints?: Array<{
			lat: number;
			lng: number;
			address: string;
			customerName: string;
			time: string;
			status: 'completed' | 'current' | 'upcoming';
			orderNumber: string;
		}>;
	}

	let { 
		center = [-6.2088, 106.8456], // Default: Jakarta
		zoom = 13,
		markers = [],
		routes = [],
		pickupPoints = []
	}: Props = $props();

	let mapContainer: HTMLDivElement;
	let mapWrapper: HTMLDivElement;
	let map: LeafletMap | null = null;
	let L: any;
	let markerInstances: Marker[] = [];
	let routeInstances: any[] = [];
	let pickupMarkerInstances: Marker[] = [];
	let isFullscreen = $state(false);

	function toggleFullscreen() {
		if (!isFullscreen) {
			if (mapWrapper.requestFullscreen) {
				mapWrapper.requestFullscreen();
			}
		} else {
			if (document.exitFullscreen) {
				document.exitFullscreen();
			}
		}
	}

	onMount(async () => {
		// Import Leaflet dynamically to avoid SSR issues
		L = await import('leaflet');

		// Initialize map
		map = L.map(mapContainer).setView(center, zoom);

		// Add Google Maps-like tile layer
		L.tileLayer('https://mt1.google.com/vt/lyrs=m&x={x}&y={y}&z={z}', {
			attribution: '&copy; Google Maps',
			maxZoom: 20,
			subdomains: ['mt0', 'mt1', 'mt2', 'mt3']
		}).addTo(map);

		// Custom marker icon with smooth animation
		const createCustomIcon = (label: string = '') => {
			const color = '#3b82f6';
			
			return L.divIcon({
				className: 'custom-marker',
				html: `
					<div class="marker-container">
						<div class="marker-ripple"></div>
						<div class="marker-ripple-2"></div>
						<div class="marker-dot" style="background: ${color};"></div>
						${label ? `<div class="marker-label" style="background: ${color};">${label}</div>` : ''}
					</div>
				`,
				iconSize: [50, 50],
				iconAnchor: [25, 25],
				popupAnchor: [0, -25]
			});
		};

		// Store custom icon creator for use in updateMarkers
		(map as any).createCustomIcon = createCustomIcon;

		// Add markers, routes, and pickup points
		updateMarkers();
		updateRoutes();
		updatePickupPoints();

		// Listen for fullscreen changes
		document.addEventListener('fullscreenchange', () => {
			isFullscreen = !!document.fullscreenElement;
			if (map) {
				setTimeout(() => map?.invalidateSize(), 100);
			}
		});
	});

	onDestroy(() => {
		if (map) {
			map.remove();
			map = null;
		}
	});

	function updateRoutes() {
		if (!map || !L) return;

		// Clear existing routes
		routeInstances.forEach((route) => map?.removeLayer(route));
		routeInstances = [];

		// Add new routes
		routes.forEach((route) => {
			const polyline = L.polyline(route.coordinates, {
				color: route.color || '#3b82f6',
				weight: 4,
				opacity: 0.7,
				smoothFactor: 1
			}).addTo(map);

			polyline.bindPopup(`
				<div class="route-popup">
					<div class="route-popup-title">${route.driverName}</div>
					<div class="route-popup-info">Route Path</div>
				</div>
			`);

			routeInstances.push(polyline);
		});
	}

	function updatePickupPoints() {
		if (!map || !L) return;

		// Clear existing pickup markers
		pickupMarkerInstances.forEach((marker) => marker.remove());
		pickupMarkerInstances = [];

		// Add new pickup points
		pickupPoints.forEach((point) => {
			const statusColors = {
				completed: '#10b981',
				current: '#f59e0b',
				upcoming: '#6b7280'
			};

			const statusIcons = {
				completed: '✓',
				current: '📍',
				upcoming: '○'
			};

			const pickupIcon = L.divIcon({
				className: 'pickup-marker',
				html: `
					<div class="pickup-marker-container">
						<div class="pickup-marker-icon" style="background: ${statusColors[point.status]};">
							${statusIcons[point.status]}
						</div>
					</div>
				`,
				iconSize: [30, 30],
				iconAnchor: [15, 15],
				popupAnchor: [0, -15]
			});

			const marker = L.marker([point.lat, point.lng], {
				icon: pickupIcon
			}).addTo(map);

			const popupContent = `
				<div class="pickup-popup">
					<div class="pickup-popup-header" style="background: ${statusColors[point.status]};">
						<div class="pickup-popup-status">${point.status.toUpperCase()}</div>
						<div class="pickup-popup-order">#${point.orderNumber}</div>
					</div>
					<div class="pickup-popup-body">
						<div class="pickup-popup-row">
							<span class="pickup-popup-label">Customer</span>
							<span class="pickup-popup-value">${point.customerName}</span>
						</div>
						<div class="pickup-popup-row">
							<span class="pickup-popup-label">Time</span>
							<span class="pickup-popup-value">${point.time}</span>
						</div>
						<div class="pickup-popup-row">
							<span class="pickup-popup-label">Address</span>
							<span class="pickup-popup-value">${point.address}</span>
						</div>
					</div>
				</div>
			`;

			marker.bindPopup(popupContent, {
				maxWidth: 280,
				className: 'pickup-popup-wrapper'
			});

			pickupMarkerInstances.push(marker);
		});
	}

	let activeMarkerMap: Record<string, Marker> = {};

	function updateMarkers() {
		if (!map || !L) return;

		const createCustomIcon = (map as any).createCustomIcon;
		if (!createCustomIcon) return;

		const currentLabels = new Set(markers.map(m => m.label || 'unknown'));

		// Remove old markers
		for (const label in activeMarkerMap) {
			if (!currentLabels.has(label)) {
				activeMarkerMap[label].remove();
				delete activeMarkerMap[label];
			}
		}

		// Update or add markers
		markers.forEach((markerData) => {
			const label = markerData.label || 'unknown';
			
			// Create detailed popup content
			const popupContent = `
				<div class="custom-popup">
					<div class="popup-header">
						<div class="popup-driver-name">${markerData.driverName || 'Unknown Driver'}</div>
						<div class="popup-status">
							<span class="status-dot"></span>
							${markerData.status || 'Active'}
						</div>
					</div>
					<div class="popup-body">
						<div class="popup-info-row">
							<span class="popup-info-label">Speed</span>
							<span class="speed-value">${markerData.speed || 0} km/h</span>
						</div>
						<div class="popup-info-row">
							<span class="popup-info-label">Last Update</span>
							<span class="time-value">${markerData.lastUpdate || 'Just now'}</span>
						</div>
						<div class="popup-info-row">
							<span class="popup-info-label">Vehicle</span>
							<span class="popup-info-value">${markerData.vehicleType || 'N/A'}</span>
						</div>
						<div class="popup-info-row">
							<span class="popup-info-label">Plate Number</span>
							<span class="popup-info-value">${markerData.plateNumber || 'N/A'}</span>
						</div>
						<div class="popup-info-row">
							<span class="popup-info-label">Location</span>
							<span class="popup-info-value">${markerData.lat.toFixed(4)}, ${markerData.lng.toFixed(4)}</span>
						</div>
					</div>
				</div>
			`;
			
			if (activeMarkerMap[label]) {
				// Update existing marker
				const marker = activeMarkerMap[label];
				marker.setLatLng([markerData.lat, markerData.lng]);
				marker.setPopupContent(popupContent);
			} else {
				// Create new marker
				const icon = createCustomIcon(label);
				const marker = L.marker([markerData.lat, markerData.lng], {
					icon: icon
				}).addTo(map);
				
				marker.bindPopup(popupContent, {
					maxWidth: 280,
					className: 'custom-popup-wrapper'
				});
				activeMarkerMap[label] = marker;
			}
		});
	}

	// Svelte 5 $effect: must explicitly read the reactive value to track changes
	$effect(() => {
		// Access markers.length to trigger reactivity on markers array changes
		const _len = markers.length;
		const _data = JSON.stringify(markers);
		if (map) {
			updateMarkers();
		}
	});
</script>

<svelte:head>
	<link
		rel="stylesheet"
		href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css"
		integrity="sha256-p4NxAoJBhIIN+hmNHrzRCf9tD/miZyoHS5obTRR9BMY="
		crossorigin=""
	/>
</svelte:head>

<div bind:this={mapWrapper} class="map-wrapper">
	<div bind:this={mapContainer} class="map-container"></div>
	<button onclick={toggleFullscreen} class="fullscreen-btn" title="Toggle Fullscreen">
		{#if isFullscreen}
			<svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
				<path d="M8 3v3a2 2 0 0 1-2 2H3m18 0h-3a2 2 0 0 1-2-2V3m0 18v-3a2 2 0 0 1 2-2h3M3 16h3a2 2 0 0 1 2 2v3"/>
			</svg>
		{:else}
			<svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
				<path d="M8 3H5a2 2 0 0 0-2 2v3m18 0V5a2 2 0 0 0-2-2h-3m0 18h3a2 2 0 0 0 2-2v-3M3 16v3a2 2 0 0 0 2 2h3"/>
			</svg>
		{/if}
	</button>
</div>

<style>
	.map-wrapper {
		position: relative;
		width: 100%;
		height: 100%;
	}

	.map-container {
		width: 100%;
		height: 100%;
		min-height: 400px;
		border-radius: 0.75rem;
		overflow: hidden;
		box-shadow: 0 4px 6px -1px rgb(0 0 0 / 0.1), 0 2px 4px -2px rgb(0 0 0 / 0.1);
	}

	.fullscreen-btn {
		position: absolute;
		top: 10px;
		right: 10px;
		z-index: 1000;
		background: white;
		border: 2px solid rgba(0, 0, 0, 0.2);
		border-radius: 4px;
		width: 34px;
		height: 34px;
		display: flex;
		align-items: center;
		justify-content: center;
		cursor: pointer;
		box-shadow: 0 1px 5px rgba(0, 0, 0, 0.2);
		transition: all 0.2s;
	}

	.fullscreen-btn:hover {
		background: #f4f4f4;
		transform: scale(1.05);
	}

	.fullscreen-btn svg {
		color: #333;
	}

	:global(.leaflet-container) {
		border-radius: 0.75rem;
		background: #f8fafc;
	}

	:global(.map-wrapper:fullscreen) {
		background: #f8fafc;
	}

	:global(.map-wrapper:fullscreen .map-container) {
		border-radius: 0;
		height: 100vh;
		min-height: 100vh;
	}

	:global(.custom-marker) {
		background: none;
		border: none;
	}

	:global(.marker-container) {
		position: relative;
		width: 50px;
		height: 50px;
		display: flex;
		align-items: center;
		justify-content: center;
	}

	:global(.marker-dot) {
		position: relative;
		width: 16px;
		height: 16px;
		border-radius: 50%;
		border: 3px solid white;
		box-shadow: 0 2px 6px rgba(0, 0, 0, 0.3);
		z-index: 2;
		animation: dotPulse 2s ease-in-out infinite;
	}

	:global(.marker-ripple) {
		position: absolute;
		width: 16px;
		height: 16px;
		border-radius: 50%;
		background: rgba(59, 130, 246, 0.4);
		animation: ripple 2s cubic-bezier(0, 0.2, 0.8, 1) infinite;
	}

	:global(.marker-ripple-2) {
		position: absolute;
		width: 16px;
		height: 16px;
		border-radius: 50%;
		background: rgba(59, 130, 246, 0.4);
		animation: ripple 2s cubic-bezier(0, 0.2, 0.8, 1) infinite 1s;
	}

	@keyframes -global-ripple {
		0% {
			transform: scale(1);
			opacity: 1;
		}
		100% {
			transform: scale(3);
			opacity: 0;
		}
	}

	@keyframes -global-dotPulse {
		0%, 100% {
			transform: scale(1);
			box-shadow: 0 2px 6px rgba(0, 0, 0, 0.3);
		}
		50% {
			transform: scale(1.1);
			box-shadow: 0 4px 12px rgba(59, 130, 246, 0.6);
		}
	}

	:global(.marker-label) {
		position: absolute;
		top: -28px;
		left: 50%;
		transform: translateX(-50%);
		padding: 3px 8px;
		border-radius: 4px;
		font-size: 11px;
		font-weight: 700;
		color: white;
		white-space: nowrap;
		box-shadow: 0 2px 4px rgba(0, 0, 0, 0.2);
		z-index: 3;
	}

	:global(.custom-popup) {
		font-family: system-ui, -apple-system, sans-serif;
		min-width: 220px;
		padding: 0;
	}

	:global(.popup-header) {
		background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%);
		color: white;
		padding: 12px;
		border-radius: 8px 8px 0 0;
		margin: -12px -12px 0 -12px;
	}

	:global(.popup-driver-name) {
		font-size: 16px;
		font-weight: 700;
		margin-bottom: 4px;
	}

	:global(.popup-status) {
		font-size: 12px;
		opacity: 0.9;
		display: flex;
		align-items: center;
		gap: 6px;
	}

	:global(.status-dot) {
		width: 8px;
		height: 8px;
		border-radius: 50%;
		background: #10b981;
		animation: statusBlink 2s ease-in-out infinite;
	}

	@keyframes -global-statusBlink {
		0%, 100% { opacity: 1; }
		50% { opacity: 0.4; }
	}

	:global(.popup-body) {
		padding: 12px 0 0 0;
	}

	:global(.popup-info-row) {
		display: flex;
		justify-content: space-between;
		align-items: center;
		padding: 8px 0;
		border-bottom: 1px solid #f1f5f9;
	}

	:global(.popup-info-row:last-child) {
		border-bottom: none;
	}

	:global(.popup-info-label) {
		font-size: 12px;
		color: #64748b;
		font-weight: 500;
	}

	:global(.popup-info-value) {
		font-size: 13px;
		color: #1e293b;
		font-weight: 600;
	}

	:global(.speed-value) {
		color: #3b82f6;
		font-size: 18px;
		font-weight: 700;
	}

	:global(.time-value) {
		color: #10b981;
		font-size: 12px;
	}

	:global(.leaflet-popup-content-wrapper) {
		border-radius: 8px;
		box-shadow: 0 10px 15px -3px rgb(0 0 0 / 0.1), 0 4px 6px -4px rgb(0 0 0 / 0.1);
	}

	:global(.leaflet-popup-tip) {
		box-shadow: 0 3px 14px rgba(0, 0, 0, 0.1);
	}
</style>
