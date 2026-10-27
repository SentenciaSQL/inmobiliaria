FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /src
COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2 mvn -B -DskipTests dependency:go-offline
COPY src src
RUN --mount=type=cache,target=/root/.m2 mvn -B -DskipTests package

FROM eclipse-temurin:21.0.7_6-jre-jammy AS runtime
RUN groupadd --system --gid 10001 alzada \
  && useradd --system --uid 10001 --gid alzada --home-dir /app alzada
WORKDIR /app
COPY --link --from=build /src/target/alzada-api.jar /app/alzada-api.jar
USER alzada
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/alzada-api.jar"]
