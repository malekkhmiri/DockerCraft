# Stage 1: Build the Angular app
FROM node:20-alpine as build
WORKDIR /app
COPY package*.json ./
RUN npm install
COPY . .
RUN npm run build --configuration=production

# Stage 2: Serve the app with Nginx
FROM nginx:alpine
# Remove default nginx static assets and config
RUN rm -rf /usr/share/nginx/html/* /etc/nginx/conf.d/*

# Copy the entire build output to a temporary location
COPY --from=build /app/dist/frontend-platform /usr/share/nginx/html-tmp

# Move files to the final location (handling potential /browser subfolder)
RUN if [ -d "/usr/share/nginx/html-tmp/browser" ]; then \
        mv /usr/share/nginx/html-tmp/browser/* /usr/share/nginx/html/; \
    else \
        mv /usr/share/nginx/html-tmp/* /usr/share/nginx/html/; \
    fi && rm -rf /usr/share/nginx/html-tmp

# Write a STATIC nginx config on port 8080
RUN echo 'server { \
    listen 8080; \
    server_name localhost; \
    root /usr/share/nginx/html; \
    index index.html index.htm; \
    location / { \
        try_files $uri $uri/ /index.html; \
    } \
    error_page 500 502 503 504 /50x.html; \
    location = /50x.html { \
        root /usr/share/nginx/html; \
    } \
}' > /etc/nginx/conf.d/default.conf

EXPOSE 8080
CMD ["nginx", "-g", "daemon off;"]
