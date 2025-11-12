# --- Giai đoạn 1: Build (Xây dựng) ---
# Sử dụng một image Java (JDK 21) để build code
# Đặt tên cho giai đoạn này là "builder"
FROM eclipse-temurin:21-jdk AS builder

# Thiết lập thư mục làm việc bên trong image
WORKDIR /workspace

# Sao chép file cấu hình Maven và pom.xml trước
# Điều này tận dụng Docker cache, nếu file pom.xml không đổi, nó sẽ không tải lại dependencies
COPY .mvn/ .mvn
COPY mvnw pom.xml ./

# Tải tất cả dependencies
RUN ./mvnw dependency:go-offline

# Sao chép toàn bộ source code
COPY src src

# Chạy lệnh build của Maven để tạo file .jar
# Bỏ qua test vì GitHub Actions đã chạy test rồi
RUN ./mvnw package -DskipTests


# --- Giai đoạn 2: Run (Chạy ứng dụng) ---
# Sử dụng một image JRE (chỉ chứa Java Runtime, nhẹ hơn JDK)
FROM eclipse-temurin:21-jre-alpine

# Lấy file .jar đã được build từ giai đoạn "builder"
# File jar thường nằm trong thư mục /workspace/target/
# Thay 'your-app-name-0.0.1-SNAPSHOT.jar' bằng tên file .jar thực tế của bạn
COPY --from=builder /workspace/target/*.jar app.jar

# (Tùy chọn) Expose port 8080 mà Spring Boot thường chạy
# Render sẽ tự động phát hiện port này, nhưng khai báo rõ ràng vẫn tốt hơn
EXPOSE 8080

# Lệnh để khởi động ứng dụng của bạn khi container chạy
# java -jar app.jar
ENTRYPOINT ["java", "-jar", "/app.jar"]