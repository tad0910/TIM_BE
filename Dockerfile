# =========================
# BUILD STAGE
# =========================
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app

# Copy toàn bộ source code
COPY . .

# Cấp quyền executable cho Maven Wrapper
RUN chmod +x mvnw

# Build project, bỏ qua test để nhanh hơn
RUN ./mvnw clean package -DskipTests

# =========================
# RUN STAGE
# =========================
FROM eclipse-temurin:21-jdk-alpine
WORKDIR /app

# Copy file jar từ build stage
COPY --from=build /app/target/*.jar app.jar

# Render sẽ cung cấp biến môi trường PORT
ENV PORT=8080

# Expose port (local hoặc mặc định)
EXPOSE 8080

# Chạy ứng dụng với port động Render
ENTRYPOINT ["sh", "-c", "java -Xmx256m -jar app.jar --server.port=${PORT}"]
