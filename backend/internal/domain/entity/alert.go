package entity

import "time"

type AlertPriority string
type AlertStatus string
type AlertType string

const (
	AlertPriorityLow      AlertPriority = "low"
	AlertPriorityMedium   AlertPriority = "medium"
	AlertPriorityHigh     AlertPriority = "high"
	AlertPriorityCritical AlertPriority = "critical"
)

const (
	AlertStatusSent      AlertStatus = "sent"
	AlertStatusDelivered AlertStatus = "delivered"
	AlertStatusRead      AlertStatus = "read"
)

const (
	AlertTypeOverspeed   AlertType = "overspeed"
	AlertTypeWarning     AlertType = "teguran"
	AlertTypeCaution     AlertType = "hati_hati"
	AlertTypeAnnouncement AlertType = "announcement"
)

type Alert struct {
	ID         uint          `json:"id" gorm:"primaryKey"`
	DriverID   uint          `json:"driver_id" gorm:"not null;index"`
	Driver     *User         `json:"driver,omitempty" gorm:"foreignKey:DriverID;constraint:OnDelete:CASCADE"`
	Title      string        `json:"title" gorm:"type:varchar(200);not null"`
	Message    string        `json:"message" gorm:"type:text;not null"`
	Priority   AlertPriority `json:"priority" gorm:"type:varchar(20);not null;default:'medium'"`
	Status     AlertStatus   `json:"status" gorm:"type:varchar(20);not null;default:'sent'"`
	AlertType  string        `json:"alert_type" gorm:"type:varchar(50);index"`
	Metadata   *string       `json:"metadata,omitempty" gorm:"type:jsonb"`
	CreatedAt  time.Time     `json:"created_at"`
	UpdatedAt  time.Time     `json:"updated_at"`
	DeliveredAt *time.Time   `json:"delivered_at,omitempty"`
	ReadAt     *time.Time    `json:"read_at,omitempty"`
}

type CreateAlertRequest struct {
	DriverID  uint          `json:"driver_id"`
	Title     string        `json:"title"`
	Message   string        `json:"message"`
	Priority  AlertPriority `json:"priority"`
	AlertType AlertType     `json:"alert_type"`
}

type AlertResponse struct {
	ID         uint          `json:"id"`
	DriverID   uint          `json:"driver_id"`
	DriverName string        `json:"driver_name"`
	Title      string        `json:"title"`
	Message    string        `json:"message"`
	Priority   AlertPriority `json:"priority"`
	Status     AlertStatus   `json:"status"`
	AlertType  AlertType     `json:"alert_type"`
	CreatedAt  time.Time     `json:"created_at"`
}
