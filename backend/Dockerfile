# Stage 1: Build
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# 使用缓存卷保存 Maven 依赖
RUN --mount=type=cache,target=/root/.m2/repository \
    --mount=type=cache,target=/root/.m2/wrapper \
    echo "Maven cache mounted"

COPY settings.xml /root/.m2/settings.xml
COPY pom.xml .

# 依赖层 - 只在 pom.xml 变化时重新构建
RUN --mount=type=cache,target=/root/.m2/repository \
    mvn dependency:go-offline -B

COPY src ./src

# 构建层
RUN --mount=type=cache,target=/root/.m2/repository \
    mvn package -DskipTests -B

# Stage 2: Run
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/oa-system-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
