FROM maven:3.8-eclipse-temurin-8 AS build
WORKDIR /app
COPY . .
RUN mvn clean install -DskipTests

FROM eclipse-temurin:8-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
ENV SERVER_PORT=8083
EXPOSE 8083
CMD ["java", "-jar", "app.jar"]