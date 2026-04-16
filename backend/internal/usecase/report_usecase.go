package usecase

import (
	"errors"
	"time"

	"driver-management-backend/internal/domain/entity"
	"driver-management-backend/internal/domain/repository"
)

type ReportUseCase struct {
	reportRepo repository.ReportRepository
	userRepo   repository.UserRepository
}

func NewReportUseCase(reportRepo repository.ReportRepository, userRepo repository.UserRepository) *ReportUseCase {
	return &ReportUseCase{
		reportRepo: reportRepo,
		userRepo:   userRepo,
	}
}

type CreateReportRequest struct {
	UserID          uint    `json:"user_id" validate:"required"`
	PlaceName       string  `json:"place_name" validate:"required"`
	Latitude        float64 `json:"latitude" validate:"required"`
	Longitude       float64 `json:"longitude" validate:"required"`
	Description     string  `json:"description"`
	ImageURL        string  `json:"image_url"`
	ReportedAtUTC   string  `json:"reported_at_utc" validate:"required"`
	Timezone        string  `json:"timezone" validate:"required"`
	ReportedAtLocal string  `json:"reported_at_local" validate:"required"`
}

func (uc *ReportUseCase) CreateReport(req CreateReportRequest) (*entity.Report, error) {
	// Check if user exists
	_, err := uc.userRepo.FindByID(req.UserID)
	if err != nil {
		return nil, errors.New("user not found")
	}

	reportedAtUTC, _ := time.Parse(time.RFC3339, req.ReportedAtUTC)
	reportedAtLocal, _ := time.Parse(time.RFC3339, req.ReportedAtLocal)

	report := &entity.Report{
		UserID:          req.UserID,
		PlaceName:       req.PlaceName,
		Latitude:        req.Latitude,
		Longitude:       req.Longitude,
		Description:     req.Description,
		ImageURL:        req.ImageURL,
		ReportedAtUTC:   reportedAtUTC,
		Timezone:        req.Timezone,
		ReportedAtLocal: reportedAtLocal,
	}

	if err := uc.reportRepo.Create(report); err != nil {
		return nil, err
	}

	return report, nil
}

func (uc *ReportUseCase) GetReportHistory(userID uint, dateStr string) ([]entity.Report, error) {
	if dateStr != "" {
		start, err := time.Parse("2006-01-02", dateStr)
		if err == nil {
			end := start.Add(24 * time.Hour).Add(-time.Second)
			return uc.reportRepo.FindByTimeRange(userID, start.Format(time.RFC3339), end.Format(time.RFC3339))
		}
	}
	return uc.reportRepo.FindByUserID(userID)
}

func (uc *ReportUseCase) GetAllReports(dateStr string) ([]entity.Report, error) {
	if dateStr != "" {
		start, err := time.Parse("2006-01-02", dateStr)
		if err == nil {
			end := start.Add(24 * time.Hour).Add(-time.Second)
			return uc.reportRepo.FindAllByTimeRange(start.Format(time.RFC3339), end.Format(time.RFC3339))
		}
	}
	return uc.reportRepo.FindAll()
}

func (uc *ReportUseCase) GetReportByID(id uint) (*entity.Report, error) {
	return uc.reportRepo.FindByID(id)
}
