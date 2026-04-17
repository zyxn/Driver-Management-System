package handler

import (
	"driver-management-backend/internal/usecase"
	"strconv"

	"github.com/gofiber/fiber/v2"
)

type ReportHandler struct {
	reportUseCase *usecase.ReportUseCase
}

func NewReportHandler(reportUseCase *usecase.ReportUseCase) *ReportHandler {
	return &ReportHandler{reportUseCase: reportUseCase}
}

func (h *ReportHandler) CreateReport(c *fiber.Ctx) error {
	var req usecase.CreateReportRequest
	if err := c.BodyParser(&req); err != nil {
		return c.Status(fiber.StatusBadRequest).JSON(fiber.Map{
			"error": "Invalid request body",
		})
	}

	fileHeader, _ := c.FormFile("image")

	report, err := h.reportUseCase.CreateReport(req, fileHeader)
	if err != nil {
		return c.Status(fiber.StatusBadRequest).JSON(fiber.Map{
			"error": err.Error(),
		})
	}

	return c.Status(fiber.StatusCreated).JSON(fiber.Map{
		"success": true,
		"data":    report,
	})
}

func (h *ReportHandler) GetReportHistory(c *fiber.Ctx) error {
	userID, err := strconv.ParseUint(c.Params("userId"), 10, 32)
	if err != nil {
		return c.Status(fiber.StatusBadRequest).JSON(fiber.Map{
			"error": "Invalid user ID",
		})
	}

	dateStr := c.Query("date")

	reports, err := h.reportUseCase.GetReportHistory(uint(userID), dateStr)
	if err != nil {
		return c.Status(fiber.StatusInternalServerError).JSON(fiber.Map{
			"error": err.Error(),
		})
	}

	return c.JSON(fiber.Map{
		"success": true,
		"data":    reports,
	})
}

func (h *ReportHandler) GetAllReports(c *fiber.Ctx) error {
	dateStr := c.Query("date")
	reports, err := h.reportUseCase.GetAllReports(dateStr)
	if err != nil {
		return c.Status(fiber.StatusInternalServerError).JSON(fiber.Map{
			"error": err.Error(),
		})
	}

	return c.JSON(fiber.Map{
		"success": true,
		"data":    reports,
	})
}

func (h *ReportHandler) GetReportByID(c *fiber.Ctx) error {
	reportID, err := strconv.ParseUint(c.Params("id"), 10, 32)
	if err != nil {
		return c.Status(fiber.StatusBadRequest).JSON(fiber.Map{
			"error": "Invalid report ID",
		})
	}

	report, err := h.reportUseCase.GetReportByID(uint(reportID))
	if err != nil {
		return c.Status(fiber.StatusNotFound).JSON(fiber.Map{
			"error": "Report not found",
		})
	}

	return c.JSON(fiber.Map{
		"success": true,
		"data":    report,
	})
}
