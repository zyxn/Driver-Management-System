package repository

import "driver-management-backend/internal/domain/entity"

type AlertRepository interface {
	Create(alert *entity.Alert) error
	GetByID(id uint) (*entity.Alert, error)
	GetAll() ([]entity.Alert, error)
	GetByDriverID(driverID uint) ([]entity.Alert, error)
	UpdateStatus(id uint, status entity.AlertStatus) error
	Delete(id uint) error
}
