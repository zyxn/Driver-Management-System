package entity

import "time"

type Location struct {
	ID              uint      `json:"id" gorm:"primaryKey"`
	UserID          uint      `json:"user_id" gorm:"not null"`
	User            *User     `json:"user,omitempty" gorm:"foreignKey:UserID"`
	Latitude        float64   `json:"latitude" gorm:"type:decimal(10,8);not null"`
	Longitude       float64   `json:"longitude" gorm:"type:decimal(11,8);not null"`
	Speed           float64   `json:"speed" gorm:"type:decimal(5,2)"` // in km/h
	Accuracy        float64   `json:"accuracy" gorm:"type:decimal(6,2)"` // in meters
	Heading         *float64  `json:"heading,omitempty" gorm:"type:decimal(5,2)"` // nullable
	Altitude        *float64  `json:"altitude,omitempty" gorm:"type:decimal(7,2)"` // nullable
	RecordedAtUTC   time.Time `json:"recorded_at_utc" gorm:"not null"`
	Timezone        string    `json:"timezone"`
	RecordedAtLocal time.Time `json:"recorded_at_local"` // stored, computed
	CreatedAt       time.Time `json:"created_at"`
}
