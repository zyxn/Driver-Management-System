package handler

import (
	"driver-management-backend/internal/domain/entity"
	"driver-management-backend/internal/usecase"
	"log"
	"strconv"

	"github.com/gofiber/fiber/v2"
)

type UserHandler struct {
	userUseCase *usecase.UserUseCase
}

func NewUserHandler(userUseCase *usecase.UserUseCase) *UserHandler {
	return &UserHandler{userUseCase: userUseCase}
}

func (h *UserHandler) GetAllDrivers(c *fiber.Ctx) error {
	drivers, err := h.userUseCase.GetAllDrivers()
	if err != nil {
		return c.Status(fiber.StatusInternalServerError).JSON(fiber.Map{
			"error": "Failed to fetch drivers",
		})
	}

	return c.JSON(fiber.Map{
		"success": true,
		"data":    drivers,
	})
}

func (h *UserHandler) GetAllDriversWithLocation(c *fiber.Ctx) error {
	drivers, err := h.userUseCase.GetAllDriversWithLocation()
	if err != nil {
		return c.Status(fiber.StatusInternalServerError).JSON(fiber.Map{
			"error": "Failed to fetch drivers with location",
		})
	}

	return c.JSON(fiber.Map{
		"success": true,
		"data":    drivers,
	})
}

func (h *UserHandler) GetAllUsers(c *fiber.Ctx) error {
	users, err := h.userUseCase.GetAllUsers()
	if err != nil {
		return c.Status(fiber.StatusInternalServerError).JSON(fiber.Map{
			"error": "Failed to fetch users",
		})
	}

	return c.JSON(fiber.Map{
		"success": true,
		"data":    users,
	})
}

func (h *UserHandler) GetUserByID(c *fiber.Ctx) error {
	id, err := strconv.ParseUint(c.Params("id"), 10, 32)
	if err != nil {
		return c.Status(fiber.StatusBadRequest).JSON(fiber.Map{
			"error": "Invalid user ID",
		})
	}

	user, err := h.userUseCase.GetUserByID(uint(id))
	if err != nil {
		return c.Status(fiber.StatusNotFound).JSON(fiber.Map{
			"error": "User not found",
		})
	}

	return c.JSON(fiber.Map{
		"success": true,
		"data":    user,
	})
}

func (h *UserHandler) CreateDriver(c *fiber.Ctx) error {
	var req entity.User

	if err := c.BodyParser(&req); err != nil {
		return c.Status(fiber.StatusBadRequest).JSON(fiber.Map{
			"error": "Invalid request body",
		})
	}

	// Force role to be driver
	req.Role = "driver"

	if err := h.userUseCase.CreateDriver(&req); err != nil {
		return c.Status(fiber.StatusBadRequest).JSON(fiber.Map{
			"error": err.Error(),
		})
	}

	return c.Status(fiber.StatusCreated).JSON(fiber.Map{
		"success": true,
		"message": "Driver created successfully",
		"data":    req,
	})
}

func (h *UserHandler) UpdateDriver(c *fiber.Ctx) error {
	id, err := strconv.ParseUint(c.Params("id"), 10, 32)
	if err != nil {
		return c.Status(fiber.StatusBadRequest).JSON(fiber.Map{
			"error": "Invalid driver ID",
		})
	}

	var req entity.User
	if err := c.BodyParser(&req); err != nil {
		return c.Status(fiber.StatusBadRequest).JSON(fiber.Map{
			"error": "Invalid request body",
		})
	}

	if err := h.userUseCase.UpdateDriver(uint(id), &req); err != nil {
		return c.Status(fiber.StatusBadRequest).JSON(fiber.Map{
			"error": err.Error(),
		})
	}

	return c.JSON(fiber.Map{
		"success": true,
		"message": "Driver updated successfully",
	})
}

func (h *UserHandler) DeleteDriver(c *fiber.Ctx) error {
	id, err := strconv.ParseUint(c.Params("id"), 10, 32)
	if err != nil {
		return c.Status(fiber.StatusBadRequest).JSON(fiber.Map{
			"error": "Invalid driver ID",
		})
	}

	if err := h.userUseCase.DeleteDriver(uint(id)); err != nil {
		return c.Status(fiber.StatusBadRequest).JSON(fiber.Map{
			"error": err.Error(),
		})
	}

	return c.JSON(fiber.Map{
		"success": true,
		"message": "Driver deleted successfully",
	})
}

func (h *UserHandler) UpdateUserStatus(c *fiber.Ctx) error {
	id, err := strconv.ParseUint(c.Params("id"), 10, 32)
	if err != nil {
		log.Printf("Invalid user ID: %v", err)
		return c.Status(fiber.StatusBadRequest).JSON(fiber.Map{
			"error": "Invalid user ID",
		})
	}

	var req struct {
		Status string `json:"status"`
	}

	if err := c.BodyParser(&req); err != nil {
		log.Printf("Failed to parse request body: %v", err)
		return c.Status(fiber.StatusBadRequest).JSON(fiber.Map{
			"error": "Invalid request body",
		})
	}

	log.Printf("Updating user %d status to: %s", id, req.Status)

	// Validate status
	if req.Status != "active" && req.Status != "inactive" {
		log.Printf("Invalid status value: %s", req.Status)
		return c.Status(fiber.StatusBadRequest).JSON(fiber.Map{
			"error": "Status must be either 'active' or 'inactive'",
		})
	}

	if err := h.userUseCase.UpdateUserStatus(uint(id), req.Status); err != nil {
		log.Printf("Failed to update user status: %v", err)
		return c.Status(fiber.StatusInternalServerError).JSON(fiber.Map{
			"error": "Failed to update user status",
		})
	}

	log.Printf("Successfully updated user %d status to %s", id, req.Status)
	return c.JSON(fiber.Map{
		"success": true,
		"message": "User status updated successfully",
	})
}

func (h *UserHandler) UpdateFCMToken(c *fiber.Ctx) error {
	userID := c.Locals("userID").(uint)

	var req struct {
		FCMToken string `json:"fcm_token"`
	}

	if err := c.BodyParser(&req); err != nil {
		return c.Status(fiber.StatusBadRequest).JSON(fiber.Map{
			"error": "Invalid request body",
		})
	}

	if err := h.userUseCase.UpdateFCMToken(userID, req.FCMToken); err != nil {
		return c.Status(fiber.StatusInternalServerError).JSON(fiber.Map{
			"error": "Failed to update FCM token",
		})
	}

	return c.JSON(fiber.Map{
		"success": true,
		"message": "FCM token updated successfully",
	})
}
