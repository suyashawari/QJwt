# Use a Node image to build and serve the dashboard
FROM node:18-alpine

WORKDIR /app

# Install dependencies
COPY package*.json ./
RUN npm install

# Copy the rest of the files
COPY . .

# Run the app
CMD ["npm", "start"]
