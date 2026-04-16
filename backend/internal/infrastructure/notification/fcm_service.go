package notification

import (
	"bytes"
	"context"
	"encoding/json"
	"fmt"
	"net/http"
	"os"

	"golang.org/x/oauth2/google"
)

type FCMService struct {
	projectID          string
	serviceAccountPath string
}

func NewFCMService(projectID, serviceAccountPath string) *FCMService {
	return &FCMService{
		projectID:          projectID,
		serviceAccountPath: serviceAccountPath,
	}
}

type FCMNotification struct {
	Title string `json:"title"`
	Body  string `json:"body"`
}

type FCMData struct {
	Type     string  `json:"type"`
	Speed    float64 `json:"speed,omitempty"`
	Limit    float64 `json:"limit,omitempty"`
	DriverID uint    `json:"driver_id,omitempty"`
	AlertID  uint    `json:"alert_id,omitempty"`
}

type FCMMessage struct {
	Message FCMMessageContent `json:"message"`
}

type FCMMessageContent struct {
	Token        string           `json:"token,omitempty"`
	Notification *FCMNotification `json:"notification,omitempty"`
	Data         map[string]string `json:"data,omitempty"`
	Android      *FCMAndroid      `json:"android,omitempty"`
}

type FCMAndroid struct {
	Priority     string                `json:"priority"`
	Notification *FCMAndroidNotification `json:"notification,omitempty"`
}

type FCMAndroidNotification struct {
	Sound string `json:"sound,omitempty"`
}

type FCMResponse struct {
	Name string `json:"name"`
}

func (s *FCMService) getAccessToken() (string, error) {
	ctx := context.Background()

	data, err := os.ReadFile(s.serviceAccountPath)
	if err != nil {
		return "", fmt.Errorf("failed to read service account file: %w", err)
	}

	conf, err := google.JWTConfigFromJSON(data, "https://www.googleapis.com/auth/firebase.messaging")
	if err != nil {
		return "", fmt.Errorf("failed to create JWT config: %w", err)
	}

	token, err := conf.TokenSource(ctx).Token()
	if err != nil {
		return "", fmt.Errorf("failed to get access token: %w", err)
	}

	return token.AccessToken, nil
}

func (s *FCMService) SendToToken(token string, notification *FCMNotification, data FCMData) error {
	// Convert FCMData to map[string]string (FCM v1 requirement)
	dataMap := make(map[string]string)
	dataMap["type"] = data.Type
	
	// IMPORTANT: Include notification data in the data payload
	// This ensures onMessageReceived is called even when app is in background
	if notification != nil {
		dataMap["title"] = notification.Title
		dataMap["body"] = notification.Body
	}
	
	if data.Speed > 0 {
		dataMap["speed"] = fmt.Sprintf("%.2f", data.Speed)
	}
	if data.Limit > 0 {
		dataMap["limit"] = fmt.Sprintf("%.2f", data.Limit)
	}
	if data.DriverID > 0 {
		dataMap["driver_id"] = fmt.Sprintf("%d", data.DriverID)
	}
	if data.AlertID > 0 {
		dataMap["alert_id"] = fmt.Sprintf("%d", data.AlertID)
	}

	message := FCMMessage{
		Message: FCMMessageContent{
			Token:        token,
			// DO NOT include notification payload - use data-only messages
			// This ensures our FCMService.onMessageReceived() is always called
			Notification: nil,
			Data:         dataMap,
			Android: &FCMAndroid{
				Priority: "high",
				// Remove notification sound config - we handle it in the app
				Notification: nil,
			},
		},
	}

	return s.send(message)
}

func (s *FCMService) SendToMultipleTokens(tokens []string, notification *FCMNotification, data FCMData) error {
	if len(tokens) == 0 {
		return nil
	}

	// FCM v1 doesn't support batch sending directly
	// We need to send one by one (or use Firebase Admin SDK batch)
	var lastErr error
	successCount := 0

	for _, token := range tokens {
		if err := s.SendToToken(token, notification, data); err != nil {
			lastErr = err
			continue
		}
		successCount++
	}

	if successCount == 0 && lastErr != nil {
		return fmt.Errorf("all notifications failed: %w", lastErr)
	}

	return nil
}

func (s *FCMService) send(message FCMMessage) error {
	if s.projectID == "" {
		return fmt.Errorf("FCM project ID not configured")
	}

	accessToken, err := s.getAccessToken()
	if err != nil {
		return fmt.Errorf("failed to get access token: %w", err)
	}

	url := fmt.Sprintf("https://fcm.googleapis.com/v1/projects/%s/messages:send", s.projectID)

	jsonData, err := json.Marshal(message)
	if err != nil {
		return fmt.Errorf("failed to marshal FCM message: %w", err)
	}

	req, err := http.NewRequest("POST", url, bytes.NewBuffer(jsonData))
	if err != nil {
		return fmt.Errorf("failed to create request: %w", err)
	}

	req.Header.Set("Content-Type", "application/json")
	req.Header.Set("Authorization", "Bearer "+accessToken)

	client := &http.Client{}
	resp, err := client.Do(req)
	if err != nil {
		return fmt.Errorf("failed to send FCM request: %w", err)
	}
	defer resp.Body.Close()

	if resp.StatusCode != http.StatusOK {
		var errBody map[string]interface{}
		json.NewDecoder(resp.Body).Decode(&errBody)
		return fmt.Errorf("FCM request failed with status %d: %v", resp.StatusCode, errBody)
	}

	var fcmResp FCMResponse
	if err := json.NewDecoder(resp.Body).Decode(&fcmResp); err != nil {
		return fmt.Errorf("failed to decode FCM response: %w", err)
	}

	return nil
}
