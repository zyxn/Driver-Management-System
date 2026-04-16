package repository

import (
	"driver-management-backend/internal/domain/entity"
	"driver-management-backend/internal/domain/repository"
	"time"

	"gorm.io/gorm"
)

type locationRepositoryImpl struct {
	db *gorm.DB
}

func NewLocationRepository(db *gorm.DB) repository.LocationRepository {
	return &locationRepositoryImpl{db: db}
}

func (r *locationRepositoryImpl) Create(location *entity.Location) error {
	return r.db.Create(location).Error
}

func (r *locationRepositoryImpl) FindByID(id uint) (*entity.Location, error) {
	var location entity.Location
	if err := r.db.First(&location, id).Error; err != nil {
		return nil, err
	}
	return &location, nil
}

func (r *locationRepositoryImpl) FindByIDWithUser(id uint) (*entity.Location, error) {
	var location entity.Location
	if err := r.db.Preload("User").First(&location, id).Error; err != nil {
		return nil, err
	}
	return &location, nil
}

func (r *locationRepositoryImpl) FindByUserID(userID uint, limit int) ([]entity.Location, error) {
	var locations []entity.Location
	query := r.db.Where("user_id = ?", userID).Order("recorded_at_utc DESC")
	if limit > 0 {
		query = query.Limit(limit)
	}
	if err := query.Find(&locations).Error; err != nil {
		return nil, err
	}
	return locations, nil
}

func (r *locationRepositoryImpl) FindByTimeRange(userID uint, start, end time.Time) ([]entity.Location, error) {
	var locations []entity.Location
	if err := r.db.Where("user_id = ? AND recorded_at_utc BETWEEN ? AND ?", userID, start, end).
		Order("recorded_at_utc ASC").Find(&locations).Error; err != nil {
		return nil, err
	}
	return locations, nil
}

func (r *locationRepositoryImpl) GetLatestByUserID(userID uint) (*entity.Location, error) {
	var location entity.Location
	if err := r.db.Where("user_id = ?", userID).Order("recorded_at_utc DESC").First(&location).Error; err != nil {
		return nil, err
	}
	return &location, nil
}

func (r *locationRepositoryImpl) GetAllLatestLocations() ([]entity.Location, error) {
	var locations []entity.Location
	
	// Get latest location for each user with role 'driver'
	// Using subquery to get the latest location per user
	subQuery := r.db.Table("locations").
		Select("user_id, MAX(recorded_at_utc) as max_time").
		Group("user_id")
	
	if err := r.db.Table("locations").
		Joins("INNER JOIN (?) as latest ON locations.user_id = latest.user_id AND locations.recorded_at_utc = latest.max_time", subQuery).
		Preload("User").
		Find(&locations).Error; err != nil {
		return nil, err
	}
	
	// Filter only drivers
	var driverLocations []entity.Location
	for _, loc := range locations {
		if loc.User != nil && loc.User.Role == "driver" {
			driverLocations = append(driverLocations, loc)
		}
	}
	
	return driverLocations, nil
}

// GetLatestLocationsByUserIDs retrieves the latest location for multiple users in a single query
// Returns a map of userID -> location to avoid N+1 query problem
func (r *locationRepositoryImpl) GetLatestLocationsByUserIDs(userIDs []uint) (map[uint]*entity.Location, error) {
	if len(userIDs) == 0 {
		return make(map[uint]*entity.Location), nil
	}

	var locations []entity.Location
	
	// Subquery to get the latest recorded_at_utc for each user
	subQuery := r.db.Table("locations").
		Select("user_id, MAX(recorded_at_utc) as max_time").
		Where("user_id IN ?", userIDs).
		Group("user_id")
	
	// Join with the subquery to get full location records
	if err := r.db.Table("locations").
		Joins("INNER JOIN (?) as latest ON locations.user_id = latest.user_id AND locations.recorded_at_utc = latest.max_time", subQuery).
		Find(&locations).Error; err != nil {
		return nil, err
	}
	
	// Convert to map for O(1) lookup
	locationMap := make(map[uint]*entity.Location)
	for i := range locations {
		locationMap[locations[i].UserID] = &locations[i]
	}
	
	return locationMap, nil
}
