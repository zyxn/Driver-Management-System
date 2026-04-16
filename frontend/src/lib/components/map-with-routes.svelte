<script lang="ts">
	import { onMount, onDestroy } from 'svelte';
	import type { Map as LeafletMap, Marker, Polyline } from 'leaflet';

	interface PickupPoint {
		lat: number;
		lng: number;
		type: 'pickup' | 'delivery' | 'checkpoint' | 'refuel' | 'rest';
		location: string;
		address: string;
		timestamp: string;
		notes: string;
		photoUrl?: string;
	}

	interface RouteData {
		driverId: number;
		driverName: string;
		points: Array<{ lat: number; lng: number }>;
		color?: string;
	}

	interface Props {
		center?: [number, number];
		zoom?: number;
		routes?: RouteData[];
		pickupPoints?: PickupPoint[];
	}

	let { 
		center = [-6.2088, 106.8456],
		zoom = 12,
		routes = [],
		pickupPoints = []
	}: Props = $props();

	let mapContainer: HTMLDivElement;
	let mapWrapper: HTMLDivElement;
	let map: LeafletMap | null = null;
	let L: any;
	let routeLines: Polyline[] = [];
	let pickupMarkers: Marker[] = [];
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

	function getPickupIcon(type: string) {
		const icons: Record<string, string> = {
			pickup: '📦',
			delivery: '🎯',
			checkpoint: '🚩',
			refuel: '⛽',
			rest: '☕'
		};
		return icons[type] || '📍';
	}

	function getPickupColor(type: string) {
		const colors: Record<string, string> = {
			pickup: '#2196F3',
			delivery: '#4CAF50',
			checkpoint: '#9C27B0',
			refuel: '#F44336',
			rest: '#FF9800'
		};
		return colors[type] || '#757575';
	}

	function getPickupLabel(type: string) {
		const labels: Record<string, string> = {
			pickup: 'Pickup',
			delivery: 'Delivery',
			checkpoint: 'Checkpoint',
			refuel: 'Refuel',
			rest: 'Rest'
		};
		return labels[type] || type;
	}

	onMount(async () => {
		L = await import('leaflet');

		map = L.map(mapContainer).setView(center, zoom);

		L.tileLayer('https://mt1.google.com/vt/lyrs=m&x={x}&y={y}&z={z}', {
			attribution: '&copy; Google Maps',
			maxZoom: 20,
			subdomains: ['mt0', 'mt1', 'mt2', 'mt3']
		}).addTo(map);

		updateMapContent();

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

	function updateMapContent() {
		if (!map || !L) return;

		// Clear existing overlays
		routeLines.forEach((line) => line.remove());
		pickupMarkers.forEach((marker) => marker.remove());
		routeLines = [];
		pickupMarkers = [];

		// Add routes (polylines)
		routes.forEach((route) => {
			if (route.points.length > 1) {
				const latLngs = route.points.map(p => L.latLng(p.lat, p.lng));
				const polyline = L.polyline(latLngs, {
					color: route.color || '#2196F3',
					weight: 4,
					opacity: 0.7,
					smoothFactor: 1
				}).addTo(map);
				
				routeLines.push(polyline);
			}
		});

		// Add pickup point markers
		pickupPoints.forEach((point, index) => {
			const color = getPickupColor(point.type);
			const icon = L.divIcon({
				className: 'custom-pickup-marker',
				html: `
					<div class="pickup-marker-container">
						<div class="pickup-marker-dot" style="background: ${color}; border-color: ${color};">
							<span class="pickup-icon">${getPickupIcon(point.type)}</span>
						</div>
						<div class="pickup-marker-number" style="background: ${color};">${index + 1}</div>
					</div>
				`,
				iconSize: [40, 40],
				iconAnchor: [20, 40],
				popupAnchor: [0, -40]
			});

			const marker = L.marker([point.lat, point.lng], { icon }).addTo(map);
			
			const popupContent = `
				<div class="pickup-popup">
					<div class="pickup-popup-header" style="background: ${color};">
						<div class="pickup-popup-type">${getPickupLabel(point.type)}</div>
						<div class="pickup-popup-location">${point.location}</div>
					</div>
					<div class="pickup-popup-body">
						<div class="pickup-popup-row">
							<span class="pickup-popup-label">⏰ Waktu</span>
							<span class="pickup-popup-value">${point.timestamp}</span>
						</div>
						<div class="pickup-popup-row">
							<span class="pickup-popup-label">📍 Alamat</span>
							<span class="pickup-popup-value">${point.address}</span>
						</div>
						<div class="pickup-popup-row">
							<span class="pickup-popup-label">📝 Catatan</span>
							<span class="pickup-popup-value">${point.notes}</span>
						</div>
						${point.photoUrl ? '<div class="pickup-popup-photo"><img src="' + point.photoUrl + '" alt="Photo" /></div>' : ''}
					</div>
				</div>
			`;
			
			marker.bindPopup(popupContent, {
				maxWidth: 300,
				className: 'pickup-popup-wrapper'
			});
			
			pickupMarkers.push(marker);
		});

		// Fit bounds to show all markers
		if (pickupMarkers.length > 0) {
			const group = L.featureGroup(pickupMarkers);
			map.fitBounds(group.getBounds().pad(0.1));
		}
	}

	// Svelte 5 $effect: explicitly read values to enforce reactivity tracking
	$effect(() => {
		const _routesLen = routes.length;
		const _pickupsLen = pickupPoints.length;
		const _r = JSON.stringify(routes);
		const _p = JSON.stringify(pickupPoints);
		if (map) {
			updateMapContent();
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

	:global(.custom-pickup-marker) {
		background: none;
		border: none;
	}

	:global(.pickup-marker-container) {
		position: relative;
		width: 40px;
		height: 40px;
		display: flex;
		align-items: center;
		justify-content: center;
	}

	:global(.pickup-marker-dot) {
		width: 36px;
		height: 36px;
		border-radius: 50%;
		border: 3px solid white;
		box-shadow: 0 2px 8px rgba(0, 0, 0, 0.3);
		display: flex;
		align-items: center;
		justify-content: center;
		position: relative;
		z-index: 2;
	}

	:global(.pickup-icon) {
		font-size: 18px;
	}

	:global(.pickup-marker-number) {
		position: absolute;
		top: -8px;
		right: -8px;
		width: 20px;
		height: 20px;
		border-radius: 50%;
		color: white;
		font-size: 11px;
		font-weight: 700;
		display: flex;
		align-items: center;
		justify-content: center;
		border: 2px solid white;
		box-shadow: 0 2px 4px rgba(0, 0, 0, 0.2);
		z-index: 3;
	}

	:global(.pickup-popup) {
		font-family: system-ui, -apple-system, sans-serif;
		min-width: 250px;
	}

	:global(.pickup-popup-header) {
		color: white;
		padding: 12px;
		border-radius: 8px 8px 0 0;
		margin: -12px -12px 0 -12px;
	}

	:global(.pickup-popup-type) {
		font-size: 11px;
		opacity: 0.9;
		text-transform: uppercase;
		letter-spacing: 0.5px;
		font-weight: 600;
	}

	:global(.pickup-popup-location) {
		font-size: 16px;
		font-weight: 700;
		margin-top: 4px;
	}

	:global(.pickup-popup-body) {
		padding: 12px 0 0 0;
	}

	:global(.pickup-popup-row) {
		display: flex;
		flex-direction: column;
		gap: 4px;
		padding: 8px 0;
		border-bottom: 1px solid #f1f5f9;
	}

	:global(.pickup-popup-row:last-child) {
		border-bottom: none;
	}

	:global(.pickup-popup-label) {
		font-size: 11px;
		color: #64748b;
		font-weight: 600;
	}

	:global(.pickup-popup-value) {
		font-size: 13px;
		color: #1e293b;
		font-weight: 500;
	}

	:global(.pickup-popup-photo) {
		margin-top: 8px;
		border-radius: 8px;
		overflow: hidden;
	}

	:global(.pickup-popup-photo img) {
		width: 100%;
		height: auto;
		display: block;
	}

	:global(.leaflet-popup-content-wrapper) {
		border-radius: 8px;
		box-shadow: 0 10px 15px -3px rgb(0 0 0 / 0.1), 0 4px 6px -4px rgb(0 0 0 / 0.1);
	}

	:global(.leaflet-popup-tip) {
		box-shadow: 0 3px 14px rgba(0, 0, 0, 0.1);
	}
</style>
