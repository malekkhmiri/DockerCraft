# Stage 1: Build the Angular app
FROM node:20-alpine as build
WORKDIR /app
COPY package*.json ./
RUN npm install
COPY . .
RUN npm run build --configuration=production && ls -la dist/frontend-platform

# Stage 2: Serve the app with Nginx
FROM nginx:alpine
# Remove default nginx static assets and config
RUN rm -rf /usr/share/nginx/html/* /etc/nginx/conf.d/*

# Copy the entire build output to a temporary location
COPY --from=build /app/dist/frontend-platform /usr/share/nginx/html-tmp

# Move files to the final location (handling potential /browser subfolder)
# And add a signature to the index.html just to be 100% sure
RUN if [ -d "/usr/share/nginx/html-tmp/browser" ]; then \
        mv /usr/share/nginx/html-tmp/browser/* /usr/share/nginx/html/; \
    else \
        mv /usr/share/nginx/html-tmp/* /usr/share/nginx/html/; \
    fi && \
    rm -rf /usr/share/nginx/html-tmp && \
    echo "<!-- DEPLOY_ID: $(date +%s) -->" >> /usr/share/nginx/html/index.html

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
