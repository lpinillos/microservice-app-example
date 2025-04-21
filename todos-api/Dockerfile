FROM node:8.17.0-buster
WORKDIR /app
COPY . .
RUN npm install
ENV TODO_API_PORT=8082
EXPOSE 8082
CMD ["npm", "start"]
