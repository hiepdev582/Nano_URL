# Stage 1: Build Nuxt App
FROM node:22-alpine AS build
WORKDIR /app

# Cài đặt dependencies
COPY package*.json ./
RUN npm ci

# Copy code và build project
COPY . .
RUN npm run build

# Stage 2: Production Engine (Node server với Nitro)
FROM node:22-alpine AS runner
WORKDIR /app

ENV NODE_ENV=production
ENV PORT=3000
# Cho phép các dịch vụ khác bên ngoài container (như Nginx) có thể kết nối được tới server này.
ENV HOST=0.0.0.0

# Chỉ copy kết quả build (.output)
COPY --from=build /app/.output ./.output

EXPOSE 3000

# Chạy Nuxt server Nitro bằng Node.js
CMD ["node", ".output/server/index.mjs"]