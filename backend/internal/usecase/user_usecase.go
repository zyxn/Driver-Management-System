package usecase

import (
	"driver-management-backend/internal/domain/entity"
	"driver-management-backend/internal/domain/repository"
	"driver-management-backend/internal/infrastructure/security"
	"driver-management-backend/internal/interface/http/dto"
	"errors"
)

type UserUseCase struct {
	userRepo     repository.UserRepository
	locationRepo repository.LocationRepository
}

func NewUserUseCase(userRepo repository.UserRepository, locationRepo repository.LocationRepository) *UserUseCase {
	return &UserUseCase{
		userRepo:     userRepo,
		locationRepo: locationRepo,
	}
}

func (uc *UserUseCase) GetAllDrivers() ([]entity.User, error) {
	return uc.userRepo.FindByRole("driver")
}

func (uc *UserUseCase) GetAllDriversWithLocation() ([]dto.DriverWithLocationResponse, error) {
	// Get all drivers
	drivers, err := uc.userRepo.FindByRole("driver")
	if err != nil {
		return nil, err
	}

	// Extract driver IDs
	driverIDs := make([]uint, len(drivers))
	for i, driver := range drivers {
		driverIDs[i] = driver.ID
	}

	// Get all latest locations in a single query (fixes N+1 problem)
	locationMap, err := uc.locationRepo.GetLatestLocationsByUserIDs(driverIDs)
	if err != nil {
		return nil, err
	}

	// Build response
	result := make([]dto.DriverWithLocationResponse, 0, len(drivers))
	for _, driver := range drivers {
		// Use FullName if available, otherwise use Username
		name := driver.FullName
		if name == "" {
			name = driver.Username
		}
		
		driverResponse := dto.DriverWithLocationResponse{
			ID:    driver.ID,
			Name:  name,
			Email: driver.Email,
			Phone: driver.Phone,
			Role:  driver.Role,
		}

		// Get location from map (O(1) lookup)
		if lastLocation, exists := locationMap[driver.ID]; exists {
			driverResponse.LastLocation = &dto.LastLocationResponse{
				ID:        lastLocation.ID,
				Latitude:  lastLocation.Latitude,
				Longitude: lastLocation.Longitude,
				Speed:     lastLocation.Speed,
				Timestamp: lastLocation.RecordedAtLocal.Format("2006-01-02T15:04:05"),
			}
		}

		result = append(result, driverResponse)
	}

	return result, nil
}

func (uc *UserUseCase) GetAllUsers() ([]entity.User, error) {
	return uc.userRepo.FindAll()
}

func (uc *UserUseCase) GetUserByID(id uint) (*entity.User, error) {
	return uc.userRepo.FindByID(id)
}

func (uc *UserUseCase) CreateDriver(user *entity.User) error {
	// Validate role
	if user.Role != "driver" {
		return errors.New("user role must be 'driver'")
	}

	// Check if username already exists
	existingUser, _ := uc.userRepo.FindByUsername(user.Username)
	if existingUser != nil {
		return errors.New("username already exists")
	}

	// Check if email already exists
	existingEmail, _ := uc.userRepo.FindByEmail(user.Email)
	if existingEmail != nil {
		return errors.New("email already exists")
	}

	// Hash password
	hashedPassword, err := security.HashPassword(user.Password)
	if err != nil {
		return err
	}
	user.Password = hashedPassword

	// Set default status if not provided
	if user.Status == "" {
		user.Status = "inactive"
	}

	return uc.userRepo.Create(user)
}

func (uc *UserUseCase) UpdateDriver(id uint, updates *entity.User) error {
	// Get existing user
	existingUser, err := uc.userRepo.FindByID(id)
	if err != nil {
		return errors.New("driver not found")
	}

	// Ensure it's a driver
	if existingUser.Role != "driver" {
		return errors.New("user is not a driver")
	}

	// Check if username is being changed and if it already exists
	if updates.Username != "" && updates.Username != existingUser.Username {
		existingUsername, _ := uc.userRepo.FindByUsername(updates.Username)
		if existingUsername != nil {
			return errors.New("username already exists")
		}
		existingUser.Username = updates.Username
	}

	// Check if email is being changed and if it already exists
	if updates.Email != "" && updates.Email != existingUser.Email {
		existingEmail, _ := uc.userRepo.FindByEmail(updates.Email)
		if existingEmail != nil {
			return errors.New("email already exists")
		}
		existingUser.Email = updates.Email
	}

	// Update other fields
	if updates.FullName != "" {
		existingUser.FullName = updates.FullName
	}
	if updates.Phone != "" {
		existingUser.Phone = updates.Phone
	}
	if updates.LicenseNo != nil {
		existingUser.LicenseNo = updates.LicenseNo
	}
	if updates.Status != "" {
		existingUser.Status = updates.Status
	}

	// Hash password if provided
	if updates.Password != "" {
		hashedPassword, err := security.HashPassword(updates.Password)
		if err != nil {
			return err
		}
		existingUser.Password = hashedPassword
	}

	return uc.userRepo.Update(existingUser)
}

func (uc *UserUseCase) DeleteDriver(id uint) error {
	// Get existing user
	existingUser, err := uc.userRepo.FindByID(id)
	if err != nil {
		return errors.New("driver not found")
	}

	// Ensure it's a driver
	if existingUser.Role != "driver" {
		return errors.New("user is not a driver")
	}

	return uc.userRepo.Delete(id)
}

func (uc *UserUseCase) UpdateUserStatus(id uint, status string) error {
	return uc.userRepo.UpdateStatus(id, status)
}

func (uc *UserUseCase) UpdateFCMToken(userID uint, token string) error {
	return uc.userRepo.UpdateFCMToken(userID, token)
}
