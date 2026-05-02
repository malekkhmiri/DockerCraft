# Stage 1: Build the Angular app
FROM node:20-alpine as build
WORKDIR /app
COPY package*.json ./
RUN npm install
COPY . .
RUN npm run build --configuration=production && ls -la dist/frontend-platform

# Stage 2: Serve the app with Nginx
FROM nginx:stable
# Remove default nginx static assets and config
RUN rm -rf /usr/share/nginx/html/* /etc/nginx/conf.d/*

# Copy build artifacts from build stage
# Angular 19 puts production files in dist/frontend-platform/browser
COPY --from=build /app/dist/frontend-platform/browser /usr/share/nginx/html

# Add a VISIBLE signature to the index.html
RUN sed -i 's/<body[^>]*>/<body style="border-top: 10px solid #00ff00;"> <div style="background: #00ff00; color: #000; text-align: center; font-weight: bold; padding: 5px; position: fixed; top: 0; left: 0; right: 0; z-index: 9999;">DOCKERCRAFT LIVE (REVISION: '$(date +%s)')<\/div>/' /usr/share/nginx/html/index.html

# Write a CUSTOM nginx config that we are SURE is ours
RUN echo 'server { \
    listen 8080; \
    server_name localhost; \
    root /usr/share/nginx/html; \
    index index.html index.htm; \
    location / { \
        add_header X-Debug-Status "DockerCraft-Config-Applied"; \
        try_files $uri $uri/ /index.html; \
    } \
    error_page 404 /404.html; \
    location = /404.html { \
        return 200 "DockerCraft Debug: Page Not Found (Check Angular Build)"; \
    } \
}' > /etc/nginx/conf.d/default.conf

EXPOSE 8080
CMD ["nginx", "-g", "daemon off;"]
