FROM maven:3.9.9-eclipse-temurin-17-alpine AS builder
WORKDIR /app

# Cache dependencies trước
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -DskipTests -B

# ========== STAGE 2: Runtime siêu nhẹ ==========
FROM eclipse-temurin:17-jre-alpine

# Tạo user non-root
RUN addgroup --system spring && adduser --system --ingroup spring spring
USER spring:spring

WORKDIR /app


COPY --from=builder /app/target/*.jar app.jar

# Environment
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=80.0 -Dfile.encoding=UTF-8 -Duser.timezone=Asia/Ho_Chi_Minh"
ENV PORT=8080
EXPOSE ${PORT}

# Healthcheck
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:${PORT}/actuator/health || exit 1


ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar --server.port=${PORT}"]