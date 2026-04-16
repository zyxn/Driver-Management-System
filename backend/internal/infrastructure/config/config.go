package config

import (
	"fmt"
	"os"
	"time"
)

type Config struct {
	Database DatabaseConfig
	JWT      JWTConfig
	Server   ServerConfig
	FCM      FCMConfig
	Alert    AlertConfig
}

type DatabaseConfig struct {
	Host     string
	Port     string
	User     string
	Password string
	DBName   string
	SSLMode  string
}

type JWTConfig struct {
	Secret string
	Expiry time.Duration
}

type ServerConfig struct {
	Port           string
	AllowedOrigins []string
}

type FCMConfig struct {
	ProjectID          string
	ServiceAccountPath string
}

type AlertConfig struct {
	SpeedLimit float64
}

func NewConfig() *Config {
	jwtExpiry, _ := time.ParseDuration(getEnv("JWT_EXPIRY", "24h"))

	return &Config{
		Database: DatabaseConfig{
			Host:     getEnv("DB_HOST", "localhost"),
			Port:     getEnv("DB_PORT", "5432"),
			User:     getEnv("DB_USER", "postgres"),
			Password: getEnv("DB_PASSWORD", "postgres"),
			DBName:   getEnv("DB_NAME", "driver_management"),
			SSLMode:  getEnv("DB_SSLMODE", "disable"),
		},
		JWT: JWTConfig{
			Secret: getEnv("JWT_SECRET", "your-secret-key"),
			Expiry: jwtExpiry,
		},
		Server: ServerConfig{
			Port:           getEnv("PORT", "8080"),
			AllowedOrigins: []string{getEnv("ALLOWED_ORIGINS", "*")},
		},
		FCM: FCMConfig{
			ProjectID:          getEnv("FCM_PROJECT_ID", ""),
			ServiceAccountPath: getEnv("FCM_SERVICE_ACCOUNT_PATH", "serviceAccount.json"),
		},
		Alert: AlertConfig{
			SpeedLimit: parseFloat(getEnv("SPEED_LIMIT", "150")),
		},
	}
}

func (c *DatabaseConfig) DSN() string {
	return fmt.Sprintf("host=%s port=%s user=%s password=%s dbname=%s sslmode=%s",
		c.Host, c.Port, c.User, c.Password, c.DBName, c.SSLMode)
}

func getEnv(key, defaultValue string) string {
	if value := os.Getenv(key); value != "" {
		return value
	}
	return defaultValue
}

func parseFloat(s string) float64 {
	var f float64
	fmt.Sscanf(s, "%f", &f)
	return f
}
