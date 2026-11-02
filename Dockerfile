FROM eclipse-temurin:25-jdk AS build
RUN apt-get update && apt-get install -y libatomic1
COPY . /app
WORKDIR /app
RUN ./mvnw package -DskipTests

FROM eclipse-temurin:25-jre
COPY --from=build /app/target/library-management-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]