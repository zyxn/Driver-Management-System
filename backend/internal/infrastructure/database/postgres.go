package database

import (
	"log"

	"driver-management-backend/internal/domain/entity"
	"driver-management-backend/internal/infrastructure/config"

	"gorm.io/driver/postgres"
	"gorm.io/gorm"
	"gorm.io/gorm/logger"
)

func NewPostgresDB(cfg *config.DatabaseConfig) (*gorm.DB, error) {
	db, err := gorm.Open(postgres.Open(cfg.DSN()), &gorm.Config{
		Logger: logger.Default.LogMode(logger.Info),
	})
	if err != nil {
		return nil, err
	}

	// Auto migrate
	if err := db.AutoMigrate(
		&entity.User{},
		&entity.Report{},
		&entity.Location{},
		&entity.Alert{},
	); err != nil {
		return nil, err
	}

	log.Println("Database connected and migrated successfully")
	return db, nil
}
