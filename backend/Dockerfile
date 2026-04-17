# ================================
# Stage 1: Build
# ================================
FROM golang:1.26-alpine AS builder

WORKDIR /app

# Install git (needed for some Go modules)
RUN apk add --no-cache git

# Copy go mod files first for better layer caching
COPY go.mod go.sum ./
RUN go mod download

# Copy source code
COPY . .

# Build the main binary
RUN CGO_ENABLED=0 GOOS=linux go build -o /app/server .

# Build the seed binary
RUN CGO_ENABLED=0 GOOS=linux go build -o /app/seed ./cmd/seed

# ================================
# Stage 2: Runtime
# ================================
FROM alpine:3.21

WORKDIR /app

# Install ca-certificates for HTTPS calls (OSRM, FCM, etc.)
RUN apk add --no-cache ca-certificates tzdata

# Copy binaries from builder
COPY --from=builder /app/server .
COPY --from=builder /app/seed .

# Copy serviceAccount.json if it exists (FCM)
# This will be mounted via volume or copied at build time
COPY serviceAccount.json ./serviceAccount.json

EXPOSE 3000

# Default command: run the server
CMD ["./server"]
