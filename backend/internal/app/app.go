package app

import (
	"log"

	"driver-management-backend/internal/infrastructure/config"
	"driver-management-backend/internal/infrastructure/database"
	"driver-management-backend/internal/infrastructure/notification"
	"driver-management-backend/internal/infrastructure/repository"
	"driver-management-backend/internal/infrastructure/storage"
	"driver-management-backend/internal/interface/http/handler"
	"driver-management-backend/internal/interface/http/router"
	"driver-management-backend/internal/usecase"

	"github.com/gofiber/fiber/v2"
	"github.com/gofiber/fiber/v2/middleware/cors"
	"github.com/gofiber/fiber/v2/middleware/logger"
	"github.com/gofiber/fiber/v2/middleware/recover"
)

type App struct {
	fiber  *fiber.App
	config *config.Config
}

func NewApp(cfg *config.Config) *App {
	// Initialize database
	db, err := database.NewPostgresDB(&cfg.Database)
	if err != nil {
		log.Fatal("Failed to connect to database:", err)
	}

	// Initialize repositories
	userRepo := repository.NewUserRepository(db)
	reportRepo := repository.NewReportRepository(db)
	locationRepo := repository.NewLocationRepository(db)
	alertRepo := repository.NewAlertRepository(db)
	storageRepo := storage.NewR2Storage(cfg)

	// Initialize FCM service
	fcmService := notification.NewFCMService(cfg.FCM.ProjectID, cfg.FCM.ServiceAccountPath)

	// Initialize use cases
	authUseCase := usecase.NewAuthUseCase(userRepo, cfg.JWT.Secret, cfg.JWT.Expiry)
	reportUseCase := usecase.NewReportUseCase(reportRepo, userRepo, storageRepo)
	locationUseCase := usecase.NewLocationUseCase(locationRepo, alertRepo, userRepo, fcmService, cfg)
	userUseCase := usecase.NewUserUseCase(userRepo, locationRepo)
	alertUseCase := usecase.NewAlertUsecase(alertRepo, userRepo, fcmService)

	// Initialize handlers
	authHandler := handler.NewAuthHandler(authUseCase)
	reportHandler := handler.NewReportHandler(reportUseCase)
	wsHandler := handler.NewWebSocketHandler(locationUseCase)
	locationHandler := handler.NewLocationHandler(locationUseCase, wsHandler)
	userHandler := handler.NewUserHandler(userUseCase)
	alertHandler := handler.NewAlertHandler(alertUseCase)

	// Initialize Fiber app
	app := fiber.New(fiber.Config{
		ErrorHandler: customErrorHandler,
	})

	// Middleware
	app.Use(recover.New())
	app.Use(logger.New())
	
	// CORS configuration
	allowedOrigins := cfg.Server.AllowedOrigins[0]
	if allowedOrigins == "*" {
		// For development: allow all origins but without credentials
		app.Use(cors.New(cors.Config{
			AllowOrigins: "*",
			AllowHeaders: "Origin, Content-Type, Accept, Authorization",
			AllowMethods: "GET, POST, PUT, PATCH, DELETE, OPTIONS",
		}))
	} else {
		// For production: specific origins with credentials
		app.Use(cors.New(cors.Config{
			AllowOrigins:     allowedOrigins,
			AllowHeaders:     "Origin, Content-Type, Accept, Authorization",
			AllowMethods:     "GET, POST, PUT, PATCH, DELETE, OPTIONS",
			AllowCredentials: true,
		}))
	}

	// Setup routes
	r := router.NewRouter(authHandler, reportHandler, locationHandler, userHandler, alertHandler, wsHandler, cfg.JWT.Secret)
	r.Setup(app)

	return &App{
		fiber:  app,
		config: cfg,
	}
}

func (a *App) Run(addr string) error {
	return a.fiber.Listen(addr)
}

func customErrorHandler(c *fiber.Ctx, err error) error {
	code := fiber.StatusInternalServerError
	if e, ok := err.(*fiber.Error); ok {
		code = e.Code
	}

	return c.Status(code).JSON(fiber.Map{
		"error": err.Error(),
	})
}
