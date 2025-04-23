FROM node:8.17.0
WORKDIR /app
COPY . .
RUN npm install
ENV PORT=8081
EXPOSE 8081
CMD ["npm", "start"]
