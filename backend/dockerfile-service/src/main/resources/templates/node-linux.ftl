FROM node:${version}-alpine
WORKDIR /app
COPY package*.json ./
RUN npm ci --only=production
COPY . .
EXPOSE ${port}
CMD ["node", "src/index.js"]
