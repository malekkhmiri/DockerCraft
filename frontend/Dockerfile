# Stage 1: Build the Angular app
FROM node:20-alpine as build
WORKDIR /app
COPY package*.json ./
RUN npm install
COPY . .
RUN npm run build --configuration=production

# Stage 2: Serve the app with Nginx
FROM nginx:alpine
# Remove default nginx static assets
RUN rm -rf /usr/share/nginx/html/*

# Copy build artifacts from build stage
# Note: Angular 17+ with application builder puts files in dist/project-name/browser
COPY --from=build /app/dist/frontend-platform/browser /usr/share/nginx/html

# Custom nginx config to handle Angular routing and dynamic PORT
RUN echo 'server { \
    listen ${PORT}; \
    location / { \
        root /usr/share/nginx/html; \
        index index.html index.htm; \
        try_files $uri $uri/ /index.html; \
    } \
}' > /etc/nginx/conf.d/default.conf.template

EXPOSE 8080
CMD ["sh", "-c", "envsubst \"\${PORT}\" < /etc/nginx/conf.d/default.conf.template > /etc/nginx/conf.d/default.conf && exec nginx -g \"daemon off;\""]
