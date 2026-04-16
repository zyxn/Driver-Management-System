package repository

import "driver-management-backend/internal/domain/entity"

type UserRepository interface {
	Create(user *entity.User) error
	FindByID(id uint) (*entity.User, error)
	FindByUsername(username string) (*entity.User, error)
	FindByEmail(email string) (*entity.User, error)
	Update(user *entity.User) error
	Delete(id uint) error
	FindAll() ([]entity.User, error)
	FindByRole(role string) ([]entity.User, error)
	UpdateStatus(id uint, status string) error
	UpdateFCMToken(userID uint, token string) error
}
