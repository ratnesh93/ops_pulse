# --- build stage ---
FROM node:20-alpine AS build
WORKDIR /app
COPY package.json package-lock.json* ./
RUN npm ci || npm install
COPY . .
RUN npm run build

# --- serve stage ---
FROM nginx:1.27-alpine
COPY --from=build /app/dist /usr/share/nginx/html
# nginx entrypoint runs envsubst on templates/*.template at container start
COPY nginx.conf.template /etc/nginx/templates/default.conf.template
ENV PORT=80
ENV BACKEND_SCHEME=http
ENV BACKEND_HOST=backend:8080
ENV BACKEND_DOMAIN_SUFFIX=""
ENV NGINX_ENTRYPOINT_LOCAL_RESOLVERS=1
EXPOSE 80
