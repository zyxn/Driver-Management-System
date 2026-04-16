package dto

// DriverWithLocationResponse represents a driver with their last location
type DriverWithLocationResponse struct {
	ID           uint                 `json:"id"`
	Name         string               `json:"name"`
	Email        string               `json:"email"`
	Phone        string               `json:"phone"`
	Role         string               `json:"role"`
	LastLocation *LastLocationResponse `json:"last_location,omitempty"`
}

// LastLocationResponse represents the last known location of a driver
type LastLocationResponse struct {
	ID        uint    `json:"id"`
	Latitude  float64 `json:"latitude"`
	Longitude float64 `json:"longitude"`
	Speed     float64 `json:"speed"`
	Timestamp string  `json:"timestamp"`
}
