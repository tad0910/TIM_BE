package com.tim.appTim.controller;

import com.tim.appTim.dto.CreateUserDTO;
import com.tim.appTim.entity.User;
import com.tim.appTim.service.KeycloakSyncService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PostMapping;

import javax.ws.rs.ClientErrorException;
import java.util.List;

@RestController
@RequestMapping("/api/v1/keycloak")
public class KeycloakController {

    @Autowired
    private KeycloakSyncService keycloakSyncService;

    @PutMapping("/users/{userId}")
    @PreAuthorize("hasAuthority('user:update_all') or authentication.principal.getSubject() == #userId")
    public ResponseEntity<?> updateUser(@PathVariable String userId, @RequestBody UpdateUserDTO updateUserDTO) {
        try {
            keycloakSyncService.updateUser(userId, updateUserDTO);
            return ResponseEntity.ok("User updated successfully");
        } catch (ClientErrorException e) {
            int status = e.getResponse().getStatus();
            String errorMessage = e.getResponse().readEntity(String.class);
            switch (status) {
                case 400:
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body("Invalid request to Keycloak: " + errorMessage);
                case 401:
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                            .body("Unauthorized access to Keycloak: " + errorMessage);
                case 403:
                    return ResponseEntity.status(HttpStatus.FORBIDDEN)
                            .body("Forbidden access to Keycloak: " + errorMessage);
                case 404:
                    return ResponseEntity.status(HttpStatus.NOT_FOUND)
                            .body("Keycloak user not found: " + errorMessage);
                default:
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body("Unexpected Keycloak error: " + errorMessage);
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Internal server error during user update: " + e.getMessage());
        }
    }

    @PostMapping("/users/{userId}/logout")
    @PreAuthorize("hasAuthority('user:logout_all') or authentication.principal.getSubject() == #userId")
    public ResponseEntity<?> logoutUser(@PathVariable String userId) {
        try {
            keycloakSyncService.logoutUserFromKeycloak(userId);
            return ResponseEntity.ok("Đã vô hiệu hóa tất cả phiên làm việc của người dùng thành công.");
        } catch (ClientErrorException e) {
            if (e.getResponse().getStatus() == 404) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("Không tìm thấy người dùng với ID: " + userId);
            }
            String errorMessage = e.getResponse().readEntity(String.class);
            return ResponseEntity.status(e.getResponse().getStatus())
                    .body("Lỗi từ Keycloak khi đăng xuất: " + errorMessage);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Lỗi hệ thống: " + e.getMessage());
        }
    }

    public static class UpdateUserDTO {
        private String username;
        private String email;
        private String firstName;
        private String lastName;
        private String status;

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getFirstName() { return firstName; }
        public void setFirstName(String firstName) { this.firstName = firstName; }
        public String getLastName() { return lastName; }
        public void setLastName(String lastName) { this.lastName = lastName; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }
}