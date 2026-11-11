FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY tcc.bilheteria/pom.xml .
RUN mvn -B -q dependency:go-offline
COPY tcc.bilheteria/src ./src
RUN mvn -B -q -DskipTests package && cp target/*.jar app.jar

FROM eclipse-temurin:21-jre
RUN apt-get update \
 && apt-get install -y --no-install-recommends curl \
 && rm -rf /var/lib/apt/lists/* \
 && useradd --system --uid 1001 bilheteria
WORKDIR /app
COPY --from=build /build/app.jar app.jar
USER bilheteria
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]