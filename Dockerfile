# DÒNG SỬA LẠI CHÍNH XÁC
FROM eclipse-temurin:21-jdk-alpine

# Đặt tên cho file .jar sẽ được build
ARG JAR_FILE=target/*.jar

# Copy file .jar từ thư mục 'target' vào bên trong image
# và đổi tên thành 'app.jar' cho thống nhất
COPY ${JAR_FILE} app.jar

# Mở cổng 8080 (cổng mặc định của Spring Boot)
EXPOSE 8080

# Lệnh để chạy ứng dụng khi container khởi động
ENTRYPOINT ["java","-jar","/app.jar"]