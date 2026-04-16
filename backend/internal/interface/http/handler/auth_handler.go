package handler

import (
	"driver-management-backend/internal/usecase"

	"github.com/gofiber/fiber/v2"
)

type AuthHandler struct {
	authUseCase *usecase.AuthUseCase
}

func NewAuthHandler(authUseCase *usecase.AuthUseCase) *AuthHandler {
	return &AuthHandler{authUseCase: authUseCase}
}

func (h *AuthHandler) Login(c *fiber.Ctx) error {
	var req usecase.LoginRequest
	if err := c.BodyParser(&req); err != nil {
		return c.Status(fiber.StatusBadRequest).JSON(fiber.Map{
			"error": "Invalid request body",
		})
	}

	result, err := h.authUseCase.Login(req)
	if err != nil {
		return c.Status(fiber.StatusUnauthorized).JSON(fiber.Map{
			"error": err.Error(),
		})
	}

	// Set JWT token as httpOnly cookie
	c.Cookie(&fiber.Cookie{
		Name:     "token",
		Value:    result.Token,
		HTTPOnly: true,
		Secure:   true, // Set to true in production with HTTPS
		SameSite: "Lax",
		MaxAge:   86400, // 24 hours
		Path:     "/",
	})

	// Return user data without token
	return c.JSON(fiber.Map{
		"success": true,
		"data": fiber.Map{
			"user": result.User,
		},
	})
}

func (h *AuthHandler) Register(c *fiber.Ctx) error {
	var req usecase.RegisterRequest
	if err := c.BodyParser(&req); err != nil {
		return c.Status(fiber.StatusBadRequest).JSON(fiber.Map{
			"error": "Invalid request body",
		})
	}

	user, err := h.authUseCase.Register(req)
	if err != nil {
		return c.Status(fiber.StatusBadRequest).JSON(fiber.Map{
			"error": err.Error(),
		})
	}

	return c.Status(fiber.StatusCreated).JSON(fiber.Map{
		"success": true,
		"data":    user,
	})
}

func (h *AuthHandler) Logout(c *fiber.Ctx) error {
	// Clear the httpOnly cookie
	c.Cookie(&fiber.Cookie{
		Name:     "token",
		Value:    "",
		HTTPOnly: true,
		Secure:   true,
		SameSite: "Lax",
		MaxAge:   -1, // Delete cookie
		Path:     "/",
	})

	return c.JSON(fiber.Map{
		"success": true,
		"message": "Logged out successfully",
	})
}

func (h *AuthHandler) Me(c *fiber.Ctx) error {
	// Get user from context (set by auth middleware)
	userID := c.Locals("userID").(uint)
	username := c.Locals("username").(string)

	return c.JSON(fiber.Map{
		"success": true,
		"data": fiber.Map{
			"id":       userID,
			"username": username,
		},
	})
}
