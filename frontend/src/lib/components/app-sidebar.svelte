<script lang="ts" module>
	import LayoutDashboardIcon from "@lucide/svelte/icons/layout-dashboard";
	import FileTextIcon from "@lucide/svelte/icons/file-text";
	import TruckIcon from "@lucide/svelte/icons/truck";
	import AlertTriangleIcon from "@lucide/svelte/icons/alert-triangle";

	// Driver Management System data
	const data = {
		user: {
			name: "Admin",
			email: "admin@dms.com",
			avatar: "/avatars/admin.jpg",
		},
		navMain: [
			{
				title: "Dashboard",
				url: "/dashboard",
				icon: LayoutDashboardIcon,
				isActive: true,
			},
			{
				title: "Driver Reports",
				url: "/reports",
				icon: FileTextIcon,
			},
			{
				title: "Drivers",
				url: "/drivers",
				icon: TruckIcon,
			},
			{
				title: "Alerts",
				url: "/alerts",
				icon: AlertTriangleIcon,
			},
		],
		projects: [],
	};
</script>

<script lang="ts">
	import NavMain from "./nav-main.svelte";
	import NavProjects from "./nav-projects.svelte";
	import NavUser from "./nav-user.svelte";
	import * as Sidebar from "$lib/components/ui/sidebar/index.js";
	import { useSidebar } from "$lib/components/ui/sidebar/index.js";
	import type { ComponentProps } from "svelte";

	let {
		ref = $bindable(null),
		collapsible = "icon",
		...restProps
	}: ComponentProps<typeof Sidebar.Root> = $props();

	const sidebar = useSidebar();
</script>

<Sidebar.Root bind:ref {collapsible} {...restProps}>
	<Sidebar.Header>
		<Sidebar.Menu>
			<Sidebar.MenuItem>
				<div class="flex items-center gap-3 px-2 py-3 {sidebar.state === 'collapsed' ? 'justify-center' : ''}">
					<img src="/logo/logo_square.png" alt="DMS" class="size-10 object-contain" />
					{#if sidebar.state === "expanded"}
						<span class="text-lg font-semibold">DMS</span>
					{/if}
				</div>
			</Sidebar.MenuItem>
		</Sidebar.Menu>
	</Sidebar.Header>
	<Sidebar.Content>
		<NavMain items={data.navMain} />
	</Sidebar.Content>
	<Sidebar.Footer>
		<NavUser user={data.user} />
	</Sidebar.Footer>
	<Sidebar.Rail />
</Sidebar.Root>
