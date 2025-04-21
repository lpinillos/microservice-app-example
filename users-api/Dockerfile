FROM eclipse-temurin:8-jdk AS build
WORKDIR /app
COPY . .
RUN ./mvnw clean install -DskipTests

FROM eclipse-temurin:8-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
ENV SERVER_PORT=8083
EXPOSE 8083
CMD ["java", "-jar", "app.jar"]
