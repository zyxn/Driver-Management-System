package usecase

import (
	"driver-management-backend/internal/domain/entity"
	"driver-management-backend/internal/domain/repository"
	"driver-management-backend/internal/infrastructure/notification"
	"errors"
	"log"
)

type AlertUsecase interface {
	CreateAlert(req *entity.CreateAlertRequest) (*entity.AlertResponse, error)
	GetAllAlerts() ([]entity.AlertResponse, error)
	GetAlertsByDriver(driverID uint) ([]entity.AlertResponse, error)
	UpdateAlertStatus(id uint, status entity.AlertStatus) error
}

type alertUsecase struct {
	alertRepo  repository.AlertRepository
	userRepo   repository.UserRepository
	fcmService *notification.FCMService
}

func NewAlertUsecase(alertRepo repository.AlertRepository, userRepo repository.UserRepository, fcmService *notification.FCMService) AlertUsecase {
	return &alertUsecase{
		alertRepo:  alertRepo,
		userRepo:   userRepo,
		fcmService: fcmService,
	}
}

func (u *alertUsecase) CreateAlert(req *entity.CreateAlertRequest) (*entity.AlertResponse, error) {
	// Verify driver exists
	driver, err := u.userRepo.FindByID(req.DriverID)
	if err != nil {
		return nil, errors.New("driver not found")
	}

	if driver.Role != "driver" {
		return nil, errors.New("user is not a driver")
	}

	alert := &entity.Alert{
		DriverID:  req.DriverID,
		Title:     req.Title,
		Message:   req.Message,
		Priority:  req.Priority,
		AlertType: string(req.AlertType),
		Status:    entity.AlertStatusSent,
	}

	if err := u.alertRepo.Create(alert); err != nil {
		return nil, err
	}

	// Send FCM notification to driver
	if driver.FCMToken != nil && *driver.FCMToken != "" {
		fcmNotification := &notification.FCMNotification{
			Title: req.Title,
			Body:  req.Message,
		}

		fcmData := notification.FCMData{
			Type:     "alert",
			DriverID: driver.ID,
			AlertID:  alert.ID,
		}

		if err := u.fcmService.SendToToken(*driver.FCMToken, fcmNotification, fcmData); err != nil {
			log.Printf("Failed to send FCM notification to driver %d: %v", driver.ID, err)
			// Don't fail the alert creation if FCM fails
		} else {
			log.Printf("FCM notification sent successfully to driver %d for alert %d", driver.ID, alert.ID)
		}
	} else {
		log.Printf("Driver %d has no FCM token, skipping notification", driver.ID)
	}

	// Load driver info
	alert, err = u.alertRepo.GetByID(alert.ID)
	if err != nil {
		return nil, err
	}

	return u.toAlertResponse(alert), nil
}

func (u *alertUsecase) GetAllAlerts() ([]entity.AlertResponse, error) {
	alerts, err := u.alertRepo.GetAll()
	if err != nil {
		return nil, err
	}

	responses := make([]entity.AlertResponse, len(alerts))
	for i, alert := range alerts {
		responses[i] = *u.toAlertResponse(&alert)
	}

	return responses, nil
}

func (u *alertUsecase) GetAlertsByDriver(driverID uint) ([]entity.AlertResponse, error) {
	alerts, err := u.alertRepo.GetByDriverID(driverID)
	if err != nil {
		return nil, err
	}

	responses := make([]entity.AlertResponse, len(alerts))
	for i, alert := range alerts {
		responses[i] = *u.toAlertResponse(&alert)
	}

	return responses, nil
}

func (u *alertUsecase) UpdateAlertStatus(id uint, status entity.AlertStatus) error {
	return u.alertRepo.UpdateStatus(id, status)
}

func (u *alertUsecase) toAlertResponse(alert *entity.Alert) *entity.AlertResponse {
	driverName := ""
	if alert.Driver != nil {
		driverName = alert.Driver.FullName
	}

	return &entity.AlertResponse{
		ID:         alert.ID,
		DriverID:   alert.DriverID,
		DriverName: driverName,
		Title:      alert.Title,
		Message:    alert.Message,
		Priority:   alert.Priority,
		Status:     alert.Status,
		AlertType:  entity.AlertType(alert.AlertType),
		CreatedAt:  alert.CreatedAt,
	}
}
