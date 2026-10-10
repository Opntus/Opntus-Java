FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml ./
COPY src ./src
RUN mvn -B package -DskipTests
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/opntus-1.0.0.jar /app/opntus.jar
RUN mkdir -p /data
ENV PORT=8080 DATABASE_URL=jdbc:sqlite:/data/opntus.db
EXPOSE 8080
CMD ["java","-jar","/app/opntus.jar"]
