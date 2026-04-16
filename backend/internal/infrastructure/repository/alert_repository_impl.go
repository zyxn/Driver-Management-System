package repository

import (
	"driver-management-backend/internal/domain/entity"
	"driver-management-backend/internal/domain/repository"

	"gorm.io/gorm"
)

type alertRepositoryImpl struct {
	db *gorm.DB
}

func NewAlertRepository(db *gorm.DB) repository.AlertRepository {
	return &alertRepositoryImpl{db: db}
}

func (r *alertRepositoryImpl) Create(alert *entity.Alert) error {
	return r.db.Create(alert).Error
}

func (r *alertRepositoryImpl) GetByID(id uint) (*entity.Alert, error) {
	var alert entity.Alert
	err := r.db.Preload("Driver").First(&alert, id).Error
	if err != nil {
		return nil, err
	}
	return &alert, nil
}

func (r *alertRepositoryImpl) GetAll() ([]entity.Alert, error) {
	var alerts []entity.Alert
	err := r.db.Preload("Driver").Order("created_at DESC").Find(&alerts).Error
	return alerts, err
}

func (r *alertRepositoryImpl) GetByDriverID(driverID uint) ([]entity.Alert, error) {
	var alerts []entity.Alert
	err := r.db.Preload("Driver").
		Where("driver_id = ?", driverID).
		Order("created_at DESC").
		Find(&alerts).Error
	return alerts, err
}

func (r *alertRepositoryImpl) UpdateStatus(id uint, status entity.AlertStatus) error {
	return r.db.Model(&entity.Alert{}).
		Where("id = ?", id).
		Update("status", status).Error
}

func (r *alertRepositoryImpl) Delete(id uint) error {
	return r.db.Delete(&entity.Alert{}, id).Error
}
