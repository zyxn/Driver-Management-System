package usecase

import (
	"context"
	"errors"
	"fmt"
	"mime/multipart"
	"path/filepath"
	"time"

	"driver-management-backend/internal/domain/entity"
	"driver-management-backend/internal/domain/repository"
	"github.com/google/uuid"
)

type ReportUseCase struct {
	reportRepo  repository.ReportRepository
	userRepo    repository.UserRepository
	storageRepo repository.StorageRepository
}

func NewReportUseCase(reportRepo repository.ReportRepository, userRepo repository.UserRepository, storageRepo repository.StorageRepository) *ReportUseCase {
	return &ReportUseCase{
		reportRepo:  reportRepo,
		userRepo:    userRepo,
		storageRepo: storageRepo,
	}
}

type CreateReportRequest struct {
	UserID          uint    `json:"user_id" form:"user_id" validate:"required"`
	PlaceName       string  `json:"place_name" form:"place_name" validate:"required"`
	Latitude        float64 `json:"latitude" form:"latitude" validate:"required"`
	Longitude       float64 `json:"longitude" form:"longitude" validate:"required"`
	Description     string  `json:"description" form:"description"`
	ImageURL        string  `json:"image_url" form:"image_url"`
	ReportedAtUTC   string  `json:"reported_at_utc" form:"reported_at_utc" validate:"required"`
	Timezone        string  `json:"timezone" form:"timezone" validate:"required"`
	ReportedAtLocal string  `json:"reported_at_local" form:"reported_at_local" validate:"required"`
}

func (uc *ReportUseCase) CreateReport(req CreateReportRequest, fileHeader *multipart.FileHeader) (*entity.Report, error) {
	// Check if user exists
	_, err := uc.userRepo.FindByID(req.UserID)
	if err != nil {
		return nil, errors.New("user not found")
	}

	reportedAtUTC, _ := time.Parse(time.RFC3339, req.ReportedAtUTC)
	reportedAtLocal, _ := time.Parse(time.RFC3339, req.ReportedAtLocal)

	imageURL := req.ImageURL

	// Handle file upload if present
	if fileHeader != nil {
		file, err := fileHeader.Open()
		if err != nil {
			return nil, fmt.Errorf("failed to open uploaded file: %w", err)
		}
		defer file.Close()

		ext := filepath.Ext(fileHeader.Filename)
		newFileName := fmt.Sprintf("reports/%d-%s%s", req.UserID, uuid.New().String(), ext)
		contentType := fileHeader.Header.Get("Content-Type")
		if contentType == "" {
			contentType = "application/octet-stream"
		}

		uploadedURL, err := uc.storageRepo.UploadFile(context.Background(), newFileName, contentType, file)
		if err != nil {
			return nil, fmt.Errorf("failed to upload image: %w", err)
		}
		imageURL = uploadedURL
	}

	report := &entity.Report{
		UserID:          req.UserID,
		PlaceName:       req.PlaceName,
		Latitude:        req.Latitude,
		Longitude:       req.Longitude,
		Description:     req.Description,
		ImageURL:        imageURL,
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
