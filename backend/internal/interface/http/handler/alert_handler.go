package handler

import (
	"driver-management-backend/internal/domain/entity"
	"driver-management-backend/internal/usecase"
	"strconv"

	"github.com/gofiber/fiber/v2"
)

type AlertHandler struct {
	alertUsecase usecase.AlertUsecase
}

func NewAlertHandler(alertUsecase usecase.AlertUsecase) *AlertHandler {
	return &AlertHandler{
		alertUsecase: alertUsecase,
	}
}

func (h *AlertHandler) CreateAlert(c *fiber.Ctx) error {
	var req entity.CreateAlertRequest
	if err := c.BodyParser(&req); err != nil {
		return c.Status(fiber.StatusBadRequest).JSON(fiber.Map{
			"success": false,
			"error":   err.Error(),
		})
	}

	alert, err := h.alertUsecase.CreateAlert(&req)
	if err != nil {
		return c.Status(fiber.StatusInternalServerError).JSON(fiber.Map{
			"success": false,
			"error":   err.Error(),
		})
	}

	return c.Status(fiber.StatusCreated).JSON(fiber.Map{
		"success": true,
		"data":    alert,
	})
}

func (h *AlertHandler) GetAllAlerts(c *fiber.Ctx) error {
	alerts, err := h.alertUsecase.GetAllAlerts()
	if err != nil {
		return c.Status(fiber.StatusInternalServerError).JSON(fiber.Map{
			"success": false,
			"error":   err.Error(),
		})
	}

	return c.JSON(fiber.Map{
		"success": true,
		"data":    alerts,
	})
}

func (h *AlertHandler) GetAlertsByDriver(c *fiber.Ctx) error {
	driverID, err := strconv.ParseUint(c.Params("id"), 10, 32)
	if err != nil {
		return c.Status(fiber.StatusBadRequest).JSON(fiber.Map{
			"success": false,
			"error":   "Invalid driver ID",
		})
	}

	alerts, err := h.alertUsecase.GetAlertsByDriver(uint(driverID))
	if err != nil {
		return c.Status(fiber.StatusInternalServerError).JSON(fiber.Map{
			"success": false,
			"error":   err.Error(),
		})
	}

	return c.JSON(fiber.Map{
		"success": true,
		"data":    alerts,
	})
}

func (h *AlertHandler) UpdateAlertStatus(c *fiber.Ctx) error {
	alertID, err := strconv.ParseUint(c.Params("id"), 10, 32)
	if err != nil {
		return c.Status(fiber.StatusBadRequest).JSON(fiber.Map{
			"success": false,
			"error":   "Invalid alert ID",
		})
	}

	var req struct {
		Status entity.AlertStatus `json:"status"`
	}

	if err := c.BodyParser(&req); err != nil {
		return c.Status(fiber.StatusBadRequest).JSON(fiber.Map{
			"success": false,
			"error":   err.Error(),
		})
	}

	if err := h.alertUsecase.UpdateAlertStatus(uint(alertID), req.Status); err != nil {
		return c.Status(fiber.StatusInternalServerError).JSON(fiber.Map{
			"success": false,
			"error":   err.Error(),
		})
	}

	return c.JSON(fiber.Map{
		"success": true,
		"message": "Alert status updated successfully",
	})
}
