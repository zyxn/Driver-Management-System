package handler

import (
	"driver-management-backend/internal/usecase"
	"log"
	"sync"
	"time"

	"github.com/gofiber/fiber/v2"
	"github.com/gofiber/websocket/v2"
)

type WebSocketHandler struct {
	locationUseCase *usecase.LocationUseCase
	clients         map[*websocket.Conn]bool
	broadcast       chan LocationBroadcast
	mu              sync.RWMutex
}

type LocationBroadcast struct {
	Type      string                 `json:"type"`
	UserID    uint                   `json:"user_id"`
	Data      interface{}            `json:"data"`
	Timestamp time.Time              `json:"timestamp"`
}

type LocationUpdate struct {
	UserID          uint      `json:"user_id"`
	Username        string    `json:"username"`
	FullName        string    `json:"full_name"`
	Latitude        float64   `json:"latitude"`
	Longitude       float64   `json:"longitude"`
	Speed           float64   `json:"speed"`
	Accuracy        float64   `json:"accuracy"`
	Heading         *float64  `json:"heading,omitempty"`
	Altitude        *float64  `json:"altitude,omitempty"`
	RecordedAtUTC   time.Time `json:"recorded_at_utc"`
	Timezone        string    `json:"timezone"`
	RecordedAtLocal time.Time `json:"recorded_at_local"`
}

func NewWebSocketHandler(locationUseCase *usecase.LocationUseCase) *WebSocketHandler {
	handler := &WebSocketHandler{
		locationUseCase: locationUseCase,
		clients:         make(map[*websocket.Conn]bool),
		broadcast:       make(chan LocationBroadcast, 256),
	}
	
	// Start broadcast goroutine
	go handler.handleBroadcast()
	
	return handler
}

func (h *WebSocketHandler) handleBroadcast() {
	for {
		msg := <-h.broadcast
		h.mu.RLock()
		for client := range h.clients {
			err := client.WriteJSON(msg)
			if err != nil {
				log.Printf("WebSocket write error: %v", err)
				client.Close()
				h.mu.RUnlock()
				h.mu.Lock()
				delete(h.clients, client)
				h.mu.Unlock()
				h.mu.RLock()
			}
		}
		h.mu.RUnlock()
	}
}

func (h *WebSocketHandler) HandleLocationTracking(c *websocket.Conn) {
	// Register client
	h.mu.Lock()
	h.clients[c] = true
	h.mu.Unlock()

	log.Printf("New WebSocket client connected. Total clients: %d", len(h.clients))

	// Send welcome message
	welcomeMsg := LocationBroadcast{
		Type:      "connected",
		Data:      map[string]string{"message": "Connected to location tracking"},
		Timestamp: time.Now(),
	}
	c.WriteJSON(welcomeMsg)

	defer func() {
		h.mu.Lock()
		delete(h.clients, c)
		h.mu.Unlock()
		c.Close()
		log.Printf("WebSocket client disconnected. Total clients: %d", len(h.clients))
	}()

	for {
		var msg map[string]interface{}
		err := c.ReadJSON(&msg)
		if err != nil {
			if websocket.IsUnexpectedCloseError(err, websocket.CloseGoingAway, websocket.CloseAbnormalClosure) {
				log.Printf("WebSocket error: %v", err)
			}
			break
		}

		// Handle different message types
		msgType, ok := msg["type"].(string)
		if !ok {
			continue
		}

		switch msgType {
		case "location_update":
			h.handleLocationUpdate(msg)
		case "ping":
			c.WriteJSON(map[string]interface{}{
				"type":      "pong",
				"timestamp": time.Now(),
			})
		}
	}
}

func (h *WebSocketHandler) handleLocationUpdate(msg map[string]interface{}) {
	// Parse location data
	data, ok := msg["data"].(map[string]interface{})
	if !ok {
		log.Println("Invalid location data format")
		return
	}

	// Extract user_id
	userID, ok := data["user_id"].(float64)
	if !ok {
		log.Println("Missing or invalid user_id")
		return
	}

	// Create location tracking request
	req := usecase.TrackLocationRequest{
		UserID:    uint(userID),
		Latitude:  data["latitude"].(float64),
		Longitude: data["longitude"].(float64),
		Timezone:  data["timezone"].(string),
	}

	// Optional fields
	if speed, ok := data["speed"].(float64); ok {
		req.Speed = speed
	}
	if accuracy, ok := data["accuracy"].(float64); ok {
		req.Accuracy = accuracy
	}
	if heading, ok := data["heading"].(float64); ok {
		req.Heading = &heading
	}
	if altitude, ok := data["altitude"].(float64); ok {
		req.Altitude = &altitude
	}

	// Save location to database
	location, err := h.locationUseCase.TrackLocation(req)
	if err != nil {
		log.Printf("Error tracking location: %v", err)
		return
	}

	// Broadcast to all connected clients
	broadcast := LocationBroadcast{
		Type:      "location_update",
		UserID:    uint(userID),
		Data:      location,
		Timestamp: time.Now(),
	}

	select {
	case h.broadcast <- broadcast:
	default:
		log.Println("Broadcast channel full, dropping message")
	}
}

// HTTP endpoint to upgrade to WebSocket
func (h *WebSocketHandler) UpgradeToWebSocket() fiber.Handler {
	return websocket.New(func(c *websocket.Conn) {
		h.HandleLocationTracking(c)
	})
}

// Broadcast location update from HTTP endpoint
func (h *WebSocketHandler) BroadcastLocationUpdate(location interface{}, userID uint) {
	broadcast := LocationBroadcast{
		Type:      "location_update",
		UserID:    userID,
		Data:      location,
		Timestamp: time.Now(),
	}

	select {
	case h.broadcast <- broadcast:
	default:
		log.Println("Broadcast channel full, dropping message")
	}
}
