package usecase

import (
	"driver-management-backend/internal/domain/entity"
	"driver-management-backend/internal/domain/repository"
	"driver-management-backend/internal/infrastructure/config"
	"driver-management-backend/internal/infrastructure/notification"
	"encoding/json"
	"fmt"
	"log"
	"time"
)

type LocationUseCase struct {
	locationRepo repository.LocationRepository
	alertRepo    repository.AlertRepository
	userRepo     repository.UserRepository
	fcmService   *notification.FCMService
	config       *config.Config
}

func NewLocationUseCase(
	locationRepo repository.LocationRepository,
	alertRepo repository.AlertRepository,
	userRepo repository.UserRepository,
	fcmService *notification.FCMService,
	cfg *config.Config,
) *LocationUseCase {
	return &LocationUseCase{
		locationRepo: locationRepo,
		alertRepo:    alertRepo,
		userRepo:     userRepo,
		fcmService:   fcmService,
		config:       cfg,
	}
}

type TrackLocationRequest struct {
	UserID          uint     `json:"user_id" validate:"required"`
	Latitude        float64  `json:"latitude" validate:"required"`
	Longitude       float64  `json:"longitude" validate:"required"`
	Speed           float64  `json:"speed"`
	Accuracy        float64  `json:"accuracy"`
	Heading         *float64 `json:"heading"`
	Altitude        *float64 `json:"altitude"`
	RecordedAtUTC   string   `json:"recorded_at_utc" validate:"required"`
	Timezone        string   `json:"timezone" validate:"required"`
	RecordedAtLocal string   `json:"recorded_at_local" validate:"required"`
}

func (uc *LocationUseCase) TrackLocation(req TrackLocationRequest) (*entity.Location, error) {
	recordedAtUTC, _ := time.Parse(time.RFC3339, req.RecordedAtUTC)
	recordedAtLocal, _ := time.Parse(time.RFC3339, req.RecordedAtLocal)

	location := &entity.Location{
		UserID:          req.UserID,
		Latitude:        req.Latitude,
		Longitude:       req.Longitude,
		Speed:           req.Speed,
		Accuracy:        req.Accuracy,
		Heading:         req.Heading,
		Altitude:        req.Altitude,
		RecordedAtUTC:   recordedAtUTC,
		Timezone:        req.Timezone,
		RecordedAtLocal: recordedAtLocal,
	}

	if err := uc.locationRepo.Create(location); err != nil {
		return nil, err
	}

	// Check for overspeed and send notification
	go uc.checkOverspeedAndNotify(req.UserID, req.Speed)

	return location, nil
}

func (uc *LocationUseCase) GetLocationHistory(userID uint, limit int, dateStr string) ([]entity.Location, error) {
	if dateStr != "" {
		start, err := time.Parse("2006-01-02", dateStr)
		if err == nil {
			end := start.Add(24 * time.Hour).Add(-time.Second)
			return uc.locationRepo.FindByTimeRange(userID, start, end)
		}
	}
	return uc.locationRepo.FindByUserID(userID, limit)
}

func (uc *LocationUseCase) GetLatestLocation(userID uint) (*entity.Location, error) {
	return uc.locationRepo.GetLatestByUserID(userID)
}

func (uc *LocationUseCase) GetAllLatestLocations() ([]entity.Location, error) {
	return uc.locationRepo.GetAllLatestLocations()
}

func (uc *LocationUseCase) GetLocationWithUser(locationID uint) (*entity.Location, error) {
	return uc.locationRepo.FindByIDWithUser(locationID)
}

func (uc *LocationUseCase) checkOverspeedAndNotify(userID uint, speed float64) {
	speedLimit := uc.config.Alert.SpeedLimit
	
	// Check if speed exceeds limit
	if speed <= speedLimit {
		return
	}

	// Get driver info
	driver, err := uc.userRepo.FindByID(userID)
	if err != nil {
		log.Printf("Failed to get driver info: %v", err)
		return
	}

	// Create alert in database
	metadata := map[string]interface{}{
		"speed":       speed,
		"speed_limit": speedLimit,
		"timestamp":   time.Now(),
	}
	metadataJSON, _ := json.Marshal(metadata)
	metadataStr := string(metadataJSON)

	alert := &entity.Alert{
		DriverID:  userID,
		Title:     "⚠️ Peringatan Kecepatan Berlebih",
		Message:   fmt.Sprintf("Kecepatan Anda %.0f km/h melebihi batas %.0f km/h. Harap kurangi kecepatan!", speed, speedLimit),
		Priority:  entity.AlertPriorityCritical,
		Status:    entity.AlertStatusSent,
		AlertType: string(entity.AlertTypeOverspeed),
		Metadata:  &metadataStr,
	}

	if err := uc.alertRepo.Create(alert); err != nil {
		log.Printf("Failed to create alert: %v", err)
		return
	}

	// Send FCM notification to driver
	if driver.FCMToken != nil && *driver.FCMToken != "" {
		fcmNotif := &notification.FCMNotification{
			Title: alert.Title,
			Body:  alert.Message,
		}

		fcmData := notification.FCMData{
			Type:     "overspeed",
			Speed:    speed,
			Limit:    speedLimit,
			DriverID: userID,
			AlertID:  alert.ID,
		}

		if err := uc.fcmService.SendToToken(*driver.FCMToken, fcmNotif, fcmData); err != nil {
			log.Printf("Failed to send FCM to driver: %v", err)
		}
	}

	// Send notification to all operators
	uc.notifyOperators(driver, speed, speedLimit, alert.ID)
}

func (uc *LocationUseCase) notifyOperators(driver *entity.User, speed, speedLimit float64, alertID uint) {
	// Get all operators
	operators, err := uc.userRepo.FindByRole("operator")
	if err != nil {
		log.Printf("Failed to get operators: %v", err)
		return
	}

	// Collect FCM tokens
	var tokens []string
	for _, op := range operators {
		if op.FCMToken != nil && *op.FCMToken != "" {
			tokens = append(tokens, *op.FCMToken)
		}
	}

	if len(tokens) == 0 {
		return
	}

	// Send notification to all operators
	fcmNotif := &notification.FCMNotification{
		Title: "🚨 Driver Overspeed Alert",
		Body:  fmt.Sprintf("%s berkendara dengan kecepatan %.0f km/h (batas: %.0f km/h)", driver.FullName, speed, speedLimit),
	}

	fcmData := notification.FCMData{
		Type:     "overspeed_operator",
		Speed:    speed,
		Limit:    speedLimit,
		DriverID: driver.ID,
		AlertID:  alertID,
	}

	if err := uc.fcmService.SendToMultipleTokens(tokens, fcmNotif, fcmData); err != nil {
		log.Printf("Failed to send FCM to operators: %v", err)
	}
}
