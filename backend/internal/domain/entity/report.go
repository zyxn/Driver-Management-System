package entity

import "time"

type Report struct {
	ID              uint      `json:"id" gorm:"primaryKey"`
	UserID          uint      `json:"user_id" gorm:"not null"`
	User            *User     `json:"user,omitempty" gorm:"foreignKey:UserID"`
	PlaceName       string    `json:"place_name"`
	Latitude        float64   `json:"latitude" gorm:"type:decimal(10,8);not null"`
	Longitude       float64   `json:"longitude" gorm:"type:decimal(11,8);not null"`
	Description     string    `json:"description" gorm:"type:text"`
	ImageURL        string    `json:"image_url"`
	ReportedAtUTC   time.Time `json:"reported_at_utc" gorm:"not null"`
	Timezone        string    `json:"timezone"`
	ReportedAtLocal time.Time `json:"reported_at_local"` // stored, computed
	CreatedAt       time.Time `json:"created_at"`
	UpdatedAt       time.Time `json:"updated_at"`
}
