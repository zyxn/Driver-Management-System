package main

import (
	"log"

	"driver-management-backend/internal/domain/entity"
	"driver-management-backend/internal/infrastructure/config"

	"github.com/joho/godotenv"
	"gorm.io/driver/postgres"
	"gorm.io/gorm"
	"gorm.io/gorm/logger"
)

func main() {
	// Load .env file
	if err := godotenv.Load(); err != nil {
		log.Println("No .env file found, using environment variables")
	}

	// Load config
	cfg := config.NewConfig()

	// Connect to database
	db, err := gorm.Open(postgres.Open(cfg.Database.DSN()), &gorm.Config{
		Logger: logger.Default.LogMode(logger.Info),
	})
	if err != nil {
		log.Fatal("Failed to connect to database:", err)
	}

	log.Println("Dropping old tables...")
	
	// Drop old tables
	db.Exec("DROP TABLE IF EXISTS alerts CASCADE")
	db.Exec("DROP TABLE IF EXISTS locations CASCADE")
	db.Exec("DROP TABLE IF EXISTS trips CASCADE")
	db.Exec("DROP TABLE IF EXISTS drivers CASCADE")
	db.Exec("DROP TABLE IF EXISTS reports CASCADE")
	db.Exec("DROP TABLE IF EXISTS users CASCADE")

	log.Println("Creating new tables...")

	// Auto migrate with new structure
	if err := db.AutoMigrate(
		&entity.User{},
		&entity.Report{},
		&entity.Location{},
		&entity.Alert{},
	); err != nil {
		log.Fatal("Failed to migrate:", err)
	}

	log.Println("Migration completed successfully!")
}
