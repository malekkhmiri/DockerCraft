# Stage 1: Build the Angular app
FROM node:20-alpine as build
WORKDIR /app
COPY package*.json ./
RUN npm install
COPY . .
# Build and DEBUG: List everything in dist to find the real path
RUN npm run build --configuration=production && \
    echo "--- DEBUG: LISTING DIST CONTENT ---" && \
    find dist -name index.html && \
    echo "--- END DEBUG ---"

# Stage 2: Serve the app with Nginx
FROM nginx:stable
# Remove default nginx static assets and config
RUN rm -rf /usr/share/nginx/html/* /etc/nginx/conf.d/*

# DYNAMIC COPY: We will find the path in the build stage and copy it
# Since we don't know the path yet, let's copy EVERYTHING from dist and then move it
COPY --from=build /app/dist /usr/share/nginx/html-tmp

RUN INDEX_PATH=$(find /usr/share/nginx/html-tmp -name index.html | head -n 1) && \
    INDEX_DIR=$(dirname $INDEX_PATH) && \
    echo "Moving files from $INDEX_DIR to /usr/share/nginx/html/" && \
    mv $INDEX_DIR/* /usr/share/nginx/html/ && \
    rm -rf /usr/share/nginx/html-tmp

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
