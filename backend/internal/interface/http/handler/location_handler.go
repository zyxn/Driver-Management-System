package handler

import (
	"driver-management-backend/internal/usecase"
	"strconv"

	"github.com/gofiber/fiber/v2"
)

type LocationHandler struct {
	locationUseCase *usecase.LocationUseCase
	wsHandler       *WebSocketHandler
}

func NewLocationHandler(locationUseCase *usecase.LocationUseCase, wsHandler *WebSocketHandler) *LocationHandler {
	return &LocationHandler{
		locationUseCase: locationUseCase,
		wsHandler:       wsHandler,
	}
}

func (h *LocationHandler) TrackLocation(c *fiber.Ctx) error {
	var req usecase.TrackLocationRequest
	if err := c.BodyParser(&req); err != nil {
		return c.Status(fiber.StatusBadRequest).JSON(fiber.Map{
			"error": "Invalid request body",
		})
	}

	location, err := h.locationUseCase.TrackLocation(req)
	if err != nil {
		return c.Status(fiber.StatusInternalServerError).JSON(fiber.Map{
			"error": err.Error(),
		})
	}

	// Broadcast to WebSocket clients (with user data if available)
	if h.wsHandler != nil {
		// Try to get location with user data for richer broadcast
		locationWithUser, err := h.locationUseCase.GetLocationWithUser(location.ID)
		if err == nil && locationWithUser != nil {
			h.wsHandler.BroadcastLocationUpdate(locationWithUser, req.UserID)
		} else {
			h.wsHandler.BroadcastLocationUpdate(location, req.UserID)
		}
	}

	return c.Status(fiber.StatusCreated).JSON(fiber.Map{
		"success": true,
		"data":    location,
	})
}

func (h *LocationHandler) GetLocationHistory(c *fiber.Ctx) error {
	userID, err := strconv.ParseUint(c.Params("userId"), 10, 32)
	if err != nil {
		return c.Status(fiber.StatusBadRequest).JSON(fiber.Map{
			"error": "Invalid user ID",
		})
	}

	limit := 100
	if limitParam := c.Query("limit"); limitParam != "" {
		if l, err := strconv.Atoi(limitParam); err == nil {
			limit = l
		}
	}

	dateStr := c.Query("date")

	locations, err := h.locationUseCase.GetLocationHistory(uint(userID), limit, dateStr)
	if err != nil {
		return c.Status(fiber.StatusInternalServerError).JSON(fiber.Map{
			"error": err.Error(),
		})
	}

	return c.JSON(fiber.Map{
		"success": true,
		"data":    locations,
	})
}

func (h *LocationHandler) GetLatestLocation(c *fiber.Ctx) error {
	userID, err := strconv.ParseUint(c.Params("userId"), 10, 32)
	if err != nil {
		return c.Status(fiber.StatusBadRequest).JSON(fiber.Map{
			"error": "Invalid user ID",
		})
	}

	location, err := h.locationUseCase.GetLatestLocation(uint(userID))
	if err != nil {
		return c.Status(fiber.StatusNotFound).JSON(fiber.Map{
			"error": "No location found",
		})
	}

	return c.JSON(fiber.Map{
		"success": true,
		"data":    location,
	})
}

func (h *LocationHandler) GetAllLatestLocations(c *fiber.Ctx) error {
	locations, err := h.locationUseCase.GetAllLatestLocations()
	if err != nil {
		return c.Status(fiber.StatusInternalServerError).JSON(fiber.Map{
			"error": err.Error(),
		})
	}

	return c.JSON(fiber.Map{
		"success": true,
		"data":    locations,
	})
}
