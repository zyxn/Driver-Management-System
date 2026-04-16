package router

import (
	"driver-management-backend/internal/interface/http/handler"
	"driver-management-backend/internal/interface/http/middleware"

	"github.com/gofiber/fiber/v2"
)

type Router struct {
	authHandler     *handler.AuthHandler
	reportHandler   *handler.ReportHandler
	locationHandler *handler.LocationHandler
	userHandler     *handler.UserHandler
	alertHandler    *handler.AlertHandler
	wsHandler       *handler.WebSocketHandler
	jwtSecret       string
}

func NewRouter(
	authHandler *handler.AuthHandler,
	reportHandler *handler.ReportHandler,
	locationHandler *handler.LocationHandler,
	userHandler *handler.UserHandler,
	alertHandler *handler.AlertHandler,
	wsHandler *handler.WebSocketHandler,
	jwtSecret string,
) *Router {
	return &Router{
		authHandler:     authHandler,
		reportHandler:   reportHandler,
		locationHandler: locationHandler,
		userHandler:     userHandler,
		alertHandler:    alertHandler,
		wsHandler:       wsHandler,
		jwtSecret:       jwtSecret,
	}
}

func (r *Router) Setup(app *fiber.App) {
	// Root welcome message
	app.Get("/", func(c *fiber.Ctx) error {
		return c.JSON(fiber.Map{
			"message": "Welcome to Driver Management System API",
			"version": "1.0.0",
			"endpoints": fiber.Map{
				"health":    "/api/v1/health",
				"auth":      "/api/v1/auth",
				"reports":   "/api/v1/reports",
				"locations": "/api/v1/locations",
			},
		})
	})

	// Test endpoint - completely bypass everything
	app.Post("/test/location", func(c *fiber.Ctx) error {
		return c.JSON(fiber.Map{
			"success": true,
			"message": "Test endpoint works - no auth!",
		})
	})

	api := app.Group("/api/v1")

	// ============================================
	// PUBLIC ROUTES - NO AUTHENTICATION REQUIRED
	// ============================================
	
	// Health check
	api.Get("/health", func(c *fiber.Ctx) error {
		return c.JSON(fiber.Map{
			"status": "ok",
		})
	})

	// Auth routes
	api.Post("/auth/login", r.authHandler.Login)
	api.Post("/auth/register", r.authHandler.Register)

	// Location tracking - for mobile app (no auth)
	api.Post("/locations/track", r.locationHandler.TrackLocation)

	// WebSocket - for real-time updates (no auth)
	app.Get("/ws/locations", r.wsHandler.UpgradeToWebSocket())

	// ============================================
	// PROTECTED ROUTES - AUTHENTICATION REQUIRED
	// ============================================
	
	protected := api.Group("", middleware.AuthMiddleware(r.jwtSecret))
	
	// Auth protected routes
	protected.Post("/auth/logout", r.authHandler.Logout)
	protected.Get("/auth/me", r.authHandler.Me)

	// Report routes
	reports := protected.Group("/reports")
	reports.Post("/", r.reportHandler.CreateReport)
	reports.Get("/user/:userId", r.reportHandler.GetReportHistory)
	reports.Get("/", r.reportHandler.GetAllReports)
	reports.Get("/:id", r.reportHandler.GetReportByID)

	// Location routes (protected)
	locations := protected.Group("/locations")
	locations.Get("/user/:userId", r.locationHandler.GetLocationHistory)
	locations.Get("/user/:userId/latest", r.locationHandler.GetLatestLocation)
	locations.Get("/latest", r.locationHandler.GetAllLatestLocations)

	// User routes
	users := protected.Group("/users")
	users.Get("/", r.userHandler.GetAllUsers)
	users.Get("/drivers", r.userHandler.GetAllDrivers)
	users.Get("/drivers/with-location", r.userHandler.GetAllDriversWithLocation)
	users.Get("/:id", r.userHandler.GetUserByID)
	users.Patch("/:id/status", r.userHandler.UpdateUserStatus)
	users.Post("/fcm-token", r.userHandler.UpdateFCMToken)
	
	// Driver CRUD routes
	users.Post("/drivers", r.userHandler.CreateDriver)
	users.Put("/drivers/:id", r.userHandler.UpdateDriver)
	users.Delete("/drivers/:id", r.userHandler.DeleteDriver)

	// Alert routes
	alerts := protected.Group("/alerts")
	alerts.Post("/", r.alertHandler.CreateAlert)
	alerts.Get("/", r.alertHandler.GetAllAlerts)
	alerts.Get("/driver/:id", r.alertHandler.GetAlertsByDriver)
	alerts.Patch("/:id/status", r.alertHandler.UpdateAlertStatus)
}
