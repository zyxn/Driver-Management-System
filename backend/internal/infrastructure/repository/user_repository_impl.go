package repository

import (
	"driver-management-backend/internal/domain/entity"
	"driver-management-backend/internal/domain/repository"

	"gorm.io/gorm"
)

type userRepositoryImpl struct {
	db *gorm.DB
}

func NewUserRepository(db *gorm.DB) repository.UserRepository {
	return &userRepositoryImpl{db: db}
}

func (r *userRepositoryImpl) Create(user *entity.User) error {
	return r.db.Create(user).Error
}

func (r *userRepositoryImpl) FindByID(id uint) (*entity.User, error) {
	var user entity.User
	if err := r.db.First(&user, id).Error; err != nil {
		return nil, err
	}
	return &user, nil
}

func (r *userRepositoryImpl) FindByUsername(username string) (*entity.User, error) {
	var user entity.User
	if err := r.db.Where("username = ?", username).First(&user).Error; err != nil {
		return nil, err
	}
	return &user, nil
}

func (r *userRepositoryImpl) FindByEmail(email string) (*entity.User, error) {
	var user entity.User
	if err := r.db.Where("email = ?", email).First(&user).Error; err != nil {
		return nil, err
	}
	return &user, nil
}

func (r *userRepositoryImpl) Update(user *entity.User) error {
	return r.db.Save(user).Error
}

func (r *userRepositoryImpl) Delete(id uint) error {
	return r.db.Delete(&entity.User{}, id).Error
}

func (r *userRepositoryImpl) FindAll() ([]entity.User, error) {
	var users []entity.User
	if err := r.db.Find(&users).Error; err != nil {
		return nil, err
	}
	return users, nil
}

func (r *userRepositoryImpl) FindByRole(role string) ([]entity.User, error) {
	var users []entity.User
	if err := r.db.Where("role = ?", role).Find(&users).Error; err != nil {
		return nil, err
	}
	return users, nil
}

func (r *userRepositoryImpl) UpdateStatus(id uint, status string) error {
	return r.db.Model(&entity.User{}).Where("id = ?", id).Update("status", status).Error
}

func (r *userRepositoryImpl) UpdateFCMToken(userID uint, token string) error {
	return r.db.Model(&entity.User{}).Where("id = ?", userID).Update("fcm_token", token).Error
}
