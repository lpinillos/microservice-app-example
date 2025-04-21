# Microservice App - PRFT Devops Training

This is the application you are going to use through the whole traninig. This, hopefully, will teach you the fundamentals you need in a real project. You will find a basic TODO application designed with a [microservice architecture](https://microservices.io). Although is a TODO application, it is interesting because the microservices that compose it are written in different programming language or frameworks (Go, Python, Vue, Java, and NodeJS). With this design you will experiment with multiple build tools and environments. 

## Components
In each folder you can find a more in-depth explanation of each component:

1. [Users API](/users-api) is a Spring Boot application. Provides user profiles. At the moment, does not provide full CRUD, just getting a single user and all users.
2. [Auth API](/auth-api) is a Go application, and provides authorization functionality. Generates [JWT](https://jwt.io/) tokens to be used with other APIs.
3. [TODOs API](/todos-api) is a NodeJS application, provides CRUD functionality over user's TODO records. Also, it logs "create" and "delete" operations to [Redis](https://redis.io/) queue.
4. [Log Message Processor](/log-message-processor) is a queue processor written in Python. Its purpose is to read messages from a Redis queue and print them to standard output.
5. [Frontend](/frontend) Vue application, provides UI.

## Architecture

Take a look at the components diagram that describes them and their interactions.
![microservice-app-example](/arch-img/Microservices.png)

### Docker

This project includes containerized versions of all microservices using Docker. Each service has its own Dockerfile, and the entire system can be launched using ```docker-compose```.

### Containerized Microservices Overview

| Service         | Language/Stack     | Dockerfile Path                        | Exposed Port |
|----------------|--------------------|----------------------------------------|--------------|
| `auth-api`     | Go                 | `./auth-api/Dockerfile`               | 8000         |
| `users-api`    | Java (Spring Boot) | `./users-api/Dockerfile`              | 8083         |
| `todos-api`    | Node.js            | `./todos-api/Dockerfile`              | 8082         |
| `log-processor`| Python             | `./log-message-processor/Dockerfile`  | N/A          |
| `frontend`     | Vue.js             | `./frontend/Dockerfile`               | 8081         |
| `redis`        | Redis              | *(Docker Hub official image)*         | 6379         |




Running Everything (Production or Dev)
From the root of the project, use:

```bash
docker-compose up --build
```
This will:

Build and run all services

Link them via Docker internal networking

Start the frontend at ```http://localhost:8081```

 Environment Variables (passed via Compose)
Each service is configured through environment variables defined in ```docker-compose.yml```. 

Example:

```
auth-api:
  environment:
    - AUTH_API_PORT=8000
    - USERS_API_ADDRESS=http://users-api:8083
    - JWT_SECRET=PRFT
```
Cleaning Containers
To stop and remove all containers:

```docker-compose down```
