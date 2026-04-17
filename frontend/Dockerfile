# ================================
# Stage 1: Build
# ================================
FROM node:22-alpine AS builder

WORKDIR /app

# Copy package files for layer caching
COPY package.json package-lock.json ./

# Install dependencies
RUN npm ci

# Copy source code
COPY . .

# Build the SvelteKit app
# The API URL is baked in at build time via VITE env vars
ARG VITE_API_URL=http://localhost:3000/api/v1
ENV VITE_API_URL=$VITE_API_URL

RUN npm run build

# ================================
# Stage 2: Runtime
# ================================
FROM node:22-alpine

WORKDIR /app

# Copy built output and production deps
COPY --from=builder /app/build ./build
COPY --from=builder /app/package.json ./
COPY --from=builder /app/node_modules ./node_modules

EXPOSE 3001

# SvelteKit adapter-auto / adapter-node listens on PORT
ENV PORT=3001
ENV HOST=0.0.0.0
ENV ORIGIN=http://localhost:3001

CMD ["node", "build"]
