# Stage 1: API Build
FROM maven:3.8.6-eclipse-temurin-17 AS API_BUILD
COPY api/checkstyle checkstyle
COPY api/pom.xml .
COPY api/src src
RUN --mount=type=cache,target=/root/.m2 mvn package
RUN java -Djarmode=layertools -jar target/*.jar extract --destination target/extracted

# Stage 2: UI Build
FROM node:18.18.0 AS UI_BUILD
WORKDIR /app
COPY ui/package.json ./
COPY ui/package-lock.json ./
RUN npm ci --silent
ARG APP_API_SERVER_URL
ARG PLAYWRIGHT_SLOW_MO 1000
ARG ADMIN_USERNAME
ARG ADMIN_PASS
ENV APP_API_SERVER_URL $APP_API_SERVER_URL
ENV PLAYWRIGHT_SLOW_MO $PLAYWRIGHT_SLOW_MO
ENV ADMIN_USERNAME $ADMIN_USERNAME
ENV ADMIN_PASS $ADMIN_PASS
COPY ui/tsconfig.json ./
COPY ui/webpack.config.js ./
COPY ui/src src
RUN npm run build

# Stage 4: Final Build
FROM eclipse-temurin:17-jre
WORKDIR /application
COPY --from=API_BUILD ./target/extracted/dependencies/ ./
COPY --from=API_BUILD ./target/extracted/snapshot-dependencies/ ./
COPY --from=API_BUILD ./target/extracted/spring-boot-loader/ ./
COPY --from=API_BUILD ./target/extracted/application/ ./
COPY --from=UI_BUILD /app/dist/ ./static/

ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
