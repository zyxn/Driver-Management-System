<script lang="ts">
	import * as Breadcrumb from "$lib/components/ui/breadcrumb/index.js";
	import { Separator } from "$lib/components/ui/separator/index.js";
	import * as Sidebar from "$lib/components/ui/sidebar/index.js";
	import * as Card from '$lib/components/ui/card/index.js';
	import { Input } from '$lib/components/ui/input/index.js';
	import { Badge } from '$lib/components/ui/badge/index.js';
	import { Button } from '$lib/components/ui/button/index.js';
	import MapWithRoutes from '$lib/components/map-with-routes.svelte';
	import { Search, Calendar } from 'lucide-svelte';
	import { onMount } from 'svelte';
	
	import { getAllDrivers } from '$lib/api/users';
	import { getReports, getUserReports } from '$lib/api/reports';
	import { getLocationHistory } from '$lib/api/locations';

	interface Driver {
		id: number;
		name: string;
		employeeId: string;
		group: string;
		status: string;
		vehicleType?: string;
		plateNumber?: string;
	}

	interface MapPoint {
		lat: number;
		lng: number;
		type: string;
		location: string;
		address: string;
		timestamp: string;
		notes: string;
		photoUrl: string;
	}

	let driversList = $state<Driver[]>([]);
	let dailyReports = $state<any[]>([]); 
	
	let searchQuery = $state('');
	let selectedDriver = $state<number | null>(null);
	let selectedDate = $state<Date>(new Date(2026, 3, 16)); // Default to April 16, 2026 (Seed date)
	let showDatePicker = $state(false);

	let driverLocations = $state<{ lat: number; lng: number }[]>([]);
	let driverReports = $state<MapPoint[]>([]);
	let isLoadingDriverData = $state(false);

	function getDateStr(d: Date) {
		const pad = (n: number) => n.toString().padStart(2, '0');
		return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;
	}

	onMount(async () => {
		try {
			const users = await getAllDrivers();
			driversList = users.map((d: any) => ({
				id: d.id,
				name: d.full_name || d.username,
				employeeId: d.username,
				group: 'Drivers',
				status: d.status || 'Active',
				vehicleType: '-', 
				plateNumber: d.license_no || '-'
			}));
			await fetchDailyReports(selectedDate);
		} catch (error) {
			console.error("Failed to load drivers", error);
		}
	});

	async function fetchDailyReports(dateObj: Date) {
		try {
			const dateStr = getDateStr(dateObj);
			const reports = await getReports(dateStr);
			dailyReports = reports || [];
		} catch (error) {
			console.error("Failed to load daily reports summary", error);
			dailyReports = [];
		}
	}

	$effect(() => {
		const d = selectedDate; 
		selectedDriver = null;
		driverLocations = [];
		driverReports = [];
		fetchDailyReports(d);
	});

	let drivers = $derived(() => {
		return driversList.map(driver => {
			const driverReportsCount = dailyReports.filter(r => r.user_id === driver.id).length;
			return {
				...driver,
				totalReports: driverReportsCount,
				speed: 0,
				lastUpdate: driverReportsCount > 0 ? 'Hari Ini' : 'No data'
			};
		});
	});

	let filteredDrivers = $derived(() => {
		return drivers().filter(driver => {
			const matchesSearch = driver.name.toLowerCase().includes(searchQuery.toLowerCase()) ||
				driver.employeeId.toLowerCase().includes(searchQuery.toLowerCase());
			
			return matchesSearch;
		});
	});

	let mapData = $derived(() => {
		const currentDrivers = drivers();
		const selectedDriverData = selectedDriver ? currentDrivers.find(d => d.id === selectedDriver) : null;
		
		if (selectedDriverData && (driverLocations.length > 0 || driverReports.length > 0)) {
			let rts = [];
			if (driverLocations.length > 0) {
				rts = [{
					driverId: selectedDriverData.id,
					driverName: selectedDriverData.name,
					points: driverLocations,
					color: '#2196F3'
				}];
			}
			return {
				routes: rts,
				pickupPoints: driverReports
			};
		} else {
			return {
				routes: [],
				pickupPoints: []
			};
		}
	});

	async function selectDriver(driverId: number) {
		selectedDriver = selectedDriver === driverId ? null : driverId;
		if (selectedDriver) {
			isLoadingDriverData = true;
			driverLocations = [];
			driverReports = [];
			
			try {
				const dateStr = getDateStr(selectedDate);
				
				// Fetch GPS location history for polyline
				const locs = await getLocationHistory(driverId, 0, dateStr);
				driverLocations = locs.map((l: any) => ({
					lat: l.latitude,
					lng: l.longitude
				}));

				// Fetch reports for markers
				const userReps = await getUserReports(driverId, dateStr);
				let reportsList = userReps.map((r: any) => {
					// Fallback dates in case reported_at_local isn't present
					const dateObj = new Date(r.reported_at_local || r.created_at || Date.now());
					return {
						lat: r.latitude,
						lng: r.longitude,
						type: 'report',
						location: r.place_name || r.title || 'Report Location',
						address: `Lat: ${r.latitude.toFixed(4)}, Lng: ${r.longitude.toFixed(4)}`,
						timestamp: dateObj.toLocaleTimeString('id-ID', { hour: '2-digit', minute: '2-digit' }) + ' WIB',
						notes: r.description,
						photoUrl: r.image_url || '',
						unixMillis: dateObj.getTime()
					};
				});
				
				// Sort chronologically for ordered route markers
				reportsList.sort((a: any, b: any) => a.unixMillis - b.unixMillis);
				driverReports = reportsList;

			} catch(e) {
				console.error(e);
			} finally {
				isLoadingDriverData = false;
			}
		}
	}

	function formatDate(date: Date): string {
		const months = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];
		return `${date.getDate()} ${months[date.getMonth()]} ${date.getFullYear()}`;
	}

	function getCalendarDays(date: Date) {
		const year = date.getFullYear();
		const month = date.getMonth();
		const firstDay = new Date(year, month, 1);
		const lastDay = new Date(year, month + 1, 0);
		const daysInMonth = lastDay.getDate();
		const startingDayOfWeek = firstDay.getDay();
		
		const days: (number | null)[] = [];
		for (let i = 0; i < startingDayOfWeek; i++) {
			days.push(null);
		}
		for (let i = 1; i <= daysInMonth; i++) {
			days.push(i);
		}
		return days;
	}

	function changeMonth(offset: number) {
		selectedDate = new Date(selectedDate.getFullYear(), selectedDate.getMonth() + offset, selectedDate.getDate());
	}

	function selectDate(day: number) {
		selectedDate = new Date(selectedDate.getFullYear(), selectedDate.getMonth(), day);
		showDatePicker = false;
	}

	function isToday(day: number): boolean {
		const today = new Date();
		return day === today.getDate() && 
			selectedDate.getMonth() === today.getMonth() && 
			selectedDate.getFullYear() === today.getFullYear();
	}

	function isSelectedDay(day: number): boolean {
		return day === selectedDate.getDate();
	}

	function handleClickOutside(event: MouseEvent) {
		const target = event.target as HTMLElement;
		if (!target.closest('.date-picker-container')) {
			showDatePicker = false;
		}
	}

	$effect(() => {
		if (showDatePicker) {
			document.addEventListener('click', handleClickOutside);
			return () => {
				document.removeEventListener('click', handleClickOutside);
			};
		}
	});

	function getStatusColor(status: string) {
		switch (status.toLowerCase()) {
			case 'on trip':
			case 'active':
				return 'bg-blue-500';
			case 'available':
				return 'bg-green-500';
			case 'off':
			case 'inactive':
				return 'bg-gray-400';
			default:
				return 'bg-gray-400';
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
					<Breadcrumb.Page>Driver Reports</Breadcrumb.Page>
				</Breadcrumb.Item>
			</Breadcrumb.List>
		</Breadcrumb.Root>
	</div>
</header>

<div class="flex flex-1 flex-col lg:flex-row gap-4 p-4 pt-0 h-[calc(100vh-80px)]">
	<!-- Left Panel - Driver List -->
	<div class="w-full lg:w-[380px] flex flex-col gap-4 h-auto lg:h-full">
		<!-- Driver List Card -->
		<Card.Root class="flex-1 flex flex-col">
			<Card.Header class="pb-3">
				<div class="flex items-center justify-between mb-3">
					<Card.Title class="text-lg">List Drivers</Card.Title>
					<div class="flex items-center gap-2">
						<Badge variant="secondary">{filteredDrivers().length} drivers</Badge>
						
						<!-- Date Filter Button -->
						<div class="relative date-picker-container">
							<Button
								variant="outline"
								size="sm"
								onclick={() => showDatePicker = !showDatePicker}
								class="h-8 gap-2"
							>
								<Calendar class="h-3.5 w-3.5" />
								<span class="text-xs">{formatDate(selectedDate)}</span>
							</Button>
							{#if showDatePicker}
								<div class="absolute right-0 top-10 z-50 w-[280px] rounded-lg border bg-card shadow-lg">
									<div class="p-3">
										<!-- Calendar Header -->
										<div class="flex items-center justify-between mb-3">
											<button
												onclick={() => changeMonth(-1)}
												class="h-7 w-7 rounded-md hover:bg-muted flex items-center justify-center"
											>
												<svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
													<path d="m15 18-6-6 6-6"/>
												</svg>
											</button>
											<div class="font-semibold text-sm">
												{selectedDate.toLocaleString('default', { month: 'long', year: 'numeric' })}
											</div>
											<button
												onclick={() => changeMonth(1)}
												class="h-7 w-7 rounded-md hover:bg-muted flex items-center justify-center"
											>
												<svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
													<path d="m9 18 6-6-6-6"/>
												</svg>
											</button>
										</div>
										
										<!-- Calendar Grid -->
										<div class="grid grid-cols-7 gap-1">
											<!-- Day headers -->
											{#each ['Su', 'Mo', 'Tu', 'We', 'Th', 'Fr', 'Sa'] as day}
												<div class="text-center text-xs font-medium text-muted-foreground py-2">
													{day}
												</div>
											{/each}
											
											<!-- Calendar days -->
											{#each getCalendarDays(selectedDate) as day}
												{#if day === null}
													<div class="aspect-square"></div>
												{:else}
													<button
														onclick={() => selectDate(day)}
														class="aspect-square rounded-md text-sm hover:bg-muted transition-colors
															{isSelectedDay(day) ? 'bg-primary text-primary-foreground hover:bg-primary' : ''}
															{isToday(day) && !isSelectedDay(day) ? 'border border-primary' : ''}"
													>
														{day}
													</button>
												{/if}
											{/each}
										</div>
										
										<!-- Today button -->
										<div class="mt-3 pt-3 border-t">
											<button
												onclick={() => { selectedDate = new Date(2026, 3, 16); showDatePicker = false; }}
												class="w-full text-sm text-primary hover:underline"
											>
												Today (Apr 16, 2026)
											</button>
										</div>
									</div>
								</div>
							{/if}
						</div>
					</div>
				</div>
				
				<!-- Search Bar -->
				<div class="relative">
					<Search class="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
					<Input
						type="text"
						placeholder="Search by name, NIP, or ID..."
						class="pl-9"
						bind:value={searchQuery}
					/>
				</div>
			</Card.Header>

			<Card.Content class="flex-1 overflow-hidden flex flex-col pb-3">
				<!-- Driver List -->
				<div class="space-y-2 pr-2 max-h-[300px] lg:max-h-[calc(100vh-280px)] overflow-y-auto">
					{#each filteredDrivers() as driver (driver.id)}
						<button
							onclick={() => selectDriver(driver.id)}
							class="w-full text-left p-3 rounded-lg border transition-all hover:shadow-md {selectedDriver === driver.id ? 'border-primary bg-primary/5 shadow-md' : 'border-border bg-card hover:border-primary/50'}"
						>
							<div class="flex items-start justify-between mb-2">
								<div class="flex-1 min-w-0">
									<div class="flex items-center gap-2 mb-1">
										<span class="font-semibold text-sm truncate">{driver.name}</span>
										<Badge variant="outline" class="text-[10px] px-1.5 py-0 h-5">{driver.employeeId}</Badge>
									</div>
									<div class="text-xs text-muted-foreground">{driver.group}</div>
								</div>
								<div class="flex items-center gap-1.5 ml-2">
									<div class="w-2 h-2 rounded-full {getStatusColor(driver.status)} animate-pulse"></div>
									<span class="text-xs font-medium">{driver.status}</span>
								</div>
							</div>

							<div class="flex items-center justify-between text-xs mb-2">
								<div class="flex items-center gap-3 text-muted-foreground">
									<span>Total Reports: <span class="font-semibold text-foreground">{driver.totalReports}</span></span>
								</div>
								{#if driver.totalReports > 0}
									<Badge class="text-[10px] px-1.5 py-0 h-5 bg-green-500 hover:bg-green-600 text-white">
										Has Data
									</Badge>
								{:else}
									<Badge variant="outline" class="text-[10px] px-1.5 py-0 h-5 text-muted-foreground border-gray-300">
										No Data
									</Badge>
								{/if}
							</div>

							{#if selectedDriver === driver.id}
								<Separator class="my-2" />
								<div class="space-y-1.5 text-xs">
									<div class="flex justify-between">
										<span class="text-muted-foreground">Vehicle:</span>
										<span class="font-medium">{driver.vehicleType}</span>
									</div>
									<div class="flex justify-between">
										<span class="text-muted-foreground">Plate:</span>
										<span class="font-medium">{driver.plateNumber}</span>
									</div>
									{#if isLoadingDriverData}
										<div class="text-center text-muted-foreground text-xs pt-1 animate-pulse">Loading real GPS & Reports...</div>
									{:else}
										<div class="flex justify-between">
											<span class="text-muted-foreground">GPS Points:</span>
											<span class="font-medium text-blue-600">{driverLocations.length} logs</span>
										</div>
										<div class="flex justify-between">
											<span class="text-muted-foreground">Stop/Reports:</span>
											<span class="font-medium text-purple-600">{driverReports.length} stops</span>
										</div>
									{/if}
								</div>
							{/if}
						</button>
					{/each}

					{#if filteredDrivers().length === 0}
						<div class="text-center py-8 text-muted-foreground text-sm">
							No drivers found
						</div>
					{/if}
				</div>
			</Card.Content>
		</Card.Root>
	</div>

	<!-- Right Panel - Map -->
	<div class="flex-1 min-h-[400px] lg:min-h-0">
		<Card.Root class="h-full">
			<Card.Header class="pb-3">
				<div class="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-2">
					<div>
						<Card.Title class="text-lg">Driver Locations</Card.Title>
						<Card.Description class="hidden sm:block">
							{formatDate(selectedDate)}
							{#if selectedDriver}
								{@const driver = drivers().find(d => d.id === selectedDriver)}
								{#if driver}
									- {driver.name}
								{/if}
							{:else}
								- Select a driver to view route
							{/if}
						</Card.Description>
					</div>
					{#if selectedDriver}
						<button
							onclick={() => selectedDriver = null}
							class="text-xs text-primary hover:underline self-start sm:self-auto"
						>
							Clear Selection
						</button>
					{/if}
				</div>
			</Card.Header>
			<Card.Content class="h-[calc(100%-80px)] min-h-[350px]">
				{@const data = mapData()}
				<MapWithRoutes 
					center={[-6.2088, 106.8456]} 
					zoom={12} 
					routes={data.routes}
					pickupPoints={data.pickupPoints}
				/>
			</Card.Content>
		</Card.Root>
	</div>
</div>
