package repository

import "driver-management-backend/internal/domain/entity"

type ReportRepository interface {
	Create(report *entity.Report) error
	FindByID(id uint) (*entity.Report, error)
	FindByUserID(userID uint) ([]entity.Report, error)
	Update(report *entity.Report) error
	Delete(id uint) error
	FindAll() ([]entity.Report, error)
	FindByTimeRange(userID uint, start, end string) ([]entity.Report, error)
	FindAllByTimeRange(start, end string) ([]entity.Report, error)
}
