package main

import (
	"log"
	"os"

	"driver-management-backend/internal/app"
	"driver-management-backend/internal/infrastructure/config"

	"github.com/joho/godotenv"
)

func main() {
	// Load environment variables
	if err := godotenv.Load(); err != nil {
		log.Println("No .env file found")
	}

	// Initialize config
	cfg := config.NewConfig()

	// Initialize and run application
	application := app.NewApp(cfg)
	
	port := os.Getenv("PORT")
	if port == "" {
		port = "8080"
	}

	log.Printf("Server starting on port %s", port)
	if err := application.Run(":" + port); err != nil {
		log.Fatal(err)
	}
}
