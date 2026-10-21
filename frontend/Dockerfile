# Frontend image (design.md 17.1): the built React app served by nginx, which also forwards /api to the backend.
# Build from the frontend/ folder:  docker build -t <name> .
# Two stages: Node builds the static files, then only those files and nginx go into the image that runs.

# ---------- Stage 1: build the static files ----------
FROM node:24-alpine AS build
WORKDIR /app

# Package files first: npm ci is redone only when the dependencies change, not at every code change
COPY package.json package-lock.json ./
RUN npm ci

COPY . .
# Read by Vite at build time and written into the JavaScript (design.md 17.3): changing it needs a new image.
# Relative, so the browser calls the same site that served the page, through nginx
ARG VITE_API_URL=/api/v1
ENV VITE_API_URL=${VITE_API_URL}
# tsc -b && vite build, the same command as on your machine; the result is in dist/
RUN npm run build

# ---------- Stage 2: serve ----------
# Stable branch of nginx (even minor number)
FROM nginx:1.30-alpine

COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html

EXPOSE 443

# Docker asks nginx for the home page over HTTPS every 30 s. --no-check-certificate: the certificate is made for the
# domain, not for 127.0.0.1. The certificate files come from a mounted folder: without them nginx does not start
HEALTHCHECK --interval=30s --timeout=5s --start-period=10s --retries=3 \
  CMD wget -q -O /dev/null --no-check-certificate https://127.0.0.1/ || exit 1
