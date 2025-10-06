package com.tim.appTim.service;

import java.time.LocalDateTime;
import java.util.List;

import javax.ws.rs.ClientErrorException;
import javax.ws.rs.core.Response;

import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.tim.appTim.entity.User;
import com.tim.appTim.repository.UserRepository;

@Service
public class KeycloakSyncService {

    @Value("${keycloak.server-url}")
    private String serverUrl;

    @Value("${keycloak.realm}")
    private String realm;

    @Value("${keycloak.client-id-app}")
    private String clientId;

    @Value("${keycloak.client-secret}")
    private String clientSecret;

    private final UserRepository userRepository;

    public KeycloakSyncService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // Tạo Keycloak client với service account
    private Keycloak getKeycloakClient() {
    System.out.println("serverUrl: " + serverUrl.trim());
    System.out.println("realm: " + realm.trim());
    System.out.println("clientId: " + clientId.trim());
    System.out.println("clientSecret: " + clientSecret.trim());
    return KeycloakBuilder.builder()
        .serverUrl(serverUrl.trim())
        .realm(realm.trim())
        .grantType(OAuth2Constants.CLIENT_CREDENTIALS)
        .clientId(clientId.trim())
        .clientSecret(clientSecret.trim())
        .build();
    }
    // Đồng bộ user Keycloak vào database
    @Scheduled(fixedDelay = 300_00) // mỗi 5 phút
    public void syncUsers() {
        Keycloak keycloak = null;
        try {
            keycloak = getKeycloakClient();
            RealmResource realmResource = keycloak.realm(realm);
            List<UserRepresentation> kcUsers = realmResource.users().list();

            for (UserRepresentation kcUser : kcUsers) {
                boolean exists = userRepository.findByUsername(kcUser.getUsername()).isPresent()
                        || userRepository.findByEmail(kcUser.getEmail()).isPresent();

                if (!exists) {
                    User newUser = new User();
                    newUser.setUsername(kcUser.getUsername());
                    newUser.setEmail(kcUser.getEmail());
                    newUser.setCreatedAt(LocalDateTime.now());
                    newUser.setPassword("KEYCLOAK_MANAGED");
                    newUser.setRole(User.Role.sinh_vien);

                    userRepository.save(newUser);
                    System.out.println("✅ Synced user to DB: " + kcUser.getUsername());
                }
            }
        } catch (ClientErrorException e) {
            System.err.println("❌ Keycloak sync failed: " + e.getResponse().readEntity(String.class));
            throw e;
        } catch (Exception e) {
            System.err.println("❌ Keycloak sync failed: " + e.getMessage());
            throw e;
        } finally {
            if (keycloak != null) {
                keycloak.close(); // Giải phóng resources
            }
        }
    }

    // Tạo user trong database và đồng bộ lên Keycloak
    public void createUserInDbAndKeycloak(String username, String email, String fullName, String password, String status) {
        Keycloak keycloak = null;
        try {
            // 1. Tạo user trong database
            User newUser = new User();
            newUser.setUsername(username);
            newUser.setEmail(email);
            newUser.setCreatedAt(LocalDateTime.now());
            newUser.setPassword("KEYCLOAK_MANAGED"); // Mật khẩu quản lý bởi Keycloak
            newUser.setRole(User.Role.sinh_vien);
            userRepository.save(newUser);
            System.out.println("✅ Created user in DB: " + username);

            // 2. Tạo user trên Keycloak
            keycloak = getKeycloakClient();
            RealmResource realmResource = keycloak.realm(realm);

            UserRepresentation user = new UserRepresentation();
            user.setUsername(username);
            user.setEmail(email);
            user.setFirstName(fullName != null ? fullName.split(" ")[0] : "");
            user.setLastName(fullName != null ? 
                String.join(" ", java.util.Arrays.copyOfRange(fullName.split(" "), 1, fullName.split(" ").length)) : "");
            user.setEnabled("active".equalsIgnoreCase(status));
            user.setEmailVerified(false);

            // Gửi yêu cầu tạo user
            Response response = realmResource.users().create(user);
            if (response.getStatus() >= 200 && response.getStatus() < 300) {
                // Lấy user ID từ header Location
                String userId = response.getLocation().getPath().replaceAll(".*/([^/]+)$", "$1");
                System.out.println("✅ Created user in Keycloak: " + username);

                // Set mật khẩu
                CredentialRepresentation credential = new CredentialRepresentation();
                credential.setType(CredentialRepresentation.PASSWORD);
                credential.setValue(password);
                credential.setTemporary(false);

                UserResource userResource = realmResource.users().get(userId);
                userResource.resetPassword(credential);
                System.out.println("✅ Password set for user: " + username);
            } else {
                throw new RuntimeException("Failed to create user in Keycloak: " + response.getStatusInfo());
            }
        } catch (ClientErrorException e) {
            System.err.println("❌ Failed to create user in Keycloak: " + e.getResponse().readEntity(String.class));
            // Xóa user trong DB nếu Keycloak thất bại để đảm bảo nhất quán
            userRepository.findByUsername(username).ifPresent(userRepository::delete);
            throw e;
        } catch (Exception e) {
            System.err.println("❌ Failed to create user: " + e.getMessage());
            userRepository.findByUsername(username).ifPresent(userRepository::delete);
            throw e;
        } finally {
            if (keycloak != null) {
                keycloak.close();
            }
        }
    }
}