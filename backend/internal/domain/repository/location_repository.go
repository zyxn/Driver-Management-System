package repository

import (
	"driver-management-backend/internal/domain/entity"
	"time"
)

type LocationRepository interface {
	Create(location *entity.Location) error
	FindByID(id uint) (*entity.Location, error)
	FindByIDWithUser(id uint) (*entity.Location, error)
	FindByUserID(userID uint, limit int) ([]entity.Location, error)
	FindByTimeRange(userID uint, start, end time.Time) ([]entity.Location, error)
	GetLatestByUserID(userID uint) (*entity.Location, error)
	GetAllLatestLocations() ([]entity.Location, error)
	GetLatestLocationsByUserIDs(userIDs []uint) (map[uint]*entity.Location, error)
}
