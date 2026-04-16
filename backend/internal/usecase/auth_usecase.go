package usecase

import (
	"errors"
	"time"

	"driver-management-backend/internal/domain/entity"
	"driver-management-backend/internal/domain/repository"
	"driver-management-backend/internal/infrastructure/security"
)

type AuthUseCase struct {
	userRepo  repository.UserRepository
	jwtSecret string
	jwtExpiry time.Duration
}

func NewAuthUseCase(userRepo repository.UserRepository, jwtSecret string, jwtExpiry time.Duration) *AuthUseCase {
	return &AuthUseCase{
		userRepo:  userRepo,
		jwtSecret: jwtSecret,
		jwtExpiry: jwtExpiry,
	}
}

type LoginRequest struct {
	Email    string `json:"email" validate:"required"`
	Password string `json:"password" validate:"required"`
}

type LoginResponse struct {
	Token string       `json:"token"`
	User  *entity.User `json:"user"`
}

func (uc *AuthUseCase) Login(req LoginRequest) (*LoginResponse, error) {
	// Try to find user by email
	user, err := uc.userRepo.FindByEmail(req.Email)
	if err != nil {
		// If not found by email, try username
		user, err = uc.userRepo.FindByUsername(req.Email)
		if err != nil {
			return nil, errors.New("invalid credentials")
		}
	}

	if !security.CheckPasswordHash(req.Password, user.Password) {
		return nil, errors.New("invalid credentials")
	}

	token, err := security.GenerateJWT(user.ID, user.Username, uc.jwtSecret, uc.jwtExpiry)
	if err != nil {
		return nil, err
	}

	return &LoginResponse{
		Token: token,
		User:  user,
	}, nil
}

type RegisterRequest struct {
	Username  string  `json:"username" validate:"required,min=3"`
	Email     string  `json:"email" validate:"required,email"`
	Password  string  `json:"password" validate:"required,min=6"`
	FullName  string  `json:"full_name" validate:"required"`
	Phone     string  `json:"phone"`
	LicenseNo *string `json:"license_no"`
	Role      string  `json:"role" validate:"required,oneof=driver operator"`
}

func (uc *AuthUseCase) Register(req RegisterRequest) (*entity.User, error) {
	// Check if username exists
	if _, err := uc.userRepo.FindByUsername(req.Username); err == nil {
		return nil, errors.New("username already exists")
	}

	// Check if email exists
	if _, err := uc.userRepo.FindByEmail(req.Email); err == nil {
		return nil, errors.New("email already exists")
	}

	hashedPassword, err := security.HashPassword(req.Password)
	if err != nil {
		return nil, err
	}

	user := &entity.User{
		Username:  req.Username,
		Email:     req.Email,
		Password:  hashedPassword,
		FullName:  req.FullName,
		Phone:     req.Phone,
		LicenseNo: req.LicenseNo,
		Role:      req.Role,
		Status:    "inactive",
	}

	if err := uc.userRepo.Create(user); err != nil {
		return nil, err
	}

	return user, nil
}
