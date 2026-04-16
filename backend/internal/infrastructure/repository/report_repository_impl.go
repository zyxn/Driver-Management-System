package repository

import (
	"driver-management-backend/internal/domain/entity"
	"driver-management-backend/internal/domain/repository"

	"gorm.io/gorm"
)

type reportRepositoryImpl struct {
	db *gorm.DB
}

func NewReportRepository(db *gorm.DB) repository.ReportRepository {
	return &reportRepositoryImpl{db: db}
}

func (r *reportRepositoryImpl) Create(report *entity.Report) error {
	return r.db.Create(report).Error
}

func (r *reportRepositoryImpl) FindByID(id uint) (*entity.Report, error) {
	var report entity.Report
	if err := r.db.Preload("User").First(&report, id).Error; err != nil {
		return nil, err
	}
	return &report, nil
}

func (r *reportRepositoryImpl) FindByUserID(userID uint) ([]entity.Report, error) {
	var reports []entity.Report
	if err := r.db.Where("user_id = ?", userID).Order("created_at DESC").Find(&reports).Error; err != nil {
		return nil, err
	}
	return reports, nil
}

func (r *reportRepositoryImpl) Update(report *entity.Report) error {
	return r.db.Save(report).Error
}

func (r *reportRepositoryImpl) Delete(id uint) error {
	return r.db.Delete(&entity.Report{}, id).Error
}

func (r *reportRepositoryImpl) FindAll() ([]entity.Report, error) {
	var reports []entity.Report
	if err := r.db.Preload("User").Order("created_at DESC").Find(&reports).Error; err != nil {
		return nil, err
	}
	return reports, nil
}

func (r *reportRepositoryImpl) FindByTimeRange(userID uint, start, end string) ([]entity.Report, error) {
	var reports []entity.Report
	if err := r.db.Where("user_id = ? AND reported_at_utc BETWEEN ? AND ?", userID, start, end).
		Order("reported_at_utc ASC").Find(&reports).Error; err != nil {
		return nil, err
	}
	return reports, nil
}

func (r *reportRepositoryImpl) FindAllByTimeRange(start, end string) ([]entity.Report, error) {
	var reports []entity.Report
	if err := r.db.Preload("User").Where("reported_at_utc BETWEEN ? AND ?", start, end).
		Order("reported_at_utc ASC").Find(&reports).Error; err != nil {
		return nil, err
	}
	return reports, nil
}
