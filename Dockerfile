# 1. Dùng ảnh nền Java 21 (nhẹ)
FROM eclipse-temurin:21-jdk-alpine

# 2. Tạo thư mục tạm
VOLUME /tmp

# 3. Copy file .jar đã build vào trong ảnh
# (Lưu ý: tên file phải KHỚP với tên trong pom.xml của bạn)
COPY target/appTim-0.0.1-SNAPSHOT.jar app.jar

# 4. Lệnh chạy ứng dụng
# (Lưu ý: Thêm các tham số bộ nhớ để tránh sập trên gói Free)
ENTRYPOINT ["java", "-Xmx300m", "-jar", "/app.jar"]