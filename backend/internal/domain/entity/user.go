package entity

import "time"

type User struct {
	ID        uint      `json:"id" gorm:"primaryKey"`
	Username  string    `json:"username" gorm:"unique;not null"`
	Email     string    `json:"email" gorm:"unique;not null"`
	Password  string    `json:"-" gorm:"not null"`
	FullName  string    `json:"full_name"`
	Phone     string    `json:"phone"`
	LicenseNo *string   `json:"license_no,omitempty"` // nullable, driver only
	Role      string    `json:"role" gorm:"not null"` // driver or operator
	Status    string    `json:"status" gorm:"default:'inactive'"` // active, inactive
	FCMToken  *string   `json:"fcm_token,omitempty" gorm:"type:text"` // for push notifications
	
	// Relations
	Alerts    []Alert   `json:"alerts,omitempty" gorm:"foreignKey:DriverID"` // one user has many alerts
	
	CreatedAt time.Time `json:"created_at"`
	UpdatedAt time.Time `json:"updated_at"`
}
