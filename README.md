# ALM-Team-Tim-BE

#Yêu cầu hệ thống
Java 21
Maven
Database: MariaDB
IDE: IntelliJ IDEA / Eclipse / VS Code

#Cài đặt
1. Clone repo
git clone https://github.com/codegym-software/ALM-Team-Tim-BE.git

2. Cấu hình Database
Sửa nội dung kết nối trong src/main/resources/application.properties

3. Build dự án
mvn clean install

4. Chạy dự án
mvn spring-boot:run

dự án mặc định ở cổng 8081

Nếu sử dụng VS Code thì cần cài đặt các extension sau:
- Java
- Debugger for Java
- Maven for Java
- Language Support for Java™ by Red Hat
- Project Manager for Java
- Spring Boot Tools