# ====== BUILD STAGE ======
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app

# Copy toàn bộ project và build bằng Maven Wrapper
COPY . .
RUN ./mvnw clean package -DskipTests

# ====== RUN STAGE ======
FROM eclipse-temurin:21-jdk-alpine
WORKDIR /app

# Copy file JAR từ giai đoạn build
COPY --from=build /app/target/*.jar app.jar

# Render sẽ cung cấp biến môi trường PORT khi chạy
ENV PORT=8080
EXPOSE 8080

# Chạy app với port động Render cấp
ENTRYPOINT ["sh", "-c", "java -Xmx256m -jar app.jar --server.port=${PORT}"]
