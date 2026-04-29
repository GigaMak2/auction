FROM amazoncorretto:21-al2023-headless
WORKDIR /app
COPY build/libs/*.jar app.jar
EXPOSE 8080
EXPOSE 9001
ENTRYPOINT ["java", "-jar", "app.jar"]
